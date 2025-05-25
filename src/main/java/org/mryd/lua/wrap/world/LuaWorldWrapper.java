package org.mryd.lua.wrap.world;

import org.bukkit.Bukkit;
import org.bukkit.World;
import org.luaj.vm2.*;
import org.luaj.vm2.lib.VarArgFunction;
import org.mryd.lua.wrap.MainThreadFunctionWrapper;
import org.bukkit.plugin.java.JavaPlugin;

public class LuaWorldWrapper extends LuaTable {
    private final World world;

    public LuaWorldWrapper(World world) {
        this.world = world;
        MainThreadFunctionWrapper wrapper = new MainThreadFunctionWrapper();

        set("getName", wrapper.wrap(new VarArgFunction() {
            @Override
            public Varargs invoke(Varargs args) {
                return LuaValue.valueOf(world.getName());
            }
        }));

        set("getSeed", wrapper.wrap(new VarArgFunction() {
            @Override
            public Varargs invoke(Varargs args) {
                return LuaValue.valueOf(world.getSeed());
            }
        }));

        set("getTime", wrapper.wrap(new VarArgFunction() {
            @Override
            public Varargs invoke(Varargs args) {
                return LuaValue.valueOf(world.getTime());
            }
        }));
    }
}
