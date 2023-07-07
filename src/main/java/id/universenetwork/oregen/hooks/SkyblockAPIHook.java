package id.universenetwork.oregen.hooks;

import org.bukkit.Location;

import java.util.Optional;
import java.util.UUID;

public interface SkyblockAPIHook {
    /**
     * Returns the island level for a defined player uuid
     *
     * @param uuid    UUID of the island owner
     * @param inWorld world of the island
     * @return island level
     */
    int getIslandLevel(UUID uuid, String inWorld);

    /**
     * Gets the owner of an island on a certain location
     *
     * @param loc location to check for island
     * @return island owner UUID
     */
    Optional<UUID> getIslandOwner(Location loc);

    /**
     * Obtains the names of the skyblock worlds
     *
     * @return the names of the skyblock worlds
     */
    String[] getSkyBlockWorldNames();
}
