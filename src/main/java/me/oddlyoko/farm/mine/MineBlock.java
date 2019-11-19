package me.oddlyoko.farm.mine;

import org.bukkit.block.Block;

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
public class MineBlock {
	private Block block;
	private int timeRemaining;

	public MineBlock(Block block, int timeRemaining) {
		this.block = block;
		this.timeRemaining = timeRemaining;
	}

	public Block getBlock() {
		return block;
	}

	public int getTimeRemaining() {
		return timeRemaining;
	}

	public boolean tick() {
		timeRemaining--;
		return timeRemaining <= 0;
	}
}
