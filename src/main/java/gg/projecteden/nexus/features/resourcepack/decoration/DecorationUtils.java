package gg.projecteden.nexus.features.resourcepack.decoration;

import gg.projecteden.api.common.utils.ReflectionUtils;
import gg.projecteden.nexus.features.clientside.models.ClientSideItemFrame;
import gg.projecteden.nexus.features.clientside.models.IClientSideEntity.ClientSideEntityType;
import gg.projecteden.nexus.features.resourcepack.decoration.common.DecorationConfig;
import gg.projecteden.nexus.features.resourcepack.decoration.common.Hitbox;
import gg.projecteden.nexus.features.workbenches.DyeStation;
import gg.projecteden.nexus.features.workbenches.DyeStation.DyeStationMenu.ColorChoice;
import gg.projecteden.nexus.features.workbenches.DyeStation.DyeStationMenu.StainChoice;
import gg.projecteden.nexus.framework.interfaces.Colored;
import gg.projecteden.nexus.models.clientside.ClientSideConfig;
import gg.projecteden.nexus.models.nerd.Rank;
import gg.projecteden.nexus.utils.Distance;
import gg.projecteden.nexus.utils.ItemBuilder;
import gg.projecteden.nexus.utils.LocationUtils;
import gg.projecteden.nexus.utils.PlayerUtils;
import gg.projecteden.nexus.utils.StringUtils;
import gg.projecteden.nexus.utils.Utils.ItemFrameRotation;
import gg.projecteden.nexus.utils.worldgroup.WorldGroup;
import lombok.Getter;
import lombok.NonNull;
import net.md_5.bungee.api.ChatColor;
import org.bukkit.Color;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.entity.ItemFrame;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import static gg.projecteden.nexus.utils.Distance.distance;
import static gg.projecteden.nexus.utils.Nullables.isNullOrAir;
import static gg.projecteden.nexus.utils.StringUtils.stripColor;

public class DecorationUtils {
	@Getter
	private static final String prefix = StringUtils.getPrefix("Decoration");
	@Getter
	private static final Set<UUID> debuggers = new HashSet<>();
	@Getter
	private static final List<BlockFace> cardinalFaces = List.of(BlockFace.NORTH, BlockFace.SOUTH, BlockFace.EAST, BlockFace.WEST);


	public static void error(Player player) {
		if (player == null)
			return;

		error(player, "&c&lHey! &7Sorry, but you can't use that here.");
	}

	public static void error(Player player, String error) {
		if (player == null)
			return;

		PlayerUtils.send(player, error);
	}

	public static void debug(String message) {
		for (UUID debugger : debuggers) {
			PlayerUtils.send(debugger, message);
		}
	}

	public static void debug(Player player, String message) {
		if (player == null)
			return;

		if (debuggers.contains(player.getUniqueId()))
			PlayerUtils.send(player, message);
	}

	public static void debug(Player player, Runnable runnable) {
		if (player == null)
			return;

		if (debuggers.contains(player.getUniqueId()))
			runnable.run();
	}

	public static void dye(ItemStack item, ChatColor color, Player debugger) {
		Colored.of(color).apply(item);
		updateLore(item, debugger);
	}

	public static void dye(ItemStack item, ColorChoice colorChoice, Player debugger) {
		colorChoice.apply(item);
		updateLore(item, debugger);
	}

	public static ItemStack updateLore(ItemStack item, Player debugger) {
		ItemBuilder resultBuilder = new ItemBuilder(item);
		Color color = resultBuilder.dyeColor();

		String colorHex = StringUtils.toHex(color);
		String colorName = colorHex;
		boolean isStain = false;
		for (StainChoice stainChoice : StainChoice.values()) {
			if (stainChoice.getColor().equals(color)) {
				isStain = true;
				colorName = StringUtils.camelCase(stainChoice.name());
				break;
			}
		}

		debug(debugger, "Color Name: " + colorName);

		boolean isPaintbrush = resultBuilder.modelId() == DyeStation.getPaintbrush().modelId();
		boolean handledPaintbrushUses = false;

		List<String> finalLore = new ArrayList<>();

		// Change lore
		List<String> newLore = new ArrayList<>();
		for (String line : resultBuilder.getLore()) {
			String _line = stripColor(line);
			// remove color line
			if (_line.contains("Color: "))
				continue;
			if (_line.contains("Stain: "))
				continue;

			// reset uses
			if (isPaintbrush && _line.contains(stripColor(DyeStation.USES_LORE))) {
				newLore.add(DyeStation.USES_LORE + DyeStation.MAX_USES_PAINTBRUSH);
				handledPaintbrushUses = true;
				continue;
			}

			newLore.add(line);
		}

		// add uses if missing
		if (isPaintbrush && !handledPaintbrushUses) {
			newLore.add(DyeStation.USES_LORE + DyeStation.MAX_USES_PAINTBRUSH);
		}

		// Add color line
		String colorLine = isStain ? "&3Stain: &" : "&3Color: &";

		finalLore.add(colorLine + colorHex + colorName);
		debug(debugger, "Adding color line: " + colorLine + colorHex + colorName);

		finalLore.addAll(newLore);

		resultBuilder.setLore(finalLore);

		ItemStack result = resultBuilder.build();
		item.setItemMeta(result.getItemMeta());

		debug(debugger, "Item lore: " + item.getLore());

		return item;
	}

	@Getter
	public static final List<BlockFace> directions = List.of(
		BlockFace.NORTH, BlockFace.NORTH_EAST, BlockFace.EAST, BlockFace.SOUTH_EAST,
		BlockFace.SOUTH, BlockFace.SOUTH_WEST, BlockFace.WEST, BlockFace.NORTH_WEST
	);

	public static BlockFace rotateClockwise(BlockFace blockFace) {
		int size = directions.size() - 1;
		int index = (directions.indexOf(blockFace) + 1);

		if (index > size)
			index = 0;

		return directions.get(index);
	}

	public static List<String> getInstancesOf(DecorationConfig config) {
		Set<Class<?>> classes = new HashSet<>();
		for (Class<? extends DecorationConfig> clazz : ReflectionUtils.superclassesOf(config.getClass())) {
			classes.add(clazz);
			classes.addAll(List.of(clazz.getInterfaces()));
		}

		List<String> result = new ArrayList<>();
		for (Class<?> clazz : classes)
			result.add(clazz.getSimpleName());

		return result;
	}

	@Nullable
	public static Object getItemFrame(Block clicked, int radius, BlockFace blockFaceOverride, Player debugger, boolean isClientside) {
		if (isNullOrAir(clicked))
			return null;

		Location location = clicked.getLocation().toCenterLocation();
		if (!hasNearbyItemFrames(location, radius, isClientside))
			return null;

		Set<Material> hitboxTypes = DecorationConfig.getHitboxTypes();
		if (!hitboxTypes.contains(clicked.getType()))
			return null;

		HitboxMaze maze = new HitboxMaze(debugger, clicked, radius);

		Object itemFrame = findNearbyItemFrame(location, isClientside, debugger);

		if (itemFrame != null) {
			debug(debugger, "Single");
			return findItemFrame(maze, clicked.getLocation(), blockFaceOverride, debugger, isClientside);
		}

		debug(debugger, "Maze Search");
		return getConnectedHitboxes(maze, blockFaceOverride, debugger, isClientside);
	}

	// It isn't pretty, but it works
	// TODO: optimize by checking nearest neighbors first
	private static @Nullable Object getConnectedHitboxes(HitboxMaze maze, BlockFace blockFaceOverride, Player debugger, boolean isClientside) {
		if (maze.getTries() > 10) {
			debug(debugger, "Maze Tries > 10");
			return null;
		}

		if (maze.getDirectionsLeft().isEmpty()) {
			Location newLoc = maze.getBlock().getLocation();

			if (newLoc.equals(maze.getOrigin().getLocation())) {
				maze.debugDot(newLoc, Color.ORANGE);
				debug(debugger, "Maze returned to origin");
				return null;
			}

			maze.goBack();
			maze.debugDot(newLoc, Color.RED);

			maze.incTries();
			return getConnectedHitboxes(maze, blockFaceOverride, debugger, isClientside);
		}

		maze.nextDirection();
		maze.setTries(0);

		Block previousBlock = maze.getBlock();
		maze.setBlock(previousBlock.getRelative(maze.getBlockFace()));

		Block currentBlock = maze.getBlock();
		Location currentLoc = currentBlock.getLocation().clone();
		Material currentType = currentBlock.getType();

		Distance distance = distance(maze.getOrigin(), currentLoc);
		Set<Material> hitboxTypes = DecorationConfig.getHitboxTypes();
		if (maze.getTried().contains(currentLoc) || !hitboxTypes.contains(currentType) || distance.gt(6)) {
			maze.setBlock(previousBlock);
			return getConnectedHitboxes(maze, blockFaceOverride, debugger, isClientside);
		}

		maze.getTried().add(currentLoc);
		maze.addToPath(previousBlock.getLocation(), maze.getDirectionsLeft());

		// Is correct item frame?
		Object itemFrame = findItemFrame(maze, currentLoc, blockFaceOverride, debugger, isClientside);
		if (itemFrame != null) {
			debug(debugger, "Maze found item frame");
			return itemFrame;
		}

		// Keep looking
		maze.resetDirections();
		maze.addToPath(currentLoc, maze.getDirectionsLeft());
		maze.debugDot(currentLoc, Color.BLACK);
		return getConnectedHitboxes(maze, blockFaceOverride, debugger, isClientside);
	}

	private static @Nullable Object findItemFrame(@NonNull HitboxMaze maze, @NonNull Location currentLoc, BlockFace blockFaceOverride, Player debugger, boolean isClientside) {
		ItemStack itemStack;
		BlockFace blockFace;

		Object itemFrame = findNearbyItemFrame(currentLoc, isClientside, debugger);
		if (itemFrame == null) {
			debug(debugger, "- no item frames found nearby");
			return null;
		}

		if (isClientside) {
			ClientSideItemFrame _itemFrame = (ClientSideItemFrame) itemFrame;

			blockFace = ItemFrameRotation.of(_itemFrame).getBlockFace();
			itemStack = _itemFrame.content();
		} else {
			ItemFrame _itemFrame = (ItemFrame) itemFrame;

			blockFace = ItemFrameRotation.of(_itemFrame).getBlockFace();
			itemStack = _itemFrame.getItem();
		}

		if (isNullOrAir(itemStack)) {
			debug(debugger, "- item frame is empty");
			return null;
		}

		DecorationConfig config = DecorationConfig.of(itemStack);
		if (config == null) {
			debug(debugger, "- item frame does not have decoration");
			return null;
		}

		debug(debugger, "Hitbox BlockFace: " + blockFace);
		if (config.isMultiBlockWallThing() && blockFaceOverride != null) {
			blockFace = blockFaceOverride;
			debug(debugger, "BlockFace Override 1: " + blockFace);
		}

		List<Hitbox> hitboxes = Hitbox.rotateHitboxes(config, blockFace);

		Location originLoc = maze.getOrigin().getLocation();

		for (Hitbox hitbox : hitboxes) {
			Block _block = hitbox.getOffsetBlock(currentLoc);
			Location _blockLoc = _block.getLocation();

			maze.debugDot(_blockLoc, Color.TEAL);

			if (LocationUtils.isFuzzyEqual(_blockLoc, originLoc)) {
				debug(debugger, "origin is in hitbox, returning item frame");
				return itemFrame;
			}
		}

		debug(debugger, "- origin isn't in hitbox");
		return null;
	}

	private static boolean hasNearbyItemFrames(Location location, double radius, boolean isClientside) {
		if (isClientside)
			return ClientSideConfig.getEntities(location, radius).size() > 0;

		return location.getNearbyEntitiesByType(ItemFrame.class, radius).size() > 0;
	}

	private static @Nullable Object findNearbyItemFrame(Location location, boolean isClientside, Player debugger) {
		Location _location = location.toCenterLocation();
		double _radius = 0.5;

		if (isClientside)
			return ClientSideConfig.getEntities(_location, ClientSideEntityType.ITEM_FRAME, _radius).stream().findFirst().orElse(null);

		return _location.getNearbyEntitiesByType(ItemFrame.class, _radius).stream().findFirst().orElse(null);
	}

	// TODO DECORATIONS - Remove on release
	@Deprecated
	public static boolean canUseFeature(Player player) {
		return Rank.of(player).isSeniorStaff() || Rank.of(player).isBuilder() || player.getUniqueId().toString().equals("32fc75e3-a278-43c4-99a7-90af03846dad");
	}

	public static boolean hasBypass(Player player) {
		// TODO DECORATIONS - Remove on release
		String errorPrefix = prefix + "&c";
		if (!DecorationUtils.canUseFeature(player)) {
			PlayerUtils.send(player, errorPrefix + "You cannot use this feature yet");
			return false;
		}
		//

		if (Rank.of(player).isAdmin() && player.getGameMode().equals(GameMode.CREATIVE)) {
			return true;
		}

		WorldGroup worldGroup = WorldGroup.of(player);
		if (worldGroup.equals(WorldGroup.STAFF) || worldGroup.equals(WorldGroup.CREATIVE))
			return true;

		return false;
	}
}
