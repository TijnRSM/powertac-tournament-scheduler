package org.powertac.tourney.services;

import org.powertac.tourney.beans.Broker;
import org.powertac.tourney.beans.Game;
import org.powertac.tourney.beans.Machine;
import org.powertac.tourney.beans.Scheduler;
import org.powertac.tourney.beans.Tournament;
import org.powertac.tourney.constants.Constants;
import org.springframework.stereotype.Service;

import javax.servlet.http.HttpServletRequest;
import java.io.*;
//import java.sql.SQLException;
import java.util.List;
import java.util.Map;

import static org.powertac.tourney.services.Utils.log;

@Service("rest")
public class Rest
{
  private TournamentProperties properties = new TournamentProperties();

  public String parseBrokerLogin (Map<String, String[]> params)
  {
    String responseType = params.get(Constants.Rest.REQ_PARAM_TYPE)[0];
    String brokerAuthToken = params.get(Constants.Rest.REQ_PARAM_AUTH_TOKEN)[0];
    String competitionName = params.get(Constants.Rest.REQ_PARAM_JOIN)[0];

    String retryResponse = "{\n \"retry\":%d\n}";
    String loginResponse = "{\n \"login\":%d\n \"jmsUrl\":%s\n \"gameToken\":%s\n}";
    String doneResponse = "{\n \"done\":\"true\"\n}";;
    if (responseType.equalsIgnoreCase("xml")) {
      String head = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n<message>";
      String tail = "</message>";
      retryResponse = head + "<retry>%d</retry>" + tail;
      loginResponse = head + "<login><jmsUrl>%s</jmsUrl><queueName>%s</queueName></login>" + tail;
      doneResponse = head + "<done></done>" + tail;
    }
    else {
      retryResponse = "{\n \"retry\":%d\n}";
      loginResponse = "{\n \"login\":%d\n \"jmsUrl\":%s\n \"queueName\":%s\n}";
      doneResponse = "{\n \"done\":\"true\"\n}";
    }

    Database db = new Database();
    try {
      db.startTrans();
      // JEC - this could be a REALLY long list after a while.
      List<Game> allGames = db.getGames();
      List<Tournament> allTournaments = db.getTournaments(Tournament.STATE.pending);
      allTournaments.addAll(db.getTournaments(Tournament.STATE.in_progress));

      if (competitionName != null && allGames != null) {
        // Find all games that match the competition name and have brokers
        // registered
        for (Game g: allGames) {
          // Only consider games that are ready (started, but waiting for logins)
          if (g.getStatus().equals(Game.STATE.game_ready.toString())) {
            Tournament t = db.getTournamentByGameId(g.getGameId());

            if (competitionName.equalsIgnoreCase(t.getTournamentName())) {
              Broker broker = g.getBrokerRegistration(brokerAuthToken);
//              synchronized (skip) {
//                if (skip.containsKey(g.getGameId() + brokerAuthToken)
//                    && skip.get(g.getGameId() + brokerAuthToken) == g
//                            .getGameId()) {
//                  System.out.println("[INFO] Broker " + brokerAuthToken
//                                     + " already recieved login for game "
//                                     + g.getGameId());
//                  continue;
//                }
//                System.out.println("[INFO] Sending login to : "
//                                   + brokerAuthToken + " jmsUrl : "
//                                   + g.getJmsUrl());
//                skip.put(g.getGameId() + brokerAuthToken, g.getGameId());
//              }
              // here we need to add the queue name
              // update ingame gameid, brokerid, true
              if (null == broker) {
                // broker is not in this game
                continue;
              }
              else if (broker.getBrokerInGame()) {
                // this broker is already in this game
                continue;
              }
              else {
                broker.setBrokerInGame(true);
                db.updateBrokerInGame(g.getGameId(), broker);
                return String.format(loginResponse, g.getJmsUrl(), broker.getQueueName());
              }
            }
          }
        }
        db.commitTrans();

        boolean competitionExists = false;
        for (Tournament t: allTournaments) {
          if (competitionName.equals(t.getTournamentName())) {
            competitionExists = true;
            break;
          }
        }

        if (competitionExists) {
          log("[INFO] Broker: {0} attempted to log into existing tournament"
              + ": {1} --sending retry", brokerAuthToken, competitionName);
          return String.format(retryResponse, 20);
        }
        else {
          log("[INFO] Broker: {0} attempted to log into non-existing tournament"
              + ": {1} --sending done", brokerAuthToken, competitionName);
          return doneResponse;
        }
      }
      Thread.sleep(30000);
    }
    catch (Exception e) {
      db.abortTrans();
      e.printStackTrace();
    }
    
    return doneResponse;
  }

  public String parseServerInterface (Map<String, String[]> params, String clientAddress)
  {
    if (!Utils.checkClientAllowed(clientAddress)) {
      return "error";
    }

    try {
      String actionString = params.get(Constants.Rest.REQ_PARAM_ACTION)[0];
      if (actionString.equalsIgnoreCase("status")) {
        String statusString = params.get(Constants.Rest.REQ_PARAM_STATUS)[0];
        int gameId = Integer.parseInt(
            params.get(Constants.Rest.REQ_PARAM_GAME_ID)[0]);
        return handleStatus(statusString, gameId);
      }
      else if (actionString.equalsIgnoreCase("boot")) {
        String gameId = params.get(Constants.Rest.REQ_PARAM_GAME_ID)[0];
        return serveBoot(gameId);
      }
    }
    catch (Exception ignored) {}
    return "error";
  }

  /***
   * Returns a properties file string
   *
   * @param params
   * @return String representing a properties file
   */
  public String parseProperties (Map<String, String[]> params)
  {
    String gameId = "0";
    try {
      gameId = params.get(Constants.Rest.REQ_PARAM_GAME_ID)[0];
    }
    catch (Exception ignored) {}

    List<String> props;
    props = CreateProperties.getPropertiesForGameId(Integer.parseInt(gameId));

    Game g;
    Database db = new Database();
    try {
    	db.startTrans();
    	g = db.getGame(Integer.parseInt(gameId));
    	db.commitTrans();
    }
    catch(Exception e) {
    	db.abortTrans();
    	e.printStackTrace();
      return "";
    }

    String weatherLocation = "server.weatherService.weatherLocation = ";
    String startTime = "common.competition.simulationBaseTime = ";
    String jms = "server.jmsManagementService.jmsBrokerUrl = ";
    String remote = "server.visualizerProxyService.remoteVisualizer = ";
    String queueName = "server.visualizerProxyService.visualizerQueueName = ";
    String minTimeslot = "common.competition.minimumTimeslotCount = 1320";
    String expectedTimeslot = "common.competition.expectedTimeslotCount = 1440";
    String serverFirstTimeout =
      "server.competitionControlService.firstLoginTimeout = 600000";
    String serverTimeout =
      "server.competitionControlService.loginTimeout = 120000";

    // Test settings
    if (g.getGameName().contains("Test") || g.getGameName().contains("test")) {
    	minTimeslot = "common.competition.minimumTimeslotCount = 200";
    	expectedTimeslot = "common.competition.expectedTimeslotCount = 220";
    }

    String result = "";
    if (props.size() == 4) {
      result += weatherLocation + props.get(0) + "\n";
      result += startTime + props.get(1) + "\n";
      if (props.get(2).isEmpty()) {
    	  result += jms + "tcp://localhost:61616" + "\n";
      }
      else {
    	  result += jms + props.get(2) + "\n";
      }
      result += serverFirstTimeout + "\n";
      result += serverTimeout + "\n";
      if (props.get(2).length() > 2) {
        result += remote + "true\n";
      }
      else {
        result += remote + "\n";
      }
      result += minTimeslot + "\n";
      result += expectedTimeslot + "\n";
      result += queueName + props.get(3) + "\n";
    }

    return result;
  }

  /***
   * Returns a pom file string
   *
   * @param params
   * @return String representing a pom file
   */
  public String parsePom (Map<String, String[]> params)
  {
    try {
      String pomId = params.get(Constants.Rest.REQ_PARAM_POM_ID)[0];
      return servePom(pomId);
    }
    catch (Exception e) {
      log("Error: " + e.getMessage());
      return "error";
    }
  }

  public String servePom(String pomId)
  {
    String result = "";
    try {
      // Determine pom-file location
      String pomLocation = properties.getProperty("pomLocation") +
          "pom."+ pomId +".xml";

      // Read the file
      FileInputStream fstream = new FileInputStream(pomLocation);
      DataInputStream in = new DataInputStream(fstream);
      BufferedReader br = new BufferedReader(new InputStreamReader(in));
      String strLine;
      while ((strLine = br.readLine()) != null) {
        result += strLine + "\n";
      }

      // Close the input stream
      fstream.close();
      in.close();
      br.close();
    }
    catch (Exception e) {
      log("Error : " + e.getMessage());
      result = "error";
    }

    return result;
  }

  public String serveBoot(String gameId)
  {
    String result = "";

    try {
      // Determine boot-file location
      String bootLocation = properties.getProperty("bootLocation") +
                            "game-" + gameId + "-boot.xml";

      // Read the file
      FileInputStream fstream = new FileInputStream(bootLocation);
      DataInputStream in = new DataInputStream(fstream);
      BufferedReader br = new BufferedReader(new InputStreamReader(in));
      String strLine;
      while ((strLine = br.readLine()) != null) {
        result += strLine + "\n";
      }

      // Close the input stream
      fstream.close();
      in.close();
      br.close();
    }
    catch (Exception e) {
      log("Error : " + e.getMessage());
      result = "error";
    }

    return result;
  }

  // TODO This should be moved to a Game StateMachine
  private String handleStatus(String status, int gameId)
  {
    log("[INFO] Recieved {0} message from game: {1}", status, gameId);

    Game.STATE state;
    try {
      state = Game.STATE.valueOf(status);
    }
    catch (Exception e) {
      return "error";
    }

    Scheduler scheduler = (Scheduler) SpringApplicationContext.getBean("scheduler");
    Database db = new Database();

    try {
      db.startTrans();
      Game g = db.getGame(gameId);

      db.updateGameStatusById(gameId, state);
      log("[INFO] Update game: {0} to {1}", gameId, status);

      switch (state) {
        case boot_in_progress:
          // Remove bootfile, it shouldn't exist anyway
          removeBootFile(gameId);
          break;

        case boot_complete:
          db.updateGameBootstrapById(gameId, true);
          log("[INFO] Update game: {0} to hasBootstrap", gameId);

          db.updateGameFreeMachine(gameId);
          log("[INFO] Freeing Machines for game: {0}", gameId);
          db.setMachineStatus(g.getMachineId(), Machine.STATE.idle);
          log("[INFO] Setting machine {0} to idle {0}", g.getMachineId());
          Scheduler.bootRunning = false;
          break;

        case boot_failed:
          log("[WARN] BOOT {0} FAILED!", gameId);

          db.updateGameFreeMachine(gameId);
          log("[INFO] Freeing Machines for game: {0}", gameId);
          db.setMachineStatus(g.getMachineId(), Machine.STATE.idle);
          log("[INFO] Setting machine {0} to idle {0}", g.getMachineId());
          Scheduler.bootRunning = false;
          break;

        case game_complete:
          db.updateGameFreeBrokers(gameId);
          log("[INFO] Freeing Brokers for game: {0}", gameId);
          db.updateGameFreeMachine(gameId);
          log("[INFO] Freeing Machines for game: {0}", gameId);
          scheduler.resetServer(g.getMachineId());
          db.setMachineStatus(g.getMachineId(), Machine.STATE.idle);
          log("[INFO] Setting machine {0} to idle {0}", g.getMachineId());
          break;

        case game_failed:
          log("[WARN] GAME {0} FAILED!", gameId);

          db.updateGameFreeBrokers(gameId);
          log("[INFO] Freeing Brokers for game: {0}", gameId);
          db.updateGameFreeMachine(gameId);
          log("[INFO] Freeing Machines for game: {0}", gameId);
          scheduler.resetServer(g.getMachineId());
          db.setMachineStatus(g.getMachineId(), Machine.STATE.idle);
          log("[INFO] Setting machine {0} to idle {0}", g.getMachineId());
          break;
      }

      db.commitTrans();
    }
    catch (Exception e) {
      db.abortTrans();
      e.printStackTrace();
    }
    return "success";
  }

  //<editor-fold desc="TODO Cleanup">
  /*
    if (status.equals(Game.STATES.boot_in_progress.toString())) {
      log("[INFO] Recieved bootstrap running message from game: {0}", gameId);

      // Remove bootfile, but it shouldn't exist anyway
      removeBootFile(gameId);

      try {
        db.startTrans();

        db.updateGameStatusById(gameId, Game.STATES.boot_in_progress.toString());
        log("[INFO] Update game: {0} to {1}", gameId, status);

        db.commitTrans();
      }
      catch (SQLException e) {
        db.abortTrans();
        e.printStackTrace();
      }
    }
    else if (status.equals(Game.STATES.boot_complete.toString())) {
      log("[INFO] Recieved bootstrap done message from game: {0}", gameId);

      try {
        db.startTrans();

        db.updateGameStatusById(gameId, Game.STATES.boot_complete.toString());
        db.updateGameBootstrapById(gameId, true);
        log("[INFO] Update game: {0} to {1} and hasBootstrap", gameId, status);
        Game g = db.getGame(gameId);
        db.updateGameFreeMachine(gameId);
        log("[INFO] Freeing Machines for game: {0}", gameId);
        db.setMachineStatus(g.getMachineId(), Machine.STATES.idle);
        log("[INFO] Setting machine {0} to idle {0}", g.getMachineId());
        Scheduler.bootRunning = false;

        db.commitTrans();
      }
      catch (Exception e) {
        db.abortTrans();
        e.printStackTrace();
      }
    }
    else if (status.equals(Game.STATES.boot_failed.toString())) {
      log("[WARN] BOOT {0} FAILED!", gameId);

      try {
        db.startTrans();

        db.updateGameStatusById(gameId, Game.STATES.boot_failed.toString());
        log("[INFO] Update game: {0} to {1}", gameId, status);
        Game g = db.getGame(gameId);
        db.updateGameFreeMachine(gameId);
        log("[INFO] Freeing Machines for game: {0}", gameId);
        db.setMachineStatus(g.getMachineId(), Machine.STATES.idle);
        log("[INFO] Setting machine {0} to idle {0}", g.getMachineId());
        Scheduler.bootRunning = false;

        db.commitTrans();
      }
      catch (SQLException e) {
        db.abortTrans();
        e.printStackTrace();
      }
    }
    else if (status.equals(Game.STATES.game_ready.toString())) {
      log("[INFO] Recieved game ready message from game: {0}", gameId);

      try {
        db.startTrans();

        db.updateGameStatusById(gameId, Game.STATES.game_ready.toString());
        log("[INFO] Update game: {0} to {1}", gameId, status);

        db.commitTrans();
      }
      catch (SQLException e) {
        db.abortTrans();
        e.printStackTrace();
      }
    }

    else if (status.equals(Game.STATES.game_in_progress.toString())) {
      log("[INFO] Recieved game ready message from game: {0}", gameId);

      try {
        db.startTrans();

        db.updateGameStatusById(gameId, Game.STATES.game_in_progress.toString());
        log("[INFO] Update game: {0} to {1}", gameId, status);

        db.commitTrans();
      }
      catch (SQLException e) {
        db.abortTrans();
        e.printStackTrace();
      }
    }
    else if (status.equals(Game.STATES.game_complete.toString())) {
      log("[INFO] Recieved game done message from game: {0}", gameId);

      try {
        db.startTrans();

        db.updateGameStatusById(gameId, Game.STATES.game_complete.toString());
        log("[INFO] Update game: {0} to {1}", gameId, status);
        db.updateGameFreeBrokers(gameId);
        log("[INFO] Freeing Brokers for game: {0}", gameId);
        db.updateGameFreeMachine(gameId);
        log("[INFO] Freeing Machines for game: {0}", gameId);
        Game g = db.getGame(gameId);
        scheduler.resetServer(g.getMachineId());
        db.setMachineStatus(g.getMachineId(), Machine.STATES.idle);
        log("[INFO] Setting machine {0} to idle {0}", g.getMachineId());

        db.commitTrans();
      }
      catch (Exception e) {
        db.abortTrans();
        e.printStackTrace();
      }
    }
    else if (status.equals(Game.STATES.game_failed.toString())) {
      log("[WARN] GAME {0} FAILED!", gameId);

      try {
        db.startTrans();

        db.updateGameStatusById(gameId, Game.STATES.game_failed.toString());
        log("[INFO] Update game: {0} to {1}", gameId, status);
        db.updateGameFreeBrokers(gameId);
        log("[INFO] Freeing Brokers for game: {0}", gameId);
        db.updateGameFreeMachine(gameId);
        log("[INFO] Freeing Machines for game: {0}", gameId);
        Game g = db.getGame(gameId);
        scheduler.resetServer(g.getMachineId());
        db.setMachineStatus(g.getMachineId(), Machine.STATES.idle);
        log("[INFO] Setting machine {0} to idle {0}", g.getMachineId());

        db.commitTrans();
      }
      catch (SQLException e) {
        db.abortTrans();
        e.printStackTrace();
      }
    }
    return "success";
  }
  */
  //</editor-fold>

  private void removeBootFile(int gameId)
  {
    String bootLocation = properties.getProperty("bootLocation") +
        gameId + "-boot.xml";
    File f = new File(bootLocation);

    if (!f.exists()) {
      return;
    }

    if (!f.canWrite()) {
      log("[Error] Write protected: {0}", bootLocation);
    }

    if (!f.delete()) {
      log("[Error] Failed to delete : {0}", bootLocation);
    }
  }

  /***
   * Handle 'PUT' to serverInterface.jsp, either boot.xml or (Boot|Sim) log
   */
  public String handleServerInterfacePUT (Map<String, String[]> params, HttpServletRequest request)
  {
    if (!Utils.checkClientAllowed(request.getRemoteAddr())) {
      return "error";
    }

    try {
      String fileName = params.get(Constants.Rest.REQ_PARAM_FILENAME)[0];

      String path;
      if (fileName.endsWith("boot.xml")) {
        path = properties.getProperty("bootLocation") + fileName;
      } else {
        path = properties.getProperty("logLocation") + fileName;
      }

      // Write to file
      InputStream is = request.getInputStream();
      FileOutputStream fos = new FileOutputStream(path);
      byte buf[] = new byte[1024];
      int letti;
      while ((letti = is.read(buf)) > 0) {
        fos.write(buf, 0, letti);
      }
      fos.close();
    } catch (Exception e) {
      return "error";
    }
    return "success";
  }
}
