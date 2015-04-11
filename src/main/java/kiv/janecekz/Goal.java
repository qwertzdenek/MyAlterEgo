/*
 * Goal.java
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

public abstract class Goal implements IGoal {

    protected MyAlterEgo bot;

    public Goal(MyAlterEgo bot) {
        this.bot = bot;
    }

    /**
     * Reverse ordering, greater numbers first, lesser later
     * @param arg0 another goal
     */
    @Override
    public int compareTo(IGoal arg0) {
        if (getPriority() == ((IGoal) arg0).getPriority()) {
            return 0;
        } else if ((getPriority()) > ((IGoal) arg0).getPriority()) {
            return -1;
        } else {
            return 1;
        }
    }
}
