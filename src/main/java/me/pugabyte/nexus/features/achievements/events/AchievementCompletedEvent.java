package me.pugabyte.nexus.features.achievements.events;

import lombok.Data;
import me.pugabyte.nexus.models.achievement.Achievement;
import me.pugabyte.nexus.models.achievement.AchievementPlayer;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;

@Data
public class AchievementCompletedEvent extends Event {
	private static final HandlerList handlers = new HandlerList();
	private AchievementPlayer achievementPlayer;
	private Achievement achievement;

	public AchievementCompletedEvent(AchievementPlayer achievementPlayer, Achievement achievement) {
		this.achievementPlayer = achievementPlayer;
		this.achievement = achievement;
	}

	public static HandlerList getHandlerList() {
		return handlers;
	}

	@Override
	public HandlerList getHandlers() {
		return handlers;
	}

}
