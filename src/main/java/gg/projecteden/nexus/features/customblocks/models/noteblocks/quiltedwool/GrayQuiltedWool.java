package gg.projecteden.nexus.features.customblocks.models.noteblocks.quiltedwool;

import gg.projecteden.nexus.features.customblocks.models.common.CustomBlockConfig;
import gg.projecteden.nexus.features.customblocks.models.noteblocks.common.CustomNoteBlockConfig;
import org.bukkit.Instrument;

@CustomBlockConfig(
	name = "Gray Quilted Wool",
	modelId = 20314
)
@CustomNoteBlockConfig(
	instrument = Instrument.COW_BELL,
	step = 14,
	customBreakSound = "block.wool.break",
	customPlaceSound = "block.wool.place",
	customStepSound = "block.wool.step",
	customHitSound = "block.wool.hit",
	customFallSound = "block.wool.fall"
)
public class GrayQuiltedWool implements IQuiltedWool {}
