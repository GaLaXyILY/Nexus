package me.pugabyte.nexus.features.discord.commands;

import com.jagrosh.jdautilities.command.Command;
import com.jagrosh.jdautilities.command.CommandEvent;
import me.pugabyte.nexus.features.discord.Bot;
import me.pugabyte.nexus.features.discord.Bot.HandledBy;
import me.pugabyte.nexus.framework.exceptions.NexusException;
import me.pugabyte.nexus.framework.exceptions.postconfigured.InvalidInputException;
import me.pugabyte.nexus.framework.exceptions.preconfigured.NegativeBalanceException;
import me.pugabyte.nexus.framework.exceptions.preconfigured.NotEnoughMoneyException;
import me.pugabyte.nexus.models.banker.BankerService;
import me.pugabyte.nexus.models.banker.Transaction.TransactionCause;
import me.pugabyte.nexus.models.discord.DiscordService;
import me.pugabyte.nexus.models.discord.DiscordUser;
import me.pugabyte.nexus.models.shop.Shop.ShopGroup;
import me.pugabyte.nexus.utils.PlayerUtils;
import me.pugabyte.nexus.utils.Tasks;
import me.pugabyte.nexus.utils.Utils;
import org.bukkit.OfflinePlayer;

import java.math.BigDecimal;

import static me.pugabyte.nexus.utils.StringUtils.prettyMoney;
import static me.pugabyte.nexus.utils.StringUtils.stripColor;

@HandledBy(Bot.KODA)
public class PayDiscordCommand extends Command {

	public PayDiscordCommand() {
		this.name = "pay";
	}

	protected void execute(CommandEvent event) {
		Tasks.async(() -> {
			try {
				DiscordUser user = new DiscordService().checkVerified(event.getAuthor().getId());

				String[] args = event.getArgs().split(" ");
				if (args.length < 2 || !Utils.isDouble(args[1]))
					throw new InvalidInputException("Correct usage: `/pay <player> <amount> [shopGroup] [reason...]`");

				OfflinePlayer player = PlayerUtils.getPlayer(user.getUuid());
				OfflinePlayer target = PlayerUtils.getPlayer(args[0]);
				double amount = Double.parseDouble(args[1]);
				ShopGroup shopGroup = ShopGroup.SURVIVAL;
				String reason = null;
				if (args.length > 2)
					shopGroup = ShopGroup.valueOf(args[2]);
				if (args.length > 3)
					reason = event.getArgs().split(" ", 4)[3];

				if (player.getUniqueId().equals(target.getUniqueId()))
					throw new InvalidInputException("You cannot pay yourself");

				if (amount < .01)
					throw new InvalidInputException("Amount must be greater than $0.01");

				try {
					new BankerService().transfer(player, target, BigDecimal.valueOf(amount), shopGroup, TransactionCause.PAY.of(player, target, BigDecimal.valueOf(amount), shopGroup, reason));
				} catch (NegativeBalanceException ex) {
					throw new NotEnoughMoneyException();
				}

				String formatted = prettyMoney(amount);
				if (target.isOnline() && target.getPlayer() != null)
					PlayerUtils.send(target, "&a" + formatted + " has been received from " + player.getName());

				event.reply("Successfully sent " + formatted + " to " + target.getName());
			} catch (Exception ex) {
				event.reply(stripColor(ex.getMessage()));
				if (!(ex instanceof NexusException))
					ex.printStackTrace();
			}
		});
	}

}
