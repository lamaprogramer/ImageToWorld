package net.iamaprogrammer.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.BoolArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.fabricmc.loader.api.FabricLoader;
import net.iamaprogrammer.command.argument.PathArgumentType;
import net.iamaprogrammer.command.argument.ScaleArgumentType;
import net.iamaprogrammer.util.ColorDataUtil;
import net.iamaprogrammer.util.MapDataUtil;
import net.minecraft.command.CommandRegistryAccess;
import net.minecraft.registry.Registries;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import net.minecraft.util.Pair;
import net.minecraft.util.math.BlockPos;
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
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

public class ImageCommand {
    public static void register(CommandDispatcher<ServerCommandSource> dispatcher, CommandRegistryAccess commandRegistryAccess, CommandManager.RegistrationEnvironment registrationEnvironment) {
        dispatcher.register(CommandManager.literal("image").requires((source) -> source.hasPermissionLevel(2))
                .then(CommandManager.literal("paste")
                        .then(CommandManager.argument("imagePath", PathArgumentType.path())
                                .executes((context) -> ImageCommand.run(context, PathArgumentType.getPath(context, "imagePath"), 1, 1))
                        )

                        .then(CommandManager.argument("imagePath", PathArgumentType.path())
                                .then(CommandManager.argument("scaleX", ScaleArgumentType.scale())
                                        .then(CommandManager.argument("scaleY", ScaleArgumentType.scale())
                                                .executes((context) -> ImageCommand.run(context, PathArgumentType.getPath(context, "imagePath"), ScaleArgumentType.getScale(context, "scaleX"), ScaleArgumentType.getScale(context, "scaleY")))
                                        )
                                )
                        )
                )
                .then(CommandManager.literal("pasteToMap")
                        .then(CommandManager.argument("imagePath", PathArgumentType.path())
                                .executes((context) -> ImageCommand.runMap(context, PathArgumentType.getPath(context, "imagePath"), 1, 1, false, true))
                        )
                        .then(CommandManager.argument("imagePath", PathArgumentType.path())
                                .then(CommandManager.argument("scaleX", ScaleArgumentType.scale())
                                        .then(CommandManager.argument("scaleY", ScaleArgumentType.scale())
                                                .executes((context) -> ImageCommand.runMap(context, PathArgumentType.getPath(context, "imagePath"), ScaleArgumentType.getScale(context, "scaleX"), ScaleArgumentType.getScale(context, "scaleY"), false, true))
                                        )
                                )
                        )
                        .then(CommandManager.argument("imagePath", PathArgumentType.path())
                                .then(CommandManager.argument("scaleX", ScaleArgumentType.scale())
                                        .then(CommandManager.argument("scaleY", ScaleArgumentType.scale())
                                                .then(CommandManager.argument("useStaircaseHeightMap", BoolArgumentType.bool())
                                                        .executes((context) -> ImageCommand.runMap(context, PathArgumentType.getPath(context, "imagePath"), ScaleArgumentType.getScale(context, "scaleX"), ScaleArgumentType.getScale(context, "scaleY"), BoolArgumentType.getBool(context, "useStaircaseHeightMap"), true))
                                                )
                                        )
                                )
                        )
                        .then(CommandManager.argument("imagePath", PathArgumentType.path())
                                .then(CommandManager.argument("scaleX", ScaleArgumentType.scale())
                                        .then(CommandManager.argument("scaleY", ScaleArgumentType.scale())
                                                .then(CommandManager.argument("useStaircaseHeightMap", BoolArgumentType.bool())
                                                        .then(CommandManager.argument("useMapColors", BoolArgumentType.bool())
                                                                .executes((context) -> ImageCommand.runMap(context, PathArgumentType.getPath(context, "imagePath"), ScaleArgumentType.getScale(context, "scaleX"), ScaleArgumentType.getScale(context, "scaleY"), BoolArgumentType.getBool(context, "useStaircaseHeightMap"), BoolArgumentType.getBool(context, "useMapColors")))
                                                        )
                                                )
                                        )
                                )
                        )
                )
        );
    }

    private static int run(CommandContext<ServerCommandSource> context, String imagePath, double scaleX, double scaleY) throws CommandSyntaxException {
        Path runFolder = FabricLoader.getInstance().getGameDir();
        Path fullPath = Path.of(runFolder.toString(), "images" + File.separator + imagePath);
        if (Files.exists(fullPath)) {
            context.getSource().sendFeedback(() -> Text.of("Image Exists"), true);

            CompletableFuture<String> future = CompletableFuture.supplyAsync(() -> {
                long before = System.nanoTime();
                loadImageAsBlockTextures(context, fullPath, scaleX, scaleY);
                long after = System.nanoTime();
                return "Finished in: " + ((after-before) / 1000000) + " ms";
            });
            future.thenAccept(System.out::println);
            return 1;
        } else {
            context.getSource().sendFeedback(() -> Text.of("Image Does not Exist"), true);
            return -1;
        }
    }

    private static int runMap(CommandContext<ServerCommandSource> context, String imagePath, double scaleX, double scaleY, boolean useStaircaseHeightMap, boolean useMapColors) throws CommandSyntaxException {
        Path runFolder = FabricLoader.getInstance().getGameDir();
        Path fullPath = Path.of(runFolder.toString(), "images" + File.separator + imagePath);
        if (Files.exists(fullPath)) {
            context.getSource().sendFeedback(() -> Text.of("Image Exists"), true);

            CompletableFuture<String> future = CompletableFuture.supplyAsync(() -> {
                long before = System.nanoTime();
                loadImageAsMap(context, fullPath, scaleX, scaleY, useStaircaseHeightMap);
                long after = System.nanoTime();
                return "Finished in: " + ((after-before) / 1000000) + " ms";
            });
            future.thenAccept(System.out::println);
            return 1;
        } else {
            context.getSource().sendFeedback(() -> Text.of("Image Does not Exist"), true);
            return -1;
        }
    }

    private static void loadImageAsMap(CommandContext<ServerCommandSource> context, Path path, double scaleX, double scaleY, boolean useStaircaseHeightMap) {
        try {
            BufferedImage image = ImageIO.read(new File(path.toUri()));
            Map<Identifier, List<Color>> colorData = ColorDataUtil.loadAverageColorData(true, useStaircaseHeightMap);
            ServerPlayerEntity player =  context.getSource().getPlayer();

            if (colorData != null && player != null) {
                ServerWorld world = player.getServerWorld();
                Vec3i startPos = getStartPos(true, player);

                Color previousPixelColor = null;
                Identifier previousBlock = null;
                Map<Color, Identifier> usedColors = new HashMap<>();

                int blockSizeX = (int) (image.getWidth() * scaleX);
                int blockSizeY = (int) (image.getHeight() * scaleY);
                Vec3i centerPos = new Vec3i(startPos.getX() - ((blockSizeX/2)), startPos.getY(), startPos.getZ() - ((blockSizeY/2)));

                int[][] heightMap = null;
                if (useStaircaseHeightMap) {
                    heightMap = MapDataUtil.generateHeightMap(blockSizeX, blockSizeY, scaleX, scaleY, image, colorData, new ArrayList<>());
                }

                int coordinateOffsetX;
                int coordinateOffsetY;
                int pixelCoordinateX;
                int pixelCoordinateY;
                Color pixelColor;

                for (int y = 0; y < blockSizeY; y++) {
                    for (int x = 0; x < blockSizeX; x++) {
                        coordinateOffsetX = (int) (x%scaleX);
                        coordinateOffsetY = (int) (y%scaleY);

                        pixelCoordinateX = (int) ((x-coordinateOffsetX)/scaleX);
                        pixelCoordinateY = (int) ((y-coordinateOffsetY)/scaleY);

                        pixelColor = new Color(image.getRGB(pixelCoordinateX, pixelCoordinateY));


                        if (pixelColor.equals(previousPixelColor)) {
                            addBlockToWorldMap(previousBlock, world, heightMap, centerPos, x, y);
                        } else if (usedColors.containsKey(pixelColor)) {
                            addBlockToWorldMap(usedColors.get(pixelColor), world, heightMap, centerPos, x, y);
                        } else {
                            previousBlock = addBlockToWorldMap(pixelColor, colorData, heightMap, world, centerPos, x, y, new ArrayList<>());
                            previousPixelColor = pixelColor;
                            usedColors.put(previousPixelColor, previousBlock);
                        }
                    }
                }
            }
        } catch (IOException ignored) {}
    }


    private static void loadImageAsBlockTextures(CommandContext<ServerCommandSource> context, Path path, double scaleX, double scaleY) {
        try {
            BufferedImage image = ImageIO.read(new File(path.toUri()));
            Map<Identifier, List<Color>> colorData = ColorDataUtil.loadAverageColorData(false, false);
            ServerPlayerEntity player =  context.getSource().getPlayer();

            if (colorData != null && player != null) {
                ServerWorld world = player.getServerWorld();
                Vec3i startPos = getStartPos(false, player);

                Color previousPixelColor = null;
                Identifier previousBlock = null;
                Map<Color, Identifier> usedColors = new HashMap<>();

                int blockSizeX = (int) (image.getWidth() * scaleX);
                int blockSizeY = (int) (image.getHeight() * scaleY);
                Vec3i centerPos = new Vec3i(startPos.getX() - ((blockSizeX/2)), startPos.getY(), startPos.getZ() - ((blockSizeY/2)));

                int coordinateOffsetX;
                int coordinateOffsetY;
                int pixelCoordinateX;
                int pixelCoordinateY;
                Color pixelColor;

                ArrayList<Identifier> blackList = new ArrayList<>();

                for (int y = 0; y < blockSizeY; y++) {
                    for (int x = 0; x < blockSizeX; x++) {
                        coordinateOffsetX = (int) (x%scaleX);
                        coordinateOffsetY = (int) (y%scaleY);
                        pixelCoordinateX = (int) ((x-coordinateOffsetX)/scaleX);
                        pixelCoordinateY = (int) ((y-coordinateOffsetY)/scaleY);

                        pixelColor = new Color(image.getRGB(pixelCoordinateX, pixelCoordinateY));

                        if (pixelColor.equals(previousPixelColor)) {
                            addBlockToWorld(previousBlock, world, centerPos, x, y);
                        } else if (usedColors.containsKey(pixelColor)) {
                            addBlockToWorld(usedColors.get(pixelColor), world, centerPos, x, y);
                        } else {
                            previousBlock = addBlockToWorld(pixelColor, colorData, world, centerPos, x, y, blackList);
                            previousPixelColor = pixelColor;
                            usedColors.put(previousPixelColor, previousBlock);
                        }
                    }
                }
            }
        } catch (IOException ignored) {}
    }

    private static Vec3i getStartPos(boolean snapToMapGrid, ServerPlayerEntity player) {
        Vec3d playerPos = player.getPos();
        Vec3i startPos = new Vec3i((int) playerPos.getX(), (int) playerPos.getY(), (int) playerPos.getZ());

        if (snapToMapGrid) {
            int remainderX = (int) (playerPos.getX() % 128);
            int remainderZ = (int) (playerPos.getZ() % 128);

            int mapX = (int) (remainderX < (128-remainderX) ? playerPos.getX() - remainderX : playerPos.getX() + (128 - remainderX));
            int mapZ = (int) (remainderZ < (128-remainderZ) ? playerPos.getZ() - remainderZ : playerPos.getZ() + (128 - remainderZ));
            startPos = new Vec3i(mapX, (int) playerPos.getY(), mapZ);
        }
        return startPos;
    }


    private static Identifier addBlockToWorld(Color imagePixelColor, Map<Identifier, List<Color>> colorData, ServerWorld world, Vec3i pos, int x, int y, ArrayList<Identifier> blacklist) {
        Identifier bestMatch = ColorDataUtil.getBestPixelToBlockMatch(imagePixelColor, colorData, blacklist);
        BlockPos blockPos = addBlockToWorld(bestMatch, world, pos, x, y);

        if (!world.getBlockState(blockPos).isFullCube(world, blockPos)) {
            blacklist.add(bestMatch);
            return addBlockToWorld(imagePixelColor, colorData, world, pos, x, y, blacklist);
        }
        return bestMatch;
    }

    private static Identifier addBlockToWorldMap(Color imagePixelColor, Map<Identifier, List<Color>> colorData, int[][] heightMap, ServerWorld world, Vec3i pos, int x, int y, ArrayList<Identifier> blacklist) {
        Pair<Identifier, Integer> bestMatch = ColorDataUtil.getBestPixelToMapColorMatch(imagePixelColor, colorData, blacklist, heightMap != null);
        BlockPos blockPos = addBlockToWorldMap(bestMatch.getLeft(), world, heightMap, pos, x, y);

        if (!world.getBlockState(blockPos).isFullCube(world, blockPos)) {
            blacklist.add(bestMatch.getLeft());
            return addBlockToWorldMap(imagePixelColor, colorData, heightMap, world, pos, x, y, blacklist);
        }
        return bestMatch.getLeft();
    }

    private static BlockPos addBlockToWorld(Identifier blockId, ServerWorld world, Vec3i pos, int x, int y) {
        BlockPos blockPos = new BlockPos(pos.getX()+x, pos.getY(), pos.getZ()+y);
        world.setBlockState(blockPos, Registries.BLOCK.get(blockId).getDefaultState());
        return blockPos;
    }
    private static BlockPos addBlockToWorldMap(Identifier blockId, ServerWorld world, int[][] heightMap, Vec3i pos, int x, int y) {
        BlockPos blockPos = new BlockPos(pos.getX()+x,  heightMap != null ? pos.getY() + heightMap[y][x] : pos.getY(), pos.getZ()+y);
        world.setBlockState(blockPos, Registries.BLOCK.get(blockId).getDefaultState());
        return blockPos;
    }
}
