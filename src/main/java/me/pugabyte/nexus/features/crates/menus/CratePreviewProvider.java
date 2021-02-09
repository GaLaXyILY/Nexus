package me.pugabyte.nexus.features.crates.menus;

import fr.minuskube.inv.ClickableItem;
import fr.minuskube.inv.content.InventoryContents;
import fr.minuskube.inv.content.InventoryProvider;
import fr.minuskube.inv.content.Pagination;
import fr.minuskube.inv.content.SlotIterator;
import me.pugabyte.nexus.features.crates.Crates;
import me.pugabyte.nexus.features.crates.models.CrateLoot;
import me.pugabyte.nexus.features.crates.models.CrateType;
import me.pugabyte.nexus.features.menus.MenuUtils;
import me.pugabyte.nexus.utils.ItemBuilder;
import org.bukkit.Material;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public class CratePreviewProvider extends MenuUtils implements InventoryProvider {

	CrateType type;
	CrateLoot loot;

	public CratePreviewProvider(CrateType type, CrateLoot loot) {
		this.type = type;
		this.loot = loot;
	}

	@Override
	public void init(Player player, InventoryContents contents) {
		contents.fillBorders(ClickableItem.empty(new ItemBuilder(Material.GRAY_STAINED_GLASS_PANE).name(" ").build()));

		Pagination page = contents.pagination();

		List<ClickableItem> items = new ArrayList<>();
		if (loot == null)
			Crates.getLootByType(type).stream().sorted(Comparator.comparingDouble(CrateLoot::getWeight).reversed())
					.forEachOrdered(crateLoot -> {
						ItemBuilder builder = new ItemBuilder(crateLoot.getDisplayItem())
								.name("&e" + crateLoot.getTitle())
								.amount(1)
								.lore("&3Chance: &e" + crateLoot.getWeight());
						if (crateLoot.getItems().size() > 1)
							builder.lore("&7&oClick for more");
						items.add(ClickableItem.from(builder.build(), e -> {
							if (crateLoot.getItems().size() > 1)
								type.previewDrops(crateLoot).open(player);
						}));
					});
		else {
			loot.getItems().forEach(itemStack -> items.add(ClickableItem.empty(itemStack)));
			addBackItem(contents, e -> type.previewDrops(null).open(player));
		}

		page.setItems(items.toArray(new ClickableItem[0]));
		page.setItemsPerPage(28);
		SlotIterator.Impl iterator = new SlotIterator.Impl(contents, type.previewDrops(loot), SlotIterator.Type.HORIZONTAL, 1, 1);
		for (int c = 0; c < 2; c++)
			for (int r = 0; r < 6; r++)
				iterator.blacklist(r, c * 8);
		page.addToIterator(iterator);

		if (!page.isFirst())
			contents.set(0, 3, ClickableItem.from(new ItemBuilder(Material.ARROW).name("<-- Back").build(), e ->
					type.previewDrops(loot).open(player, page.last().getPage())));
		if (!page.isLast())
			contents.set(5, 3, ClickableItem.from(new ItemBuilder(Material.ARROW).name("Next -->").build(), e ->
					type.previewDrops(loot).open(player, page.next().getPage())));

	}

	@Override
	public void update(Player player, InventoryContents contents) {
	}
}
