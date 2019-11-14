package me.oddlyoko.farm;

import java.util.logging.Level;

import org.bukkit.Bukkit;
import org.bukkit.plugin.java.JavaPlugin;

import me.oddlyoko.farm.farm.FarmManager;

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
public class Farm extends JavaPlugin {
	private static Farm farm;
	private FarmCommand farmCommand;
	private FarmManager farmManager;

	public Farm() {
		farm = this;
	}

	@Override
	public void onEnable() {
		farmManager = new FarmManager();
		Bukkit.getPluginCommand("farm").setExecutor(farmCommand = new FarmCommand());
		Bukkit.getLogger().log(Level.INFO, "Plugin loaded");
	}

	@Override
	public void onDisable() {
		Bukkit.getLogger().log(Level.INFO, "Plugin unloaded");
	}

	public FarmManager getFarmManager() {
		return farmManager;
	}

	public static Farm get() {
		return farm;
	}
}
