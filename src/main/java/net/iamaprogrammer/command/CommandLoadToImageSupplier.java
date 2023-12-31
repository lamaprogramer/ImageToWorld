package net.iamaprogrammer.command;

import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.minecraft.block.MapColor;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.Identifier;

import java.awt.image.BufferedImage;
import java.util.Map;

public interface CommandLoadToImageSupplier {
    void load(BufferedImage image, ServerPlayerEntity player, Map<Identifier, MapColor> colorData) throws CommandSyntaxException;
}
