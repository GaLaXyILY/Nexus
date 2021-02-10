package me.pugabyte.nexus.models.honeypot;

import dev.morphia.annotations.Converters;
import dev.morphia.annotations.Entity;
import dev.morphia.annotations.Id;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import me.pugabyte.nexus.framework.persistence.serializer.mongodb.LocationConverter;
import me.pugabyte.nexus.framework.persistence.serializer.mongodb.UUIDConverter;
import me.pugabyte.nexus.models.PlayerOwnedObject;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Data
@Builder
@Entity("honeypot_bans")
@NoArgsConstructor
@AllArgsConstructor
@RequiredArgsConstructor
@Converters({UUIDConverter.class, LocationConverter.class})
public class HoneyPotBans extends PlayerOwnedObject {
	@Id
	@NonNull
	private UUID uuid;
	private List<HoneyPot> honeyPots = new ArrayList<>();

	public HoneyPot get(String id) {
		return honeyPots.stream().filter(honeyPot -> honeyPot.getId().equalsIgnoreCase(id)).findFirst().orElseGet(() -> {
			HoneyPot honeyPot = new HoneyPot(id);
			honeyPots.add(honeyPot);
			return honeyPot;
		});
	}

	@Data
	@NoArgsConstructor
	@RequiredArgsConstructor
	public static class HoneyPot {
		@NonNull
		private String id;
		private int bans;

		public void addBan() {
			++bans;
		}
	}

}
