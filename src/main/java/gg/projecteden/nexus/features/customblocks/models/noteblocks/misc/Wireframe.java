package gg.projecteden.nexus.features.customblocks.models.noteblocks.misc;

import gg.projecteden.nexus.features.customblocks.models.common.CustomBlockConfig;
import gg.projecteden.nexus.features.customblocks.models.noteblocks.common.CustomNoteBlockConfig;
import gg.projecteden.nexus.features.customblocks.models.noteblocks.common.ICustomNoteBlock;
import org.bukkit.Instrument;
import org.bukkit.Material;

@CustomBlockConfig(
	name = "Wireframe",
	modelId = 20120
)
@CustomNoteBlockConfig(
	instrument = Instrument.BASS_GUITAR,
	step = 20
)
public class Wireframe implements ICustomNoteBlock {
	@Override
	public double getBlockHardness() {
		return 1.5;
	}

	@Override
	public Material getMinimumPreferredTool() {
		return Material.WOODEN_PICKAXE;
	}

}
