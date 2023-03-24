package gg.projecteden.nexus.features.commands.staff.admin;

import gg.projecteden.nexus.framework.commands.models.CustomCommand;
import gg.projecteden.nexus.framework.commands.models.annotations.Description;
import gg.projecteden.nexus.framework.commands.models.annotations.Path;
import gg.projecteden.nexus.framework.commands.models.annotations.Permission;
import gg.projecteden.nexus.framework.commands.models.annotations.Permission.Group;
import gg.projecteden.nexus.framework.commands.models.events.CommandEvent;
import gg.projecteden.nexus.framework.exceptions.postconfigured.InvalidInputException;
import gg.projecteden.nexus.utils.ColorType;
import lombok.NonNull;
import net.md_5.bungee.api.ChatColor;
import org.bukkit.entity.BlockDisplay;
import org.bukkit.entity.Entity;
import org.bukkit.entity.ItemDisplay;
import org.bukkit.entity.TextDisplay;
import org.jetbrains.annotations.NotNull;

@Permission(Group.ADMIN)
public class DisplayEntityCommand extends CustomCommand {

	public DisplayEntityCommand(@NonNull CommandEvent event) {
		super(event);
	}

	@Path("edit item")
	@Description("Update the item on the nearest item display")
	void edit_item() {
		itemDisplay().setItemStack(getToolRequired());
		send(PREFIX + "Item updated");
	}

	@Path("get item")
	@Description("Get the item on the nearest item display")
	void get_item() {
		giveItem(itemDisplay().getItemStack());
	}

	@Path("edit text background <color>")
	@Description("Get the text background color on the nearest text display")
	void edit_text_background(ChatColor color) {
		textDisplay().setBackgroundColor(ColorType.toBukkitColor(color));
	}

	@NotNull
	private ItemDisplay itemDisplay() {
		final Entity nearestEntityRequired = getNearestEntityRequired();
		if (!(nearestEntityRequired instanceof ItemDisplay itemDisplay))
			throw new InvalidInputException("Nearest entity is not an item display");
		return itemDisplay;
	}

	@NotNull
	private TextDisplay textDisplay() {
		final Entity nearestEntityRequired = getNearestEntityRequired();
		if (!(nearestEntityRequired instanceof TextDisplay textDisplay))
			throw new InvalidInputException("Nearest entity is not a text display");
		return textDisplay;
	}

	@NotNull
	private BlockDisplay blockDisplay() {
		final Entity nearestEntityRequired = getNearestEntityRequired();
		if (!(nearestEntityRequired instanceof BlockDisplay blockDisplay))
			throw new InvalidInputException("Nearest entity is not a block display");
		return blockDisplay;
	}

}
