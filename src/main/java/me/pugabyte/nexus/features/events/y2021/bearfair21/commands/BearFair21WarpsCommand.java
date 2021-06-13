package me.pugabyte.nexus.features.events.y2021.bearfair21.commands;

import me.pugabyte.nexus.features.events.y2021.bearfair21.BearFair21;
import me.pugabyte.nexus.features.warps.commands._WarpCommand;
import me.pugabyte.nexus.framework.commands.models.annotations.Aliases;
import me.pugabyte.nexus.framework.commands.models.annotations.Path;
import me.pugabyte.nexus.framework.commands.models.events.CommandEvent;
import me.pugabyte.nexus.models.warps.WarpService;
import me.pugabyte.nexus.models.warps.WarpType;

@Aliases({"bearfair21warp", "bf21warps", "bf21warp"})
public class BearFair21WarpsCommand extends _WarpCommand {

	public BearFair21WarpsCommand(CommandEvent event) {
		super(event);
	}

	@Path
	public void bearfair() {
		if (!BearFair21.canWarp() && !isStaff())
			error("Soon™");

		teleport(new WarpService().get("bearfair", WarpType.BEAR_FAIR21));
	}

	@Override
	public WarpType getWarpType() {
		return WarpType.BEAR_FAIR21;
	}
}
