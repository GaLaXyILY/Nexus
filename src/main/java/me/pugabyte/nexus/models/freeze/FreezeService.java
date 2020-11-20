package me.pugabyte.nexus.models.freeze;

import me.pugabyte.nexus.framework.persistence.annotations.PlayerClass;
import me.pugabyte.nexus.models.MongoService;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@PlayerClass(Freeze.class)
public class FreezeService extends MongoService {
	private final static Map<UUID, Freeze> cache = new HashMap<>();

	public Map<UUID, Freeze> getCache() {
		return cache;
	}

}
