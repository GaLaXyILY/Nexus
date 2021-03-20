package me.pugabyte.nexus.features.mcmmo;

import me.pugabyte.nexus.framework.commands.models.CustomCommand;
import me.pugabyte.nexus.framework.commands.models.annotations.Arg;
import me.pugabyte.nexus.framework.commands.models.annotations.Path;
import me.pugabyte.nexus.framework.commands.models.events.CommandEvent;
import me.pugabyte.nexus.models.mcmmo.McMMOPrestige;
import me.pugabyte.nexus.models.mcmmo.McMMOService;
import org.bukkit.OfflinePlayer;

public class McMMOPrestigeCommand extends CustomCommand {
	private McMMOService service = new McMMOService();

	public McMMOPrestigeCommand(CommandEvent event) {
		super(event);
	}

	@Path("[player]")
	void main(@Arg("self") OfflinePlayer player) {
		McMMOPrestige mcMMOPrestige = service.getPrestige(player.getUniqueId().toString());

		line();
		send("&ePrestige for " + player.getName());
		mcMMOPrestige.getPrestiges().forEach((type, count) -> send("&3" + camelCase(type) + ": &e" + count));

	}

}
