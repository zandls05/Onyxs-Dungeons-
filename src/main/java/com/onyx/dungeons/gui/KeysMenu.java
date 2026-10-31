package com.onyx.dungeons.gui;

import com.onyx.dungeons.OnyxDungeons;
import com.onyx.dungeons.manager.DungeonManager;
import com.onyx.dungeons.util.EntityUtil;
import com.onyx.dungeons.util.ItemUtil;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.function.Consumer;
import java.util.function.Supplier;

/** Gestion de llaves. Si onSelect != null funciona como selector. */
public class KeysMenu extends Menu {
    private final Consumer<String> onSelect;
    private final Supplier<Menu> back;

    public KeysMenu(OnyxDungeons plugin, Player player, Consumer<String> onSelect, Supplier<Menu> back) {
        super(plugin, player, onSelect == null ? "&eLlaves" : "&eElige una llave", 6);
        this.onSelect = onSelect;
        this.back = back;
    }

    @Override
    protected void build() {
        int i = 0;
        for (String id : plugin.keys().ids()) {
            if (i >= 45) break;
            ItemStack icon = plugin.keys().template(id);
            if (icon == null) continue;
            icon = ItemUtil.withExtraLore(icon, onSelect == null
                    ? java.util.Arrays.asList("&8-----------------", "&7ID: &f" + id, "&aClic izq: &7darme la llave",
                    "&eShift + clic izq: &7reemplazar por el item de mi mano", "&cShift + clic der: &7eliminar")
                    : java.util.Arrays.asList("&8-----------------", "&7ID: &f" + id, "&aClic: &7seleccionar"));
            set(i++, icon, e -> {
                if (onSelect != null) {
                    onSelect.accept(id);
                    go(back.get());
                } else if (e.isShiftClick() && e.isRightClick()) {
                    go(new ConfirmMenu(plugin, player, "Eliminar la llave " + id + "?", () -> plugin.keys().delete(id),
                            () -> new KeysMenu(plugin, player, null, back)));
                } else if (e.isShiftClick()) {
                    ItemStack hand = player.getInventory().getItemInMainHand();
                    if (ItemUtil.isAir(hand)) {
                        msg("&cTen un item en la mano.");
                        return;
                    }
                    plugin.keys().define(id, hand);
                    msg("&aLlave &f" + id + " &aactualizada.");
                    refresh();
                } else {
                    EntityUtil.give(player, plugin.keys().create(id, 1));
                }
            });
        }
        if (onSelect != null) {
            set(48, ItemUtil.make(Material.BARRIER, "&cSin llave"), e -> {
                onSelect.accept(null);
                go(back.get());
            });
        }
        set(49, ItemUtil.make(Material.EMERALD, "&a&lCrear llave desde mi mano",
                "&7Sosten el item que quieres usar como llave", "&7(cualquier item, con nombre y lore propios)."), e -> {
            ItemStack hand = player.getInventory().getItemInMainHand();
            if (ItemUtil.isAir(hand)) {
                msg("&cPrimero ten en la mano el item que sera la llave.");
                return;
            }
            final ItemStack template = hand.clone();
            plugin.prompts().ask(player, "&eEscribe el ID de la llave (ej: &fllave_nivel1&e):", text -> {
                String id = text.trim().toLowerCase();
                if (!DungeonManager.ID_PATTERN.matcher(id).matches() || plugin.keys().exists(id)) {
                    msg("&cID invalido o ya existe.");
                } else {
                    plugin.keys().define(id, template);
                    msg("&aLlave &f" + id + " &acreada.");
                }
                new KeysMenu(plugin, player, onSelect, back).open();
            });
        });
        set(45, ItemUtil.make(Material.ARROW, "&7Volver"), e -> go(back.get()));
        fillEmpty();
    }
}
