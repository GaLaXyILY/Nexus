package me.pugabyte.nexus.features.commands;

import me.pugabyte.nexus.framework.commands.models.CustomCommand;
import me.pugabyte.nexus.framework.commands.models.annotations.Path;
import me.pugabyte.nexus.framework.commands.models.events.CommandEvent;
import org.bukkit.Location;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class MeasureCommand extends CustomCommand {

	public MeasureCommand(CommandEvent event) {
		super(event);
	}

	static Map<UUID, Location> playerMap = new HashMap<>();

	@Path("1")
	void one() {
		playerMap.put(uuid(), location());
		send(PREFIX + "First position set");
	}

	@Path("2")
	void two() {
		if (!playerMap.containsKey(uuid()))
			error("You have not set your first position yet");

		send("&3Distance: &e" + (int) playerMap.get(uuid()).distance(location()));
		playerMap.remove(uuid());
	}

}
