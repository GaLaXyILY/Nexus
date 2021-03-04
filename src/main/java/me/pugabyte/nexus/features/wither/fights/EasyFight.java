package me.pugabyte.nexus.features.wither.fights;

import lombok.NoArgsConstructor;
import me.pugabyte.nexus.features.wither.WitherChallenge;
import me.pugabyte.nexus.features.wither.models.WitherFight;
import org.bukkit.Location;
import org.bukkit.entity.Wither;
import org.bukkit.inventory.ItemStack;

import java.util.List;

@NoArgsConstructor
public class EasyFight extends WitherFight {

	@Override
	public WitherChallenge.Difficulty getDifficulty() {
		return WitherChallenge.Difficulty.EASY;
	}

	@Override
	public void spawnWither(Location location) {
		this.wither = location.getWorld().spawn(location, Wither.class);
	}

	@Override
	public boolean shouldGiveStar() {
		return (Math.random() * 101) < 12.5;
	}

	@Override
	public List<ItemStack> getAlternateDrops() {
		return null;
	}
}
