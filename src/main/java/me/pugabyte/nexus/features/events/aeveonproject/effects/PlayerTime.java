package me.pugabyte.nexus.features.events.aeveonproject.effects;

import me.pugabyte.nexus.Nexus;
import me.pugabyte.nexus.features.events.aeveonproject.APUtils;
import me.pugabyte.nexus.utils.Tasks;
import me.pugabyte.nexus.utils.Time;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerChangedWorldEvent;

import java.util.Collection;

import static me.pugabyte.nexus.features.events.aeveonproject.AeveonProject.APWorld;

public class PlayerTime implements Listener {

	public PlayerTime() {
		Nexus.registerListener(this);

		Tasks.repeat(0, Time.TICK.x(10), () -> {
			Collection<? extends Player> players = Bukkit.getOnlinePlayers();
			for (Player player : players) {
				if (!APUtils.isInWorld(player)) continue;

				if (APUtils.isInSpace(player)) {
					if (player.getPlayerTime() != 570000)
						player.setPlayerTime(18000, false);
				} else {
					if (player.getPlayerTime() == 570000)
						player.resetPlayerTime();
				}
			}
		});
	}

	@EventHandler
	public void onWorldChange(PlayerChangedWorldEvent event) {
		if (event.getFrom().equals(APWorld))
			event.getPlayer().resetPlayerTime();
	}
}
