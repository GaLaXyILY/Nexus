package me.pugabyte.nexus.models.buildcontest;

import me.pugabyte.nexus.framework.persistence.annotations.PlayerClass;
import me.pugabyte.nexus.models.MongoService;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@PlayerClass(BuildContest.class)
public class BuildContestService extends MongoService {
	private final static Map<UUID, BuildContest> cache = new HashMap<>();

	public Map<UUID, BuildContest> getCache() {
		return cache;
	}

}
