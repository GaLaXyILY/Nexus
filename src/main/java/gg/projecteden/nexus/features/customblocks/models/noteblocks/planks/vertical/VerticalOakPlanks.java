package gg.projecteden.nexus.features.customblocks.models.noteblocks.planks.vertical;

import gg.projecteden.nexus.features.customblocks.models.common.CustomBlockConfig;
import gg.projecteden.nexus.features.customblocks.models.noteblocks.common.CustomNoteBlockConfig;
import org.bukkit.Instrument;

@CustomBlockConfig(
	name = "Vertical Oak Planks",
	modelId = 20001
)
@CustomNoteBlockConfig(
	instrument = Instrument.BANJO,
	step = 1
)
public class VerticalOakPlanks implements IVerticalPlanks {}
