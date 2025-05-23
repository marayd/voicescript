package org.mryd;

import de.maxhenkel.voicechat.api.BukkitVoicechatService;
import lombok.AccessLevel;
import lombok.Getter;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerChatEvent;
import org.bukkit.plugin.java.JavaPlugin;
import org.jetbrains.annotations.NotNull;
import org.luaj.vm2.LuaValue;
import org.mryd.api.simple.SimpleVoiceHook;
import org.mryd.api.vosk.VoskHandler;
import org.mryd.lua.LuaEngine;
import org.mryd.lua.LuaInitializer;
import org.mryd.lua.loader.ScriptLoader;
import org.mryd.lua.wrap.events.LuaSpeechEventWrapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Objects;

@Getter
public class VoiceScript extends JavaPlugin implements Listener, CommandExecutor {

    @Getter(AccessLevel.NONE)
    private static final Logger log = LoggerFactory.getLogger(VoiceScript.class);

    public static VoiceScript instance;
    public static LuaEngine lua;

    public static ScriptLoader loader;

    @Override
    public void onEnable() {
        instance = this;
        LuaInitializer initializer = new LuaInitializer();
        lua = initializer.getEngine();

        log.info("╔══════════════════════════════════════════════════════════╗");
        log.info("║                      VoiceScript                         ║");
        log.info("║                    Script Language                       ║");
        log.info("╠══════════════════════════════════════════════════════════╣");
        log.info("║ Developer : mryd (MrydDev)                               ║");
        log.info("║ Website   : https://mryd.org                             ║");
        log.info("║ GitHub    : https://github.com/marayd/voicescript        ║");
        log.info("║ License   : Apache 2.0                                   ║");
        log.info("╚══════════════════════════════════════════════════════════╝");
        BukkitVoicechatService service = Bukkit.getServer().getServicesManager().load(BukkitVoicechatService.class);
        if (service != null) {
            service.registerPlugin(new SimpleVoiceHook());
        }
        VoskHandler.init();
        loader = new ScriptLoader();
        Bukkit.getPluginManager().registerEvents(this, this);

        // COMMANDS:
        Objects.requireNonNull(getCommand("lua")).setExecutor(this);
    }

    @Override
    public void onDisable() {
        log.info("Disabled");
    }

    @EventHandler
    public void onPlayerChats(PlayerChatEvent event) {
        if (event.getMessage().startsWith("LUA: ") && event.getPlayer().isOp()) {
            String code = event.getMessage().substring(5);
            Bukkit.getScheduler().runTask(this, () -> lua.execute(code));
//            lua.execute(code);
        } else if (event.getMessage().startsWith("START EVENT") && event.getPlayer().isOp()) {
            loader.getAllEngines().forEach((string, luaEngine) -> {
                LuaValue globals = luaEngine.getGlobals();

                LuaValue onSpeechEventFunc = globals.get("onSpeechEvent");
                if (!onSpeechEventFunc.isnil()) {
                    LuaSpeechEventWrapper luaEvent = new LuaSpeechEventWrapper("test", event.getPlayer());
                    onSpeechEventFunc.call(luaEvent);
                }
            });

        }
    }

    private boolean isPluginNotAvailable(String pluginName) {
        return !Bukkit.getPluginManager().isPluginEnabled(pluginName);
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String @NotNull [] args) {
        if (args.length == 1 && args[0].equalsIgnoreCase("reload")) {
            loader.reloadScripts();
            sender.sendMessage(Component.text("✔ Скрипты успешно перезагружены.", NamedTextColor.GREEN));
            return true;
        }

        sender.sendMessage(Component.text("✖ Использование: /" + label + " reload", NamedTextColor.RED));
        return false;
    }

}