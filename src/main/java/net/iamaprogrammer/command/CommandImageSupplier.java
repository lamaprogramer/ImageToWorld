package net.iamaprogrammer.command;

import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.minecraft.util.Identifier;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.util.List;
import java.util.Map;

public interface CommandImageSupplier {
    void load(BufferedImage image, Map<Identifier, List<Color>> colorData) throws CommandSyntaxException;
}
