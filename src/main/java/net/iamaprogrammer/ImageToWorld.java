package net.iamaprogrammer;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.command.v2.ArgumentTypeRegistry;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.iamaprogrammer.command.ImageCommand;
import net.iamaprogrammer.command.argument.HorizontalDirectionArgumentType;
import net.iamaprogrammer.command.argument.ImagePathArgumentType;
import net.iamaprogrammer.command.argument.ScaleArgumentSerializer;
import net.iamaprogrammer.command.argument.ScaleArgumentType;
import net.iamaprogrammer.event.ServerStartedHandler;
import net.iamaprogrammer.network.packets.ImageDataPacket;
import net.minecraft.command.argument.serialize.ConstantArgumentSerializer;
import net.minecraft.util.Identifier;

public class ImageToWorld implements ModInitializer {
	public static final String MODID = "imagetoworld";

	@Override
	public void onInitialize() {
		ServerLifecycleEvents.SERVER_STARTED.register(new ServerStartedHandler());
		CommandRegistrationCallback.EVENT.register(ImageCommand::register);

		PayloadTypeRegistry.playC2S().register(ImageDataPacket.PACKET_ID, ImageDataPacket.PACKET_CODEC);
		PayloadTypeRegistry.playS2C().register(ImageDataPacket.PACKET_ID, ImageDataPacket.PACKET_CODEC);

		ArgumentTypeRegistry.registerArgumentType(
				Identifier.of(MODID, "scale"), ScaleArgumentType.class, new ScaleArgumentSerializer());
		ArgumentTypeRegistry.registerArgumentType(
				Identifier.of(MODID, "direction"), HorizontalDirectionArgumentType.class, ConstantArgumentSerializer.of(HorizontalDirectionArgumentType::direction));
		ArgumentTypeRegistry.registerArgumentType(
				Identifier.of(MODID, "path"), ImagePathArgumentType.class, ConstantArgumentSerializer.of(ImagePathArgumentType::image));


	}
}