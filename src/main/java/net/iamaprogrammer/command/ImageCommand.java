package net.iamaprogrammer.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.BoolArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.fabricmc.loader.api.FabricLoader;
import net.iamaprogrammer.command.argument.HorizontalDirectionArgumentType;
import net.iamaprogrammer.command.argument.PathArgumentType;
import net.iamaprogrammer.command.argument.ScaleArgumentType;
import net.iamaprogrammer.util.ColorDataUtil;
import net.iamaprogrammer.util.MapDataUtil;
import net.minecraft.block.Block;
import net.minecraft.block.Blocks;
import net.minecraft.block.FallingBlock;
import net.minecraft.block.MapColor;
import net.minecraft.command.CommandRegistryAccess;
import net.minecraft.item.FilledMapItem;
import net.minecraft.item.ItemStack;
import net.minecraft.item.map.MapState;
import net.minecraft.registry.Registries;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import net.minecraft.util.Pair;
import net.minecraft.util.math.*;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.List;
import java.util.concurrent.CompletableFuture;

public class ImageCommand {
    public static void register(CommandDispatcher<ServerCommandSource> dispatcher, CommandRegistryAccess commandRegistryAccess, CommandManager.RegistrationEnvironment registrationEnvironment) {
        dispatcher.register(CommandManager.literal("image").requires((source) -> source.hasPermissionLevel(2))
                .then(CommandManager.literal("paste")
                        .then(CommandManager.argument("imagePath", PathArgumentType.path())
                                .then(CommandManager.argument("scaleX", ScaleArgumentType.scale())
                                        .then(CommandManager.argument("scaleY", ScaleArgumentType.scale())
                                                .then(CommandManager.argument("direction", HorizontalDirectionArgumentType.direction())
                                                        .executes((context) -> ImageCommand.generate(context, PathArgumentType.getPath(context, "imagePath"), ScaleArgumentType.getScale(context, "scaleX"), ScaleArgumentType.getScale(context, "scaleY"), HorizontalDirectionArgumentType.getDirection(context, "direction")))
                                                ).executes((context) -> ImageCommand.generate(context, PathArgumentType.getPath(context, "imagePath"), ScaleArgumentType.getScale(context, "scaleX"), ScaleArgumentType.getScale(context, "scaleY"), Direction.NORTH))
                                        )
                                ).executes((context) -> ImageCommand.generate(context, PathArgumentType.getPath(context, "imagePath"), 1, 1, Direction.NORTH))
                        )
                )
                .then(CommandManager.literal("pasteToMap")
                        .then(CommandManager.argument("imagePath", PathArgumentType.path())
                                .then(CommandManager.argument("scaleX", ScaleArgumentType.scale())
                                        .then(CommandManager.argument("scaleY", ScaleArgumentType.scale())
                                                .then(CommandManager.argument("direction", HorizontalDirectionArgumentType.direction())
                                                        .executes((context) -> ImageCommand.generateForMap(context, PathArgumentType.getPath(context, "imagePath"), ScaleArgumentType.getScale(context, "scaleX"), ScaleArgumentType.getScale(context, "scaleY"), HorizontalDirectionArgumentType.getDirection(context, "direction"), false, true))
                                                )
                                                .then(CommandManager.argument("useStaircaseHeightMap", BoolArgumentType.bool())
                                                        .then(CommandManager.argument("useMapColors", BoolArgumentType.bool())
                                                                .executes((context) -> ImageCommand.generateForMap(context, PathArgumentType.getPath(context, "imagePath"), ScaleArgumentType.getScale(context, "scaleX"), ScaleArgumentType.getScale(context, "scaleY"), Direction.NORTH, BoolArgumentType.getBool(context, "useStaircaseHeightMap"), BoolArgumentType.getBool(context, "useMapColors")))
                                                        ).executes((context) -> ImageCommand.generateForMap(context, PathArgumentType.getPath(context, "imagePath"), ScaleArgumentType.getScale(context, "scaleX"), ScaleArgumentType.getScale(context, "scaleY"), Direction.NORTH, BoolArgumentType.getBool(context, "useStaircaseHeightMap"), true))
                                                ).executes((context) -> ImageCommand.generateForMap(context, PathArgumentType.getPath(context, "imagePath"), ScaleArgumentType.getScale(context, "scaleX"), ScaleArgumentType.getScale(context, "scaleY"), Direction.NORTH, false, true))
                                        )
                                ).executes((context) -> ImageCommand.generateForMap(context, PathArgumentType.getPath(context, "imagePath"), 1, 1, Direction.NORTH, false, true))
                        )
                )
                .then(CommandManager.literal("give")
                        .then(CommandManager.argument("imagePath", PathArgumentType.path())
                                .then(CommandManager.argument("scaleX", ScaleArgumentType.scale())
                                        .then(CommandManager.argument("scaleY", ScaleArgumentType.scale())
                                                .then(CommandManager.argument("direction", HorizontalDirectionArgumentType.direction())
                                                        .executes((context) -> ImageCommand.giveMap(context, PathArgumentType.getPath(context, "imagePath"), ScaleArgumentType.getScale(context, "scaleX"), ScaleArgumentType.getScale(context, "scaleY"), HorizontalDirectionArgumentType.getDirection(context, "direction")))
                                                ).executes((context) -> ImageCommand.giveMap(context, PathArgumentType.getPath(context, "imagePath"), ScaleArgumentType.getScale(context, "scaleX"), ScaleArgumentType.getScale(context, "scaleY"), Direction.NORTH))
                                        )
                                ).executes((context) -> ImageCommand.giveMap(context, PathArgumentType.getPath(context, "imagePath"), 1, 1, Direction.NORTH))
                        )
                )
        );
    }

    private static int generate(CommandContext<ServerCommandSource> context, String imagePath, double scaleX, double scaleY, Direction direction) throws CommandSyntaxException {
        Path runFolder = FabricLoader.getInstance().getGameDir();
        Path fullPath = Path.of(runFolder.toString(), "images" + File.separator + imagePath);
        if (Files.exists(fullPath)) {
            context.getSource().sendFeedback(() -> Text.of("Image Exists"), true);

            CompletableFuture<String> future = CompletableFuture.supplyAsync(() -> {
                long before = System.nanoTime();
                loadImageAsBlockTextures(context, fullPath, scaleX, scaleY, direction);
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

    private static int generateForMap(CommandContext<ServerCommandSource> context, String imagePath, double scaleX, double scaleY, Direction direction, boolean useStaircaseHeightMap, boolean useMapColors) throws CommandSyntaxException {
        Path runFolder = FabricLoader.getInstance().getGameDir();
        Path fullPath = Path.of(runFolder.toString(), "images" + File.separator + imagePath);
        if (Files.exists(fullPath)) {
            context.getSource().sendFeedback(() -> Text.of("Image Exists"), true);

            CompletableFuture<String> future = CompletableFuture.supplyAsync(() -> {
                long before = System.nanoTime();
                loadImageAsMap(context, fullPath, scaleX, scaleY, direction, useStaircaseHeightMap);
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

    private static int giveMap(CommandContext<ServerCommandSource> context, String imagePath, double scaleX, double scaleY, Direction direction) throws CommandSyntaxException {
        Path runFolder = FabricLoader.getInstance().getGameDir();
        Path fullPath = Path.of(runFolder.toString(), "images" + File.separator + imagePath);
        if (Files.exists(fullPath)) {
            context.getSource().sendFeedback(() -> Text.of("Image Exists"), true);

            CompletableFuture<String> future = CompletableFuture.supplyAsync(() -> {
                long before = System.nanoTime();
                loadImageToMap(context, fullPath, scaleX, scaleY, direction, true);
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



    private static void loadImageAsMap(CommandContext<ServerCommandSource> context, Path path, double scaleX, double scaleY, Direction direction, boolean useStaircaseHeightMap) {
        try {
            BufferedImage image = ImageIO.read(new File(path.toUri()));
            Map<Identifier, List<Color>> colorData = ColorDataUtil.loadColorData(true, useStaircaseHeightMap);
            ServerPlayerEntity player =  context.getSource().getPlayer();

            if (colorData != null && player != null) {

                Direction directionZ = direction;
                Direction directionX = direction.rotateYCounterclockwise();

                Vec3i startDirection = directionZ.getVector().add(directionX.getVector());
                Vec3i multiplier = directionZ.getOpposite().getVector().add(directionX.getOpposite().getVector());

                ServerWorld world = player.getServerWorld();
                Vec3i startPos = getStartPos(true, player);

                Color previousPixelColor = null;
                Identifier previousBlock = null;
                Map<Color, Identifier> usedColors = new HashMap<>();

                int blockSizeX = (int) (image.getWidth() * scaleX);
                int blockSizeY = (int) (image.getHeight() * scaleY);

                Vec3i centerPos = calculatePositionToCenter(startPos, startDirection, blockSizeX, blockSizeY);

                int[][] heightMap = null;
                if (useStaircaseHeightMap) {
                    heightMap = MapDataUtil.generateHeightMap(blockSizeX, blockSizeY, scaleX, scaleY, image, colorData, new ArrayList<>());
                }

                Color pixelColor;
                ArrayList<Identifier> blackList = new ArrayList<>();

                for (int y = 0; y < blockSizeY; y++) {
                    for (int x = 0; x < blockSizeX; x++) {
                        int[] pixelCoordinates = pixelCoordinatesWithScale(x, y, scaleX, scaleY);
                        pixelColor = new Color(image.getRGB(pixelCoordinates[0], pixelCoordinates[1]));

                        int coordX = direction.getAxis() == Direction.Axis.X ? y : x;
                        int coordZ = direction.getAxis() == Direction.Axis.X ? x : y;

                        if (pixelColor.equals(previousPixelColor)) {
                            addBlockToWorldMap(previousBlock, world, heightMap, centerPos, coordX*multiplier.getX(), coordZ*multiplier.getZ());
                        } else if (usedColors.containsKey(pixelColor)) {
                            addBlockToWorldMap(usedColors.get(pixelColor), world, heightMap, centerPos, coordX*multiplier.getX(), coordZ*multiplier.getZ());
                        } else {
                            previousBlock = addBlockToWorldMap(pixelColor, colorData, heightMap, world, centerPos, coordX*multiplier.getX(), coordZ*multiplier.getZ(), blackList);
                            previousPixelColor = pixelColor;
                            usedColors.put(previousPixelColor, previousBlock);
                        }
                    }
                }
            }
        } catch (IOException ignored) {
            System.out.println(ignored);
        }
    }


    private static void loadImageToMap(CommandContext<ServerCommandSource> context, Path path, double scaleX, double scaleY, Direction direction, boolean useStaircaseHeightMap) {
        try {
            BufferedImage image = ImageIO.read(new File(path.toUri()));
            Map<Identifier, MapColor> colorData = ColorDataUtil.loadColorDataAsMapColor(useStaircaseHeightMap);
            ServerPlayerEntity player = context.getSource().getPlayer();

            if (image.getWidth() > 128 || image.getHeight() > 128) {
                double divisor = 2;
                while (true) {
                    if (image.getWidth() > 128 && image.getWidth() * (1/divisor) <= 128) {
                        scaleX = 1/divisor;
                    }
                    if (image.getHeight() > 128 && image.getHeight() * (1/divisor) <= 128) {
                        scaleY = 1/divisor;
                    }
                    if (image.getWidth()*scaleX <= 128 && image.getHeight()*scaleY <= 128) {
                        break;
                    }
                    divisor++;
                }
            }

            if (colorData != null && player != null) {
                Direction directionX = direction.rotateYCounterclockwise();
                Direction directionZ = direction;

                Vec3i multiplier = directionZ.getOpposite().getVector().add(directionX.getOpposite().getVector());
                ServerWorld world = player.getServerWorld();

                Color previousPixelColor = null;
                Pair<MapColor, MapColor.Brightness> previousMapColor;
                Map<Color, Pair<MapColor, MapColor.Brightness>> usedColors = new HashMap<>();

                int blockSizeX = (int) (image.getWidth() * scaleX);
                int blockSizeY = (int) (image.getHeight() * scaleY);

                Color pixelColor;
                ItemStack stack = FilledMapItem.createMap(world, 0, 0, (byte)0, false, false);
                MapState state = FilledMapItem.getMapState(stack, world);

                for (int y = 0; y < blockSizeY; y++) {
                    for (int x = 0; x < blockSizeX; x++) {
                        int[] pixelCoordinates = pixelCoordinatesWithScale(x, y, scaleX, scaleY);
                        pixelColor = new Color(image.getRGB(pixelCoordinates[0], pixelCoordinates[1]));

                        int coordX = direction.getAxis() == Direction.Axis.X ? y : x;
                        int coordZ = direction.getAxis() == Direction.Axis.X ? x : y;

                        if (pixelColor.equals(previousPixelColor)) {
                            addColorToMap(state, usedColors.get(pixelColor).getLeft(), usedColors.get(pixelColor).getRight(), coordX*multiplier.getX(), coordZ*multiplier.getZ());
                        } else if (usedColors.containsKey(pixelColor)) {
                            addColorToMap(state, usedColors.get(pixelColor).getLeft(), usedColors.get(pixelColor).getRight(), coordX*multiplier.getX(), coordZ*multiplier.getZ());
                        } else {
                            previousMapColor = addColorToMap(state, pixelColor, colorData, coordX*multiplier.getX(), coordZ*multiplier.getZ(), new ArrayList<>());
                            previousPixelColor = pixelColor;
                            usedColors.put(previousPixelColor, previousMapColor);
                        }
                    }
                }
                stack.getOrCreateNbt().putBoolean("map_to_lock", true);
                player.getInventory().insertStack(stack);
            }
        } catch (IOException ignored) {}
    }

    private static void loadImageAsBlockTextures(CommandContext<ServerCommandSource> context, Path path, double scaleX, double scaleY, Direction direction) {
        try {
            BufferedImage image = ImageIO.read(new File(path.toUri()));
            Map<Identifier, List<Color>> colorData = ColorDataUtil.loadColorData(false, false);
            ServerPlayerEntity player =  context.getSource().getPlayer();

            if (colorData != null && player != null) {
                Direction directionZ = direction;
                Direction directionX = direction.rotateYCounterclockwise();

                Vec3i startDirection = directionZ.getVector().add(directionX.getVector());
                Vec3i multiplier = directionZ.getOpposite().getVector().add(directionX.getOpposite().getVector());

                ServerWorld world = player.getServerWorld();
                Vec3i startPos = getStartPos(false, player);

                Color previousPixelColor = null;
                Identifier previousBlock = null;
                Map<Color, Identifier> usedColors = new HashMap<>();

                int blockSizeX = (int) (image.getWidth() * scaleX);
                int blockSizeY = (int) (image.getHeight() * scaleY);

                Vec3i centerPos = calculatePositionToCenter(startPos, startDirection, blockSizeX, blockSizeY);

                Color pixelColor;
                ArrayList<Identifier> blackList = new ArrayList<>();

                for (int y = 0; y < blockSizeY; y++) {
                    for (int x = 0; x < blockSizeX; x++) {
                        int[] pixelCoordinates = pixelCoordinatesWithScale(x, y, scaleX, scaleY);
                        pixelColor = new Color(image.getRGB(pixelCoordinates[0], pixelCoordinates[1]));

                        int coordX = direction.getAxis() == Direction.Axis.X ? y : x;
                        int coordZ = direction.getAxis() == Direction.Axis.X ? x : y;

                        if (pixelColor.equals(previousPixelColor)) {
                            addBlockToWorld(previousBlock, world, centerPos, coordX*multiplier.getX(), coordZ*multiplier.getZ());
                        } else if (usedColors.containsKey(pixelColor)) {
                            addBlockToWorld(usedColors.get(pixelColor), world, centerPos, coordX*multiplier.getX(), coordZ*multiplier.getZ());
                        } else {
                            previousBlock = addBlockToWorld(pixelColor, colorData, world, centerPos, coordX*multiplier.getX(), coordZ*multiplier.getZ(), blackList);
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
            int west = 0;
            int north = 0;
            for (int i = 0; i < 128; i++) {
                int tempX = startPos.getX() - i;
                int tempZ = startPos.getZ() - i;

                if (tempX % 64 == 0 && (tempX+64) % 128 == 0) {
                    west = tempX;
                }
                if (tempZ % 64 == 0 && (tempZ+64) % 128 == 0) {
                    north = tempZ;
                }
            }

            int side1 = west;
            int side2 = side1 + 128;

            int side3 = north;
            int side4 = side3 + 128;

            int posX = (side1 + side2)/2;
            int posZ = (side3 + side4)/2;

            startPos = new Vec3i(posX, (int) playerPos.getY(), posZ);
        }
        return startPos;
    }

    private static int[] pixelCoordinatesWithScale(int x, int y, double scaleX, double scaleY) {
        int coordinateOffsetX = (int) (x%scaleX);
        int coordinateOffsetY = (int) (y%scaleY);
        int pixelCoordinateX = (int) ((x-coordinateOffsetX)/scaleX);
        int pixelCoordinateY = (int) ((y-coordinateOffsetY)/scaleY);
        return new int[]{pixelCoordinateX, pixelCoordinateY};
    }

    private static Vec3i calculatePositionToCenter(Vec3i startPos, Vec3i startDirection, int blockSizeX, int blockSizeY) {
        int directionalBlockSizeX = (blockSizeX/2) * startDirection.getX();
        int directionalBlockSizeZ = (blockSizeY/2) * startDirection.getZ();

        return new Vec3i(startPos.getX() + (directionalBlockSizeX > 0 ? directionalBlockSizeX - 1 : directionalBlockSizeX), startPos.getY(), startPos.getZ() + (directionalBlockSizeZ > 0 ? directionalBlockSizeZ - 1 : directionalBlockSizeZ));
    }


    private static Identifier addBlockToWorld(Color imagePixelColor, Map<Identifier, List<Color>> colorData, ServerWorld world, Vec3i pos, int x, int y, ArrayList<Identifier> blacklist) {
        Identifier bestMatch = ColorDataUtil.getBestPixelToBlockMatch(imagePixelColor, colorData, blacklist);
        addBlockToWorld(bestMatch, world, pos, x, y);
        return bestMatch;
    }

    private static Identifier addBlockToWorldMap(Color imagePixelColor, Map<Identifier, List<Color>> colorData, int[][] heightMap, ServerWorld world, Vec3i pos, int x, int y, ArrayList<Identifier> blacklist) {
        Pair<Identifier, Integer> bestMatch = ColorDataUtil.getBestPixelToMapColorMatch(imagePixelColor, colorData, blacklist, heightMap != null);
        addBlockToWorldMap(bestMatch.getLeft(), world, heightMap, pos, x, y);
        return bestMatch.getLeft();
    }

    private static BlockPos addBlockToWorld(Identifier blockId, ServerWorld world, Vec3i pos, int x, int y) {
        BlockPos blockPos = new BlockPos(pos.getX()+x, pos.getY(), pos.getZ()+y);
        Block block = Registries.BLOCK.get(blockId);
        world.setBlockState(blockPos, block.getDefaultState());

        if (block instanceof FallingBlock) {
            world.setBlockState(blockPos.down(1), Blocks.BLACK_CONCRETE.getDefaultState());
        }

        return blockPos;
    }
    private static BlockPos addBlockToWorldMap(Identifier blockId, ServerWorld world, int[][] heightMap, Vec3i pos, int x, int y) {
        BlockPos blockPos = new BlockPos(pos.getX()+x,  heightMap != null ? pos.getY() + heightMap[y][x] : pos.getY(), pos.getZ()+y);
        Block block = Registries.BLOCK.get(blockId);
        world.setBlockState(blockPos, block.getDefaultState());

        if (block instanceof FallingBlock) {
            world.setBlockState(blockPos.down(1), Blocks.BLACK_CONCRETE.getDefaultState());
        }
        return blockPos;
    }

    private static Pair<MapColor, MapColor.Brightness> addColorToMap(MapState state, Color imagePixelColor, Map<Identifier, MapColor> colorData, int x, int y, ArrayList<Identifier> blacklist) {
        Pair<MapColor, MapColor.Brightness> bestMatch = ColorDataUtil.getBestPixelToMapColorMatch(imagePixelColor, colorData, blacklist);
        addColorToMap(state, bestMatch.getLeft(), bestMatch.getRight(), x, y);
        return bestMatch;
    }

    private static void addColorToMap(MapState state, MapColor color, MapColor.Brightness brightness, int x, int y) {
        state.putColor(x, y, color.getRenderColorByte(brightness));
    }
}
