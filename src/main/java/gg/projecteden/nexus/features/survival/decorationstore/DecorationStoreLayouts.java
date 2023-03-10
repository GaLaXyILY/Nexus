package gg.projecteden.nexus.features.survival.decorationstore;

import gg.projecteden.api.common.utils.TimeUtils.TickTime;
import gg.projecteden.nexus.features.survival.Survival;
import gg.projecteden.nexus.framework.exceptions.postconfigured.InvalidInputException;
import gg.projecteden.nexus.models.decorationstore.DecorationStoreConfig;
import gg.projecteden.nexus.utils.PlayerUtils;
import gg.projecteden.nexus.utils.Tasks;
import gg.projecteden.nexus.utils.TitleBuilder;
import gg.projecteden.nexus.utils.WorldEditUtils;
import lombok.Getter;
import lombok.NonNull;
import org.bukkit.Location;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

public class DecorationStoreLayouts {
	@Getter
	private static final String directory = "survival/decor_store/";
	@Getter
	private static final String directoryAbsolute = WorldEditUtils.getSchematicsDirectory() + directory;
	@Getter
	private static final String reset_schematic = directory + "reset";
	@Getter
	private static boolean animating = false;

	public static String getLayoutSchematic(int id) {
		return directory + id;
	}

	public static void pasteLayout(String schematic) {
		// TODO: Interaction/Display entities
		List<EntityType> deleteEntities = List.of(EntityType.ITEM_FRAME, EntityType.ARMOR_STAND, EntityType.PAINTING, EntityType.GLOW_ITEM_FRAME);
		for (Entity entity : Survival.worldguard().getEntitiesInRegion(DecorationStore.getStoreRegionSchematic())) {
			if (deleteEntities.contains(entity.getType())) {
				entity.remove();
			}
		}

		Survival.worldedit().paster()
			.file(schematic)
			.entities(true)
			.at(new Location(Survival.getWorld(), 362, 64, 15))
			.pasteAsync();
	}

	public static void pasteNextLayout() {
		if (animating)
			throw new InvalidInputException("Decoration Store is already pasting the next layout!");

		animating = true;
		DecorationStoreConfig config = DecorationStore.getConfig();
		config.setActive(false);
		DecorationStore.saveConfig();

		List<Player> players = DecorationStore.getPlayersInStore();
		new TitleBuilder()
			.title("鄜")
			.fade(TickTime.TICK.x(10))
			.stay(TickTime.TICK.x(10))
			.players(players)
			.send();

		Tasks.wait(10, () -> {
			for (Player player : DecorationStore.getPlayersInStore()) {
				player.teleportAsync(DecorationStore.getWarpLocation());
				PlayerUtils.send(player, DecorationStore.PREFIX + "You have been removed for remodeling, come back shortly!");
			}
			DecorationStore.resetPlayerData();

			pasteLayout(reset_schematic);

			Tasks.wait(TickTime.MINUTE, () -> {
				int schematicId = getNextSchematicId();

				config.setSchematicId(schematicId);
				pasteLayout(getLayoutSchematic(schematicId));

				config.setActive(true);
				DecorationStore.saveConfig();
				animating = false;
			});
		});
	}

	public static int getNextSchematicId() {
		DecorationStoreConfig config = DecorationStore.getConfigService().get();
		int id = config.getSchematicId() + 1;
		int maxId = getTotalLayouts();

		if (id > maxId)
			id = 1;

		return id;
	}

	public static int getTotalLayouts() {
		return getLayoutFiles().size();
	}

	public static @NonNull List<File> getLayoutFiles() {
		File dir = new File(directoryAbsolute);
		File[] files = dir.listFiles();
		if (files == null || files.length == 0)
			throw new NullPointerException("Couldn't find any schematics under directory: " + directory);

		List<File> filesList = new ArrayList<>(List.of(files));
		filesList.removeIf(file -> !file.getPath().matches(directoryAbsolute + "\\d+\\.schem"));

		return filesList;
	}

	public static String getSchematicPath(File layoutFile) {
		String path;
		path = layoutFile.getPath().replace(WorldEditUtils.getSchematicsDirectory(), "");
		path = path.replaceAll(".schem", "");

		return path;
	}
}
