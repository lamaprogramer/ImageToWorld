package net.iamaprogrammer;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientLifecycleEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.loader.api.FabricLoader;
import net.iamaprogrammer.event.ClientStartedHandler;
import net.iamaprogrammer.network.packets.ImageDataPacket;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

public class ImageToWorldClient implements ClientModInitializer {
    @Override
    public void onInitializeClient() {
        Path runFolder = FabricLoader.getInstance().getGameDir();
        try {
            Files.createDirectories(Path.of(runFolder.toString(), "images"));
        } catch (IOException ignored) {}

        ClientLifecycleEvents.CLIENT_STARTED.register(new ClientStartedHandler());

        ClientPlayNetworking.registerGlobalReceiver(ImageDataPacket.PACKET_ID, ImageDataPacket::receiveClient);
    }
}
