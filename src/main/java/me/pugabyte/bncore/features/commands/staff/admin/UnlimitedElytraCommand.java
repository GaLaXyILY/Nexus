package me.pugabyte.bncore.features.commands.staff.admin;

import me.pugabyte.bncore.framework.commands.models.CustomCommand;
import me.pugabyte.bncore.framework.commands.models.annotations.Path;
import me.pugabyte.bncore.framework.commands.models.annotations.Permission;
import me.pugabyte.bncore.framework.commands.models.events.CommandEvent;
import me.pugabyte.bncore.utils.Tasks;
import me.pugabyte.bncore.utils.Time;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.util.Vector;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Permission("group.admin")
public class UnlimitedElytraCommand extends CustomCommand {

	private static final List<UUID> flyingPlayers = new ArrayList<>();
	double speedMultiplayer = 0.2;

	public UnlimitedElytraCommand(CommandEvent event) {
		super(event);

		Tasks.repeatAsync(0, Time.TICK.x(2), () -> {
			if (flyingPlayers.size() == 0)
				return;

			Tasks.sync(() -> {
				List<UUID> uuids = new ArrayList<>(flyingPlayers);
				for (UUID uuid : uuids) {
					Player player = Bukkit.getPlayer(uuid);
					if (player == null || !player.isGliding()) continue;

					Vector unitVector = new Vector(0, player.getLocation().getDirection().getY(), 0);
					player.setVelocity(player.getVelocity().add(unitVector.multiply(speedMultiplayer)));
				}
			});
		});
	}

	@Path("[enable]")
	public void elytra(Boolean enable) {
		UUID uuid = player().getUniqueId();

		if (enable == null)
			enable = !flyingPlayers.contains(uuid);

		if (enable) {
			flyingPlayers.add(uuid);
		} else {
			flyingPlayers.remove(uuid);
		}

		send(PREFIX + (enable ? "&aenabled" : "&cdisabled"));
	}


}
