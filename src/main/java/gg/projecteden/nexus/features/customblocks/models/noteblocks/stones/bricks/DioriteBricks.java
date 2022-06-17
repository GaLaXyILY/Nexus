package gg.projecteden.nexus.features.customblocks.models.noteblocks.stones.bricks;

import gg.projecteden.nexus.features.customblocks.models.common.CustomBlockConfig;
import gg.projecteden.nexus.features.customblocks.models.noteblocks.common.CustomNoteBlockConfig;
import org.bukkit.Instrument;

@CustomBlockConfig(
	name = "Diorite Bricks",
	modelId = 20355
)
@CustomNoteBlockConfig(
	instrument = Instrument.DIDGERIDOO,
	step = 6,
	customBreakSound = "custom.block.stone.break",
	customPlaceSound = "custom.block.stone.place",
	customStepSound = "custom.block.stone.step",
	customHitSound = "custom.block.stone.hit",
	customFallSound = "custom.block.stone.fall"
)
public class DioriteBricks implements IStoneBricks {}
