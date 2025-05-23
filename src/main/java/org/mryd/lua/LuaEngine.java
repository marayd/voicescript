package org.mryd.lua;

import lombok.Getter;
import org.luaj.vm2.Globals;
import org.luaj.vm2.LuaValue;
import org.luaj.vm2.lib.jse.JsePlatform;

@Getter
public class LuaEngine {
    private final Globals globals;

    public LuaEngine() {
        this.globals = JsePlatform.standardGlobals();
    }

    public LuaEngine(LuaValue... bindings) {
        this();
        for (int i = 0; i < bindings.length; i += 2) {
            globals.set(bindings[i].tojstring(), bindings[i + 1]);
        }
    }

    public LuaValue execute(String luaScript) {
        return globals.load(luaScript).call();
    }

    public void bind(String name, LuaValue value) {
        globals.set(name, value);
    }

}
