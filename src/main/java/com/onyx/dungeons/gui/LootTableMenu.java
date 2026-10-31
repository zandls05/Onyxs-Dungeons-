package com.onyx.dungeons.gui;

import com.onyx.dungeons.OnyxDungeons;
import com.onyx.dungeons.model.LootEntry;
import com.onyx.dungeons.model.LootTable;
import com.onyx.dungeons.util.ItemUtil;
import com.onyx.dungeons.util.Text;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemStack;

import java.util.Arrays;
import java.util.List;
import java.util.function.Supplier;

/**
 * Editor de una tabla. Para anadir items: pon un item en el cursor y haz clic en la parte de arriba,
 * o usa el boton "Anadir item de mi mano". Clic en un item = editar su probabilidad y cantidad.
 */
public class LootTableMenu extends Menu {
    private final LootTable table;
    private final Supplier<Menu> back;

    public LootTableMenu(OnyxDungeons plugin, Player player, LootTable table, Supplier<Menu> back) {
        super(plugin, player, "&6Tabla: " + table.id, 6);
        this.table = table;
        this.back = back;
    }

    @Override
    public boolean cancelBottomClicks() {
        return false;   // permite coger items de tu inventario para soltarlos arriba
    }

    @Override
    protected void persist() {
        plugin.loot().save();
    }

    @Override
    public void handleClick(InventoryClickEvent e) {
        ItemStack cursor = e.getCursor();
        if (e.getRawSlot() < 45 && !ItemUtil.isAir(cursor)) {
            e.setCancelled(true);
            if (table.entries.size() >= 45) {
                msg("&cMaximo 45 items por tabla.");
                return;
            }
            table.entries.add(new LootEntry(cursor));
            plugin.loot().save();
            click();
            refresh();
            return;
        }
        super.handleClick(e);
    }

    @Override
    protected void build() {
        for (int i = 0; i < table.entries.size() && i < 45; i++) {
            LootEntry en = table.entries.get(i);
            List<String> extra = Arrays.asList("&8-----------------",
                    "&7Probabilidad: &e" + String.format("%.1f", en.chance) + "%",
                    "&7Cantidad: &e" + en.min + " - " + en.max,
                    "&aClic: &7editar", "&cShift + clic der: &7quitar");
            set(i, ItemUtil.withExtraLore(en.item, extra), e -> {
                if (e.isShiftClick() && e.isRightClick()) {
                    table.entries.remove(en);
                    plugin.loot().save();
                    refresh();
                } else {
                    go(new LootEntryMenu(plugin, player, table, en, () -> new LootTableMenu(plugin, player, table, back)));
                }
            });
        }
        set(45, ItemUtil.make(Material.HOPPER, "&a&lAnadir item de mi mano", "&7Agrega el item que tienes en la mano.",
                "&8Tambien puedes soltar un item desde el cursor."), e -> {
            ItemStack hand = player.getInventory().getItemInMainHand();
            if (ItemUtil.isAir(hand)) {
                msg("&cTen un item en la mano.");
                return;
            }
            if (table.entries.size() >= 45) {
                msg("&cMaximo 45 items por tabla.");
                return;
            }
            table.entries.add(new LootEntry(hand));
            plugin.loot().save();
            refresh();
        });
        numeric(46, Material.PAPER, "Items minimos por cofre", table.minRolls, 1, 0, 45, v -> {
            table.minRolls = v;
            if (table.maxRolls < v) table.maxRolls = v;
        });
        numeric(47, Material.PAPER, "Items maximos por cofre", table.maxRolls, 1, 0, 45, v -> {
            table.maxRolls = v;
            if (table.minRolls > v) table.minRolls = v;
        });
        set(48, ItemUtil.make(table.unique ? Material.LIME_DYE : Material.GRAY_DYE,
                "&eSin repetir items: " + (table.unique ? "&aSI" : "&cNO"),
                "&7Si esta activo, cada item aparece como maximo", "&7una vez por cofre (aleatorio y sin duplicados)."), e -> {
            table.unique = !table.unique;
            plugin.loot().save();
            refresh();
        });
        set(50, ItemUtil.make(Material.ENDER_EYE, "&bProbar tirada", "&7Muestra en el chat que saldria en un cofre."), e -> {
            List<ItemStack> r = plugin.dungeons().previewRoll(table);
            msg("&bTirada de prueba (&f" + r.size() + " &bitems):");
            for (ItemStack it : r) player.sendMessage(Text.color("  &7- &f" + it.getAmount() + "x " + ItemUtil.niceName(it)));
        });
        set(53, ItemUtil.make(Material.BOOK, "&7Como funciona",
                "&7Cada item tiene una probabilidad (%).", "&7Los que pasan la tirada entran a un pool,",
                "&7se mezcla y se toman entre min y max", "&7items distintos para cada cofre."));
        set(49, ItemUtil.make(Material.ARROW, "&7Volver"), e -> go(back.get()));
        fillBottomRow();
    }

    private void fillBottomRow() {
        ItemStack glass = ItemUtil.make(Material.GRAY_STAINED_GLASS_PANE, " ");
        for (int i = 45; i < 54; i++) if (inventory.getItem(i) == null) inventory.setItem(i, glass);
    }
}
