package org.powertac.tourney.services;

import org.powertac.tourney.beans.Machine;

import java.io.InputStream;
import java.net.URL;
import java.net.URLConnection;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.TimerTask;

public class RunBootstrap extends TimerTask
{
  private String logSuffix = "boot-";   // boot-game-" + game.getGameId() + "-tourney-"+
                                        // game.getCompetitionName();
  private String tourneyUrl = "";       // game.getTournamentSchedulerUrl();
  private String serverConfig = "";     // game.getServerConfigUrl();
  private String bootstrapUrl = "";     // This needs to be empty for jenkins to run
                                        // a bootstrapgame.getBootstrapUrl();
  private String pomUrl = "";           // game.getPomUrl();
  private String gameId = "";           // String.valueOf(game.getGameId());
  private String machineName = "";
  private String destination = "";
  private boolean usingMachine = false;

  public RunBootstrap (int gameId, String tourneyUrl, String pomUrl,
                       String destination)
  {
    this.gameId = String.valueOf(gameId);
    this.tourneyUrl = tourneyUrl;
    this.pomUrl = pomUrl;
    this.destination = destination;

    // Assumes Jenkins and TS live in the same location as per the install
    this.serverConfig = tourneyUrl + "/faces/properties.jsp?gameId=" + gameId;
  }

  public RunBootstrap (int gameId, String tourneyUrl, String pomUrl,
                       String destination, String machineName)
  {
    this.gameId = String.valueOf(gameId);
    this.tourneyUrl = tourneyUrl;
    this.pomUrl = pomUrl;
    this.destination = destination;
    this.machineName = machineName;
    this.usingMachine = true;

    // Assumes Jenkins and TS live in the same location as per the install
    this.serverConfig = tourneyUrl + "/faces/properties.jsp?gameId=" + gameId;
  }

  private void checkMachineAvailable ()
  {
    // TODO This doesn't seem to work properly

    Database db = new Database();
    try {
      db.startTrans();
      List<Machine> machines = db.getMachines();
      List<Machine> available = new ArrayList<Machine>();
      for (Machine m: machines) {
        if (m.getStatus().equalsIgnoreCase("idle") && m.isAvailable()) {
          available.add(m);
        }
      }
      if (available.size() > 0) {
        if (!usingMachine) {
          db.updateGameJmsUrlById(
            Integer.parseInt(gameId  ), "tcp://" + available.get(0).getName() + ":61616");
         db.updateGameMachine(
             Integer.parseInt(gameId),
             available.get(0).getMachineId());
         db.setMachineStatus(available.get(0).getMachineId(), "running");
         this.machineName = available.get(0).getName();
       }
       System.out.println("[INFO] Running boot " + gameId + " on machine "
                          + machineName);
       db.commitTrans();
     }
     else {
       db.abortTrans();
       System.out.println(
           "[INFO] No machines available to run scheduled boot: "
           + gameId + " ... will retry in 5 minutes");
       // Thread.sleep(300000);
       // this.run();
     }
   }
   catch (NumberFormatException e) {
     e.printStackTrace();
   }
   catch (SQLException e) {
     e.printStackTrace();
   }
 }

  public void run ()
  {
    // TODO
    checkMachineAvailable();

    String finalUrl =
      "http://localhost:8080/jenkins/job/"
              + "start-server-instance/buildWithParameters?"
              + "token=start-instance" + "&tourneyUrl=" + tourneyUrl
              + "&suffix=" + logSuffix + "&propUrl=" + serverConfig
              + "&pomUrl=" + pomUrl + "&bootstrapUrl=" + bootstrapUrl
              + "&machine=" + machineName + "&gameId=" + gameId
              + "&destination=" + destination;

    System.out.println("[INFO] Final url: " + finalUrl);

    try {
      URL url = new URL(finalUrl);
      URLConnection conn = url.openConnection();
      // Get the response
      InputStream input = conn.getInputStream();
      System.out.println("[INFO] Jenkins request to bootstrap game: " + gameId);
    }
    catch (Exception e) {
      e.printStackTrace();
      System.out.println("[INFO] Jenkins failure to bootstrap game: " + gameId);
      Database db = new Database();
      try {
        db.updateGameStatusById(Integer.parseInt(gameId), "boot-failed");
      }
      catch (NumberFormatException e1) {
        // TODO Auto-generated catch block
        e1.printStackTrace();
      }
      catch (SQLException e1) {
        // TODO Auto-generated catch block
        e1.printStackTrace();
      }
    }
  }
}
