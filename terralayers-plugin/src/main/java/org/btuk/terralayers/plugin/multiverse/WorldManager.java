package org.btuk.terralayers.plugin.multiverse;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.Difficulty;
import org.bukkit.GameMode;
import org.bukkit.World;
import org.bukkit.command.CommandSender;
import org.bukkit.plugin.java.JavaPlugin;
import org.mvplugins.multiverse.core.MultiverseCoreApi;
import org.mvplugins.multiverse.core.world.LoadedMultiverseWorld;
import org.mvplugins.multiverse.core.world.options.CreateWorldOptions;
import org.mvplugins.multiverse.core.world.options.UnloadWorldOptions;

import java.util.concurrent.CompletableFuture;

public class WorldManager {

    private MultiverseCoreApi coreApi;

    private MultiverseCoreApi getCoreApi() {
        if (this.coreApi == null) {
            this.coreApi = MultiverseCoreApi.get();
            if (this.coreApi == null) {
                throw new IllegalStateException("MultiverseCoreApi is not available. Ensure Multiverse is fully loaded.");
            }
        }
        return this.coreApi;
    }

    public CompletableFuture<LoadedMultiverseWorld> createWorld(JavaPlugin plugin, CommandSender sender, String name, String generator, long delayTicks) {
        MultiverseCoreApi coreApi = getCoreApi();
        CompletableFuture<LoadedMultiverseWorld> future = new CompletableFuture<>();

        // Schedule the world creation on the main thread to avoid blocking the caller
        plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
            if (coreApi.getWorldManager().getWorld(name).isDefined()) {
                sender.sendMessage(Component.text("World " + name + " already exists!", NamedTextColor.RED));
                future.completeExceptionally(new MultiverseException("World " + name + " already exists!"));
                return;
            }

            coreApi.getWorldManager().createWorld(
                            CreateWorldOptions.worldName(name)
                                    .environment(World.Environment.NORMAL)
                                    .generator(generator))
                    .onFailure(reason -> future.completeExceptionally(new MultiverseException("Failed to create world " + name + ": " + reason.getFailureReason())))
                    .onSuccess(world -> {
                        world.setAllowWeather(false);
                        world.setKeepSpawnInMemory(false);
                        world.setAllowAdvancementGrant(false);
                        world.setAllowFlight(true);
                        world.setDifficulty(Difficulty.PEACEFUL);
                        world.setGameMode(GameMode.CREATIVE);
                        world.setPvp(false);

                        coreApi.getWorldManager().saveWorldsConfig();
                        sender.sendMessage(Component.text("Created world " + name, NamedTextColor.GREEN));
                        future.complete(world);
                    });
        }, delayTicks);
        return future;
    }

    public void unloadWorld(String name) {
        MultiverseCoreApi coreApi = getCoreApi();
        coreApi.getWorldManager().getWorld(name).peek(
                world -> {
                    if (coreApi.getWorldManager().isLoadedWorld(world)) {
                        LoadedMultiverseWorld loadedWorld = (LoadedMultiverseWorld) world;
                        coreApi.getWorldManager().unloadWorld(UnloadWorldOptions.world(loadedWorld));
                    }
                }
        );
    }
}
