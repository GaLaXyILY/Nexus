package me.pugabyte.nexus.features.economy.commands;

import lombok.NonNull;
import me.pugabyte.nexus.framework.commands.models.CustomCommand;
import me.pugabyte.nexus.framework.commands.models.annotations.Aliases;
import me.pugabyte.nexus.framework.commands.models.annotations.Arg;
import me.pugabyte.nexus.framework.commands.models.annotations.Async;
import me.pugabyte.nexus.framework.commands.models.annotations.Path;
import me.pugabyte.nexus.framework.commands.models.events.CommandEvent;
import me.pugabyte.nexus.models.banker.Banker;
import me.pugabyte.nexus.models.banker.BankerService;
import me.pugabyte.nexus.models.nerd.Nerd;
import me.pugabyte.nexus.models.shop.Shop.ShopGroup;
import me.pugabyte.nexus.utils.JsonBuilder;
import me.pugabyte.nexus.utils.StringUtils;

import java.math.BigDecimal;
import java.util.Comparator;
import java.util.List;
import java.util.function.BiFunction;
import java.util.stream.Collectors;

@Aliases({"baltop", "moneytop"})
public class BalanceTopCommand extends CustomCommand {
	private final BankerService service = new BankerService();

	public BalanceTopCommand(@NonNull CommandEvent event) {
		super(event);
		PREFIX = StringUtils.getPrefix("Economy");
	}

	@Async
	@Path("[shopGroup] [page]")
	void baltop(@Arg("current") ShopGroup shopGroup, @Arg(value = "1", min = 1) int page) {
		List<Banker> bankers = service.<Banker>getAll().stream()
				.filter(banker -> !banker.isMarket() && banker.getBalance(shopGroup).compareTo(BigDecimal.valueOf(500)) != 0)
				.sorted(Comparator.comparing(banker -> banker.getBalance(shopGroup), Comparator.reverseOrder()))
				.collect(Collectors.toList());

		if (bankers.isEmpty())
			error("No balances found");

		double sum = bankers.stream().mapToDouble(banker -> banker.getBalance(shopGroup).doubleValue()).sum();

		send(PREFIX + "Top " + camelCase(shopGroup) + " balances  &3|  Total: &e" + StringUtils.prettyMoney(sum));
		BiFunction<Banker, String, JsonBuilder> formatter = (banker, index) ->
				json("&3" + index + " &e" + (banker.getName() == null ? banker.getUuid() : Nerd.of(banker).getNickname()) + " &7- " + banker.getBalanceFormatted(shopGroup));
		paginate(bankers, formatter, "/baltop " + shopGroup.name().toLowerCase(), page);
	}

}
