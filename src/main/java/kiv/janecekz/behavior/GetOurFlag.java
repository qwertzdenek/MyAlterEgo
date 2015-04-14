/*
 * GetOutFlag.java
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
package kiv.janecekz.behavior;

import cz.cuni.amis.pogamut.base3d.worldview.object.ILocated;
import cz.cuni.amis.pogamut.base3d.worldview.object.Location;
import cz.cuni.amis.pogamut.unreal.communication.messages.UnrealId;
import cz.cuni.amis.pogamut.ut2004.communication.messages.gbinfomessages.Player;
import cz.cuni.amis.utils.Heatup;
import kiv.janecekz.Goal;
import kiv.janecekz.MyAlterEgo;

public class GetOurFlag extends Goal {

    private static final double CAPTURE_SUPPORT_DISTANCE = 200;
    protected Player enemy = null;
    Location flagLocation;
    Heatup onPlace = new Heatup(4000);

    public GetOurFlag(MyAlterEgo bot) {
        super(bot);
    }

    @Override
    public void perform() {
        if (bot.getOurFlag() != null) {
            if (bot.getOurFlag().getLocation() != null) {
                flagLocation = bot.getOurFlag().getLocation();
            }

            if (flagLocation != null) {
                enemy = bot.getPlayers().getPlayer(bot.getOurFlag().getHolder());

                if (enemy != null) {
                    bot.goTo(enemy);

                    if (enemy.isVisible()) {
                        bot.updateFight(enemy);
                        return;
                    }
                } else {
                    if (bot.getInfo().getLocation().equals(flagLocation)) {
                        bot.goTo(bot.getEnemyFlagBase());
                    } else if (bot.getOurFlag().getState().equalsIgnoreCase("home")) {
                        bot.goForSniper();
                    } else {
                        bot.goTo(flagLocation);
                    }
                }
            } else {
                if (bot.isCoveringBack()) bot.goForSniper();
                else bot.goTo(bot.getEnemyFlagBase());
            }
        }

        bot.updateFight();
    }

    @Override
    public double getPriority() {
        if (bot.getOurFlag() == null
                || bot.getOurFlag().getState().equalsIgnoreCase("home")) {
            return 0d + (bot.isCoveringBack() ? 30d : 0d);
        }

        if (bot.getEnemyFlag() != null) {
            UnrealId holderId = bot.getEnemyFlag().getHolder();

            if (holderId != null) {
                Player holder = bot.getPlayers().getFriends().get(holderId);

                if (holder != null && bot.getPlayers().getFriends().size() > 1) {
                    if (holderId.equals(bot.getInfo().getId())
                            || holder.getLocation().getDistance(
                                    bot.getInfo().getLocation())
                            < CAPTURE_SUPPORT_DISTANCE) {
                        return 0d;
                    }
                } else {
                    return 70d;
                }
            } else {
                return 70d;
            }
        }
        return 20d;
    }

    @Override
    public boolean hasFailed() {
        return false;
    }

    @Override
    public boolean hasFinished() {
        return false;
    }

    @Override
    public void abandon() {
        bot.reset();
    }

    @Override
    public String toString() {
        return "GetOurFlag";
    }
}
