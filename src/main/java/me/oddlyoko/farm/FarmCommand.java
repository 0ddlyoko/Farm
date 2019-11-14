package me.oddlyoko.farm;

import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

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
							+ "- /farm <add/create> farm [radius] [time] [carrots|potatoes|wheat|beetroot]"
							+ ChatColor.YELLOW + " : Add a new Farm");
				}
				if (sender.hasPermission("farm.remove"))
					sender.sendMessage(
							ChatColor.AQUA + "- /farm <remove> [farm]" + ChatColor.YELLOW + " : Remove a Farm");
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
					p.sendMessage(__.PREFIX + ChatColor.RED
							+ "Syntax: /farm <add/create> farm [radius] [time] [carrots|potatoes|wheat|beetroot]");
					return true;
				}
				int time = 1;
				Location loc = p.getLocation();
				if ("farm".equalsIgnoreCase(args[1])) {
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
					me.oddlyoko.farm.farm.Farm.Type type = Type.CARROTS;
					if (args.length == 5)
						type = me.oddlyoko.farm.farm.Farm.Type.valueOf(args[4].toUpperCase());

					Farm.get().getFarmManager().addFarm(loc, radius, type, time);
					p.sendMessage(__.PREFIX + ChatColor.GREEN + "Farm succesfully added ! type = " + type
							+ ", radius = " + radius + ", time = " + time);
				}
			} else if ("remove".equalsIgnoreCase(args[0])) {
				if (!sender.hasPermission("farm.remove")) {
					sender.sendMessage(__.PREFIX + ChatColor.RED + "You don't have permission to execute this command");
					return true;
				}
				Player p = (Player) sender;
				if (args.length <= 1) {
					p.sendMessage(__.PREFIX + ChatColor.RED + "Syntax: /autofarm <remove> [farm|tree]");
					return true;
				}

				if ("farm".equalsIgnoreCase(args[1])) {
					me.oddlyoko.farm.farm.Farm f = Farm.get().getFarmManager().getFarm(p.getLocation());
					if (f == null) {
						p.sendMessage(__.PREFIX + ChatColor.GREEN + "No farm found !");
						return true;
					}
					Farm.get().getFarmManager().removeFarm(f);
					p.sendMessage(__.PREFIX + ChatColor.GREEN + "Farm succesfully supress ! type = " + f.getType()
							+ ", radius = " + f.getRadius() + ", tickTime = " + f.getTickTime());
				}
			} else if ("reload".equalsIgnoreCase(args[0])) {
				if (!sender.hasPermission("autofarm.reload")) {
					sender.sendMessage(__.PREFIX + ChatColor.RED + "You don't have permission to execute this command");
					return true;
				}
				sender.sendMessage(__.PREFIX + ChatColor.GREEN + "Reloading ...");
				Farm.get().getFarmManager().reload();
				sender.sendMessage(__.PREFIX + ChatColor.GREEN + "Reloaded");
			}
			return true;
		}
		return false;
	}

}
