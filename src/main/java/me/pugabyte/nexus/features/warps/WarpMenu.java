package me.pugabyte.nexus.features.warps;

import me.pugabyte.nexus.Nexus;
import me.pugabyte.nexus.models.buildcontest.BuildContest;
import me.pugabyte.nexus.models.buildcontest.BuildContestService;

public enum WarpMenu {
	MAIN(4),
	SURVIVAL(5),
	LEGACY(5),
	MINIGAMES(5),
	CREATIVE(),
	SKYBLOCK(),
	OTHER(6),
	BUILD_CONTESTS(3);

	private int size = 0;

	WarpMenu() {
	}

	WarpMenu(int size) {
		this.size = size;
	}

	public int getSize() {
		if (this == MAIN) {
			BuildContest buildContest = new BuildContestService().get(Nexus.getUUID0());
			if (buildContest.isActive())
				return size + 2;
		}
		return size;
	}


}
