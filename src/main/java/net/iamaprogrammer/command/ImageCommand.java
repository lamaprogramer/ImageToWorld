package net.iamaprogrammer.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.command.CommandRegistryAccess;
import net.minecraft.registry.Registries;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Supplier;

public class ImageCommand {
    public static void register(CommandDispatcher<ServerCommandSource> dispatcher, CommandRegistryAccess commandRegistryAccess, CommandManager.RegistrationEnvironment registrationEnvironment) {
        dispatcher.register(CommandManager.literal("image").requires((source) -> source.hasPermissionLevel(2))
                .then(CommandManager.literal("paste")
                        .then(CommandManager.argument("imagePath", StringArgumentType.word()).executes(ImageCommand::run))
        ));
    }

    private static int run(CommandContext<ServerCommandSource> context) throws CommandSyntaxException {
        String imageName = context.getArgument("imagePath", String.class);
        Path runFolder = FabricLoader.getInstance().getGameDir();

        Path fullPath = Path.of(runFolder.toString(), "images" + File.separator + imageName);
        if (Files.exists(fullPath)) {
            context.getSource().sendFeedback(() -> Text.of("Image Exists"), true);
            loadImage(context, fullPath);
            return 1;
        } else {
            context.getSource().sendFeedback(() -> Text.of("Image Does not Exist"), true);
            return -1;
        }
    }

    private static void loadImage(CommandContext<ServerCommandSource> context, Path path) {
        try {
            BufferedImage image = ImageIO.read(new File(path.toUri()));
            Map<Identifier, Color> colorData = loadAverageColorData();
            ServerPlayerEntity player =  context.getSource().getPlayer();

            if (colorData != null && player != null) {
                ServerWorld world = player.getServerWorld();
                Vec3d pos = player.getPos();

                for (int y = 0; y < image.getHeight()-1; y++) {
                    for (int x = 0; x < image.getWidth()-1; x++) {
                        Color pixelColor = new Color(image.getRGB(x, y));
                        addBlockToWorld(pixelColor, colorData, world, pos, x, y, new ArrayList<>());
                    }
                }
            }
        } catch (IOException ignored) {}
    }

    private static Map<Identifier, Color> loadAverageColorData() throws IOException {
        Path path = Path.of(FabricLoader.getInstance().getConfigDir().toString(), "imagetoworld" + File.separator + "texturedata.txt");
        Map<Identifier, Color> data = new HashMap<>();
        if (Files.exists(path)) {
            String[] lines = Files.readString(path).split("\n");

            for (String line : lines) {
                String[] splitLine = line.split("#");
                Identifier blockId = new Identifier(splitLine[0]);
                String[] colors = splitLine[1].split(" ");
                int r = Integer.parseInt(colors[0]);
                int g = Integer.parseInt(colors[1]);
                int b = Integer.parseInt(colors[2]);
                Color color = new Color(r, g, b);
                data.put(blockId, color);
            }
            return data;
        }
        return null;
    }

    private static void addBlockToWorld(Color imagePixelColor, Map<Identifier, Color> colorData, ServerWorld world, Vec3d pos, int x, int y, ArrayList<Identifier> blacklist) {
        Identifier bestMatch = getBestPixelToBlockMatch(imagePixelColor, colorData, blacklist);
        BlockPos blockPos = new BlockPos((int) (pos.getX()+x), (int) pos.getY(), (int) (pos.getZ()+y));
        world.setBlockState(blockPos, Registries.BLOCK.get(bestMatch).getDefaultState());

        if (!world.getBlockState(blockPos).isFullCube(world, blockPos)) {
            blacklist.add(bestMatch);
            addBlockToWorld(imagePixelColor, colorData, world, pos, x, y, blacklist);
        }
    }

    private static Identifier getBestPixelToBlockMatch(Color imagePixelColor, Map<Identifier, Color> colorData, ArrayList<Identifier> blacklist) {
        int previousSumDifference = 255+255+255;
        Identifier bestMatch = null;

        for (Identifier key : colorData.keySet()) {
            if (!blacklist.contains(key)) {
                Color color = colorData.get(key);

                int redDifference = Math.abs(color.getRed() - imagePixelColor.getRed());
                int greenDifference = Math.abs(color.getGreen() - imagePixelColor.getGreen());
                int blueDifference = Math.abs(color.getBlue() - imagePixelColor.getBlue());
                int sumDifference = redDifference + greenDifference + blueDifference;

                if (sumDifference < previousSumDifference) {
                    previousSumDifference = sumDifference;
                    bestMatch = key;
                }
            }
        }
        return bestMatch;
    }
}
