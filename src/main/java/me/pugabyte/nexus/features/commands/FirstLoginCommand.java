package me.pugabyte.nexus.features.commands;

import me.pugabyte.nexus.framework.commands.models.CustomCommand;
import me.pugabyte.nexus.framework.commands.models.annotations.Aliases;
import me.pugabyte.nexus.framework.commands.models.annotations.Arg;
import me.pugabyte.nexus.framework.commands.models.annotations.Path;
import me.pugabyte.nexus.framework.commands.models.events.CommandEvent;
import me.pugabyte.nexus.models.nerd.Nerd;
import me.pugabyte.nexus.utils.StringUtils;

@Aliases("firstjoin")
public class FirstLoginCommand extends CustomCommand {

	public FirstLoginCommand(CommandEvent event) {
		super(event);
	}

	@Path("[player]")
	void firstJoin(@Arg("self") Nerd nerd) {
		send("&e&l" + nerd.getName() + " &3first joined Bear Nation on &e" + StringUtils.longDateTimeFormat(nerd.getFirstJoin()) + " &3US Eastern Time");
	}
}
