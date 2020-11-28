package me.pugabyte.nexus.features.delivery;

import fr.minuskube.inv.ClickableItem;
import fr.minuskube.inv.SmartInventory;
import fr.minuskube.inv.content.InventoryContents;
import fr.minuskube.inv.content.InventoryProvider;
import fr.minuskube.inv.content.SlotPos;
import me.pugabyte.nexus.features.menus.MenuUtils;
import me.pugabyte.nexus.models.delivery.Delivery;
import me.pugabyte.nexus.models.delivery.DeliveryService;
import me.pugabyte.nexus.utils.ItemBuilder;
import me.pugabyte.nexus.utils.PlayerUtils;
import me.pugabyte.nexus.utils.WorldGroup;
import net.md_5.bungee.api.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.Arrays;
import java.util.List;
import java.util.function.Consumer;

import static me.pugabyte.nexus.utils.StringUtils.camelCase;

public class DeliveryWorldMenu extends MenuUtils implements InventoryProvider {
	private final List<ItemStack> items;
	private final DeliveryService service = new DeliveryService();
	private final SmartInventory menu;

	public DeliveryWorldMenu(ItemStack... items) {
		this(Arrays.asList(items));
	}

	public DeliveryWorldMenu(List<ItemStack> items) {
		this.items = items;

		menu = SmartInventory.builder()
				.provider(this)
				.size(3, 9)
				.title(ChatColor.DARK_AQUA + "Choose world to deliver to")
				.closeable(false)
				.build();
	}

	public void open(Player player) {
		menu.open(player);
	}

	@Override
	public void init(Player player, InventoryContents contents) {
		Delivery delivery = service.get(player);
		ItemStack survival = new ItemBuilder(Material.GRASS_BLOCK).name("&3Survival").build();
		ItemStack creative = new ItemBuilder(Material.QUARTZ_BLOCK).name("&3Creative").build();
		ItemStack skyblock = new ItemBuilder(Material.COBBLESTONE).name("&3Skyblock").lore("&cCurrently disabled").build();

		Consumer<WorldGroup> save = worldGroup -> {
			delivery.add(worldGroup, items);
			service.save(delivery);

			menu.close(player);
			PlayerUtils.send(player, DeliveryCommand.PREFIX + "Your items have been delivered to &e" + camelCase(worldGroup));
		};

		contents.set(new SlotPos(1, 2), ClickableItem.from(survival, e -> save.accept(WorldGroup.SURVIVAL)));
		contents.set(new SlotPos(1, 4), ClickableItem.from(creative, e -> save.accept(WorldGroup.CREATIVE)));
		contents.set(new SlotPos(1, 6), ClickableItem.empty(skyblock));

//			Tasks.async(() -> {
//				Utils.send(player, "Size1: " + delivery.getSurvivalItems().size());
//				Utils.send(player, stripColor(delivery.getSurvivalItems().toString());
//				service.deleteSync(delivery);
//
//				Utils.send(player, "\nSize1.5: " + delivery.getSurvivalItems().size());
//				Utils.send(player, stripColor(delivery.getSurvivalItems().toString());
//				service.saveSync(delivery);
//
//				Delivery delivery1 = service.get(player);
//				Utils.send(player, "\nSize2: " + delivery1.getSurvivalItems().size());
//				Utils.send(player, stripColor(delivery1.getSurvivalItems().toString());
//			});

//		contents.set(new SlotPos(1, 6), ClickableItem.from(skyblock, e -> {
//			delivery.add(WorldGroup.SKYBLOCK, items);
//			service.delete(delivery);
//			service.save(delivery);
//
//			player.closeInventory();
//			Utils.send(player, DeliveryCommand.PREFIX + "Your items have been delivered to &eSkyblock");
//		}));
	}

	@Override
	public void update(Player player, InventoryContents contents) {
	}
}
