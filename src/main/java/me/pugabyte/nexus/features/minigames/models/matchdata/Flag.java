package me.pugabyte.nexus.features.minigames.models.matchdata;

import com.destroystokyo.paper.ParticleBuilder;
import lombok.Data;
import lombok.NonNull;
import me.pugabyte.nexus.Nexus;
import me.pugabyte.nexus.features.minigames.Minigames;
import me.pugabyte.nexus.features.minigames.managers.ArenaManager;
import me.pugabyte.nexus.features.minigames.mechanics.CaptureTheFlag;
import me.pugabyte.nexus.features.minigames.mechanics.OneFlagCaptureTheFlag;
import me.pugabyte.nexus.features.minigames.models.Match;
import me.pugabyte.nexus.features.minigames.models.Minigamer;
import me.pugabyte.nexus.features.minigames.models.Team;
import me.pugabyte.nexus.utils.MaterialTag;
import me.pugabyte.nexus.utils.Time;
import me.pugabyte.nexus.utils.WorldGuardUtils;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.block.Block;
import org.bukkit.block.BlockState;
import org.bukkit.block.Sign;
import org.bukkit.block.data.BlockData;

@Data
public class Flag {
	// Spawn data
	@NonNull
	private Location spawnLocation;
	@NonNull
	private Material material;
	@NonNull
	private BlockData blockData;
	@NonNull
	private String[] lines;
	@NonNull
	private Match match;
	private Team team;

	// Carrier data
	private Minigamer carrier;

	// Dropped data
	private Location currentLocation;
	private BlockState blockBelowState;
	private int taskId = -1;

	public Flag(Sign sign, Match match) {
		this(sign, match, null);
	}

	public Flag(Sign sign, Match match, Team team) {
		this.spawnLocation = sign.getLocation();
		this.material = sign.getType();
		this.blockData = sign.getBlockData();
		this.lines = sign.getLines();
		this.match = match;
		this.team = team;
	}

	public static void particle(Minigamer carrier) {
		new ParticleBuilder(Particle.FLAME)
				.location(carrier.getPlayer().getLocation().add(0, 1, 0))
				.offset(0.35, 0.75, 0.35)
				.extra(0)
				.count(25)
				.spawn();
	}

	public void respawn() {
		if (currentLocation != null) {
			currentLocation.getBlock().setType(Material.AIR);
			currentLocation = null;
		}

		Block block = spawnLocation.getBlock();

		block.setType(material);
		block.setBlockData(blockData);

		Sign sign = (Sign) block.getState();

		for (int line = 0; line <= 3; line++)
			sign.setLine(line, lines[line]);

		sign.update();
	}

	public void despawn() {
		if (currentLocation != null) {
			currentLocation.getBlock().setType(Material.AIR);
			currentLocation = null;
		} else {
			spawnLocation.getBlock().setType(Material.AIR);
		}
	}

	public void drop(Location location) {
		currentLocation = getSuitableLocation(location);

		Block block = currentLocation.getBlock();
		block.setType(Material.OAK_SIGN);

		Sign sign = (Sign) block.getState();

		for (int line = 0; line <= 3; line++)
			sign.setLine(line, lines[line]);

		sign.update();
		taskId = match.getTasks().wait(Time.SECOND.x(60), () -> {
			respawn();
			if (match.getMechanic() instanceof CaptureTheFlag)
				match.broadcast(team.getColoredName() + "&3's flag has respawned");
			else if (match.getMechanic() instanceof OneFlagCaptureTheFlag)
				match.broadcast("The flag has respawned");
		});
	}

	public static Location getSuitableLocation(Location originalLocation) {
		Location location = originalLocation.clone();
		int maxHeight = location.getWorld().getMaxHeight();
		int minHeight = 0; // 1.17: use location.getWorld().getMinHeight()

		while (!MaterialTag.ALL_AIR.isTagged(location.getBlock().getType()) && location.getY() < maxHeight)
			location.add(0, 1, 0);

		Block below = location.clone().subtract(0, 1, 0).getBlock();
		while ((MaterialTag.ALL_AIR.isTagged(below.getType()) || !MaterialTag.ALL_AIR.isTagged(location.getBlock().getType())) &&
				ArenaManager.getFromLocation(originalLocation).getRegion().contains(new WorldGuardUtils(Minigames.getWorld()).toBlockVector3(location))) {
			location.subtract(0, 1, 0);
			below = location.clone().subtract(0, 1, 0).getBlock();

			if (location.getY() <= minHeight) {
				// maybe this should throw an exception to avoid possible harm in deleting blocks?
				// although it's probably just bedrock...
				Nexus.warn("Could not find a safe location for flag, dumping it at " + location.toString() + " (overwriting " + location.getBlock().getType().name() + ")");
				break;
			}
		}
		return location;
	}

}
