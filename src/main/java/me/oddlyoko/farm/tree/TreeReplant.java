package me.oddlyoko.farm.tree;

import org.bukkit.Axis;
import org.bukkit.Location;
import org.bukkit.Material;

/**
 * Farm Copyright (C) 2019 0ddlyoko
 * 
 * This program is free software: you can redistribute it and/or modify it under
 * the terms of the GNU General Public License as published by the Free Software
 * Foundation, either version 3 of the License, or (at your option) any later
 * version.
 * 
 * This program is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
 * FOR A PARTICULAR PURPOSE. See the GNU General Public License for more
 * details.
 * 
 * You should have received a copy of the GNU General Public License along with
 * this program. If not, see <http://www.gnu.org/licenses/>.
 *
 * @author 0ddlyoko
 */
public class TreeReplant {
	private Location location;
	private Material material;
	private Axis direction;

	public TreeReplant(Location location, Material material, Axis direction) {
		this.location = location;
		this.material = material;
		this.direction = direction;
	}

	public Location getLocation() {
		return location;
	}

	public Material getMaterial() {
		return material;
	}

	public Axis getDirection() {
		return direction;
	}
}
