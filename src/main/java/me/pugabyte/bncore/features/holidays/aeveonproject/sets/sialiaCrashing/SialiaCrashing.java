package me.pugabyte.bncore.features.holidays.aeveonproject.sets.sialiaCrashing;

import me.pugabyte.bncore.BNCore;
import me.pugabyte.bncore.features.holidays.aeveonproject.APUtils;
import me.pugabyte.bncore.features.holidays.aeveonproject.sets.APRegions;
import me.pugabyte.bncore.features.holidays.aeveonproject.sets.APSet;
import me.pugabyte.bncore.features.holidays.aeveonproject.sets.APSetType;
import me.pugabyte.bncore.features.holidays.annotations.Region;
import me.pugabyte.bncore.utils.RandomUtils;
import me.pugabyte.bncore.utils.Tasks;
import me.pugabyte.bncore.utils.Time;
import org.bukkit.Axis;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.data.BlockData;
import org.bukkit.block.data.Orientable;
import org.bukkit.event.Listener;

import java.util.Arrays;
import java.util.List;

import static me.pugabyte.bncore.features.holidays.aeveonproject.APUtils.APLoc;

@Region("sialia_crashing")
public class SialiaCrashing implements Listener, APSet {
	public static boolean active = false;
	public static final Location shipRobot = APUtils.APLoc(-843, 85, -1088);
	//
	// sialia -> crashing = ~471 ~ ~-8
	//
	private final List<Location> light1 = Arrays.asList(APLoc(-824, 90, -1177), APLoc(-823, 90, -1177));
	private final List<Location> light2 = Arrays.asList(APLoc(-823, 90, -1173), APLoc(-824, 90, -1173));
	private final List<Location> light3 = Arrays.asList(APLoc(-823, 90, -1164), APLoc(-824, 90, -1164));
	private final List<Location> light4 = Arrays.asList(APLoc(-823, 90, -1160), APLoc(-824, 90, -1160));
	private final List<Location> light5 = Arrays.asList(APLoc(-823, 90, -1125), APLoc(-824, 90, -1125));
	private final List<Location> light6 = Arrays.asList(APLoc(-823, 90, -1121), APLoc(-824, 90, -1121));
	private final List<Location> light7 = Arrays.asList(APLoc(-823, 90, -1117), APLoc(-824, 90, -1117));
	private final List<Location> light8 = Arrays.asList(APLoc(-823, 90, -1113), APLoc(-824, 90, -1113));
	private final List<Location> light9 = Arrays.asList(APLoc(-823, 90, -1109), APLoc(-824, 90, -1109));
	private final List<Location> light10 = Arrays.asList(APLoc(-826, 89, -1103), APLoc(-821, 89, -1103));
	private final List<Location> light11 = Arrays.asList(APLoc(-826, 89, -1098), APLoc(-821, 89, -1098));
	private final List<Location> light12 = Arrays.asList(APLoc(-824, 75, -1169), APLoc(-824, 75, -1168),
			APLoc(-823, 75, -1169), APLoc(-823, 75, -1168));
	List<List<Location>> lights = Arrays.asList(light1, light2, light3, light4, light5, light6, light7, light8, light9, light10, light11, light12);
	//

	public SialiaCrashing() {
		BNCore.registerListener(this);

		new Sounds();
		new Particles();

		flickeringLightsTask();
	}

	private void flickeringLightsTask() {
		final Material boneBlock = Material.BONE_BLOCK;
		final Material seaLantern = Material.SEA_LANTERN;
		BlockData blockData = Material.BONE_BLOCK.createBlockData();

		Orientable orientable = (Orientable) blockData;
		orientable.setAxis(Axis.X);
		blockData = orientable;
		BlockData finalBlockData = blockData;

		Tasks.repeat(0, Time.TICK.x(10), () -> {
			if (!APSetType.SIALIA_CRASHING.get().isActive())
				return;

			for (List<Location> locs : lights) {
				if (RandomUtils.chanceOf(60)) {

					int wait = RandomUtils.randomInt(3, 10);
					for (Location loc : locs) {
						loc.getBlock().setType(seaLantern);

						Tasks.wait(Time.TICK.x(wait), () -> {
							loc.getBlock().setType(boneBlock);

							if (light10.contains(loc) || light11.contains(loc))
								loc.getBlock().setBlockData(finalBlockData);
						});
					}
				}
			}
		});
	}

	@Override
	public List<String> getUpdateRegions() {
		return Arrays.asList(APRegions.sialiaCrashing_shipColor, APRegions.sialiaCrashing_dockingport_1, APRegions.sialiaCrashing_dockingport_2, APRegions.sialiaCrashing_vent_door);
	}

	@Override
	public boolean isActive() {
		return active;
	}

	@Override
	public void setActive(boolean bool) {
		active = bool;
	}
}
