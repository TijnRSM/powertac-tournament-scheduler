package com.powertac.tourney.beans;

import java.util.HashMap;

import javax.faces.context.FacesContext;

public class Games {
	HashMap<Integer,Game> games = null;
	
	public static final String key = "games";
	
	private static int gameId = 0;
	
	private String sortColumn = null;
	private boolean sortAscending = true;
	private int rowCount = 5;
	
	
	public Games(){
		this.games = new HashMap<Integer,Game>();
	}
	
	public static String getKey(){
		return key;
	}
	public static Games getAllGames(){
		return (Games) FacesContext.getCurrentInstance()
				.getExternalContext().getApplicationMap().get(Games.getKey());
	}
	
	public HashMap<Integer,Game> getGames(){
		return this.games;
	}
	
	public static int getNewGameId(){
		return gameId++;
	}
	
	public void addGame(Game game){
		games.put(game.getGameId(), game);
	}
	
	public Game[] getGameList(){
		if(games.size() == 0){
			return null;
		}else{
			Game[] newGame = new Game[games.size()];
			int i = 0;
			for(Game t : games.values()){
				newGame[i] = t;
				i++;
			}
			
			return newGame;
		}
	}

	public String getSortColumn() {
		return sortColumn;
	}

	public void setSortColumn(String sortColumn) {
		this.sortColumn = sortColumn;
	}

	public boolean isSortAscending() {
		return sortAscending;
	}

	public void setSortAscending(boolean sortAscending) {
		this.sortAscending = sortAscending;
	}

	public int getRowCount() {
		return rowCount;
	}

	public void setRowCount(int rowCount) {
		this.rowCount = rowCount;
	}
	
	

}
