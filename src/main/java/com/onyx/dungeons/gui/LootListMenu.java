package com.onyx.dungeons.gui;

import com.onyx.dungeons.OnyxDungeons;
import com.onyx.dungeons.manager.DungeonManager;
import com.onyx.dungeons.model.LootTable;
import com.onyx.dungeons.util.ItemUtil;
import org.bukkit.Material;
import org.bukkit.entity.Player;

import java.util.function.Consumer;
import java.util.function.Supplier;

/** Lista de tablas de loot. Si onSelect != null funciona como selector. */
public class LootListMenu extends Menu {
    private final Consumer<String> onSelect;
    private final Supplier<Menu> back;

    public LootListMenu(OnyxDungeons plugin, Player player, Consumer<String> onSelect, Supplier<Menu> back) {
        super(plugin, player, onSelect == null ? "&6Tablas de recompensas" : "&6Elige una tabla", 6);
        this.onSelect = onSelect;
        this.back = back;
    }

    @Override
    protected void build() {
        int i = 0;
        for (String id : plugin.loot().ids()) {
            if (i >= 45) break;
            LootTable t = plugin.loot().get(id);
            set(i++, ItemUtil.make(Material.CHEST, "&6" + id,
                    "&7Items: &f" + t.entries.size(),
                    "&7Items por cofre: &f" + t.minRolls + "-" + t.maxRolls,
                    "&7Sin repetir: &f" + (t.unique ? "si" : "no"),
                    "", onSelect == null ? "&aClic izq: &7editar" : "&aClic: &7seleccionar",
                    onSelect == null ? "&cShift + clic der: &7eliminar" : ""), e -> {
                if (onSelect != null) {
                    onSelect.accept(id);
                    go(back.get());
                } else if (e.isShiftClick() && e.isRightClick()) {
                    go(new ConfirmMenu(plugin, player, "Eliminar la tabla " + id + "?", () -> plugin.loot().delete(id),
                            () -> new LootListMenu(plugin, player, null, back)));
                } else {
                    go(new LootTableMenu(plugin, player, t, () -> new LootListMenu(plugin, player, null, back)));
                }
            });
        }
        set(49, ItemUtil.make(Material.EMERALD, "&a&lCrear tabla", "&7Crea una nueva tabla de recompensas."), e ->
                plugin.prompts().ask(player, "&eEscribe el ID de la nueva tabla (ej: &fcofre_nivel1&e):", text -> {
                    String id = text.trim();
                    if (!DungeonManager.ID_PATTERN.matcher(id).matches() || plugin.loot().get(id) != null) {
                        msg("&cID invalido o ya existe.");
                        new LootListMenu(plugin, player, onSelect, back).open();
                        return;
                    }
                    LootTable t = plugin.loot().create(id);
                    new LootTableMenu(plugin, player, t, () -> new LootListMenu(plugin, player, onSelect, back)).open();
                }));
        set(45, ItemUtil.make(Material.ARROW, "&7Volver"), e -> go(back.get()));
        fillEmpty();
    }
}
