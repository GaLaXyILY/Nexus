package me.pugabyte.nexus.features.discord.commands;

import com.jagrosh.jdautilities.command.Command;
import com.jagrosh.jdautilities.command.CommandEvent;
import me.pugabyte.nexus.features.discord.Bot;
import me.pugabyte.nexus.features.discord.Bot.HandledBy;
import me.pugabyte.nexus.framework.exceptions.NexusException;
import me.pugabyte.nexus.utils.Tasks;

import static me.pugabyte.nexus.utils.StringUtils.stripColor;

@HandledBy(Bot.KODA)
public class SocialMediaCommand extends Command {

	public SocialMediaCommand() {
		this.name = "socialmedia";
		this.aliases = new String[]{"sm"};
		this.guildOnly = true;
		this.requiredRole = "Staff";
	}

	protected void execute(CommandEvent event) {
		Tasks.async(() -> {
			try {

			} catch (Exception ex) {
				event.reply(stripColor(ex.getMessage()));
				if (!(ex instanceof NexusException))
					ex.printStackTrace();
			}
		});
	}

}
