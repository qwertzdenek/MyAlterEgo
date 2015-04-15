/*
 * GetHealth.java
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

import cz.cuni.amis.pogamut.base.utils.math.DistanceUtils;
import java.util.Set;

import cz.cuni.amis.pogamut.ut2004.communication.messages.ItemType.Category;
import cz.cuni.amis.pogamut.ut2004.communication.messages.gbinfomessages.Item;
import kiv.janecekz.Goal;
import kiv.janecekz.MyAlterEgo;

public class GetHealth extends Goal {

    protected Item health = null;
    protected boolean covering = false;

    public GetHealth(MyAlterEgo bot) {
        super(bot);
    }

    @Override
    public void perform() {
        if (bot.isDangerous(bot.getInfo().getLocation())) {
            bot.coverYourself();
        } else {
            bot.updateFight();
        }

        Set<Item> healths = bot.getTaboo().filter(
                bot.getItems().getSpawnedItems(
                        Category.HEALTH).values());

        if (health != null && bot.getInfo().atLocation(health)) {
            bot.getTaboo().add(health, 10);
            health = null;
        } else if (health == null) {
            health = DistanceUtils.getNearest(healths, bot.getInfo());

            if (health != null) bot.goCovered(health.getLocation());
            else bot.goTo(bot.getOurFlagBase());
        }
    }

    @Override
    public double getPriority() {
        if (bot.getItems().getAllItems(Category.HEALTH).size() > 0
                && bot.getInfo().getHealth() < 20
                && !(bot.getEnemyFlag() != null && bot.getInfo().getId().equals(bot.getEnemyFlag().getHolder()))) {
            return 100d;
        }

        return 0d;
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
        this.health = null;
        bot.reset();
    }

    @Override
    public String toString() {
        return "GetHealth";
    }
}
