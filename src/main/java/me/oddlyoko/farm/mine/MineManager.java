package me.oddlyoko.farm.mine;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.logging.Level;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Item;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.entity.ItemSpawnEvent;
import org.bukkit.event.player.AsyncPlayerChatEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.util.FileUtil;

import me.oddlyoko.farm.Farm;
import me.oddlyoko.farm.__;
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
	private World world;
	private List<Mine> mines;
	private Player playerMineMode;
	private Mine mineMineMode;
	private HashMap<Location, Player> blockBreaks;

	public MineManager() {
		config = new Config(new File("plugins" + File.separator + "Farm" + File.separator + "mines.yml"));
		mines = new ArrayList<>();
		world = Bukkit.getWorld(Farm.get().getConfigManager().getWorld());
		blockBreaks = new HashMap<>();
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
		for (Mine.Type type : Mine.Type.values()) {
			String k = "mines." + type.name();
			int tickTime = 100;
			int percent = 5;
			if (config.exist(k)) {
				tickTime = config.getInt(k + ".tickTime");
				percent = config.getInt(k + ".percent");
			}
			mines.add(new Mine(world, type, tickTime, percent));
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
						Bukkit.getLogger().log(Level.SEVERE, "Error while creating mines.yml file", ex);
						return;
					}
				} else
					FileUtil.copy(config.getFile(),
							new File("plugins" + File.separator + "Farm" + File.separator + "mines_old.yml"));
			}
			// Copy the old list to prevent modification errors
			for (Mine m : mines) {
				String k = "mines." + m.getType();
				config.set(k + ".tickTime", m.getTickTime());
				config.set(k + ".percent", m.getPercent());
			}
			Bukkit.getLogger().info("Mines saved");
		});
	}

	public Mine getMine(Type type) {
		if (type == null)
			return null;
		return mines.get(type.ordinal());
	}

	public Mine getMine(Location loc) {
		if (loc.getWorld() != world)
			return null;
		for (Mine m : mines)
			if (m.isInside(loc))
				return m;
		return null;
	}

	public List<Mine> getMines() {
		return mines;
	}

	public void setMine(Type type, int tickTime, int percent) {
		Mine m = getMine(type);
		m.setTickTime(tickTime);
		m.setPercent(percent);
		save();
	}

	public void clearMine(Mine mine) {
		mine.clear();
		save();
	}

	public void stop() {
		for (Mine m : mines)
			m.stop();
		mines = new ArrayList<>();
	}

	/**
	 * Switch in mine mode for specific player<br />
	 * <ul>
	 * <li>If p is null, mine mode will be removed from current player</li>
	 * <li>If another player is in mine mode, nothing will be done</li>
	 * <li>If same player is in mine mode, the mine mode will be removed</li>
	 * <li>If nobody is on mine mode, p will be in mine mode</li>
	 * </ul>
	 * 
	 * @param p
	 *                 The player
	 * @param Mine
	 *                 The mine
	 */
	public void mineMode(Player p, Mine mine) {
		if (p == null && playerMineMode == null)
			return;
		if (p != null && playerMineMode != null && playerMineMode != p) {
			// Do not remove
			p.sendMessage(__.PREFIX + ChatColor.RED + "Cannot switch in mine mode: player " + playerMineMode.getName()
					+ " is already in mine mode");
			return;
		}
		if (p == null || playerMineMode == p) {
			// Remove
			playerMineMode.sendMessage(__.PREFIX + ChatColor.GREEN + "Removing mine mode");
			playerMineMode = null;
			mineMineMode.stopMineMode();
			return;
		}
		// Add
		if (mine == null) {
			p.sendMessage(__.PREFIX + ChatColor.RED + "No mine found");
			return;
		}
		p.sendMessage(
				__.PREFIX + ChatColor.GREEN + "Switched to mine mode. By using this mode, the mine will be stopped");
		p.sendMessage(ChatColor.YELLOW + "Commands:");
		p.sendMessage(ChatColor.YELLOW + "- save" + ChatColor.GREEN + " : " + ChatColor.YELLOW + "Save & reload");
		p.sendMessage(ChatColor.YELLOW + "- show" + ChatColor.GREEN + " : " + ChatColor.YELLOW + "Show all logs");
		p.sendMessage(ChatColor.YELLOW + "- hide" + ChatColor.GREEN + " : " + ChatColor.YELLOW
				+ "Replace registered stone to sponge");
		playerMineMode = p;
		mineMineMode = mine;
		mine.mineMode(p);
	}

	@EventHandler
	public void onPlayerDisconnect(PlayerQuitEvent e) {
		if (playerMineMode == e.getPlayer())
			mineMode(null, null);
	}

	@EventHandler
	public void onPlayerChat(AsyncPlayerChatEvent e) {
		if (playerMineMode == e.getPlayer() && mineMineMode.mineModeCmd(e.getMessage()))
			e.setCancelled(true);
	}

	@EventHandler(priority = EventPriority.HIGHEST)
	public void onBlockDestroy(BlockBreakEvent e) {
		Block b = e.getBlock();
		if (b == null)
			return;

		if (b.getType() != Material.STONE && b.getType() != Material.COBBLESTONE && b.getType() != Material.BEDROCK
				&& b.getType() != Material.COAL_ORE && b.getType() != Material.DIAMOND_ORE
				&& b.getType() != Material.EMERALD_ORE && b.getType() != Material.GOLD_ORE
				&& b.getType() != Material.IRON_ORE && b.getType() != Material.LAPIS_ORE
				&& b.getType() != Material.REDSTONE_ORE && b.getType() != Material.NETHER_QUARTZ_ORE
				&& b.getType() != Material.SPONGE)
			return;

		if (e.getPlayer() == playerMineMode) {
			// Mine mode
			if (mineMineMode.isInside(b.getLocation()))
				// Remove it
				mineMineMode.removeBlockMineMode(b);
			else
				// Add it
				mineMineMode.addBlockMineMode(b);
			e.setCancelled(true);
			return;
		}
		if (b.getType() == Material.SPONGE)
			return;

		Mine m = getMine(b.getLocation());
		if (m == null)
			return;
		// Don't cancel for drop
		e.setCancelled(false);
		m.mine(b);
		blockBreaks.put(b.getLocation(), e.getPlayer());
		Bukkit.getScheduler().runTaskLater(Farm.get(), () -> {
			blockBreaks.remove(b.getLocation());
		}, 1);
	}

	@EventHandler(ignoreCancelled = true, priority = EventPriority.HIGHEST)
	public void onItemSpawn(ItemSpawnEvent e) {
		if (e.getEntityType() != EntityType.DROPPED_ITEM)
			return;
		ItemStack is = ((Item) e.getEntity()).getItemStack();
		Location loc = new Location(e.getLocation().getWorld(), e.getLocation().getBlockX(),
				e.getLocation().getBlockY(), e.getLocation().getBlockZ());
		if (!blockBreaks.containsKey(loc))
			return;
		Player p = blockBreaks.get(loc);
		e.setCancelled(true);
		p.getInventory().addItem(is);
	}
}
