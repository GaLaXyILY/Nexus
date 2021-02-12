package me.pugabyte.nexus.features.votes.vps;

import fr.minuskube.inv.ClickableItem;
import fr.minuskube.inv.content.InventoryContents;
import fr.minuskube.inv.content.InventoryProvider;
import me.pugabyte.nexus.Nexus;
import me.pugabyte.nexus.features.menus.MenuUtils;
import me.pugabyte.nexus.features.votes.vps.VPSMenu.VPSPage;
import me.pugabyte.nexus.features.votes.vps.VPSMenu.VPSPage.VPSSlot;
import me.pugabyte.nexus.models.vote.VoteService;
import me.pugabyte.nexus.models.vote.Voter;
import me.pugabyte.nexus.utils.ItemBuilder;
import me.pugabyte.nexus.utils.ItemUtils;
import me.pugabyte.nexus.utils.PlayerUtils;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static me.pugabyte.nexus.features.votes.vps.VPS.PREFIX;
import static me.pugabyte.nexus.utils.StringUtils.plural;
import static me.pugabyte.nexus.utils.StringUtils.stripColor;

public class VPSProvider extends MenuUtils implements InventoryProvider {
	private VPSMenu menu;
	private VPSPage page;
	private int index;

	public VPSProvider(VPSMenu menu, VPSPage page) {
		this.menu = menu;
		this.page = page;
		this.index = menu.indexOf(page) + 1;
	}

	@Override
	public void init(Player player, InventoryContents contents) {
		Voter voter = new VoteService().get(player);

		addCloseItem(contents);
		addPagination(contents, player);
		contents.set(0, 8, ClickableItem.empty(new ItemBuilder(Material.BOOK).name("&3You have &e" + voter.getPoints() + " &3vote points").build()));

		page.getItems().forEach((slot, item) -> {
			ItemStack display = item.getDisplay().clone();
			if (item.getPrice() > 0)
				if (voter.getPoints() >= item.getPrice())
					ItemBuilder.addLore(display, "", "&6Price: &e" + item.getPrice());
				else
					ItemBuilder.addLore(display, "", "&6Price: &c" + item.getPrice());

			contents.set(slot, ClickableItem.from(display, e -> {
				if (voter.getPoints() < item.getPrice()) {
					PlayerUtils.send(player, PREFIX + "&cYou do not have enough vote points! &3Use &c/vote &3to vote!");
					return;
				}

				if (item.getOnPurchase() != null)
					if (!item.getOnPurchase().test(player, item))
						return;

				if (item.getMoney() > 0)
					Nexus.getEcon().depositPlayer(player, item.getMoney());
				if (item.getConsoleCommand() != null && item.getConsoleCommand().length() > 0)
					PlayerUtils.runCommandAsConsole(item.getConsoleCommand().replaceAll("\\[player]", player.getName()));
				if (item.getCommand() != null && item.getCommand().length() > 0)
					PlayerUtils.runCommand(player, item.getCommand().replaceAll("\\[player]", player.getName()));
				if (item.getItems() != null && item.getItems().size() > 0)
					ItemUtils.giveItems(player, item.getItems());

				if (item.getPrice() > 0) {
					voter.takePoints(item.getPrice());
					PlayerUtils.send(player, PREFIX + "You spent &e" + item.getPrice() + plural(" &3point", item.getPrice())
							+ " on &e" + stripColor(item.getName()) + "&3. &e" + voter.getPoints() + " &3points remaining.");
				}

				log(player, item);

				if (item.isClose())
					player.closeInventory();
				else
					VPS.open(player, menu, index);
			}));
		});
	}

	public void addPagination(InventoryContents contents, Player player) {
		if (!menu.isFirst(page)) {
			ItemStack back = new ItemBuilder(Material.PAPER).amount(index - 1).name("&6<-").build();
			contents.set(5, 0, ClickableItem.from(back, e -> VPS.open(player, menu, index - 1)));
		}
		if (!menu.isLast(page)) {
			ItemStack forward = new ItemBuilder(Material.PAPER).amount(index + 1).name("&6->").build();
			contents.set(5, 8, ClickableItem.from(forward, e -> VPS.open(player, menu, index + 1)));
		}
	}

	@Override
	public void update(Player player, InventoryContents contents) {
	}


	public void log(Player player, VPSSlot vpsSlot) {
		List<String> columns = new ArrayList<>(Arrays.asList(
				DateTimeFormatter.ISO_LOCAL_DATE_TIME.format(LocalDateTime.now()),
				player.getUniqueId().toString(),
				player.getName(),
				vpsSlot.getName(),
				String.valueOf(vpsSlot.getPrice())
		));

		Nexus.csvLog("vps", String.join(",", columns));
	}

}
