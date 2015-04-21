/*
 * TCSupportMe.java
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

package kiv.janecekz.teamcomm;

import cz.cuni.amis.pogamut.base3d.worldview.object.Location;
import cz.cuni.amis.pogamut.ut2004.teamcomm.mina.messages.TCMessageData;
import cz.cuni.amis.utils.token.Tokens;

public class TCFlagUpdate extends TCMessageData {
    public final Location loc;
    public final int team;
    public final long time;
    public final FlagHuntingState type;

    public TCFlagUpdate(int team, Location loc, long time, FlagHuntingState type) {
        super(Tokens.get("TCFlagUpdate"));
        this.team = team;
        this.loc = loc;
        this.time = time;
        this.type = type;
    }
}
