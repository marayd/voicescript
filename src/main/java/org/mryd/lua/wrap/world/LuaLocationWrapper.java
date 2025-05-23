package org.mryd.lua.wrap.world;

import org.bukkit.Location;
import org.luaj.vm2.*;
import org.luaj.vm2.lib.TwoArgFunction;

public class LuaLocationWrapper extends LuaTable {
    private final Location location;

    public LuaLocationWrapper(Location location) {
        this.location = location;

        LuaTable meta = new LuaTable();
        meta.set("__index", new TwoArgFunction() {
            @Override
            public LuaValue call(LuaValue table, LuaValue key) {
                String k = key.checkjstring();
                return switch (k) {
                    case "x" -> LuaValue.valueOf(location.getX());
                    case "y" -> LuaValue.valueOf(location.getY());
                    case "z" -> LuaValue.valueOf(location.getZ());
                    case "world" -> new LuaWorldWrapper(location.getWorld());
                    default -> NIL;
                };
            }
        });

        setmetatable(meta);
    }

    public Location getBukkitLocation() {
        return location;
    }
}
