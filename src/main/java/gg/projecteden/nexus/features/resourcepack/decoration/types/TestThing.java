package gg.projecteden.nexus.features.resourcepack.decoration.types;

import com.destroystokyo.paper.ParticleBuilder;
import gg.projecteden.nexus.features.resourcepack.decoration.common.DecorationConfig;
import gg.projecteden.nexus.features.resourcepack.decoration.common.Tickable;
import gg.projecteden.nexus.features.resourcepack.models.CustomMaterial;
import org.bukkit.Location;
import org.bukkit.Particle;

public class TestThing extends DecorationConfig implements Tickable {

	public TestThing(String name, CustomMaterial material) {
		super(name, material);
	}

	@Override
	public void tick(Location location) {
		new ParticleBuilder(Particle.ENCHANTMENT_TABLE).location(location.add(0, 1, 0).toCenterLocation()).spawn();
	}
}
