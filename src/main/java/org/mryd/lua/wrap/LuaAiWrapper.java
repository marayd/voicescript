package org.mryd.lua.wrap;

import lombok.extern.slf4j.Slf4j;
import org.luaj.vm2.*;
import org.luaj.vm2.lib.VarArgFunction;
import org.mryd.api.ai.IntelligenceUtils;

@Slf4j
public class LuaAiWrapper extends LuaTable {

    public LuaAiWrapper() {
        set("generate", new VarArgFunction() {
            @Override
            public Varargs invoke(Varargs args) {
                if (args.narg() < 6) return NIL;

                String content = args.arg(1).tojstring();
                String instruction = args.arg(2).tojstring();
                String baseUrl = args.arg(3).tojstring();
                String apiKey = args.arg(4).tojstring();
                String model = args.arg(5).tojstring();
                LuaValue callback = args.arg(6); // Lua function

                if (!callback.isfunction()) {
                    return LuaValue.valueOf("Error: callback must be a function");
                }

                new Thread(() -> {
                    try {
                        String result = IntelligenceUtils.generateContent(content, instruction, baseUrl, apiKey, model);
                        callback.call(LuaValue.valueOf(result));
                    } catch (Exception e) {
                        callback.call(LuaValue.valueOf("Error: " + e.getMessage()));
                        log.error(e.getMessage(), e);
                    }
                }).start();

                return LuaValue.valueOf("Processing started");
            }
        });
    }
}
