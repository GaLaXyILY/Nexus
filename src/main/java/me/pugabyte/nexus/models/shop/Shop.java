package me.pugabyte.nexus.models.shop;

import com.mongodb.DBObject;
import dev.morphia.annotations.Converters;
import dev.morphia.annotations.Embedded;
import dev.morphia.annotations.Entity;
import dev.morphia.annotations.Id;
import dev.morphia.annotations.PostLoad;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import me.pugabyte.nexus.Nexus;
import me.pugabyte.nexus.features.shops.ShopUtils;
import me.pugabyte.nexus.framework.exceptions.postconfigured.InvalidInputException;
import me.pugabyte.nexus.framework.persistence.serializer.mongodb.ItemStackConverter;
import me.pugabyte.nexus.framework.persistence.serializer.mongodb.UUIDConverter;
import me.pugabyte.nexus.models.PlayerOwnedObject;
import me.pugabyte.nexus.models.banker.BankerService;
import me.pugabyte.nexus.models.banker.Transaction;
import me.pugabyte.nexus.models.banker.Transaction.TransactionCause;
import me.pugabyte.nexus.utils.EnumUtils.IteratableEnum;
import me.pugabyte.nexus.utils.ItemBuilder;
import me.pugabyte.nexus.utils.ItemUtils;
import me.pugabyte.nexus.utils.PlayerUtils;
import me.pugabyte.nexus.utils.SerializationUtils.JSON;
import me.pugabyte.nexus.utils.StringUtils;
import me.pugabyte.nexus.utils.WorldGroup;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Beehive;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.BlockStateMeta;
import org.bukkit.inventory.meta.Damageable;
import org.bukkit.inventory.meta.Repairable;
import org.jetbrains.annotations.NotNull;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

import static com.google.common.base.Strings.isNullOrEmpty;
import static me.pugabyte.nexus.features.shops.ShopUtils.giveItems;
import static me.pugabyte.nexus.features.shops.ShopUtils.prettyMoney;
import static me.pugabyte.nexus.features.shops.Shops.PREFIX;
import static me.pugabyte.nexus.utils.ItemUtils.getShulkerContents;
import static me.pugabyte.nexus.utils.StringUtils.pretty;
import static me.pugabyte.nexus.utils.StringUtils.stripColor;

@Data
@Builder
@Entity("shop")
@NoArgsConstructor
@AllArgsConstructor
@RequiredArgsConstructor
@Converters({UUIDConverter.class, ItemStackConverter.class})
public class Shop extends PlayerOwnedObject {
	@Id
	@NonNull
	private UUID uuid;
	private List<String> description = new ArrayList<>();
	@Embedded
	private List<Product> products = new ArrayList<>();
	@Embedded
	private List<ItemStack> holding = new ArrayList<>();
	// TODO holding for money, maybe? would make withdrawing money more complicated
	// private double profit;

	public List<Product> getProducts(ShopGroup shopGroup) {
		return products.stream().filter(product -> product.getShopGroup().equals(shopGroup)).collect(Collectors.toList());
	}

	public List<String> getDescription() {
		return description.stream().filter(line -> !isNullOrEmpty(line)).collect(Collectors.toList());
	}

	public void setDescription(List<String> description) {
		this.description = new ArrayList<String>() {{
			for (String line : description)
				if (!isNullOrEmpty(stripColor(line).replace(StringUtils.getColorChar(), "")))
					add(line.startsWith("&") ? line : "&f" + line);
		}};
	}

	public boolean isMarket() {
		return uuid.equals(Nexus.getUUID0());
	}

	public String[] getDescriptionArray() {
		return description.isEmpty() ? new String[]{"", "", "", ""} : description.stream().map(StringUtils::decolorize).toArray(String[]::new);
	}

	public List<Product> getInStock(ShopGroup shopGroup) {
		return getProducts(shopGroup).stream().filter(product -> product.isEnabled() && product.isPurchasable() && product.canFulfillPurchase()).collect(Collectors.toList());
	}

	public List<Product> getOutOfStock(ShopGroup shopGroup) {
		return getProducts(shopGroup).stream().filter(product -> product.isEnabled() && product.isPurchasable() && !product.canFulfillPurchase()).collect(Collectors.toList());
	}

	public void addHolding(List<ItemStack> itemStacks) {
		if (isMarket())
			return;

		itemStacks.forEach(this::addHolding);
	}

	public void addHolding(ItemStack itemStack) {
		if (isMarket())
			return;

		ItemUtils.combine(holding, itemStack.clone());
	}

	public void removeProduct(Product product) {
		products.remove(product);
		ShopUtils.giveItems(getOfflinePlayer(), product.getItemStacks());
	}

	public enum ShopGroup {
		SURVIVAL,
		RESOURCE,
		SKYBLOCK,
		ONEBLOCK;

		public static ShopGroup get(org.bukkit.entity.Entity entity) {
			return get(entity.getWorld());
		}

		public static ShopGroup get(World world) {
			return get(world.getName());
		}

		public static ShopGroup get(String world) {
			if (WorldGroup.get(world) == WorldGroup.SURVIVAL)
				return SURVIVAL;
			if (WorldGroup.get(world) == WorldGroup.SKYBLOCK)
				return SKYBLOCK;
			if (WorldGroup.get(world) == WorldGroup.ONEBLOCK)
				return ONEBLOCK;

			return null;
		}
	}

	@Data
	@NoArgsConstructor
	@AllArgsConstructor
	@RequiredArgsConstructor
	@Converters({UUIDConverter.class, ItemStackConverter.class})
	public static class Product implements Comparable {
		@NonNull
		private UUID uuid;
		@NonNull
		private ShopGroup shopGroup;
		private boolean resourceWorld;
		private boolean purchasable = true;
		private boolean enabled = true;
		@Embedded
		private ItemStack item;
		private double stock;
		private ExchangeType exchangeType;
		private Object price;

		private transient boolean editing;

		public Product(@NonNull UUID uuid, @NonNull ShopGroup shopGroup, ItemStack item, double stock, ExchangeType exchangeType, Object price) {
			this(uuid, shopGroup, false, item, stock, exchangeType, price);
		}

		public Product(@NonNull UUID uuid, @NonNull ShopGroup shopGroup, boolean isResourceWorld, ItemStack item, double stock, ExchangeType exchangeType, Object price) {
			this.uuid = uuid;
			this.shopGroup = shopGroup;
			this.resourceWorld = isResourceWorld;
			this.item = item;
			this.stock = stock;
			this.exchangeType = exchangeType;
			this.price = price;
		}

		@PostLoad
		void fix(DBObject dbObject) {
			if (!(price instanceof Number))
				price = JSON.deserializeItemStack((Map<String, Object>) dbObject.get("price"));
		}

		public Shop getShop() {
			return new ShopService().get(uuid);
		}

		public ItemStack getItem() {
			return item.clone();
		}

		public void addStock(int amount) {
			setStock(stock + amount);
		}

		public void removeStock(int amount) {
			setStock(stock - amount);
		}

		public void setStock(double stock) {
			if (isMarket())
				return;

			if (exchangeType == ExchangeType.BUY && stock < 0)
				this.stock = -1;
			else
				this.stock = Math.max(stock, 0);
		}

		public double getCalculatedStock() {
			if (exchangeType == ExchangeType.BUY && stock == -1)
				return new BankerService().getBalance(getShop().getOfflinePlayer(), shopGroup);
			else
				return stock;
		}

		public boolean canFulfillPurchase() {
			if (isMarket())
				return true;
			return getExchange().canFulfillPurchase();
		}

		@SneakyThrows
		public void process(Player customer) {
			if (uuid.equals(customer.getUniqueId()))
				throw new InvalidInputException("You cannot buy items from yourself");

			if (editing)
				throw new InvalidInputException("You cannot buy this item right now, it is being edited by the shop owner");

			getExchange().process(customer);
			log(customer);
		}

		public void log(Player customer) {
			List<String> columns = new ArrayList<>(Arrays.asList(
					DateTimeFormatter.ISO_LOCAL_DATE_TIME.format(LocalDateTime.now()),
					getUuid().toString(),
					getShop().getOfflinePlayer().getName(),
					customer.getUniqueId().toString(),
					customer.getName(),
					getShopGroup().name(),
					item.getType().name(),
					String.valueOf(item.getAmount()),
					exchangeType.name()
			));

			if (price instanceof ItemStack) {
				columns.add(((ItemStack) price).getType().name());
				columns.add(String.valueOf(((ItemStack) price).getAmount()));
			} else {
				columns.add(String.valueOf(price));
				columns.add("");
			}

			Nexus.csvLog("exchange", String.join(",", columns));
		}

		@NotNull
		public Exchange getExchange() {
			return exchangeType.init(this);
		}

		public ItemBuilder getItemWithLore() {
			ItemBuilder builder = new ItemBuilder(item).lore("&f");

			if (item.getType() != Material.ENCHANTED_BOOK)
				builder.itemFlags(ItemFlag.HIDE_ATTRIBUTES);

			short maxDurability = item.getType().getMaxDurability();
			if (item.getType() == Material.ENCHANTED_BOOK || maxDurability > 0)
				builder.lore("&7Repair Cost: " + ((Repairable) item.getItemMeta()).getRepairCost());

			if (item.getItemMeta() instanceof Damageable) {
				Damageable meta = (Damageable) item.getItemMeta();
				if (meta.hasDamage())
					builder.lore("&7Durability: " + (maxDurability - meta.getDamage()) + " / " + maxDurability);
			}

			if (item.getItemMeta() instanceof BlockStateMeta) {
				BlockStateMeta meta = (BlockStateMeta) item.getItemMeta();
				if (meta.getBlockState() instanceof Beehive) {
					Beehive beehive = (Beehive) meta.getBlockState();
					builder.lore("&7Bees: " + beehive.getEntityCount() + " / " + beehive.getMaxEntities());
				}
			}

			if (!getShulkerContents(item).isEmpty())
				builder.lore("&7Right click to view contents");

			if (item.getLore() != null) {
				if (builder.getLore().size() > (item.getLore().size() + 1))
					builder.lore("&f");
			} else if (builder.getLore().size() > 1)
				builder.lore("&f");

			return builder;
		}

		public ItemBuilder getItemWithCustomerLore() {
			if (!purchasable)
				return new ItemBuilder(item).lore("&f").lore("&cNot Purchasable");

			return getItemWithLore().lore(getExchange().getLore());
		}

		public ItemBuilder getItemWithOwnLore() {
			ItemBuilder builder;
			if (!purchasable)
				builder = new ItemBuilder(item).lore("&f").lore("&cNot Purchasable");
			else
				builder = getItemWithLore();

			if (!enabled)
				builder.lore("&cDisabled");

			if (purchasable)
				builder.lore(getExchange().getOwnLore());

			return builder.lore("", "&7Click to edit");
		}

		public boolean isMarket() {
			return getShop().isMarket();
		}

		public List<ItemStack> getItemStacks() {
			return getItemStacks(-1);
		}

		public List<ItemStack> getItemStacks(int maxStacks) {
			List<ItemStack> items = new ArrayList<>();

			if (exchangeType == ExchangeType.BUY)
				return items;

			ItemStack item = this.item.clone();
			double stock = this.stock;
			int maxStackSize = item.getMaxStackSize();

			while (stock > 0) {
				if (maxStacks > 0 && items.size() > maxStacks)
					break;

				ItemStack next = new ItemStack(item.clone());
				next.setAmount((int) Math.min(maxStackSize, stock));
				stock -= next.getAmount();
				items.add(next);
			}

			return items;
		}

		@Override
		public int compareTo(@NotNull Object o) {
			Product product = (Product) o;
			if (item.getType().name().equals(product.getItem().getType().name())) {
				if (price instanceof Number && product.getPrice() instanceof Number)
					return ((Double) price).compareTo((Double) product.getPrice());
				else if (price instanceof Number)
					return ((Double) price).compareTo(Double.MAX_VALUE);
				else if (product.getPrice() instanceof Number)
					return ((Double) Double.MAX_VALUE).compareTo(((Double) product.getPrice()));
				else
					return 0;
			} else
				return item.getType().name().compareTo(product.getItem().getType().name());
		}
	}

	// Dumb enum due to morphia refusing to deserialize interfaces properly
	public enum ExchangeType implements IteratableEnum {
		SELL(SellExchange.class),
		TRADE(TradeExchange.class),
		BUY(BuyExchange.class);

		@Getter
		private final Class<? extends Exchange> clazz;

		ExchangeType(Class<? extends Exchange> clazz) {
			this.clazz = clazz;
		}

		@SneakyThrows
		public Exchange init(Product product) {
			return (Exchange) clazz.getDeclaredConstructors()[0].newInstance(product);
		}
	}

	public interface Exchange {

		Product getProduct();

		void process(Player customer);
		boolean canFulfillPurchase();

		List<String> getLore();
		List<String> getOwnLore();

		default void checkStock() {
			if (!getProduct().isMarket()) {
				if (getProduct().getCalculatedStock() <= 0)
					throw new InvalidInputException("This item is out of stock");
				if (!canFulfillPurchase())
					throw new InvalidInputException("There is not enough stock to fulfill your purchase");
			}
		}
	}

	@Data
	// Customer buying an item from the shop owner for money
	public static class SellExchange implements Exchange {
		@NonNull
		private final Product product;
		private final double price;

		public SellExchange(@NonNull Product product) {
			this.product = product;
			this.price = (double) product.getPrice();
		}

		@Override
		public void process(Player customer) {
			checkStock();

			if (!new BankerService().has(customer, price, product.getShopGroup()))
				throw new InvalidInputException("You do not have enough money to purchase this item");

			product.setStock(product.getStock() - product.getItem().getAmount());
			transaction(customer);
			giveItems(customer, product.getItem());
			new ShopService().save(product.getShop());
			PlayerUtils.send(customer, PREFIX + "You purchased " + pretty(product.getItem()) + " for " + prettyMoney(price));
		}

		private void transaction(Player customer) {
			if (price <= 0)
				return;

			TransactionCause cause = product.isMarket() ? TransactionCause.MARKET_SALE : TransactionCause.SHOP_SALE;
			Transaction transaction = cause.of(customer, product.getShop().getOfflinePlayer(), BigDecimal.valueOf(price), product.getShopGroup(), pretty(product.getItem()));
			new BankerService().transfer(customer, product.getShop().getOfflinePlayer(), BigDecimal.valueOf(price), product.getShopGroup(), transaction);
		}

		public boolean canFulfillPurchase() {
			return product.getCalculatedStock() >= product.getItem().getAmount();
		}

		@Override
		public List<String> getLore() {
			int stock = (int) product.getStock();
			String desc = "&7Buy &e" + product.getItem().getAmount() + " &7for &a" + prettyMoney(price);

			if (product.getUuid().equals(Nexus.getUUID0()))
				return Arrays.asList(
						desc,
						"&7Owner: &6Market"
				);
			else
				return Arrays.asList(
						desc,
						"&7Stock: " + (stock > 0 ? "&e" : "&c") + stock,
						"&7Owner: &e" + product.getShop().getOfflinePlayer().getName()
				);
		}

		@Override
		public List<String> getOwnLore() {
			int stock = (int) product.getStock();
			return Arrays.asList(
					"&7Selling &e" + product.getItem().getAmount() + " &7for &a" + prettyMoney(price),
					"&7Stock: " + (stock > 0 ? "&e" : "&c") + stock
			);
		}
	}

	@Data
	// Customer buying an item from the shop owner for other items
	public static class TradeExchange implements Exchange {
		@NonNull
		private Product product;
		private final ItemStack price;

		public TradeExchange(@NonNull Product product) {
			this.product = product;
			this.price = (ItemStack) product.getPrice();
		}

		@Override
		public void process(Player customer) {
			checkStock();

			if (!customer.getInventory().containsAtLeast(price, price.getAmount()))
				throw new InvalidInputException("You do not have " + pretty(price) + " to purchase this item");

			product.setStock(product.getStock() - product.getItem().getAmount());
			customer.getInventory().removeItem(price);
			product.getShop().addHolding(price);
			giveItems(customer, product.getItem());
			new ShopService().save(product.getShop());
			PlayerUtils.send(customer, PREFIX + "You purchased " + pretty(product.getItem()) + " for " + pretty(price));
		}

		@Override
		public boolean canFulfillPurchase() {
			return product.getCalculatedStock() >= product.getItem().getAmount();
		}

		@Override
		public List<String> getLore() {
			int stock = (int) product.getStock();
			String desc = "&7Buy &e" + product.getItem().getAmount() + " &7for &a" + pretty(price);
			if (product.getUuid().equals(Nexus.getUUID0()))
				return Arrays.asList(
						desc,
						"&7Owner: &6Market"
				);
			else
				return Arrays.asList(
						desc,
						"&7Stock: " + (stock > 0 ? "&e" : "&c") + stock,
						"&7Owner: &e" + product.getShop().getOfflinePlayer().getName()
				);
		}

		@Override
		public List<String> getOwnLore() {
			int stock = (int) product.getStock();
			return Arrays.asList(
					"&7Selling &e" + product.getItem().getAmount() + " &7for &a" + pretty(price),
					"&7Stock: " + (stock > 0 ? "&e" : "&c") + stock
			);
		}
	}

	@Data
	// Customer selling an item to the shop owner for money
	public static class BuyExchange implements Exchange {
		@NonNull
		private final Product product;
		private final Double price;

		public BuyExchange(@NonNull Product product) {
			this.product = product;
			this.price = (Double) product.getPrice();
		}

		@Override
		public void process(Player customer) {
			checkStock();

			if (!customer.getInventory().containsAtLeast(product.getItem(), product.getItem().getAmount()))
				throw new InvalidInputException("You do not have " + pretty(product.getItem()) + " to sell");

			product.setStock(product.getStock() - price);
			transaction(customer);
			customer.getInventory().removeItem(product.getItem());
			product.getShop().addHolding(product.getItem());
			new ShopService().save(product.getShop());
			PlayerUtils.send(customer, PREFIX + "You sold " + pretty(product.getItem()) + " for " + prettyMoney(price));
		}

		private void transaction(Player customer) {
			if (price <= 0)
				return;
			TransactionCause cause = product.isMarket() ? TransactionCause.MARKET_PURCHASE : TransactionCause.SHOP_PURCHASE;
			Transaction transaction = cause.of(product.getShop().getOfflinePlayer(), customer, BigDecimal.valueOf(price), product.getShopGroup(), pretty(product.getItem()));
			new BankerService().transfer(product.getShop().getOfflinePlayer(), customer, BigDecimal.valueOf(price), product.getShopGroup(), transaction);
		}

		@Override
		public boolean canFulfillPurchase() {
			return product.getCalculatedStock() >= price;
		}

		@Override
		public List<String> getLore() {
			String desc = "&7Sell &e" + product.getItem().getAmount() + " &7for &a" + prettyMoney(price);
			if (product.getUuid().equals(Nexus.getUUID0()))
				return Arrays.asList(
						desc,
						"&7Owner: &6Market"
				);
			else
				return Arrays.asList(
						desc,
						"&7Stock: &e" + prettyMoney(product.getCalculatedStock(), false),
						"&7Owner: &e" + product.getShop().getOfflinePlayer().getName()
				);
		}

		@Override
		public List<String> getOwnLore() {
			return Arrays.asList(
					"&7Buying &e" + product.getItem().getAmount() + " &7for &a" + prettyMoney(price),
					"&7Stock: &e" + prettyMoney(product.getCalculatedStock(), false)
			);
		}

	}

}
