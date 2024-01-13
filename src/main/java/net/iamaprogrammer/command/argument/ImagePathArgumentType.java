package net.iamaprogrammer.command.argument;

import com.mojang.brigadier.StringReader;
import com.mojang.brigadier.arguments.ArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.command.CommandSource;

import java.io.File;
import java.io.IOException;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.concurrent.CompletableFuture;

public class ImagePathArgumentType implements ArgumentType<String> {
    private static final Path RUN_FOLDER = FabricLoader.getInstance().getGameDir();
    private static final Path IMAGES_FOLDER = Path.of(RUN_FOLDER.toString(), "images" + File.separator);


    public static ImagePathArgumentType image() {
        return new ImagePathArgumentType();
    }

    public static String getImage(final CommandContext<?> context, final String name) {
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
    public String parse(StringReader reader) {
        final int start = reader.getCursor();
        while (reader.canRead() && isAllowedInPath(reader.peek())) {
            reader.skip();
        }

        return reader.getString().substring(start, reader.getCursor());
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
}
