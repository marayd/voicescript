package org.mryd.lua.loader;

import lombok.Getter;
import org.mryd.lua.LuaEngine;

import java.io.File;

@Getter
public class Script {
    private final File file;
    private final LuaEngine engine;

    public Script(File file, LuaEngine engine) {
        this.file = file;
        this.engine = engine;
    }

    public void run(String script) {
        engine.execute(script);
    }


}
