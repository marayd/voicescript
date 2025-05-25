package org.mryd.lua.wrap;

import net.kyori.adventure.text.Component;
import org.bukkit.*;
import org.bukkit.entity.Player;
import org.luaj.vm2.*;
import org.luaj.vm2.lib.VarArgFunction;
import org.mryd.lua.wrap.world.LuaLocationWrapper;
import org.mryd.lua.wrap.world.LuaPlayerWrapper;
import org.mryd.lua.wrap.world.LuaWorldWrapper;

import java.util.UUID;

public class LuaServerWrapper extends LuaTable {

    public LuaServerWrapper() {
        Server server = Bukkit.getServer();
        MainThreadFunctionWrapper wrapper = MainThreadFunctionWrapper.get();

        // Get player by name or UUID
        set("getPlayer", wrapper.wrap(new VarArgFunction() {
            @Override
            public Varargs invoke(Varargs args) {
                if (args.narg() < 1) return NIL;

                LuaValue value = args.arg(1);
                Player player = null;

                if (value.isstring()) {
                    String input = value.tojstring();
                    try {
                        UUID uuid = UUID.fromString(input);
                        player = server.getPlayer(uuid);
                    } catch (IllegalArgumentException e) {
                        player = server.getPlayer(input);
                    }
                }

                return (player != null) ? new LuaPlayerWrapper(player) : NIL;
            }
        }));

        // Create location object by (x, y, z, world)
        set("getLocation", wrapper.wrap(new VarArgFunction() {
            @Override
            public Varargs invoke(Varargs args) {
                if (args.narg() < 4) return NIL;

                double x = args.arg(1).todouble();
                double y = args.arg(2).todouble();
                double z = args.arg(3).todouble();
                String worldName = args.arg(4).tojstring();

                World world = server.getWorld(worldName);
                if (world == null) return NIL;

                return new LuaLocationWrapper(new Location(world, x, y, z));
            }
        }));

        // Get world by name
        set("getWorld", wrapper.wrap(new VarArgFunction() {
            @Override
            public Varargs invoke(Varargs args) {
                if (args.narg() < 1) return NIL;

                String name = args.arg(1).tojstring();
                World world = server.getWorld(name);

                return (world != null) ? new LuaWorldWrapper(world) : NIL;
            }
        }));

        // Broadcast message
        set("broadcast", wrapper.wrap(new VarArgFunction() {
            @Override
            public Varargs invoke(Varargs args) {
                if (args.narg() < 1) return NIL;

                String message = args.arg(1).tojstring();
                Bukkit.broadcast(Component.text(message));
                return NIL;
            }
        }));

        // Execute console command
        set("execute", wrapper.wrap(new VarArgFunction() {
            @Override
            public Varargs invoke(Varargs args) {
                if (args.narg() < 1) return NIL;

                String command = args.arg(1).tojstring();
                Bukkit.dispatchCommand(Bukkit.getConsoleSender(), command);
                return NIL;
            }
        }));
    }
}
