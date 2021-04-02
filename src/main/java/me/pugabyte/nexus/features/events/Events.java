package me.pugabyte.nexus.features.events;

import me.pugabyte.nexus.features.events.aeveonproject.AeveonProject;
import me.pugabyte.nexus.framework.annotations.Environments;
import me.pugabyte.nexus.framework.features.Feature;
import me.pugabyte.nexus.utils.Env;
import me.pugabyte.nexus.utils.StringUtils;

@Environments(Env.PROD)
public class Events extends Feature {
	public static String PREFIX = StringUtils.getPrefix("Events");

	@Override
	public void onStart() {
		new ArmorStandStalker();

		new ScavHuntLegacy();

		new AeveonProject();
	}

	@Override
	public void onStop() {
	}

}
