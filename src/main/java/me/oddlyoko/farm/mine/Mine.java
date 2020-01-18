package me.oddlyoko.farm.mine;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.logging.Level;
import java.util.regex.Pattern;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;

import me.oddlyoko.farm.Farm;
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
public class Mine {
	private World world;
	private Type type;
	private int tickTime;
	private int percent;

	// The number of special blocks (IRON, GOLD, DIAMOND, ...)
	private int nbrSpecialBlocks;
	// The maximum number of special blocks
	private int maxNbrSpecialBlocks;
	// An HashMap containing blocks that will be replaced to stone
	private HashMap<Location, MineBlock> toReplace;
	// The file that contains all stone
	private File mineFile;
	// An array containing all blocks
	private List<Block> blocks;
	// Thread to reload
	private BukkitTask reloadAllThread;
	// The Mine Thread
	private BukkitRunnable mineThread;
	// The player that is using mine mode
	private Player playerMineMode;
	// If false, hide blocks that are registered
	private boolean showMineMode;
	private boolean stop;

	public Mine(World world, Type type, int tickTime, int percent) {
		this.world = world;
		this.type = type;
		this.tickTime = tickTime;
		this.percent = percent;
		mineFile = new File(
				"plugins" + File.separator + "Farm" + File.separator + "mine" + File.separator + "mine_" + type);
		stop = false;
		toReplace = new HashMap<>();
		reloadAll();
		initRunnable();
	}

	private void initRunnable() {
		mineThread = new BukkitRunnable() {

			@Override
			public void run() {
				if (stop || playerMineMode != null)
					return;
				if (toReplace.size() > 0) {
					synchronized (toReplace) {
						// Iterate over all blocks
						Iterator<MineBlock> it = toReplace.values().iterator();
						while (it.hasNext()) {
							MineBlock mb = it.next();
							if (mb.tick()) {
								// Replace and remove
								Bukkit.getScheduler().runTask(Farm.get(), () -> {
									mb.getBlock().setType(Material.STONE);
								});
								it.remove();
							}
						}
					}
				}
				if (blocks != null && blocks.size() != 0 && nbrSpecialBlocks < maxNbrSpecialBlocks) {
					// Add one special block
					final Block b = blocks.get((int) (Math.random() * blocks.size()));
					synchronized (toReplace) {
						if (!toReplace.containsKey(b.getLocation()) && b.getType() == Material.STONE) {
							Bukkit.getScheduler().runTask(Farm.get(), () -> {
								b.setType(type.material);
							});
							nbrSpecialBlocks++;
						}
					}
				}
			}
		};
		mineThread.runTaskTimerAsynchronously(Farm.get(), 1, 1);
	}

	private Pattern space = Pattern.compile(" ");

	private void reloadAll() {
		reloadAllThread = Bukkit.getScheduler().runTaskAsynchronously(Farm.get(), () -> {
			try {
				stop = true;
				if (!mineFile.exists()) {
					mineFile.getParentFile().mkdirs();
					try {
						mineFile.createNewFile();
					} catch (IOException ex) {
						Bukkit.getLogger().log(Level.SEVERE, "Error while creating " + mineFile.getName() + " file",
								ex);
						return;
					}
				}
				List<Location> locs = new ArrayList<>();
				// Load all blocks
				try (BufferedReader br = new BufferedReader(new FileReader(mineFile))) {
					String line;
					while ((line = br.readLine()) != null) {
						String[] word = space.split(line);
						if (word.length != 3) {
							Bukkit.getLogger().warning("Cannot parse line " + line + " !");
							continue;
						}
						int x;
						int y;
						int z;
						try {
							x = Integer.parseInt(word[0]);
							y = Integer.parseInt(word[1]);
							z = Integer.parseInt(word[2]);
							Location loc = new Location(world, x, y, z);
							locs.add(loc);
						} catch (Exception ex) {
							Bukkit.getLogger().warning("Cannot parse line " + line + " !");
							continue;
						}
					}
				}
				this.nbrSpecialBlocks = 0;
				this.maxNbrSpecialBlocks = locs.size() * percent / 100;
				this.blocks = new ArrayList<>(locs.size());
				// Used to wait
				CountDownLatch latch = new CountDownLatch(1);
				// At start, put every block to their original place
				Bukkit.getScheduler().runTask(Farm.get(), () -> {
					try {
						for (Location l : locs) {
							Block b = world.getBlockAt(l);
							b.setType(Material.STONE);
							blocks.add(b);
						}
					} finally {
						latch.countDown();
					}
				});
				latch.await();
			} catch (Exception ex) {
				Bukkit.getLogger().log(Level.SEVERE, "An error has occured while retrieving all block", ex);
			} finally {
				synchronized (toReplace) {
					toReplace = new HashMap<>();
				}
				stop = false;
			}
		});
	}

	public void save(Runnable then) {
		Bukkit.getScheduler().runTaskAsynchronously(Farm.get(), () -> {
			try {
				if (!mineFile.exists()) {
					mineFile.getParentFile().mkdirs();
					try {
						mineFile.createNewFile();
					} catch (IOException ex) {
						Bukkit.getLogger().log(Level.SEVERE, "Error while creating " + mineFile.getName() + " file",
								ex);
						return;
					}
				}
				// Save all blocks
				StringBuilder sb = new StringBuilder();
				for (Block b : blocks) {
					sb.append(b.getLocation().getBlockX()).append(" ");
					sb.append(b.getLocation().getBlockY()).append(" ");
					sb.append(b.getLocation().getBlockZ()).append('\n');
				}
				try (FileWriter fw = new FileWriter(mineFile)) {
					fw.write(sb.toString());
					fw.flush();
				}
				then.run();
			} catch (Exception ex) {
				Bukkit.getLogger().log(Level.SEVERE, "An error has occured while saving mines", ex);
			}
		});
	}

	public void mine(Block b) {
		switch (b.getType()) {
		case STONE:
			Bukkit.getScheduler().runTask(Farm.get(), () -> {
				b.setType(Material.COBBLESTONE);
			});
			break;
		case COBBLESTONE:
			Bukkit.getScheduler().runTask(Farm.get(), () -> {
				b.setType(Material.BEDROCK);
			});
			break;
		case COAL_ORE:
		case DIAMOND_ORE:
		case EMERALD_ORE:
		case GOLD_ORE:
		case IRON_ORE:
		case LAPIS_ORE:
		case REDSTONE_ORE:
		case NETHER_QUARTZ_ORE:
			nbrSpecialBlocks--;
			Bukkit.getScheduler().runTask(Farm.get(), () -> {
				b.setType(Material.STONE);
			});
			return;
		default:
			return;
		}
		synchronized (toReplace) {
			toReplace.put(b.getLocation(), new MineBlock(b, tickTime));
		}
	}

	/**
	 * Start mine mode for player p
	 * 
	 * @param p
	 *              The player
	 */
	public void mineMode(Player p) {
		playerMineMode = p;
		showMineMode = true;
		synchronized (toReplace) {
			for (MineBlock mb : toReplace.values())
				mb.getBlock().setType(Material.STONE);
			toReplace.clear();
			nbrSpecialBlocks = 0;
		}
	}

	/**
	 * Stop mine mode and reload all
	 */
	public void stopMineMode() {
		showMineMode = true;
		for (Block b : blocks)
			// Show
			b.setType(Material.STONE);
		playerMineMode = null;
		reloadAll();
	}

	/**
	 * Add a block
	 */
	public void addBlockMineMode(Block b) {
		if (playerMineMode == null)
			return;
		if (b.getType() != Material.STONE && b.getType() != Material.COBBLESTONE && b.getType() != Material.BEDROCK
				&& b.getType() != Material.COAL_ORE && b.getType() != Material.DIAMOND_ORE
				&& b.getType() != Material.EMERALD_ORE && b.getType() != Material.GOLD_ORE
				&& b.getType() != Material.IRON_ORE && b.getType() != Material.LAPIS_ORE
				&& b.getType() != Material.REDSTONE_ORE && b.getType() != Material.NETHER_QUARTZ_ORE
				&& b.getType() != Material.SPONGE)
			return;
		blocks.add(b);
		playerMineMode.playSound(playerMineMode.getLocation(), Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 0.4f, 1.7f);
		if (!showMineMode)
			b.setType(Material.SPONGE);
	}

	public void removeBlockMineMode(Block block) {
		if (playerMineMode == null || block == null)
			return;
		blocks.remove(block);
		block.setType(Material.STONE);
	}

	public boolean mineModeCmd(String cmd) {
		switch (cmd) {
		case "save":
			playerMineMode.sendMessage(__.PREFIX + ChatColor.GREEN + "Saving ...");
			save(() -> {
				playerMineMode.sendMessage(__.PREFIX + ChatColor.GREEN + "Saved");
			});
			return true;
		case "show":
			if (showMineMode)
				playerMineMode.sendMessage(__.PREFIX + ChatColor.RED + "Already on");
			else
				Bukkit.getScheduler().runTask(Farm.get(), () -> {
					showMineMode = true;
					for (Block b : blocks)
						// Show
						b.setType(Material.STONE);
				});
			return true;
		case "hide":
			if (!showMineMode)
				playerMineMode.sendMessage(__.PREFIX + ChatColor.RED + "Already off");
			else
				Bukkit.getScheduler().runTask(Farm.get(), () -> {
					showMineMode = false;
					for (Block b : blocks)
						// Hide
						b.setType(Material.SPONGE);
					playerMineMode.sendMessage(__.PREFIX + ChatColor.GREEN + "Switched off");
				});
			return true;
		}
		return false;
	}

	public void clear() {
		blocks = new ArrayList<>();
		nbrSpecialBlocks = 0;
		maxNbrSpecialBlocks = 0;
		tickTime = 100;
		percent = 5;
		mineFile.delete();
	}

	public void stop() {
		stop = true;
		if (reloadAllThread != null && Bukkit.getScheduler().isCurrentlyRunning(reloadAllThread.getTaskId()))
			reloadAllThread.cancel();
		reloadAllThread = null;
		if (mineThread != null)
			mineThread.cancel();
		mineThread = null;
	}

	/**
	 * Check if specific location is a block for that mine.<br />
	 * !!!! This method doesn't check the world
	 * 
	 * @param loc
	 *                The Location
	 * @return true if location match
	 */
	public boolean isInside(Location loc) {
		for (Block b : blocks)
			if (b.getLocation().getBlockX() == loc.getBlockX() && b.getLocation().getBlockY() == loc.getBlockY()
					&& b.getLocation().getBlockZ() == loc.getBlockZ())
				return true;
		return false;
	}

	public Type getType() {
		return type;
	}

	public int getTickTime() {
		return tickTime;
	}

	public void setTickTime(int tickTime) {
		this.tickTime = tickTime;
	}

	public int getPercent() {
		return percent;
	}

	public void setPercent(int percent) {
		this.percent = percent;
	}

	public enum Type {
		STONE(Material.STONE), //
		COAL(Material.COAL_ORE), //
		IRON(Material.IRON_ORE), //
		GOLD(Material.GOLD_ORE), //
		LAPIS(Material.LAPIS_ORE), //
		REDSTONE(Material.REDSTONE_ORE), //
		NETHER_QUARTZ(Material.NETHER_QUARTZ_ORE), //
		EMERALD(Material.EMERALD_ORE), //
		DIAMOND(Material.DIAMOND_ORE); //

		private Material material;

		private Type(Material material) {
			this.material = material;
		}

		public Material getMaterial() {
			return material;
		}

		public boolean is(Material m) {
			return m == material;
		}
	}
}
