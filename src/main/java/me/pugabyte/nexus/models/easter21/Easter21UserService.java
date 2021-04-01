package me.pugabyte.nexus.models.easter21;

import me.pugabyte.nexus.framework.persistence.annotations.PlayerClass;
import me.pugabyte.nexus.models.MongoService;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@PlayerClass(Easter21User.class)
public class Easter21UserService extends MongoService {
	private final static Map<UUID, Easter21User> cache = new HashMap<>();

	public Map<UUID, Easter21User> getCache() {
		return cache;
	}

}
