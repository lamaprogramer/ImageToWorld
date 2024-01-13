package net.iamaprogrammer;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientLifecycleEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.iamaprogrammer.event.ClientStartedHandler;
import net.iamaprogrammer.network.ImageS2CPacket;
import net.iamaprogrammer.network.NetworkingConstants;

public class ImageToWorldClient implements ClientModInitializer {
    @Override
    public void onInitializeClient() {
        ClientLifecycleEvents.CLIENT_STARTED.register(new ClientStartedHandler());
        ClientPlayNetworking.registerGlobalReceiver(NetworkingConstants.IMAGE_DATA_ID, ImageS2CPacket::receive);
    }
}
