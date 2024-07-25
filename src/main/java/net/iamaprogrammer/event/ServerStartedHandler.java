package net.iamaprogrammer.event;

import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.iamaprogrammer.util.DataDefaults;
import net.minecraft.server.MinecraftServer;

public class ServerStartedHandler implements ServerLifecycleEvents.ServerStarted {
    @Override
    public void onServerStarted(MinecraftServer server) {
        if (server.isDedicated()) {
            DataDefaults.loadDefaults();
        }
    }
}
