package com.onyx.dungeons.command;

import com.onyx.dungeons.OnyxDungeons;
import com.onyx.dungeons.gui.KeysMenu;
import com.onyx.dungeons.gui.LootListMenu;
import com.onyx.dungeons.gui.LootTableMenu;
import com.onyx.dungeons.gui.MainMenu;
import com.onyx.dungeons.manager.WandManager.WandType;
import com.onyx.dungeons.model.Dungeon;
import com.onyx.dungeons.model.LootTable;
import com.onyx.dungeons.util.EntityUtil;
import com.onyx.dungeons.util.ItemUtil;
import com.onyx.dungeons.util.LocUtil;
import com.onyx.dungeons.util.Text;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;

public class DungeonCommand implements CommandExecutor, TabCompleter {
    private static final List<String> PLAYER_SUBS = Arrays.asList("spawn", "leave", "help");
    private static final List<String> ADMIN_SUBS = Arrays.asList("menu", "setspawn", "create", "delete", "list", "info", "start", "stop",
            "wand", "givekey", "key", "loot", "reload");

    private final OnyxDungeons plugin;

    public DungeonCommand(OnyxDungeons plugin) {
        this.plugin = plugin;
    }

    private void say(CommandSender s, String text) {
        s.sendMessage(Text.color(plugin.messages().prefix() + text));
    }

    private boolean admin(CommandSender s) {
        if (s.hasPermission("onyx.dungeons.admin")) return true;
        plugin.messages().chat(s, "no-permission");
        return false;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (args.length == 0) {
            if (sender instanceof Player p && sender.hasPermission("onyx.dungeons.admin")) {
                new MainMenu(plugin, p).open();
            } else {
                help(sender);
            }
            return true;
        }
        String sub = args[0].toLowerCase(Locale.ROOT);
        switch (sub) {
            case "help":
                help(sender);
                return true;
            case "menu": {
                if (!admin(sender)) return true;
                if (!(sender instanceof Player p)) { say(sender, "&cSolo jugadores."); return true; }
                new MainMenu(plugin, p).open();
                return true;
            }
            case "spawn": {
                if (!(sender instanceof Player p)) { say(sender, "&cSolo jugadores."); return true; }
                if (!p.hasPermission("onyx.dungeons.spawn")) { plugin.messages().chat(sender, "no-permission"); return true; }
                if (plugin.dungeons().isParticipant(p)) plugin.dungeons().leave(p);
                else plugin.dungeons().teleportLobby(p);
                plugin.messages().send(p, null, "spawn-teleported");
                return true;
            }
            case "leave": {
                if (!(sender instanceof Player p)) { say(sender, "&cSolo jugadores."); return true; }
                if (!plugin.dungeons().isParticipant(p)) plugin.messages().send(p, null, "not-in-dungeon");
                else plugin.dungeons().leave(p);
                return true;
            }
            case "setspawn": {
                if (!admin(sender)) return true;
                if (!(sender instanceof Player p)) { say(sender, "&cSolo jugadores."); return true; }
                plugin.dungeons().setLobbySpawn(p.getLocation());
                say(sender, "&aSpawn de /dungeon spawn fijado en tu posicion.");
                return true;
            }
            case "create": {
                if (!admin(sender)) return true;
                if (args.length < 2) { say(sender, "&cUso: /dungeon create <id>"); return true; }
                Dungeon d = plugin.dungeons().create(args[1]);
                if (d == null) say(sender, "&cID invalido o ya existe (usa letras, numeros, - y _).");
                else {
                    say(sender, "&aDungeon &f" + d.id + " &acreada. Abre &f/dungeon &apara configurarla.");
                    if (sender instanceof Player p) new com.onyx.dungeons.gui.DungeonMenu(plugin, p, d).open();
                }
                return true;
            }
            case "delete": {
                if (!admin(sender)) return true;
                if (args.length < 2 || plugin.dungeons().get(args[1]) == null) { say(sender, "&cDungeon no encontrada."); return true; }
                plugin.dungeons().delete(args[1]);
                say(sender, "&eDungeon eliminada.");
                return true;
            }
            case "list": {
                if (!admin(sender)) return true;
                say(sender, "&7Dungeons: &f" + (plugin.dungeons().ids().isEmpty() ? "ninguna" : String.join(", ", plugin.dungeons().ids())));
                return true;
            }
            case "info": {
                if (!admin(sender)) return true;
                Dungeon d = args.length < 2 ? null : plugin.dungeons().get(args[1]);
                if (d == null) { say(sender, "&cDungeon no encontrada."); return true; }
                say(sender, "&5&l" + Text.strip(d.displayName) + " &7(" + d.id + ")");
                sender.sendMessage(Text.color("&7Estado: &f" + d.state + "  &7Jugadores: &f" + d.minPlayers + "-" + d.maxPlayers + "  &7Cuenta: &f" + d.countdown + "s"));
                sender.sendMessage(Text.color("&7Zona: &f" + (d.lobbyZone == null ? "no definida" : d.lobbyZone.describe())));
                sender.sendMessage(Text.color("&7Teletransporte: &f" + LocUtil.pretty(d.teleport)));
                sender.sendMessage(Text.color("&7Mapa: &f" + (d.mapRegion == null ? "no definido" : d.mapRegion.describe())));
                sender.sendMessage(Text.color("&7Niveles: &f" + d.levels.size() + "  &7Ultimo: &f" + d.lastLevelNumber() + "  &7Cofres: &f" + d.chests.size()));
                return true;
            }
            case "start": {
                if (!admin(sender)) return true;
                Dungeon d = args.length < 2 ? null : plugin.dungeons().get(args[1]);
                if (d == null) { say(sender, "&cDungeon no encontrada."); return true; }
                Player p = sender instanceof Player pl ? pl : null;
                say(sender, plugin.dungeons().forceStart(d, p) ? "&aDungeon iniciada." : "&cNo se pudo iniciar (incompleta, en curso o sin jugadores en la plataforma).");
                return true;
            }
            case "stop": {
                if (!admin(sender)) return true;
                Dungeon d = args.length < 2 ? null : plugin.dungeons().get(args[1]);
                if (d == null) { say(sender, "&cDungeon no encontrada."); return true; }
                plugin.dungeons().stop(d);
                say(sender, "&eDungeon detenida y reiniciada.");
                return true;
            }
            case "wand": {
                if (!admin(sender)) return true;
                if (!(sender instanceof Player p)) { say(sender, "&cSolo jugadores."); return true; }
                if (args.length < 3) { say(sender, "&cUso: /dungeon wand <zone|map|level|door|chest> <dungeon> [nivel]"); return true; }
                WandType type;
                try {
                    type = WandType.valueOf(args[1].toUpperCase(Locale.ROOT));
                } catch (IllegalArgumentException ex) {
                    say(sender, "&cTipos: zone, map, level, door, chest");
                    return true;
                }
                Dungeon d = plugin.dungeons().get(args[2]);
                if (d == null) { say(sender, "&cDungeon no encontrada."); return true; }
                int level = 1;
                if (args.length > 3) {
                    try { level = Integer.parseInt(args[3]); } catch (NumberFormatException ex) { say(sender, "&cNivel invalido."); return true; }
                }
                if (type == WandType.CHEST && plugin.wands().context(p).table == null) {
                    say(sender, "&cPrimero elige una tabla en el menu de cofres (o usa /dungeon loot edit).");
                    return true;
                }
                plugin.wands().give(p, type, d.id, level, null);
                return true;
            }
            case "givekey": {
                if (!admin(sender)) return true;
                if (args.length < 3) { say(sender, "&cUso: /dungeon givekey <jugador> <llave> [cantidad]"); return true; }
                Player target = Bukkit.getPlayerExact(args[1]);
                if (target == null) { say(sender, "&cJugador no conectado."); return true; }
                int amount = 1;
                if (args.length > 3) {
                    try { amount = Math.max(1, Integer.parseInt(args[3])); } catch (NumberFormatException ignored) { amount = 1; }
                }
                ItemStack key = plugin.keys().create(args[2], amount);
                if (key == null) { say(sender, "&cEsa llave no existe."); return true; }
                EntityUtil.give(target, key);
                say(sender, "&aLlave entregada a &f" + target.getName() + "&a.");
                return true;
            }
            case "key": {
                if (!admin(sender)) return true;
                if (args.length >= 2 && args[1].equalsIgnoreCase("list")) {
                    say(sender, "&7Llaves: &f" + (plugin.keys().ids().isEmpty() ? "ninguna" : String.join(", ", plugin.keys().ids())));
                    return true;
                }
                if (args.length < 3) { say(sender, "&cUso: /dungeon key <create|delete|list> <id>"); return true; }
                if (args[1].equalsIgnoreCase("create")) {
                    if (!(sender instanceof Player p)) { say(sender, "&cSolo jugadores."); return true; }
                    ItemStack hand = p.getInventory().getItemInMainHand();
                    if (ItemUtil.isAir(hand)) { say(sender, "&cTen en la mano el item que sera la llave."); return true; }
                    if (!com.onyx.dungeons.manager.DungeonManager.ID_PATTERN.matcher(args[2]).matches()) { say(sender, "&cID invalido."); return true; }
                    plugin.keys().define(args[2], hand);
                    say(sender, "&aLlave &f" + args[2].toLowerCase() + " &acreada con el item de tu mano.");
                } else if (args[1].equalsIgnoreCase("delete")) {
                    plugin.keys().delete(args[2]);
                    say(sender, "&eLlave eliminada.");
                }
                return true;
            }
            case "loot": {
                if (!admin(sender)) return true;
                if (args.length >= 2 && args[1].equalsIgnoreCase("list")) {
                    say(sender, "&7Tablas: &f" + (plugin.loot().ids().isEmpty() ? "ninguna" : String.join(", ", plugin.loot().ids())));
                    return true;
                }
                if (args.length < 3) { say(sender, "&cUso: /dungeon loot <create|delete|edit|list> <id>"); return true; }
                String action = args[1].toLowerCase(Locale.ROOT);
                if (action.equals("create")) {
                    if (!com.onyx.dungeons.manager.DungeonManager.ID_PATTERN.matcher(args[2]).matches() || plugin.loot().get(args[2]) != null) {
                        say(sender, "&cID invalido o ya existe.");
                        return true;
                    }
                    LootTable t = plugin.loot().create(args[2]);
                    if (sender instanceof Player p) new LootTableMenu(plugin, p, t, () -> new LootListMenu(plugin, p, null, () -> new MainMenu(plugin, p))).open();
                } else if (action.equals("delete")) {
                    plugin.loot().delete(args[2]);
                    say(sender, "&eTabla eliminada.");
                } else if (action.equals("edit")) {
                    LootTable t = plugin.loot().get(args[2]);
                    if (t == null) { say(sender, "&cTabla no encontrada."); return true; }
                    if (sender instanceof Player p) {
                        plugin.wands().context(p).table = t.id;
                        new LootTableMenu(plugin, p, t, () -> new LootListMenu(plugin, p, null, () -> new MainMenu(plugin, p))).open();
                    }
                }
                return true;
            }
            case "reload": {
                if (!admin(sender)) return true;
                plugin.reloadAll();
                say(sender, "&aConfiguracion recargada.");
                return true;
            }
            default:
                help(sender);
                return true;
        }
    }

    private void help(CommandSender s) {
        say(s, "&5&lOnyx Dungeons");
        s.sendMessage(Text.color("&f/dungeon spawn &7- ir al lobby (si estas en una dungeon, la abandonas)"));
        s.sendMessage(Text.color("&f/dungeon leave &7- abandonar la dungeon"));
        if (s.hasPermission("onyx.dungeons.admin")) {
            s.sendMessage(Text.color("&f/dungeon &7- abrir el menu de administracion"));
            s.sendMessage(Text.color("&f/dungeon create|delete|list|info <id> &7- gestionar dungeons"));
            s.sendMessage(Text.color("&f/dungeon start|stop <id> &7- iniciar o detener"));
            s.sendMessage(Text.color("&f/dungeon wand <zone|map|level|door|chest> <dungeon> [nivel]"));
            s.sendMessage(Text.color("&f/dungeon key <create|delete|list> <id> &7y &f/dungeon givekey <jugador> <llave> [n]"));
            s.sendMessage(Text.color("&f/dungeon loot <create|delete|edit|list> <id>"));
            s.sendMessage(Text.color("&f/dungeon setspawn &7- fijar el spawn del lobby"));
            s.sendMessage(Text.color("&f/dungeon reload"));
        }
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        List<String> out = new ArrayList<>();
        boolean isAdmin = sender.hasPermission("onyx.dungeons.admin");
        if (args.length == 1) {
            out.addAll(PLAYER_SUBS);
            if (isAdmin) out.addAll(ADMIN_SUBS);
            return filter(out, args[0]);
        }
        if (!isAdmin) return out;
        String sub = args[0].toLowerCase(Locale.ROOT);
        if (args.length == 2) {
            switch (sub) {
                case "delete": case "info": case "start": case "stop":
                    out.addAll(plugin.dungeons().ids());
                    break;
                case "wand":
                    for (WandType t : WandType.values()) out.add(t.name().toLowerCase(Locale.ROOT));
                    break;
                case "givekey":
                    for (Player p : Bukkit.getOnlinePlayers()) out.add(p.getName());
                    break;
                case "key":
                    out.addAll(Arrays.asList("create", "delete", "list"));
                    break;
                case "loot":
                    out.addAll(Arrays.asList("create", "delete", "edit", "list"));
                    break;
                default:
                    break;
            }
        } else if (args.length == 3) {
            switch (sub) {
                case "wand":
                    out.addAll(plugin.dungeons().ids());
                    break;
                case "givekey":
                    out.addAll(plugin.keys().ids());
                    break;
                case "key":
                    if (args[1].equalsIgnoreCase("delete")) out.addAll(plugin.keys().ids());
                    break;
                case "loot":
                    if (args[1].equalsIgnoreCase("delete") || args[1].equalsIgnoreCase("edit")) out.addAll(plugin.loot().ids());
                    break;
                default:
                    break;
            }
        }
        return filter(out, args[args.length - 1]);
    }

    private List<String> filter(List<String> in, String prefix) {
        List<String> out = new ArrayList<>();
        String p = prefix.toLowerCase(Locale.ROOT);
        for (String s : in) if (s.toLowerCase(Locale.ROOT).startsWith(p)) out.add(s);
        return out;
    }
}
