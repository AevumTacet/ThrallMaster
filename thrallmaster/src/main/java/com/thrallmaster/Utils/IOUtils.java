package com.thrallmaster.Utils;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;

public final class IOUtils {

	public static String writeLocation(Location location) {
		return location.getWorld().getName() + ", " + location.getX() + ", " + location.getY() + ", " + location.getZ();
	}

	public static Location readLocation(String str) {
		String[] parts = str.split(",\\s*");
		if (parts.length != 4) {
			System.err.println("Location string need to have four components: world, x, y, z.");
			return null;
		}

		World world = Bukkit.getWorld(parts[0]);
		if (world == null) {
			System.err.println("World: " + parts[0] + " does not exist.");
			return null;
		}

		double x = Double.parseDouble(parts[1]);
		double y = Double.parseDouble(parts[2]);
		double z = Double.parseDouble(parts[3]);
		return new Location(world, x, y, z);
	}
}
