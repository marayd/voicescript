package org.mryd.lua.wrap;

import lombok.extern.slf4j.Slf4j;
import org.bukkit.Bukkit;
import org.bukkit.scheduler.BukkitScheduler;
import org.luaj.vm2.Varargs;
import org.luaj.vm2.lib.VarArgFunction;
import org.mryd.VoiceScript;

@Slf4j
public class MainThreadFunctionWrapper {
    private static final MainThreadFunctionWrapper instance = new MainThreadFunctionWrapper();
    private final BukkitScheduler scheduler = Bukkit.getScheduler();

    public static MainThreadFunctionWrapper get() {
        return instance;
    }

    public VarArgFunction wrap(VarArgFunction function) {
        return new VarArgFunction() {
            @Override
            public Varargs invoke(Varargs args) {
                if (Bukkit.isPrimaryThread()) {
                    return function.invoke(args);
                }

                final Object lock = new Object();
                final Varargs[] result = new Varargs[1];

                scheduler.callSyncMethod(VoiceScript.instance, () -> {
                    synchronized (lock) {
                        result[0] = function.invoke(args);
                        lock.notify();
                    }
                    return null;
                });

                synchronized (lock) {
                    try {
                        lock.wait();
                    } catch (InterruptedException e) {
                        throw new RuntimeException("Interrupted waiting for main thread", e);
                    }
                }

                return result[0];
            }
        };
    }
}
