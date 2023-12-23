package net.iamaprogrammer.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.BoolArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.SimpleCommandExceptionType;
import net.fabricmc.loader.api.FabricLoader;
import net.iamaprogrammer.command.argument.HorizontalDirectionArgumentType;
import net.iamaprogrammer.command.argument.PathArgumentType;
import net.iamaprogrammer.command.argument.ScaleArgumentType;
import net.iamaprogrammer.util.ColorDataUtil;
import net.iamaprogrammer.util.LoggingUtil;
import net.iamaprogrammer.util.MapDataUtil;
import net.iamaprogrammer.util.MapToWorldData;
import net.minecraft.block.*;
import net.minecraft.command.CommandRegistryAccess;
import net.minecraft.command.argument.BlockStateArgumentType;
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
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
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

public class ImageCommand {
    private static final SimpleCommandExceptionType NOT_AN_IMAGE_EXCEPTION = new SimpleCommandExceptionType(Text.translatable("commands.place.feature.failed"));
    public static void register(CommandDispatcher<ServerCommandSource> dispatcher, CommandRegistryAccess commandRegistryAccess, CommandManager.RegistrationEnvironment registrationEnvironment) {
        dispatcher.register(CommandManager.literal("image").requires((source) -> source.hasPermissionLevel(2))
                .then(CommandManager.literal("paste")
                        .then(CommandManager.argument("imagePath", PathArgumentType.path())
                                .then(CommandManager.argument("scaleX", ScaleArgumentType.scale())
                                        .then(CommandManager.argument("scaleZ", ScaleArgumentType.scale())
                                                .then(CommandManager.argument("vertical", BoolArgumentType.bool())
                                                        .executes((context) -> ImageCommand.generate(context, PathArgumentType.getPath(context, "imagePath"), ScaleArgumentType.getScale(context, "scaleX"), ScaleArgumentType.getScale(context, "scaleZ"), true, Direction.NORTH))
                                                )
                                                .then(CommandManager.argument("direction", HorizontalDirectionArgumentType.direction())
                                                        .executes((context) -> ImageCommand.generate(context, PathArgumentType.getPath(context, "imagePath"), ScaleArgumentType.getScale(context, "scaleX"), ScaleArgumentType.getScale(context, "scaleZ"), false, HorizontalDirectionArgumentType.getDirection(context, "direction")))
                                                ).executes((context) -> ImageCommand.generate(context, PathArgumentType.getPath(context, "imagePath"), ScaleArgumentType.getScale(context, "scaleX"), ScaleArgumentType.getScale(context, "scaleZ"), false, Direction.NORTH))
                                        )
                                ).executes((context) -> ImageCommand.generate(context, PathArgumentType.getPath(context, "imagePath"), 1, 1, false, Direction.NORTH))
                        )
                )
                .then(CommandManager.literal("pasteToMap")
                        .then(CommandManager.argument("imagePath", PathArgumentType.path())
                                .then(CommandManager.argument("scaleX", ScaleArgumentType.scale())
                                        .then(CommandManager.argument("scaleZ", ScaleArgumentType.scale())
                                                .then(CommandManager.argument("direction", HorizontalDirectionArgumentType.direction())
                                                        .executes((context) -> ImageCommand.generateForMap(context, PathArgumentType.getPath(context, "imagePath"), ScaleArgumentType.getScale(context, "scaleX"), ScaleArgumentType.getScale(context, "scaleZ"), HorizontalDirectionArgumentType.getDirection(context, "direction"), false, true))
                                                )
                                                .then(CommandManager.argument("useStaircaseHeightMap", BoolArgumentType.bool())
                                                        .then(CommandManager.argument("useMapColors", BoolArgumentType.bool())
                                                                .executes((context) -> ImageCommand.generateForMap(context, PathArgumentType.getPath(context, "imagePath"), ScaleArgumentType.getScale(context, "scaleX"), ScaleArgumentType.getScale(context, "scaleZ"), Direction.NORTH, BoolArgumentType.getBool(context, "useStaircaseHeightMap"), BoolArgumentType.getBool(context, "useMapColors")))
                                                        ).executes((context) -> ImageCommand.generateForMap(context, PathArgumentType.getPath(context, "imagePath"), ScaleArgumentType.getScale(context, "scaleX"), ScaleArgumentType.getScale(context, "scaleZ"), Direction.NORTH, BoolArgumentType.getBool(context, "useStaircaseHeightMap"), true))
                                                ).executes((context) -> ImageCommand.generateForMap(context, PathArgumentType.getPath(context, "imagePath"), ScaleArgumentType.getScale(context, "scaleX"), ScaleArgumentType.getScale(context, "scaleZ"), Direction.NORTH, false, true))
                                        )
                                ).executes((context) -> ImageCommand.generateForMap(context, PathArgumentType.getPath(context, "imagePath"), 1, 1, Direction.NORTH, false, true))
                        )
                )
                .then(CommandManager.literal("give")
                        .then(CommandManager.argument("imagePath", PathArgumentType.path())
                                .then(CommandManager.argument("scaleX", ScaleArgumentType.scale())
                                        .then(CommandManager.argument("scaleZ", ScaleArgumentType.scale())
                                                .then(CommandManager.argument("direction", HorizontalDirectionArgumentType.direction())
                                                        .executes((context) -> ImageCommand.giveMap(context, PathArgumentType.getPath(context, "imagePath"), ScaleArgumentType.getScale(context, "scaleX"), ScaleArgumentType.getScale(context, "scaleZ"), HorizontalDirectionArgumentType.getDirection(context, "direction")))
                                                ).executes((context) -> ImageCommand.giveMap(context, PathArgumentType.getPath(context, "imagePath"), ScaleArgumentType.getScale(context, "scaleX"), ScaleArgumentType.getScale(context, "scaleZ"), Direction.NORTH))
                                        )
                                ).executes((context) -> ImageCommand.giveMap(context, PathArgumentType.getPath(context, "imagePath"), 1, 1, Direction.NORTH))
                        )
                )
                .then(CommandManager.literal("heightmap")
                        .then(CommandManager.argument("imagePath", PathArgumentType.path())
                                .then(CommandManager.argument("block", BlockStateArgumentType.blockState(commandRegistryAccess))
                                        .then(CommandManager.argument("scaleX", ScaleArgumentType.scale())
                                                .then(CommandManager.argument("scaleZ", ScaleArgumentType.scale())
                                                        .then(CommandManager.argument("scaleY", ScaleArgumentType.scale())
                                                                .then(CommandManager.argument("direction", HorizontalDirectionArgumentType.direction())
                                                                        .executes((context) -> ImageCommand.generateHeightMap(context, PathArgumentType.getPath(context, "imagePath"), BlockStateArgumentType.getBlockState(context, "block").getBlockState(), ScaleArgumentType.getScale(context, "scaleX"), ScaleArgumentType.getScale(context, "scaleZ"), ScaleArgumentType.getScale(context, "scaleY"), HorizontalDirectionArgumentType.getDirection(context, "direction")))
                                                                ).executes((context) -> ImageCommand.generateHeightMap(context, PathArgumentType.getPath(context, "imagePath"), BlockStateArgumentType.getBlockState(context, "block").getBlockState(), ScaleArgumentType.getScale(context, "scaleX"), ScaleArgumentType.getScale(context, "scaleZ"), ScaleArgumentType.getScale(context, "scaleY"), Direction.NORTH))
                                                        )
                                                        .then(CommandManager.argument("direction", HorizontalDirectionArgumentType.direction())
                                                                .executes((context) -> ImageCommand.generateHeightMap(context, PathArgumentType.getPath(context, "imagePath"), BlockStateArgumentType.getBlockState(context, "block").getBlockState(), ScaleArgumentType.getScale(context, "scaleX"), ScaleArgumentType.getScale(context, "scaleZ"), 1, HorizontalDirectionArgumentType.getDirection(context, "direction")))
                                                        ).executes((context) -> ImageCommand.generateHeightMap(context, PathArgumentType.getPath(context, "imagePath"), BlockStateArgumentType.getBlockState(context, "block").getBlockState(), ScaleArgumentType.getScale(context, "scaleX"), ScaleArgumentType.getScale(context, "scaleZ"), 1, Direction.NORTH))
                                                )
                                        ).executes((context) -> ImageCommand.generateHeightMap(context, PathArgumentType.getPath(context, "imagePath"), BlockStateArgumentType.getBlockState(context, "block").getBlockState(), 1, 1, 1, Direction.NORTH))
                                )
                        )
                )
        );
    }

    private static int generate(CommandContext<ServerCommandSource> context, String imagePath, double scaleX, double scaleZ, boolean vertical, Direction direction) throws CommandSyntaxException {
        return loadImage(context, imagePath, false, false, (fullPath, image, colorData) ->
                LoggingUtil.logTimeToComplete(context, () ->
                        loadImageAsBlockTextures(image, colorData, context, fullPath, scaleX, scaleZ, vertical, direction)));
    }
    private static int generateForMap(CommandContext<ServerCommandSource> context, String imagePath, double scaleX, double scaleZ, Direction direction, boolean useStaircaseHeightMap, boolean useMapColors) throws CommandSyntaxException {
        return loadImage(context, imagePath, useMapColors, useStaircaseHeightMap, (fullPath, image, colorData) ->
                LoggingUtil.logTimeToComplete(context, () ->
                        loadImageAsMap(image, colorData, context, fullPath, scaleX, scaleZ, direction, useStaircaseHeightMap, useMapColors)));
    }
    private static int giveMap(CommandContext<ServerCommandSource> context, String imagePath, double scaleX, double scaleZ, Direction direction) throws CommandSyntaxException {
        return loadImage(context, imagePath, (fullPath, image, colorData) ->
                LoggingUtil.logTimeToComplete(context, () ->
                        loadImageToMap(image, colorData, context, fullPath, scaleX, scaleZ, direction)));
    }
    private static int generateHeightMap(CommandContext<ServerCommandSource> context, String imagePath, BlockState blockState, double scaleX, double scaleZ, double scaleY, Direction direction) throws CommandSyntaxException {
        return loadImage(context, imagePath, true, false, (fullPath, image, colorData) ->
                LoggingUtil.logTimeToComplete(context, () ->
                        loadImageAsHeightMap(image, colorData, context, fullPath, blockState, scaleX, scaleZ, scaleY, direction)));
    }
    private static int loadImage(CommandContext<ServerCommandSource> context, String imagePath, boolean useMapColors, boolean useAllMapColors, CommandImageSupplier<Path> function) throws CommandSyntaxException {
        Path runFolder = FabricLoader.getInstance().getGameDir();
        Path fullPath = Path.of(runFolder.toString(), "images" + File.separator + imagePath);
        if (Files.exists(fullPath)) {
            context.getSource().sendFeedback(() -> Text.of("Image Exists"), true);
            File imageFile = new File(fullPath.toUri());
            try {
                BufferedImage image = ImageIO.read(new File(fullPath.toUri()));
                Map<Identifier, List<Color>> colorData = ColorDataUtil.loadColorData(useMapColors, useAllMapColors);

                tryThrowWithCondition(isNotImageFile(imageFile), NOT_AN_IMAGE_EXCEPTION.create());

                function.load(fullPath, image, colorData);
            } catch (IOException e) {
                System.out.println("An error occurred");
            }
            return 1;
        } else {
            context.getSource().sendFeedback(() -> Text.of("Image Does not Exist"), true);
            return -1;
        }
    }

    private static int loadImage(CommandContext<ServerCommandSource> context, String imagePath, CommandLoadToImageSupplier<Path> function) throws CommandSyntaxException {
        Path runFolder = FabricLoader.getInstance().getGameDir();
        Path fullPath = Path.of(runFolder.toString(), "images" + File.separator + imagePath);
        if (Files.exists(fullPath)) {
            context.getSource().sendFeedback(() -> Text.of("Image Exists"), true);
            File imageFile = new File(fullPath.toUri());
            try {
                BufferedImage image = ImageIO.read(new File(fullPath.toUri()));
                Map<Identifier, MapColor> colorData = ColorDataUtil.loadColorDataAsMapColor();
                tryThrowWithCondition(isNotImageFile(imageFile), NOT_AN_IMAGE_EXCEPTION.create());
                function.load(fullPath, image, colorData);
            } catch (IOException e) {
                System.out.println("An error occurred");
            }
            return 1;
        } else {
            context.getSource().sendFeedback(() -> Text.of("Image Does not Exist"), true);
            return -1;
        }
    }

    private static void tryThrowWithCondition(boolean condition, CommandSyntaxException exception) throws CommandSyntaxException {
        if (condition) {
            throw exception;
        }
    }
    private static boolean isNotImageFile(File file) throws IOException {
        String mimetype = Files.probeContentType(file.toPath());
        return mimetype == null || !mimetype.split("/")[0].equals("image");
    }
    private static void loadImageAsBlockTextures(BufferedImage image, Map<Identifier, List<Color>> colorData, CommandContext<ServerCommandSource> context, Path path, double scaleX, double scaleZ, boolean vertical, Direction direction) throws CommandSyntaxException {
        ServerPlayerEntity player = context.getSource().getPlayer();

        if (colorData != null && player != null) {
            MapToWorldData mapToWorldData = new MapToWorldData(image, player, direction, scaleX, scaleZ, false, vertical);

            Color pixelColor;
            Color previousPixelColor = null;
            Identifier previousBlock = null;
            Map<Color, Identifier> usedColors = new HashMap<>();

            int placedBlocks = 0;
            int previousPercentage = 0;
            for (int y = 0; y < mapToWorldData.getPixelToBlockSizeY(); y++) {
                for (int x = 0; x < mapToWorldData.getPixelToBlockSizeX(); x++) {

                    int log = LoggingUtil.logPercentageCompleted(context, placedBlocks, mapToWorldData.getSize(), previousPercentage);
                    previousPercentage = log != -1 ? log : previousPercentage;

                    int[] pixelCoordinates = pixelCoordinatesWithScale(x, y, scaleX, scaleZ);
                    pixelColor = new Color(image.getRGB(pixelCoordinates[0], pixelCoordinates[1]));

                    if (vertical) {
                        if (pixelColor.equals(previousPixelColor)) {
                            addBlockToWorldVertical(previousBlock, mapToWorldData.getWorld(), mapToWorldData.getCenterPosition(), x * mapToWorldData.getDirectionMultiplier().getZ(), y, mapToWorldData.getDirectionZ().getAxis());
                        } else if (usedColors.containsKey(pixelColor)) {
                            addBlockToWorldVertical(usedColors.get(pixelColor), mapToWorldData.getWorld(), mapToWorldData.getCenterPosition(), x * mapToWorldData.getDirectionMultiplier().getZ(), y, mapToWorldData.getDirectionZ().getAxis());
                        } else {
                            previousBlock = addBlockToWorldVertical(pixelColor, colorData, mapToWorldData.getWorld(), mapToWorldData.getCenterPosition(), x * mapToWorldData.getDirectionMultiplier().getZ(), y, mapToWorldData.getDirectionZ().getAxis());
                            previousPixelColor = pixelColor;
                            usedColors.put(previousPixelColor, previousBlock);
                        }
                    } else {
                        int coordX = direction.getAxis() == Direction.Axis.X ? y : x;
                        int coordZ = direction.getAxis() == Direction.Axis.X ? x : y;

                        if (pixelColor.equals(previousPixelColor)) {
                            addBlockToWorld(previousBlock, mapToWorldData.getWorld(), mapToWorldData.getCenterPosition(), coordX * mapToWorldData.getDirectionMultiplier().getX(), coordZ * mapToWorldData.getDirectionMultiplier().getZ());
                        } else if (usedColors.containsKey(pixelColor)) {
                            addBlockToWorld(usedColors.get(pixelColor), mapToWorldData.getWorld(), mapToWorldData.getCenterPosition(), coordX * mapToWorldData.getDirectionMultiplier().getX(), coordZ * mapToWorldData.getDirectionMultiplier().getZ());
                        } else {
                            previousBlock = addBlockToWorld(pixelColor, colorData, mapToWorldData.getWorld(), mapToWorldData.getCenterPosition(), coordX * mapToWorldData.getDirectionMultiplier().getX(), coordZ * mapToWorldData.getDirectionMultiplier().getZ());
                            previousPixelColor = pixelColor;
                            usedColors.put(previousPixelColor, previousBlock);
                        }
                    }
                    placedBlocks++;
                }
            }
        }
    }
    private static void loadImageAsMap(BufferedImage image, Map<Identifier, List<Color>> colorData, CommandContext<ServerCommandSource> context, Path path, double scaleX, double scaleZ, Direction direction, boolean useStaircaseHeightMap, boolean useMapColors) throws CommandSyntaxException {
        ServerPlayerEntity player =  context.getSource().getPlayer();

        if (colorData != null && player != null) {
            MapToWorldData mapToWorldData = new MapToWorldData(image, player, direction, scaleX, scaleZ, true, false);

            int[][] heightMap = null;
            if (useStaircaseHeightMap) {
                heightMap = MapDataUtil.generateHeightMap(mapToWorldData.getPixelToBlockSizeX(), mapToWorldData.getPixelToBlockSizeY(), scaleX, scaleZ, image, colorData, new ArrayList<>());
            }

            Color pixelColor;
            Color previousPixelColor = null;
            Identifier previousBlock = null;
            Map<Color, Identifier> usedColors = new HashMap<>();

            int placedBlocks = 0;
            int previousPercentage = 0;
            for (int y = 0; y < mapToWorldData.getPixelToBlockSizeY(); y++) {
                for (int x = 0; x < mapToWorldData.getPixelToBlockSizeX(); x++) {

                    int log = LoggingUtil.logPercentageCompleted(context, placedBlocks, mapToWorldData.getSize(), previousPercentage);
                    previousPercentage = log != -1 ? log : previousPercentage;

                    int[] pixelCoordinates = pixelCoordinatesWithScale(x, y, scaleX, scaleZ);
                    pixelColor = new Color(image.getRGB(pixelCoordinates[0], pixelCoordinates[1]));

                    int coordX = direction.getAxis() == Direction.Axis.X ? y : x;
                    int coordZ = direction.getAxis() == Direction.Axis.X ? x : y;

                    if (pixelColor.equals(previousPixelColor)) {
                        addBlockToWorldMap(previousBlock, mapToWorldData.getWorld(), heightMap, mapToWorldData.getCenterPosition(), coordX*mapToWorldData.getDirectionMultiplier().getX(), coordZ*mapToWorldData.getDirectionMultiplier().getZ());
                    } else if (usedColors.containsKey(pixelColor)) {
                        addBlockToWorldMap(usedColors.get(pixelColor), mapToWorldData.getWorld(), heightMap, mapToWorldData.getCenterPosition(), coordX*mapToWorldData.getDirectionMultiplier().getX(), coordZ*mapToWorldData.getDirectionMultiplier().getZ());
                    } else {
                        previousBlock = addBlockToWorldMap(pixelColor, colorData, heightMap, mapToWorldData.getWorld(), mapToWorldData.getCenterPosition(), coordX*mapToWorldData.getDirectionMultiplier().getX(), coordZ*mapToWorldData.getDirectionMultiplier().getZ());
                        previousPixelColor = pixelColor;
                        usedColors.put(previousPixelColor, previousBlock);
                    }
                    placedBlocks++;
                }
            }
        }
    }
    private static void loadImageToMap(BufferedImage image, Map<Identifier, MapColor> colorData, CommandContext<ServerCommandSource> context, Path path, double scaleX, double scaleZ, Direction direction) throws CommandSyntaxException {
        ServerPlayerEntity player = context.getSource().getPlayer();
        int DEFAULT_SCALE = 128;

        if (image.getWidth() > DEFAULT_SCALE || image.getHeight() > DEFAULT_SCALE) {
            double divisor = 2;
            while (true) {
                if (image.getWidth() > DEFAULT_SCALE && image.getWidth() * (1/divisor) <= DEFAULT_SCALE) {
                    scaleX = 1/divisor;
                }
                if (image.getHeight() > DEFAULT_SCALE && image.getHeight() * (1/divisor) <= DEFAULT_SCALE) {
                    scaleZ = 1/divisor;
                }
                if (image.getWidth()*scaleX <= DEFAULT_SCALE && image.getHeight()*scaleZ <= DEFAULT_SCALE) {
                    break;
                }
                divisor++;
            }
        }

        if (colorData != null && player != null) {
            MapToWorldData mapToWorldData = new MapToWorldData(image, player, direction, scaleX, scaleZ, true, false);

            Color pixelColor;
            Color previousPixelColor = null;
            Pair<MapColor, MapColor.Brightness> previousMapColor;
            Map<Color, Pair<MapColor, MapColor.Brightness>> usedColors = new HashMap<>();

            ItemStack stack = FilledMapItem.createMap(mapToWorldData.getWorld(), 0, 0, (byte)0, false, false);
            MapState state = FilledMapItem.getMapState(stack, mapToWorldData.getWorld());

            if (state != null) {
                for (int y = 0; y < mapToWorldData.getPixelToBlockSizeY(); y++) {
                    for (int x = 0; x < mapToWorldData.getPixelToBlockSizeX(); x++) {

                        int[] pixelCoordinates = pixelCoordinatesWithScale(x, y, scaleX, scaleZ);
                        pixelColor = new Color(image.getRGB(pixelCoordinates[0], pixelCoordinates[1]));

                        int coordX = direction.getAxis() == Direction.Axis.X ? y : x;
                        int coordZ = direction.getAxis() == Direction.Axis.X ? x : y;

                        if (pixelColor.equals(previousPixelColor)) {
                            addColorToMap(state, usedColors.get(pixelColor).getLeft(), usedColors.get(pixelColor).getRight(), coordX * mapToWorldData.getDirectionMultiplier().getX(), coordZ * mapToWorldData.getDirectionMultiplier().getZ());
                        } else if (usedColors.containsKey(pixelColor)) {
                            addColorToMap(state, usedColors.get(pixelColor).getLeft(), usedColors.get(pixelColor).getRight(), coordX * mapToWorldData.getDirectionMultiplier().getX(), coordZ * mapToWorldData.getDirectionMultiplier().getZ());
                        } else {
                            previousMapColor = addColorToMap(state, pixelColor, colorData, coordX * mapToWorldData.getDirectionMultiplier().getX(), coordZ * mapToWorldData.getDirectionMultiplier().getZ());
                            previousPixelColor = pixelColor;
                            usedColors.put(previousPixelColor, previousMapColor);
                        }
                    }
                }
                stack.getOrCreateNbt().putBoolean("map_to_lock", true);
                player.getInventory().insertStack(stack);
            } else {
                // TODO: throw error here
            }
        }
    }
    private static void loadImageAsHeightMap(BufferedImage image, Map<Identifier, List<Color>> colorData, CommandContext<ServerCommandSource> context, Path path, BlockState blockState, double scaleX, double scaleZ, double scaleY, Direction direction) throws CommandSyntaxException {
        ServerPlayerEntity player =  context.getSource().getPlayer();

        if (colorData != null && player != null) {
            MapToWorldData mapToWorldData = new MapToWorldData(image, player, direction, scaleX, scaleZ, true, false);

            Color pixelColor;
            int pixelColorNorth;
            int pixelColorSouth;
            int pixelColorEast;
            int pixelColorWest;

            int placedBlocks = 0;
            int previousPercentage = 0;
            for (int y = 0; y < mapToWorldData.getPixelToBlockSizeY(); y++) {
                for (int x = 0; x < mapToWorldData.getPixelToBlockSizeX(); x++) {

                    int log = LoggingUtil.logPercentageCompleted(context, placedBlocks, mapToWorldData.getSize(), previousPercentage);
                    previousPercentage = log != -1 ? log : previousPercentage;

                    int[] pixelCoordinates = pixelCoordinatesWithScale(x, y, scaleX, scaleZ);
                    pixelColor = new Color(image.getRGB(pixelCoordinates[0], pixelCoordinates[1]));

                    pixelColorNorth = getRelativePixelColor(image, x, y, mapToWorldData.getPixelToBlockSizeX(), mapToWorldData.getPixelToBlockSizeY(), scaleX, scaleZ, 0, -1);
                    pixelColorSouth = getRelativePixelColor(image, x, y, mapToWorldData.getPixelToBlockSizeX(), mapToWorldData.getPixelToBlockSizeY(), scaleX, scaleZ, 0, 1);
                    pixelColorEast = getRelativePixelColor(image, x, y, mapToWorldData.getPixelToBlockSizeX(), mapToWorldData.getPixelToBlockSizeY(), scaleX, scaleZ, 1, 0);
                    pixelColorWest = getRelativePixelColor(image, x, y, mapToWorldData.getPixelToBlockSizeX(), mapToWorldData.getPixelToBlockSizeY(), scaleX, scaleZ, -1, 0);

                    int lowest = Math.min(Math.min(pixelColorNorth, pixelColorSouth), Math.min(pixelColorEast, pixelColorWest));
                    int depth = Math.abs(pixelColor.getRed()-lowest);

                    int coordX = direction.getAxis() == Direction.Axis.X ? y : x;
                    int coordZ = direction.getAxis() == Direction.Axis.X ? x : y;

                    addBlockToHeightMap2(pixelColor, mapToWorldData.getWorld(), blockState, mapToWorldData.getCenterPosition(), scaleY, coordX*mapToWorldData.getDirectionMultiplier().getX(), coordZ*mapToWorldData.getDirectionMultiplier().getZ(), depth);
                    placedBlocks++;
                }
            }
        }
    }
    private static int[] pixelCoordinatesWithScale(int x, int y, double scaleX, double scaleZ) {
        int coordinateOffsetX = (int) (x%scaleX);
        int coordinateOffsetY = (int) (y%scaleZ);
        int pixelCoordinateX = (int) ((x-coordinateOffsetX)/scaleX);
        int pixelCoordinateY = (int) ((y-coordinateOffsetY)/scaleZ);
        return new int[]{pixelCoordinateX, pixelCoordinateY};
    }
    private static int getRelativePixelColor(BufferedImage image, int x, int y, int boundX, int boundY, double scaleX, double scaleZ, int offsetX, int offsetY) {
        if (y+offsetY >= 0 && y+offsetY < boundY && x+offsetX >= 0 && x+offsetX < boundX) {
            int[] pixelCoordinatesNorth = pixelCoordinatesWithScale(x+offsetX, y+offsetY, scaleX, scaleZ);
            return new Color(image.getRGB(pixelCoordinatesNorth[0], pixelCoordinatesNorth[1])).getRed();
        } else {
            return 255;
        }
    }
    private static Identifier addBlockToWorld(Color imagePixelColor, Map<Identifier, List<Color>> colorData, ServerWorld world, Vec3i pos, int x, int y) {
        Identifier bestMatch = ColorDataUtil.getBestPixelToBlockMatch(imagePixelColor, colorData);
        addBlockToWorld(bestMatch, world, pos, x, y);
        return bestMatch;
    }
    private static Identifier addBlockToWorldVertical(Color imagePixelColor, Map<Identifier, List<Color>> colorData, ServerWorld world, Vec3i pos, int x, int y, Direction.Axis axis) {
        Identifier bestMatch = ColorDataUtil.getBestPixelToBlockMatch(imagePixelColor, colorData);
        addBlockToWorldVertical(bestMatch, world, pos, x, y, axis);
        return bestMatch;
    }
    private static Identifier addBlockToWorldMap(Color imagePixelColor, Map<Identifier, List<Color>> colorData, int[][] heightMap, ServerWorld world, Vec3i pos, int x, int y) {
        Pair<Identifier, Integer> bestMatch = ColorDataUtil.getBestPixelToMapColorMatch(imagePixelColor, colorData, heightMap != null);
        addBlockToWorldMap(bestMatch.getLeft(), world, heightMap, pos, x, y);
        return bestMatch.getLeft();
    }
    private static void addBlockToHeightMap2(Color imagePixelColor, ServerWorld world, BlockState blockState, Vec3i pos, double scaleY, int x, int y, int depth) {
        if (depth != 0) {
            for (int i = 0; i < depth*scaleY; i++) {
                addBlockToHeightMap(blockState, world, pos, x, y, (int) ((imagePixelColor.getRed()*scaleY) - i));
            }
        } else {
            addBlockToHeightMap(blockState, world, pos, x, y, (int) (imagePixelColor.getRed()*scaleY));
        }
    }
    private static Pair<MapColor, MapColor.Brightness> addColorToMap(MapState state, Color imagePixelColor, Map<Identifier, MapColor> colorData, int x, int y) {
        Pair<MapColor, MapColor.Brightness> bestMatch = ColorDataUtil.getBestPixelToMapColorMatch(imagePixelColor, colorData);
        addColorToMap(state, bestMatch.getLeft(), bestMatch.getRight(), x, y);
        return bestMatch;
    }
    private static void addColorToMap(MapState state, MapColor color, MapColor.Brightness brightness, int x, int y) {
        state.putColor(x, y, color.getRenderColorByte(brightness));
    }
    private static void addBlockToWorld(Identifier blockId, ServerWorld world, Vec3i pos, int x, int y) {
        BlockPos blockPos = new BlockPos(pos.getX()+x, pos.getY(), pos.getZ()+y);
        Block block = Registries.BLOCK.get(blockId);
        world.setBlockState(blockPos, block.getDefaultState(), 2);

        if (block instanceof FallingBlock && world.getBlockState(blockPos).getBlock() instanceof AirBlock) {
            world.setBlockState(blockPos.down(1), Blocks.BLACK_CONCRETE.getDefaultState(), 2);
        }
    }
    private static void addBlockToWorldVertical(Identifier blockId, ServerWorld world, Vec3i pos, int x, int y, Direction.Axis axis) {
        BlockPos blockPos = axis == Direction.Axis.X ? new BlockPos(pos.getX(), pos.getY()-y, pos.getZ()+x) : new BlockPos(pos.getX()+x, pos.getY()-y, pos.getZ());
        Block block = Registries.BLOCK.get(blockId);
        world.setBlockState(blockPos, block.getDefaultState(), 2);

        if (block instanceof FallingBlock && world.getBlockState(blockPos).getBlock() instanceof AirBlock) {
            world.setBlockState(blockPos.down(1), Blocks.BLACK_CONCRETE.getDefaultState(), 2);
        }
    }
    private static void addBlockToHeightMap(BlockState blockState, ServerWorld world, Vec3i pos, int x, int z, int yOffset) {
        BlockPos blockPos = new BlockPos(pos.getX()+x, pos.getY()+yOffset, pos.getZ()+z);
        Block block = blockState.getBlock();
        world.setBlockState(blockPos, blockState, 2);

        if (block instanceof FallingBlock && world.getBlockState(blockPos).getBlock() instanceof AirBlock) {
            world.setBlockState(blockPos.down(1), Blocks.BLACK_CONCRETE.getDefaultState(), 2);
        }
    }
    private static void addBlockToWorldMap(Identifier blockId, ServerWorld world, int[][] heightMap, Vec3i pos, int x, int y) {
        BlockPos blockPos = new BlockPos(pos.getX()+x,  heightMap != null ? pos.getY() + heightMap[y][x] : pos.getY(), pos.getZ()+y);
        Block block = Registries.BLOCK.get(blockId);
        world.setBlockState(blockPos, block.getDefaultState(), 2);

        if (block instanceof FallingBlock && world.getBlockState(blockPos).getBlock() instanceof AirBlock) {
            world.setBlockState(blockPos.down(1), Blocks.BLACK_CONCRETE.getDefaultState(), 2);
        }
    }
}