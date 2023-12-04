package net.iamaprogrammer;

import net.fabricmc.api.ModInitializer;

import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.event.EventFactory;
import net.iamaprogrammer.command.ImageCommand;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ImageToWorld implements ModInitializer {
	// This logger is used to write text to the console and the log file.
	// It is considered best practice to use your mod id as the logger's name.
	// That way, it's clear which mod wrote info, warnings, and errors.
    public static final Logger LOGGER = LoggerFactory.getLogger("imagetoworld");

	@Override
	public void onInitialize() {
		// This code runs as soon as Minecraft is in a mod-load-ready state.
		// However, some things (like resources) may still be uninitialized.
		// Proceed with mild caution.

		CommandRegistrationCallback.EVENT.register(ImageCommand::register);
		LOGGER.info("Hello Fabric world!");
	}
}