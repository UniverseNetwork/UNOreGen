package id.universenetwork.oregen;

import com.google.gson.reflect.TypeToken;
import id.universenetwork.oregen.hooks.HookInfo;
import id.universenetwork.oregen.hooks.HookVanilla;
import id.universenetwork.oregen.hooks.SkyblockAPICached;
import id.universenetwork.oregen.hooks.SkyblockAPIHook;
import id.universenetwork.oregen.misc.NamePlaceholder;
import id.universenetwork.oregen.utils.ConfigHandler;
import id.universenetwork.oregen.utils.JSONConfig;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.OfflinePlayer;
import org.bukkit.World;
import org.bukkit.command.ConsoleCommandSender;
import org.bukkit.entity.Player;
import org.bukkit.plugin.PluginManager;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.IOException;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.*;
import java.util.stream.Collectors;

public class UNOreGen extends JavaPlugin {

    /*
     * Configurations for all generators (defined in the config.yml)
     */
    private List<GeneratorConfig> generatorConfigs = new ArrayList<GeneratorConfig>();

    /*
     * Disabled worlds blacklist
     */
    private List<String> disabledWorlds = new ArrayList<String>();

    /*
     * Our logger
     */
    private ConsoleCommandSender clogger;

    /*
     * Cache for GeneratorConfig ID's for each player
     */
    private HashMap<UUID, Integer> cachedOregenConfigs = new HashMap<UUID, Integer>();
    private JSONConfig cachedOregenJsonConfig;

    /*
     * API Hook for the corresponding SkyBlock plugin
     */
    private SkyblockAPIHook skyblockAPI;

    /*
     * API Hook but cached
     */
    private SkyblockAPICached skyblockAPICached;


    /*
     * Object that handles the loading process of the config.yml file
     */
    private ConfigHandler configHandler = new ConfigHandler(this, "plugins/UNOreGen/config.yml");
    ;

    /*
     * Prefix for the clogger
     */
    private final String PREFIX = "§6[§bU§eN§6OreGen] ";

    /*
     * Main event class
     */
    private Events events;


    @Override
    public void onEnable() {
        
        clogger = getServer().getConsoleSender();

        PluginManager pm = Bukkit.getPluginManager();

        this.loadHook();

        Bukkit.getPluginCommand("unoregen").setExecutor(new Cmd(this));

        // load the config.yml
        try {
            configHandler.loadConfig();
        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }

        events = new Events(this);

        pm.registerEvents(events, this);

        // some persisting saving stuff

        cachedOregenJsonConfig = new JSONConfig(new TypeToken<HashMap<UUID, Integer>>() {
        }.getType(), cachedOregenConfigs, this);

        cachedOregenConfigs = (HashMap<UUID, Integer>) cachedOregenJsonConfig.getObject();

        if (cachedOregenConfigs == null) {
            cachedOregenConfigs = new HashMap<UUID, Integer>();
        }

        disabledWorlds = getConfig().getStringList("disabled-worlds");

        // Registering Placeholders
        if (Bukkit.getPluginManager().getPlugin("PlaceholderAPI") != null) new NamePlaceholder(this).register();
    }

    @Override
    public void onDisable() {
        cachedOregenJsonConfig.saveToDisk();
    }

    /**
     * Acquires the corresponding skyblock hook class
     */
    private void loadHook() {
        skyblockAPI = getHook();
        skyblockAPICached = new SkyblockAPICached(skyblockAPI);
    }

    public SkyblockAPIHook getHook() {
        Throwable loadException = null;
        SkyblockAPIHook skyblockAPI = null;

        for (HookInfo hookInfo : HookInfo.values()) {
            String pluginName = hookInfo.name().replace("Legacy", "");
            if (Bukkit.getServer().getPluginManager().isPluginEnabled(pluginName)) {
                sendConsole(String.format("&aUsing %s as SkyBlock-Plugin, hook class: %s", pluginName, hookInfo.getHookClass().getName()));
                try {
                    for (Constructor<?> constructor : hookInfo.getHookClass().getDeclaredConstructors()) {
                        if (constructor.getParameterCount() > 0)
                            skyblockAPI = (SkyblockAPIHook) constructor.newInstance(this);
                        else skyblockAPI = (SkyblockAPIHook) constructor.newInstance();
                    }
                } catch (NoClassDefFoundError | IllegalArgumentException | InvocationTargetException |
                         InstantiationException | IllegalAccessException e) {
                    loadException = e;
                    continue;
                }
                break;
            }
        }

        if (skyblockAPI == null) {
            if (loadException != null)
                loadException.printStackTrace();
            sendConsole("§cYou are not using any supported skyblock plugin! Will use the vanilla range check hook instead.");
            skyblockAPI = new HookVanilla();
        }

        return skyblockAPI;
    }

    /**
     * @return all active skyblock worlds
     */
    public List<World> getActiveWorlds() {
        return Arrays.stream(skyblockAPI.getSkyBlockWorldNames()).map(v -> Bukkit.getWorld(v))
                .collect(Collectors.toList());
    }

    /**
     * Returns the current island level based on the skyblock world and player.
     *
     * @param uuid  UUID of the player to check
     * @param world the world of the island to check the level
     * @return player's island level
     */
    public int getLevel(UUID uuid, String world) {
        return this.getSkyblockAPICached().getIslandLevel(uuid, world);
    }

    /**
     * Gathers the owner of an island at a certain location
     *
     * @param loc Location to check
     * @return owner
     */
    public OfflinePlayer getApplicablePlayer(Location loc) {
        if (this.getSkyblockAPICached().getIslandOwner(loc).isEmpty()) return null;
        Optional<UUID> uuid = this.getSkyblockAPICached().getIslandOwner(loc);

        return uuid.map(Bukkit::getOfflinePlayer).orElse(null);
    }

    /**
     * Gets the skyblock hook name in use
     *
     * @return The name of the hook class
     */
    public String getHookName() {
        return this.skyblockAPI.getClass().getName();
    }

    public void reload() throws IOException {
        reloadConfig();
        configHandler.loadConfig();

        events.load();
    }

    /**
     * Acquires a generator config that applies for the given player,
     * The result depends on the permission or island level of the player.
     *
     * @param offlinePlayer the offline player (Usually created using the UUID)
     * @param world         the skyblock world
     * @return the generator config
     */
    public GeneratorConfig getGeneratorConfigForPlayer(OfflinePlayer offlinePlayer, String world) {
        GeneratorConfig gc = null;
        int id = 0;

        if (offlinePlayer == null) {
            gc = generatorConfigs.get(0);
            cacheOreGen(offlinePlayer.getUniqueId(), id);
        } else {
            int islandLevel = getLevel(offlinePlayer.getUniqueId(), world);
            if (offlinePlayer.isOnline()) {
                Player realP = offlinePlayer.getPlayer();

                if (this.getActiveWorlds().contains(realP.getWorld())) {
                    for (GeneratorConfig gc2 : generatorConfigs) {
                        if (gc2 == null) continue;
                        if ((realP.hasPermission(gc2.permission) || gc2.permission.length() == 0)
                                && islandLevel >= gc2.unlock_islandLevel) {
                            // continue
                            gc = gc2;
                            id++;
                        }
                    }
                }
            } else gc = getCachedGeneratorConfig(offlinePlayer.getUniqueId());
        }

        if (id > 0) cacheOreGen(offlinePlayer.getUniqueId(), id - 1);

        // fail over if there wasn't found any applicable generator but still no permission and level 0
        if (gc == null && generatorConfigs.get(0) != null
                && (generatorConfigs.get(0).permission == ";" || generatorConfigs.get(0).permission == "" || generatorConfigs.get(0).permission.length() == 0)
                && generatorConfigs.get(0).unlock_islandLevel == 0) {

            gc = generatorConfigs.get(0);
        }

        return gc;
    }

    /**
     * Returns all worlds in which the plugin is disabled in (configurable inside the config.yml)
     *
     * @return A list of world names as string
     */
    public List<String> getDisabledWorlds() {
        return disabledWorlds;
    }

    /**
     * Returns a cached generator config. Useful when an island owner left the server, but a player is still mining at a generator.
     *
     * @param uuid the owners UUID
     * @return the generator config
     */
    public GeneratorConfig getCachedGeneratorConfig(UUID uuid) {
        if (cachedOregenConfigs.containsKey(uuid)) return generatorConfigs.get(cachedOregenConfigs.get(uuid));
        return null;
    }

    /**
     * Writes an existing ore generator config to the cache
     *
     * @param uuid     UUID of the owner
     * @param configID the ID of the generator config
     */
    public void cacheOreGen(UUID uuid, int configID) {
        cachedOregenConfigs.put(uuid, configID);
    }

    /**
     * Sends a formatted messages to the console, colors supported
     *
     * @param msg A string using either & or § for colors
     */
    public void sendConsole(String msg) {
        clogger.sendMessage(PREFIX + msg.replace("&", "§"));
    }

    public List<GeneratorConfig> getGeneratorConfigs() {
        return generatorConfigs;
    }

    public void setGeneratorConfigs(List<GeneratorConfig> generatorConfigs) {
        this.generatorConfigs = generatorConfigs;
    }

    /**
     * @return the skyblockAPICached
     */
    public SkyblockAPICached getSkyblockAPICached() {
        return skyblockAPICached;
    }
}