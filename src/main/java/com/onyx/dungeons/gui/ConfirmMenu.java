package com.onyx.dungeons.gui;

import com.onyx.dungeons.OnyxDungeons;
import com.onyx.dungeons.util.ItemUtil;
import org.bukkit.Material;
import org.bukkit.entity.Player;

import java.util.function.Supplier;

public class ConfirmMenu extends Menu {
    private final String question;
    private final Runnable onYes;
    private final Supplier<Menu> back;

    public ConfirmMenu(OnyxDungeons plugin, Player player, String question, Runnable onYes, Supplier<Menu> back) {
        super(plugin, player, "&4Confirmar", 3);
        this.question = question;
        this.onYes = onYes;
        this.back = back;
    }

    @Override
    protected void build() {
        set(4, ItemUtil.make(Material.PAPER, "&e" + question));
        set(11, ItemUtil.make(Material.LIME_WOOL, "&a&lSI, confirmar"), e -> {
            onYes.run();
            click();
            go(back.get());
        });
        set(15, ItemUtil.make(Material.RED_WOOL, "&c&lNO, cancelar"), e -> go(back.get()));
        fillEmpty();
    }
}
