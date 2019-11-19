package me.oddlyoko.farm.mine;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.util.FileUtil;

import me.oddlyoko.farm.Farm;
import me.oddlyoko.farm.config.Config;
import me.oddlyoko.farm.mine.Mine.Type;

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
public class MineManager implements Listener {
	private Config config;
	private List<Mine> mines;

	public MineManager(String worldName, String regionName) {
		config = new Config(new File("plugins" + File.separator + "Farm" + File.separator + "mines.yml"));
		mines = new ArrayList<>();
		Bukkit.getPluginManager().registerEvents(this, Farm.get());
		reload();
	}

	public void reload() {
		if (!config.getFile().exists()) {
			config.getFile().getParentFile().mkdirs();
			try {
				config.getFile().createNewFile();
			} catch (IOException ex) {
				Bukkit.getLogger().log(Level.SEVERE, "Error while creating mines.yml file", ex);
				return;
			}
		}
		stop();
		mines = new ArrayList<>();
		config.reload();
		List<String> keys = config.getKeys("mines");
		for (String key : keys) {
			String k = "mines." + key;
			String worldName = config.getString(k + ".worldName");
			String regionName = config.getString(k + ".regionName");
			Type type = Type.STONE;
			try {
				type = Type.valueOf(config.getString(k + ".type"));
			} catch (Exception ex) {
				Bukkit.getLogger().log(Level.SEVERE, "Cannot parse mine type " + config.getString(k + ".type"));
				continue;
			}
			int tickTime = config.getInt(k + ".tickTime");
			int percent = config.getInt(k + ".percent");
			mines.add(new Mine(worldName, regionName, type, tickTime, percent));
		}

	}

	public void save() {
		Bukkit.getScheduler().runTaskAsynchronously(Farm.get(), () -> {
			Bukkit.getLogger().info("Saving mines");
			// Copy (if error)
			if (!config.getFile().exists()) {
				// Copy (if error)
				if (!config.getFile().exists()) {
					config.getFile().getParentFile().mkdirs();
					try {
						config.getFile().createNewFile();
					} catch (IOException ex) {
						Bukkit.getLogger().log(Level.SEVERE, "Error while creating trees.yml file", ex);
						return;
					}
				} else
					FileUtil.copy(config.getFile(),
							new File("plugins" + File.separator + "Farm" + File.separator + "mines_old.yml"));
			}
			int i = 0;
			// Copy the old list to prevent modification errors
			for (Mine m : new ArrayList<>(mines)) {
				String k = "mines." + i;
				config.set(k + ".worldName", m.getWorldName());
				config.set(k + ".regionName", m.getRegionName());
				config.set(k + ".type", m.getType().name());
				config.set(k + ".tickTime", m.getTickTime());
				config.set(k + ".percent", m.getPercent());

				i++;
			}
			Bukkit.getLogger().info("Mines saved");
		});
	}

	public Mine getInsideMine(Location loc) {
		for (Mine m : mines)
			if (m.isBetween(loc))
				return m;
		return null;
	}

	public Mine getMine(Location loc) {
		for (Mine m : mines)
			if (m.isInside(loc))
				return m;
		return null;
	}

	public List<Mine> getMines() {
		return mines;
	}

	public void addMine(String worldName, String regionName, Type type, int tickTime, int percent) {
		mines.add(new Mine(worldName, regionName, type, tickTime, percent));
		save();
	}

	public void removeMine(Mine mine) {
		mines.remove(mine);
		mine.stop();
		save();
	}

	public void stop() {
		for (Mine m : mines)
			m.stop();
		mines = new ArrayList<>();
	}

	@EventHandler(priority = EventPriority.MONITOR)
	public void onBlockDestroy(BlockBreakEvent e) {
		if (e.isCancelled())
			return;

		Block b = e.getBlock();
		if (b == null)
			return;
		if (b.getType() != Material.STONE && b.getType() != Material.COBBLESTONE && b.getType() != Material.BEDROCK
				&& b.getType() != Material.COAL_ORE && b.getType() != Material.DIAMOND_ORE
				&& b.getType() != Material.EMERALD_ORE && b.getType() != Material.GOLD_ORE
				&& b.getType() != Material.IRON_ORE && b.getType() != Material.LAPIS_ORE
				&& b.getType() != Material.REDSTONE_ORE && b.getType() != Material.NETHER_QUARTZ_ORE)
			return;
		Mine m = getMine(b.getLocation());
		if (m == null)
			return;
		m.mine(b);
	}
}
