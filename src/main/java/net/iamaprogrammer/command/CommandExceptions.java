package net.iamaprogrammer.command;

import com.mojang.brigadier.exceptions.DynamicCommandExceptionType;
import com.mojang.brigadier.exceptions.SimpleCommandExceptionType;
import net.minecraft.text.Text;

public class CommandExceptions {
    public static DynamicCommandExceptionType NOT_AN_IMAGE_EXCEPTION = new DynamicCommandExceptionType((fileName) -> Text.translatable("imagetoworld.command.image.invalid", fileName));
    public static DynamicCommandExceptionType IMAGE_TOO_LARGE = new DynamicCommandExceptionType((fileName) -> Text.translatable("imagetoworld.command.image.large", fileName));
    public static SimpleCommandExceptionType COLOR_DATA_MISSING = new SimpleCommandExceptionType(Text.translatable("imagetoworld.command.color.data.missing"));
    public static SimpleCommandExceptionType IMAGE_PROCESSING_EXCEPTION = new SimpleCommandExceptionType(Text.translatable("imagetoworld.command.image.failed"));

}
