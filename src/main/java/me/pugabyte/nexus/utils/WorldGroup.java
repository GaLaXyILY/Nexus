package me.pugabyte.nexus.utils;

import lombok.Getter;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Entity;

import java.util.Arrays;
import java.util.List;

@Getter
public enum WorldGroup {
	SURVIVAL("world", "world_nether", "world_the_end",
			"survival", "survival_nether", "survival_the_end",
			"legacy", "legacy_nether", "legacy_the_end",
			"resource", "resource_nether", "resource_the_end",
			"staff_world", "staff_world_nether", "staff_world_the_end",
			"safepvp", "wither"),
	CREATIVE("creative", "buildcontest"),
	MINIGAMES("gameworld", "blockball", "deathswap"),
	SKYBLOCK("skyblock", "skyblock_nether"),
	ADVENTURE("stranded", "aeveon_project"),
	EVENT("2y", "events"),
	STAFF("buildadmin", "jail", "pirate", "tiger"),
	UNKNOWN;

	private List<String> worlds;

	WorldGroup() {
		this(new String[0]);
	}

	WorldGroup(String... worlds) {
		this.worlds = Arrays.asList(worlds);
	}

	@Override
	public String toString() {
		return StringUtils.camelCase(name());
	}

	public boolean contains(World world) {
		return contains(world.getName());
	}

	public boolean contains(String world) {
		return worlds.contains(world);
	}

	public static WorldGroup get(Entity entity) {
		return get(entity.getWorld());
	}

	public static WorldGroup get(Location location) {
		return get(location.getWorld());
	}

	public static WorldGroup get(World world) {
		return get(world.getName());
	}

	public static WorldGroup get(String world) {
		for (WorldGroup group : values())
			if (group.getWorlds() != null)
				if (group.contains(world))
					return group;

		if (world.toLowerCase().startsWith("build"))
			return CREATIVE;

		return UNKNOWN;
	}
}
