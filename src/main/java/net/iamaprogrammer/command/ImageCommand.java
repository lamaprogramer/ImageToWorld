package net.iamaprogrammer.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.fabricmc.loader.api.FabricLoader;
import net.iamaprogrammer.command.argument.PathArgumentType;
import net.iamaprogrammer.command.argument.ScaleArgumentType;
import net.minecraft.command.CommandRegistryAccess;
import net.minecraft.registry.Registries;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.math.Vec3i;

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

public class ImageCommand {
    public static void register(CommandDispatcher<ServerCommandSource> dispatcher, CommandRegistryAccess commandRegistryAccess, CommandManager.RegistrationEnvironment registrationEnvironment) {
        dispatcher.register(CommandManager.literal("image").requires((source) -> source.hasPermissionLevel(2))
                .then(CommandManager.literal("paste")
                        .then(CommandManager.argument("imagePath", PathArgumentType.path()).executes(ImageCommand::run))
                        .then(CommandManager.argument("imagePath", PathArgumentType.path())
                                .then(CommandManager.argument("scalex", ScaleArgumentType.scale())
                                        .then(CommandManager.argument("scaley", ScaleArgumentType.scale()).executes(ImageCommand::run))
                                )
                        )
                )
        );
    }

    private static int run(CommandContext<ServerCommandSource> context) throws CommandSyntaxException {
        String imageName = context.getArgument("imagePath", String.class);
        double scaleX = 1;
        double scaleY = 1;
        try {
            scaleX = context.getArgument("scalex", double.class);
            scaleY = context.getArgument("scaley", double.class);
        } catch (IllegalArgumentException ignored) {}

        Path runFolder = FabricLoader.getInstance().getGameDir();
        Path fullPath = Path.of(runFolder.toString(), "images" + File.separator + imageName);
        if (Files.exists(fullPath)) {
            context.getSource().sendFeedback(() -> Text.of("Image Exists"), true);
            loadImage(context, fullPath, scaleX, scaleY);
            return 1;
        } else {
            context.getSource().sendFeedback(() -> Text.of("Image Does not Exist"), true);
            return -1;
        }
    }

    private static void loadImage(CommandContext<ServerCommandSource> context, Path path, double scaleX, double scaleY) {
        try {
            BufferedImage image = ImageIO.read(new File(path.toUri()));
            Map<Identifier, Color> colorData = loadAverageColorData();
            ServerPlayerEntity player =  context.getSource().getPlayer();

            if (colorData != null && player != null) {
                ServerWorld world = player.getServerWorld();
                Vec3d playerPos = player.getPos();


                Color previousPixelColor = null;
                Identifier previousBlock = null;
                Map<Color, Identifier> usedColors = new HashMap<>();


                int blockSizeX = (int) (image.getWidth() * scaleX);
                int blockSizeY = (int) (image.getHeight() * scaleY);

                Vec3i centerPos = new Vec3i((int) (playerPos.getX() - ((blockSizeX/2))), (int) playerPos.getY(), (int) (playerPos.getZ() - ((blockSizeY/2))));
                for (int y = 0; y < blockSizeY-1; y++) {
                    for (int x = 0; x < blockSizeX-1; x++) {
                        int coordinateOffsetX = (int) (x%scaleX);
                        int coordinateOffsetY = (int) (y%scaleY);

                        int pixelCoordinateX = (int) ((x-coordinateOffsetX)/scaleX);
                        int pixelCoordinateY = (int) ((y-coordinateOffsetY)/scaleY);

                        Color pixelColor = new Color(image.getRGB(pixelCoordinateX, pixelCoordinateY));


                        if (pixelColor.equals(previousPixelColor)) {
                            addBlockToWorld(previousBlock, world, centerPos, x, y);
                        } else if (usedColors.containsKey(pixelColor)) {
                            addBlockToWorld(usedColors.get(pixelColor), world, centerPos, x, y);
                        } else {
                            previousBlock = addBlockToWorld(pixelColor, colorData, world, centerPos, x, y, new ArrayList<>());
                            previousPixelColor = pixelColor;
                            usedColors.put(previousPixelColor, previousBlock);
                        }

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

    private static Identifier addBlockToWorld(Color imagePixelColor, Map<Identifier, Color> colorData, ServerWorld world, Vec3i pos, int x, int y, ArrayList<Identifier> blacklist) {
        Identifier bestMatch = getBestPixelToBlockMatch(imagePixelColor, colorData, blacklist);
        BlockPos blockPos = addBlockToWorld(bestMatch, world, pos, x, y);

        if (!world.getBlockState(blockPos).isFullCube(world, blockPos)) {
            blacklist.add(bestMatch);
            return addBlockToWorld(imagePixelColor, colorData, world, pos, x, y, blacklist);
        }
        return bestMatch;
    }

    private static BlockPos addBlockToWorld(Identifier blockId, ServerWorld world, Vec3i pos, int x, int y) {
        BlockPos blockPos = new BlockPos((int) (pos.getX()+x), (int) pos.getY(), (int) (pos.getZ()+y));
        world.setBlockState(blockPos, Registries.BLOCK.get(blockId).getDefaultState());
        return blockPos;
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
