package me.pugabyte.nexus.features.quests.pathfinder;

import com.sk89q.worldedit.regions.Region;
import lombok.NoArgsConstructor;
import me.pugabyte.nexus.Nexus;
import me.pugabyte.nexus.framework.commands.models.CustomCommand;
import me.pugabyte.nexus.framework.commands.models.annotations.Confirm;
import me.pugabyte.nexus.framework.commands.models.annotations.Path;
import me.pugabyte.nexus.framework.commands.models.annotations.Permission;
import me.pugabyte.nexus.framework.commands.models.events.CommandEvent;
import me.pugabyte.nexus.models.quests.WebConfig;
import me.pugabyte.nexus.models.quests.WebConfig.Node;
import me.pugabyte.nexus.models.quests.WebConfig.Route;
import me.pugabyte.nexus.models.quests.WebConfig.Web;
import me.pugabyte.nexus.models.quests.WebConfigService;
import me.pugabyte.nexus.utils.PlayerUtils;
import me.pugabyte.nexus.utils.RandomUtils;
import me.pugabyte.nexus.utils.StringUtils;
import me.pugabyte.nexus.utils.Tasks;
import me.pugabyte.nexus.utils.WorldEditUtils;
import me.pugabyte.nexus.utils.WorldGuardUtils;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

import static me.pugabyte.nexus.utils.StringUtils.getShortLocationString;

@NoArgsConstructor
@Permission("group.admin")
public class PathfinderCommand extends CustomCommand implements Listener {

	public PathfinderCommand(CommandEvent event) {
		super(event);
	}

	@Path("list")
	public void list() {
		WebConfigService service = new WebConfigService();
		WebConfig config = service.get(Nexus.getUUID0());
		send("Total Webs: " + config.getWebs().size());
		for (Web web : config.getWebs()) {
			send(" - Web: " + web.getId());
			send(" | Routes: " + web.getRoutes().size());
			send(" | Nodes: " + web.getNodes().size());

			web.getNodes().forEach(node -> {
				send(" | - Loc: " + StringUtils.getShortLocationString(node.getLocation()));
				send(" | - Rad: " + node.getRadius());
				send(" | - Neighbors: " + node.getNeighbors().keySet().size());
				send(" | ");
			});
		}
	}

	@Confirm
	@Path("reset")
	public void resetData() {
		WebConfigService service = new WebConfigService();
		service.deleteAll();
		send("Deleted all webs");
	}

	@Path("new test")
	public void newTest() {
		WebConfigService service = new WebConfigService();
		WebConfig config = service.get(Nexus.getUUID0());
		Web web = new Web("test");

		WorldGuardUtils WGUtils = new WorldGuardUtils(player());
		Region region = WGUtils.getRegion("pathfinder_test");
		WorldEditUtils WEUtils = new WorldEditUtils(player());
		for (Block block : WEUtils.getBlocks(region)) {
			if (block.getType().equals(Material.BLACK_CONCRETE)) {
				Node node = new Node(block.getLocation());
				web.getNodes().add(node);
			}
		}

		config.getWebs().add(web);
		service.save(config);
		send("&aNew web created: \"" + web.getId() + "\"");
	}

	@EventHandler
	public void onPlaceBlock(BlockPlaceEvent event) {
		Player player = event.getPlayer();
		if (!PlayerUtils.isWakka(player)) return;

		Material type = event.getBlock().getType();
		boolean end = true;
		switch (type) {
			case LIME_CONCRETE_POWDER:
			case PURPLE_CONCRETE_POWDER:
			case YELLOW_CONCRETE_POWDER:
			case PINK_CONCRETE_POWDER:
			case CYAN_CONCRETE_POWDER:
			case WHITE_CONCRETE_POWDER:
			case RED_CONCRETE_POWDER:
			case ORANGE_CONCRETE_POWDER:
				end = false;
		}
		if (end)
			return;

		WorldGuardUtils WGUtils = new WorldGuardUtils(player);
		if (!WGUtils.isInRegion(player, "pathfinder_test"))
			return;

		WebConfigService service = new WebConfigService();
		WebConfig config = service.get(Nexus.getUUID0());
		Web web = config.getById("test");
		if (web == null) {
			send(player, "&cWeb is null");
			return;
		}

		Node selectedNode = web.getNodeByLocation(Pathfinder.getSelectedLoc());

		Location currentLoc = event.getBlock().getRelative(BlockFace.DOWN).getLocation();
		Node currentNode = web.getNodeByLocation(currentLoc);
		if (currentNode == null) {
			send(player, "&cThat node doesn't exist");
			return;
		}

		if (type.equals(Material.LIME_CONCRETE_POWDER) && selectedNode == null) {
			Pathfinder.setSelectedLoc(currentNode.getLocation());
			send(player, "&aSelected node at " + getShortLocationString(currentLoc));

		} else if (type.equals(Material.PURPLE_CONCRETE_POWDER) && selectedNode != null) {
			Location selectedLoc = selectedNode.getLocation();
			double distance = selectedLoc.distance(currentLoc);

			selectedNode.getNeighbors().put(currentNode.getUuid(), distance);
			currentNode.getNeighbors().put(selectedNode.getUuid(), distance);

			service.save(config);
			send(player, "&dAdded the nodes as neighbors");

		} else if (type.equals(Material.WHITE_CONCRETE_POWDER)) {
			for (Node neighbor : web.getNeighborNodes(currentNode))
				neighbor.getLocation().getBlock().getRelative(BlockFace.UP).setType(Material.LIGHT_GRAY_CONCRETE_POWDER);

		} else if (type.equals(Material.YELLOW_CONCRETE_POWDER))
			randomPath(web, currentNode, true);

		else if (type.equals(Material.PINK_CONCRETE_POWDER))
			randomPath(web, currentNode, false);

		else if (type.equals(Material.CYAN_CONCRETE_POWDER))
			furthestPath(web, currentNode);

		else if (type.equals(Material.RED_CONCRETE_POWDER)) {
			Pathfinder.setTargetA(currentLoc);
			send("Target A set");
			targetPath(web);
		} else if (type.equals(Material.ORANGE_CONCRETE_POWDER)) {
			Pathfinder.setTargetB(currentLoc);
			send("Target B set");

		}


	}

	@EventHandler
	public void onBreakBlock(BlockBreakEvent event) {
		Player player = event.getPlayer();
		if (!PlayerUtils.isWakka(player)) return;

		Material type = event.getBlock().getType();
		boolean end = true;
		switch (type) {
			case LIME_CONCRETE_POWDER:
			case PURPLE_CONCRETE_POWDER:
			case WHITE_CONCRETE_POWDER:
				end = false;
		}
		if (end)
			return;

		WorldGuardUtils WGUtils = new WorldGuardUtils(player);
		if (!WGUtils.isInRegion(player, "pathfinder_test"))
			return;

		WebConfigService service = new WebConfigService();
		WebConfig config = service.get(Nexus.getUUID0());
		Web web = config.getById("test");
		if (web == null) {
			send(player, "&cWeb is null");
			return;
		}

		Location location = event.getBlock().getRelative(BlockFace.DOWN).getLocation();
		Node currentNode = web.getNodeByLocation(location);
		if (currentNode == null) {
			send(player, "&cThat node doesn't exist");
			return;
		}

		if (type.equals(Material.WHITE_CONCRETE_POWDER)) {
			for (Node neighbor : web.getNeighborNodes(currentNode))
				neighbor.getLocation().getBlock().getRelative(BlockFace.UP).setType(Material.AIR);
			return;
		}

		Node selectedNode = web.getNodeByLocation(Pathfinder.getSelectedLoc());
		if (selectedNode == null) {
			send(player, "&cYou don't have a node selected");
			return;
		}

		if (type.equals(Material.LIME_CONCRETE_POWDER)) {
			send(player, "&2Unselected node at " + getShortLocationString(selectedNode.getLocation()));
			Pathfinder.setSelectedLoc(null);

		} else if (type.equals(Material.PURPLE_CONCRETE_POWDER)) {
			send(player, "&5Removed nodes as neighbors");
			selectedNode.getNeighbors().remove(currentNode.getUuid());
			currentNode.getNeighbors().remove(selectedNode.getUuid());
			service.save(config);

		}
	}

	private void furthestPath(Web web, Node currentNode) {
		Location origin = currentNode.getLocation();
		Route route = new Route();
		Node furthest = currentNode;

		int SAFETY = 0;
		while (true) {
			++SAFETY;
			if (SAFETY > web.getNodes().size())
				break;

			List<Node> neighbors = new ArrayList<>(web.getNeighborNodes(furthest));
			double furthestDistance = 0;
			Node temp = null;
			for (Node neighbor : neighbors) {
				double distance = neighbor.getLocation().distance(origin);
				if (distance >= furthestDistance) {
					temp = neighbor;
					furthestDistance = distance;
				}
			}

			if (furthest.getLocation().distance(origin) > furthestDistance) {
				break;
			} else {
				furthest = temp;
				if (furthest != null)
					route.addNode(furthest);
			}
		}

		walkRoute(web, route);
	}

	private void randomPath(Web web, Node currentNode, boolean repeating) {
		int steps = RandomUtils.randomInt(1, web.getNodes().size());
		Node previous = null;
		Route route = new Route();

		int SAFETY = 0;
		for (int i = 0; i < steps; i++) {
			++SAFETY;
			if (SAFETY > 200)
				break;

			List<Node> neighbors = new ArrayList<>(web.getNeighborNodes(currentNode));
			if (previous != null)
				neighbors.remove(previous);

			Node next = RandomUtils.randomElement(neighbors);
			if (next == null)
				break;

			if (!repeating && route.getNodeUuids().contains(next.getUuid()))
				--i;
			else {
				previous = currentNode;
				currentNode = next;
				route.addNode(currentNode);
			}
		}

		walkRoute(web, route);
	}

	private void targetPath(Web web) {
		Node origin = web.getNodeByLocation(Pathfinder.getTargetA());
		Node target = web.getNodeByLocation(Pathfinder.getTargetB());

	}

	private void walkRoute(Web web, Route route) {
		int wait = 0;
		Material primary = Material.IRON_BLOCK;
		Material secondary = Material.QUARTZ_BLOCK;
		LinkedList<Node> routeNodes = web.getRouteNodes(route);
		int count = 0;
		int size = routeNodes.size();
		for (Node routeNode : routeNodes) {
			++count;
			Location loc = routeNode.getLocation();
			int finalCount = count;

			Tasks.wait(wait += 10, () -> {
				Block block = loc.getBlock().getRelative(BlockFace.UP);
				if (finalCount == size)
					block.setType(Material.REDSTONE_BLOCK);
				else if (!block.getType().equals(Material.AIR))
					block.setType(secondary);
				else
					block.setType(primary);
			});
		}
	}

}
