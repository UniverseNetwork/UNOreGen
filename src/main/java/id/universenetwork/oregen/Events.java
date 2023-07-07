package id.universenetwork.oregen;

import org.bukkit.*;
import org.bukkit.block.Block;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockFormEvent;
import org.bukkit.event.player.PlayerJoinEvent;

import java.util.Arrays;
import java.util.Optional;


public class Events implements Listener {
    /*
     * UNOreGen main class
     */
    private final UNOreGen plugin;

    private boolean enableStoneGenerator;
    private Optional<Sound> soundEffect;
    private boolean enableParticleEffect;

    public Events(UNOreGen unOreGen) {
        this.plugin = unOreGen;
        load();
    }

    public void load() {
        this.enableStoneGenerator = plugin.getConfig().getBoolean("enable-stone-generator");
        this.enableParticleEffect = plugin.getConfig().getBoolean("enable-particle-effect", false);

        if (enableParticleEffect) {
            try {
                Class.forName("org.bukkit.Particle");
            } catch (ClassNotFoundException e) {
                this.plugin.getLogger().info(
                        String.format("Particle effects are not supported for your bukkit version, disable 'enable-particle-effect' in %s/config.yml to get rid of this message.", this.plugin.getDataFolder().getPath()));
                this.enableParticleEffect = false;
            }
        }

        if (plugin.getConfig().getBoolean("enable-sound-effect", false)) {
            // disabling sound effects when enum value not present
            soundEffect = Arrays.stream(Sound.values()).filter(s -> {
                return s.name().equals("BLOCK_FIRE_EXTINGUISH") || s.name().equals("FIZZ");
            }).findAny();
        }
    }

    /**
     * This is used in minecraft versions >= 1.12 and does nothing on other version.
     */
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void blockFormEvent(BlockFormEvent event) {
        if (plugin.getDisabledWorlds().contains(event.getBlock().getLocation().getWorld().getName())) return;
        if (event.getNewState().getType().equals(Material.COBBLESTONE)
                || (!enableStoneGenerator && event.getNewState().getType().equals(Material.STONE))) {
            event.setCancelled(true);

            GeneratorConfig generatorConfig = this.getGeneratorConfigAtLocation(event.getBlock().getLocation());
            if (generatorConfig != null) {
                GeneratorItem generatorItem = generatorConfig.getRandomItem();
                Material material = Material.getMaterial(generatorItem.getName());

                if (material != null) placeBlock(event.getBlock(), material); // set actual block
                return;
            }

            placeBlock(event.getBlock(), event.getNewState().getType());
        }
    }

    private void placeBlock(Block block, Material material) {
        block.setType(material);
        block.getState().update(true);


        soundEffect.ifPresent(sound -> block.getWorld().playSound(block.getLocation(),
                sound,
                0.5f, 2.6f + ((float) Math.random() - (float) Math.random()) * 0.8f));

        if (enableParticleEffect) {
            block.getWorld().spawnParticle(Particle.SMOKE_LARGE,
                    block.getLocation().getBlockX() + 0.5D,
                    block.getLocation().getBlockY() + 0.25D,
                    block.getLocation().getBlockZ() + 0.5D,
                    8, 0.5D, 0.25D, 0.5D, 0.0D);
        }
    }

    private GeneratorConfig getGeneratorConfigAtLocation(Location location) {
        OfflinePlayer player = plugin.getApplicablePlayer(location);
        if (player == null) return null;

        return plugin.getGeneratorConfigForPlayer(player, location.getWorld().getName());
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent e) {
        plugin.getGeneratorConfigForPlayer(e.getPlayer(), e.getPlayer().getWorld().getName());
    }
}