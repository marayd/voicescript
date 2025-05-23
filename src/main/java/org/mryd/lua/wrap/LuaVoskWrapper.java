package org.mryd.lua.wrap;

import org.luaj.vm2.LuaTable;
import org.luaj.vm2.LuaValue;
import org.luaj.vm2.lib.OneArgFunction;
import org.mryd.api.vosk.VoskHandler;

import java.util.Base64;

public class LuaVoskWrapper extends LuaTable {

    public LuaVoskWrapper() {
        set("parse", new OneArgFunction() {
            @Override
            public LuaValue call(LuaValue arg) {
                try {
                    String result = VoskHandler.recognizeFromBytesAsync(Base64.getDecoder().decode(arg.tojstring())).get(); // blocks
                    return LuaValue.valueOf(result.substring(result.indexOf("\"text\"") + 9, result.lastIndexOf("\"")));
                } catch (Exception e) {
                    return LuaValue.valueOf("Error: " + e.getMessage());
                }
            }
        });
    }
}
