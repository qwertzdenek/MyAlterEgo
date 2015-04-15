/*
 * TCPlayerSeen.java
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
import cz.cuni.amis.pogamut.unreal.communication.messages.UnrealId;
import cz.cuni.amis.pogamut.ut2004.teamcomm.mina.messages.TCMessageData;
import cz.cuni.amis.utils.token.Tokens;
import kiv.janecekz.MyAlterEgo;

public class TCPlayerSeen extends TCMessageData {
    public final UnrealId playerId;
    public final Location location;
    public final int team;

    public TCPlayerSeen(MyAlterEgo aThis, UnrealId id, Location location, int team) {
        super(Tokens.get("TCPlayerSeen"));
        this.location = location;
        this.playerId = id;
        this.team = team;
    }
}
