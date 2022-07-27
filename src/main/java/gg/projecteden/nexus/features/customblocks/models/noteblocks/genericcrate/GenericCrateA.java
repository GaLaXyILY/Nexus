package gg.projecteden.nexus.features.customblocks.models.noteblocks.genericcrate;

import gg.projecteden.nexus.features.customblocks.models.common.CustomBlockConfig;
import gg.projecteden.nexus.features.customblocks.models.noteblocks.common.CustomNoteBlockConfig;
import org.bukkit.Instrument;

@CustomBlockConfig(
	name = "Generic Crate",
	modelId = 20101
)
@CustomNoteBlockConfig(
	instrument = Instrument.BASS_GUITAR,
	step = 1
)
public class GenericCrateA implements IGenericCrate {
}
