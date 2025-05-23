package org.mryd.lua;

import lombok.Getter;
import org.mryd.lua.wrap.*;

@Getter
public class LuaInitializer {
    private final LuaEngine engine;

    public LuaInitializer() {
        engine = new LuaEngine();

        engine.bind("server", new LuaVoskWrapper());
    }
}
