package me.pugabyte.nexus.features.commands;

import lombok.NonNull;
import me.pugabyte.nexus.features.chat.Chat;
import me.pugabyte.nexus.framework.commands.models.CustomCommand;
import me.pugabyte.nexus.framework.commands.models.annotations.Aliases;
import me.pugabyte.nexus.framework.commands.models.annotations.Arg;
import me.pugabyte.nexus.framework.commands.models.annotations.Path;
import me.pugabyte.nexus.framework.commands.models.events.CommandEvent;
import me.pugabyte.nexus.models.nerd.Nerd;
import me.pugabyte.nexus.utils.PlayerUtils;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.util.List;
import java.util.stream.Collectors;

@Aliases("nearby")
public class NearCommand extends CustomCommand {

	public NearCommand(@NonNull CommandEvent event) {
		super(event);
	}

	@Path("[player]")
	void run(@Arg("self") Player player) {
		List<Player> nearby = Bukkit.getOnlinePlayers().stream()
				.filter(_player -> player.getUniqueId() != _player.getUniqueId()
						 && player.getWorld() == _player.getWorld()
						 && getDistance(player, _player) <= Chat.getLocalRadius()
						 && (!isPlayer() || PlayerUtils.canSee(player(), _player)))
				.collect(Collectors.toList());

		boolean showDistance = player.hasPermission("near.distance");

		String message = "&ePlayers nearby" + (isSelf(player) ? "" : " " + player.getName()) + "";
		if (nearby.size() == 0)
			send(message + ": &fNone");
		else
			send(message + " (&3" + nearby.size() + "&e): &f" + nearby.stream()
					.map(_player -> {
						if (showDistance)
							return Nerd.of(_player).getNickname() + " (&3" + getDistance(player, _player) + "m&f)";
						else
							return Nerd.of(_player).getNickname();
					})
					.collect(Collectors.joining(", ")));
	}

	private long getDistance(@Arg("self") Player player, Player _player) {
		return Math.round(player.getLocation().distance(_player.getLocation()));
	}

}
