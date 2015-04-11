/*
 * SupportFriend.java
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

import kiv.janecekz.Goal;
import kiv.janecekz.MyAlterEgo;

public class SupportFriend extends Goal {

    public SupportFriend(MyAlterEgo bot) {
        super(bot);
    }

    @Override
    public void perform() {
        bot.updateFight();
        bot.goTo(bot.supportTarget());
    }

    @Override
    public double getPriority() {
        return bot.supportPriority();
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
