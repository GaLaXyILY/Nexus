package gg.projecteden.nexus.features.customblocks.models.noteblocks.lanterns;

import gg.projecteden.nexus.features.customblocks.models.noteblocks.common.IDirectionalNoteBlock;
import org.bukkit.Material;
import org.jetbrains.annotations.NotNull;

import static gg.projecteden.nexus.utils.StringUtils.camelToSnake;

public interface ILantern extends IDirectionalNoteBlock {

	@NotNull
	default Material getMaterial() {
		final String woodType = getClass().getSimpleName()
			.replace("Paper", "")
			.replace("Shroom", "")
			.replace("Lantern", "");
		return Material.valueOf(camelToSnake(woodType).toUpperCase() + "_PLANKS");
	}

	@Override
	default double getBlockHardness() {
		return 0.8;
	}

	@Override
	default boolean requiresCorrectToolForDrops() {
		return false;
	}

	@Override
	default boolean requiresSilkTouchForDrops() {
		return true;
	}

}
