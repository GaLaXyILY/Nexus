package me.pugabyte.nexus.features.radio;

import com.xxmicloxx.NoteBlockAPI.model.FadeType;
import com.xxmicloxx.NoteBlockAPI.model.Playlist;
import com.xxmicloxx.NoteBlockAPI.model.RepeatMode;
import com.xxmicloxx.NoteBlockAPI.model.Song;
import com.xxmicloxx.NoteBlockAPI.model.SoundCategory;
import com.xxmicloxx.NoteBlockAPI.songplayer.Fade;
import com.xxmicloxx.NoteBlockAPI.songplayer.PositionSongPlayer;
import com.xxmicloxx.NoteBlockAPI.songplayer.RadioSongPlayer;
import com.xxmicloxx.NoteBlockAPI.songplayer.SongPlayer;
import me.pugabyte.nexus.Nexus;
import me.pugabyte.nexus.models.radio.RadioConfig;
import me.pugabyte.nexus.models.radio.RadioConfig.Radio;
import me.pugabyte.nexus.models.radio.RadioConfigService;
import me.pugabyte.nexus.models.radio.RadioType;
import me.pugabyte.nexus.models.radio.RadioUser;
import me.pugabyte.nexus.models.radio.RadioUserService;
import me.pugabyte.nexus.utils.ActionBarUtils;
import org.bukkit.entity.Player;

import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

public class RadioUtils {

	public static void actionBar(Player player, Song song) {
		actionBar(player, song, false);
	}

	public static void actionBar(Player player, Song song, boolean nowPlaying) {
		String message = "&2&lCurrently Playing: &a";
		if (nowPlaying) message = "&2&lNow Playing: &a";
		message += " " + song.getTitle();
		ActionBarUtils.sendActionBar(player, message);
	}

	public static boolean isListening(Player player, Radio radio) {
		return radio.getSongPlayer().getPlayerUUIDs().contains(player.getUniqueId());
	}

	public static Radio getListenedRadio(Player player) {
		return getListenedRadio(player, false);
	}

	public static Radio getListenedRadio(Player player, boolean includeRadius) {
		for (Radio radio : getRadios()) {
			SongPlayer songPlayer = radio.getSongPlayer();

			if (songPlayer instanceof RadioSongPlayer) {
				if (isListening(player, radio))
					return radio;

			} else if (songPlayer instanceof PositionSongPlayer) {
				if (isListening(player, radio)) {
					if (includeRadius)
						if (isInRangeOfRadiusRadio(player, radio))
							return radio;
						else
							return radio;
				}
			}
		}

		return null;
	}

	public static Set<Radio> getRadios() {
		RadioConfigService configService = new RadioConfigService();
		RadioConfig config = configService.get(Nexus.getUUID0());
		return config.getRadios();
	}

	public static Set<Radio> getServerRadios() {
		Set<Radio> result = new HashSet<>();
		for (Radio radio : getRadios()) {
			if (radio.getType().equals(RadioType.SERVER))
				result.add(radio);
		}
		return result;
	}

	public static Radio getRadio(SongPlayer songPlayer) {
		for (Radio radio : getRadios()) {
			if (songPlayer.equals(radio.getSongPlayer()))
				return radio;
		}
		return null;
	}

	public static void setRadioDefaults(SongPlayer radio) {
		if (radio instanceof RadioSongPlayer)
			((RadioSongPlayer) radio).setStereo(true);

		Fade fadeIn = radio.getFadeIn();
		fadeIn.setType(FadeType.LINEAR);
		fadeIn.setFadeDuration(60);

		Fade fadeOut = radio.getFadeOut();
		fadeOut.setType(FadeType.LINEAR);
		fadeOut.setFadeDuration(60);

		radio.setRepeatMode(RepeatMode.ALL);
		radio.setCategory(SoundCategory.RECORDS);
		radio.setPlaying(true);
	}

	public static void addPlayer(Player player, Radio radio) {
		if (!RadioUtils.isListening(player, radio)) {
			RadioUserService userService = new RadioUserService();
			RadioUser user = userService.get(player);

			if (radio.getType().equals(RadioType.SERVER)) {
				user.setServerRadioId(radio.getId());
				userService.save(user);
			}

			SongPlayer songPlayer = radio.getSongPlayer();
			songPlayer.addPlayer(player);
			if (songPlayer instanceof RadioSongPlayer)
				actionBar(player, songPlayer.getSong());
			else if (songPlayer instanceof PositionSongPlayer) {
				if (RadioUtils.isInRangeOfRadiusRadio(player, radio))
					actionBar(player, songPlayer.getSong());
			}
		}
	}

	public static void removePlayer(Player player, Radio radio) {
		if (radio == null)
			return;

		if (RadioUtils.isListening(player, radio)) {
			RadioUserService userService = new RadioUserService();
			RadioUser user = userService.get(player);

			if (radio.getType().equals(RadioType.SERVER)) {
				user.setServerRadioId(null);
				userService.save(user);
			}

			SongPlayer songPlayer = radio.getSongPlayer();
			songPlayer.removePlayer(player);
			if (radio.getSongPlayer() instanceof RadioSongPlayer)
				ActionBarUtils.sendActionBar(player, "&c&lYou have left the server radio");
		}
	}

	public static void removeRadio(Radio radio) {
		if (radio == null)
			return;

		RadioFeature.removeSongPlayer(radio.getSongPlayer());

		RadioConfigService configService = new RadioConfigService();
		RadioConfig config = configService.get(Nexus.getUUID0());
		config.getRadios().remove(radio);
		configService.save(config);
	}

	public static boolean isInRangeOfRadiusRadio(Player player) {
		return getRadiusRadio(player) != null;
	}

	public static Radio getRadiusRadio(Player player) {
		for (Radio radio : getRadios()) {
			if (isInRangeOfRadiusRadio(player, radio))
				return radio;
		}
		return null;
	}

	public static boolean isInRangeOfRadiusRadio(Player player, Radio radio) {
		SongPlayer songPlayer = radio.getSongPlayer();
		if (songPlayer == null) return false;
		if (!(songPlayer instanceof PositionSongPlayer)) return false;

		PositionSongPlayer positionSongPlayer = (PositionSongPlayer) songPlayer;
		if (positionSongPlayer.getTargetLocation() == null) return false;
		if (positionSongPlayer.getTargetLocation().getWorld() == null) return false;
		return positionSongPlayer.getTargetLocation().getWorld().equals(player.getWorld()) && positionSongPlayer.isInRange(player);
	}

	public static Playlist shufflePlaylist(Playlist playlist) {
		List<Song> songList = playlist.getSongList();
		Collections.shuffle(songList);
		return new Playlist(songList.toArray(new Song[0]));
	}

	public static List<String> getPlaylistHover(Radio radio) {
		AtomicInteger ndx = new AtomicInteger(1);
		List<Song> songList = radio.getSongPlayer().getPlaylist().getSongList();
		return songList.stream().map(song -> "&3" + ndx.getAndIncrement() + " &e" + song.getTitle()).collect(Collectors.toList());
	}
}
