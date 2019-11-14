/**
 * 
 */
package me.oddlyoko.farm.config;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;

import org.bukkit.Bukkit;
import org.bukkit.configuration.file.YamlConfiguration;

/**
 * Farm
 * Copyright (C) 2019 0ddlyoko
 * 
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 *
 * @author 0ddlyoko
 */
public final class L {
	private static Config config;
	private static Map<String, String> messages;

	static {
		File lang = new File("plugins" + File.separator + "Farm" + File.separator + "lang.yml");
		if (!lang.exists()) {
			try {
				lang.getParentFile().mkdirs();
				lang.createNewFile();
				InputStream is = L.class.getClassLoader().getResourceAsStream("lang.yml");
				if (is != null) {
					YamlConfiguration yamlConf = YamlConfiguration.loadConfiguration(new InputStreamReader(is));
					yamlConf.save(lang);
				} else
					throw new FileNotFoundException("File lang.yml doesn't exist in jar file");
			} catch (Exception ex) {
				Bukkit.getLogger().log(Level.SEVERE, "An error has occured while creating Farm directory: ", ex);
			}
		}
		config = new Config(lang);
		messages = new HashMap<>();
		// Load messages
		for (String key : config.getAllKeys())
			put(key);
	}

	/**
	 * Used to load the class
	 */
	public static void init() {
		// Nothing to do here (see code above)
	}

	private static void put(String key) {
		messages.put(key, config.getString(key));
	}

	public static String get(String name) {
		return messages.get(name);
	}
}
