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

import java.util.Set;

import cz.cuni.amis.pogamut.ut2004.communication.messages.ItemType.Category;
import cz.cuni.amis.pogamut.ut2004.communication.messages.gbinfomessages.Item;
import kiv.janecekz.Goal;
import kiv.janecekz.MyAlterEgo;

public class GetHealth extends Goal {

    protected Item health = null;

    public GetHealth(MyAlterEgo bot) {
        super(bot);
    }

    @Override
    public void perform() {
        bot.updateFight();

        if (health == null) {
            Set<Item> healths = bot.getTaboo().filter(
                    bot.getItems().getSpawnedItems(
                            Category.HEALTH).values());

            double min_distance = Double.MAX_VALUE;
            Item winner = null;

            for (Item item : healths) {
                if (bot.isDangerous(item)) {
                    continue;
                }
                double dist = item.getLocation().getDistance(
                        bot.getInfo().getLocation());
                if (dist < min_distance) {
                    min_distance = dist;
                    winner = item;
                }
            }
            this.health = winner;
        }

        if (bot.getEnemyFlag() == null
                || !bot.getInfo().getId()
                .equals(bot.getEnemyFlag().getHolder())) {
            bot.goCovered(health.getLocation());
        }
    }

    @Override
    public double getPriority() {
        if (bot.getItems().getAllItems(Category.HEALTH).size() > 0
                && bot.getInfo().getHealth() < 20 && !(bot.getEnemyFlag() != null
                && bot.getInfo().getId()
                .equals(bot.getEnemyFlag().getHolder())
                && bot.getInfo().atLocation(bot.getOurFlagBase(), 5d))) {
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
        bot.reset();
    }
}
