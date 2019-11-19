package me.oddlyoko.farm;

import org.bukkit.Location;
import org.bukkit.Particle;

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
public class Util {

	/**
	 * Show a line of particle between two points with custom space
	 * 
	 * @param p
	 *                  The particle
	 * @param from
	 *                  The first location
	 * @param to
	 *                  The second location
	 * @param space
	 *                  The space
	 */
	public static void makeParticleLine(Particle p, Location from, Location to, int space) {
		makeParticleLine(p, from, to, space, null);
	}

	/**
	 * Show a line of particle between two points with custom space
	 * 
	 * @param p
	 *                  The particle
	 * @param from
	 *                  The first location
	 * @param to
	 *                  The second location
	 * @param space
	 *                  The space
	 * @param data
	 *                  The particle data
	 */
	public static void makeParticleLine(Particle p, Location from, Location to, int space, Object data) {
		double x = to.getX() - from.getX();
		double y = to.getY() - from.getY();
		double z = to.getZ() - from.getZ();
		double distance = Math.sqrt(x * x + y * y + z * z);
		// Normalize (distance to 1)
		x /= distance;
		y /= distance;
		z /= distance;
		for (int i = 0; i < distance; i += space)
			from.getWorld().spawnParticle(p, from.getX() + x * i, from.getY() + y * i, from.getZ() + z * i, 2, 0, 0, 0,
					0, data);
		from.getWorld().spawnParticle(p, to, 10, 0, 0, 0, 0, data);
	}
}
