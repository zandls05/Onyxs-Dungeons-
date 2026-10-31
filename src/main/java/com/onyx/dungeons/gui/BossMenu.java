package com.onyx.dungeons.gui;

import com.onyx.dungeons.OnyxDungeons;
import com.onyx.dungeons.model.BossDef;
import com.onyx.dungeons.model.Dungeon;
import com.onyx.dungeons.model.Level;
import com.onyx.dungeons.util.ItemUtil;
import com.onyx.dungeons.util.LocUtil;
import org.bukkit.Material;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;

public class BossMenu extends Menu {
    private static final List<EntityType> TYPES = new ArrayList<>();

    static {
        String[] names = {"ZOMBIE", "SKELETON", "WITHER_SKELETON", "HUSK", "STRAY", "SPIDER", "CAVE_SPIDER", "CREEPER", "ENDERMAN",
                "BLAZE", "PIGLIN_BRUTE", "VINDICATOR", "EVOKER", "RAVAGER", "IRON_GOLEM", "WITCH", "WARDEN", "SLIME",
                "MAGMA_CUBE", "HOGLIN", "ZOGLIN", "PHANTOM", "DROWNED", "GUARDIAN", "SHULKER"};
        for (String n : names) {
            try {
                TYPES.add(EntityType.valueOf(n));
            } catch (IllegalArgumentException ignored) {
                // no existe en esta version
            }
        }
    }

    private final Dungeon d;
    private final Level lv;
    private final BossDef b;

    public BossMenu(OnyxDungeons plugin, Player player, Dungeon d, Level lv, BossDef b) {
        super(plugin, player, "&cJefe " + b.id + " - Nivel " + lv.number, 4);
        this.d = d;
        this.lv = lv;
        this.b = b;
    }

    @Override
    protected void build() {
        boolean external = b.tag != null && !b.tag.isEmpty();
        if (!external) {
            set(10, ItemUtil.make(BossListMenu.eggOf(b), "&eTipo: &f" + b.type.name(), "&7Clic izq: siguiente", "&7Clic der: anterior"), e -> {
                int i = TYPES.indexOf(b.type);
                i = e.isRightClick() ? i - 1 : i + 1;
                if (i < 0) i = TYPES.size() - 1;
                if (i >= TYPES.size()) i = 0;
                b.type = TYPES.get(i);
                plugin.dungeons().save();
                refresh();
            });
            numeric(12, Material.GOLDEN_APPLE, "Vida", (int) b.health, 20, 1, 100000, v -> b.health = v);
            numeric(13, Material.IRON_SWORD, "Dano (0 = normal)", (int) b.damage, 2, 0, 1000, v -> b.damage = v);
            set(14, ItemUtil.make(Material.ENDER_PEARL, "&bPunto de aparicion", "&7" + LocUtil.pretty(b.spawn),
                    "", "&aClic izq: &7fijar aqui", "&aClic der: &7ir alli"), e -> {
                if (e.isRightClick()) {
                    if (b.spawn != null) plugin.dungeons().teleportPlugin(player, b.spawn);
                } else {
                    b.spawn = player.getLocation();
                    plugin.dungeons().save();
                    refresh();
                }
            });
        } else {
            set(10, ItemUtil.make(Material.NAME_TAG, "&bTag externo", "&7" + b.tag, "", "&eClic para cambiar"), e ->
                    plugin.prompts().ask(player, "&eEscribe el nuevo tag:", text -> {
                        b.tag = text.trim();
                        plugin.dungeons().save();
                        new BossMenu(plugin, player, d, lv, b).open();
                    }));
        }
        set(11, ItemUtil.make(Material.WRITABLE_BOOK, "&eNombre", "&7" + b.name, "", "&eClic para cambiar"), e ->
                plugin.prompts().ask(player, "&eEscribe el nombre del jefe (colores con &&):", text -> {
                    b.name = text;
                    plugin.dungeons().save();
                    new BossMenu(plugin, player, d, lv, b).open();
                }));
        set(16, ItemUtil.make(Material.TNT, "&4Eliminar jefe"), e ->
                go(new ConfirmMenu(plugin, player, "Eliminar el jefe " + b.id + "?", () -> {
                    lv.bosses.remove(b);
                    plugin.dungeons().save();
                }, () -> new BossListMenu(plugin, player, d, lv))));
        set(31, ItemUtil.make(Material.ARROW, "&7Volver"), e -> go(new BossListMenu(plugin, player, d, lv)));
        fillEmpty();
    }
}
