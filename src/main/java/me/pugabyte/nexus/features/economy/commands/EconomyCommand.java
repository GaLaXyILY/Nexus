package me.pugabyte.nexus.features.economy.commands;

import lombok.NonNull;
import me.pugabyte.nexus.framework.commands.models.CustomCommand;
import me.pugabyte.nexus.framework.commands.models.annotations.Aliases;
import me.pugabyte.nexus.framework.commands.models.annotations.Arg;
import me.pugabyte.nexus.framework.commands.models.annotations.Path;
import me.pugabyte.nexus.framework.commands.models.annotations.Permission;
import me.pugabyte.nexus.framework.commands.models.events.CommandEvent;
import me.pugabyte.nexus.models.banker.Banker;
import me.pugabyte.nexus.models.banker.BankerService;
import me.pugabyte.nexus.models.banker.Transaction.TransactionCause;
import me.pugabyte.nexus.utils.StringUtils;

import java.math.BigDecimal;

import static me.pugabyte.nexus.utils.StringUtils.prettyMoney;

@Aliases("eco")
public class EconomyCommand extends CustomCommand {
	private final BankerService service = new BankerService();

	public EconomyCommand(@NonNull CommandEvent event) {
		super(event);
		PREFIX = StringUtils.getPrefix("Economy");
	}

	@Path("selling")
	void sell() {
		line(3);
		send("&3There are a few ways you can trade with other players:");
		send(json("&3[+] &eShops").url("https://wiki.bnn.gg/wiki/Shops").hover("&3Click to open the wiki section on Shops."));
		send("&3[+] &eSimply ask in chat!");
		line();
		send(json("&3 « &eClick here to return to the economy menu.").command("/economy"));
	}

	@Path("commands")
	void commands() {
		line(3);
		send("&eEconomy Related Commands");
		send(json("&3[+] &c/pay <player> <amount>").hover("&3Give someone some money. \nEx: &c/pay notch 666").suggest("/pay "));
		send(json("&3[+] &c/bal [player]").hover("&3View your balance.\n&3Add a player name to view another player's balance.").suggest("/bal "));
		send(json("&3[+] &c/baltop [#]").hover("&3View the richest people on the server").suggest("/baltop"));
		send(json("&3[+] &c/market").hover("&3Visit the market").suggest("/market"));
		line();
		send(json("&3 « &eClick here to return to the economy menu.").command("/economy"));
	}

	@Path
	@Override
	public void help() {
		line(3);
		send("&3Each player starts out with &e$500&3.");
		send("&3There are multiple ways to make money, such as:");
		line();
		send(json("&3[+] &eSelling items at the &c/market").suggest("/market"));
		send(json("&3[+] &eSelling items at the &c/market &3in the &eresource world").hover("&3Non auto-farmable resources sell for more in this world").suggest("/warp resource"));
		send(json("&3[+] &eSelling items to other players").command("/economy selling").hover("&3Click for a few tips on how to sell to other players"));
		send(json("&3[+] &eKilling mobs").url("https://wiki.bnn.gg/wiki/Main_Page#Mobs").hover("&3Click to open the wiki section on mobs."));
		send("&3[+] &eWorking for other players");
		send(json("&3[+] &eVoting and getting &2&lTop Voter").command("/vote"));
		send(json("&3[+] &eWinning Events").hover("&3Make sure to check Discord's &e#announcements &3channel and the home page for upcoming events!"));
		line();
		send(json("&3[+] &eEconomy related commands").command("/economy commands"));
	}

	@Path("set <player> <balance> [reason]")
	@Permission("group.admin")
	void set(Banker banker, BigDecimal balance, @Arg("server") TransactionCause cause) {
		service.setBalance(banker.getOfflinePlayer(), balance, cause);
		send(PREFIX + "Set &e" + banker.getName() + "'s &3balance to &e" + banker.getBalanceFormatted());
	}

	@Path("give <player> <balance> [reason]")
	@Permission("group.admin")
	void give(Banker banker, BigDecimal balance, @Arg("server") TransactionCause cause) {
		service.deposit(banker.getOfflinePlayer(), balance, cause);
		send(PREFIX + "Added &e" + prettyMoney(balance) + " &3to &e" + banker.getName() + "'s &3balance. New balance: &e" + banker.getBalanceFormatted());
	}

	@Path("take <player> <balance> [reason]")
	@Permission("group.admin")
	void take(Banker banker, BigDecimal balance, @Arg("server") TransactionCause cause) {
		service.withdraw(banker.getOfflinePlayer(), balance, cause);
		send(PREFIX + "Removed &e" + prettyMoney(balance) + " &3from &e" + banker.getName() + "'s &3balance. New balance: &e" + banker.getBalanceFormatted());
	}

//	@Async
//	@Path("convertBalances")
//	void convertBalance() {
//		int wait = 0;
//		Essentials essentials = (Essentials) Bukkit.getPluginManager().getPlugin("Essentials");
//		for (UUID uuid : essentials.getUserMap().getAllUniqueUsers()) {
//			int count = 0;
//			User user = essentials.getUser(uuid);
//			Banker banker = service.get(uuid);
//			if (user.getMoney().doubleValue() > 550 || user.getMoney().doubleValue() < 450) {
//				++count;
//				Tasks.wait(wait, () -> {
//					banker.setBalance(user.getMoney());
//					service.save(banker);
//				});
//				if (count > 200)
//					wait += 3;
//			}
//		}
//	}

}
