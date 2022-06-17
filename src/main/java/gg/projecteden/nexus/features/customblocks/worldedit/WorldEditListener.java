package gg.projecteden.nexus.features.customblocks.worldedit;

import com.sk89q.worldedit.EditSession.Stage;
import com.sk89q.worldedit.WorldEdit;
import com.sk89q.worldedit.bukkit.BukkitAdapter;
import com.sk89q.worldedit.event.extent.EditSessionEvent;
import com.sk89q.worldedit.util.eventbus.Subscribe;
import gg.projecteden.nexus.Nexus;
import lombok.Data;
import org.bukkit.World;

@Data
public class WorldEditListener {
	private static boolean initialized = false;
	private static WorldEditListener event = new WorldEditListener();

	public static void register() {
		if (initialized)
			return;

		try {
			WorldEdit.getInstance().getEventBus().register(event);
			initialized = true;
		} catch (Exception e) {
			Nexus.warn("Failed to register CustomBlock's WorldEditListener");
			e.printStackTrace();
		}
	}

	public static void unregister() {
		if (!initialized)
			return;

		try {
			WorldEdit.getInstance().getEventBus().unregister(event);
			initialized = false;
		} catch (Exception e) {
			Nexus.warn("Failed to unregister CustomBlock's WorldEditListener");
			e.printStackTrace();
		}
	}

	@Subscribe
	public void onEditSessionEvent(EditSessionEvent event) {
		if (event.isCancelled())
			return;

		if (event.getWorld() == null)
			return;

		World world = BukkitAdapter.adapt(event.getWorld());
		if (world == null)
			return;

		if (event.getStage() == Stage.BEFORE_HISTORY)
			event.setExtent(new CustomBlockExtent(event.getExtent(), world));
	}
}
