package net.iamaprogrammer.command.argument;

import com.mojang.brigadier.StringReader;
import com.mojang.brigadier.arguments.ArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.DynamicCommandExceptionType;
import net.minecraft.text.Text;

public class ScaleArgumentType implements ArgumentType<Double> {
    private static final DynamicCommandExceptionType INVALID_SCALE_FORMAT;
    private static final DynamicCommandExceptionType IS_ZERO;

    private final double minimum;
    private final double maximum;

    private ScaleArgumentType(final double minimum, final double maximum) {
        this.minimum = minimum;
        this.maximum = maximum;
    }

    public static ScaleArgumentType scale() {
        return scale(Double.MIN_VALUE);
    }
    public static ScaleArgumentType scale(final double min) {
        return scale(min, Double.MAX_VALUE);
    }
    public static ScaleArgumentType scale(final double min, final double max) {
        return new ScaleArgumentType(min, max);
    }

    public static double getScale(final CommandContext<?> context, final String name) {
        return context.getArgument(name, double.class);
    }

    public double getMinimum() {
        return minimum;
    }

    public double getMaximum() {
        return maximum;
    }

    private boolean isAllowedInScale(final char c) {
        return c >= '0' && c <= '9' || c == ':';
    }

    @Override
    public Double parse(StringReader reader) throws CommandSyntaxException {
        final int start = reader.getCursor();
        while (reader.canRead() && isAllowedInScale(reader.peek())) {
            reader.skip();
        }

        String text = reader.getString().substring(start, reader.getCursor());
        if (text.contains(":")) {
            String[] numbers = text.split(":");
            if (numbers.length == 2) {
                try {
                    double firstNum = Integer.parseInt(numbers[0]);
                    double secondNum = Integer.parseInt(numbers[1]);

                    if (firstNum == 0 || secondNum == 0) {
                        throw IS_ZERO.createWithContext(reader, text);
                    }

                    double scale = firstNum / secondNum;

                    if (scale < this.minimum) {
                        throw CommandSyntaxException.BUILT_IN_EXCEPTIONS.integerTooLow().createWithContext(reader, scale, minimum);
                    } else if (scale > this.maximum) {
                        throw CommandSyntaxException.BUILT_IN_EXCEPTIONS.integerTooHigh().createWithContext(reader, scale, maximum);
                    }

                    return firstNum/secondNum;
                } catch (NumberFormatException e){
                    throw INVALID_SCALE_FORMAT.createWithContext(reader, text);
                }
            } else {
                throw INVALID_SCALE_FORMAT.createWithContext(reader, text);
            }
        } else {
            throw INVALID_SCALE_FORMAT.createWithContext(reader, text);
        }
    }

    static {
        INVALID_SCALE_FORMAT = new DynamicCommandExceptionType((scale) -> Text.translatable("argument.scale.invalid", scale));
        IS_ZERO = new DynamicCommandExceptionType((scale) -> Text.translatable("argument.scale.invalid.zero", scale));
    }
}
