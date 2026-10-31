package com.onyx.dungeons.util;

import org.bukkit.ChatColor;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/** Utilidades de texto: colores con '&' y reemplazo de placeholders {clave}. */
public final class Text {
    private Text() {}

    public static String color(String s) {
        return s == null ? "" : ChatColor.translateAlternateColorCodes('&', s);
    }

    public static List<String> color(List<String> list) {
        List<String> out = new ArrayList<>();
        if (list != null) for (String s : list) out.add(color(s));
        return out;
    }

    /** Reemplaza {clave} por su valor y luego aplica colores. */
    public static String apply(String s, Map<String, String> placeholders) {
        if (s == null) return "";
        if (placeholders != null) {
            for (Map.Entry<String, String> e : placeholders.entrySet()) {
                s = s.replace("{" + e.getKey() + "}", e.getValue() == null ? "" : e.getValue());
            }
        }
        return color(s);
    }

    public static String strip(String s) {
        return ChatColor.stripColor(color(s));
    }

    public static String time(int seconds) {
        if (seconds < 0) seconds = 0;
        return String.format("%02d:%02d", seconds / 60, seconds % 60);
    }
}
