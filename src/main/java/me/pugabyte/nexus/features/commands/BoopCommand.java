package me.pugabyte.nexus.features.commands;

import me.pugabyte.nexus.features.minigames.Minigames;
import me.pugabyte.nexus.framework.commands.models.CustomCommand;
import me.pugabyte.nexus.framework.commands.models.annotations.Cooldown;
import me.pugabyte.nexus.framework.commands.models.annotations.Cooldown.Part;
import me.pugabyte.nexus.framework.commands.models.annotations.Description;
import me.pugabyte.nexus.framework.commands.models.annotations.Path;
import me.pugabyte.nexus.framework.commands.models.events.CommandEvent;
import me.pugabyte.nexus.utils.Time;
import org.bukkit.Sound;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class BoopCommand extends CustomCommand {

	public BoopCommand(CommandEvent event) {
		super(event);
	}

	@Path("<player> -a [message...]")
	@Description("boop a player anonymously")
	@Cooldown(value = @Part(value = Time.SECOND, x = 5), bypass = "group.admin")
	void boopAnon(Player playerArg, String message) {
		message = "-a " + message;
		boopPlayer(playerArg, message);
	}

	@Path("<player> [message...]")
	@Description("boop a player")
	@Cooldown(value = @Part(value = Time.SECOND, x = 5), bypass = "group.admin")
	void boopPlayer(Player playerArg, String message) {
		if (message == null)
			message = "";

		if (message.equalsIgnoreCase("-s"))
			error("The anon flag has been changed to -a");

		if (message.contains("-a")) {
			String[] messageSplit = message.split(" ");
			List<String> list = new ArrayList<>(Arrays.asList(messageSplit));
			if (list.get(0).equalsIgnoreCase("-a")) {
				if (list.size() > 1) {
					list.remove(0);
					message = String.join(" ", list);
				} else {
					message = "";
				}

				boop(player(), playerArg, message, true);
				return;
			}
		}

		boop(player(), playerArg, message, false);
	}

	public void boop(Player booper, Player booped, String message, boolean anon) {
		if (message == null)
			message = "";

		if (isSelf(booped))
			error("You cannot boop yourself!");

		if (Minigames.isMinigameWorld(booper.getWorld()))
			error("You cannot boop in minigames!");

		if (Minigames.isMinigameWorld(booped.getWorld()))
			error("You cannot boop " + booped.getName() + " (in minigames)");

		String toBooper = PREFIX;
		String toBooped = PREFIX;
		if (!message.equalsIgnoreCase(""))
			message = " &3and said &e" + message;

		if (anon) {
			toBooper += "&3You anonymously booped &e" + booped.getName() + message;
			toBooped += "&eSomebody &3booped you" + message;
		} else {
			toBooper += "&3You booped &e" + booped.getName() + message;
			toBooped += "&e" + name() + " &3booped you" + message;
		}

		send(toBooper);
		send(booped, toBooped);
		booped.playSound(booped.getLocation(), Sound.BLOCK_NOTE_BLOCK_XYLOPHONE, 10.0F, 0.1F);
	}
}
