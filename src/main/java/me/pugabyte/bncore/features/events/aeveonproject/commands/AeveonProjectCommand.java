package me.pugabyte.bncore.features.events.aeveonproject.commands;

import lombok.NoArgsConstructor;
import me.pugabyte.bncore.features.events.aeveonproject.APUtils;
import me.pugabyte.bncore.features.events.aeveonproject.menus.ShipColorMenu;
import me.pugabyte.bncore.features.events.aeveonproject.sets.APSet;
import me.pugabyte.bncore.features.events.aeveonproject.sets.APSetType;
import me.pugabyte.bncore.framework.commands.models.CustomCommand;
import me.pugabyte.bncore.framework.commands.models.annotations.Aliases;
import me.pugabyte.bncore.framework.commands.models.annotations.Arg;
import me.pugabyte.bncore.framework.commands.models.annotations.Path;
import me.pugabyte.bncore.framework.commands.models.annotations.Permission;
import me.pugabyte.bncore.framework.commands.models.events.CommandEvent;
import me.pugabyte.bncore.models.aeveonproject.AeveonProjectService;
import me.pugabyte.bncore.models.aeveonproject.AeveonProjectUser;
import me.pugabyte.bncore.utils.BlockUtils;
import me.pugabyte.bncore.utils.RandomUtils;
import me.pugabyte.bncore.utils.StringUtils;
import me.pugabyte.bncore.utils.Tasks;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.entity.Player;
import org.bukkit.event.Listener;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;

import static me.pugabyte.bncore.utils.RandomUtils.getWeightedRandom;

@Aliases("ap")
@NoArgsConstructor
@Permission("group.staff")
public class AeveonProjectCommand extends CustomCommand implements Listener {
	AeveonProjectService service = new AeveonProjectService();
	AeveonProjectUser user;

	public AeveonProjectCommand(CommandEvent event) {
		super(event);
		PREFIX = StringUtils.getPrefix("AP");
	}

	@Path("start")
	public void start() {
		if (service.hasStarted(player()))
			error("Already started");

		user = service.get(player());
		service.save(user);
		send(PREFIX + "Started!");
	}

	@Path("shipColor")
	public void chooseShipColor() {
		if (!service.hasStarted(player()))
			error("Not started");

		new ShipColorMenu().open(player());
	}

	@Path("showData")
	public void showData() {
		if (!service.hasStarted(player()))
			error("No data");

		user = service.get(player());

		send(PREFIX + "Player Data:");
		send("ShipColor: " + user.getShipColor());

		send("-----");
	}

	@Path("debug set")
	public void debug() {
		send("Set: Status | Players");
		for (APSetType setType : APSetType.values()) {
			APSet set = setType.get();

			String name = StringUtils.camelCaseWithUnderscores(setType.name());
			String status = (set.isActive() ? "&aActive" : "&cInactive");
			Collection<Player> players = APUtils.getPlayersInSet(set);
			Integer amt = null;
			if (players != null)
				amt = players.size();

			send(" - " + name + ": " + status + "&f | " + amt);
		}
	}

	@Path("warps [string...]")
	public void warps(String arguments) {
		if (isNullOrEmpty(arguments))
			arguments = "";
		else
			arguments = " " + arguments;
		runCommand("aeveonprojectwarps" + arguments);
	}

	@Path("beepboop <text>")
	public void beepboop(String type) {
		int times = RandomUtils.randomInt(5, 10);
		for (int i = 0; i < times; i++) {
			double pitch = RandomUtils.randomDouble(0.0, 2.0);
			if (type.equalsIgnoreCase("high"))
				pitch = RandomUtils.randomDouble(1.0, 2.0);
			else if (type.equalsIgnoreCase("low"))
				pitch = RandomUtils.randomDouble(0.0, 1.0);

			double finalPitch = pitch;
			Tasks.wait(2 * i, () ->
					player().getWorld().playSound(player().getLocation(), Sound.BLOCK_NOTE_BLOCK_BIT, 2F, (float) finalPitch));
		}
	}

	@Path("clearDatabase")
	@Permission(value = "group.admin", absolute = true)
	public void clearDatabase() {
		service.clearCache();
		service.deleteAll();
		service.clearCache();
	}

	private final static List<Material> allowedFloraMaterials = Arrays.asList(Material.WARPED_NYLIUM, Material.SOUL_SOIL);

	private final static LinkedHashMap<Material, Double> floraChanceMap = new LinkedHashMap<Material, Double>() {{
		put(Material.AIR, 36.08);
		put(Material.WARPED_ROOTS, 33.16);
		put(Material.NETHER_SPROUTS, 24.37);
		put(Material.WARPED_FUNGUS, 05.08);
		put(Material.CRIMSON_FUNGUS, 00.43);
		put(Material.CRIMSON_ROOTS, 00.34);
		put(Material.TWISTING_VINES_PLANT, 00.50);
	}};

	@Path("flora [radius]")
	public void flora(@Arg("5") int radius) {
		Tasks.async(() -> {
			final int finalRadius = Math.max(1, Math.min(radius, 25));
			List<Block> placeFloraOn = new ArrayList<>();
			List<Block> blocks = BlockUtils.getBlocksInRadius(player().getLocation(), finalRadius);

			for (Block block : blocks) {
				Block above = block.getRelative(BlockFace.UP);
				if (allowedFloraMaterials.contains(block.getType()) && BlockUtils.isNullOrAir(above))
					placeFloraOn.add(above);
			}

			for (Block block : placeFloraOn) {
				Material material = getWeightedRandom(floraChanceMap);
				Tasks.sync(() -> {
					block.setType(material);
					if (material.equals(Material.TWISTING_VINES_PLANT) && RandomUtils.chanceOf(75))
						block.getRelative(BlockFace.UP).setType(Material.TWISTING_VINES);
				});
			}
		});
	}

}
