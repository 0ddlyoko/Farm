package me.oddlyoko.farm.tree;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.logging.Level;
import java.util.regex.Pattern;

import org.bukkit.Axis;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Particle.DustOptions;
import org.bukkit.Sound;
import org.bukkit.block.Block;
import org.bukkit.block.data.BlockData;
import org.bukkit.block.data.Orientable;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;

import me.oddlyoko.farm.Farm;
import me.oddlyoko.farm.Util;
import me.oddlyoko.farm.__;

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
public class Tree {
	private Location center;
	private int tickTime;

	// A LinkedList of plants to replant
	private LinkedList<TreeReplant> toPlant;
	// The file that contain all log
	private File logFile;
	// An HashMap containing all LOG
	private HashMap<Location, TreeReplant> logs;
	// Thread to reload
	private BukkitTask reloadAllThread;
	// The Tree Thread
	private BukkitRunnable treeThread;
	// Ok
	private final DustOptions white = new Particle.DustOptions(Color.WHITE, 1);
	// TreeMode
	private final DustOptions purple = new Particle.DustOptions(Color.PURPLE, 1);
	// The player that is using tree mode
	private Player playerTreeMode;
	// If false,
	private boolean showTreeMode;
	private boolean stop;

	public Tree(Location center, int tickTime) {
		this.center = center;
		this.tickTime = tickTime;
		logFile = new File("plugins" + File.separator + "Farm" + File.separator + "tree" + File.separator + "tree_"
				+ center.getWorld().getName() + "_" + center.getBlockX() + "_" + center.getBlockY() + "_"
				+ center.getBlockZ());
		stop = false;
		toPlant = new LinkedList<>();
		logs = new HashMap<>();
		reloadAll();
		initRunnable();
	}

	private int tick;

	private void initRunnable() {
		tick = 0;
		treeThread = new BukkitRunnable() {

			@Override
			public void run() {
				tick++;
				if (playerTreeMode == null && toPlant.size() > 0 && tick % tickTime == 0) {
					TreeReplant tr = toPlant.poll();
					Bukkit.getScheduler().runTask(Farm.get(), () -> {
						grow(tr);
					});
				}
				if (tick % 5 == 0)
					center.getWorld().spawnParticle(Particle.REDSTONE, center, 10, 0, 0, 0, 0,
							playerTreeMode == null ? white : purple);
			}
		};
		treeThread.runTaskTimerAsynchronously(Farm.get(), 1, 1);
	}

	private Pattern space = Pattern.compile(" ");

	private void reloadAll() {
		reloadAllThread = Bukkit.getScheduler().runTaskAsynchronously(Farm.get(), () -> {
			try {
				if (!logFile.exists()) {
					logFile.getParentFile().mkdirs();
					try {
						logFile.createNewFile();
					} catch (IOException ex) {
						Bukkit.getLogger().log(Level.SEVERE, "Error while creating " + logFile.getName() + " file", ex);
						return;
					}
				}
				HashMap<Location, TreeReplant> logs = new HashMap<>();
				// Load all blocks
				try (BufferedReader br = new BufferedReader(new FileReader(logFile))) {
					String line;
					while ((line = br.readLine()) != null) {
						String[] word = space.split(line);
						if (word.length != 5) {
							Bukkit.getLogger().log(Level.WARNING, "Cannot parse line " + line + " !");
							continue;
						}
						int x;
						int y;
						int z;
						Material mat;
						Axis axis;
						try {
							x = Integer.parseInt(word[0]);
							y = Integer.parseInt(word[1]);
							z = Integer.parseInt(word[2]);
							mat = Material.valueOf(word[3]);
							axis = Axis.valueOf(word[4]);
							Location loc = new Location(center.getWorld(), x, y, z);
							logs.put(loc, new TreeReplant(loc, mat, axis));
						} catch (Exception ex) {
							Bukkit.getLogger().log(Level.WARNING, "Cannot parse line " + line + " !");
							continue;
						}
					}
				}
				this.logs.clear();
				this.logs = logs;
				// At start, put every tree to their original place
				Bukkit.getScheduler().runTask(Farm.get(), () -> {
					for (TreeReplant tr : logs.values())
						grow(tr);
				});
				toPlant.clear();
			} catch (Exception ex) {
				Bukkit.getLogger().log(Level.SEVERE, "An error has occured while retrieving all block", ex);
			}
		});
	}

	public void save(Runnable then) {
		Bukkit.getScheduler().runTaskAsynchronously(Farm.get(), () -> {
			try {
				if (!logFile.exists()) {
					logFile.getParentFile().mkdirs();
					try {
						logFile.createNewFile();
					} catch (IOException ex) {
						Bukkit.getLogger().log(Level.SEVERE, "Error while creating " + logFile.getName() + " file", ex);
						return;
					}
				}
				// Save all blocks
				StringBuilder sb = new StringBuilder();
				for (TreeReplant tr : logs.values()) {
					Location loc = tr.getLocation();
					sb.append(loc.getBlockX()).append(" ");
					sb.append(loc.getBlockY()).append(" ");
					sb.append(loc.getBlockZ()).append(" ");
					sb.append(tr.getMaterial().name()).append(" ");
					sb.append(tr.getDirection()).append('\n');
				}
				try (FileWriter fw = new FileWriter(logFile)) {
					fw.write(sb.toString());
					fw.flush();
				}
				then.run();
			} catch (Exception ex) {
				Bukkit.getLogger().log(Level.SEVERE, "An error has occured while saving trees", ex);
			}
		});
	}

	/**
	 * Add a block in the list and set Metadatas
	 * 
	 * @param loc
	 *                      The location
	 * @param m
	 *                      The material
	 * @param direction
	 *                      The direction of log
	 */
	public void add(Location loc, Material m, Axis direction) {
		toPlant.add(new TreeReplant(loc, m, direction));
	}

	/**
	 * Grow
	 */
	private void grow(TreeReplant tr) {
		Block block = center.getWorld().getBlockAt(tr.getLocation());
		if (block.getType() == Material.AIR) {
			BlockData data = block.getBlockData();
			if (!(data instanceof Orientable))
				data = tr.getMaterial().createBlockData();
			// Here data is Orientable
			Orientable or = (Orientable) data;
			or.setAxis(tr.getDirection());
			block.setBlockData(or);
			// Particle line from center to this block
			Util.makeParticleLine(Particle.REDSTONE, center, block.getLocation(), 1, white);
		}
	}

	/**
	 * Start tree mode for player p
	 * 
	 * @param p
	 *              The player
	 */
	public void treeMode(Player p) {
		playerTreeMode = p;
		showTreeMode = true;
		for (TreeReplant tr : toPlant)
			grow(tr);
		toPlant.clear();
	}

	/**
	 * Stop tree mode and reload all
	 */
	public void stopTreeMode() {
		playerTreeMode = null;
		showTreeMode = true;
		reloadAll();
	}

	/**
	 * Add a LOG block
	 */
	public void addBlockTreeMode(Block b) {
		if (playerTreeMode == null)
			return;
		if (b.getType() != Material.ACACIA_LOG && b.getType() != Material.BIRCH_LOG
				&& b.getType() != Material.DARK_OAK_LOG && b.getType() != Material.JUNGLE_LOG
				&& b.getType() != Material.OAK_LOG && b.getType() != Material.SPRUCE_LOG) {
			return;
		}
		Axis axis = Axis.Y;
		if (b.getBlockData() instanceof Orientable)
			axis = ((Orientable) b.getBlockData()).getAxis();
		logs.put(b.getLocation(), new TreeReplant(b.getLocation(), b.getType(), axis));
		playerTreeMode.playSound(playerTreeMode.getLocation(), Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 0.4f, 1.7f);
		System.out.println("Adding ...");
		if (!showTreeMode)
			b.setType(Material.AIR);
	}

	public void removeBlockTreeMode(Location loc) {
		if (playerTreeMode == null)
			return;
		TreeReplant tr = logs.remove(loc);
		playerTreeMode.playSound(playerTreeMode.getLocation(), Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 0.4f, 1.7f);
		System.out.println("Removing ...");
		if (showTreeMode && tr != null)
			grow(tr);
	}

	public boolean treeModeCmd(String cmd) {
		switch (cmd) {
		case "save":
			playerTreeMode.sendMessage(__.PREFIX + ChatColor.GREEN + "Saving ...");
			save(() -> {
				playerTreeMode.sendMessage(__.PREFIX + ChatColor.GREEN + "Saved ...");
			});
			return true;
		case "show":
			Bukkit.getScheduler().runTask(Farm.get(), () -> {
				if (showTreeMode) {
					playerTreeMode.sendMessage(__.PREFIX + ChatColor.RED + "Already on");
					return;
				}
				showTreeMode = true;
				for (TreeReplant tr : logs.values())
					// Show
					grow(tr);
				playerTreeMode.sendMessage(__.PREFIX + ChatColor.GREEN + "Switched on");
			});
			return true;
		case "hide":
			Bukkit.getScheduler().runTask(Farm.get(), () -> {
				if (!showTreeMode) {
					playerTreeMode.sendMessage(__.PREFIX + ChatColor.RED + "Already off");
					return;
				}
				showTreeMode = false;
				for (TreeReplant tr : logs.values()) {
					// Hide
					center.getWorld().getBlockAt(tr.getLocation()).setType(Material.AIR);
				}
				playerTreeMode.sendMessage(__.PREFIX + ChatColor.GREEN + "Switched off");
			});
			return true;
		}
		return false;
	}

	/**
	 * Remove
	 */
	public void remove() {
		logFile.delete();
	}

	/**
	 * Stop reloadAll Thread and tree Thread if running
	 */
	public void stop() {
		stop = true;
		if (reloadAllThread != null && Bukkit.getScheduler().isCurrentlyRunning(reloadAllThread.getTaskId()))
			reloadAllThread.cancel();
		reloadAllThread = null;
		if (treeThread != null)
			treeThread.cancel();
		treeThread = null;
	}

	public boolean isInside(Location loc) {
		return logs.containsKey(loc);
	}

	public Location getCenter() {
		return center;
	}

	public int getTickTime() {
		return tickTime;
	}
}
