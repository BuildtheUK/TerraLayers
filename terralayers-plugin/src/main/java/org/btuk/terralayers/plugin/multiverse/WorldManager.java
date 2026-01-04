package org.btuk.terralayers.plugin.multiverse;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.Difficulty;
import org.bukkit.GameMode;
import org.bukkit.World;
import org.bukkit.command.CommandSender;
import org.mvplugins.multiverse.core.MultiverseCoreApi;
import org.mvplugins.multiverse.core.world.LoadedMultiverseWorld;
import org.mvplugins.multiverse.core.world.options.CreateWorldOptions;
import org.mvplugins.multiverse.core.world.options.UnloadWorldOptions;

import java.util.concurrent.CompletableFuture;

public class WorldManager {

    private MultiverseCoreApi coreApi;

    public WorldManager() {
        this.coreApi = MultiverseCoreApi.get();
    }

    public CompletableFuture<LoadedMultiverseWorld> createWorld(CommandSender sender, String name) {
        CompletableFuture<LoadedMultiverseWorld> future = new CompletableFuture<>();
        if (coreApi.getWorldManager().getWorld(name).isDefined()) {
            sender.sendMessage(Component.text("World " + name + " already exists!", NamedTextColor.RED));
            future.completeExceptionally(new MultiverseException("World " + name + " already exists!"));
            return future;
        }

        coreApi.getWorldManager().createWorld(
                        CreateWorldOptions.worldName(name)
                                .environment(World.Environment.NORMAL)
                                .generatorSettings("minecraft:flat")) // TODO: T+- generator.
                .onFailure(reason -> future.completeExceptionally(new MultiverseException("Failed to create world " + name + ": " + reason)))
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

        return future;
    }

    public void unloadWorld(String name) {
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
