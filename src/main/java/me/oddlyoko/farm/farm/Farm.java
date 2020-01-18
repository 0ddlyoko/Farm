package me.oddlyoko.farm.farm;

import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Map.Entry;
import java.util.concurrent.CountDownLatch;
import java.util.logging.Level;

import org.bukkit.Bukkit;
import org.bukkit.Chunk;
import org.bukkit.ChunkSnapshot;
import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Particle.DustOptions;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.data.Ageable;
import org.bukkit.block.data.BlockData;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;

import me.oddlyoko.farm.Util;

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
public class Farm {
	private Location center;
	private int radius;
	private Type type;
	private int tickTime;

	// A LinkedList of plants to replant
	private LinkedList<Block> toPlant;
	// An ArrayList containing plants that is growing
	private ArrayList<Block> toGrow;
	// Thread to reload
	private BukkitTask reloadAllThread;
	// The Farm Thread
	private BukkitRunnable farmThread;
	// Loading
	private final DustOptions red = new Particle.DustOptions(Color.RED, 1);
	// Ok
	private final DustOptions white = new Particle.DustOptions(Color.WHITE, 1);
	// The current chunk that is inspected by the reloadAll() method
	private Chunk currentChunk;
	private boolean stop;

	public Farm(Location center, int radius, Type type, int tickTime) {
		this.center = center;
		this.radius = radius;
		this.type = type;
		this.tickTime = tickTime;
		stop = false;
		toPlant = new LinkedList<>();
		toGrow = new ArrayList<>();
		reloadAll();
		initRunnable();
	}

	private int tick;

	private void initRunnable() {
		tick = 0;
		farmThread = new BukkitRunnable() {

			@Override
			public void run() {
				if (stop)
					cancel();
				tick++;
				if (toPlant.size() > 0 && tick % tickTime == 0) {
					Block b = toPlant.poll();
					if (b != null && b.getType() == Material.AIR) {
						Bukkit.getScheduler().runTask(me.oddlyoko.farm.Farm.get(), () -> {
							Block under = b.getWorld().getBlockAt(b.getLocation().getBlockX(),
									b.getLocation().getBlockY() - 1, b.getLocation().getBlockZ());
							if (under != null && under.getType() == Material.FARMLAND) {
								toGrow.add(b);
								b.setType(type.getType());
								// Particle line from center to this block
								Util.makeParticleLine(Particle.REDSTONE, center, b.getLocation(), 1, white);
							}
						});
					}
				}
				if (toGrow.size() > 0 && tick % 8 == 0) {
					// Copy to prevent Modification Exception
					List<Entry<Block, Integer>> toUpdate = new ArrayList<>();
					for (Block b : new ArrayList<>(toGrow)) {
						// Don't update if chunk is not loaded
						if (!b.getChunk().isLoaded())
							continue;
						if (b.getBlockData() instanceof Ageable) {
							Ageable age = (Ageable) b.getBlockData();
							int a = age.getAge() + 1;
							if (a > age.getMaximumAge()) {
								toGrow.remove(b);
								continue;
							}
							toUpdate.add(new AbstractMap.SimpleEntry<Block, Integer>(b, a));
						} else
							toGrow.remove(b);
					}
					Bukkit.getScheduler().runTask(me.oddlyoko.farm.Farm.get(), () -> {
						for (Entry<Block, Integer> e : toUpdate) {
							if (!(e.getKey() instanceof Ageable))
								continue;
							Ageable age = (Ageable) e.getKey().getBlockData();
							age.setAge(e.getValue());
							e.getKey().setBlockData(age);
						}
					});
				}
				if (tick % 5 == 0) {
					center.getWorld().spawnParticle(Particle.REDSTONE, center, 10, 0, 0, 0, 0, white);
					if (currentChunk != null) {
						int cX = currentChunk.getX();
						int cZ = currentChunk.getZ();
						Util.makeParticleLine(Particle.REDSTONE, center,
								new Location(center.getWorld(), (cX << 4), 72, (cZ << 4)), 1, red);
						Util.makeParticleLine(Particle.REDSTONE, center,
								new Location(center.getWorld(), (cX << 4), 72, (cZ << 4) + 15), 1, red);
						Util.makeParticleLine(Particle.REDSTONE, center,
								new Location(center.getWorld(), (cX << 4) + 15, 72, (cZ << 4)), 1, red);
						Util.makeParticleLine(Particle.REDSTONE, center,
								new Location(center.getWorld(), (cX << 4) + 15, 72, (cZ << 4) + 15), 1, red);
					}
				}
			}
		};
		farmThread.runTaskTimerAsynchronously(me.oddlyoko.farm.Farm.get(), 1, 1);
	}

	/**
	 * Check for every blocks around center and within a radius if the block is
	 * Material.SOIL type and above block is Material.AIR or an available block
	 * 
	 * @see Type#is(Material)
	 */
	public void reloadAll() {
		reloadAllThread = Bukkit.getScheduler().runTaskAsynchronously(me.oddlyoko.farm.Farm.get(), () -> {
			try {
				World w = center.getWorld();
				int centerX = center.getBlockX();
				int centerY = center.getBlockY();
				int centerZ = center.getBlockZ();
				int radiusSquare = radius * radius;

				// We calculate the two chunks at the opposite side
				int chunkX = (centerX - radius) >> 4;
				int chunkZ = (centerZ - radius) >> 4;
				int endChunkX = (centerX + radius) >> 4;
				int endChunkZ = (centerZ + radius) >> 4;
				// Variables
				Material m;
				// X position of block
				int realX;
				// Z position of block
				int realZ;
				// Pythagore: Hypothenuse = sqrt( x^2 + z^2 )
				// Minecraft is a 3D world so Hypothenuse = sqrt( x^2 + y^2 + z^2 )
				// So Hypothenuse^2 = (x^2 + y^2 + z^2)
				// Hypothenuse = distance between (x1, y1, z1) and (x2, y2, z2) so :
				// distance^2 = ((x2 - x1) ^ 2) + ((y2 - y1) ^ 2) + ((z2 - z1) ^ 2))
				// The x distance between the block and the center ^ 2
				int distX;
				// The y distance between the block and the center ^ 2
				int distY;
				// The z distance between the block and the center ^ 2
				int distZ;
				// The addition of distX and distZ
				int distXZ;
				// We'll loop for each chunk
				for (int cX = chunkX; cX <= endChunkX && !stop; cX++) {
					for (int cZ = chunkZ; cZ <= endChunkZ && !stop; cZ++) {
						// Load in sync and wait for it
						// Waawwww Java
						final int copyX = cX;
						final int copyZ = cZ;
						// Used to wait
						CountDownLatch latch = new CountDownLatch(1);
						Bukkit.getScheduler().runTask(me.oddlyoko.farm.Farm.get(), () -> {
							// Here we make a copy of the chunk to use it in sync
							currentChunk = w.getChunkAt(copyX, copyZ);
							currentChunk.load();
							latch.countDown();
						});
						latch.await();
						ChunkSnapshot c = currentChunk.getChunkSnapshot();
						// System.out.println("Chunk at x = " + cX + ", z = " + cZ);
						for (int x = 0; x <= 15; x++) {
							realX = (cX << 4) + x;
							distX = (realX - centerX) * (realX - centerX);
							for (int z = 0; z <= 15; z++) {
								realZ = (cZ << 4) + z;
								distZ = (realZ - centerZ) * (realZ - centerZ);
								distXZ = distX + distZ;
								// The minimum y is maximum between 0 and centerY - (radius / 2)
								// The maximum y is minimum between 255, centerY + (radius / 2) and highestBlock
								for (int y = Math.max(0, centerY - (radius / 2)); y <= Math.min(centerY + (radius / 2),
										Math.min(255, c.getHighestBlockYAt(x, z))); y++) {
									distY = (y - centerY) * (y - centerY);
									// Test if we're in the sphere
									if (distXZ + distY <= radiusSquare) {
										// Test if block is SOIL
										if (c.getBlockType(x, y, z) == Material.FARMLAND) {

											// Test if it's air or the material that will grow
											m = c.getBlockType(x, y + 1, z);
											if (m == Material.AIR) {
												add(currentChunk.getBlock(x, y + 1, z));
												// This block isn't SOIL so skip this one
												y++;
											} else if (Type.is(m)) {
												// Testing if it's grown
												BlockData bd = c.getBlockData(x, y + 1, z);
												if (bd instanceof Ageable) {
													Ageable age = (Ageable) bd;
													if (age.getAge() != age.getMaximumAge())
														toGrow.add(currentChunk.getBlock(x, y + 1, z));
												}
												// This block isn't SOIL so skip this one
												y++;
											}
										}
									}
								}
							}
						}
					}
				}
			} catch (Exception ex) {
				Bukkit.getLogger().log(Level.SEVERE, "An error has occured while retrieving all block", ex);
			} finally {
				currentChunk = null;
			}
		});
	}

	public void add(Block b) {
		if (toGrow.contains(b))
			toGrow.remove(b);
		toPlant.add(b);
	}

	/**
	 * Stop reloadAll Thread and farm Thread if running
	 */
	public void stop() {
		stop = true;
		if (reloadAllThread != null && Bukkit.getScheduler().isCurrentlyRunning(reloadAllThread.getTaskId()))
			reloadAllThread.cancel();
		reloadAllThread = null;
	}

	public boolean isInside(Location loc) {
		double distX = loc.getX() - center.getX();
		double distY = loc.getY() - center.getY();
		double distZ = loc.getZ() - center.getZ();
		return loc.getWorld() == center.getWorld()
				&& (distX * distX + distY * distY + distZ * distZ) <= radius * radius;
	}

	public Location getCenter() {
		return center;
	}

	public int getRadius() {
		return radius;
	}

	public Type getType() {
		return type;
	}

	public int getTickTime() {
		return tickTime;
	}

	public enum Type {
		POTATOES(Material.POTATOES, 7), // Potatoes
		CARROTS(Material.CARROTS, 7), // Carrots
		WHEAT(Material.WHEAT, 7), // Wheats
		BEETROOT(Material.BEETROOTS, 3); // Beetroots

		private Material type;
		private int maxGrow;

		private Type(Material type, int maxGrow) {
			this.type = type;
			this.maxGrow = maxGrow;
		}

		public Material getType() {
			return type;
		}

		public int getMaxGrow() {
			return maxGrow;
		}

		/**
		 * Check if the material is the same as type
		 * 
		 * @param m
		 *              The material to check
		 * @return true if material is one of the type listed before
		 */
		public static boolean is(Material m) {
			return m == Material.POTATOES || m == Material.CARROTS || m == Material.WHEAT || m == Material.BEETROOTS;
		}
	}
}
