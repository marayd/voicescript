package org.mryd.lua.wrap.world;

import net.kyori.adventure.text.Component;
import org.bukkit.*;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.luaj.vm2.LuaTable;
import org.luaj.vm2.LuaValue;
import org.luaj.vm2.lib.OneArgFunction;
import org.luaj.vm2.lib.TwoArgFunction;
import org.luaj.vm2.lib.ZeroArgFunction;

public class LuaPlayerWrapper extends LuaTable {
    private final Player player;

    public LuaPlayerWrapper(Player player) {
        this.player = player;

        LuaTable meta = new LuaTable();
        meta.set("__index", new TwoArgFunction() {
            @Override
            public LuaValue call(LuaValue table, LuaValue key) {
                String k = key.checkjstring();
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

        // Messaging
        set("sendMessage", new OneArgFunction() {
            public LuaValue call(LuaValue msg) {
                player.sendMessage(msg.tojstring());
                return NIL;
            }
        });

        // Commands
        set("performCommand", new OneArgFunction() {
            public LuaValue call(LuaValue cmd) {
                player.performCommand(cmd.tojstring());
                return NIL;
            }
        });

        // Health
        set("setHealth", new OneArgFunction() {
            public LuaValue call(LuaValue val) {
                player.setHealth(val.checkdouble());
                return NIL;
            }
        });

        // Experience
        set("setExp", new OneArgFunction() {
            public LuaValue call(LuaValue val) {
                player.setExp((float) val.checkdouble());
                return NIL;
            }
        });

        set("setLevel", new OneArgFunction() {
            public LuaValue call(LuaValue val) {
                player.setLevel(val.checkint());
                return NIL;
            }
        });

        // Food
        set("setFoodLevel", new OneArgFunction() {
            public LuaValue call(LuaValue val) {
                player.setFoodLevel(val.checkint());
                return NIL;
            }
        });

        set("setSaturation", new OneArgFunction() {
            public LuaValue call(LuaValue val) {
                player.setSaturation((float) val.checkdouble());
                return NIL;
            }
        });

        // Flying
        set("setFlying", new OneArgFunction() {
            public LuaValue call(LuaValue val) {
                player.setFlying(val.checkboolean());
                return NIL;
            }
        });

        set("setAllowFlight", new OneArgFunction() {
            public LuaValue call(LuaValue val) {
                player.setAllowFlight(val.checkboolean());
                return NIL;
            }
        });

        // GameMode
        set("setGameMode", new OneArgFunction() {
            public LuaValue call(LuaValue val) {
                try {
                    GameMode mode = GameMode.valueOf(val.checkjstring().toUpperCase());
                    player.setGameMode(mode);
                } catch (IllegalArgumentException e) {
                    return error("Invalid game mode: " + val.tojstring());
                }
                return NIL;
            }
        });

        // Teleportation
        set("teleport", new OneArgFunction() {
            public LuaValue call(LuaValue loc) {
                if (loc instanceof LuaLocationWrapper luaLoc) {
                    player.teleport(luaLoc.getBukkitLocation());
                    return NIL;
                }
                return error("Expected location object");
            }
        });

        // Inventory
        set("clearInventory", new ZeroArgFunction() {
            public LuaValue call() {
                player.getInventory().clear();
                return NIL;
            }
        });

        set("getItemInHand", new ZeroArgFunction() {
            public LuaValue call() {
                return valueOf(player.getInventory().getItemInMainHand().getType().toString());
            }
        });

        set("giveItem", new TwoArgFunction() {
            public LuaValue call(LuaValue materialName, LuaValue amount) {
                try {
                    Material mat = Material.valueOf(materialName.checkjstring().toUpperCase());
                    player.getInventory().addItem(new ItemStack(mat, amount.checkint()));
                    return NIL;
                } catch (IllegalArgumentException e) {
                    return error("Invalid material: " + materialName.tojstring());
                }
            }
        });

        // Potion effects
        set("addEffect", new TwoArgFunction() {
            public LuaValue call(LuaValue name, LuaValue seconds) {
                try {
                    PotionEffectType type = PotionEffectType.getByName(name.checkjstring().toUpperCase());
                    if (type == null) return error("Invalid potion type");
                    player.addPotionEffect(new PotionEffect(type, seconds.checkint() * 20, 1));
                    return NIL;
                } catch (Exception e) {
                    return error("Failed to add potion effect: " + e.getMessage());
                }
            }
        });

        set("clearEffects", new ZeroArgFunction() {
            public LuaValue call() {
                player.getActivePotionEffects().forEach(effect -> player.removePotionEffect(effect.getType()));
                return NIL;
            }
        });

        // Attribute API (e.g., speed, attack damage, etc.)
        set("getAttribute", new OneArgFunction() {
            public LuaValue call(LuaValue attrName) {
                Attribute attr = Registry.ATTRIBUTE.get(NamespacedKey.minecraft(attrName.checkjstring().toLowerCase()));
                if (attr == null) return error("Invalid attribute: " + attrName.tojstring());

                AttributeInstance instance = player.getAttribute(attr);
                return instance != null ? valueOf(instance.getValue()) : NIL;
            }
        });

        set("setAttribute", new TwoArgFunction() {
            public LuaValue call(LuaValue attrName, LuaValue val) {
                Attribute attr = Registry.ATTRIBUTE.get(NamespacedKey.minecraft(attrName.checkjstring().toLowerCase()));
                if (attr == null) return error("Invalid attribute: " + attrName.tojstring());

                AttributeInstance instance = player.getAttribute(attr);
                if (instance != null) {
                    instance.setBaseValue(val.checkdouble());
                }
                return NIL;
            }
        });

        // Sound
        set("playSound", new TwoArgFunction() {
            public LuaValue call(LuaValue name, LuaValue volume) {
                try {
                    Sound sound = Sound.valueOf(name.checkjstring().toUpperCase());
                    player.playSound(player.getLocation(), sound, (float) volume.checkdouble(), 1.0f);
                } catch (Exception e) {
                    return error("Invalid sound: " + name.tojstring());
                }
                return NIL;
            }
        });

        // Utilities
        set("kick", new OneArgFunction() {
            public LuaValue call(LuaValue reason) {
                player.kick(Component.text(reason.tojstring()));
                return NIL;
            }
        });
    }
}
