package org.mryd.lua.wrap.events;

import org.bukkit.entity.Player;
import org.luaj.vm2.*;
import org.luaj.vm2.lib.TwoArgFunction;
import org.mryd.lua.wrap.world.LuaPlayerWrapper;

public class LuaSpeechEventWrapper extends LuaTable {

    public LuaSpeechEventWrapper(String phrase, Player player) {
        LuaTable meta = new LuaTable();
        meta.set("__index", new TwoArgFunction() {
            @Override
            public LuaValue call(LuaValue table, LuaValue key) {
                String k = key.checkjstring();
                return switch (k) {
                    case "phrase" -> LuaValue.valueOf(phrase);
                    case "player" -> new LuaPlayerWrapper(player);
                    default -> NIL;
                };
            }
        });

        setmetatable(meta);
    }
}
