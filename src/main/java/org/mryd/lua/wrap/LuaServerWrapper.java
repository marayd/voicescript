package org.mryd.lua.wrap;

import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Server;
import org.bukkit.World;
import org.bukkit.entity.Player;

import org.luaj.vm2.*;
import org.luaj.vm2.lib.OneArgFunction;
import org.luaj.vm2.lib.VarArgFunction;
import org.mryd.lua.wrap.world.LuaLocationWrapper;
import org.mryd.lua.wrap.world.LuaPlayerWrapper;
import org.mryd.lua.wrap.world.LuaWorldWrapper;

import java.util.UUID;

public class LuaServerWrapper extends LuaTable {

    public LuaServerWrapper() {
        Server server = Bukkit.getServer();

        // getPlayer(name or UUID)
        set("getPlayer", new VarArgFunction() {
            @Override
            public Varargs invoke(Varargs args) {
                if (args.narg() < 1) return NIL;
                LuaValue arg = args.arg(1);
                Player player = null;

                if (arg.isstring()) {
                    String str = arg.tojstring();
                    try {
                        UUID uuid = UUID.fromString(str);
                        player = server.getPlayer(uuid);
                    } catch (IllegalArgumentException e) {
                        player = server.getPlayer(str);
                    }
                }

                if (player == null) return NIL;
                return new LuaPlayerWrapper(player);
            }
        });

        // getLocation(x, y, z, worldName)
        set("getLocation", new VarArgFunction() {
            @Override
            public Varargs invoke(Varargs args) {
                if (args.narg() < 4) return NIL;

                double x = args.arg(1).todouble();
                double y = args.arg(2).todouble();
                double z = args.arg(3).todouble();
                String worldName = args.arg(4).tojstring();

                World world = server.getWorld(worldName);
                if (world == null) return NIL;

                Location loc = new Location(world, x, y, z);
                return new LuaLocationWrapper(loc);
            }
        });

        // getWorld(name)
        set("getWorld", new OneArgFunction() {
            @Override
            public LuaValue call(LuaValue arg) {
                World world = server.getWorld(arg.tojstring());
                if (world == null) return NIL;
                return new LuaWorldWrapper(world);
            }
        });

        set("broadcast", new OneArgFunction() {
            @Override
            public LuaValue call(LuaValue arg) {
                Bukkit.broadcast(Component.text(arg.tojstring()));
                return NIL;
            }
        });

        set("execute", new OneArgFunction() {
            @Override
            public LuaValue call(LuaValue arg) {
                Bukkit.dispatchCommand(Bukkit.getConsoleSender(), arg.tojstring());
                return NIL;
            }
        });
    }
}
