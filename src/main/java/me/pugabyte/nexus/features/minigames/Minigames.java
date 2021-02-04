package me.pugabyte.nexus.features.minigames;

import com.sk89q.worldguard.protection.regions.ProtectedRegion;
import lombok.Getter;
import me.lucko.helper.Services;
import me.lucko.helper.scoreboard.PacketScoreboard;
import me.lucko.helper.scoreboard.PacketScoreboardProvider;
import me.pugabyte.nexus.Nexus;
import me.pugabyte.nexus.features.minigames.lobby.ActionBar;
import me.pugabyte.nexus.features.minigames.lobby.Basketball;
import me.pugabyte.nexus.features.minigames.lobby.Parkour;
import me.pugabyte.nexus.features.minigames.managers.ArenaManager;
import me.pugabyte.nexus.features.minigames.managers.MatchManager;
import me.pugabyte.nexus.features.minigames.managers.PlayerManager;
import me.pugabyte.nexus.features.minigames.menus.MinigamesMenus;
import me.pugabyte.nexus.features.minigames.models.Match;
import me.pugabyte.nexus.features.minigames.models.MatchData;
import me.pugabyte.nexus.features.minigames.models.Minigamer;
import me.pugabyte.nexus.features.minigames.models.annotations.MatchDataFor;
import me.pugabyte.nexus.features.minigames.models.mechanics.Mechanic;
import me.pugabyte.nexus.features.minigames.models.mechanics.MechanicType;
import me.pugabyte.nexus.framework.features.Feature;
import me.pugabyte.nexus.utils.PlayerUtils;
import me.pugabyte.nexus.utils.StringUtils;
import me.pugabyte.nexus.utils.Tasks;
import me.pugabyte.nexus.utils.Time;
import me.pugabyte.nexus.utils.WorldEditUtils;
import me.pugabyte.nexus.utils.WorldGroup;
import me.pugabyte.nexus.utils.WorldGuardUtils;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.configuration.serialization.ConfigurationSerializable;
import org.bukkit.configuration.serialization.ConfigurationSerialization;
import org.bukkit.configuration.serialization.SerializableAs;
import org.bukkit.entity.Player;
import org.bukkit.event.Listener;
import org.reflections.Reflections;

import java.lang.reflect.Constructor;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

public class Minigames extends Feature {
	public static final String PREFIX = StringUtils.getPrefix("Minigames");
	@Getter
	private static final World world = Bukkit.getWorld("gameworld");
	@Getter
	private static final Location lobby = new Location(world, 1861.5, 38.1, 247.5, 0, 0);
	@Getter
	@Deprecated // Use Match#getWGUtils or Arena#getWGUtils
	private static final WorldGuardUtils worldGuardUtils = new WorldGuardUtils(world);
	@Getter
	@Deprecated // Use Match#getWEUtils or Arena#getWEUtils
	private static final WorldEditUtils worldEditUtils = new WorldEditUtils(world);
	@Getter
	private static final ProtectedRegion lobbyRegion = worldGuardUtils.getProtectedRegion("minigamelobby");
	@Getter
	public static final MinigamesMenus menus = new MinigamesMenus();
	@Getter
	public static final PacketScoreboard scoreboard = Services.load(PacketScoreboardProvider.class).getScoreboard();

	@Override
	public void startup() {
		registerSerializables();
		registerMatchDatas();
		ArenaManager.read();
		registerListeners();
		Tasks.repeat(Time.SECOND.x(5), 10, MatchManager::janitor);

		new ActionBar();
		new Basketball();
		new Parkour();
	}

	@Override
	public void shutdown() {
		new ArrayList<>(MatchManager.getAll()).forEach(Match::end);
		ArenaManager.write();
	}

	public static boolean isMinigameWorld(World world) {
		return WorldGroup.get(world) == WorldGroup.MINIGAMES;
	}

	public static List<Player> getPlayers() {
		return Bukkit.getOnlinePlayers().stream().filter(player -> isMinigameWorld(player.getWorld())).collect(Collectors.toList());
	}

	public static List<Minigamer> getMinigamers() {
		return getPlayers().stream().map(PlayerManager::get).collect(Collectors.toList());
	}

	public static List<Minigamer> getActiveMinigamers() {
		return getPlayers().stream().map(PlayerManager::get).filter(minigamer -> minigamer.getMatch() != null).collect(Collectors.toList());
	}

	public static void broadcast(String announcement) {
		getPlayers().forEach(player -> PlayerUtils.send(player, Minigames.PREFIX + announcement));

		// TODO: If arena is public, announce to discord and whole server
	}

	// Registration

	private String getPath() {
		return this.getClass().getPackage().getName();
	}

	private void registerListeners() {
		for (Class<? extends Listener> clazz : new Reflections(getPath() + ".listeners").getSubTypesOf(Listener.class)) {
			try {
				Nexus.registerListener(clazz.newInstance());
			} catch (Exception ex) {
				ex.printStackTrace();
			}
		}
	}

	private void registerSerializables() {
		new Reflections(getPath()).getTypesAnnotatedWith(SerializableAs.class).forEach(clazz -> {
			String alias = clazz.getAnnotation(SerializableAs.class).value();
			ConfigurationSerialization.registerClass((Class<? extends ConfigurationSerializable>) clazz, alias);
		});
	}

	@Getter
	private static final Map<Mechanic, Constructor<?>> matchDataMap = new HashMap<>();

	public static void registerMatchDatas() {
		try {
			String path = Minigames.class.getPackage().getName();
			Set<Class<? extends MatchData>> matchDataTypes = new Reflections(path + ".models.matchdata")
					.getSubTypesOf(MatchData.class);

			for (Class<?> matchDataType : matchDataTypes)
				if (matchDataType.getAnnotation(MatchDataFor.class) != null)
					for (MechanicType mechanicType : MechanicType.values())
						for (Class<? extends Mechanic> superclass : mechanicType.get().getSuperclasses())
							for (Class<? extends Mechanic> applicableMechanic : matchDataType.getAnnotation(MatchDataFor.class).value())
							if (applicableMechanic.equals(superclass))
								try {
									Constructor<?> constructor = matchDataType.getConstructor(Match.class);
									constructor.setAccessible(true);
									matchDataMap.put(mechanicType.get(), constructor);
								} catch (NoSuchMethodException ex) {
									Nexus.warn("MatchData " + matchDataType.getSimpleName() + " has no Match constructor");
								}
		} catch (Exception ex) {
			ex.printStackTrace();
		}
	}

}
