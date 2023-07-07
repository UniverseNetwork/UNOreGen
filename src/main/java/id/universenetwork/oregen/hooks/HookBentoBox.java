package id.universenetwork.oregen.hooks;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import world.bentobox.bentobox.BentoBox;
import world.bentobox.bentobox.database.objects.Island;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.Optional;
import java.util.UUID;

public class HookBentoBox implements SkyblockAPIHook {
    private final BentoBox api;

    public HookBentoBox() {
        api = (BentoBox) Bukkit.getPluginManager().getPlugin("BentoBox");
    }

    @Override
    public int getIslandLevel(UUID uuid, String onWorld) {
        int[] level = new int[]{0};
        api.getAddonsManager().getAddonByName("Level").ifPresent(addon -> {
            try {
                Method method = addon.getClass().getMethod("getIslandLevel", World.class, UUID.class);
                long rawLevel = (long) method.invoke(addon, Bukkit.getWorld(onWorld), uuid);
                level[0] = Math.toIntExact(rawLevel);
            } catch (NoSuchMethodException | IllegalAccessException | InvocationTargetException e) {
                e.printStackTrace();
            }
        });

        return level[0];
    }

    @Override
    public Optional<UUID> getIslandOwner(Location loc) {
        Optional<Island> optIsland = api.getIslands().getIslandAt(loc);
        Optional<UUID> optional = Optional.empty();

        if (optIsland.isPresent() && optIsland.get().getOwner() != null)
            optional = Optional.of(optIsland.get().getOwner());
        return optional;
    }

    @Override
    public String[] getSkyBlockWorldNames() {
        return api.getIWM().getOverWorlds().stream().map(w -> new String[]{w.getName(), w.getName() + "_nether", w.getName() + "_the_end"}).flatMap(Arrays::stream).toArray(String[]::new);
    }
}