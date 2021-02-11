package me.pugabyte.nexus.models.vote;

import me.pugabyte.nexus.framework.persistence.annotations.PlayerClass;
import me.pugabyte.nexus.models.MongoService;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@PlayerClass(VotePoints.class)
public class VotePointsService extends MongoService {
	private final static Map<UUID, VotePoints> cache = new HashMap<>();

	public Map<UUID, VotePoints> getCache() {
		return cache;
	}

}
