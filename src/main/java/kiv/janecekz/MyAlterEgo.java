/*
 * MyAlterEgo.java
 * Copyright (C) 2015 ycdmdj@gmail.com
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation and version 3 of the License
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program. If not, see <http://www.gnu.org/licenses/>.
 */
package kiv.janecekz;

import kiv.janecekz.teamcomm.TCIamOK;
import kiv.janecekz.teamcomm.TCPlayerSeen;
import kiv.janecekz.teamcomm.TCCoverBack;
import kiv.janecekz.teamcomm.TCSupportMe;
import kiv.janecekz.teamcomm.TCFlagUpdate;
import kiv.janecekz.behavior.GetOurFlag;
import kiv.janecekz.behavior.GetItems;
import kiv.janecekz.behavior.GetHealth;
import kiv.janecekz.behavior.CloseInOnEnemy;
import kiv.janecekz.behavior.GetEnemyFlag;
import cz.cuni.amis.introspection.java.JProp;
import cz.cuni.amis.pathfinding.alg.astar.AStarResult;
import cz.cuni.amis.pathfinding.map.IPFMapView;
import cz.cuni.amis.pogamut.base.agent.navigation.IPathExecutorState;
import cz.cuni.amis.pogamut.base.agent.navigation.IPathFuture;
import cz.cuni.amis.pogamut.base.agent.navigation.PathExecutorState;
import cz.cuni.amis.pogamut.base.agent.navigation.impl.PrecomputedPathFuture;
import cz.cuni.amis.pogamut.base.communication.worldview.listener.annotation.EventListener;
import cz.cuni.amis.pogamut.base.communication.worldview.listener.annotation.ObjectClassEventListener;
import cz.cuni.amis.pogamut.base.communication.worldview.listener.annotation.ObjectClassListener;
import cz.cuni.amis.pogamut.base.communication.worldview.object.IWorldObjectEvent;
import cz.cuni.amis.pogamut.base.communication.worldview.object.event.WorldObjectUpdatedEvent;
import cz.cuni.amis.pogamut.base.utils.guice.AgentScoped;
import cz.cuni.amis.pogamut.base.utils.math.DistanceUtils;
import cz.cuni.amis.pogamut.base3d.worldview.object.ILocated;
import cz.cuni.amis.pogamut.base3d.worldview.object.Location;
import cz.cuni.amis.pogamut.base3d.worldview.object.event.WorldObjectAppearedEvent;
import cz.cuni.amis.pogamut.unreal.communication.messages.UnrealId;
import cz.cuni.amis.pogamut.ut2004.agent.module.utils.TabooSet;
import cz.cuni.amis.pogamut.ut2004.agent.navigation.UT2004PathAutoFixer;
import cz.cuni.amis.pogamut.ut2004.agent.navigation.navmesh.pathfollowing.UT2004AcceleratedPathExecutor;
import cz.cuni.amis.pogamut.ut2004.agent.navigation.stuckdetector.AccUT2004DistanceStuckDetector;
import cz.cuni.amis.pogamut.ut2004.agent.navigation.stuckdetector.AccUT2004PositionStuckDetector;
import cz.cuni.amis.pogamut.ut2004.agent.navigation.stuckdetector.AccUT2004TimeStuckDetector;
import cz.cuni.amis.pogamut.ut2004.bot.impl.UT2004Bot;
import cz.cuni.amis.pogamut.ut2004.bot.params.UT2004BotParameters;
import cz.cuni.amis.pogamut.ut2004.communication.messages.UT2004ItemType;
import cz.cuni.amis.pogamut.ut2004.communication.messages.gbcommands.Initialize;
import cz.cuni.amis.pogamut.ut2004.communication.messages.gbinfomessages.BotKilled;
import cz.cuni.amis.pogamut.ut2004.communication.messages.gbinfomessages.ConfigChange;
import cz.cuni.amis.pogamut.ut2004.communication.messages.gbinfomessages.FlagInfo;
import cz.cuni.amis.pogamut.ut2004.communication.messages.gbinfomessages.FlagInfoMessage;
import cz.cuni.amis.pogamut.ut2004.communication.messages.gbinfomessages.GameInfo;
import cz.cuni.amis.pogamut.ut2004.communication.messages.gbinfomessages.HearNoise;
import cz.cuni.amis.pogamut.ut2004.communication.messages.gbinfomessages.InitedMessage;
import cz.cuni.amis.pogamut.ut2004.communication.messages.gbinfomessages.Item;
import cz.cuni.amis.pogamut.ut2004.communication.messages.gbinfomessages.NavPoint;
import cz.cuni.amis.pogamut.ut2004.communication.messages.gbinfomessages.Player;
import cz.cuni.amis.pogamut.ut2004.communication.messages.gbinfomessages.PlayerMessage;
import cz.cuni.amis.pogamut.ut2004.communication.messages.gbinfomessages.Self;
import cz.cuni.amis.pogamut.ut2004.utils.UT2004BotRunner;
import cz.cuni.amis.pogamut.ut2004.teamcomm.bot.UT2004BotTCController;
import cz.cuni.amis.pogamut.ut2004.teamcomm.server.UT2004TCServer;
import cz.cuni.amis.utils.Cooldown;
import cz.cuni.amis.utils.Heatup;
import cz.cuni.amis.utils.IFilter;
import cz.cuni.amis.utils.collections.MyCollections;
import cz.cuni.amis.utils.exception.PogamutException;
import cz.cuni.amis.utils.flag.FlagListener;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedList;
import java.util.logging.Level;
import kiv.janecekz.behavior.SupportFriend;
import kiv.janecekz.teamcomm.FlagHuntingState;
import kiv.janecekz.teamcomm.TCHuntFlag;
import kiv.janecekz.teamcomm.TCPlayerUpdate;

/**
 * My Alter Ego bot :).
 *
 * @author Zdenek Janecek
 */
@AgentScoped
public class MyAlterEgo extends UT2004BotTCController {

    @JProp
    private String name;

    @JProp
    public int botID;

    /**
     * Used internally to maintain the information about the bot we're currently
     * hunting, i.e., should be firing at.
     */
    private Player enemy = null;

    /**
     * Stores target location.
     */
    private NavPoint pathTarget = null;
    private PrecomputedPathFuture<NavPoint> actualPath;

    TabooSet<Item> tabooItems;

    // Sniping stuffs
    private ArrayList<NavPoint> invSpots;
    private ArrayList<NavPoint> snipSpots;
    private boolean snipingSupported;

    private UT2004PathAutoFixer autoFixer;
    private GoalManager goalManager;

    /**
     * combat heatup
     */
    private final Heatup targetHU = new Heatup(5000);

    /**
     * protect flag for some time
     */
    private final Heatup coverBack = new Heatup(7000);

    /**
     * need help for some time
     */
    private final Heatup friendDefender = new Heatup(7000);

    /**
     * if we want to coverBack cooldown and do some action instead
     */
    private final Cooldown coverBackCD = new Cooldown(12000);

    /**
     * We don't want to spam teamcomm to much with our position
     */
    private final Cooldown locUpdateCd = new Cooldown(1000);

    private boolean flagHunter;

    private GetItems getItemsGoal;
    public volatile UnrealId whoNeedsMe;
    private UnrealId backupPlayer;
    private UnrealId helper;

    private Location[] flagLoc = new Location[2];
    private long[] flagTime = new long[2];

    private final int rotUnit = 32767 / 180;

    private MyAlterEgoParams getBotParams() {
        return (MyAlterEgoParams) bot.getParams();
    }

    /**
     * Initialize all necessary variables here, before the bot actually receives
     * anything from the environment.
     *
     * @param bot
     */
    @Override
    public void prepareBot(UT2004Bot bot) {
        botID = getBotParams().getOrder();
        if (botID == 0) {
            flagHunter = true;
        }
        name = "Bot " + botID;
    }

    /**
     * Here we can modify initializing command for our bot, e.g., sets its name
     * or skin.
     *
     * @return instance of {@link Initialize}
     */
    @Override
    public Initialize getInitializeCommand() {
        return new Initialize().setName("MyAlterEgo");
    }

    /**
     * Handshake with GameBots2004 is over - bot has information about the map
     * in its world view. Many agent modules are usable since this method is
     * called.
     *
     * @param gameInfo informaton about the game type
     * @param currentConfig information about configuration
     * @param init information about configuration
     */
    @Override
    public void botInitialized(GameInfo gameInfo, ConfigChange currentConfig, InitedMessage init) {
        tabooItems = new TabooSet<Item>(bot);
        
        if (navBuilder.isMapName("DM-1on1-Albatross")) {
            navBuilder.removeEdge("JumpSpot8", "PathNode88");
        }
        if (navBuilder.isMapName("CTF-BP2-Concentrate")) {
            navBuilder.removeEdge("PathNode39", "JumpSpot3");
            navBuilder.removeEdge("PathNode75", "JumpSpot2");

            navBuilder.removeEdge("PathNode74", "JumpSpot2");
            navBuilder.removeEdge("PathNode81", "JumpSpot2");
            navBuilder.removeEdge("PathNode2", "PathNode76");
            navBuilder.removeEdge("InventorySpot1", "AIMarker6");
            navBuilder.removeEdge("InventorySpot55", "PathNode44");
            navBuilder.removeEdge("PathNode0", "JumpSpot3");
            navBuilder.removeEdgesBetween("PathNode44", "JumpSpot3");
            navBuilder.removeEdge("InventorySpot9", "PathNode43");

            navBuilder.removeEdge("PathNode0", "PathNode39");
            navBuilder.removeEdge("PathNode0", "JumpSpot0");
            navBuilder.removeEdge("PathNode0", "xBlueFlagBase0");

            navBuilder.removeEdge("PathNode44", "PathNode39");
            navBuilder.removeEdge("PathNode44", "JumpSpot0");
            navBuilder.removeEdge("PathNode44", "xBlueFlagBase0");

            navBuilder.removeEdge("PathNode74", "PathNode75");
            navBuilder.removeEdge("PathNode74", "JumpSpot1");
            navBuilder.removeEdge("PathNode74", "xRedFlagBase1");

            navBuilder.removeEdge("PathNode81", "PathNode75");
            navBuilder.removeEdge("PathNode81", "JumpSpot1");
            navBuilder.removeEdge("PathNode81", "xRedFlagBase1");

            navBuilder.removeEdge("PathNode68", "JumpSpot12");
            navBuilder.removeEdge("PathNode69", "JumpSpot10");
            navBuilder.removeEdge("PathNode76", "JumpSpot11");
            navBuilder.removeEdge("PathNode44", "JumpSpot11");
            
            navBuilder.removeEdge("InventorySpot2", "AIMarker6");
            navBuilder.removeEdge("JumpSpot3", "xBlueFlagBase0");
        }
        if (navBuilder.isMapName("CTF-Citadel")) {
            tabooItems.add(items.getItem("CTF-Citadel.InventorySpot232"));
            tabooItems.add(items.getItem("CTF-Citadel.InventorySpot233"));
        }

        goalManager = new GoalManager(this);

        goalManager.addGoal(new GetEnemyFlag(this));
        goalManager.addGoal(new GetOurFlag(this));
        goalManager.addGoal(new GetHealth(this));
        goalManager.addGoal(getItemsGoal = new GetItems(this));
        goalManager.addGoal(new CloseInOnEnemy(this));
        goalManager.addGoal(new SupportFriend(this));

        UT2004AcceleratedPathExecutor accPathExecutor = ((UT2004AcceleratedPathExecutor) nmNav.getPathExecutor());
        accPathExecutor.removeAllStuckDetectors();

        accPathExecutor.addStuckDetector(new AccUT2004TimeStuckDetector(bot, 3000, 10000));
        accPathExecutor.addStuckDetector(new AccUT2004PositionStuckDetector(bot));
        accPathExecutor.addStuckDetector(new AccUT2004DistanceStuckDetector(bot));

        autoFixer = new UT2004PathAutoFixer(bot, accPathExecutor, fwMap, aStar, navBuilder);

        // listeners
        accPathExecutor.getState().addStrongListener(new FlagListener<IPathExecutorState>() {
            @Override
            public void flagChanged(IPathExecutorState changedValue) {
                pathExecutorStateChange(changedValue.getState());
            }
        });

        weaponPrefs.addGeneralPref(UT2004ItemType.MINIGUN, true);
        weaponPrefs.addGeneralPref(UT2004ItemType.LINK_GUN, false); // secondary

        weaponPrefs.newPrefsRange(100).add(UT2004ItemType.SHIELD_GUN, true);
        weaponPrefs.newPrefsRange(300).add(UT2004ItemType.FLAK_CANNON, true).add(UT2004ItemType.BIO_RIFLE, true);
        weaponPrefs.newPrefsRange(700).add(UT2004ItemType.MINIGUN, true).add(UT2004ItemType.LINK_GUN, true);
        weaponPrefs.newPrefsRange(1300).add(UT2004ItemType.LIGHTNING_GUN, true).add(UT2004ItemType.SHOCK_RIFLE, true);
    }

    /**
     * The bot is initilized in the environment - a physical representation of
     * the bot is present in the game.
     *
     * @param gameInfo informaton about the game type
     * @param config information about configuration
     * @param init information about configuration
     * @param self information about the agent
     */
    @Override
    public void botFirstSpawn(GameInfo gameInfo, ConfigChange config, InitedMessage init, Self self) {
        // Display a welcome message in the game engine
        // right in the time when the bot appears in the environment, i.e., his body has just been spawned 
        // into the UT2004 for the first time.    	
        body.getCommunication().sendGlobalTextMessage("Hello world! I am alive!");

        log.log(Level.INFO, "VISIBILITY: {0}", visibility.isInitialized());
        log.log(Level.INFO, "NAVMESH: {0}", navMeshModule.isInitialized());
        log.log(Level.INFO, "GEOMETRY: {0}", levelGeometryModule.isInitialized());

        if (navBuilder.isMapName("CTF-Citadel")) {
            snipingSupported = true;
            invSpots = new ArrayList<NavPoint>(4);
            snipSpots = new ArrayList<NavPoint>(2);
            if (info.getTeam() == 0) {
                invSpots.add(navPoints.getNavPoint("CTF-Citadel.InventorySpot184"));
                invSpots.add(navPoints.getNavPoint("CTF-Citadel.InventorySpot182"));
                invSpots.add(navPoints.getNavPoint("CTF-Citadel.InventorySpot156"));
                invSpots.add(navPoints.getNavPoint("CTF-Citadel.InventorySpot183"));

                snipSpots.add(navPoints.getNavPoint("CTF-Citadel.AIMarker24"));
                snipSpots.add(navPoints.getNavPoint("CTF-Citadel.AIMarker34"));
            } else if (info.getTeam() == 1) {
                invSpots.add(navPoints.getNavPoint("CTF-Citadel.InventorySpot217"));
                invSpots.add(navPoints.getNavPoint("CTF-Citadel.InventorySpot215"));
                invSpots.add(navPoints.getNavPoint("CTF-Citadel.InventorySpot160"));
                invSpots.add(navPoints.getNavPoint("CTF-Citadel.InventorySpot216"));

                snipSpots.add(navPoints.getNavPoint("CTF-Citadel.AIMarker25"));
                snipSpots.add(navPoints.getNavPoint("CTF-Citadel.AIMarker35"));
            }
        } else if (navBuilder.isMapName("CTF-BP2-Concentrate")) {
            snipingSupported = true;
            invSpots = new ArrayList<NavPoint>(1);
            snipSpots = new ArrayList<NavPoint>(1);

            if (info.getTeam() == 0) {
                invSpots.add(navPoints.getNavPoint("CTF-BP2-Concentrate.InventorySpot6"));
                snipSpots.add(navPoints.getNavPoint("CTF-BP2-Concentrate.AssaultPath12"));
            } else if (info.getTeam() == 1) {
                invSpots.add(navPoints.getNavPoint("CTF-BP2-Concentrate.InventorySpot10"));
                snipSpots.add(navPoints.getNavPoint("CTF-BP2-Concentrate.AssaultPath5"));
            }
        }
    }

    /**
     * This method is called only once, right before actual logic() method is
     * called for the first time.
     *
     * Similar to
     * {@link EmptyBot#botFirstSpawn(cz.cuni.amis.pogamut.ut2004.communication.messages.gbinfomessages.GameInfo, cz.cuni.amis.pogamut.ut2004.communication.messages.gbinfomessages.ConfigChange, cz.cuni.amis.pogamut.ut2004.communication.messages.gbinfomessages.InitedMessage, cz.cuni.amis.pogamut.ut2004.communication.messages.gbinfomessages.Self)}.
     */
    @Override
    public void beforeFirstLogic() {
    }

    private void sayGlobal(String msg) {
        // Simple way to send msg into the UT2004 chat
        body.getCommunication().sendGlobalTextMessage(msg);
        // And user log as well
        log.info(msg);
    }

    /**
     * Called each time the bot dies. Good for reseting all bot's state
     * dependent variables.
     *
     * @param event
     */
    @Override
    public void botKilled(BotKilled event) {
        if (helper != null) {
            tcClient.sendToBot(helper, new TCIamOK(info.getId()));
            helper = null;
        }
        if (flagHunter && players.getFriends().size() > 1) {
            Collection<Player> col = new LinkedList<Player>(players.getFriends().values());
            col.remove(players.getPlayer(info.getId()));
            UnrealId nextHunter = MyCollections.getRandom(col).getId();
            if (nextHunter != null) {
                tcClient.sendToBot(nextHunter, new TCHuntFlag());
                flagHunter = false;
            }
        }
        backupPlayer = null;
        reset();
    }

    // ENVIRONMENT CALLBACKS
    @ObjectClassEventListener(eventClass = WorldObjectAppearedEvent.class, objectClass = Player.class)
    public void playerAppeared(WorldObjectAppearedEvent<Player> event) {
        if (tcClient.isConnected()) {
            tcClient.sendToTeam(new TCPlayerSeen(this, event.getObject().getId(), event.getObject().getLocation(), event.getObject().getTeam()));
        }
    }

    @ObjectClassListener(objectClass = FlagInfo.class)
    public void flagStateChanged(IWorldObjectEvent event) {
        FlagInfoMessage fi = (FlagInfoMessage) event.getObject();
        int team = fi.getTeam();
        
        if (fi.getSimTime() < flagTime[team])
            return;

        if (event instanceof WorldObjectUpdatedEvent) { // send to team
            if (fi.getState().equalsIgnoreCase("held") && !fi.isVisible()) {
                flagLoc[team] = null;
                flagTime[team] = fi.getSimTime();
            } else if (fi.isVisible()) {
                tcClient.sendToTeam(new TCFlagUpdate(fi.getTeam(), fi.getLocation(), fi.getSimTime(), FlagHuntingState.NEW_FP));
            }
        } else if (event instanceof WorldObjectAppearedEvent) {
            flagLoc[team] = fi.getLocation();
            flagTime[team] = fi.getSimTime();
        }
    }

    @EventListener(eventClass = HearNoise.class)
    public void hearNoise(HearNoise event) {
        if (coverBack.isHot()) {
            move.turnHorizontal((int) (event.getRotation().yaw / rotUnit - 180));
        }
    }

    // TEAMCOM
    @EventListener(eventClass = TCFlagUpdate.class)
    public void flagUpdate(TCFlagUpdate seen) {
        if (seen.time < flagTime[seen.team])
            return;

        switch (seen.type) {
            case DONE:
                flagLoc[seen.team] = seen.team == info.getTeam() ? ctf.getOurBase().getLocation() : ctf.getEnemyBase().getLocation();
                flagTime[seen.team] = seen.time;
                break;
            case NEW_FP:
                flagLoc[seen.team] = seen.loc;
                flagTime[seen.team] = seen.time;
                break;
            case INVALID:
                flagLoc[seen.team] = null;
                flagTime[seen.team] = seen.time;
        }
    }

    @EventListener(eventClass = TCCoverBack.class)
    public void coverBack(TCCoverBack seen) {
        if (coverBackCD.isCool()) {
            coverBack.heat();
            coverBackCD.use();
        }
    }

    @EventListener(eventClass = TCHuntFlag.class)
    public void huntFlag(TCHuntFlag seen) {
        flagHunter = true;
    }

    @EventListener(eventClass = TCSupportMe.class)
    public void supportRequest(TCSupportMe seen) {
        whoNeedsMe = seen.player;
        friendDefender.heat();
    }

    @EventListener(eventClass = TCIamOK.class)
    public void abbadonSupport(TCIamOK seen) {
        if (friendDefender.isCool()) {
            whoNeedsMe = null;
        }
    }

    @EventListener(eventClass = TCPlayerSeen.class)
    public void playerSeen(TCPlayerSeen seen) {
        Player player = players.getPlayer(seen.playerId);

        if (player != null && player.isVisible()) {
            return;
        }

        // copy zprava PlayerMessage do playerUpdate
        PlayerMessage playerUpdate = new PlayerMessage(
                seen.playerId, null, null, false, null, false, null, seen.location,
                null, seen.team, null, false, 0, "None", "None", "None", "None", "None");

        world.notifyImmediately(playerUpdate);
    }

    @EventListener(eventClass = TCPlayerUpdate.class)
    public void playerUpdate(TCPlayerUpdate pl) {
        PlayerMessage playerUpdate = new PlayerMessage(
                pl.playerId, null, null, false, null, false, null, pl.location,
                null, info.getTeam(), null, false, 0, "None", "None", "None", "None", "None");

        world.notifyImmediately(playerUpdate);
    }

    /**
     * Path executor has changed its state (note that
     * {@link UT2004BotModuleController${symbol_pound}getPathExecutor()} is
     * internally used by
     * {@link UT2004BotModuleController${symbol_pound}getNavigation()} as
     * well!).
     *
     * @param event
     */
    protected void pathExecutorStateChange(PathExecutorState event) {
        Item item = getItemsGoal.getItem();
        if (event == PathExecutorState.STUCK) {
            if (item != null && pathTarget != null
                    && item.getLocation().equals(pathTarget.getLocation(), 10d)) {
                tabooItems.add(item, 10d);
            }
        } else if (event == PathExecutorState.TARGET_REACHED) {
            if (item != null && pathTarget != null) {
                tabooItems.add(item, 10d);
            }
        }
    }

    public boolean ammoOK() {
        boolean state = true;

        if (weaponry.getCurrentAmmo() < weaponry.getCurrentWeapon().getDescriptor().getPriMaxAmount() / 2
                || weaponry.getWeapons().size() <= 3) {
            state = false;
        }

        return state;
    }

    public void callHelp() {
        if (!flagHunter || players.getFriends().size() <= 2)
            return;

        Location myPos = info.getLocation();
        Player pl = DistanceUtils.getNearestFiltered(players.getFriends().values(), myPos, new IFilter() {
            @Override
            public boolean isAccepted(Object object) {
                return !object.equals(backupPlayer);
            }
        });
        if (pl == null)
            return;

        if (helper != null && myPos.getDistance(pl.getLocation()) < myPos.getDistance(players.getPlayer(helper).getLocation())) {
            dismissHelp();
        }
        if (tcClient.isConnected() && tcClient.isConnected(pl.getId())) {
            helper = pl.getId();
            tcClient.sendToBot(helper, new TCSupportMe(info.getId()));
        }
    }

    public void dismissHelp() {
        if (flagHunter && tcClient.isConnected() && tcClient.isConnected(helper)) {
            tcClient.sendToBot(helper, new TCIamOK(info.getId()));
            helper = null;
        }
    }

    public void setBackup() {
        if (flagHunter && players.getFriends().size() > 2) {
            Collection<Player> col = new LinkedList<Player>(players.getFriends().values());
            col.remove(players.getPlayer(info.getId()));
            col.remove(players.getPlayer(helper));
            backupPlayer = MyCollections.getRandom(col).getId();
            tcClient.sendToBot(backupPlayer, new TCCoverBack());
        }
    }

    private class CoverMapView implements IPFMapView<NavPoint> {

        @Override
        public Collection<NavPoint> getExtraNeighbors(NavPoint node, Collection<NavPoint> mapNeighbors) {
            return null;
        }

        @Override
        public int getNodeExtraCost(NavPoint node, int mapCost) {
            int cost = 0;

            Item item = node.getItemInstance();

            if (isDangerous(node)) {
                cost += 100;
            }
            if (node.isInvSpot() && items.isPickable(item) && items.isPickupSpawned(item)) {
                cost -= 10;
            }
            if (node.isAIMarker()) {
                cost -= 20;
            }

            return cost;
        }

        @Override
        public int getArcExtraCost(NavPoint nodeFrom, NavPoint nodeTo, int mapCost) {
            return 0;
        }

        @Override
        public boolean isNodeOpened(NavPoint node) {
            return true;
        }

        @Override
        public boolean isArcOpened(NavPoint nodeFrom, NavPoint nodeTo) {
            // ALL ARCS ARE OPENED
            return true;
        }
    }

    private void navigateCoverPath(NavPoint target) {
        if (pathTarget == null || !target.equals(pathTarget)) {
            pathTarget = target;
            actualPath = generateCoverPath(pathTarget);
            nmNav.stopNavigation();
            if (actualPath == null) {
                navigateStandard(target);
            } else {
                nmNav.navigate((IPathFuture) actualPath);
            }
        }
    }

    private PrecomputedPathFuture<NavPoint> generateCoverPath(NavPoint targetNav) {
        NavPoint startNav = info.getNearestNavPoint();

        AStarResult<NavPoint> result = aStar.findPath(startNav, targetNav, new CoverMapView());

        PrecomputedPathFuture<NavPoint> pathFuture = new PrecomputedPathFuture<NavPoint>(startNav, targetNav, result.getPath());

        return pathFuture;
    }

    public boolean goCovered(NavPoint target) {
        if (target == null) {
            log.info("goTo: null");
            return false;
        }

        if (coverBack.isCool()) {
            navigateCoverPath(target);
        }

        return true;
    }

    public boolean goCovered(ILocated target) {
        if (target == null) {
            log.info("goTo: null");
            return false;
        }

        if (coverBack.isCool()) {
            navigateCoverPath(nmNav.getNearestNavPoint(target));
        }

        return true;
    }

    private void navigateStandard(NavPoint target) {
        nmNav.navigate(target);
    }

    public boolean goTo(NavPoint target) {
        if (pathTarget == null || !target.equals(pathTarget)) {
            pathTarget = target;

            if (coverBack.isCool()) {
                navigateStandard(pathTarget);
            }

            return true;
        } else {
            return false;
        }
    }

    public boolean goTo(ILocated target) {
        if (pathTarget == null || !target.equals(pathTarget)) {
            pathTarget = nmNav.getNearestNavPoint(target);

            if (coverBack.isCool()) {
                navigateStandard(pathTarget);
            }

            return true;
        } else {
            return false;
        }
    }

    private final Cooldown place = new Cooldown(7000);

    public void goForSniper() {
        if (!game.getMapName().equals("CTF-BP2-Concentrate") && !game.getMapName().equals("CTF-Citadel")) {
            return;
        }

        if (!snipingSupported) {
            return;
        }

        if (place.isCool()) {
            nmNav.navigate(invSpots.get(0).getLocation());
            for (int i = 1; i < invSpots.size(); i++) {
                NavPoint p = invSpots.get(i);
                if (items.isPickable(p.getItemInstance()) && items.isPickupSpawned(p.getItemInstance())) {
                    nmNav.setContinueTo(p.getLocation());
                }
            }
            nmNav.setContinueTo(snipSpots.get(getRandom().nextInt(snipSpots.size())));
            nmNav.setFocus(null);
            place.use();
        }
    }

    public boolean holdingOrSupporting() {
        FlagInfo ourFlag = ctf.getOurFlag();

        if (ourFlag == null) {
            return false;
        }

        UnrealId holderId = ourFlag.getHolder();

        if (holderId == null) {
            return false;
        }

        if (info.getId().equals(holderId)) {
            return true;
        }

        Player holder = players.getPlayer(holderId);

        return holder.getTeam() == info.getTeam() && getInfo().getDistance(holder) < 60d;
    }

    public void updateFight() {
        if (enemy == null || enemy.isVisible()) {
            enemy = (Player) getPlayers().getNearestVisibleEnemy();
        }

        Collection<Player> every = players.getVisibleEnemies().values();
        Player best_target = null;
        double min = Double.POSITIVE_INFINITY;
        for (Player p : every) {
            if (ctf.getEnemyFlag() != null && ctf.getEnemyFlag().getHolder() != null && ctf.getEnemyFlag().getHolder().equals(p.getId())) {
                best_target = p;
                break;
            }
            if (p.getLocation().getDistance(info.getLocation()) < min) {
                best_target = p;
            }
        }

        if (enemy == null) {
            if (best_target == null) {
                return;
            } else {
                enemy = best_target;
            }
        } else {
            if (best_target == null) {
                return;
            } else if (best_target == enemy) {
                targetHU.heat();
            } else if (targetHU.isCool()) {
                enemy = best_target;
            }
        }

        shoot();
    }

    public void updateFight(Player newEnemy) {
        if (newEnemy == null || !newEnemy.isVisible()) {
            updateFight();
        }

        enemy = newEnemy;

        shoot();
    }

    public void shoot() {
        if (enemy != null && enemy.isVisible() && enemy.getLocation().getDistance(info.getLocation()) < 4000) {
            shoot.shoot(weaponPrefs, enemy);
            nmNav.setFocus(enemy);
        } else {
            shoot.stopShooting();
            nmNav.setFocus(null);
            enemy = null;
        }
    }

    public void coverYourself() {
        Player p = DistanceUtils.getNearest(players.getVisibleEnemies().values(), info.getLocation());
        if (p == null) {
            nmNav.setFocus(null);
            shoot.stopShooting();
        } else {
            nmNav.setFocus(p);
            shoot.shoot(weaponry.getWeapon(UT2004ItemType.SHIELD_GUN), false, p.getId());
        }
    }

    /**
     * Main method that controls the bot - makes decisions what to do next. It
     * is called iteratively by Pogamut engine every time a synchronous batch
     * from the environment is received. This is usually 4 times per second - it
     * is affected by visionTime variable, that can be adjusted in GameBots ini
     * file in UT2004/System folder.
     *
     */
    @Override
    public void logic() throws PogamutException {
        goalManager.executeBestGoal();

        // inform team members about our location
        if (locUpdateCd.isCool()) {
            tcClient.sendToTeamOthers(new TCPlayerUpdate(info.getId(), info.getLocation()));
            locUpdateCd.use();
        }
    }

    public NavPoint getOurFlagBase() {
        return ctf.getOurBase();
    }

    public NavPoint getEnemyFlagBase() {
        return ctf.getEnemyBase();
    }

    public FlagInfo getOurFlag() {
        return ctf.getOurFlag();
    }

    public FlagInfo getEnemyFlag() {
        return ctf.getEnemyFlag();
    }

    public Location getOurFlagLocation() {
        return flagLoc[info.getTeam()];
    }

    public Location getEnemyFlagLocation() {
        return flagLoc[info.getTeam()];
    }

    public long getSimTime() {
        return world.getSingle(Self.class).getSimTime();
    }

    public Player getEnemy() {
        return enemy;
    }

    public int supportPriority() {
        return friendDefender.isHot() ? 60 : 0;
    }

    public ILocated supportTarget() {
        if (whoNeedsMe == null || players.getPlayer(whoNeedsMe) == null) {
            return null;
        }
        return players.getPlayer(whoNeedsMe).getLocation();
    }

    public TabooSet<Item> getTaboo() {
        return tabooItems;
    }

    public void setPostfix(String s) {
        config.setName(name + ": " + s);
    }

    public boolean isDangerous(ILocated node) {
        boolean isDangerous = false;
        for (Player player : players.getEnemies().values()) {
            if (!player.isSpectator() && visibility.isVisible(node, player.getLocation())) {
                isDangerous = true;
                break;
            }
        }
        return isDangerous;
    }

    public boolean isCoveringBack() {
        return coverBack.isHot();
    }

    /**
     * Resets the state of the Hunter.
     */
    public void reset() {
        enemy = null;
        pathTarget = null;
        nmNav.stopNavigation();
        shoot.stopShooting();
        nmNav.setFocus(null);
    }

    /**
     * This method is called when the bot is started either from IDE or from
     * command line.
     *
     * @param args
     */
    public static void main(String args[]) throws PogamutException {
        int team, skill, numberOfBots, port;
        String address;

        UT2004TCServer.startTCServer();

        try {
            team            = Integer.parseInt(args[1]);
            skill           = Integer.parseInt(args[2]);
            numberOfBots    = Integer.parseInt(args[3]);
            address              = args[4];
            port            = 3000;
        } catch (Exception e) {
            System.out.println("Wrong arguments.");
            return;
        }

        MyAlterEgoParams[] teamParams = new MyAlterEgoParams[numberOfBots];
        for (int i = 0; i < numberOfBots / 2; i++) {
            teamParams[i] = new MyAlterEgoParams().setTeam(team).setOrder(i).setSkillLevel(skill);
        }

        for (int i = numberOfBots / 2; i < numberOfBots; i++) {
            teamParams[i] = new MyAlterEgoParams().setTeam(1-team).setOrder(i).setSkillLevel(skill);
        }

        new UT2004BotRunner<UT2004Bot, UT2004BotParameters>(MyAlterEgo.class, "EgoTeam", address, port).setMain(true).startAgents(teamParams);
    }
}
