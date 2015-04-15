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

import cz.cuni.amis.pogamut.ut2004.communication.messages.UT2004ItemType;

import cz.cuni.amis.pogamut.ut2004.communication.messages.gbinfomessages.Item;
import cz.cuni.amis.utils.IFilter;
import cz.cuni.amis.utils.collections.MyCollections;
import java.util.List;
import kiv.janecekz.Goal;
import kiv.janecekz.MyAlterEgo;

public class GetItems extends Goal {

    protected Item item;
    protected List<Item> itemsToRunAround;

    public GetItems(MyAlterEgo bot) {
        super(bot);
        item = null;
    }

    @Override
    public void perform() {
        bot.updateFight();

        itemsToRunAround = availableItems();

        if (item != null && bot.getInfo().atLocation(item)) {
            bot.getTaboo().add(item, 10);
            item = null;
        } else if (item == null) {
            item = MyCollections.getRandom(itemsToRunAround);

            bot.goTo(item.getLocation());
        }
    }

    @Override
    public double getPriority() {
        itemsToRunAround = availableItems();

        if (itemsToRunAround.isEmpty())
            return 0;
        else
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

    public List<Item> availableItems() {
        return MyCollections.getFiltered(bot.getItems().getAllItems().values(), new IFilter<Item>() {
            @Override
            public boolean isAccepted(Item object) {
                return !bot.getTaboo().contains(object) && bot.getItems().isPickupSpawned(object) && bot.getItems().isPickable(object) && object.isVisible() && !object.getType().equals(UT2004ItemType.ADRENALINE_PACK);
            }
        });
    }

    @Override
    public String toString() {
        return "GetItems";
    }
}
