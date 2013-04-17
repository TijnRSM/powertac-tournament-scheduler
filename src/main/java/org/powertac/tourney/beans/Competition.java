package org.powertac.tourney.beans;

import org.apache.log4j.Logger;
import org.hibernate.Criteria;
import org.hibernate.Session;
import org.hibernate.Transaction;
import org.powertac.tourney.constants.Constants;
import org.powertac.tourney.services.HibernateUtil;
import org.powertac.tourney.services.Utils;

import javax.faces.bean.ManagedBean;
import javax.persistence.*;
import java.util.*;

import static javax.persistence.GenerationType.IDENTITY;


@ManagedBean
@Entity
@Table(name = "competitions")
public class Competition
{
  private static Logger log = Logger.getLogger("TMLogger");

  private int competitionId;
  private String competitionName;
  private STATE state;
  private int pomId;

  private Map<Integer, Level> levelMap =
      new HashMap<Integer, Level>();

  private static enum STATE
  {
    open,         // This is the initial state. Accepts registrations.
    closed,       // No more registrations. Adjustments still allowed.
    scheduled0,   // Tournaments for level 1 created, no more editing.
    completed0,   // Tournaments for level 1 completed.
    scheduled1,
    completed1,
    scheduled2,
    completed2,
    scheduled3,
    completed3,
    complete;     // All the tournaments are done.

    public static final EnumSet<STATE> editingAllowed = EnumSet.of(
        open,
        closed,
        completed0,
        completed1,
        completed2);

    public static final EnumSet<STATE> schedulingAllowed = EnumSet.of(
        closed,
        completed0,
        completed1,
        completed2);

    public static final EnumSet<STATE> completingAllowed = EnumSet.of(
        scheduled0,
        scheduled1,
        scheduled2,
        scheduled3);
  }

  public Competition ()
  {
    state = STATE.open;
  }

  public void scheduleTournaments (Session session)
  {
    Level level = getCurrentLevel();

    log.info("Scheduling tournaments for level " + level.getLevelNr());

    for (int i=0; i < level.getNofTournaments(); i++) {
      String tournamentName = competitionName +"_"+ level.getLevelName();
      if (level.getNofTournaments() != 1) {
        tournamentName += "_" + i;
      }
      scheduleTournament(session, tournamentName, level);
    }
  }

  private void scheduleTournament (Session session, String name,
                                   Level level)
  {
    log.info("Creating tournament : " + name);

    int nofBrokers = Math.max(100, level.getNofWinners());
    if (level.getLevelNr() != 0) {
      nofBrokers = Math.min(getPreviousLevel().getNofBrokers(),
                            getPreviousLevel().getNofWinners());
    }
    int maxBrokers = (int) Math.ceil(nofBrokers / level.getNofTournaments());

    int size1 = level.getLevelNr() == 0 ? 1 : maxBrokers;
    int size2 = level.getLevelNr() == 0 ? 1 : Math.max(maxBrokers / 2, 2);
    int size3 = level.getLevelNr() == 0 ? 1 : Math.min(maxBrokers, 2);
    Date startDate = level.getStartTime();
    if (startDate.compareTo(Utils.offsetDate()) < 0) {
      startDate = Utils.offsetDate(1);
    }

    Tournament tournament = new Tournament();
    tournament.setTournamentName(name);
    tournament.setLevel(level);
    tournament.setType(Tournament.TYPE.MULTI_GAME);
    tournament.setMaxBrokers(maxBrokers);
    tournament.setMaxAgents(2);
    tournament.setSize1(size1);
    tournament.setSize2(size2);
    tournament.setSize3(size3);
    tournament.setMultiplier1(1);
    tournament.setMultiplier2(1);
    tournament.setMultiplier3(1);
    tournament.setStartTime(startDate);
    Location location = new Location();
    tournament.setDateFrom(location.getDateFrom());
    tournament.setDateTo(location.getDateTo());
    tournament.setLocations(location.getLocation() + ",");
    tournament.setPomId(pomId);
    tournament.setClosed(level.getLevelNr() != 0);
    tournament.setStateToPending();
    session.save(tournament);

    level.getTournamentMap().put(tournament.getTournamentId(), tournament);

    log.debug("Tournament created : " + tournament.getTournamentId());
  }

  private void scheduleBrokers (Session session)
  {
    Level previousLevel = getPreviousLevel();
    Level level = getCurrentLevel();

    log.info("Scheduling brokers for level " + level.getLevelNr());

    // Loop through tournaments, pick top winners
    int winnersPerTournament =
        previousLevel.getNofWinners() / level.getNofTournaments();
    List<Broker> winners = new ArrayList<Broker>();
    for (Tournament tournament: previousLevel.getTournamentMap().values()) {
      List<Broker> tournamentWinners = tournament.rankList();
      winners.addAll(tournamentWinners.subList(0,
          Math.min(winnersPerTournament, tournamentWinners.size())));
    }

    log.debug("Winners from previous level : " + winners);

    // Randomly shuffle picked brokers into tournaments via registering
    Random randomGenerator = new Random();
    int winnerCount = Math.min(previousLevel.getNofWinners(), winners.size());
    int brokersPerTournament = winnerCount / level.getNofTournaments();

    log.debug("winnerCount /  brokersPerTournament " +
        winnerCount + " / " + brokersPerTournament);

    for (Tournament tournament: level.getTournamentMap().values()) {
      log.debug("Tournament : " + tournament.getTournamentName());
      for (int i = 0; i < brokersPerTournament; i++) {
        Broker broker = winners.remove(randomGenerator.nextInt(winners.size()));
        broker.register(session, tournament.getTournamentId());
      }
    }
  }

  @Transient
  private Level getCurrentLevel ()
  {
    return levelMap.get(getCurrentLevelNr());
  }

  @Transient
  private Level getPreviousLevel ()
  {
    int currentLevelNr = getCurrentLevelNr();

    if (currentLevelNr == 0) {
      return null;
    }

    return levelMap.get(getCurrentLevelNr() - 1);
  }

  @Transient
  private Level getNextLevel ()
  {
    int currentLevelNr = getCurrentLevelNr();

    if (currentLevelNr == levelMap.size()) {
      return null;
    }

    return levelMap.get(getCurrentLevelNr() + 1);
  }

  //<editor-fold desc="State methods">
  public boolean scheduleNextLevel (Session session)
  {
    STATE oldState = state;

    if (state == STATE.closed) {
      state = STATE.scheduled0;
      // Tournaments are already scheduled during competition creation
      // Brokers are already scheduled via registering for the competition
    }
    else if (state == STATE.completed0) {
      state = STATE.scheduled1;
      scheduleTournaments(session);
      scheduleBrokers(session);
    }
    else if (state == STATE.completed1) {
      state = STATE.scheduled2;
      scheduleTournaments(session);
      scheduleBrokers(session);
    }
    else if (state == STATE.completed2) {
      state = STATE.scheduled3;
      scheduleTournaments(session);
      scheduleBrokers(session);
    }
    else {
      log.error("ScheduleNextlevel : This shouldn't happen!");
      return false;
    }
    log.info(String.format("Changing state from %s to %s", oldState, state));
    return true;
  }

  public boolean completeLevel ()
  {
    STATE oldState = state;

    if (state == STATE.scheduled0) {
      state = STATE.completed0;
    }
    else if (state == STATE.scheduled1) {
      state = STATE.completed1;
    }
    else if (state == STATE.scheduled2) {
      state = STATE.completed2;
    }
    else if (state == STATE.scheduled3) {
      state = STATE.completed3;
    }
    else {
      log.error("CloseLevel : This shouldn't happen!");
      return false;
    }

    Level nextLevel = getNextLevel();
    if (nextLevel == null ||
        nextLevel.getNofTournaments() == 0 || nextLevel.getNofWinners() == 0) {
      state = STATE.complete;
    }

    log.info(String.format("Changing state from %s to %s", oldState, state));
    return true;
  }

  public void setStateToClosed ()
  {
    this.setState(STATE.closed);
  }

  public boolean editingAllowed ()
  {
    return STATE.editingAllowed.contains(state);
  }

  public boolean closingAllowed ()
  {
    return state.equals(STATE.open);
  }

  public boolean schedulingAllowed ()
  {
    return STATE.schedulingAllowed.contains(state);
  }

  public boolean completingAllowed ()
  {
    if (!STATE.completingAllowed.contains(state)) {
      return false;
    }

    // Loop over tourneys, check all complete
    Level level = getCurrentLevel();
    for (Tournament tournament: level.getTournamentMap().values()) {
      if (!tournament.isComplete()) {
        return false;
      }
    }

    return true;
  }

  @Transient
  public boolean isOpen ()
  {
    return state.equals(STATE.open);
  }

  @Transient
  public boolean isComplete ()
  {
    return state.equals(STATE.complete);
  }

  @Transient
  public int getCurrentLevelNr () {
    if (state.compareTo(STATE.scheduled1) < 0) {
      return 0;
    }
    else if (state.compareTo(STATE.scheduled2) < 0) {
      return 1;
    }
    else if (state.compareTo(STATE.scheduled3) < 0) {
      return 2;
    }

    return 3;
  }

  public static String getStateComplete ()
  {
    return STATE.complete.toString();
  }
  //</editor-fold>

  //<editor-fold desc="Collections">
  @SuppressWarnings("unchecked")
  public static List<Competition> getNotCompleteCompetitionList ()
  {
    List<Competition> competitions = new ArrayList<Competition>();

    Session session = HibernateUtil.getSessionFactory().openSession();
    Transaction transaction = session.beginTransaction();
    try {
      competitions = (List<Competition>) session
          .createQuery(Constants.HQL.GET_COMPETITION_NOT_COMPLETE)
          .setResultTransformer(Criteria.DISTINCT_ROOT_ENTITY).list();
      transaction.commit();
    } catch (Exception e) {
      transaction.rollback();
      e.printStackTrace();
    }
    session.close();

    return competitions;
  }

  @OneToMany
  @JoinColumn(name = "competitionId")
  @MapKey(name = "levelNr")
  public Map<Integer, Level> getLevelMap ()
  {
    return levelMap;
  }
  public void setLevelMap (Map<Integer, Level> levelMap)
  {
    this.levelMap = levelMap;
  }
  //</editor-fold>

  //<editor-fold desc="Setters and Getters">
  @Id
  @GeneratedValue(strategy = IDENTITY)
  @Column(name = "competitionId", unique = true, nullable = false)
  public int getCompetitionId ()
  {
    return competitionId;
  }
  public void setCompetitionId (int competitionId)
  {
    this.competitionId = competitionId;
  }

  @Column(name = "competitionName", nullable = false)
  public String getCompetitionName ()
  {
    return competitionName;
  }
  public void setCompetitionName (String competitionName)
  {
    this.competitionName = competitionName;
  }

  @Column(name = "state", nullable = false)
  @Enumerated(EnumType.STRING)
  public STATE getState ()
  {
    return state;
  }
  public void setState (STATE state)
  {
    this.state = state;
  }

  @Column(name = "pomId", nullable = false)
  public int getPomId ()
  {
    return pomId;
  }
  public void setPomId (int pomId)
  {
    this.pomId = pomId;
  }
  //</editor-fold>
}