package me.pugabyte.nexus.features.minigames.perks.arrowparticles;

import me.pugabyte.nexus.features.minigames.models.perks.common.ParticleProjectilePerk;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.inventory.ItemStack;

public class BasicTrail extends ParticleProjectilePerk {
    @Override
    public Particle getParticle() {
        return Particle.END_ROD;
    }

    @Override
    public String getName() {
        return "Basic Trail";
    }

    @Override
    public ItemStack getMenuItem() {
        return new ItemStack(Material.WHITE_DYE);
    }

    @Override
    public String[] getDescription() {
        return new String[]{
                "Give your arrows some",
                "sparkles with this",
                "simple trail"
        };
    }

    @Override
    public int getPrice() {
        return 2;
    }

    @Override
    public double getSpeed() {
        return 0.01d;
    }
}
