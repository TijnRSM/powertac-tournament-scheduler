package com.powertac.tourney.constants;

public class Constants {
	
	
	// Possible Rest Parameters for Broker Login
	public static final String REQ_PARAM_AUTH_TOKEN = "authToken";
	public static final String REQ_PARAM_JOIN = "requestJoin";
	public static final String REQ_PARAM_TYPE = "type";
	
	
	// Possible Rest Paramenters for Server Interface
	public static final String REQ_PARAM_STATUS = "status";
	public static final String REQ_PARAM_MACHINE = "machine";
	public static final String REQ_PARAM_GAME_ID = "gameId";
	// action=config - returns server.properties file
	// action=pom - returns the pom.xml file
	// action=bootstrap - returns the bootstrap.xml file
	public static final String REQ_PARAM_ACTION = "action";
	
	
	// Possible Rest Parameters for properties service
	public static final String REQ_PARAM_PROP_ID = "propId";
	
	// Possible Rest Parameters for pom service
	public static final String REQ_PARAM_POM = "location";
	
	
	// Prepared Statements for Database access
	
	/***
	 * Start db transaction
	 */
	public static final String START_TRANS = "START TRANSACTION;";
	
	/***
	 * Commit db transaction
	 */
	public static final String COMMIT_TRANS = "COMMIT;";
	
	
	/***
	 * @param userName : User name attempting to login
	 * @param password : salted md5 hash of entered password
	 */
	public static final String LOGIN_USER = "SELECT * FROM users WHERE userName=? AND password=? LIMIT 1;";
	public static final String LOGIN_SALT = "SELECT password, salt, permissionId, userId FROM users WHERE userName=?;";
	/***
	 * @param userName : User name to update account info   
	 */
	public static final String UPDATE_USER = "";
	
	/***
	 * @param userName : The desired username to use (this must be unique)
	 * @param password : The salted md5 hash of the password
	 * @param permissionId : The desired permission level 0=Admin 4=Guest (Recommend Guest)
	 */
	public static final String ADD_USER = "INSERT INTO tourney.users (userName, salt, password, permissionId) VALUES (?,?,?,?); ";
	
	
	/***
	 * Select all users
	 */
	public static final String SELECT_USERS = "SELECT * FROM tourney.users;";
	
	/***
	 * @param brokerName : The name of the Broker to use for logins
	 * @param brokerAuth : The md5 hash token to use for broker authorization
	 * @param brokerShort : The short description about the broker
	 * @param userId : The userId of the user that owns this broker
	 */
	public static final String ADD_BROKER = "INSERT INTO tourney.brokers (brokerName,brokerAuth,brokerShort, userId, numberInGame) VALUES (?,?,?,?,0);";
	
	/***
	 * Select all brokers by their userId
	 * @param userId : The userId of the brokers to return
	 */
	public static final String SELECT_BROKERS_BY_USERID = "SELECT * FROM tourney.brokers WHERE userID = ?;";
	
	/***
	 * Select broker by their brokerId
	 * @param brokerId : The brokerId of the broker you wish to return
	 */
	public static final String SELECT_BROKER_BY_BROKERID = "SELECT * FROM tourney.brokers WHERE brokerId = ? LIMIT 1;";
	
	/**
	 * Delete a broker by their brokerId
	 * @param brokerId : The brokerId of the broker you wish to delete 
	 * 
	 */
	public static final String DELETE_BROKER_BY_BROKERID = "DELETE FROM tourney.brokers WHERE brokerId = ?;";
	
	/**
	 * Update a broker by their brokerId
	 * 
	 * @param brokerName 
	 * @param brokerAuth
	 * @param brokerShort
	 * @param brokerID : The brokerId of the broker you wish to update
	 */
	public static final String UPDATE_BROKER_BY_BROKERID = "UPDATE tourney.brokers SET brokerName = ?, brokerAuth = ?, brokerShort = ? WHERE brokerId = ?;";
	
	
	
		
	/***
	 * Returns the list of all tournaments in the database of a particular status (pending, in-progress, complete) possible
	 * @param status : either "pending", "in-progress", or "complete" 
	 */
	public static final String SELECT_TOURNAMENTS = "SELECT * FROM tourney.tournaments WHERE status=?;";
	
	/***
	 * Selects a tournament from the database by tournamentId
	 * @param tournamentId : Specify the unique field to select a particular tournamnet by Id.
	 * 
	 */
	public static final String SELECT_TOURNAMENT_BYID = "SELECT * FROM tourney.tournaments WHERE tourneyId=?;";
	
	/***
	 * Adds a tournament to the database with pending status by default
	 * @param tourneyName : The name of the tournament
	 * @param startTime : The timestamp when the tournament scheduler will issue a request to start the powertac simulation server
	 * @param type : This is either "MULTI_GAME" or "SINGLE_GAME"
	 * @param pomUrl : This is the url where the pom.xml file can be located for this tournament 
	 * @param locations : This is a comma delimited list of the possible locations available in the tournament (Used for weather models)
	 * @param maxBrokers : Maximum brokers allowed in this tournament round
	 */
	
	public static final String ADD_TOURNAMENT = "INSERT INTO tourney.tournaments (tourneyName, startTime, type, pomUrl, locations, maxBrokers, status) VALUES (?,?,?,?,?,?,'pending');";
	
	/***
	 * Updates a particular tournament given the id
	 * @param status : The new status of the server either "pending", "in-progress", or "complete"
	 * @param tournamentId : The id of the tournament you wish to change
	 */
	public static final String UPDATE_TOURNAMENT_STATUS_BYID = "UPDATE tourney.tournaments SET status = ? WHERE tourneyId=?";
	
	/***
	 * Delete a particular tournament permanently, works only if all the games associated with it have been deleted
	 * @param tournamentId : The id of the tournament you wish to delete
	 */
	public static final String DELETE_TOURNAMENT_BYID = "DELETE FROM tourney.tournaments WHERE tourneyId=?;";
	
	/**
	 * Select the max tournament id from all the tournaments
	 */
	public static final String SELECT_MAX_TOURNAMENTID = "SELECT MAX(tourneyId) as maxId FROM tourney.tournaments;";
	
	/***
	 * Get the number of brokers registered for a tournament
	 * @param tourneyId : The id of the tournament you wish to query
	 */
	public static final String GET_NUMBER_REGISTERED_BYTOURNAMENTID = "SELECT COUNT(brokerId) as numRegistered FROM registration JOIN games ON registration.tourneyId = games.tourneyId WHERE games.touneyId=?;";
	
	
	/***
	 * Get a list of registered brokers for a tournament
	 * @param tourneyId : The id of the tournament you wish to query
	 */
	public static final String GET_BROKERS_BYTOURNAMENTID = "SELECT * FROM brokers JOIN registration ON registration.brokerId = brokers.brokerId WHERE registration.tourneyId=?;";
	
	/***
	 * Register for a tournament by tourneyId and brokerId
	 * @param tourneyId : The id of the tournament you wish to register for
	 * @param brokerId : The id of the broker you wish to register
	 */
	public static final String REGISTER_BROKER = "INSERT INTO tourney.registration (tourneyId,brokerId) VALUES (?,?);";	
	
	/***
	 * Unregister for a tournament (admin functionality)
	 * @param tourneyId : The id of the tournament
	 * @param brokerId : The id of the broker
	 */
	public static final String UNREGISTER_BROKER = "DELETE FROM tourney.registration WHERE tourneyId=? AND brokerId=?;";
	
	/***
	 * Check if a broker is registered for a tournament
	 * @param tourneyId : The id of the tournamnet
	 * @param brokerId : The id of the broker
	 */
	public static final String REGISTERED = "SELECT COUNT(*)=1 as registered FROM tourney.registration WHERE tourneyId=? AND brokerId=?;";
	
	/***
	 * Insert a new game into the database to be run (only ever insert games without bootstraps
	 * @param gameName : The name of the running game
	 * @param tourneyId : The id of the tournament the game is running under
	 * @param machineId : The id of the machine the game is running on
	 * @param maxBrokers : The maximum number of brokers allowed in this game
	 * @param startTime : The scheduled start time of the sim
	 */
	public static final String ADD_GAME = "INSERT INTO tourney.games (gameName, tourneyId, machineId, maxBrokers,startTime, status,jmsUrl, bootstrapUrl, visualizerUrl, propertiesUrl, location, hasBootstrap, brokers) VALUES (?,?,?,?,?,'pending','','','','','',false,'');";
	
	/***
	 * Select game by id
	 * @param gameId : The id of the game you wish to retrieve from the db
	 */
	public static final String SELECT_GAMEBYID = "SELECT * FROM tourney.games WHERE gameId=?;";
	
	/***
	 * Update bootstrap information in database, this is done directly before starting a game
	 * @param bootstrapUrl : The url where the bootstrap file can be accessed
	 * @param gameId : The id of the game you wish to update
	 */
	public static final String UPDATE_GAME_BOOTSTRAP = "UPDATE tourney.games SET status='pending', bootstrapUrl=?, hasBootstrap=true WHERE gameId=?;";
	
	/***
	 * Update jmsUrl
	 * @param jmsUrl
	 * @param gameId
	 */
	public static final String UPDATE_GAME_JMSURL = "UPDATE tourney.games SET jmsUrl=? WHERE gameId=?;";
	
	/***
	 * Update properties information in the database
	 * @param propertiesUrl : The url where the properties file can be accessed
	 * @param gameId : The id of the game you wish to update
	 */
	public static final String UPDATE_GAME_PROPERTIES = "UPDATE tourney.games SET propertiesUrl=? WHERE gameId=?;";
	
	/***
	 * Select all running and pending games
	 * 
	 */
	public static final String SELECT_GAME = "SELECT * FROM tourney.games WHERE status='pending' OR status='in-progress';";
	
	/*** 
	 * Select all games belonging to a tournament
	 * @param tourneyId : 
	 */
	public static final String SELECT_GAMES_IN_TOURNEY = "SELECT * FROM tourney.games WHERE tourneyId=?;";
	
	/***
	 * Update Game status by gameId
	 * @param status : The new status of the game either "pending", "in-progress", or "complete"
	 * @param gameId : The id of the game you wish to change
	 */
	public static final String UPDATE_GAME = "UPDATE tourney.games SET status = ? WHERE gameId = ?";
	
	/***
	 * Get max gameid of all games
	 */
	public static final String SELECT_MAX_GAMEID = "SELECT MAX(gameId) as maxId FROM tourney.games;";
	
	/***
	 * Check to see if a gameid has a bootstrap
	 * @param gameId : The id of the game to check
	 */
	public static final String GAME_READY = "SELECT hasBootstrap as ready FROM tourneny.games WHERE gameId=?;";
	
	/***
	 * Select the properties given a certain property id
	 * @param propId : The id of the properties you wish to query
	 */
	public static final String SELECT_PROPERTIES_BY_ID = "SELECT * FROM tourney.properties WHERE gameId=?;";
	
	/***
	 * Add properties to the database
	 * @param location : The location key value pair for the properties file as a string in the database
	 * @param startTime : The startTime key value pair for the properties file as a string in the database
	 * @param gameId : The gameId that this property file belongs to
	 * @param tourneyId : The tournament the game belongs to (THis is denormalization to spead up queries)
	 */
	public static final String ADD_PROPERTIES = "INSERT INTO tourney.properties (location,startTime,gameId) VALUES (?,?,?);";
	
	/***
	 * Add pom names and locations
	 * @param uploadingUser
	 * @param name
	 * @param location
	 */
	public static final String ADD_POM = "INSERT INTO tourney.poms (uploadingUser, name, location) VALUES (?,?,?);";
	
	/***
	 * Select all poms
	 */
	public static final String SELECT_POMS = "SELECT * FROM tourney.poms;";
	
	
	/***
	 * Select all machines 
	 */
	public static final String SELECT_MACHINES = "SELECT * FROM tourney.machines;";
	
	/***
	 * Change a machine's status based on id
	 * @param status : The new status to change to either "running" or "idle"
	 * @param machineId : The id of the machine to change
	 */
	public static final String UPDATE_MACHINE_STATUS_BY_ID = "UPDATE tourney.machines SET status=? WHERE machineId=?;";
	
	/***
	 * Change a machine's status based on name
	 * @param status : The new status to change to either "running" or "idle"
	 * @param machineName : The name of the machine to change
	 * 
	 */
	public static final String UPDATE_MACHINE_STATUS_BY_NAME = "UPDATE tourney.machines SET status=? WHERE machineName=?;";
	
	/***
	 * Add a machine into the database, default status is "idle"
	 * @param machineName : The shorthand name of the machine to be displayed to the users like "tac04"
	 * @param machineUrl : The fully qualified name of the machine like "tac04.cs.umn.edu"
	 */
	public static final String ADD_MACHINE = "INSERT INTO tourney.machines (machineName, machineUrl, status) VALUES (?,?,'idle');";
	
	/***
	 * Remove a machine from the database by id
	 * @param machineId : THe id of the machine you wish to remove
	 */
	public static final String REMOVE_MACHINE = "DELETE FROM tourney.machines WHERE machineId=?;";
	
	/***
	 * Select all available locations in the database
	 * 
	 */
	public static final String SELECT_LOCATIONS = "SELECT * FROM tourney.locations";
	
	/***
	 * Adds a location to the database
	 * @param location : The name of the location
	 * @param fromDate : The start date of the weather data
	 * @param toDate : The end date of the weather data
	 */
	public static final String ADD_LOCATION = "INSERT INTO tourney.locations (location, fromDate, toDate) VALUES (?,?,?);";
	
	/***
	 * Delete a location by id
	 * @param locationId : The id of the location you wish to remove
	 */
	public static final String DELETE_LOCATION = "DELETE FROM tourney.locations WHERE locationId=?;";
	
	/***
	 * Select the minimum date available for a location
	 * @param location : The location you wish to query
	 */
	public static final String SELECT_MIN_DATE = "SELECT MIN(fromDate) as minDate WHERE location=?;";
	
	/***
	 * Select the maximum date available for a location
	 * @param location : The location you wish to query
	 */
	public static final String SELECT_MAX_DATE = "SELECT MAX(toDate) as maxDate WHERE location=?;";
	
}
