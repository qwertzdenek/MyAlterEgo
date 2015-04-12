/*
 * GoalManager.java
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

import cz.cuni.amis.pogamut.ut2004.bot.impl.UT2004Bot;
import java.util.Collections;
import java.util.LinkedList;

public class GoalManager {
    private final LinkedList<IGoal> goals = new LinkedList<IGoal>();
    private IGoal currentGoal = null;
    private MyAlterEgo bot;

    public GoalManager(MyAlterEgo bot) {
        this.bot = bot;
    }

    public boolean addGoal(IGoal goal) {
        if (!goals.contains(goal)) {
            goals.add(goal);
            return true;
        } else {
            return false;
        }
    }

    public IGoal executeBestGoal() {

        Collections.sort(goals);

        IGoal next_goal = goals.peekFirst();
        if (next_goal != currentGoal && currentGoal != null) {
            currentGoal.abandon();
        }

        currentGoal = next_goal;
        currentGoal.perform();

        bot.setPostfix(currentGoal.toString());

        return currentGoal;
    }

    public IGoal getCurrentGoal() {
        return currentGoal;
    }

    public void abandonAllGoals() {
        for (IGoal goal : goals) {
            goal.abandon();
        }
    }
}
