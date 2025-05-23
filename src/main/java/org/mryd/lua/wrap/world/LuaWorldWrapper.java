package org.mryd.lua.wrap.world;

import org.bukkit.World;
import org.luaj.vm2.*;
import org.luaj.vm2.lib.ZeroArgFunction;

public class LuaWorldWrapper extends LuaTable {
    private final World world;

    public LuaWorldWrapper(World world) {
        this.world = world;

        set("getName", new ZeroArgFunction() {
            public LuaValue call() {
                return LuaValue.valueOf(world.getName());
            }
        });

        set("getSeed", new ZeroArgFunction() {
            public LuaValue call() {
                return LuaValue.valueOf(world.getSeed());
            }
        });

        set("getTime", new ZeroArgFunction() {
            public LuaValue call() {
                return LuaValue.valueOf(world.getTime());
            }
        });
    }
}
