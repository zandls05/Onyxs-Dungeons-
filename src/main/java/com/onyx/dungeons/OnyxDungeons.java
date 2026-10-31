package com.onyx.dungeons;

import com.onyx.dungeons.command.DungeonCommand;
import com.onyx.dungeons.hologram.HologramManager;
import com.onyx.dungeons.listener.GameListener;
import com.onyx.dungeons.listener.MenuListener;
import com.onyx.dungeons.listener.PromptListener;
import com.onyx.dungeons.listener.RestrictionListener;
import com.onyx.dungeons.listener.WandListener;
import com.onyx.dungeons.manager.DungeonManager;
import com.onyx.dungeons.manager.KeyManager;
import com.onyx.dungeons.manager.LootManager;
import com.onyx.dungeons.manager.MessageManager;
import com.onyx.dungeons.manager.PromptManager;
import com.onyx.dungeons.manager.WandManager;
import org.bukkit.NamespacedKey;
import org.bukkit.command.PluginCommand;
import org.bukkit.plugin.java.JavaPlugin;

/** Onyx Dungeons - dungeons por niveles con jefes, llaves, puertas, cofres con loot aleatorio y menus. */
public class OnyxDungeons extends JavaPlugin {
    private static OnyxDungeons instance;

    private MessageManager messages;
    private KeyManager keys;
    private LootManager loot;
    private DungeonManager dungeons;
    private HologramManager holograms;
    private WandManager wands;
    private PromptManager prompts;

    public static OnyxDungeons getInstance() {
        return instance;
    }

    public static NamespacedKey key(String name) {
        return new NamespacedKey(instance, name);
    }

    @Override
    public void onEnable() {
        instance = this;
        saveDefaultConfig();
        messages = new MessageManager(this);
        messages.load();
        keys = new KeyManager(this);
        keys.load();
        loot = new LootManager(this);
        loot.load();
        dungeons = new DungeonManager(this);
        dungeons.load();
        holograms = new HologramManager(this);
        holograms.init();
        wands = new WandManager(this);
        prompts = new PromptManager(this);

        getServer().getPluginManager().registerEvents(new MenuListener(), this);
        getServer().getPluginManager().registerEvents(new WandListener(this), this);
        getServer().getPluginManager().registerEvents(new PromptListener(this), this);
        getServer().getPluginManager().registerEvents(new RestrictionListener(this), this);
        getServer().getPluginManager().registerEvents(new GameListener(this), this);

        PluginCommand cmd = getCommand("dungeon");
        if (cmd != null) {
            DungeonCommand handler = new DungeonCommand(this);
            cmd.setExecutor(handler);
            cmd.setTabCompleter(handler);
        }

        dungeons.cleanOnEnable();
        dungeons.startTask();
        getLogger().info("Onyx Dungeons activado: " + dungeons.all().size() + " dungeon(s), "
                + loot.ids().size() + " tabla(s) de loot, " + keys.ids().size() + " llave(s).");
    }

    @Override
    public void onDisable() {
        if (dungeons != null) dungeons.shutdown();
        if (holograms != null) holograms.shutdown();
        getLogger().info("Onyx Dungeons desactivado.");
    }

    /** Recarga config, mensajes, llaves, loot y dungeons (termina las partidas en curso). */
    public void reloadAll() {
        dungeons.shutdown();
        reloadConfig();
        messages.load();
        keys.load();
        loot.load();
        dungeons.load();
        holograms.shutdown();
        holograms.init();
        dungeons.cleanOnEnable();
        dungeons.startTask();
    }

    public MessageManager messages() { return messages; }

    public KeyManager keys() { return keys; }

    public LootManager loot() { return loot; }

    public DungeonManager dungeons() { return dungeons; }

    public HologramManager holograms() { return holograms; }

    public WandManager wands() { return wands; }

    public PromptManager prompts() { return prompts; }
}
