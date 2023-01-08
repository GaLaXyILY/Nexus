package gg.projecteden.nexus.features.resourcepack.decoration.catalog;

import gg.projecteden.nexus.features.menus.api.ClickableItem;
import gg.projecteden.nexus.features.menus.api.ItemClickData;
import gg.projecteden.nexus.features.menus.api.content.InventoryProvider;
import gg.projecteden.nexus.features.resourcepack.decoration.DecorationType;
import gg.projecteden.nexus.features.resourcepack.decoration.DecorationType.CategoryTree;
import gg.projecteden.nexus.features.resourcepack.decoration.catalog.Catalog.Tab;
import gg.projecteden.nexus.features.resourcepack.decoration.catalog.Catalog.Theme;
import gg.projecteden.nexus.utils.ItemBuilder;
import gg.projecteden.nexus.utils.SoundBuilder;
import gg.projecteden.nexus.utils.StringUtils;
import lombok.NonNull;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

public class CatalogProvider extends InventoryProvider {
	InventoryProvider previousMenu;
	Catalog.Theme catalogTheme;
	CategoryTree currentTree;

	public CatalogProvider(@NonNull Catalog.Theme catalogTheme, @NonNull CategoryTree tree, @Nullable InventoryProvider previousMenu) {
		this(catalogTheme, previousMenu);
		this.currentTree = tree;
	}

	public CatalogProvider(@NonNull Catalog.Theme catalogTheme, @Nullable InventoryProvider previousMenu) {
		this.catalogTheme = catalogTheme;
		this.previousMenu = previousMenu;
	}

	@Override
	public String getTitle() {
		String catalogName = StringUtils.camelCase(catalogTheme);
		String tabName = StringUtils.camelCase(currentTree.getTabParent());
		if (currentTree.isRoot())
			return "Catalog | " + catalogName;

		return catalogName + " | " + tabName;
	}

	@Override
	public void onPageTurn(Player viewer) {
		new SoundBuilder(Sound.ITEM_BOOK_PAGE_TURN).location(viewer).play();
	}

	@Override
	public void onClose(InventoryCloseEvent event, List<ItemStack> contents) {
		new SoundBuilder(Sound.ITEM_BOOK_PUT).location(viewer).play();
	}

	@Override
	public void init() {
		addBackOrCloseItem(previousMenu);

		List<ClickableItem> items = new ArrayList<>();

		if (currentTree == null)
			currentTree = DecorationType.getCategoryTree();

		// Add Children Folders

		List<CategoryTree> children = currentTree.getTabChildren();
		for (CategoryTree child : children) {
			if (child.isRoot() || child.isInvisible())
				continue;

			List<DecorationType> decorationTypes = child.getDecorationTypes();
			if (decorationTypes.isEmpty() && child.getTabChildren().isEmpty())
				continue;

			if (child.getTabParent() != Tab.COUNTERS_MENU && getClickableTabItems(child, catalogTheme).isEmpty())
				continue;

			ItemBuilder icon = child.getTabParent().getIcon()
				.name(StringUtils.camelCase(child.getTabParent()))
				.glow();

			Consumer<ItemClickData> consumer = e -> Catalog.openCatalog(viewer, catalogTheme, child, this);
			if (child.getTabParent() == Tab.COUNTERS_MENU)
				consumer = e -> Catalog.openCountersCatalog(viewer, catalogTheme, child, this);

			items.add(ClickableItem.of(icon.build(), consumer));
		}

		// Separation
		if (!items.isEmpty()) {
			while (items.size() % 9 != 0)
				items.add(ClickableItem.NONE);

			for (int i = 0; i < 9; i++)
				items.add(ClickableItem.NONE);
		}

		// Add Items
		items.addAll(getClickableTabItems(currentTree, catalogTheme));

		paginator().items(items).useGUIArrows().build();
	}

	private List<ClickableItem> getClickableTabItems(CategoryTree tree, Theme theme) {
		if (tree.isInvisible())
			return new ArrayList<>();

		List<ClickableItem> clickableItems = new ArrayList<>();
		for (ItemStack itemStack : getDecoration(tree, theme)) {
			clickableItems.add(ClickableItem.of(itemStack, e -> Catalog.spawnItem(viewer, itemStack)));
		}

		return clickableItems;
	}

	private List<ItemStack> getDecoration(CategoryTree tree, Theme theme) {
		if (tree.isInvisible())
			return new ArrayList<>();

		return tree.getDecorationTypes().stream()
			.filter(type -> type.getTheme() == theme)
			.map(type -> type.getConfig().getItem())
			.toList();
	}
}
