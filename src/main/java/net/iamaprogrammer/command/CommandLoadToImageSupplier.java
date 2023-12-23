package net.iamaprogrammer.command;

import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.minecraft.block.MapColor;
import net.minecraft.util.Identifier;

import java.awt.image.BufferedImage;
import java.util.Map;

public interface CommandLoadToImageSupplier<T> {
    void load(T path, BufferedImage image, Map<Identifier, MapColor> colorData) throws CommandSyntaxException;
}
