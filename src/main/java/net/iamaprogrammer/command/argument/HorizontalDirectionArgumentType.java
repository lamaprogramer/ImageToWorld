package net.iamaprogrammer.command.argument;

import com.mojang.brigadier.StringReader;
import com.mojang.brigadier.arguments.ArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.DynamicCommandExceptionType;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import net.minecraft.command.CommandSource;
import net.minecraft.text.Text;
import net.minecraft.util.math.Direction;

import java.util.concurrent.CompletableFuture;

public class HorizontalDirectionArgumentType implements ArgumentType<Direction> {
    private static final DynamicCommandExceptionType INVALID_DIRECTION;

    public static HorizontalDirectionArgumentType direction() {
        return new HorizontalDirectionArgumentType();
    }
    public static Direction getDirection(final CommandContext<?> context, final String name) {
        return context.getArgument(name, Direction.class);
    }
    @Override
    public Direction parse(StringReader reader) throws CommandSyntaxException {
        String directionName = reader.readUnquotedString();
        Direction direction = Direction.byName(directionName);
        if (direction != null && (direction.getId() >= 2 && direction.getId() <= 5)) {
            return direction;
        }

        throw INVALID_DIRECTION.createWithContext(reader, directionName);
    }

    @Override
    public <S> CompletableFuture<Suggestions> listSuggestions(CommandContext<S> context, SuggestionsBuilder builder) {
        return CommandSource.suggestMatching(new String[]{"north", "south", "east", "west"}, builder);
    }

    static {
        INVALID_DIRECTION = new DynamicCommandExceptionType((direction) -> Text.translatable("argument.direction.invalid", direction));
    }
}
