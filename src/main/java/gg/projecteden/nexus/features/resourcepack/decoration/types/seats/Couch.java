package gg.projecteden.nexus.features.resourcepack.decoration.types.seats;

import gg.projecteden.nexus.features.resourcepack.decoration.common.Colorable;
import gg.projecteden.nexus.features.resourcepack.models.CustomMaterial;
import lombok.Getter;

public class Couch extends Chair implements Colorable {
	@Getter
	private final CouchPart couchPart;

	public Couch(String name, CustomMaterial material, ColorableType colorableType, CouchPart couchPart) {
		this(name, material, colorableType, couchPart, null);
	}

	public Couch(String name, CustomMaterial material, ColorableType colorableType, CouchPart couchPart, Double sitHeight) {
		super(name, material, colorableType, sitHeight);
		this.couchPart = couchPart;
	}

	public enum CouchPart {
		STRAIGHT,
		END,
		CORNER,
		;
	}
}
