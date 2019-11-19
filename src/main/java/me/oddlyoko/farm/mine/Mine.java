package me.oddlyoko.farm.mine;

import java.util.HashMap;
import java.util.Iterator;
import java.util.concurrent.CountDownLatch;
import java.util.logging.Level;

import org.bukkit.Bukkit;
import org.bukkit.Chunk;
import org.bukkit.ChunkSnapshot;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;

import com.sk89q.worldedit.bukkit.BukkitWorld;
import com.sk89q.worldedit.math.BlockVector3;
import com.sk89q.worldguard.WorldGuard;
import com.sk89q.worldguard.protection.managers.RegionManager;
import com.sk89q.worldguard.protection.regions.ProtectedRegion;
import com.sk89q.worldguard.protection.regions.RegionContainer;

import me.oddlyoko.farm.Farm;

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
	private String worldName;
	private String regionName;
	private Type type;
	private int tickTime;
	private int percent;

	// The number of special blocks (IRON, GOLD, DIAMOND, ...)
	private int nbrSpecialBlocks;
	// The maximum number of special blocks
	private int maxNbrSpecialBlocks;
	// An HashMap containing blocks that will be replaced to stone
	private HashMap<Location, MineBlock> toReplace;
	// An HashMap containing all stone blocks
	private HashMap<Location, Block> blocks;
	// An array of blocks (blocks.values)
	private Block[] arrBlocks;
	// Thread to reload
	private BukkitTask reloadAllThread;
	// The Mine Thread
	private BukkitRunnable mineThread;
	// The current chunk that is inspected by the reload() method
	private Chunk currentChunk;
	// The min Vector3
	private BlockVector3 min;
	// The max Vector3
	private BlockVector3 max;
	private boolean stop;

	public Mine(String worldName, String regionName, Type type, int tickTime, int percent) {
		this.worldName = worldName;
		this.regionName = regionName;
		this.type = type;
		this.tickTime = tickTime;
		this.percent = percent;
		stop = false;
		toReplace = new HashMap<>();
		blocks = new HashMap<>();
		reloadAll();
		initRunnable();
	}

	private void initRunnable() {
		mineThread = new BukkitRunnable() {

			@Override
			public void run() {
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
				if (arrBlocks != null && nbrSpecialBlocks <= maxNbrSpecialBlocks) {
					// Add one special block
					final Block b = arrBlocks[(int) (Math.random() * arrBlocks.length)];
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

	private void reloadAll() {
		reloadAllThread = Bukkit.getScheduler().runTaskAsynchronously(Farm.get(), () -> {
			try {
				RegionContainer rc = WorldGuard.getInstance().getPlatform().getRegionContainer();
				World w = Bukkit.getWorld(worldName);
				if (w == null) {
					Bukkit.getLogger().log(Level.SEVERE, "World " + worldName + " not found");
					return;
				}
				RegionManager rm = rc.get(new BukkitWorld(w));
				if (rm == null) {
					Bukkit.getLogger().log(Level.SEVERE, "Cannot get RegionManager for world " + worldName);
					return;
				}
				if ("__global__".equalsIgnoreCase(regionName)) {
					Bukkit.getLogger().log(Level.SEVERE,
							"Cannot use __global__ as region for mine. Please choose another region");
					return;
				}
				ProtectedRegion region = rm.getRegion(regionName);
				if (region == null) {
					Bukkit.getLogger().log(Level.SEVERE, "Region " + regionName + " not found");
					return;
				}
				min = region.getMinimumPoint();
				max = region.getMaximumPoint();
				// Two chunks at the opposite side
				int chunkX = min.getBlockX() >> 4;
				int chunkZ = min.getBlockZ() >> 4;
				int endChunkX = max.getBlockX() >> 4;
				int endChunkZ = max.getBlockZ() >> 4;
				// Variables
				Material m;
				HashMap<Location, Block> blocks = new HashMap<>();
				// We'll loop for each chunk
				for (int cX = chunkX; cX <= endChunkX && !stop; cX++) {
					for (int cZ = chunkZ; cZ <= endChunkZ && !stop; cZ++) {
						// Load in sync and wait for it
						// Waawwww Java, I love it <3
						final int copyX = cX;
						final int copyZ = cZ;
						// Used to wait
						CountDownLatch latch = new CountDownLatch(1);
						Bukkit.getScheduler().runTask(Farm.get(), () -> {
							// Here we make a copy of the chunk to use it in sync
							currentChunk = w.getChunkAt(copyX, copyZ);
							currentChunk.load();
							latch.countDown();
						});
						latch.await();
						ChunkSnapshot c = currentChunk.getChunkSnapshot();
						for (int x = 0; x <= 15; x++) {
							for (int z = 0; z <= 15; z++) {
								// The minimum y is maximum between 0 and min.getY()
								// The maximum y is minimum between 255, max.getY() and highestBlock
								for (int y = Math.max(0, min.getBlockY()); y <= Math.min(max.getBlockY(),
										Math.min(255, c.getHighestBlockYAt(x, z))); y++) {
									m = c.getBlockType(x, y, z);
									if (m == Material.STONE || m == Material.COBBLESTONE || m == Material.BEDROCK
											|| m == Material.COAL_ORE || m == Material.DIAMOND_ORE
											|| m == Material.EMERALD_ORE || m == Material.GOLD_ORE
											|| m == Material.IRON_ORE || m == Material.LAPIS_ORE
											|| m == Material.REDSTONE_ORE || m == Material.NETHER_QUARTZ_ORE) {
										Block b = currentChunk.getBlock(x, y, z);
										blocks.put(b.getLocation(), b);
										if (m != Material.STONE)
											synchronized (toReplace) {
												toReplace.put(b.getLocation(), new MineBlock(b, 1));
											}
									}
								}
							}
						}
					}
				}
				this.blocks = blocks;
				this.arrBlocks = blocks.values().toArray(new Block[0]);
				this.nbrSpecialBlocks = 0;
				this.maxNbrSpecialBlocks = blocks.size() * percent / 100;
			} catch (Exception ex) {
				Bukkit.getLogger().log(Level.SEVERE, "An error has occured while retrieving all block", ex);
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

	public void stop() {
		stop = true;
		if (reloadAllThread != null && Bukkit.getScheduler().isCurrentlyRunning(reloadAllThread.getTaskId()))
			reloadAllThread.cancel();
		reloadAllThread = null;
		if (mineThread != null)
			mineThread.cancel();
		mineThread = null;
	}

	public boolean isInside(Location loc) {
		return blocks.containsKey(loc);
	}

	public boolean isBetween(Location loc) {
		return loc.getWorld().getName().equalsIgnoreCase(worldName) && loc.getX() >= min.getBlockX()
				&& loc.getX() <= max.getBlockX() && loc.getY() >= min.getBlockY() && loc.getY() <= max.getBlockY()
				&& loc.getZ() >= min.getBlockZ() && loc.getZ() <= max.getBlockZ();
	}

	public String getWorldName() {
		return worldName;
	}

	public String getRegionName() {
		return regionName;
	}

	public Type getType() {
		return type;
	}

	public int getTickTime() {
		return tickTime;
	}

	public int getPercent() {
		return percent;
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
