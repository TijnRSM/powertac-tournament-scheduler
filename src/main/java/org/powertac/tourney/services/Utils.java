/**
 * Created by IntelliJ IDEA.
 * User: govert
 * Date: 6/29/12
 * Time: 1:07 PM
 */

package org.powertac.tourney.services;

import org.apache.log4j.Logger;
import org.powertac.tourney.beans.Machine;

import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.TimeZone;


public class Utils {
  private static Logger log = Logger.getLogger("TMLogger");

  public static HashMap<String, String> machineIPs = null;
  public static HashMap<String, String> vizIPs = null;
  public static HashMap<String, String> localIPs = null;

  public static void getIpAddresses ()
  {
    machineIPs = new HashMap<String, String>();
    vizIPs = new HashMap<String, String>();
    localIPs = new HashMap<String, String>();

    Database db = new Database();
    try {
      db.startTrans();

      for (Machine m: db.getMachines()) {
        String machineIP = InetAddress.getByName(m.getUrl()).toString();
        String vizIP = InetAddress.getByName(
            m.getVizUrl().split(":")[0].split("/")[0]).toString();
        if (machineIP.contains("/")) {
          machineIP = machineIP.split("/")[1];
        }
        if (vizIP.contains("/")) {
          vizIP = vizIP.split("/")[1];
        }

        machineIPs.put(machineIP, m.getName());
        vizIPs.put(vizIP, m.getName());
      }

      db.commitTrans();
    }
    catch (Exception e) {
      db.abortTrans();
      e.printStackTrace();
    }

    localIPs.put("127.0.0.1", "loopback");
    try {
      for (Enumeration<NetworkInterface> en =
               NetworkInterface.getNetworkInterfaces(); en.hasMoreElements();) {
        NetworkInterface intf = en.nextElement();

        if (!intf.getName().startsWith("eth")) {
          continue;
        }

        for (Enumeration<InetAddress> enumIpAddr =
                 intf.getInetAddresses(); enumIpAddr.hasMoreElements(); ) {
          String ip = enumIpAddr.nextElement().toString();
          if (ip.contains(":")) {
            continue;
          }
          if (ip.contains("/")) {
            ip = ip.split("/")[1];
          }
          localIPs.put(ip, intf.getName());
        }
      }
    } catch (SocketException e) {
      System.out.println(" (error retrieving network interface list)");
    }
  }

  public static boolean checkMachineAllowed(String slaveAddress)
  {
    log.debug("Testing checkMachineAllowed : " + slaveAddress);

    if (machineIPs == null) {
      getIpAddresses();
    }

    assert localIPs != null;
    if (localIPs.containsKey(slaveAddress)) {
      log.debug("Localhost is always allowed");
      return true;
    }

    assert machineIPs != null;
    if (machineIPs.containsKey(slaveAddress)) {
      log.debug(slaveAddress + " is allowed");
      return true;
    }

    log.debug(slaveAddress + " is not allowed");
    return false;
  }

  public static boolean checkVizAllowed(String vizAddress)
  {
    log.debug("Testing checkVizAllowed : " + vizAddress);

    if (vizIPs == null) {
      getIpAddresses();
    }

    assert localIPs != null;
    if (localIPs.containsKey(vizAddress)) {
      log.debug("Localhost is always allowed");
      return true;
    }

    assert vizIPs != null;
    if (vizIPs.containsKey(vizAddress)) {
      log.debug(vizAddress + " is allowed");
      return true;
    }

    log.debug(vizAddress + " is not allowed");
    return true;
  }

  //<editor-fold desc="Date format">
  public static String dateFormat (Date date)
  {
    try {
      SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
      return sdf.format(date);
    }
    catch (Exception e) {
      return "";
    }
  }
  public static String dateFormatUTC (Date date)
  {
    try {
      TimeZone.setDefault(TimeZone.getTimeZone("UTC"));
      SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
      return sdf.format(date);
    }
    catch (Exception e) {
      return "";
    }
  }
  public static Date dateFormatUTCmilli (String date)
  {
    try {
      TimeZone.setDefault(TimeZone.getTimeZone("UTC"));
      SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.S");
      return sdf.parse(date);
    }
    catch (Exception e) {
      return null;
    }
  }
  public static String dateFormatUTCmilli (Date date)
  {
    try {
      TimeZone.setDefault(TimeZone.getTimeZone("UTC"));
      SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.S");
      return sdf.format(date);
    }
    catch (Exception e) {
      return "";
    }
  }
  //</editor-fold>
}
