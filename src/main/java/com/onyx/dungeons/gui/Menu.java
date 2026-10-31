package com.onyx.dungeons.gui;

import com.onyx.dungeons.OnyxDungeons;
import com.onyx.dungeons.util.ItemUtil;
import com.onyx.dungeons.util.Text;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Consumer;
import java.util.function.DoubleConsumer;
import java.util.function.IntConsumer;

/** Base de todos los menus: cada boton tiene una accion asociada a su slot. */
public abstract class Menu implements InventoryHolder {
    protected final OnyxDungeons plugin;
    protected final Player player;
    protected final Inventory inventory;
    private final Map<Integer, Consumer<InventoryClickEvent>> actions = new HashMap<>();

    protected Menu(OnyxDungeons plugin, Player player, String title, int rows) {
        this.plugin = plugin;
        this.player = player;
        this.inventory = Bukkit.createInventory(this, rows * 9, Text.color(title));
    }

    protected abstract void build();

    public void open() {
        rebuild();
        player.openInventory(inventory);
    }

    public void rebuild() {
        inventory.clear();
        actions.clear();
        build();
    }

    public void refresh() {
        rebuild();
        player.updateInventory();
    }

    /** Abre otro menu en el siguiente tick (evita problemas al cambiar de inventario dentro de un clic). */
    protected void go(Menu next) {
        Bukkit.getScheduler().runTask(plugin, () -> next.open());
    }

    protected void set(int slot, ItemStack item, Consumer<InventoryClickEvent> action) {
        if (slot < 0 || slot >= inventory.getSize()) return;
        inventory.setItem(slot, item);
        if (action != null) actions.put(slot, action);
    }

    protected void set(int slot, ItemStack item) {
        set(slot, item, null);
    }

    protected void fillEmpty() {
        ItemStack glass = ItemUtil.make(Material.GRAY_STAINED_GLASS_PANE, " ");
        for (int i = 0; i < inventory.getSize(); i++) {
            if (inventory.getItem(i) == null) inventory.setItem(i, glass);
        }
    }

    protected void click() {
        player.playSound(player.getLocation(), "ui.button.click", 0.4f, 1.2f);
    }

    protected void msg(String text) {
        player.sendMessage(Text.color(plugin.messages().prefix() + text));
    }

    /** Boton numerico: clic izq suma, clic der resta, shift multiplica por 10. */
    protected void numeric(int slot, Material mat, String label, int value, int step, int min, int max, IntConsumer setter) {
        ItemStack it = ItemUtil.make(mat, "&e" + label + ": &f" + value,
                "&7Clic izq: &a+" + step, "&7Clic der: &c-" + step, "&7Shift: &fx10");
        set(slot, it, e -> {
            int delta = e.isShiftClick() ? step * 10 : step;
            if (e.isRightClick()) delta = -delta;
            int nv = Math.max(min, Math.min(max, value + delta));
            setter.accept(nv);
            persist();
            click();
            refresh();
        });
    }

    /** Guarda los cambios hechos desde los botones numericos (los menus de loot lo sobrescriben). */
    protected void persist() {
        plugin.dungeons().save();
    }

    protected void numericD(int slot, Material mat, String label, double value, double step, double min, double max, DoubleConsumer setter) {
        ItemStack it = ItemUtil.make(mat, "&e" + label + ": &f" + String.format("%.1f", value),
                "&7Clic izq: &a+" + step, "&7Clic der: &c-" + step, "&7Shift: &fx10");
        set(slot, it, e -> {
            double delta = e.isShiftClick() ? step * 10 : step;
            if (e.isRightClick()) delta = -delta;
            double nv = Math.max(min, Math.min(max, Math.round((value + delta) * 10.0) / 10.0));
            setter.accept(nv);
            persist();
            click();
            refresh();
        });
    }

    /** Si es true se bloquean los clics en el inventario del jugador mientras el menu esta abierto. */
    public boolean cancelBottomClicks() {
        return true;
    }

    public void handleClick(InventoryClickEvent e) {
        e.setCancelled(true);
        Consumer<InventoryClickEvent> a = actions.get(e.getRawSlot());
        if (a != null) a.accept(e);
    }

    public void onClose(InventoryCloseEvent e) {
        // por defecto nada
    }

    @Override
    public Inventory getInventory() {
        return inventory;
    }
}
