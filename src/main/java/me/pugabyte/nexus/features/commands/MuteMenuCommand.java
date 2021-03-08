package me.pugabyte.nexus.features.commands;

import fr.minuskube.inv.ClickableItem;
import fr.minuskube.inv.SmartInventory;
import fr.minuskube.inv.content.InventoryContents;
import fr.minuskube.inv.content.InventoryProvider;
import joptsimple.internal.Strings;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import me.pugabyte.nexus.features.menus.MenuUtils;
import me.pugabyte.nexus.framework.commands.models.CustomCommand;
import me.pugabyte.nexus.framework.commands.models.annotations.Path;
import me.pugabyte.nexus.framework.commands.models.events.CommandEvent;
import me.pugabyte.nexus.models.mutemenu.MuteMenuService;
import me.pugabyte.nexus.models.mutemenu.MuteMenuUser;
import me.pugabyte.nexus.utils.ItemBuilder;
import me.pugabyte.nexus.utils.PlayerUtils;
import me.pugabyte.nexus.utils.StringUtils;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemStack;

import static me.pugabyte.nexus.utils.StringUtils.colorize;

public class MuteMenuCommand extends CustomCommand {

	public MuteMenuCommand(CommandEvent event) {
		super(event);
	}

	@Path
	void muteMenu() {
		new MuteMenuProvider().open(player());
	}

	public static class MuteMenuProvider extends MenuUtils implements InventoryProvider {
		private final MuteMenuService service = new MuteMenuService();
		private PageType pageType = PageType.MESSAGES;

		@Getter
		@AllArgsConstructor
		@RequiredArgsConstructor
		public enum MuteMenuItem {
			CHANNEL_GLOBAL("Global and Discord Chat", Material.GREEN_WOOL),
			CHANNEL_LOCAL("Local Chat", Material.YELLOW_WOOL),
			CHANNEL_MINIGAMES("Minigames Chat", Material.CYAN_WOOL, "chat.use.minigames"),
			CHANNEL_CREATIVE("Creative Chat", Material.LIGHT_BLUE_WOOL, "chat.use.creative"),
			CHANNEL_SKYBLOCK("Skyblock Chat", Material.ORANGE_WOOL, "chat.use.skyblock"),
			REMINDERS("Reminders", Material.REPEATER),
			AFK("AFK Broadcasts", Material.REDSTONE_LAMP),
			JOIN_QUIT("Join/Quit Messages", Material.OAK_FENCE_GATE),
			EVENTS("Event Broadcasts", Material.BEACON),
			MINIGAMES("Minigame Broadcasts", Material.DIAMOND_SWORD), // TODO
			// Sounds
			FIRST_JOIN_SOUND("First Join", Material.GOLD_BLOCK, 50),
			JOIN_QUIT_SOUNDS("Join/Quit", Material.NOTE_BLOCK, 50),
			ALERTS("Alerts", Material.NAME_TAG, 50),
			RANK_UP("Rank Up", Material.EMERALD, 50);

			@NonNull
			public String title;
			@NonNull
			public Material material;
			public String permission = null;
			public Integer defaultVolume = null;

			MuteMenuItem(String title, Material material, int defaultVolume) {
				this.title = title;
				this.material = material;
				this.defaultVolume = defaultVolume;
			}

			MuteMenuItem(String title, Material material, String permission) {
				this.title = title;
				this.material = material;
				this.permission = permission;
			}
		}

		@Override
		public void open(Player viewer, int page) {
			open(viewer, PageType.MESSAGES);
		}

		public void open(Player viewer, PageType pageType) {
			this.pageType = pageType;
			String title = "&3" + StringUtils.camelCase(pageType.name());
			SmartInventory.builder()
					.title(colorize(title))
					.size(getRows(getViewableItems(viewer, pageType), 2, 7), 9)
					.provider(this)
					.build()
					.open(viewer);
		}

		@Override
		public void init(Player player, InventoryContents contents) {
			MuteMenuUser user = service.get(player.getUniqueId());
			int row = 1;
			int column = 1;

			if (pageType.equals(PageType.MESSAGES)) {
				addCloseItem(contents);
				contents.set(0, 8, ClickableItem.from(nameItem(Material.COMMAND_BLOCK, "&dSounds"), e -> open(player, PageType.SOUNDS)));

				for (MuteMenuItem item : MuteMenuItem.values()) {
					if (item.getDefaultVolume() != null)
						continue;
					if (!Strings.isNullOrEmpty(item.getPermission()) && !player.hasPermission(item.getPermission()))
						continue;

					boolean muted = user.hasMuted(item);
					ItemStack stack = nameItem(item.getMaterial(), "&e" + item.getTitle(), muted ? "&cMuted" : "&aUnmuted");
					if (muted)
						addGlowing(stack);

					contents.set(row, column, ClickableItem.from(stack, e -> {
						toggleMute(user, item);
						open(player, PageType.MESSAGES);
					}));

					if (column == 7) {
						column = 1;
						row++;
					} else
						column++;
				}
			} else {
				addBackItem(contents, e -> open(player, PageType.MESSAGES));

				ItemStack info = new ItemBuilder(Material.BOOK)
						.name("&3Info")
						.lore("&eLClick - Increase volume", "&eRClick - Decrease volume")
						.build();
				contents.set(0, 8, ClickableItem.empty(info));

				for (MuteMenuItem item : MuteMenuItem.values()) {
					if (item.getDefaultVolume() == null)
						continue;
					if (!Strings.isNullOrEmpty(item.getPermission()) && !player.hasPermission(item.getPermission()))
						continue;

					boolean muted = user.hasMuted(item);
					int volume = user.getVolume(item);
					ItemStack stack = nameItem(item.getMaterial(), "&e" + item.getTitle(), muted ? "&c0%" : "&a" + volume + "%");
					if (muted)
						addGlowing(stack);

					contents.set(row, column, ClickableItem.from(stack, e -> {
						InventoryClickEvent clickEvent = ((InventoryClickEvent) e.getEvent());
						if (clickEvent.isRightClick())
							decreaseVolume(user, item);
						else if (clickEvent.isLeftClick())
							increaseVolume(user, item);
						open(player, PageType.SOUNDS);
					}));

					if (column == 7) {
						column = 1;
						row++;
					} else
						column++;
				}
			}

		}

		private void increaseVolume(MuteMenuUser user, MuteMenuItem item) {
			int previous = user.getVolume(item);
			int current = Math.min(previous + 10, 100);
			user.setVolume(item, current);
			service.save(user);
		}

		private void decreaseVolume(MuteMenuUser user, MuteMenuItem item) {
			int previous = user.getVolume(item);
			int current = Math.max(0, previous - 10);
			user.setVolume(item, current);
			service.save(user);
		}

		public void toggleMute(MuteMenuUser user, MuteMenuItem item) {
			Player player = user.getPlayer();
			if (item.name().startsWith("CHANNEL_"))
				if (user.hasMuted(item))
					PlayerUtils.runCommand(player, "ch join " + item.name().replace("CHANNEL_", "").toLowerCase());
				else
					PlayerUtils.runCommand(player, "ch leave " + item.name().replace("CHANNEL_", "").toLowerCase());
			else {
				if (user.hasMuted(item))
					user.unMute(item);
				else
					user.mute(item);

				service.save(user);
			}
		}

		@Override
		public void update(Player player, InventoryContents contents) {
		}

		private int getViewableItems(Player player, PageType pageType) {
			int count = 0;
			if (pageType.equals(PageType.SOUNDS)) {
				for (MuteMenuItem item : MuteMenuItem.values()) {
					if (item.getDefaultVolume() != null)
						count++;
				}

			} else {
				for (MuteMenuItem item : MuteMenuItem.values()) {
					if (item.getDefaultVolume() != null)
						continue;
					if (Strings.isNullOrEmpty(item.getPermission()) || player.hasPermission(item.getPermission()))
						count++;
				}
			}
			return count;
		}

		private enum PageType {
			MESSAGES,
			SOUNDS
		}
	}


}
