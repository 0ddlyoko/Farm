package me.oddlyoko.farm;

import java.util.logging.Level;

import org.bukkit.Bukkit;
import org.bukkit.plugin.java.JavaPlugin;

import me.oddlyoko.farm.config.ConfigManager;
import me.oddlyoko.farm.farm.FarmManager;
import me.oddlyoko.farm.mine.MineManager;
import me.oddlyoko.farm.tree.TreeManager;

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
	private ConfigManager configManager;
	private FarmManager farmManager;
	private TreeManager treeManager;
	private MineManager mineManager;
	private FarmCommand farmCommand;

	public Farm() {
		farm = this;
	}

	@Override
	public void onEnable() {
		saveDefaultConfig();
		configManager = new ConfigManager();
		farmManager = new FarmManager();
		treeManager = new TreeManager();
		mineManager = new MineManager();
		Bukkit.getPluginCommand("farm").setExecutor(farmCommand = new FarmCommand());
		Bukkit.getLogger().log(Level.INFO, "Plugin loaded");
	}

	@Override
	public void onDisable() {
		Bukkit.getLogger().log(Level.INFO, "Plugin unloaded");
	}

	public ConfigManager getConfigManager() {
		return configManager;
	}

	public FarmManager getFarmManager() {
		return farmManager;
	}

	public TreeManager getTreeManager() {
		return treeManager;
	}

	public MineManager getMineManager() {
		return mineManager;
	}

	public static Farm get() {
		return farm;
	}
}
