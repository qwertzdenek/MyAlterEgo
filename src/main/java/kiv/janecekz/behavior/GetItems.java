/*
 * GetItems.java
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

import cz.cuni.amis.pogamut.ut2004.communication.messages.ItemType;
import cz.cuni.amis.pogamut.ut2004.communication.messages.UT2004ItemType;
import java.util.LinkedList;

import cz.cuni.amis.pogamut.ut2004.communication.messages.gbinfomessages.Item;
import cz.cuni.amis.utils.IFilter;
import kiv.janecekz.Goal;
import kiv.janecekz.MyAlterEgo;

public class GetItems extends Goal {

    protected Item item;
    protected LinkedList<Item> itemsToRunAround;

    public GetItems(MyAlterEgo bot) {
        super(bot);
        item = null;
    }

    @Override
    public void perform() {
        bot.updateFight();

        if (item != null && bot.getInfo().atLocation(item)) {
            bot.getTaboo().add(item, 10);
            item = null;
        } else if (item == null || !item.getNavPoint().isItemSpawned()) {
            item = oneItem();

            bot.goTo(item.getLocation());
        }
    }

    @Override
    public double getPriority() {
        itemsToRunAround = filterItems(UT2004ItemType.ADRENALINE_PACK);

        return 6 + itemsToRunAround.size() + (bot.ammoOK() ? 0 : 10);
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
        item = null;
        bot.reset();
    }

    public Item getItem() {
        return item;
    }

    public Item oneItem() {
        return bot.getFwMap().getNearestFilteredItem(bot.getItems().getAllItems().values(), bot.getInfo().getNearestNavPoint(), new IFilter<Item>() {
            @Override
            public boolean isAccepted(Item object) {
                return !bot.getTaboo().contains(object) && bot.getItems().isPickupSpawned(object) && bot.getItems().isPickable(object) && !object.getType().equals(UT2004ItemType.ADRENALINE_PACK);
            }
        });
    }

    public LinkedList<Item> filterItems(ItemType type) {
        LinkedList<Item> res = new LinkedList<Item>();

        for (Item i : res) {
            if (!i.getType().equals(type)) {
                res.add(i);
            }
        }

        return res;
    }

    @Override
    public String toString() {
        return "GetItems";
    }
}
