package org.btuk.terralayers.plugin.listeners;

import org.btuk.terralayers.api.LayerManager;
import org.btuk.terralayers.api.LayeredWorld;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerTeleportEvent;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.Optional;

/**
 * Listener that ensures player movement and teleports end up in the correct layer.
 */
public final class SwitchLayerListener implements TerraLayersListener {

    private final JavaPlugin plugin;

    private final LayerManager layerManager;

    public SwitchLayerListener(JavaPlugin plugin, LayerManager layerManager) {
        this.plugin = plugin;
        this.layerManager = layerManager;
        this.register();
    }

    private void register() {
        this.plugin.getServer().getPluginManager().registerEvents(this, plugin);
    }

    public void unregister() {
        PlayerMoveEvent.getHandlerList().unregister(this);
        PlayerTeleportEvent.getHandlerList().unregister(this);
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    void onPlayerMove(PlayerMoveEvent event) {
        switchLayer(event);
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    void onPlayerTeleport(PlayerTeleportEvent event) {
        switchLayer(event);
    }

    private void switchLayer(PlayerMoveEvent event) {
        // Get the new layered world.
        Optional<LayeredWorld> optionalCurrentLayer = layerManager.getLayerForWorld(event.getTo().getWorld());

        // If the y-level of the new layer does not fit in the world, teleport them to the correct layer.
        optionalCurrentLayer.ifPresent(currentLayer -> {
            if (currentLayer.getTeleportMinY() > event.getTo().getY() || currentLayer.getTeleportMaxY() < event.getTo().getY()) {
                layerManager.getLayerForGlobalY(event.getTo().getBlockY()).ifPresent(newLayer -> {
                    event.getTo().setWorld(newLayer.getWorld());
                });
            }
        });
    }
}
