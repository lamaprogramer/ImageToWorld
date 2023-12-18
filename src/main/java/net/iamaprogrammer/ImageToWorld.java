package net.iamaprogrammer;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.command.v2.ArgumentTypeRegistry;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.iamaprogrammer.command.ImageCommand;
import net.iamaprogrammer.command.argument.HorizontalDirectionArgumentType;
import net.iamaprogrammer.command.argument.PathArgumentType;
import net.iamaprogrammer.command.argument.ScaleArgumentSerializer;
import net.iamaprogrammer.command.argument.ScaleArgumentType;
import net.minecraft.command.argument.serialize.ConstantArgumentSerializer;
import net.minecraft.util.Identifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ImageToWorld implements ModInitializer {
    public static final Logger LOGGER = LoggerFactory.getLogger("imagetoworld");
	public static final String MODID = "imagetoworld";

	@Override
	public void onInitialize() {
		CommandRegistrationCallback.EVENT.register(ImageCommand::register);
		ArgumentTypeRegistry.registerArgumentType(
				new Identifier(MODID, "scale"), ScaleArgumentType.class, new ScaleArgumentSerializer());
		ArgumentTypeRegistry.registerArgumentType(
				new Identifier(MODID, "direction"), HorizontalDirectionArgumentType.class, ConstantArgumentSerializer.of(HorizontalDirectionArgumentType::direction));
		ArgumentTypeRegistry.registerArgumentType(
				new Identifier(MODID, "path"), PathArgumentType.class, ConstantArgumentSerializer.of(PathArgumentType::path));
		LOGGER.info("Hello Fabric world!");
	}
}