package gg.projecteden.nexus.features.customblocks.customblockbreaking;

import gg.projecteden.api.common.annotations.Environments;
import gg.projecteden.api.common.utils.Env;
import gg.projecteden.nexus.framework.features.Feature;
import lombok.Getter;

@Environments(Env.TEST)
public class CustomBlockBreaking extends Feature {
	@Getter
	private static final BrokenBlocksManager manager = new BrokenBlocksManager();

	@Override
	public void onStart() {
		new BreakListener();
	}


}
