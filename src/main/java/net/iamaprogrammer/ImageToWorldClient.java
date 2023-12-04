package net.iamaprogrammer;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientLifecycleEvents;
import net.iamaprogrammer.event.ClientStartedHandler;

public class ImageToWorldClient implements ClientModInitializer {
    @Override
    public void onInitializeClient() {
        ClientLifecycleEvents.CLIENT_STARTED.register(new ClientStartedHandler());
    }
}
