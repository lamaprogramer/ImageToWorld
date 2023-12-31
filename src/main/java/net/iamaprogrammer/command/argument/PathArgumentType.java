package net.iamaprogrammer.command.argument;

import com.mojang.brigadier.StringReader;
import com.mojang.brigadier.arguments.ArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.DynamicCommandExceptionType;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.command.CommandSource;
import net.minecraft.text.Text;

import java.io.File;
import java.io.IOException;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.concurrent.CompletableFuture;

public class PathArgumentType implements ArgumentType<String> {
    private static final Path RUN_FOLDER = FabricLoader.getInstance().getGameDir();
    private static final Path IMAGES_FOLDER = Path.of(RUN_FOLDER.toString(), "images" + File.separator);
    private static final DynamicCommandExceptionType FILE_NOT_FOUND;


    public static PathArgumentType path() {
        return new PathArgumentType();
    }

    public static String getPath(final CommandContext<?> context, final String name) {
        return context.getArgument(name, String.class);
    }

    private static boolean isAllowedInPath(final char c) {
        return c >= '0' && c <= '9'
                || c >= 'A' && c <= 'Z'
                || c >= 'a' && c <= 'z'
                || c == '/' || c == '\\'
                || c == '.' || c == '_';
    }
    @Override
    public String parse(StringReader reader) throws CommandSyntaxException {
        final int start = reader.getCursor();
        while (reader.canRead() && isAllowedInPath(reader.peek())) {
            reader.skip();
        }

        String text = reader.getString().substring(start, reader.getCursor());
        Path fullPath = Path.of(IMAGES_FOLDER + File.separator + text);
        if (Files.exists(fullPath)) {
            return text;
        } else {
            throw FILE_NOT_FOUND.createWithContext(reader, text);
        }
    }

    @Override
    public <S> CompletableFuture<Suggestions> listSuggestions(CommandContext<S> context, SuggestionsBuilder builder) {
        try (DirectoryStream<Path> fileStream = Files.newDirectoryStream(IMAGES_FOLDER)) {
            ArrayList<String> files = new ArrayList<>();

            for (Path path : fileStream) {
                if (!Files.isDirectory(path)) {
                    files.add(path.toString().replace(IMAGES_FOLDER + File.separator, ""));
                }
            }
            return CommandSource.suggestMatching(files, builder);
        } catch (IOException e) {
            return Suggestions.empty();
        }
    }

    static {
        FILE_NOT_FOUND = new DynamicCommandExceptionType((file) -> Text.translatable("argument.path.filenotfound", file));
    }
}
