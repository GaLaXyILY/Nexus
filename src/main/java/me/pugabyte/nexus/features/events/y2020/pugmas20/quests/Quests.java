package me.pugabyte.nexus.features.events.y2020.pugmas20.quests;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.experimental.Accessors;
import me.pugabyte.nexus.features.events.models.Quest;
import me.pugabyte.nexus.features.events.models.QuestStage;
import me.pugabyte.nexus.features.events.y2020.pugmas20.Pugmas20;
import me.pugabyte.nexus.features.events.y2020.pugmas20.models.QuestNPC;
import me.pugabyte.nexus.models.pugmas20.Pugmas20User;
import me.pugabyte.nexus.utils.ItemUtils;
import me.pugabyte.nexus.utils.SoundUtils;
import me.pugabyte.nexus.utils.Utils;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.Listener;
import org.bukkit.inventory.ItemStack;
import org.reflections.Reflections;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.BiConsumer;
import java.util.function.Function;

public class Quests {
	public static final String fullInvError_obtain = Pugmas20.PREFIX + "&cYour inventory is too full to get this!";
	public static final String fullInvError_open = Pugmas20.PREFIX + "&cYour inventory is too full to open this!";
	public static final String leftoverItems = Pugmas20.PREFIX + "Giving leftover items...";

	public Quests() {
		new Reflections(getClass().getPackage().getName()).getSubTypesOf(Listener.class).forEach(Utils::tryRegisterListener);
	}

	@Getter
	@AllArgsConstructor
	@Accessors(fluent = true)
	public enum Pugmas20QuestStageHelper {
		GIFT_GIVER(Pugmas20User::getGiftGiverStage, Pugmas20User::setGiftGiverStage),
		LIGHT_THE_TREE(Pugmas20User::getLightTreeStage, Pugmas20User::setLightTreeStage),
		ORNAMENT_VENDOR(Pugmas20User::getOrnamentVendorStage, Pugmas20User::setOrnamentVendorStage),
		THE_MINES(Pugmas20User::getMinesStage, Pugmas20User::setMinesStage),
		TOY_TESTING(Pugmas20User::getToyTestingStage, Pugmas20User::setToyTestingStage);

		private final Function<Pugmas20User, QuestStage> getter;
		private final BiConsumer<Pugmas20User, QuestStage> setter;
	}

	@Getter
	@AllArgsConstructor
	public enum Pugmas20Quest implements Quest {
		GIFT_GIVER(user -> new HashMap<QuestStage, String>() {{
			put(QuestStage.NOT_STARTED, "Find " + QuestNPC.GIFT_GIVER.getName() + " in the Workshop");
		}}),

		TOY_TESTING(user -> new HashMap<QuestStage, String>() {{
			put(QuestStage.NOT_STARTED, "Find " + QuestNPC.QA_ELF.getName() + " in the Workshop");
			put(QuestStage.STARTED, "Talk to " + QuestNPC.QA_ELF.getName());
			put(QuestStage.STEPS_DONE, "Talk to " + QuestNPC.QA_ELF.getName());
		}}),

		THE_MINES(user -> new HashMap<QuestStage, String>() {{
			put(QuestStage.INELIGIBLE, "Complete Light The Tree");
			put(QuestStage.NOT_STARTED, "Talk to " + QuestNPC.FORELF.getName() + " in the coal mine");
			put(QuestStage.STARTED, "Trade ingots in the sell crate next to " + QuestNPC.FORELF.getName());
		}}),

		ORNAMENT_VENDOR(user -> new HashMap<QuestStage, String>() {{
			put(QuestStage.NOT_STARTED, "Find " + QuestNPC.ELF3.getName() + " in the Harbor District");
			put(QuestStage.STARTED, "Trade logs with the Ornament Vendor and bring each of the 10 ornaments to " + QuestNPC.ELF3.getName());
		}}),

		LIGHT_THE_TREE(user -> new HashMap<QuestStage, String>() {{
			put(QuestStage.NOT_STARTED, "Find " + QuestNPC.ELF2.getName() + " near the Pugmas tree");
			put(QuestStage.STARTED, "Find " + QuestNPC.ELF1.getName() + " in the workshop");
			put(QuestStage.STEP_ONE, "Help " + QuestNPC.ELF1.getName() + " find the Ceremonial Lighter in the basement");
			put(QuestStage.STEP_TWO, "Talk to " + QuestNPC.ELF2.getName() + " near the Pugmas tree to fix the Ceremonial Lighter");
			put(QuestStage.STEP_THREE, "Find " + QuestNPC.FORELF.getName() + " in the coal mine to get the necessary materials");
			put(QuestStage.STEPS_DONE, "Talk to " + QuestNPC.ELF1.getName() + " in the workshop");
		}});

		private final Function<Pugmas20User, Map<QuestStage, String>> instructions;
	}

	public static void sound_obtainItem(Player player) {
		SoundUtils.playSound(player, Sound.ENTITY_PLAYER_LEVELUP, 0.5F, 2F);
	}

	public static void sound_villagerNo(Player player) {
		SoundUtils.playSound(player, Sound.ENTITY_VILLAGER_NO, 0.5F, 1F);
	}

	public static void sound_npcAlert(Player player) {
		SoundUtils.playSound(player, Sound.BLOCK_NOTE_BLOCK_BIT, 0.5F, 1F);
	}

	public static boolean hasRoomFor(Player player, ItemStack... items) {
		List<ItemStack> itemList = new ArrayList<>();
		for (ItemStack item : new ArrayList<>(Arrays.asList(items))) {
			if (!ItemUtils.isNullOrAir(item))
				itemList.add(item);
		}

		return hasRoomFor(player, itemList.size());
	}

	public static boolean hasRoomFor(Player player, int slots) {
		ItemStack[] contents = player.getInventory().getContents();
		int slotsUsed = 0;
		for (ItemStack content : contents) {
			if (!ItemUtils.isNullOrAir(content))
				slotsUsed++;
		}

		return (slotsUsed <= (36 - slots));
	}

}
