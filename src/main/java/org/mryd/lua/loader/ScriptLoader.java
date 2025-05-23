package org.mryd.lua.loader;

import lombok.Getter;
import org.jetbrains.annotations.Nullable;
import org.luaj.vm2.LuaValue;
import org.mryd.lua.LuaEngine;
import org.mryd.lua.wrap.LuaAiWrapper;
import org.mryd.lua.wrap.LuaServerWrapper;
import org.mryd.lua.wrap.LuaVoskWrapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.util.HashMap;
import java.util.Map;

import static org.mryd.VoiceScript.instance;

@Getter
public class ScriptLoader {
    private static final String DEFAULT_PATH = instance.getDataPath() + "/scripts";
    private static final Logger log = LoggerFactory.getLogger(ScriptLoader.class);

    private final Map<String, Script> scripts = new HashMap<>();
    private final Map<String, LuaEngine> sharedEngines = new HashMap<>();

    public ScriptLoader() {
        loadScripts();
    }

    private void loadScripts() {
        File directory = new File(DEFAULT_PATH);
        File[] files = directory.listFiles();
        if (files == null) {
            log.warn("Script directory not found or empty: {}", DEFAULT_PATH);
            return;
        }

        for (File file : files) {
            if (file.getName().endsWith(".lua")) {
                loadScript(file);
            }
        }
    }

    private void loadScript(File file) {
        String engineType = "default";

        try {
            String header = readFirstComment(file);
            if (header != null && header.startsWith("-- lua")) {
                String[] parts = header.split(" ", 3);
                if (parts.length == 3 && !"default".equalsIgnoreCase(parts[2])) {
                    engineType = parts[2].toLowerCase(); // e.g. "physics"
                }
            }
        } catch (IOException e) {
            log.warn("Could not read engine type from {}: {}", file.getName(), e.getMessage());
        }

        try (FileReader reader = new FileReader(file)) {
            LuaEngine engine = sharedEngines.computeIfAbsent(engineType, k -> {
                log.info("Creating shared LuaEngine '{}'", k);
                return new LuaEngine();
            });

            engine.bind("server", new LuaServerWrapper());
            engine.bind("vosk", new LuaVoskWrapper());
            engine.bind("ai", new LuaAiWrapper());

            LuaValue chunk = engine.getGlobals().load(reader, file.getName());
            chunk.call();

            scripts.put(file.getName(), new Script(file, engine));

            log.info("Loaded script: {} | Engine: {}", file.getName(), engineType);

        } catch (IOException e) {
            log.error("Failed to load script {}: {}", file.getName(), e.getMessage());
        }
    }

    private String readFirstComment(File file) throws IOException {
        try (BufferedReader br = new BufferedReader(new FileReader(file))) {
            String line = br.readLine();
            if (line != null && line.trim().startsWith("--")) {
                return line.trim();
            }
        }
        return null;
    }

    @Nullable
    public LuaEngine getEngine(String scriptFileName) {
        Script script = scripts.get(scriptFileName);
        return script != null ? script.getEngine() : null;
    }

    public Map<String, LuaEngine> getAllEngines() {
        return sharedEngines;
    }

    public void reloadScripts() {
        log.info("Reloading scripts...");

        scripts.clear();

        sharedEngines.clear();

        loadScripts();

        log.info("Scripts reloaded successfully.");
    }
}
