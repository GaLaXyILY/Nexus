package me.pugabyte.nexus.features.minigames.mechanics;

import me.pugabyte.nexus.features.minigames.Minigames;
import me.pugabyte.nexus.features.minigames.models.Arena;
import me.pugabyte.nexus.features.minigames.models.Match;
import me.pugabyte.nexus.features.minigames.models.Minigamer;
import me.pugabyte.nexus.features.minigames.models.Team;
import me.pugabyte.nexus.features.minigames.models.matchdata.Flag;
import me.pugabyte.nexus.features.minigames.models.matchdata.OneFlagCaptureTheFlagMatchData;
import net.md_5.bungee.api.ChatColor;
import org.bukkit.Material;
import org.bukkit.block.Sign;
import org.bukkit.inventory.ItemStack;

import java.util.Map;

import static me.pugabyte.nexus.utils.StringUtils.stripColor;
import static me.pugabyte.nexus.utils.Utils.getMax;

public final class Siege extends OneFlagCaptureTheFlag {

	@Override
	public String getName() {
		return "Siege";
	}

	@Override
	public String getDescription() {
		return "One team protects their flag while the other tries to capture it";
	}

	@Override
	public ItemStack getMenuItem() {
		return new ItemStack(Material.GREEN_BANNER);
	}

	protected Team getAttackingTeam(Arena arena) {
		return arena.getTeams().stream().filter(team -> team.getName().toLowerCase().contains("attack")).findFirst().orElse(null);
	}

	protected Team getAttackingTeam(Match match) {
		return getAttackingTeam(match.getArena());
	}

	protected Team getDefendingTeam(Arena arena) {
		return arena.getTeams().stream().filter(team -> team.getName().toLowerCase().contains("defend")).findFirst().orElse(null);
	}

	protected Team getDefendingTeam(Match match) {
		return getDefendingTeam(match.getArena());
	}

	@Override
	public void announceWinners(Match match) {
		Map<Team, Integer> scores = match.getScores();

		int winningScore = getWinningScore(scores.values());

		String announcement;
		Team winningTeam;
		if (winningScore == 0) {
			winningTeam = getDefendingTeam(match);
			announcement = winningTeam == null ? "Defenders" : winningTeam.getColoredName();
			announcement += "&3 protected the flag on &e"+match.getArena().getName();
		} else {
			winningTeam = getMax(match.getAliveTeams(), team -> team.getScore(match)).getObject();
			announcement = winningTeam.getColoredName() + "&3 captured the flag on &e" + match.getArena().getName();
		}
		Minigames.broadcast(announcement);
	}

	@Override
	protected void onFlagInteract(Minigamer minigamer, Sign sign) {
		Match match = minigamer.getMatch();
		OneFlagCaptureTheFlagMatchData matchData = match.getMatchData();

		if (!minigamer.isPlaying(this)) return;

		if (matchData.getFlag() == null)
			matchData.setFlag(new Flag(sign, match));

		if ((ChatColor.GREEN + "Capture").equalsIgnoreCase(sign.getLine(2))) {
			if (!minigamer.equals(matchData.getFlagCarrier()))
				return;

			captureFlag(minigamer);
		} else if (!minigamer.getTeam().getName().equalsIgnoreCase(stripColor(sign.getLine(2))))
			takeFlag(minigamer);
		else if (minigamer.getTeam().getName().equalsIgnoreCase(stripColor(sign.getLine(2))) && !matchData.getFlag().getSpawnLocation().equals(sign.getLocation()))
			returnFlag(minigamer);
	}

	private void returnFlag(Minigamer minigamer) {
		Match match = minigamer.getMatch();
		OneFlagCaptureTheFlagMatchData matchData = match.getMatchData();

		flagMessage(match.getMinigamers(), minigamer, minigamer.getColoredName() + "&3 returned the flag", true);

		Flag flag = matchData.getFlag();
		if (flag != null) {
			flag.respawn();
			match.getTasks().cancel(flag.getTaskId());
		}
	}

}
