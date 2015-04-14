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
import cz.cuni.amis.pogamut.base.utils.guice.AgentScoped;
import cz.cuni.amis.pogamut.base.utils.math.DistanceUtils;
import cz.cuni.amis.pogamut.base.utils.math.DistanceUtils.IGetDistance;
import cz.cuni.amis.pogamut.base3d.worldview.object.ILocated;
import cz.cuni.amis.pogamut.base3d.worldview.object.Location;
import cz.cuni.amis.pogamut.base3d.worldview.object.event.WorldObjectAppearedEvent;
import cz.cuni.amis.pogamut.unreal.communication.messages.UnrealId;
import cz.cuni.amis.pogamut.ut2004.agent.module.utils.TabooSet;
import cz.cuni.amis.pogamut.ut2004.agent.navigation.floydwarshall.FloydWarshallMap;
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
import cz.cuni.amis.pogamut.ut2004.communication.messages.gbinfomessages.GameInfo;
import cz.cuni.amis.pogamut.ut2004.communication.messages.gbinfomessages.IncomingProjectile;
import cz.cuni.amis.pogamut.ut2004.communication.messages.gbinfomessages.InitedMessage;
import cz.cuni.amis.pogamut.ut2004.communication.messages.gbinfomessages.Item;
import cz.cuni.amis.pogamut.ut2004.communication.messages.gbinfomessages.NavPoint;
import cz.cuni.amis.pogamut.ut2004.communication.messages.gbinfomessages.Player;
import cz.cuni.amis.pogamut.ut2004.communication.messages.gbinfomessages.PlayerMessage;
import cz.cuni.amis.pogamut.ut2004.communication.messages.gbinfomessages.Self;
import cz.cuni.amis.pogamut.ut2004.utils.UT2004BotRunner;
import cz.cuni.amis.pogamut.ut2004.teamcomm.bot.UT2004BotTCController;
import cz.cuni.amis.pogamut.ut2004.teamcomm.server.UT2004TCServer;
import cz.cuni.amis.utils.Heatup;
import cz.cuni.amis.utils.collections.MyCollections;
import cz.cuni.amis.utils.exception.PogamutException;
import cz.cuni.amis.utils.flag.FlagListener;
import java.util.ArrayList;
import java.util.Collection;
import java.util.logging.Level;
import kiv.janecekz.behavior.SupportFriend;

/**
 * My Alter Ego bot :).
 * 
 * @author Zdenek Janecek
 */
@AgentScoped
public class MyAlterEgo extends UT2004BotTCController {
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
    TabooSet<NavPoint> tabooNavPoints;

    private boolean sniping = false;
    private ArrayList<NavPoint> invSpots;
    private ArrayList<NavPoint> snipSpots;
    
//    private UT2004PathAutoFixer autoFixer;
    private GoalManager goalManager;
    private Heatup targetHU = new Heatup(5000);
    private Heatup coverBack = new Heatup(20000); // protect flag for some time
    private GetItems getItemsGoal;
    public volatile UnrealId whoNeedsMe;
    public volatile UnrealId helper;
    private String name;

    private IGetDistance<ILocated> getDistance = new DistanceUtils.IGetDistance<ILocated>() {
        @Override
        public double getDistance(ILocated object, ILocated target) {
            return fwMap.getDistance(navPoints.getNearestNavPoint(object), navPoints.getNearestNavPoint(target));
        }
    };

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
        name = "Bot "+botID;
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
     * @param config information about configuration
     * @param init information about configuration
     */
    @Override
    public void botInitialized(GameInfo gameInfo, ConfigChange currentConfig, InitedMessage init) {
        if (navBuilder.isMapName("DM-1on1-Albatross")) {
            navBuilder.removeEdge("JumpSpot8", "PathNode88");
        }

        tabooItems = new TabooSet<Item>(bot);
        tabooNavPoints = new TabooSet<NavPoint>(bot);

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

        // listeners
        nmNav.getPathExecutor().getState().addStrongListener(new FlagListener<IPathExecutorState>() {
            @Override
            public void flagChanged(IPathExecutorState changedValue) {
                pathExecutorStateChange(changedValue.getState());
            }
        });

        weaponPrefs.addGeneralPref(UT2004ItemType.MINIGUN, true);
        weaponPrefs.addGeneralPref(UT2004ItemType.LINK_GUN, false); // secondary

        weaponPrefs.newPrefsRange(100).add(UT2004ItemType.SHIELD_GUN, true);
        weaponPrefs.newPrefsRange(300).add(UT2004ItemType.FLAK_CANNON, true).add(UT2004ItemType.LINK_GUN, true);
        weaponPrefs.newPrefsRange(1000).add(UT2004ItemType.MINIGUN, true);
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
            throw new UnsupportedOperationException("NeznĂˇmĂˇ mapa");
        } else throw new UnsupportedOperationException("NeznĂˇmĂˇ mapa");
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
        reset();
    }

    // ENVIRONMENT CALLBACKS
    @ObjectClassEventListener(eventClass = WorldObjectAppearedEvent.class, objectClass = Player.class)
    public void playerAppeared(WorldObjectAppearedEvent<Player> event) {
        if (tcClient.isConnected())
            tcClient.sendToTeam(new TCPlayerSeen(this, event.getObject().getId(), event.getObject().getLocation()));
    }

/*
    @ObjectClassEventListener(eventClass = WorldObjectAppearedEvent.class, objectClass = Item.class)
    public void itemAppeared(WorldObjectAppearedEvent<Item> event) {
        tcClient.sendToTeam(new TCItemSeen(this, event.getObject().getNavPointId(), event.getObject().getNavPoint().isItemSpawned(), event.getObject()));
    }
*/
    // TEAMCOM
    @EventListener(eventClass = TCCoverBack.class)
    public void coverBack(TCCoverBack seen) {
        if (coverBack.isCool())
            coverBack.heat();
    }
    
    @EventListener(eventClass = TCSupportMe.class)
    public void supportRequest(TCSupportMe seen) {
        whoNeedsMe = seen.player;
    }

    @EventListener(eventClass = TCIamOK.class)
    public void abbadonSupport(TCIamOK seen) {
        if (whoNeedsMe != null && whoNeedsMe.equals(seen.player)) {
            whoNeedsMe = null;
        }
    }

    @EventListener(eventClass = TCPlayerSeen.class)
    public void playerSeen(TCPlayerSeen seen) {
        Player player = players.getPlayer(seen.playerId);
        if (player == null) {
            return;
        }
        if (player.isVisible()) {
            return;
        }

        // copy zprava PlayerMessage do playerUpdate
        PlayerMessage playerUpdate = new PlayerMessage(
                seen.playerId,
                player.getJmx(),
                player.getName(),
                player.isSpectator(),
                player.getAction(),
                false,
                player.getRotation(),
                seen.location,
                player.getVelocity(),
                player.getTeam(),
                player.getWeapon(),
                player.isCrouched(),
                player.getFiring(),
                player.getEmotLeft(),
                player.getEmotCenter(),
                player.getEmotRight(),
                player.getBubble(),
                player.getAnim());

        world.notifyImmediately(playerUpdate);
    }

/*
    @EventListener(eventClass = TCItemSeen.class)
    public void itemSeen(TCItemSeen seen) {
        log.log(Level.INFO, "GOT message about item "+seen.navPointId);

        NavPoint navPoint = navPoints.getNavPoint(seen.navPointId);

        // copy zprava PlayerMessage do playerUpdate
        NavPointMessage navPointUpdate = new NavPointMessage(
                seen.navPointId,
                navPoint.getLocation(),
                navPoint.getVelocity(),
                navPoint.isVisible(),
                seen.object.getId(),
                seen.object.getType(),
                seen.spawned,
                navPoint.isDoorOpened(),
                navPoint.getMover(),
                navPoint.getLiftOffset(),
                navPoint.isLiftJumpExit(),
                navPoint.isNoDoubleJump(),
                navPoint.isInvSpot(),
                navPoint.isPlayerStart(),
                navPoint.getTeamNumber(),
                navPoint.isDomPoint(),
                navPoint.getDomPointController(),
                navPoint.isDoor(),
                navPoint.isLiftCenter(),
                navPoint.isLiftExit(),
                navPoint.isAIMarker(),
                navPoint.isJumpSpot(),
                navPoint.isJumpPad(),
                navPoint.isJumpDest(),
                navPoint.isTeleporter(),
                navPoint.getRotation(),
                navPoint.isRoamingSpot(),
                navPoint.isSnipingSpot(),
                navPoint.getItemInstance(),
                navPoint.getOutgoingEdges(),
                navPoint.getIncomingEdges(),
                navPoint.getPreferedWeapon()
        );

        world.notifyImmediately(navPointUpdate);
    }
*/

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
        switch (event) {
            case PATH_COMPUTATION_FAILED:
                // if path computation fails to whatever reason, just try another navpoint
                // taboo bad navpoint for 3 minutes
                tabooNavPoints.add(pathTarget, 180);
                break;

            case TARGET_REACHED:
                tabooNavPoints.add(pathTarget, 20);
                break;

            case STUCK:
                // the bot has stuck! ... target nav point is unavailable currently
                tabooNavPoints.add(pathTarget, 60);

                Item item = getItemsGoal.getItem();
                if (item != null && pathTarget != null
                        && item.getLocation().equals(pathTarget.getLocation(), 10d)) {
                    tabooItems.add(item, 10d);
                }
                reset();
                break;

            case STOPPED:
                // path execution has stopped
                pathTarget = null;
                break;
            case PATH_COMPUTED:
                break;
        }
    }

    public boolean ammoOK() {
        boolean state = true;

        if (weaponry.getCurrentAmmo() < weaponry.getCurrentWeapon().getDescriptor().getPriMaxAmount() / 2 ||
            weaponry.getWeapons().size() <= 3) {
            state = false;
        }

        return state;
    }

    public void callHelp() {
        Location myPos = info.getLocation();
        Player pl = DistanceUtils.getNearest(players.getFriends().values(), myPos);
        if (pl == null || (whoNeedsMe != null && whoNeedsMe.equals(pl.getId())) || pl.equals(ctf.getEnemyFlag().getHolder()))
            return;
        if (helper != null && myPos.getDistance(pl.getLocation()) < myPos.getDistance(players.getPlayer(helper).getLocation()))
            dismissHelp();
        if (tcClient.isConnected() && tcClient.isConnected(pl.getId())) {
            helper = pl.getId();
            tcClient.sendToBot(helper, new TCSupportMe(info.getId()));
        }
    }

    public void dismissHelp() {
        if (helper != null && tcClient.isConnected() && tcClient.isConnected(helper)) {
            tcClient.sendToBot(helper, new TCIamOK(info.getId()));
            helper = null;
        }
    }

    public void setBackup() {
        if (players.getFriends().size() <= 2)
            return;
        Player pl = MyCollections.getRandom(players.getFriends().values());
        tcClient.sendToBot(pl.getId(), new TCCoverBack());
    }

    private class CoverMapView implements IPFMapView<NavPoint> {

        @Override
        public Collection<NavPoint> getExtraNeighbors(NavPoint node, Collection<NavPoint> mapNeighbors) {
            return null;
        }

        @Override
        public int getNodeExtraCost(NavPoint node, int mapCost) {
            if (isDangerous(node))
                return 100;
            else
                return 0;
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

    private void navigateCoverPath(NavPoint target) { // TODO: update when new enemy seen
        if (pathTarget == null || !target.equals(pathTarget)) {
            pathTarget = target;
            actualPath = generateCoverPath(target);
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

        if (!sniping) navigateCoverPath(target);

        return true;
    }

    public boolean goCovered(ILocated target) {
        if (target == null) {
            log.info("goTo: null");
            return false;
        }

        if (!sniping) navigateCoverPath(nmNav.getNearestNavPoint(target));

        return true;
    }

    private void navigateStandard(NavPoint target) {
        nmNav.navigate(target);
    }

    public boolean goTo(NavPoint target) {
        if (target == null) {
            return false;
        }

        if (!sniping) navigateStandard(target);

        return true;
    }

    public boolean goTo(ILocated target) {
        if (target == null) {
            return false;
        }

        if (!sniping) navigateStandard(nmNav.getNearestNavPoint(target));

        return true;
    }

    private Heatup placeHU = new Heatup(7000);
    
    public void goForSniper() {
        sniping = true;

        if (placeHU.isCool()) {
            nmNav.navigate(snipSpots.get(getRandom().nextInt(snipSpots.size())));
            placeHU.heat();
        } else {
            nmNav.navigate(invSpots.get(0).getLocation());
            for (NavPoint p : invSpots) {
                if (items.isPickable(p.getItemInstance()) && items.isPickupSpawned(p.getItemInstance())) {
                    if (nmNav.isNavigating())
                        nmNav.setContinueTo(p.getLocation());
                    else
                        nmNav.navigate(p.getLocation());
                }
            }
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
            newEnemy = (Player) getPlayers().getNearestVisibleEnemy();
        }

        if (newEnemy == null) {
            return;
        } else {
            enemy = newEnemy;
        }

        shoot();
    }

    public void shoot() {
        if (enemy != null && enemy.isVisible() && enemy.getLocation().getDistance(info.getLocation()) < 2000) {
            shoot.shoot(weaponPrefs, enemy);
            nmNav.setFocus(enemy);
        } else {
            shoot.stopShooting();
            nmNav.setFocus(null);
            enemy = null;
        }
    }

    public void coverYourself() { // TODO: protect from the agressive player
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
        // we are more important ;)
        if (ctf.getOurFlag() != null && ctf.getOurFlag().getHolder() != null && ctf.getOurFlag().getHolder().equals(info.getId()))
            whoNeedsMe = null;
        
        goalManager.executeBestGoal();

        // Dodging!!
        Collection<IncomingProjectile> projectiles = world.getAll(IncomingProjectile.class).values();
        for (IncomingProjectile projectile : projectiles) {
            if (projectile.isVisible()) {
                double distance = info.getLocation().getDistance(projectile.getLocation());
                double timeToTravel = distance / projectile.getSpeed();
                Location projectileDirection = new Location(projectile.getLocation().x,
                        projectile.getLocation().y,
                        projectile.getLocation().z).getNormalized();
                Location impactLocation = projectile.getLocation().add(projectileDirection.scale(timeToTravel));
                if (timeToTravel < 0.5 && info.getLocation().getDistance(impactLocation) < 300) {
                    move.dodge(projectileDirection.cross(new Location(0, 0, 1)), true);
                    break;
                }
            }
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

    public Player getEnemy() {
        return enemy;
    }

    public int supportPriority() {
        return whoNeedsMe == null ? 0 : 60;
    }

    public ILocated supportTarget() {
        if (whoNeedsMe == null || players.getPlayer(whoNeedsMe) == null)
            return null;
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

    public void setFocus(ILocated loc) {
        nmNav.setFocus(loc);
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
        if (coverBack.isCool())
            sniping = false;
    }

    /**
     * This method is called when the bot is started either from IDE or from
     * command line.
     *
     * @param args
     */
    public static void main(String args[]) throws PogamutException {
        UT2004TCServer.startTCServer();

        new UT2004BotRunner<UT2004Bot, UT2004BotParameters>(MyAlterEgo.class, "TeamCTF").setMain(true).setHost("localhost").setLogLevel(Level.WARNING).startAgents(
                new MyAlterEgoParams().setTeam(0).setOrder(0),
                new MyAlterEgoParams().setTeam(0).setOrder(1),
                new MyAlterEgoParams().setTeam(0).setOrder(2),
                new MyAlterEgoParams().setTeam(1).setOrder(0),
                new MyAlterEgoParams().setTeam(1).setOrder(1),
                new MyAlterEgoParams().setTeam(1).setOrder(2)
        );
    }
}
