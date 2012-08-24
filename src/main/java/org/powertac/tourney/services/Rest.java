package org.powertac.tourney.services;

import org.apache.log4j.Logger;
import org.powertac.tourney.beans.Agent;
import org.powertac.tourney.beans.Broker;
import org.powertac.tourney.beans.Game;
import org.powertac.tourney.beans.Tournament;
import org.powertac.tourney.constants.Constants;
import org.springframework.stereotype.Service;

import javax.servlet.http.HttpServletRequest;
import java.io.*;
import java.sql.SQLException;
import java.util.Date;
import java.util.List;
import java.util.Map;

@Service("rest")
public class Rest
{
  private static Logger log = Logger.getLogger("TMLogger");

  public synchronized String parseBrokerLogin (Map<String, String[]> params)
  {
    String responseType = params.get(Constants.Rest.REQ_PARAM_TYPE)[0];
    String brokerAuthToken = params.get(Constants.Rest.REQ_PARAM_AUTH_TOKEN)[0];
    String tournamentName = params.get(Constants.Rest.REQ_PARAM_JOIN)[0];
    String retryResponse = "{\n \"retry\":%d\n}";
    String loginResponse = "{\n \"login\":%d\n \"jmsUrl\":%s\n \"queueName\":%s\n \"serverQueue\":%s\n}";
    String doneResponse = "{\n \"done\":\"true\"\n}";
    if (responseType.equalsIgnoreCase("xml")) {
      String head = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n<message>";
      String tail = "</message>";
      retryResponse = head + "<retry>%d</retry>" + tail;
      loginResponse = head + "<login><jmsUrl>%s</jmsUrl><queueName>%s</queueName><serverQueue>%s</serverQueue></login>" + tail;
      doneResponse = head + "<done></done>" + tail;
    }

    // TODO Remove below
    log.debug("");
    log.info(String.format("Broker %s login request : %s",
        brokerAuthToken, tournamentName));

    Database db = new Database();
    try {
      db.startTrans();

      // Check if broker exists
      Broker broker = db.getBroker(brokerAuthToken);
      if (broker == null) {
        log.info("Broker doesn't exists : " + brokerAuthToken);
        db.commitTrans();
        return doneResponse;
      }
      log.debug("Broker id is : " + broker.getBrokerId());

      // Check if tournament exists
      Tournament t = db.getTournamentByName(tournamentName);
      if (t == null) {
        log.info("Tournament doesn't exists : " + tournamentName);
        db.commitTrans();
        return doneResponse;
      }

      // Check if tournament is finished
      if (t.stateEquals(Tournament.STATE.complete)) {
        log.info("Tournament is finished, we're done : " + tournamentName);
        db.commitTrans();
        return doneResponse;
      }

      // Check if broker is registered for this tournament
      if (!db.isRegistered(t.getTournamentId(), broker.getBrokerId())) {
        log.info(String.format("Broker not registered for tournament " +
            t.getTournamentName()));
        db.commitTrans();
        return doneResponse;
      }

      // TODO Should this be a config item? Move this to sql?
      long readyDeadline = 2*60*1000;
      long nowStamp = new Date().getTime();

      // Check if any games are more than X minutes ready (to allow Viz Login)
      for (Game game: db.getReadyGamesInTourney(t.getTournamentId())) {
        Agent.STATE state = db.getAgentStatus(
            game.getGameId(), broker.getBrokerId());

        if (state == null) {
          // The broker isn't in this game
          continue;
        }
        else if (!state.equals(Agent.STATE.pending)) {
          // Another agent of this broker already logged into this game
          continue;
        }

        log.debug("Game " + game.getGameId() + " is ready");

        long diff = nowStamp - game.getReadyTime().getTime();
        if (diff < readyDeadline) {
          log.debug("Broker needs to wait for the viz timeout : " +
            (readyDeadline - diff) / 1000);
          continue;
        }

        db.updateAgentStatus(game.getGameId(), broker.getBrokerId(),
            Agent.STATE.in_progress);
        String queueName = db.getBrokerQueueName(
            game.getGameId(), broker.getBrokerId());
        log.info(String.format("Sending login to broker %s : %s, %s, %s",
            broker.getBrokerName(), game.getJmsUrl(),
            queueName, game.getServerQueue()));
        db.commitTrans();
        return String.format(loginResponse, game.getJmsUrl(),
            queueName, game.getServerQueue());
      }

      db.commitTrans();
      log.debug(String.format("No games ready to start for tournament %s",
          tournamentName));
      return String.format(retryResponse, 60);
    }
    catch (Exception e) {
      db.abortTrans();
      e.printStackTrace();
      log.error("Error, sending done response");
      return doneResponse;
    }
  }

  /**
   * Handles a login GET request from a visualizer of the form<br/>
   * &nbsp;../visualizerLogin.jsp?machineName<br/>
   * Response is either retry(n) to tell the viz to wait n seconds and try again,
   * or queueName(qn) to tell the visualizer to connect to its machine and
   * listen on the queue named qn.
   */
  public synchronized String parseVisualizerLogin (HttpServletRequest request,
                                      Map<String, String[]> params)
  {
    String machineName = params.get("machineName")[0];
    String head = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n<message>";
    String tail = "</message>";
    String retryResponse = head + "<retry>%d</retry>" + tail;
    String loginResponse = head + "<login><queueName>%s</queueName><serverQueue>%s</serverQueue></login>" + tail;
    String errorResponse = head + "<error>%s</error>" + tail;

    // TODO Remove below
    log.debug("");
    log.info("Visualizer login request : " + machineName);

    // Validate source of request
    if (!Utils.checkVizAllowed(request.getRemoteHost())) {
      return String.format(errorResponse, "invalid login request");
    }

    // If there is a game in game_ready state on the machine for this viz,then
    // return a login with the correct queue name; otherwise, return a retry.
    List<Game> readyGames;
    Database db = new Database();
    try {
      db.startTrans();
      // Only allow to log in to 'ready' games
      readyGames = db.findGamesByStatusAndMachine(Game.STATE.game_ready,
                                                      machineName);
      if (readyGames.isEmpty()) {
        db.commitTrans();
        log.debug("No games available, retry visualizer");
        return String.format(retryResponse, 60);
      }
      else {
        // We'll use the first Game in the list
        Game candidate = readyGames.get(0);
        String queue = candidate.getVisualizerQueue();
        String svrQueue = candidate.getServerQueue();
        db.commitTrans();
        log.info("Game available, login visualizer, " + queue +", "+ svrQueue);
        return String.format(loginResponse, queue, svrQueue);
      }
    }
    catch (SQLException e) {
      db.abortTrans();
      log.error(e.toString());
      e.printStackTrace();
      return String.format(errorResponse, "database error");
    }
  }

  public String parseServerInterface (Map<String, String[]> params, String clientAddress)
  {
    if (!Utils.checkMachineAllowed(clientAddress)) {
      return "error";
    }

    try {
      // TODO Make these constants as well

      String actionString = params.get(Constants.Rest.REQ_PARAM_ACTION)[0];
      if (actionString.equalsIgnoreCase("status")) {
        String statusString = params.get(Constants.Rest.REQ_PARAM_STATUS)[0];
        int gameId = Integer.parseInt(
            params.get(Constants.Rest.REQ_PARAM_GAME_ID)[0]);
        return Game.handleStatus(statusString, gameId);
      }
      else if (actionString.equalsIgnoreCase("boot")) {
        String gameId = params.get(Constants.Rest.REQ_PARAM_GAME_ID)[0];
        return serveBoot(gameId);
      }

      else if (actionString.equalsIgnoreCase("heartbeat")) {
        String message = params.get(Constants.Rest.REQ_PARAM_MESSAGE)[0];
        return handleHeartBeat(message);
      }
    }

    catch (Exception ignored) {}
    return "error";
  }

  /***
   * Returns a properties file string
   *
   * @param params :
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
    String remote = "server.visualizerProxyService.remoteVisualizer = true";
    String vizQ = "server.visualizerProxyService.visualizerQueueName = ";
    String minTimeslot = "common.competition.minimumTimeslotCount = 1380";
    String expectedTimeslot = "common.competition.expectedTimeslotCount = 1440";
    String serverFirstTimeout =
      "server.competitionControlService.firstLoginTimeout = 600000";
    String serverTimeout =
      "server.competitionControlService.loginTimeout = 120000";

    // Test settings
    if (g.getGameName().toLowerCase().contains("test")) {
    	minTimeslot = "common.competition.minimumTimeslotCount = 200";
    	expectedTimeslot = "common.competition.expectedTimeslotCount = 220";
    }

    String result = "";
    // JEC - replaced a HORRIBLE magic number.
    if (props.size() > 0) {
      result += weatherLocation + props.get(0) + "\n";
    }
    if (props.size() > 1) {
      result += startTime + props.get(1) + "\n";
    }
    if (props.size() > 2) {
      if (props.get(2).isEmpty()) {
    	  result += jms + "tcp://localhost:61616" + "\n";
      }
      else {
    	  result += jms + props.get(2) + "\n";
      }
    }
    result += serverFirstTimeout + "\n";
    result += serverTimeout + "\n";
    result += remote + "\n";
    result += vizQ + g.getVisualizerQueue() + "\n";
    result += minTimeslot + "\n";
    result += expectedTimeslot + "\n";

    return result;
  }

  /***
   * Returns a pom file string
   *
   * @param params :
   * @return String representing a pom file
   */
  public String parsePom (Map<String, String[]> params)
  {
    try {
      String pomId = params.get(Constants.Rest.REQ_PARAM_POM_ID)[0];
      return servePom(pomId);
    }
    catch (Exception e) {
      log.error(e.getMessage());
      return "error";
    }
  }

  public String servePom(String pomId)
  {
    String result = "";
    try {
      // Determine pom-file location
      TournamentProperties properties = TournamentProperties.getProperties();
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
      log.error(e.getMessage());
      result = "error";
    }

    return result;
  }

  public String serveBoot(String gameId)
  {
    String result = "";

    try {
      // Determine boot-file location
      TournamentProperties properties = TournamentProperties.getProperties();
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
      log.error(e.getMessage());
      result = "error";
    }

    return result;
  }

  public String handleHeartBeat (String message)
  {
    log.info("We received this heartBeat message from the server :");
    log.info(message);

    return "success";
  }

  /***
   * Handle 'PUT' to serverInterface.jsp, either boot.xml or (Boot|Sim) log
   */
  public String handleServerInterfacePUT (Map<String, String[]> params, HttpServletRequest request)
  {
    if (!Utils.checkMachineAllowed(request.getRemoteAddr())) {
      return "error";
    }

    try {
      String fileName = params.get(Constants.Rest.REQ_PARAM_FILENAME)[0];
      TournamentProperties properties = TournamentProperties.getProperties();

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

      if (fileName.contains("sim-logs")) {
        try {
          new LogParser(properties.getProperty("logLocation"), fileName);
        }
        catch (Exception e) {
          log.error("Error parsing log " + fileName);
        }
      }
    } catch (Exception e) {
      return "error";
    }
    return "success";
  }

  /***
   * Handle 'POST' to serverInterface.jsp, this is a end-of-game message
   */
  public String handlePostServerInterfacePOST(Map<String, String[]> params, HttpServletRequest request)
  {
    if (!Utils.checkMachineAllowed(request.getRemoteAddr())) {
      return "error";
    }

    try {
      String actionString = params.get(Constants.Rest.REQ_PARAM_ACTION)[0];
      if (!actionString.equalsIgnoreCase("gameresults")) {
        log.debug("The message didn't have the right action-string!");
        return "error";
      }

      String message = params.get(Constants.Rest.REQ_PARAM_MESSAGE)[0];
      log.info("We received this gameResult message from the server :");
      log.info(message);

      return "success";
    } catch (Exception e) {
      log.error("Something went wrong with receiving the POST message!");
      log.error(e.getMessage());
      return "error";
    }
  }
}
