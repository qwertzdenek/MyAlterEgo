/*
 * Copyright (C) 2015 AMIS research group, Faculty of Mathematics and Physics, Charles University in Prague, Czech Republic
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package kiv.janecekz;

import cz.cuni.amis.pogamut.ut2004.bot.params.UT2004BotParameters;

public class MyAlterEgoParams extends UT2004BotParameters {

    @Override
    public MyAlterEgoParams setTeam(Integer team) {
        return (MyAlterEgoParams) super.setTeam(team);
    }

    @Override
    public Integer getTeam() {
        return super.getTeam();
    }

    /**
     * Agent's order in array
     */
    private int order;

    public MyAlterEgoParams setOrder(int newOrder) {
        this.order = newOrder;
        return this;
    }

    public int getOrder() {
        return this.order;
    }

    /**
     * Agent's skill level.
     */
    private int skillLevel;

    /**
     * Method sets a skill level.
     *
     * @param newSkillLevel new skill level
     * @return this
     */
    public MyAlterEgoParams setSkillLevel(int newSkillLevel) {
        this.skillLevel = newSkillLevel;
        return this;
    }

    /**
     * Method returns a skill level.
     *
     * @return skill level
     */
    public int getSkillLevel() {
        return this.skillLevel;
    }
}
