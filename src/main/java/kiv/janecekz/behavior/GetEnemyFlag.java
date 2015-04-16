/*
 * GetEnemyFlag.java
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

import cz.cuni.amis.pogamut.base3d.worldview.object.Location;
import cz.cuni.amis.pogamut.unreal.communication.messages.UnrealId;
import kiv.janecekz.Goal;
import kiv.janecekz.MyAlterEgo;

public class GetEnemyFlag extends Goal {
    public GetEnemyFlag(MyAlterEgo bot) {
        super(bot);
    }

    @Override
    public void perform() {
        if (bot.getEnemyFlag() != null) {
            UnrealId holderId = bot.getEnemyFlag().getHolder();

            if (bot.getInfo().getId().equals(holderId)) {
                bot.goTo(bot.getOurFlagBase().getLocation());
            } else {
                if (bot.getCTF().isEnemyFlagHome()) {
                    bot.goCovered(bot.getEnemyFlagBase().getLocation());
                } else {
                    Location target = bot.getEnemyFlag().getLocation();
                    if (target == null) {
                        target = bot.getEnemyFlagBase().getLocation();
                    }

                    bot.goTo(target);
                }
            }
        } else {
            bot.goCovered(bot.getEnemyFlagBase().getLocation());
        }

        if (bot.isDangerous(bot.getInfo().getLocation()))
            bot.callHelp();
        bot.setBackup();
        bot.updateFight();
    }

    @Override
    public double getPriority() {
        if (bot.getEnemyFlag() != null
                && bot.getInfo().getId().equals(bot.getEnemyFlag().getHolder())) {
            return 50d;
        } else if (bot.getEnemyFlag() != null && bot.getEnemyFlag().getLocation() != null
                && bot.getInfo().atLocation(bot.getEnemyFlag().getLocation(), 300d)) {
            return 40d;
        } else {
            return 10d;
        }
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
        bot.dismissHelp();
        bot.reset();
    }

    @Override
    public String toString() {
        return "GetEnemyFlag";
    }
}
