package com.powertac.tourney.actions;

import java.net.InetAddress;
import java.net.URL;
import java.net.UnknownHostException;
import java.util.Date;
import java.util.List;

import javax.faces.bean.ManagedBean;
import javax.faces.bean.ManagedProperty;
import javax.faces.bean.RequestScoped;
import javax.faces.context.FacesContext;
import javax.faces.model.SelectItem;

import org.apache.commons.codec.digest.DigestUtils;
import org.apache.myfaces.custom.fileupload.UploadedFile;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;
import org.springframework.stereotype.Controller;

import com.powertac.tourney.beans.Game;
import com.powertac.tourney.beans.Games;
import com.powertac.tourney.beans.Machines;
import com.powertac.tourney.beans.Scheduler;
import com.powertac.tourney.beans.Tournament;
import com.powertac.tourney.beans.Tournaments;
import com.powertac.tourney.services.StartServer;
import com.powertac.tourney.services.Upload;

@Component("actionTournament")
@Scope("request")
public class ActionTournament {

	@Autowired
	private Upload upload;

	public enum TourneyType {
		SINGLE_GAME, MULTI_GAME;
	}

	private Date startTime = new Date(); // Default to current date/time
	private Date fromTime = new Date();
	private Date toTime = new Date();
	
	private String tournamentName;
	private int maxBrokers; // -1 means inf, otherwise integer specific
	private List<String> machines;
	private List<String> locations;
	private String pomName;
	private String bootName;
	private String propertiesName;
	private UploadedFile pom;
	private UploadedFile boot;
	private UploadedFile properties;
	private TourneyType type = TourneyType.SINGLE_GAME;

	/**
	 * @return the properties
	 */
	public UploadedFile getProperties() {
		return properties;
	}

	/**
	 * @param properties
	 *            the properties to set
	 */
	public void setProperties(UploadedFile properties) {
		this.properties = properties;
	}

	/**
	 * @return the boot
	 */
	public UploadedFile getBoot() {
		return boot;
	}

	/**
	 * @param boot
	 *            the boot to set
	 */
	public void setBoot(UploadedFile boot) {
		this.boot = boot;
	}

	/**
	 * @return the pom
	 */
	public UploadedFile getPom() {
		return pom;
	}

	/**
	 * @param pom
	 *            the pom to set
	 */
	public void setPom(UploadedFile pom) {
		this.pom = pom;
	}

	/**
	 * @return the propertiesName
	 */
	public String getPropertiesName() {
		return propertiesName;
	}

	/**
	 * @param propertiesName
	 *            the propertiesName to set
	 */
	
	public void setPropertiesName(String propertiesName) {

		// Generate MD5 hash
		this.propertiesName = DigestUtils.md5Hex(propertiesName
				+ (new Date()).toString() + Math.random());
	}

	/**
	 * @return the bootName
	 */
	public String getBootName() {
		return bootName;
	}

	/**
	 * @param bootName
	 *            the bootName to set
	 */
	public void setBootName(String bootName) {
		// Generate MD5 hash
		
		this.bootName = DigestUtils.md5Hex(bootName + (new Date()).toString()
				+ Math.random());
		
	}

	public String getPomName() {
		return pomName;
	}

	
	public void setPomName(String pomName) {
		// Generate MD5 hash
		this.pomName = DigestUtils.md5Hex(pomName
				+ (new Date()).toString() + Math.random());
	}

	public List<String> getMachines() {
		return machines;
	}

	public void setMachines(List<String> machines) {
		this.machines = machines;
	}

	// Method to list the type enumeration in the jsf select Item component
	public SelectItem[] getTypes() {
		SelectItem[] items = new SelectItem[TourneyType.values().length];
		int i = 0;
		for (TourneyType t : TourneyType.values()) {
			items[i++] = new SelectItem(t, t.name());
		}
		return items;
	}

	public TourneyType getType() {
		return type;
	}

	public void setType(TourneyType type) {
		this.type = type;
	}

	public Date getStartTime() {
		return startTime;
	}

	public void setStartTime(Date startTime) {
		this.startTime = startTime;
	}

	public int getMaxBrokers() {
		return maxBrokers;
	}

	public void setMaxBrokers(int maxBrokers) {
		this.maxBrokers = maxBrokers;
	}

	public String getTournamentName() {
		return tournamentName;
	}

	public void setTournamentName(String tournamentName) {
		this.tournamentName = tournamentName;
	}

	public String createTournament() {
		// Create a tournament and insert it into the application context
		Tournament newTourney = new Tournament();
		if (type == TourneyType.SINGLE_GAME) {
		
		
			this.setPomName(pom.getName());
			upload.setUploadedFile(getPom());
			String finalFile = upload.submit(this.getPomName());
			newTourney.setPomName(finalFile);
			
			
			InetAddress thisIp = null;
			try {
				thisIp = InetAddress.getLocalHost();
				System.out.println("IP:"+thisIp.getHostAddress());
			} catch (UnknownHostException e1) {
				// TODO Auto-generated catch block
				e1.printStackTrace();
			}
			
			
			
			newTourney.setPomUrl(thisIp.getHostAddress()+":8080/TournamentScheduler/"+newTourney.getPomName());
			newTourney.setMaxBrokers(getMaxBrokers());
			newTourney.setStartTime(getStartTime());
			newTourney.setTournamentName(getTournamentName());

			// Add one game to the global context and to the tournament
			Game newGame = new Game();
			newGame.setMaxBrokers(getMaxBrokers());
			newGame.setCompetitionId(newTourney.getTournamentId());
			newGame.setCompetitionName(getTournamentName());
			newGame.setStatus("pending");
			newGame.setStartTime(getStartTime());

			Games allGames = (Games) FacesContext.getCurrentInstance()
					.getExternalContext().getApplicationMap()
					.get(Games.getKey());

			// Add game to all games and to Tournament
			allGames.addGame(newGame);
			newTourney.addGame(newGame);

			Tournaments.getAllTournaments().addTournament(newTourney);

			// Start a single game and send jenkins request to kick the server
			// at the appropriate time
			Scheduler.getScheduler().schedule(
					new StartServer(newGame, Machines.getAllMachines(),
							Tournaments.getAllTournaments()),
					newGame.getStartTime());
			try {
				// TODO:REMOVE this is only to simulate the message from the
				// server
				// Thread.sleep(6000);
				// URL test = new
				// URL("http://localhost:8080/TournamentScheduler/faces/serverInterface.jsp?action=status&status=ready&gameId="+newGame.getGameId());
				// test.openConnection().getInputStream();

			} catch (Exception e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}

		} else if (type == TourneyType.MULTI_GAME) {

		} else {

		}

		// Tournaments allTournaments = (Tournaments)
		// FacesContext.getCurrentInstance()
		// .getExternalContext().getApplicationMap().get(Tournaments.getKey());

		// allTournaments.addTournament(newTourney);

		return "Success";

	}

	public Date getFromTime() {
		return fromTime;
	}

	public void setFromTime(Date fromTime) {
		this.fromTime = fromTime;
	}

	public Date getToTime() {
		return toTime;
	}

	public void setToTime(Date toTime) {
		this.toTime = toTime;
	}

	public List<String> getLocations() {
		return locations;
	}

	public void setLocations(List<String> locations) {
		this.locations = locations;
	}

}
