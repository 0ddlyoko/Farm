package me.oddlyoko.farm.tree;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;

import org.bukkit.Axis;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.data.Orientable;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.player.AsyncPlayerChatEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.metadata.FixedMetadataValue;
import org.bukkit.metadata.MetadataValue;
import org.bukkit.util.FileUtil;

import me.oddlyoko.farm.Farm;
import me.oddlyoko.farm.__;
import me.oddlyoko.farm.config.Config;

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
public class TreeManager implements Listener {
	private Config config;
	private List<Tree> trees;
	private Player playerTreeMode;
	private Tree treeTreeMode;

	public TreeManager() {
		config = new Config(new File("plugins" + File.separator + "Farm" + File.separator + "trees.yml"));
		trees = new ArrayList<>();
		Bukkit.getPluginManager().registerEvents(this, Farm.get());
		reload();
	}

	public void reload() {
		if (!config.getFile().exists()) {
			config.getFile().getParentFile().mkdirs();
			try {
				config.getFile().createNewFile();
			} catch (IOException ex) {
				Bukkit.getLogger().log(Level.SEVERE, "Error while creating trees.yml file", ex);
				return;
			}
		}
		stop();
		trees = new ArrayList<>();
		config.reload();
		List<String> keys = config.getKeys("trees");
		for (String key : keys) {
			String k = "trees." + key;
			Location center = config.getLocation(k + ".center");
			int time = config.getInt(k + ".time");
			trees.add(new Tree(center, time));
		}
	}

	public void save() {
		Bukkit.getScheduler().runTaskAsynchronously(me.oddlyoko.farm.Farm.get(), () -> {
			Bukkit.getLogger().info("Saving trees");
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
						new File("plugins" + File.separator + "Farm" + File.separator + "trees_old.yml"));
			config.set("trees", null);
			int i = 0;
			// Copy the old list to prevent modification errors
			for (Tree v : new ArrayList<>(trees)) {
				String k = "trees." + i;
				config.set(k + ".center", v.getCenter());
				config.set(k + ".time", v.getTickTime());

				i++;
			}
			Bukkit.getLogger().info("Trees saved");
		});
	}

	public Tree getNearbyTree(Location loc) {
		double dist = -1;
		Tree t = null;
		for (Tree tree : trees) {
			if (t == null || tree.getCenter().distance(loc) < dist) {
				t = tree;
				dist = tree.getCenter().distance(loc);
			}
		}
		return t;
	}

	public Tree getTree(Location loc) {
		for (Tree t : trees)
			if (t.isInside(loc))
				return t;
		return null;
	}

	public List<Tree> getTrees() {
		return trees;
	}

	public void addTree(Location center, int tickTime) {
		trees.add(new Tree(center, tickTime));
		save();
	}

	public void removeTree(Tree tree) {
		trees.remove(tree);
		tree.remove();
		tree.stop();
		save();
	}

	public void stop() {
		treeMode(null, null);
		for (Tree t : trees)
			t.stop();
		trees = new ArrayList<>();
	}

	/**
	 * Switch in tree mode for specific player<br />
	 * <ul>
	 * <li>If p is null, tree mode will be removed from current player</li>
	 * <li>If another player is in tree mode, nothing will be done</li>
	 * <li>If same player is in tree mode, the tree mode will be removed</li>
	 * <li>If nobody is on tree mode, p will be in tree mode</li>
	 * </ul>
	 * 
	 * @param p
	 *                 The player
	 * @param tree
	 *                 The tree
	 */
	public void treeMode(Player p, Tree tree) {
		if (p == null && playerTreeMode == null)
			return;
		if (p != null && playerTreeMode != null && playerTreeMode != p) {
			// Do not remove
			p.sendMessage(__.PREFIX + ChatColor.RED + "Cannot switch in tree mode: player " + playerTreeMode.getName()
					+ " is already in tree mode");
			return;
		}
		if (p == null || playerTreeMode == p) {
			// Remove
			playerTreeMode.sendMessage(__.PREFIX + ChatColor.GREEN + "Removing tree mode");
			playerTreeMode = null;
			treeTreeMode.stopTreeMode();
			return;
		}
		// Add
		if (tree == null) {
			p.sendMessage(__.PREFIX + ChatColor.RED + "No tree found");
			return;
		}
		p.sendMessage(
				__.PREFIX + ChatColor.GREEN + "Switched to tree mode. By using this mode, the tree will be stopped");
		p.sendMessage(ChatColor.YELLOW + "Commands:");
		p.sendMessage(ChatColor.YELLOW + "- save" + ChatColor.GREEN + " : " + ChatColor.YELLOW + "Save & reload");
		p.sendMessage(ChatColor.YELLOW + "- show" + ChatColor.GREEN + " : " + ChatColor.YELLOW + "Show all logs");
		p.sendMessage(
				ChatColor.YELLOW + "- hide" + ChatColor.GREEN + " : " + ChatColor.YELLOW + "Hide registered logs");
		playerTreeMode = p;
		treeTreeMode = tree;
		tree.treeMode(p);
	}

	@EventHandler
	public void onPlayerDisconnect(PlayerQuitEvent e) {
		if (playerTreeMode == e.getPlayer())
			treeMode(null, null);
	}

	@EventHandler
	public void onPlayerChat(AsyncPlayerChatEvent e) {
		Player p = e.getPlayer();
		if (playerTreeMode == p)
			if (treeTreeMode.treeModeCmd(e.getMessage()))
				e.setCancelled(true);
	}

	@EventHandler(priority = EventPriority.MONITOR)
	public void onBlockDestroy(BlockBreakEvent e) {
		if (e.isCancelled())
			return;

		Block b = e.getBlock();
		if (b == null)
			return;

		if (b.getType() != Material.ACACIA_LOG && b.getType() != Material.BIRCH_LOG
				&& b.getType() != Material.DARK_OAK_LOG && b.getType() != Material.JUNGLE_LOG
				&& b.getType() != Material.OAK_LOG && b.getType() != Material.SPRUCE_LOG)
			return;

		if (e.getPlayer() == playerTreeMode) {
			// Tree mode
			if (treeTreeMode.isInside(b.getLocation()))
				// Remove it
				treeTreeMode.removeBlockTreeMode(b.getLocation());
			else
				// Add it
				treeTreeMode.addBlockTreeMode(b);
			e.setCancelled(true);
			return;
		}

		Tree t = getTree(b.getLocation());
		if (t == null)
			return;

		long lastUpdate = System.currentTimeMillis();
		if (b.getMetadata("last_update").size() != 0)
			lastUpdate = b.getMetadata("last_update").get(0).asLong();
		b.setMetadata("last_update", new FixedMetadataValue(Farm.get(), System.currentTimeMillis()));
		List<MetadataValue> vals = b.getMetadata("attempt");
		b.setMetadata("attempt", new FixedMetadataValue(Farm.get(), (vals.size() > 0) ? vals.get(0).asInt() + 1 : 1));

		Axis direction = Axis.Y;
		if (b.getBlockData() instanceof Orientable)
			direction = ((Orientable) b.getBlockData()).getAxis();
		System.out.println("Adding block to list");
		t.add(b.getLocation(), b.getType(), direction);
		if (vals.size() > 0 && vals.get(0).asInt() >= 4) {
			if ((System.currentTimeMillis() - lastUpdate) / 1000 < 6)
				// To many destroid
				b.setType(Material.AIR);
			else
				b.setMetadata("attempt", new FixedMetadataValue(Farm.get(), 0));
		}
	}
}
