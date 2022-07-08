package gg.projecteden.nexus.features.customblocks.models.noteblocks.planks.colored;

import com.mojang.datafixers.util.Pair;
import gg.projecteden.nexus.features.customblocks.models.CustomBlockTag;
import gg.projecteden.nexus.features.customblocks.models.common.IDyeable;
import gg.projecteden.nexus.features.customblocks.models.noteblocks.planks.IPlanks;
import gg.projecteden.nexus.features.recipes.models.builders.RecipeBuilder;
import gg.projecteden.nexus.utils.MaterialTag;
import org.bukkit.Material;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import static gg.projecteden.nexus.utils.StringUtils.camelToSnake;

public interface IColoredPlanks extends IDyeable, IPlanks {

	@Override
	default CustomBlockTag getRedyeTag() {
		return CustomBlockTag.COLORED_PLANKS;
	}

	default @NotNull Material getMaterial() {
		return Material.valueOf(camelToSnake(getClass().getSimpleName().replace("Planks", "")).toUpperCase() + "_DYE");
	}

	@Override
	default @Nullable Pair<RecipeBuilder<?>, Integer> getCraftRecipe() {
		return getSurroundRecipe(getMaterial(), MaterialTag.PLANKS);
	}

}
