package me.pugabyte.nexus.features.achievements;

import me.pugabyte.nexus.framework.features.Feature;
import me.pugabyte.nexus.utils.Tasks;
import me.pugabyte.nexus.utils.Time;
import org.bukkit.Bukkit;
import org.bukkit.GameRule;
import org.bukkit.World;

public class Achievements extends Feature {

	// TODO:
	// - Test DB implementation, Mongo may not like Object
	// - Update economy listener
	// - Update event listeners (see events folder)
	// - Add more achievements

	@Override
	public void startup() {
		Tasks.wait(Time.SECOND.x(5), () -> {
			for (World world : Bukkit.getWorlds())
				world.setGameRule(GameRule.ANNOUNCE_ADVANCEMENTS, false);
		});

	/*
		new Reflections(getClass().getPackage().getName()).getSubTypesOf(Listener.class).forEach(listener -> {
			try {
				if (listener.getAnnotation(Disabled.class) == null)
					Nexus.registerListener(new ObjenesisStd().newInstance(listener));
			} catch (Exception ex) {
				ex.printStackTrace();
			}
		});
	*/
	}

}
