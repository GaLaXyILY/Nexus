package me.pugabyte.nexus.features.commands;

import lombok.NonNull;
import me.pugabyte.nexus.Nexus;
import me.pugabyte.nexus.features.chat.Chat;
import me.pugabyte.nexus.features.chat.Chat.StaticChannel;
import me.pugabyte.nexus.framework.commands.models.CustomCommand;
import me.pugabyte.nexus.framework.commands.models.annotations.Aliases;
import me.pugabyte.nexus.framework.commands.models.annotations.Arg;
import me.pugabyte.nexus.framework.commands.models.annotations.Confirm;
import me.pugabyte.nexus.framework.commands.models.annotations.ConverterFor;
import me.pugabyte.nexus.framework.commands.models.annotations.Cooldown;
import me.pugabyte.nexus.framework.commands.models.annotations.Cooldown.Part;
import me.pugabyte.nexus.framework.commands.models.annotations.Description;
import me.pugabyte.nexus.framework.commands.models.annotations.Path;
import me.pugabyte.nexus.framework.commands.models.annotations.Permission;
import me.pugabyte.nexus.framework.commands.models.annotations.TabCompleterFor;
import me.pugabyte.nexus.framework.commands.models.events.CommandEvent;
import me.pugabyte.nexus.framework.exceptions.postconfigured.InvalidInputException;
import me.pugabyte.nexus.models.modreview.ModReview;
import me.pugabyte.nexus.models.modreview.ModReview.Mod;
import me.pugabyte.nexus.models.modreview.ModReview.Mod.ModVerdict;
import me.pugabyte.nexus.models.modreview.ModReview.ModReviewRequest;
import me.pugabyte.nexus.models.modreview.ModReviewService;
import me.pugabyte.nexus.utils.JsonBuilder;
import me.pugabyte.nexus.utils.PlayerUtils;
import me.pugabyte.nexus.utils.Time;

import java.util.HashSet;
import java.util.List;
import java.util.function.BiFunction;
import java.util.stream.Collectors;

import static me.pugabyte.nexus.utils.PlayerUtils.getPlayer;

@Aliases({"modcheck", "checkmod"})
public class ModReviewCommand extends CustomCommand {
	private final ModReviewService service = new ModReviewService();
	private final ModReview modReview = service.get(Nexus.getUUID0());
	private final List<Mod> mods = modReview.getMods();
	private final List<ModReviewRequest> requests = modReview.getRequests();

	public ModReviewCommand(@NonNull CommandEvent event) {
		super(event);
	}

	@Path("<mod>")
	@Description("View detailed information on a mod and it's verdict")
	void check(Mod mod) {
		line();
		send(PREFIX + "&e" + mod.getName());
		if (mod.getAliases().size() > 0)
			send(" &3Also known as: &7" + String.join(", ", mod.getAliases()));
		send(" &3Verdict: " + mod.getVerdict().getColor() + camelCase(mod.getVerdict()));
		if (!isNullOrEmpty(mod.getNotes()))
			send(" &3Notes: &7" + mod.getNotes());
		line();
	}

	@Path("list [page]")
	@Description("List reviewed mods")
	void list(@Arg("1") int page) {
		if (mods.isEmpty())
			error("No available mod reviews");

		if (page == 1)
			send(PREFIX + "List of reviewed mods. Click on a mod for more info");

		BiFunction<Mod, Integer, JsonBuilder> formatter = (mod, index) -> json()
				.next("&3" + (index + 1) + " &e" + mod.getName() + " &7- " + mod.getVerdict().getColor() + camelCase(mod.getVerdict()))
				.command("/modreview " + mod.getName())
				.hover("&3Click for more info");

		paginate(mods, formatter, "/modreview list", page);

		if (page == 1)
			send(PREFIX + "&3If your mod is not on this list, request it to be reviewed with &c/modreview request <name> [notes...]");
	}

	@Cooldown(@Part(value = Time.SECOND, x = 30))
	@Path("request <name> [notes...]")
	@Description("Request a mod to be reviewed by the staff team")
	void request(String name, String notes) {
		ModReviewRequest request = new ModReviewRequest(uuid(), name, notes);
		modReview.request(request);
		save();
		send(PREFIX + "Requested mod &e" + name + " &3to be reviewed");
		Chat.broadcast(json(PREFIX + "&e" + player().getName() + " &3has requested mod &e" + name + " &3to be reviewed").command("/modreview requests"), StaticChannel.STAFF);
	}

	@Permission("group.staff")
	@Path("requests [page]")
	void requests(@Arg("1") int page) {
		if (requests.isEmpty())
			error("No pending review requests");

		BiFunction<ModReviewRequest, Integer, JsonBuilder> formatter = (request, index) -> {
			JsonBuilder json = json("&3" + (index + 1) + " &3" + getPlayer(request.getRequester()).getName() + " &e" + request.getName() +
					(isNullOrEmpty(request.getNotes()) ? "" : " &7- " + request.getNotes()));
			if (PlayerUtils.isAdminGroup(player()))
				json.suggest("/modreview add " + request.getName() + " ").hover("&3Click to review");
			return json;
		};

		paginate(requests, formatter, "/modreview requests", page);
	}

	@Confirm
	@Permission("group.admin")
	@Path("requests remove <mod>")
	void removeRequest(ModReviewRequest request) {
		requests.remove(request);
		save();
		send(PREFIX + "Removed aliases to mod &e" + request.getName());
	}

	@Permission("group.admin")
	@Path("add <name> <verdict> [notes...]")
	void add(String name, ModVerdict verdict, String notes) {
		Mod mod = new Mod(name, verdict, notes);
		modReview.add(mod);
		save();
		send(PREFIX + "Added mod &e" + mod.getName());
	}

	@Permission("group.admin")
	@Path("alias add <mod> <aliases...>")
	void addAliases(Mod mod, @Arg(type = String.class) List<String> aliases) {
		mod.getAliases().addAll(aliases);
		save();
		send(PREFIX + "Added aliases to mod &e" + mod.getName());
	}

	@Permission("group.admin")
	@Path("alias remove <mod> <aliases...>")
	void removeAliases(Mod mod, @Arg(type = String.class) List<String> aliases) {
		mod.getAliases().removeAll(aliases);
		save();
		send(PREFIX + "Removed aliases to mod &e" + mod.getName());
	}

	@Permission("group.admin")
	@Path("set name <mod> <name>")
	void setName(Mod mod, String name) {
		mod.setName(name);
		save();
		send(PREFIX + "Name updated for mod &e" + mod.getName());
	}

	@Permission("group.admin")
	@Path("set verdict <mod> <verdict>")
	void setVerdict(Mod mod, ModVerdict verdict) {
		mod.setVerdict(verdict);
		save();
		send(PREFIX + "Verdict updated for mod &e" + mod.getName());
	}

	@Permission("group.admin")
	@Path("set notes <mod> <notes>")
	void setNotes(Mod mod, String notes) {
		mod.setNotes(notes);
		save();
		send(PREFIX + "Notes updated for mod &e" + mod.getName());
	}

	@Confirm
	@Permission("group.admin")
	@Path("delete <mod>")
	void delete(Mod mod) {
		modReview.getMods().remove(mod);
		save();
		send(PREFIX + "Deleted mod &e" + mod.getName());
	}

	private void save() {
		service.save(modReview);
	}

	@ConverterFor(Mod.class)
	Mod convertToMod(String value) {
		return modReview.findMatch(value).orElseThrow(() -> new InvalidInputException("Mod &e" + value +" &cnot found"));
	}

	@TabCompleterFor(Mod.class)
	List<String> tabCompleteMod(String filter) {
		return new HashSet<String>() {{
			mods.forEach(mod -> {
				add(mod.getName());
				addAll(mod.getAliases());
			});
		}}.stream()
				.filter(mod -> mod.toLowerCase().startsWith(filter.toLowerCase()))
				.collect(Collectors.toList());
	}

	@ConverterFor(ModReviewRequest.class)
	ModReviewRequest convertToModReviewRequest(String value) {
		return modReview.findRequestMatch(value).orElseThrow(() -> new InvalidInputException("Mod review request &e" + value +" &cnot found"));
	}

	@TabCompleterFor(ModReviewRequest.class)
	List<String> tabCompleteModReviewRequest(String filter) {
		return requests.stream()
				.map(ModReviewRequest::getName)
				.filter(request -> request.toLowerCase().startsWith(filter.toLowerCase()))
				.collect(Collectors.toList());
	}

}
