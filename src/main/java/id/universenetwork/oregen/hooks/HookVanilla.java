package id.universenetwork.oregen.hooks;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Player;

import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public class HookVanilla implements SkyblockAPIHook {
    @Override
    public int getIslandLevel(UUID uuid, String world) {
        return 0;
    }

    @Override
    public Optional<UUID> getIslandOwner(Location loc) {
        Optional<UUID> optional = Optional.empty();

        List<Player> list = loc.getWorld().getPlayers().stream()
                .sorted(Comparator.comparingDouble(e -> e.getLocation().distance(loc)))
                .toList();


        if (list.size() > 0) optional = Optional.of(list.get(0).getUniqueId());

        return optional;
    }

    @Override
    public String[] getSkyBlockWorldNames() {
        return Bukkit.getWorlds().stream().map(World::getName).toArray(String[]::new);
    }
}