package me.pugabyte.nexus.features.commands.staff;

import de.tr7zw.nbtapi.NBTItem;
import me.pugabyte.nexus.framework.commands.models.CustomCommand;
import me.pugabyte.nexus.framework.commands.models.annotations.Aliases;
import me.pugabyte.nexus.framework.commands.models.annotations.Arg;
import me.pugabyte.nexus.framework.commands.models.annotations.Path;
import me.pugabyte.nexus.framework.commands.models.annotations.Permission;
import me.pugabyte.nexus.framework.commands.models.events.CommandEvent;
import me.pugabyte.nexus.utils.MaterialTag;
import me.pugabyte.nexus.utils.SerializationUtils.JSON;
import me.pugabyte.nexus.utils.StringUtils;
import me.pugabyte.nexus.utils.Tasks;
import org.bukkit.Material;
import org.bukkit.block.data.BlockData;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.Nullable;

import static me.pugabyte.nexus.utils.ItemUtils.isNullOrAir;
import static me.pugabyte.nexus.utils.SerializationUtils.JSON.serializeItemStack;
import static me.pugabyte.nexus.utils.StringUtils.colorize;
import static me.pugabyte.nexus.utils.StringUtils.paste;
import static me.pugabyte.nexus.utils.StringUtils.stripColor;
import static me.pugabyte.nexus.utils.Utils.dump;

@Aliases({"nbt", "itemdb"})
@Permission("group.staff")
public class ItemInfoCommand extends CustomCommand {

	public ItemInfoCommand(CommandEvent event) {
		super(event);
	}

	@Path("[material]")
	void itemInfo(Material material) {
		ItemStack tool = material == null ? getToolRequired() : new ItemStack(material);

		sendJson(tool);
	}

	@Path("extended [material]")
	void extended(Material material) {
		ItemStack tool = material == null ? getToolRequired() : new ItemStack(material);
		material = tool.getType();

		line(5);
		sendJson(tool);
		line();
		send("Namespaced key: " + material.getKey());
		send("Blast resistance: " + material.getBlastResistance());
		send("Hardness: " + material.getHardness());
		send("Max durability: " + material.getMaxDurability());
		send("Max stack size: " + material.getMaxStackSize());
		line();
		send("Has gravity: " + StringUtils.bool(material.hasGravity()));
		send("Is air: " + StringUtils.bool(material.isAir()));
		send("Is block: " + StringUtils.bool(material.isBlock()));
		send("Is burnable: " + StringUtils.bool(material.isBurnable()));
		send("Is edible: " + StringUtils.bool(material.isEdible()));
		send("Is empty: " + StringUtils.bool(material.isEmpty()));
		send("Is flammable: " + StringUtils.bool(material.isFlammable()));
		send("Is fuel: " + StringUtils.bool(material.isFuel()));
		send("Is interactable: " + StringUtils.bool(material.isInteractable()));
		send("Is item: " + StringUtils.bool(material.isItem()));
		send("Is occluding: " + StringUtils.bool(material.isOccluding()));
		send("Is record: " + StringUtils.bool(material.isRecord()));
		send("Is solid: " + StringUtils.bool(material.isSolid()));
		send("Is transparent: " + StringUtils.bool(material.isTransparent()));
		line();
		send("Applicable tags: " + String.join(", ", MaterialTag.getApplicable(material).keySet()));
		line();
		BlockData blockData = material.createBlockData();
		send("BlockData: " + material.data.getSimpleName());
		dump(blockData).forEach((method, output) -> send(method + "(): " + output));
		line();
	}

	private void sendJson(ItemStack tool) {
		line();
		send("Material: " + tool.getType() + " (" + tool.getType().ordinal() + ")");

		if (!isNullOrAir(tool)) {
			final String nbtString = getNBTString(tool);

			if (nbtString != null && !"{}".equals(nbtString)) {
				int length = nbtString.length();
				if (length > 256) {
					Tasks.async(() -> {
						if (length < 32000) // max char limit in command blocks
							send("NBT: " + colorize(nbtString));
						String url = paste(stripColor(nbtString));
						send(json("&e&l[Click to Open NBT]").url(url).hover(url));
					});
				} else {
					send("NBT: " + colorize(nbtString));
					send(json("&e&l[Click to Copy NBT]").hover("&e&l[Click to Copy NBT]").copy(nbtString));
				}
			}
		}
	}

	@Path("serialize json [material] [amount]")
	void serializeJson(Material material, @Arg("1") int amount) {
		ItemStack tool = material == null ? getToolRequired() : new ItemStack(material);

		send(json("&e&l[Click to Copy NBT]").hover("&e&l[Click to Copy NBT]").copy(JSON.toString(serializeItemStack(tool))));
	}

	@Nullable
	private String getNBTString(ItemStack itemStack) {
		NBTItem nbtItem = new NBTItem(itemStack);
		String nbtString = null;

		if (nbtItem.hasNBTData()) {
			nbtString = nbtItem.asNBTString();
			nbtString = StringUtils.stripColor(nbtString);
		}

		if (nbtString != null) {
			// highlight keywords
			nbtString = nbtString.replaceAll("run_command", "&crun_command&f");
			nbtString = nbtString.replaceAll("suggest_command", "&csuggest_command&f");
			nbtString = nbtString.replaceAll("insert_command", "&cinsert_command&f");
			nbtString = nbtString.replaceAll("open_url", "&copen_url&f");
			nbtString = nbtString.replaceAll("open_file", "&copen_file&f");

			nbtString = nbtString.replaceAll("clickEvent", "&cclickEvent&f");
			nbtString = nbtString.replaceAll("hoverEvent", "&choverEvent&f");

			// clean up of garbage
			nbtString = nbtString.replaceAll("\"\"", "");
			nbtString = nbtString.replaceAll("\\{\"\"text\"\":\"\"\\n\"\"},", "");
			nbtString = nbtString.replaceAll("\\n", "");
			nbtString = nbtString.replaceAll("\\\\", "");
		}
		return nbtString;
	}

	@Path("notItems")
	void notItems() {
		for (Material material : Material.values()) {
			if (!material.isLegacy() && !material.isItem())
				send(material.name());
		}
	}

}
