package me.pugabyte.nexus.features.quests.itemtags;

import me.pugabyte.nexus.framework.commands.models.CustomCommand;
import me.pugabyte.nexus.framework.commands.models.annotations.Aliases;
import me.pugabyte.nexus.framework.commands.models.annotations.Description;
import me.pugabyte.nexus.framework.commands.models.annotations.Path;
import me.pugabyte.nexus.framework.commands.models.annotations.Permission;
import me.pugabyte.nexus.framework.commands.models.events.CommandEvent;
import me.pugabyte.nexus.utils.ItemUtils;
import me.pugabyte.nexus.utils.RandomUtils;
import org.bukkit.inventory.ItemStack;

import static me.pugabyte.nexus.features.quests.itemtags.ItemTagsUtils.addCondition;
import static me.pugabyte.nexus.features.quests.itemtags.ItemTagsUtils.addRarity;
import static me.pugabyte.nexus.features.quests.itemtags.ItemTagsUtils.finalizeItem;
import static me.pugabyte.nexus.features.quests.itemtags.ItemTagsUtils.updateItem;

@Aliases({"itemtags"})
@Permission("group.admin")
public class ItemTagCommand extends CustomCommand {

	public ItemTagCommand(CommandEvent event) {
		super(event);
	}

	@Path("get")
	@Description("Get item tags on held item")
	void getTags() {
		ItemStack tool = getToolRequired();

		send("");
		send("Item Tags: ");
		Condition condition = Condition.of(tool);
		if (condition != null)
			send(condition.getTag());

		Rarity rarity = Rarity.of(tool);
		if (rarity != null)
			send(rarity.getTag());
		send("");
	}

	@Path("update")
	@Description("Update item tags on held item")
	void update() {
		ItemStack tool = getToolRequired();

		ItemStack updated = updateItem(tool);
		int heldSlot = player().getInventory().getHeldItemSlot();
		player().getInventory().setItem(heldSlot, updated);
	}

	@Path("updateInv")
	@Description("Update item tags on all items in inventory")
	void updateInv() {
		ItemStack[] contents = player().getInventory().getContents();
		int count = 0;
		for (ItemStack item : contents) {
			if (ItemUtils.isNullOrAir(item))
				continue;

			ItemStack updated = updateItem(item);
			item.setItemMeta(updated.getItemMeta());
			++count;
		}

		send(PREFIX + count + " items itemtags updated!");
	}

	@Path("setRarity <rarity>")
	void setRarity(Rarity rarity) {
		ItemStack tool = getToolRequired();

		ItemStack updated = finalizeItem(addRarity(tool, rarity, true));
		int heldSlot = player().getInventory().getHeldItemSlot();
		player().getInventory().setItem(heldSlot, updated);
	}

	@Path("setCondition <condition>")
	void setCondition(Condition condition) {
		ItemStack tool = getToolRequired();

		ItemStack updated = finalizeItem(addCondition(tool, condition, true));
		ItemUtils.setDurability(updated, RandomUtils.randomInt(condition.getMin(), condition.getMax()));

		int heldSlot = player().getInventory().getHeldItemSlot();
		player().getInventory().setItem(heldSlot, updated);
	}

	@Path("reload")
	@Permission("group.staff")
	void reload() {
		ItemTags.reloadConfig();
	}
}
