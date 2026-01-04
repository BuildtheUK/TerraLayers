package org.btuk.terralayers.api;

import org.bukkit.Bukkit;
import org.bukkit.plugin.ServicesManager;

import java.util.Objects;
import java.util.Optional;

/**
 * Static accessors for TerraLayers services via Bukkit's ServicesManager.
 * Other plugins should depend on the TerraLayers plugin at runtime and
 * compile against the terralayers-api artifact.
 */
public final class TerraLayersApi {
    private TerraLayersApi() {}

    public static Optional<LayerManager> getLayerManager() {
        ServicesManager sm = Bukkit.getServer().getServicesManager();
        return Optional.ofNullable(sm.load(LayerManager.class));
    }

    public static LayerManager requireLayerManager() {
        return Objects.requireNonNull(getLayerManager().orElse(null), "TerraLayers LayerManager service not available yet");
    }
}
