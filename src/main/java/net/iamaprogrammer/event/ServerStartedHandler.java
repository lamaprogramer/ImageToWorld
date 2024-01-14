package net.iamaprogrammer.event;

import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.loader.api.FabricLoader;
import net.iamaprogrammer.ImageToWorld;
import net.iamaprogrammer.util.DataDefaults;
import net.minecraft.client.MinecraftClient;
import net.minecraft.server.MinecraftServer;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;

public class ServerStartedHandler implements ServerLifecycleEvents.ServerStarted {
    @Override
    public void onServerStarted(MinecraftServer server) {
        try {
            MinecraftClient.getInstance();
        } catch (RuntimeException e) {
            DataDefaults.loadDefaults();
        }
    }
}
