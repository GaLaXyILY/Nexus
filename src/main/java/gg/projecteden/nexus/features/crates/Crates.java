package gg.projecteden.nexus.features.crates;

import gg.projecteden.nexus.features.commands.staff.admin.CustomBoundingBoxCommand.CustomBoundingBoxEntityInteractEvent;
import gg.projecteden.nexus.features.commands.staff.admin.RebootCommand;
import gg.projecteden.nexus.features.crates.menus.CratePreviewProvider;
import gg.projecteden.nexus.features.menus.MenuUtils.ConfirmationMenu;
import gg.projecteden.nexus.framework.exceptions.NexusException;
import gg.projecteden.nexus.framework.exceptions.postconfigured.CrateOpeningException;
import gg.projecteden.nexus.framework.features.Feature;
import gg.projecteden.nexus.models.crate.CrateConfig.CrateLoot;
import gg.projecteden.nexus.models.crate.CrateConfigService;
import gg.projecteden.nexus.models.crate.CrateType;
import gg.projecteden.nexus.utils.PlayerUtils;
import gg.projecteden.nexus.utils.StringUtils;
import lombok.NoArgsConstructor;
import org.bukkit.entity.ArmorStand;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;

import java.util.List;
import java.util.stream.Collectors;

/*
	TODO
		Better preview menu
			Grouping loots
		Animations:
			Wakka
			Wither
			Mystery
				Models ?
 */

@NoArgsConstructor
public class Crates extends Feature implements Listener {

	public static final String PREFIX = StringUtils.getPrefix("Crates");

	public static List<CrateLoot> getLootByType(CrateType type) {
		if (type == null) return CrateConfigService.get().getLoot();
		return CrateConfigService.get().getLoot().stream().filter(loot -> loot.getType() == type).collect(Collectors.toList());
	}

	@EventHandler
	public void onClickWithKey(CustomBoundingBoxEntityInteractEvent event) {
		try {
			if (!(event.getEntity().getEntity() instanceof ArmorStand armorStand))
				return;

			CrateType crateType = CrateType.fromEntity(armorStand);
			if (crateType == null)
				return;
			event.setCancelled(true);

			if (!event.getHand().equals(EquipmentSlot.HAND)) return;

			if (!CrateConfigService.get().isEnabled())
				throw new CrateOpeningException("Crates are temporarily disabled");
			if (RebootCommand.isQueued())
				throw new CrateOpeningException("Server reboot is queued, cannot open crates");

			if (!crateType.isEnabled())
				throw new CrateOpeningException("&3Currently disabled");

			ItemStack item = event.getPlayer().getInventory().getItemInMainHand();
			CrateType keyType = CrateType.fromKey(item);
			if (crateType != keyType) {
				if (Crates.getLootByType(crateType).stream().noneMatch(CrateLoot::isActive))
					throw new CrateOpeningException("&3Coming soon...");
				else
					new CratePreviewProvider(crateType, null).open(event.getPlayer());
			}
			else {
				try {
					int amount = item.getAmount();
					if (amount > 1 && event.getPlayer().isSneaking())
						ConfirmationMenu.builder()
							.title("Open " + amount + " keys?")
							.onConfirm(e -> CrateHandler.openCrate(keyType, armorStand, event.getPlayer(), amount))
							.open(event.getPlayer());
					else
						CrateHandler.openCrate(keyType, armorStand, event.getPlayer(), 1);
				} catch (CrateOpeningException ex) {
					if (ex.getMessage() != null)
						PlayerUtils.send(event.getPlayer(), Crates.PREFIX + ex.getMessage());
					CrateHandler.reset(armorStand);
				}
			}
		} catch (NexusException ex) {
			PlayerUtils.send(event.getPlayer(), ex.withPrefix(Crates.PREFIX));
		}
	}

}
