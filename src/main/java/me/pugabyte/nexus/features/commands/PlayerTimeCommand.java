package me.pugabyte.nexus.features.commands;

import me.pugabyte.nexus.framework.commands.models.CustomCommand;
import me.pugabyte.nexus.framework.commands.models.annotations.Aliases;
import me.pugabyte.nexus.framework.commands.models.annotations.Arg;
import me.pugabyte.nexus.framework.commands.models.annotations.Path;
import me.pugabyte.nexus.framework.commands.models.events.CommandEvent;
import me.pugabyte.nexus.utils.DescParseTickFormat;
import me.pugabyte.nexus.utils.PlayerUtils;
import org.bukkit.entity.Player;

@Aliases("ptime")
public class PlayerTimeCommand extends CustomCommand {

	public PlayerTimeCommand(CommandEvent event) {
		super(event);
	}

	@Path("reset")
	void reset() {
		player().resetPlayerTime();
		send(PREFIX + "Reset player time");
	}

	@Path("<time> [player]")
	public void time(String time, @Arg("self") Player player) {
		long ticks = PlayerUtils.setPlayerTime(player, time);
		if (player == player().getPlayer())
			send(PREFIX + "Player time set to &e" + DescParseTickFormat.format12(ticks) + " &3or &e" + ticks + " ticks");
		else {
			send(player, PREFIX + "Player time set to &e" + DescParseTickFormat.format12(ticks) + " &3or &e" + ticks + " ticks");
			send(PREFIX + "&e" + player.getName() + "'s&3 player time set to &e" + DescParseTickFormat.format12(ticks) + " &3or &e" + ticks + " ticks");
		}
	}

}
