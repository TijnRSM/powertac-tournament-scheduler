package org.powertac.tourney.actions;

import org.apache.log4j.Logger;
import org.apache.myfaces.custom.fileupload.UploadedFile;
import org.hibernate.Session;
import org.hibernate.Transaction;
import org.hibernate.exception.ConstraintViolationException;
import org.powertac.tourney.beans.*;
import org.powertac.tourney.services.*;

import javax.faces.application.FacesMessage;
import javax.faces.bean.ManagedBean;
import javax.faces.bean.RequestScoped;
import javax.faces.context.FacesContext;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;

@ManagedBean
@RequestScoped
public class ActionAdmin
{
  private static Logger log = Logger.getLogger("TMLogger");

  private String sortColumnPom = null;
  private boolean sortAscendingPom = true;
  private String sortColumnMachine = null;
  private boolean sortAscendingMachine = true;
  private String sortColumnUsers = null;
  private boolean sortAscendingUsers = true;

  private int locationId = -1;
  private String locationName = "";
  private int locationTimezone = 0;
  private Date locationStartTime = null;
  private Date locationEndTime = null;

  private int machineId = -1;
  private String machineName = "";
  private String machineUrl = "";
  private String machineViz = "";

  private Upload upload = new Upload();
  private UploadedFile uploadedPom;
  private String pomName;

  private Integer selectedTournament;

  private TournamentProperties properties = TournamentProperties.getProperties();

  private List<Tournament> availableTournaments = new ArrayList<Tournament>();

  public ActionAdmin ()
  {
    loadData();
  }

  //<editor-fold desc="Header stuff">
  @SuppressWarnings("unchecked")
  private void loadData ()
  {
    for (Tournament tournament : Tournament.getNotCompleteTournamentList()) {
      if (tournament.isMulti()) {
        availableTournaments.add(tournament);
      }
    }

    Collections.sort(availableTournaments, new Utils.AlphanumComparator());
  }

  public void restartWatchDog ()
  {
    log.info("Restarting WatchDog");
    Scheduler scheduler = Scheduler.getScheduler();
    if (!scheduler.restartWatchDog()) {
      log.info("Not restarting WatchDog, to close");
    }
  }

  public void loadTournament ()
  {
    log.info("Loading Tournament " + selectedTournament);

    Scheduler scheduler = Scheduler.getScheduler();
    scheduler.loadTournament(selectedTournament);
  }

  public void unloadTournament ()
  {
    log.info("Unloading Tournament");

    Scheduler scheduler = Scheduler.getScheduler();
    scheduler.unloadTournament();
  }

  public List<Tournament> getAvailableTournaments ()
  {
    return availableTournaments;
  }

  public List<String> getConfigErrors ()
  {
    return properties.getErrorMessages();
  }

  public void removeMessage (String message)
  {
    properties.removeErrorMessage(message);
  }
  //</editor-fold>

  //<editor-fold desc="Location stuff">
  public List<Location> getLocationList ()
  {
    return Location.getLocationList();
  }

  public void editLocation (Location l)
  {
    locationId = l.getLocationId();
    locationName = l.getLocation();
    locationTimezone = l.getTimezone();
    locationStartTime = l.getDateFrom();
    locationEndTime = l.getDateTo();
  }

  public void saveLocation ()
  {
    if (locationName.isEmpty() || locationStartTime == null || locationEndTime == null) {
      String msg = "Error: location not saved, some fields were empty!";
      FacesMessage fm = new FacesMessage(FacesMessage.SEVERITY_ERROR, msg, null);
      FacesContext.getCurrentInstance().addMessage("saveLocation", fm);
      return;
    }

    if (locationId == -1) {
      addLocation();
    } else {
      editLocation();
    }
  }

  public void addLocation ()
  {
    Location location = new Location();
    location.setLocation(locationName);
    location.setDateFrom(locationStartTime);
    location.setDateTo(locationEndTime);
    location.setTimezone(locationTimezone);

    Session session = HibernateUtil.getSessionFactory().openSession();
    Transaction transaction = session.beginTransaction();
    try {
      session.save(location);
      transaction.commit();
    } catch (Exception e) {
      transaction.rollback();
      e.printStackTrace();
    }
    if (transaction.wasCommitted()) {
      log.info("Added new location " + locationName);
      resetLocationData();
    }

    session.close();
  }

  public void editLocation ()
  {
    Session session = HibernateUtil.getSessionFactory().openSession();
    Transaction transaction = session.beginTransaction();
    try {
      Location location = (Location) session.get(Location.class, locationId);
      location.setLocation(locationName);
      location.setDateFrom(locationStartTime);
      location.setDateTo(locationEndTime);
      location.setTimezone(locationTimezone);

      session.update(location);
      transaction.commit();
    } catch (Exception e) {
      transaction.rollback();
      e.printStackTrace();

      String msg = "Error : location not edited " + e.getMessage();
      FacesMessage fm = new FacesMessage(FacesMessage.SEVERITY_ERROR, msg, null);
      FacesContext.getCurrentInstance().addMessage("saveLocation", fm);
    }
    if (transaction.wasCommitted()) {
      log.info("Edited location " + locationName);
      resetLocationData();
    }

    session.close();
  }

  public void deleteLocation (Location location)
  {
    Session session = HibernateUtil.getSessionFactory().openSession();
    Transaction transaction = session.beginTransaction();
    try {
      session.delete(location);
      transaction.commit();
    } catch (Exception e) {
      transaction.rollback();
      e.printStackTrace();
    }
    session.close();
    resetLocationData();
  }

  private void resetLocationData ()
  {
    locationId = -1;
    locationName = "";
    locationTimezone = 0;
    locationStartTime = null;
    locationEndTime = null;
  }
  //</editor-fold>

  //<editor-fold desc="Pom stuff">
  public List<Pom> getPomList ()
  {
    return Pom.getPomList();
  }

  public void submitPom ()
  {
    if (pomName.isEmpty()) {
      // Show succes message.
      String msg = "Error: You need to fill in the pom name";
      FacesMessage fm = new FacesMessage(FacesMessage.SEVERITY_ERROR, msg, null);
      FacesContext.getCurrentInstance().addMessage("pomUploadForm", fm);
      return;
    }

    if (uploadedPom == null) {
      String msg = "Error: You need to choose a pom file";
      FacesMessage fm = new FacesMessage(FacesMessage.SEVERITY_ERROR, msg, null);
      FacesContext.getCurrentInstance().addMessage("pomUploadForm", fm);
      return;
    }

    User currentUser = User.getCurrentUser();
    Pom pom = new Pom();
    pom.setPomName(getPomName());
    pom.setUser(currentUser);

    Session session = HibernateUtil.getSessionFactory().openSession();
    session.beginTransaction();
    try {
      session.save(pom);
    } catch (ConstraintViolationException e) {
      session.getTransaction().rollback();
      String msg = "Error: This name is already used";
      FacesMessage fm = new FacesMessage(FacesMessage.SEVERITY_ERROR, msg, null);
      FacesContext.getCurrentInstance().addMessage("pomUploadForm", fm);
      return;
    }

    upload.setUploadedFile(uploadedPom);
    upload.setUploadLocation(properties.getProperty("pomLocation"));
    boolean pomStored = upload.submit("pom." + pom.getPomId() + ".xml");

    if (pomStored) {
      session.getTransaction().commit();
    } else {
      session.getTransaction().rollback();
    }
    session.close();
  }
  //</editor-fold>

  //<editor-fold desc="Machine stuff">
  public List<Machine> getMachineList ()
  {
    return Machine.getMachineList();
  }

  public void toggleAvailable (Machine machine)
  {
    Session session = HibernateUtil.getSessionFactory().openSession();
    Transaction transaction = session.beginTransaction();
    try {
      machine.setAvailable(!machine.isAvailable());
      session.update(machine);
      transaction.commit();
    } catch (Exception e) {
      transaction.rollback();
      e.printStackTrace();
    }
    session.close();
  }

  public void toggleState (Machine machine)
  {
    Session session = HibernateUtil.getSessionFactory().openSession();
    Transaction transaction = session.beginTransaction();
    try {
      if (machine.isInProgress()) {
        machine.setState(Machine.STATE.idle);
      } else {
        machine.setState(Machine.STATE.running);
      }
      session.update(machine);
      transaction.commit();
    } catch (Exception e) {
      transaction.rollback();
      e.printStackTrace();
    }
    session.close();
  }

  public void editMachine (Machine m)
  {
    machineId = m.getMachineId();
    machineName = m.getMachineName();
    machineUrl = m.getMachineUrl();
    machineViz = m.getVizUrl();
  }

  public void saveMachine ()
  {
    machineUrl = machineUrl.replace("https://", "").replace("http://", "");
    machineViz = machineViz.replace("https://", "").replace("http://", "");

    if (machineName.isEmpty() || machineUrl.isEmpty() || machineViz.isEmpty()) {
      String msg = "Error: machine not saved, some fields were empty!";
      FacesMessage fm = new FacesMessage(FacesMessage.SEVERITY_ERROR, msg, null);
      FacesContext.getCurrentInstance().addMessage("saveMachine", fm);
      return;
    }

    // Make sure we get a new list of IPs
    MemStore.machineIPs = null;
    MemStore.vizIPs = null;

    // It's a new machine
    if (machineId == -1) {
      addMachine();
    } else {
      editMachine();
    }
  }

  public void addMachine ()
  {
    Machine machine = new Machine();
    machine.setMachineName(machineName);
    machine.setMachineUrl(machineUrl);
    machine.setVizUrl(machineViz);
    machine.setState(Machine.STATE.idle);
    machine.setAvailable(false);

    Session session = HibernateUtil.getSessionFactory().openSession();
    Transaction transaction = session.beginTransaction();
    try {
      session.save(machine);
      transaction.commit();
    } catch (Exception e) {
      transaction.rollback();
      e.printStackTrace();

      String msg = "Error : machine not added " + e.getMessage();
      FacesMessage fm = new FacesMessage(FacesMessage.SEVERITY_ERROR, msg, null);
      FacesContext.getCurrentInstance().addMessage("saveMachine", fm);
    }
    if (transaction.wasCommitted()) {
      log.info("Added new machine " + machineName);
      resetMachineData();
    }

    session.close();
  }

  public void editMachine ()
  {
    Session session = HibernateUtil.getSessionFactory().openSession();
    Transaction transaction = session.beginTransaction();
    try {
      Machine machine = (Machine) session.get(Machine.class, machineId);
      machine.setMachineName(machineName);
      machine.setMachineUrl(machineUrl);
      machine.setVizUrl(machineViz);

      session.update(machine);
      transaction.commit();
    } catch (Exception e) {
      transaction.rollback();
      e.printStackTrace();

      String msg = "Error : machine not edited " + e.getMessage();
      FacesMessage fm = new FacesMessage(FacesMessage.SEVERITY_ERROR, msg, null);
      FacesContext.getCurrentInstance().addMessage("saveMachine", fm);
    }
    if (transaction.wasCommitted()) {
      log.info("Edited machine " + machineName);
      resetMachineData();
    }

    session.close();
  }

  public void deleteMachine (Machine machine)
  {
    Session session = HibernateUtil.getSessionFactory().openSession();
    Transaction transaction = session.beginTransaction();
    try {
      log.info("Deleting machine " + machine.getMachineId());
      session.delete(machine);
      transaction.commit();
    } catch (Exception e) {
      transaction.rollback();
      e.printStackTrace();

      String msg = "Error : machine not deleted " + e.getMessage();
      FacesMessage fm = new FacesMessage(FacesMessage.SEVERITY_ERROR, msg, null);
      FacesContext.getCurrentInstance().addMessage("saveMachine", fm);
    }
    session.close();
    resetMachineData();
  }

  private void resetMachineData ()
  {
    machineId = -1;
    machineViz = "";
    machineName = "";
    machineUrl = "";
  }
  //</editor-fold>

  public List<User> getUserList ()
  {
    return User.getUserList();
  }

  //<editor-fold desc="Setters and Getters">
  public int getLocationId ()
  {
    return locationId;
  }

  public void setLocationId (int locationId)
  {
    this.locationId = locationId;
  }

  public String getLocationName ()
  {
    return locationName;
  }

  public void setLocationName (String locationName)
  {
    this.locationName = locationName;
  }

  public int getLocationTimezone ()
  {
    return locationTimezone;
  }

  public void setLocationTimezone (int locationTimezone)
  {
    this.locationTimezone = locationTimezone;
  }

  public Date getLocationStartTime ()
  {
    return locationStartTime;
  }

  public void setLocationStartTime (Date locationStartTime)
  {
    this.locationStartTime = locationStartTime;
  }

  public Date getLocationEndTime ()
  {
    return locationEndTime;
  }

  public void setLocationEndTime (Date locationEndTime)
  {
    this.locationEndTime = locationEndTime;
  }

  public String getPomName ()
  {
    return pomName;
  }

  public void setPomName (String pomName)
  {
    this.pomName = pomName.trim();
  }

  public UploadedFile getUploadedPom ()
  {
    return uploadedPom;
  }

  public void setUploadedPom (UploadedFile uploadedPom)
  {
    this.uploadedPom = uploadedPom;
  }

  public int getMachineId ()
  {
    return machineId;
  }

  public void setMachineId (int machineId)
  {
    this.machineId = machineId;
  }

  public String getMachineName ()
  {
    return machineName;
  }

  public void setMachineName (String machineName)
  {
    this.machineName = machineName;
  }

  public String getMachineUrl ()
  {
    return machineUrl;
  }

  public void setMachineUrl (String machineUrl)
  {
    this.machineUrl = machineUrl;
  }

  public String getMachineViz ()
  {
    return machineViz;
  }

  public void setMachineViz (String machineViz)
  {
    this.machineViz = machineViz;
  }
  //</editor-fold>

  //<editor-fold desc="Sorting setters and getters">
  public boolean isSortAscendingPom ()
  {
    return sortAscendingPom;
  }

  public void setSortAscendingPom (boolean sortAscendingPom)
  {
    this.sortAscendingPom = sortAscendingPom;
  }

  public String getSortColumnPom ()
  {
    return sortColumnPom;
  }

  public void setSortColumnPom (String sortColumnPom)
  {
    this.sortColumnPom = sortColumnPom;
  }

  public String getSortColumnMachine ()
  {
    return sortColumnMachine;
  }

  public void setSortColumnMachine (String sortColumnMachine)
  {
    this.sortColumnMachine = sortColumnMachine;
  }

  public boolean isSortAscendingMachine ()
  {
    return sortAscendingMachine;
  }

  public void setSortAscendingMachine (boolean sortAscendingMachine)
  {
    this.sortAscendingMachine = sortAscendingMachine;
  }

  public String getSortColumnUsers ()
  {
    return sortColumnUsers;
  }

  public void setSortColumnUsers (String sortColumnUsers)
  {
    this.sortColumnUsers = sortColumnUsers;
  }

  public boolean isSortAscendingUsers ()
  {
    return sortAscendingUsers;
  }

  public void setSortAscendingUsers (boolean sortAscendingUsers)
  {
    this.sortAscendingUsers = sortAscendingUsers;
  }

  public Integer getSelectedTournament ()
  {
    return selectedTournament;
  }

  public void setSelectedTournament (Integer selectedTournament)
  {
    this.selectedTournament = selectedTournament;
  }
  //</editor-fold>
}
