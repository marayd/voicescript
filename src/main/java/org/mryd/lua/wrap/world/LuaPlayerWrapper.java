package org.mryd.lua.wrap.world;

import net.kyori.adventure.text.Component;
import org.bukkit.*;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.luaj.vm2.*;
import org.luaj.vm2.lib.VarArgFunction;
import org.mryd.lua.wrap.MainThreadFunctionWrapper;

public class LuaPlayerWrapper extends LuaTable {
    private final Player player;
    private final MainThreadFunctionWrapper wrapper = MainThreadFunctionWrapper.get();

    public LuaPlayerWrapper(Player player) {
        this.player = player;

        LuaTable meta = new LuaTable();
        meta.set("__index", new VarArgFunction() {
            @Override
            public Varargs invoke(Varargs args) {
                String k = args.arg(2).checkjstring();

                return switch (k) {
                    case "name" -> valueOf(player.getName());
                    case "uuid" -> valueOf(player.getUniqueId().toString());
                    case "health" -> valueOf(player.getHealth());
                    case "exp" -> valueOf(player.getExp());
                    case "level" -> valueOf(player.getLevel());
                    case "foodLevel" -> valueOf(player.getFoodLevel());
                    case "saturation" -> valueOf(player.getSaturation());
                    case "location" -> new LuaLocationWrapper(player.getLocation());
                    case "isFlying" -> valueOf(player.isFlying());
                    case "isSneaking" -> valueOf(player.isSneaking());
                    case "isSprinting" -> valueOf(player.isSprinting());
                    case "gamemode" -> valueOf(player.getGameMode().toString());
                    case "world" -> valueOf(player.getWorld().getName());
                    default -> NIL;
                };
            }
        });
        setmetatable(meta);

        set("sendMessage", wrapper.wrap(new VarArgFunction() {
            public Varargs invoke(Varargs args) {
                player.sendMessage(args.arg(1).tojstring());
                return NIL;
            }
        }));

        set("performCommand", wrapper.wrap(new VarArgFunction() {
            public Varargs invoke(Varargs args) {
                player.performCommand(args.arg(1).tojstring());
                return NIL;
            }
        }));

        set("setHealth", wrapper.wrap(new VarArgFunction() {
            public Varargs invoke(Varargs args) {
                player.setHealth(args.arg(1).checkdouble());
                return NIL;
            }
        }));

        set("setExp", wrapper.wrap(new VarArgFunction() {
            public Varargs invoke(Varargs args) {
                player.setExp((float) args.arg(1).checkdouble());
                return NIL;
            }
        }));

        set("setLevel", wrapper.wrap(new VarArgFunction() {
            public Varargs invoke(Varargs args) {
                player.setLevel(args.arg(1).checkint());
                return NIL;
            }
        }));

        set("setFoodLevel", wrapper.wrap(new VarArgFunction() {
            public Varargs invoke(Varargs args) {
                player.setFoodLevel(args.arg(1).checkint());
                return NIL;
            }
        }));

        set("setSaturation", wrapper.wrap(new VarArgFunction() {
            public Varargs invoke(Varargs args) {
                player.setSaturation((float) args.arg(1).checkdouble());
                return NIL;
            }
        }));

        set("setFlying", wrapper.wrap(new VarArgFunction() {
            public Varargs invoke(Varargs args) {
                player.setFlying(args.arg(1).checkboolean());
                return NIL;
            }
        }));

        set("setAllowFlight", wrapper.wrap(new VarArgFunction() {
            public Varargs invoke(Varargs args) {
                player.setAllowFlight(args.arg(1).checkboolean());
                return NIL;
            }
        }));

        set("setGameMode", wrapper.wrap(new VarArgFunction() {
            public Varargs invoke(Varargs args) {
                try {
                    GameMode mode = GameMode.valueOf(args.arg(1).checkjstring().toUpperCase());
                    player.setGameMode(mode);
                } catch (IllegalArgumentException e) {
                    return error("Invalid game mode: " + args.arg(1).tojstring());
                }
                return NIL;
            }
        }));

        set("teleport", wrapper.wrap(new VarArgFunction() {
            public Varargs invoke(Varargs args) {
                LuaValue loc = args.arg(1);
                if (loc instanceof LuaLocationWrapper luaLoc) {
                    player.teleport(luaLoc.getBukkitLocation());
                    return NIL;
                }
                return error("Expected location object");
            }
        }));

        set("clearInventory", wrapper.wrap(new VarArgFunction() {
            public Varargs invoke(Varargs args) {
                player.getInventory().clear();
                return NIL;
            }
        }));

        set("getItemInHand", wrapper.wrap(new VarArgFunction() {
            public Varargs invoke(Varargs args) {
                return valueOf(player.getInventory().getItemInMainHand().getType().toString());
            }
        }));

        set("giveItem", wrapper.wrap(new VarArgFunction() {
            public Varargs invoke(Varargs args) {
                String materialName = args.arg(1).checkjstring().toUpperCase();
                int amount = args.arg(2).checkint();
                try {
                    Material mat = Material.valueOf(materialName);
                    player.getInventory().addItem(new ItemStack(mat, amount));
                    return NIL;
                } catch (IllegalArgumentException e) {
                    return error("Invalid material: " + materialName);
                }
            }
        }));

        set("addEffect", wrapper.wrap(new VarArgFunction() {
            public Varargs invoke(Varargs args) {
                String typeName = args.arg(1).checkjstring().toUpperCase();
                int duration = args.arg(2).checkint() * 20;
                PotionEffectType type = PotionEffectType.getByName(typeName);
                if (type == null) return error("Invalid potion type");
                player.addPotionEffect(new PotionEffect(type, duration, 1));
                return NIL;
            }
        }));

        set("clearEffects", wrapper.wrap(new VarArgFunction() {
            public Varargs invoke(Varargs args) {
                player.getActivePotionEffects().forEach(effect -> player.removePotionEffect(effect.getType()));
                return NIL;
            }
        }));

        set("getAttribute", wrapper.wrap(new VarArgFunction() {
            public Varargs invoke(Varargs args) {
                String attrName = args.arg(1).checkjstring().toLowerCase();
                Attribute attr = Registry.ATTRIBUTE.get(NamespacedKey.minecraft(attrName));
                if (attr == null) return error("Invalid attribute: " + attrName);
                AttributeInstance instance = player.getAttribute(attr);
                return instance != null ? valueOf(instance.getValue()) : NIL;
            }
        }));

        set("setAttribute", wrapper.wrap(new VarArgFunction() {
            public Varargs invoke(Varargs args) {
                String attrName = args.arg(1).checkjstring().toLowerCase();
                double value = args.arg(2).checkdouble();
                Attribute attr = Registry.ATTRIBUTE.get(NamespacedKey.minecraft(attrName));
                if (attr == null) return error("Invalid attribute: " + attrName);
                AttributeInstance instance = player.getAttribute(attr);
                if (instance != null) instance.setBaseValue(value);
                return NIL;
            }
        }));

        set("playSound", wrapper.wrap(new VarArgFunction() {
            public Varargs invoke(Varargs args) {
                try {
                    Sound sound = Sound.valueOf(args.arg(1).checkjstring().toUpperCase());
                    float volume = (float) args.arg(2).checkdouble();
                    player.playSound(player.getLocation(), sound, volume, 1.0f);
                } catch (Exception e) {
                    return error("Invalid sound: " + args.arg(1).tojstring());
                }
                return NIL;
            }
        }));

        set("kick", wrapper.wrap(new VarArgFunction() {
            public Varargs invoke(Varargs args) {
                player.kick(Component.text(args.arg(1).tojstring()));
                return NIL;
            }
        }));
    }
}
