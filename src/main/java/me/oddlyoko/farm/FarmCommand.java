package me.oddlyoko.farm;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import me.oddlyoko.farm.mine.Mine;
import me.oddlyoko.farm.tree.Tree;

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
public class FarmCommand implements CommandExecutor {

	@Override
	public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
		if ("farm".equalsIgnoreCase(command.getName())) {
			if (args.length == 0 || "help".equalsIgnoreCase(args[0])) {
				sender.sendMessage(ChatColor.YELLOW + "-----------[" + ChatColor.GOLD + "Farm" + ChatColor.YELLOW
						+ "]-----------");
				sender.sendMessage(
						ChatColor.AQUA + "- /farm info" + ChatColor.YELLOW + " : See informations about plugin");
				if (sender.hasPermission("farm.create")) {
					sender.sendMessage(ChatColor.AQUA
							+ "- /farm <add|create> farm [radius] [time] [carrots|potatoes|wheat|beetroot]"
							+ ChatColor.YELLOW + " : Add a new Farm");
					sender.sendMessage(ChatColor.AQUA + "- /farm <add|create> tree [time]" + ChatColor.YELLOW
							+ " : Add a new Tree");
					sender.sendMessage(ChatColor.AQUA
							+ "- /farm <add|create> mine <world> <region> [stone|coal|iron|gold|lapis|redstone|nether_quartz|emerald|diamond] [time] [percent]"
							+ ChatColor.YELLOW + " : Add a new Mine");
				}
				if (sender.hasPermission("farm.remove"))
					sender.sendMessage(ChatColor.AQUA + "- /farm <remove> <farm|tree|mine>" + ChatColor.YELLOW
							+ " : Remove a Farm / a Tree");
				if (sender.hasPermission("farm.tree"))
					sender.sendMessage(ChatColor.AQUA + "- /farm tree" + ChatColor.YELLOW
							+ " : Switch on tree mode on nearby tree");
				if (sender.hasPermission("farm.reload"))
					sender.sendMessage(ChatColor.AQUA + "- /farm reload" + ChatColor.YELLOW + " : Reload the plugin");
			} else if ("info".equalsIgnoreCase(args[0])) {
				sender.sendMessage(ChatColor.YELLOW + "-----------[" + ChatColor.GOLD + "Farm" + ChatColor.YELLOW
						+ "]-----------");
				sender.sendMessage(ChatColor.AQUA + "Created by 0ddlyoko");
				sender.sendMessage(ChatColor.GREEN + "v" + Farm.get().getDescription().getVersion());
				sender.sendMessage(ChatColor.AQUA + "https://www.0ddlyoko.be");
				sender.sendMessage(ChatColor.AQUA + "https://www.github.com/0ddlyoko");
			} else if ("create".equalsIgnoreCase(args[0]) || "add".equalsIgnoreCase(args[0])) {
				if (!(sender instanceof Player)) {
					sender.sendMessage(__.PREFIX + ChatColor.RED + "You must be a player to execute this command");
					return true;
				}
				Player p = (Player) sender;
				if (!p.hasPermission("farm.create")) {
					p.sendMessage(__.PREFIX + ChatColor.RED + "You don't have permission to execute this command");
					return true;
				}
				if (args.length <= 1) {
					p.sendMessage(__.PREFIX + ChatColor.RED + "Syntax: /farm <add|create> <farm|tree|mine> [...]");
					return true;
				}
				if ("farm".equalsIgnoreCase(args[1])) {
					int time = 20;
					Location loc = p.getLocation();
					int radius = 10;
					try {
						if (args.length >= 3) {
							radius = Integer.parseInt(args[2]);
							if (args.length >= 4)
								time = Integer.parseInt(args[3]);
						}
					} catch (Exception ex) {
						p.sendMessage(__.PREFIX + ChatColor.RED + "Please enter corrects values for radius / time");
						return true;
					}
					if (radius <= 0) {
						p.sendMessage(__.PREFIX + ChatColor.RED + "Please enter a correct radius");
						return true;
					}
					if (time <= 0) {
						p.sendMessage(__.PREFIX + ChatColor.RED + "Please enter a correct time");
						return true;
					}
					me.oddlyoko.farm.farm.Farm.Type type = me.oddlyoko.farm.farm.Farm.Type.CARROTS;
					if (args.length == 5)
						type = me.oddlyoko.farm.farm.Farm.Type.valueOf(args[4].toUpperCase());

					Farm.get().getFarmManager().addFarm(loc, radius, type, time);
					p.sendMessage(__.PREFIX + ChatColor.GREEN + "Farm successfully created !");
				} else if ("tree".equalsIgnoreCase(args[1])) {
					int time = 20;
					Location loc = p.getLocation();
					if (args.length >= 3)
						try {
							time = Integer.parseInt(args[2]);
						} catch (Exception ex) {
							p.sendMessage(__.PREFIX + ChatColor.RED + "Please enter a correct value for time");
							return true;
						}
					if (time <= 0) {
						p.sendMessage(__.PREFIX + ChatColor.RED + "Please enter a correct time");
						return true;
					}
					Farm.get().getTreeManager().addTree(loc, time);
					p.sendMessage(__.PREFIX + ChatColor.GREEN + "Tree successfully created !");
				} else if ("mine".equalsIgnoreCase(args[1])) {
					if (args.length <= 3) {
						p.sendMessage(__.PREFIX + ChatColor.RED
								+ "Syntax: /farm <add|create> mine <world> <region> [stone|coal|iron|gold|lapis|redstone|nether_quartz|emerald|diamond] [time] [percent]");
						return true;
					}
					String world = args[2];
					if (Bukkit.getWorld(world) == null) {
						p.sendMessage(__.PREFIX + ChatColor.RED + "World " + world + " not found");
						return true;
					}
					String region = args[3];
					Mine.Type type = Mine.Type.STONE;
					if (args.length >= 5)
						try {
							type = Mine.Type.valueOf(args[4]);
						} catch (Exception ex) {
							p.sendMessage(__.PREFIX + ChatColor.RED + "Incorrect type " + type);
							return true;
						}
					// 5 seconds
					int tickTime = 100;
					if (args.length >= 6)
						try {
							tickTime = Integer.parseInt(args[5]);
						} catch (Exception ex) {
							p.sendMessage(__.PREFIX + ChatColor.RED + "Please enter a correct value for time");
							return true;
						}
					if (tickTime <= 0) {
						p.sendMessage(__.PREFIX + ChatColor.RED + "Please enter a correct time");
						return true;
					}
					int percent = 5;
					if (args.length >= 7)
						try {
							percent = Integer.parseInt(args[6]);
						} catch (Exception ex) {
							p.sendMessage(__.PREFIX + ChatColor.RED + "Please enter a correct value for percent");
							return true;
						}
					if (percent < 0 || percent > 100) {
						p.sendMessage(__.PREFIX + ChatColor.RED + "Percent must be between 0 and 100");
						return true;
					}
					Farm.get().getMineManager().addMine(world, region, type, tickTime, percent);
					p.sendMessage(__.PREFIX + ChatColor.GREEN + "Mine successfully created !");
				}
			} else if ("remove".equalsIgnoreCase(args[0])) {
				if (!(sender instanceof Player)) {
					sender.sendMessage(__.PREFIX + ChatColor.RED + "You must be a player to execute this command");
					return true;
				}
				Player p = (Player) sender;
				if (!p.hasPermission("farm.remove")) {
					p.sendMessage(__.PREFIX + ChatColor.RED + "You don't have permission to execute this command");
					return true;
				}

				if (args.length <= 1) {
					p.sendMessage(__.PREFIX + ChatColor.RED + "Syntax: /farm remove <farm|tree>");
					return true;
				}

				if ("farm".equalsIgnoreCase(args[1])) {
					me.oddlyoko.farm.farm.Farm f = Farm.get().getFarmManager().getNearbyFarm(p.getLocation());
					if (f == null) {
						p.sendMessage(__.PREFIX + ChatColor.GREEN + "No farm found !");
						return true;
					}
					Farm.get().getFarmManager().removeFarm(f);
					p.sendMessage(__.PREFIX + ChatColor.GREEN + "Farm successfully removed !");
				} else if ("tree".equalsIgnoreCase(args[1])) {
					Tree t = Farm.get().getTreeManager().getNearbyTree(p.getLocation());
					if (t == null) {
						p.sendMessage(__.PREFIX + ChatColor.GREEN + "No tree found !");
						return true;
					}
					Farm.get().getTreeManager().removeTree(t);
					p.sendMessage(__.PREFIX + ChatColor.GREEN + "Tree successfully removed !");
				} else if ("mine".equalsIgnoreCase(args[1])) {
					Mine m = Farm.get().getMineManager().getInsideMine(p.getLocation());
					if (m == null) {
						p.sendMessage(__.PREFIX + ChatColor.RED + "No mine found !");
						return true;
					}
					Farm.get().getMineManager().removeMine(m);
					p.sendMessage(__.PREFIX + ChatColor.GREEN + "Mine successfully removed !");
				}

			} else if ("tree".equalsIgnoreCase(args[0])) {
				if (!(sender instanceof Player)) {
					sender.sendMessage(__.PREFIX + ChatColor.RED + "You must be a player to execute this command");
					return true;
				}
				Player p = (Player) sender;
				if (!p.hasPermission("farm.tree")) {
					p.sendMessage(__.PREFIX + ChatColor.RED + "You don't have permission to execute this command");
					return true;
				}
				Tree tree = Farm.get().getTreeManager().getNearbyTree(p.getLocation());
				Farm.get().getTreeManager().treeMode(p, tree);
			} else if ("reload".equalsIgnoreCase(args[0])) {
				if (!sender.hasPermission("farm.reload")) {
					sender.sendMessage(__.PREFIX + ChatColor.RED + "You don't have permission to execute this command");
					return true;
				}
				sender.sendMessage(__.PREFIX + ChatColor.GREEN + "Reloading ...");
				Farm.get().getFarmManager().reload();
				Farm.get().getTreeManager().reload();
				Farm.get().getMineManager().reload();
				sender.sendMessage(__.PREFIX + ChatColor.GREEN + "Reloaded");
			}
			return true;
		}
		return false;
	}

}
