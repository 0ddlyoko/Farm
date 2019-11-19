package me.oddlyoko.farm.farm;

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
import org.bukkit.metadata.FixedMetadataValue;
import org.bukkit.metadata.MetadataValue;
import org.bukkit.util.FileUtil;

import me.oddlyoko.farm.config.Config;
import me.oddlyoko.farm.farm.Farm.Type;

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
public class FarmManager implements Listener {
	private Config config;
	private List<Farm> farms;

	public FarmManager() {
		config = new Config(new File("plugins" + File.separator + "Farm" + File.separator + "farms.yml"));
		farms = new ArrayList<>();
		Bukkit.getPluginManager().registerEvents(this, me.oddlyoko.farm.Farm.get());
		reload();
	}

	public void reload() {
		if (!config.getFile().exists()) {
			config.getFile().getParentFile().mkdirs();
			try {
				config.getFile().createNewFile();
			} catch (IOException ex) {
				Bukkit.getLogger().log(Level.SEVERE, "Error while creating farms.yml file", ex);
				return;
			}
		}
		stop();
		farms = new ArrayList<>();
		config.reload();
		List<String> keys = config.getKeys("farms");
		for (String key : keys) {
			String k = "farms." + key;
			Location center = config.getLocation(k + ".center");
			int radius = config.getInt(k + ".radius");
			String strType = config.getString(k + ".type");
			Farm.Type type = Type.CARROTS;
			try {
				type = Farm.Type.valueOf(strType);
			} catch (Exception ex) {
				Bukkit.getLogger().log(Level.WARNING, "Unknown Farm.Type " + strType + ", using default one (CARROTS)",
						ex);
			}
			int time = config.getInt(k + ".time");
			farms.add(new Farm(center, radius, type, time));
		}
	}

	public void save() {
		Bukkit.getScheduler().runTaskAsynchronously(me.oddlyoko.farm.Farm.get(), () -> {
			Bukkit.getLogger().info("Saving farms");
			// Copy (if error)
			if (!config.getFile().exists()) {
				config.getFile().getParentFile().mkdirs();
				try {
					config.getFile().createNewFile();
				} catch (IOException ex) {
					Bukkit.getLogger().log(Level.SEVERE, "Error while creating farms.yml file", ex);
					return;
				}
			} else
				FileUtil.copy(config.getFile(),
						new File("plugins" + File.separator + "Farm" + File.separator + "farms_old.yml"));
			config.set("farms", null);
			int i = 0;
			// Copy the old list to prevent modification errors
			for (Farm v : new ArrayList<>(farms)) {
				String k = "farms." + i;
				config.set(k + ".center", v.getCenter());
				config.set(k + ".radius", v.getRadius());
				config.set(k + ".type", v.getType().name());
				config.set(k + ".time", v.getTickTime());

				i++;
			}
			Bukkit.getLogger().info("Farms saved");
		});
	}

	public Farm getNearbyFarm(Location loc) {
		double dist = -1;
		Farm f = null;
		for (Farm farm : farms) {
			if (farm.isInside(loc) && (f == null || farm.getCenter().distance(loc) < dist)) {
				f = farm;
				dist = farm.getCenter().distance(loc);
			}
		}
		return f;
	}

	public Farm getFarm(Location loc) {
		for (Farm f : farms)
			if (f.isInside(loc))
				return f;
		return null;
	}

	public List<Farm> getFarms() {
		return farms;
	}

	public void addFarm(Location center, int radius, Farm.Type type, int tickTime) {
		farms.add(new Farm(center, radius, type, tickTime));
		save();
	}

	public void removeFarm(Farm farm) {
		farms.remove(farm);
		farm.stop();
		save();
	}

	public void stop() {
		for (Farm f : farms)
			f.stop();
		farms = new ArrayList<>();
	}

	@EventHandler(priority = EventPriority.MONITOR)
	public void onBlockDestroy(BlockBreakEvent e) {
		if (e.isCancelled())
			return;

		Block b = e.getBlock();
		if (b == null)
			return;

		if (!Farm.Type.is(b.getType()))
			return;

		Farm f = getFarm(new Location(b.getWorld(), b.getX(), b.getY() - 1, b.getZ()));
		if (f == null)
			return;

		long lastUpdate = System.currentTimeMillis();
		if (b.getMetadata("last_update").size() != 0)
			lastUpdate = b.getMetadata("last_update").get(0).asLong();
		b.setMetadata("last_update", new FixedMetadataValue(me.oddlyoko.farm.Farm.get(), System.currentTimeMillis()));
		List<MetadataValue> vals = b.getMetadata("attempt");
		b.setMetadata("attempt",
				new FixedMetadataValue(me.oddlyoko.farm.Farm.get(), (vals.size() > 0) ? vals.get(0).asInt() + 1 : 1));
		System.out.println("Adding block to list");
		f.add(b);
		if (vals.size() > 0 && vals.get(0).asInt() >= 4) {
			if ((System.currentTimeMillis() - lastUpdate) / 1000 < 6)
				// To many destroid
				b.setType(Material.AIR);
			else
				b.setMetadata("attempt", new FixedMetadataValue(me.oddlyoko.farm.Farm.get(), 0));
		}
	}
}
