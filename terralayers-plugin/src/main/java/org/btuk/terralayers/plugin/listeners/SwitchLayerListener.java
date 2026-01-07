package org.btuk.terralayers.plugin.listeners;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.btuk.terralayers.api.LayerManager;
import org.btuk.terralayers.api.LayeredWorld;
import org.btuk.terralayers.plugin.config.ConfigManager;
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
    private final ConfigManager configManager;

    public SwitchLayerListener(JavaPlugin plugin, LayerManager layerManager, ConfigManager configManager) {
        this.plugin = plugin;
        this.layerManager = layerManager;
        this.configManager = configManager;
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
        LayeredWorld currentLayer = layerManager.getLayerForWorld(event.getTo().getWorld());

        if (currentLayer == null) {
            return;
        }

        // If the y-level of the new layer does not fit in the world, teleport them to the correct layer.
        // Correct the y-level to take the offset of the new world into account.
        int actualY = event.getTo().getBlockY() + currentLayer.getMinY();
        if (currentLayer.getTeleportMinY() > actualY || currentLayer.getTeleportMaxY() < actualY) {
            // TODO: Deal with players outside min or max y.
            layerManager.getLayerForGlobalY(actualY).ifPresent(newLayer -> {
                plugin.getLogger().info("Player " + event.getPlayer().getName() + " moved to a layer outside of their current world, teleporting them to the correct layer.");
                double y = event.getTo().getY();
                y = currentLayer.getTeleportMinY() > actualY ? y + configManager.getWorldHeight() : y - configManager.getWorldHeight();
                event.getTo().setWorld(newLayer.getWorld());
                event.getTo().setY(y);
                plugin.getLogger().info("Player " + event.getPlayer().getName() + " teleported to layer " + newLayer.getName() + " at y=" + y);
            });
        }
        event.getPlayer().sendActionBar(Component.text("Y: " + actualY, NamedTextColor.GOLD, TextDecoration.BOLD));
    }
}
