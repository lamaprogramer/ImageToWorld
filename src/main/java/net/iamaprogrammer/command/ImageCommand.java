package net.iamaprogrammer.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.BoolArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.fabricmc.fabric.api.networking.v1.PacketByteBufs;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.iamaprogrammer.command.argument.HorizontalDirectionArgumentType;
import net.iamaprogrammer.command.argument.ImagePathArgumentType;
import net.iamaprogrammer.command.argument.ScaleArgumentType;
import net.iamaprogrammer.network.NetworkingConstants;
import net.iamaprogrammer.network.packets.ImageDataPacket;
import net.iamaprogrammer.util.*;
import net.minecraft.block.*;
import net.minecraft.command.CommandRegistryAccess;
import net.minecraft.command.argument.BlockPosArgumentType;
import net.minecraft.command.argument.BlockStateArgumentType;
import net.minecraft.command.argument.EntityArgumentType;
import net.minecraft.item.FilledMapItem;
import net.minecraft.item.ItemStack;
import net.minecraft.item.map.MapState;
import net.minecraft.network.codec.PacketCodecs;
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
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.List;
import java.util.*;

public class ImageCommand {
    public static void register(CommandDispatcher<ServerCommandSource> dispatcher, CommandRegistryAccess commandRegistryAccess, CommandManager.RegistrationEnvironment registrationEnvironment) {
        dispatcher.register(CommandManager.literal("image").requires((source) -> source.hasPermissionLevel(3))
                .then(CommandManager.literal("paste")
                        .then(CommandManager.argument("imagePath", ImagePathArgumentType.image())
                                .then(CommandManager.argument("position", BlockPosArgumentType.blockPos())
                                        .then(CommandManager.argument("scaleX", ScaleArgumentType.scale())
                                                .then(CommandManager.argument("scaleZ", ScaleArgumentType.scale())
                                                        .then(CommandManager.argument("vertical", BoolArgumentType.bool())
                                                                .executes((context) -> ImageCommand.generate(context, BlockPosArgumentType.getBlockPos(context, "position"), ImagePathArgumentType.getImage(context, "imagePath"), ScaleArgumentType.getScale(context, "scaleX"), ScaleArgumentType.getScale(context, "scaleZ"), true, Direction.NORTH))
                                                        )
                                                        .then(CommandManager.argument("direction", HorizontalDirectionArgumentType.direction())
                                                                .executes((context) -> ImageCommand.generate(context, BlockPosArgumentType.getBlockPos(context, "position"), ImagePathArgumentType.getImage(context, "imagePath"), ScaleArgumentType.getScale(context, "scaleX"), ScaleArgumentType.getScale(context, "scaleZ"), false, HorizontalDirectionArgumentType.getDirection(context, "direction")))
                                                        ).executes((context) -> ImageCommand.generate(context, BlockPosArgumentType.getBlockPos(context, "position"), ImagePathArgumentType.getImage(context, "imagePath"), ScaleArgumentType.getScale(context, "scaleX"), ScaleArgumentType.getScale(context, "scaleZ"), false, Direction.NORTH))
                                                )
                                        ).executes((context) -> ImageCommand.generate(context, BlockPosArgumentType.getBlockPos(context, "position"), ImagePathArgumentType.getImage(context, "imagePath"), 1, 1, false, Direction.NORTH))
                                )
                        )
                )
                .then(CommandManager.literal("pasteToMap")
                        .then(CommandManager.argument("imagePath", ImagePathArgumentType.image())
                                .then(CommandManager.argument("position", BlockPosArgumentType.blockPos())
                                        .then(CommandManager.argument("scaleX", ScaleArgumentType.scale())
                                                .then(CommandManager.argument("scaleZ", ScaleArgumentType.scale())
                                                        .then(CommandManager.argument("direction", HorizontalDirectionArgumentType.direction())
                                                                .executes((context) -> ImageCommand.generateForMap(context, BlockPosArgumentType.getBlockPos(context, "position"), ImagePathArgumentType.getImage(context, "imagePath"), ScaleArgumentType.getScale(context, "scaleX"), ScaleArgumentType.getScale(context, "scaleZ"), HorizontalDirectionArgumentType.getDirection(context, "direction"), false))
                                                        )
                                                        .then(CommandManager.argument("useStaircaseHeightMap", BoolArgumentType.bool())
                                                                .executes((context) -> ImageCommand.generateForMap(context, BlockPosArgumentType.getBlockPos(context, "position"), ImagePathArgumentType.getImage(context, "imagePath"), ScaleArgumentType.getScale(context, "scaleX"), ScaleArgumentType.getScale(context, "scaleZ"), Direction.NORTH, BoolArgumentType.getBool(context, "useStaircaseHeightMap")))
                                                        ).executes((context) -> ImageCommand.generateForMap(context, BlockPosArgumentType.getBlockPos(context, "position"), ImagePathArgumentType.getImage(context, "imagePath"), ScaleArgumentType.getScale(context, "scaleX"), ScaleArgumentType.getScale(context, "scaleZ"), Direction.NORTH, false))
                                                )
                                        ).executes((context) -> ImageCommand.generateForMap(context, BlockPosArgumentType.getBlockPos(context, "position"), ImagePathArgumentType.getImage(context, "imagePath"), 1, 1, Direction.NORTH, false))
                                )
                        )
                )
                .then(CommandManager.literal("heightmap")
                        .then(CommandManager.argument("imagePath", ImagePathArgumentType.image())
                                .then(CommandManager.argument("block", BlockStateArgumentType.blockState(commandRegistryAccess))
                                        .then(CommandManager.argument("position", BlockPosArgumentType.blockPos())
                                                .then(CommandManager.argument("scaleX", ScaleArgumentType.scale())
                                                        .then(CommandManager.argument("scaleZ", ScaleArgumentType.scale())
                                                                .then(CommandManager.argument("scaleY", ScaleArgumentType.scale())
                                                                        .then(CommandManager.argument("direction", HorizontalDirectionArgumentType.direction())
                                                                                .executes((context) -> ImageCommand.generateHeightMap(context, BlockPosArgumentType.getBlockPos(context, "position"), ImagePathArgumentType.getImage(context, "imagePath"), BlockStateArgumentType.getBlockState(context, "block").getBlockState(), ScaleArgumentType.getScale(context, "scaleX"), ScaleArgumentType.getScale(context, "scaleZ"), ScaleArgumentType.getScale(context, "scaleY"), HorizontalDirectionArgumentType.getDirection(context, "direction")))
                                                                        ).executes((context) -> ImageCommand.generateHeightMap(context, BlockPosArgumentType.getBlockPos(context, "position"), ImagePathArgumentType.getImage(context, "imagePath"), BlockStateArgumentType.getBlockState(context, "block").getBlockState(), ScaleArgumentType.getScale(context, "scaleX"), ScaleArgumentType.getScale(context, "scaleZ"), ScaleArgumentType.getScale(context, "scaleY"), Direction.NORTH))
                                                                )
                                                                .then(CommandManager.argument("direction", HorizontalDirectionArgumentType.direction())
                                                                        .executes((context) -> ImageCommand.generateHeightMap(context, BlockPosArgumentType.getBlockPos(context, "position"), ImagePathArgumentType.getImage(context, "imagePath"), BlockStateArgumentType.getBlockState(context, "block").getBlockState(), ScaleArgumentType.getScale(context, "scaleX"), ScaleArgumentType.getScale(context, "scaleZ"), 1, HorizontalDirectionArgumentType.getDirection(context, "direction")))
                                                                ).executes((context) -> ImageCommand.generateHeightMap(context, BlockPosArgumentType.getBlockPos(context, "position"), ImagePathArgumentType.getImage(context, "imagePath"), BlockStateArgumentType.getBlockState(context, "block").getBlockState(), ScaleArgumentType.getScale(context, "scaleX"), ScaleArgumentType.getScale(context, "scaleZ"), 1, Direction.NORTH))
                                                        )
                                                ).executes((context) -> ImageCommand.generateHeightMap(context, BlockPosArgumentType.getBlockPos(context, "position"), ImagePathArgumentType.getImage(context, "imagePath"), BlockStateArgumentType.getBlockState(context, "block").getBlockState(), 1, 1, 1, Direction.NORTH))
                                        )
                                )
                        )
                )
                .then(CommandManager.literal("give")
                        .then(CommandManager.argument("target", EntityArgumentType.players())
                                .then(CommandManager.argument("imagePath", ImagePathArgumentType.image())
                                        .then(CommandManager.argument("scaleX", ScaleArgumentType.scale())
                                                .then(CommandManager.argument("scaleZ", ScaleArgumentType.scale())
                                                        .executes((context) -> ImageCommand.giveMap(context, EntityArgumentType.getPlayers(context, "target"), ImagePathArgumentType.getImage(context, "imagePath"), ScaleArgumentType.getScale(context, "scaleX"), ScaleArgumentType.getScale(context, "scaleZ")))
                                                )
                                        ).executes((context) -> ImageCommand.giveMap(context, EntityArgumentType.getPlayers(context, "target"), ImagePathArgumentType.getImage(context, "imagePath"), 1, 1))
                                )
                        )
                )
        );
    }

    private static int generate(CommandContext<ServerCommandSource> context, BlockPos position, String imagePath, double scaleX, double scaleZ, boolean vertical, Direction direction) throws CommandSyntaxException {
        return loadImage(context, position, imagePath, false, false, (image, blockPos, colorData) ->
                LoggingUtil.logTimeToComplete(context, () ->
                        loadImageWithBlockTextures(context, image, blockPos, colorData, scaleX, scaleZ, vertical, direction)));
    }

    private static int generateForMap(CommandContext<ServerCommandSource> context, BlockPos position, String imagePath, double scaleX, double scaleZ, Direction direction, boolean useStaircaseHeightMap) throws CommandSyntaxException {
        return loadImage(context, position, imagePath, true, useStaircaseHeightMap, (image, blockPos, colorData) ->
                LoggingUtil.logTimeToComplete(context, () ->
                        loadImageAsMap(context, image, blockPos, colorData, scaleX, scaleZ, direction, useStaircaseHeightMap)));
    }

    private static int generateHeightMap(CommandContext<ServerCommandSource> context, BlockPos position, String imagePath, BlockState blockState, double scaleX, double scaleZ, double scaleY, Direction direction) throws CommandSyntaxException {
        return loadImage(context, position, imagePath, true, false, (image, blockPos, colorData) ->
                LoggingUtil.logTimeToComplete(context, () ->
                        loadImageAsHeightMap(context, image, blockPos, blockState, scaleX, scaleZ, scaleY, direction)));
    }

    private static int giveMap(CommandContext<ServerCommandSource> context, Collection<ServerPlayerEntity> players, String imagePath, double scaleX, double scaleZ) throws CommandSyntaxException {
        return loadImage(context, players, imagePath, (image, player, colorData) ->
                LoggingUtil.logTimeToComplete(context, () ->
                        loadImageToMap(context, image, player, colorData, scaleX, scaleZ, Direction.NORTH)));
    }

    private static int loadImage(CommandContext<ServerCommandSource> context, BlockPos position, String imagePath, boolean useMapColors, boolean useAllMapColors, CommandImageSupplier function) throws CommandSyntaxException {
        try {
            Map<Identifier, List<Color>> colorData = ColorDataUtil.loadColorData(useMapColors, useAllMapColors);

            if (colorData == null) {
                DataDefaults.loadDefaults();
                colorData = ColorDataUtil.loadColorData(useMapColors, useAllMapColors);
            }
            tryThrowWithCondition(colorData == null, CommandExceptions.COLOR_DATA_MISSING.create());

            Map<Identifier, List<Color>> finalColorData = colorData;
            ServerPlayNetworking.registerReceiver(Objects.requireNonNull(context.getSource().getPlayer()).networkHandler,
                    ImageDataPacket.PACKET_ID, (payload, networkContext) -> {
                        ByteArrayInputStream stream = new ByteArrayInputStream(payload.imageBytes());
                        int errorId = payload.status();
                        String message = payload.message();

                        if (errorId != 0) {
                            context.getSource().sendError(Text.of(message));
                        } else {
                            try {
                                BufferedImage image = ImageIO.read(stream);
                                function.load(image, position, finalColorData);
                            } catch (IOException e) {
                                context.getSource().sendError(Text.of("Failed to read image."));
                            }
                        }
                        ServerPlayNetworking.unregisterReceiver(networkContext.player().networkHandler,
                                NetworkingConstants.IMAGE_DATA_ID);
                    });
            //ServerPlayNetworking.send(Objects.requireNonNull(context.getSource().getPlayer()), PacketCodecs.STRING.);
            ServerPlayNetworking.send(Objects.requireNonNull(context.getSource().getPlayer()),
                    new ImageDataPacket(imagePath, new byte[0], 0, "")
            );
        } catch (IOException e) {
            throw CommandExceptions.IMAGE_PROCESSING_EXCEPTION.create();
        }
        return 1;
    }

    private static int loadImage(CommandContext<ServerCommandSource> context, Collection<ServerPlayerEntity> players, String imagePath, CommandLoadToImageSupplier function) throws CommandSyntaxException {
        try {
            Map<Identifier, MapColor> colorData = ColorDataUtil.loadColorDataAsMapColor();
            tryThrowWithCondition(colorData == null, CommandExceptions.COLOR_DATA_MISSING.create());

            for (ServerPlayerEntity player : players.stream().toList()) {
                ServerPlayNetworking.registerReceiver(Objects.requireNonNull(player).networkHandler,
                        ImageDataPacket.PACKET_ID, (payload, networkContext) -> {
                            ByteArrayInputStream stream = new ByteArrayInputStream(payload.imageBytes());
                            int errorId = payload.status();
                            String message = payload.message();

                            if (errorId != 0) {
                                context.getSource().sendError(Text.of(message));
                            } else {
                                try {
                                    BufferedImage image = ImageIO.read(stream);
                                    function.load(image, networkContext.player(), colorData);
                                } catch (IOException e) {
                                    context.getSource().sendError(Text.of(e.getMessage()));
                                }
                            }
                            ServerPlayNetworking.unregisterReceiver(networkContext.player().networkHandler,
                                    ImageDataPacket.PACKET_ID.id());
                        });
                ServerPlayNetworking.send(Objects.requireNonNull(context.getSource().getPlayer()),
                        new ImageDataPacket(imagePath, new byte[0], 0, "")
                );
            }
        } catch (IOException e) {
            throw CommandExceptions.IMAGE_PROCESSING_EXCEPTION.create();
        }
        return 1;
    }

    private static void tryThrowWithCondition(boolean condition, CommandSyntaxException exception) throws CommandSyntaxException {
        if (condition) {
            throw exception;
        }
    }

    private static void loadImageWithBlockTextures(CommandContext<ServerCommandSource> context, BufferedImage image, BlockPos blockPos, Map<Identifier, List<Color>> colorData, double scaleX, double scaleZ, boolean vertical, Direction direction) {
        MapToWorldData mapToWorldData = new MapToWorldData(context, image, blockPos, direction, scaleX, scaleZ, false, vertical);

        Color pixelColor;
        Color previousPixelColor = null;
        Identifier previousBlock = null;
        Map<Color, Identifier> usedColorCache = new HashMap<>();

        int placedBlocks = 0;
        int previousPercentage = 0;

        for (int y = 0; y < mapToWorldData.getPixelToBlockSizeY(); y++) {
            for (int x = 0; x < mapToWorldData.getPixelToBlockSizeX(); x++) {

                int percentageUntilComplete = LoggingUtil.logPercentageCompleted(context, placedBlocks, mapToWorldData.getSize(), previousPercentage);
                previousPercentage = percentageUntilComplete != -1 ? percentageUntilComplete : previousPercentage;

                int[] scaledPixelCoordinates = pixelCoordinatesWithScale(x, y, scaleX, scaleZ);
                pixelColor = new Color(image.getRGB(scaledPixelCoordinates[0], scaledPixelCoordinates[1]));

                if (vertical) {
                    if (pixelColor.equals(previousPixelColor)) {
                        addBlockToWorldVertical(previousBlock, mapToWorldData.getWorld(), mapToWorldData.getCenterPosition(), x * mapToWorldData.getDirectionMultiplier().getZ(), y, mapToWorldData.getDirectionZ().getAxis());
                    } else if (usedColorCache.containsKey(pixelColor)) {
                        addBlockToWorldVertical(usedColorCache.get(pixelColor), mapToWorldData.getWorld(), mapToWorldData.getCenterPosition(), x * mapToWorldData.getDirectionMultiplier().getZ(), y, mapToWorldData.getDirectionZ().getAxis());
                    } else {
                        previousBlock = addBlockToWorldVertical(pixelColor, colorData, mapToWorldData.getWorld(), mapToWorldData.getCenterPosition(), x * mapToWorldData.getDirectionMultiplier().getZ(), y, mapToWorldData.getDirectionZ().getAxis());
                        previousPixelColor = pixelColor;
                        usedColorCache.put(previousPixelColor, previousBlock);
                    }
                } else {
                    int coordX = direction.getAxis() == Direction.Axis.X ? y : x;
                    int coordZ = direction.getAxis() == Direction.Axis.X ? x : y;

                    if (pixelColor.equals(previousPixelColor)) {
                        addBlockToWorld(previousBlock, mapToWorldData.getWorld(), mapToWorldData.getCenterPosition(), coordX * mapToWorldData.getDirectionMultiplier().getX(), coordZ * mapToWorldData.getDirectionMultiplier().getZ());
                    } else if (usedColorCache.containsKey(pixelColor)) {
                        addBlockToWorld(usedColorCache.get(pixelColor), mapToWorldData.getWorld(), mapToWorldData.getCenterPosition(), coordX * mapToWorldData.getDirectionMultiplier().getX(), coordZ * mapToWorldData.getDirectionMultiplier().getZ());
                    } else {
                        previousBlock = addBlockToWorld(pixelColor, colorData, mapToWorldData.getWorld(), mapToWorldData.getCenterPosition(), coordX * mapToWorldData.getDirectionMultiplier().getX(), coordZ * mapToWorldData.getDirectionMultiplier().getZ());
                        previousPixelColor = pixelColor;
                        usedColorCache.put(previousPixelColor, previousBlock);
                    }
                }
                placedBlocks++;
            }
        }
    }

    private static void loadImageAsMap(CommandContext<ServerCommandSource> context, BufferedImage image, BlockPos blockPos, Map<Identifier, List<Color>> colorData, double scaleX, double scaleZ, Direction direction, boolean useStaircaseHeightMap) {
        MapToWorldData mapToWorldData = new MapToWorldData(context, image, blockPos, direction, scaleX, scaleZ, true, false);

        int[][] heightMap = null;
        if (useStaircaseHeightMap) {
            heightMap = MapDataUtil.generateHeightMap(mapToWorldData.getPixelToBlockSizeX(), mapToWorldData.getPixelToBlockSizeY(), scaleX, scaleZ, image, colorData);
        }

        Color pixelColor;
        Color previousPixelColor = null;
        Identifier previousBlock = null;
        Map<Color, Identifier> usedColorCache = new HashMap<>();

        int placedBlocks = 0;
        int previousPercentage = 0;
        for (int y = 0; y < mapToWorldData.getPixelToBlockSizeY(); y++) {
            for (int x = 0; x < mapToWorldData.getPixelToBlockSizeX(); x++) {

                int percentageUntilComplete = LoggingUtil.logPercentageCompleted(context, placedBlocks, mapToWorldData.getSize(), previousPercentage);
                previousPercentage = percentageUntilComplete != -1 ? percentageUntilComplete : previousPercentage;

                int[] scaledPixelCoordinates = pixelCoordinatesWithScale(x, y, scaleX, scaleZ);
                pixelColor = new Color(image.getRGB(scaledPixelCoordinates[0], scaledPixelCoordinates[1]));

                int coordX = direction.getAxis() == Direction.Axis.X ? y : x;
                int coordZ = direction.getAxis() == Direction.Axis.X ? x : y;

                if (pixelColor.equals(previousPixelColor)) {
                    addBlockToWorldMap(previousBlock, mapToWorldData.getWorld(), heightMap, mapToWorldData.getCenterPosition(), coordX*mapToWorldData.getDirectionMultiplier().getX(), coordZ*mapToWorldData.getDirectionMultiplier().getZ());
                } else if (usedColorCache.containsKey(pixelColor)) {
                    addBlockToWorldMap(usedColorCache.get(pixelColor), mapToWorldData.getWorld(), heightMap, mapToWorldData.getCenterPosition(), coordX*mapToWorldData.getDirectionMultiplier().getX(), coordZ*mapToWorldData.getDirectionMultiplier().getZ());
                } else {
                    previousBlock = addBlockToWorldMap(pixelColor, colorData, heightMap, mapToWorldData.getWorld(), mapToWorldData.getCenterPosition(), coordX*mapToWorldData.getDirectionMultiplier().getX(), coordZ*mapToWorldData.getDirectionMultiplier().getZ());
                    previousPixelColor = pixelColor;
                    usedColorCache.put(previousPixelColor, previousBlock);
                }
                placedBlocks++;
            }
        }
    }

    private static void loadImageToMap(CommandContext<ServerCommandSource> context, BufferedImage image, ServerPlayerEntity player, Map<Identifier, MapColor> colorData, double scaleX, double scaleZ, Direction direction){
        double[] autoScaled = autoScale(image, 128, scaleX, scaleZ);
        scaleX = autoScaled[0];
        scaleZ = autoScaled[1];

        if (colorData != null && player != null) {
            MapToWorldData mapToWorldData = new MapToWorldData(context, image, player.getBlockPos(), direction, scaleX, scaleZ, true, false);

            Color pixelColor;
            Color previousPixelColor = null;
            Pair<MapColor, MapColor.Brightness> previousMapColor;
            Map<Color, Pair<MapColor, MapColor.Brightness>> usedColors = new HashMap<>();

            ItemStack stack = LockedMap.createLockedMap(mapToWorldData.getWorld(), (byte)0);
            MapState state = FilledMapItem.getMapState(stack, mapToWorldData.getWorld());

            if (state != null) {
                for (int y = 0; y < mapToWorldData.getPixelToBlockSizeY(); y++) {
                    for (int x = 0; x < mapToWorldData.getPixelToBlockSizeX(); x++) {

                        int[] pixelCoordinates = pixelCoordinatesWithScale(x, y, scaleX, scaleZ);
                        pixelColor = new Color(image.getRGB(pixelCoordinates[0], pixelCoordinates[1]));

                        int posX = x + (128 - mapToWorldData.getPixelToBlockSizeX())/2;
                        int posY = y + (128 - mapToWorldData.getPixelToBlockSizeY())/2;


                        if (pixelColor.equals(previousPixelColor)) {
                            addColorToMap(state, usedColors.get(pixelColor).getLeft(), usedColors.get(pixelColor).getRight(), posX, posY);
                        } else if (usedColors.containsKey(pixelColor)) {
                            addColorToMap(state, usedColors.get(pixelColor).getLeft(), usedColors.get(pixelColor).getRight(), posX, posY);
                        } else {
                            previousMapColor = addColorToMap(state, pixelColor, colorData, posX, posY);
                            previousPixelColor = pixelColor;
                            usedColors.put(previousPixelColor, previousMapColor);
                        }

                    }
                }
                player.getInventory().insertStack(stack);
            }
        }
    }

    private static void loadImageAsHeightMap(CommandContext<ServerCommandSource> context, BufferedImage image, BlockPos blockPos, BlockState blockState, double scaleX, double scaleZ, double scaleY, Direction direction) {
        MapToWorldData mapToWorldData = new MapToWorldData(context, image, blockPos, direction, scaleX, scaleZ, false, false);

        Color pixelColor;
        int pixelColorNorth;
        int pixelColorSouth;
        int pixelColorEast;
        int pixelColorWest;

        int placedBlocks = 0;
        int previousPercentage = 0;
        for (int y = 0; y < mapToWorldData.getPixelToBlockSizeY(); y++) {
            for (int x = 0; x < mapToWorldData.getPixelToBlockSizeX(); x++) {

                int percentageUntilComplete = LoggingUtil.logPercentageCompleted(context, placedBlocks, mapToWorldData.getSize(), previousPercentage);
                previousPercentage = percentageUntilComplete != -1 ? percentageUntilComplete : previousPercentage;

                int[] scaledPixelCoordinates = pixelCoordinatesWithScale(x, y, scaleX, scaleZ);
                pixelColor = new Color(image.getRGB(scaledPixelCoordinates[0], scaledPixelCoordinates[1]));

                pixelColorNorth = getRelativePixelColor(image, x, y, mapToWorldData.getPixelToBlockSizeX(), mapToWorldData.getPixelToBlockSizeY(), scaleX, scaleZ, 0, -1);
                pixelColorSouth = getRelativePixelColor(image, x, y, mapToWorldData.getPixelToBlockSizeX(), mapToWorldData.getPixelToBlockSizeY(), scaleX, scaleZ, 0, 1);
                pixelColorEast = getRelativePixelColor(image, x, y, mapToWorldData.getPixelToBlockSizeX(), mapToWorldData.getPixelToBlockSizeY(), scaleX, scaleZ, 1, 0);
                pixelColorWest = getRelativePixelColor(image, x, y, mapToWorldData.getPixelToBlockSizeX(), mapToWorldData.getPixelToBlockSizeY(), scaleX, scaleZ, -1, 0);

                int lowest = Math.min(Math.min(pixelColorNorth, pixelColorSouth), Math.min(pixelColorEast, pixelColorWest));
                int depth = Math.abs(pixelColor.getRed()-lowest);

                int coordX = direction.getAxis() == Direction.Axis.X ? y : x;
                int coordZ = direction.getAxis() == Direction.Axis.X ? x : y;

                addBlockToHeightMap(pixelColor, mapToWorldData.getWorld(), blockState, mapToWorldData.getCenterPosition(), scaleY, coordX*mapToWorldData.getDirectionMultiplier().getX(), coordZ*mapToWorldData.getDirectionMultiplier().getZ(), depth);
                placedBlocks++;
            }
        }
    }

    private static double[] autoScale(BufferedImage image, int maxSize, double scaleX, double scaleZ) {
        if (image.getWidth() > maxSize || image.getHeight() > maxSize) {
            double divisor = 2;
            while (true) {
                if (image.getWidth() > maxSize && image.getWidth() * (1/divisor) <= maxSize) {
                    scaleX = 1/divisor;
                }
                if (image.getHeight() > maxSize && image.getHeight() * (1/divisor) <= maxSize) {
                    scaleZ = 1/divisor;
                }
                if (image.getWidth()*scaleX <= maxSize && image.getHeight()*scaleZ <= maxSize) {
                    break;
                }
                divisor++;
            }
        }
        return new double[]{scaleX, scaleZ};
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

    private static void addBlockToWorld(Identifier blockId, ServerWorld world, Vec3i pos, int x, int y) {
        BlockPos blockPos = new BlockPos(pos.getX()+x, pos.getY(), pos.getZ()+y);
        Block block = Registries.BLOCK.get(blockId);
        world.setBlockState(blockPos, block.getDefaultState(), 2);

        placeScaffoldingIfNeeded(world, block, blockPos);
    }

    private static Identifier addBlockToWorldVertical(Color imagePixelColor, Map<Identifier, List<Color>> colorData, ServerWorld world, Vec3i pos, int x, int y, Direction.Axis axis) {
        Identifier bestMatch = ColorDataUtil.getBestPixelToBlockMatch(imagePixelColor, colorData);
        addBlockToWorldVertical(bestMatch, world, pos, x, y, axis);
        return bestMatch;
    }

    private static void addBlockToWorldVertical(Identifier blockId, ServerWorld world, Vec3i pos, int x, int y, Direction.Axis axis) {
        BlockPos blockPos = axis == Direction.Axis.X ? new BlockPos(pos.getX(), pos.getY()-y, pos.getZ()+x) : new BlockPos(pos.getX()+x, pos.getY()-y, pos.getZ());
        Block block = Registries.BLOCK.get(blockId);
        world.setBlockState(blockPos, block.getDefaultState(), 2);

        placeScaffoldingIfNeeded(world, block, blockPos);
    }

    private static Identifier addBlockToWorldMap(Color imagePixelColor, Map<Identifier, List<Color>> colorData, int[][] heightMap, ServerWorld world, Vec3i pos, int x, int y) {
        Pair<Identifier, Integer> bestMatch = ColorDataUtil.getBestPixelToMapColorMatch(imagePixelColor, colorData, heightMap != null);
        addBlockToWorldMap(bestMatch.getLeft(), world, heightMap, pos, x, y);
        return bestMatch.getLeft();
    }

    private static void addBlockToWorldMap(Identifier blockId, ServerWorld world, int[][] heightMap, Vec3i pos, int x, int y) {
        BlockPos blockPos = new BlockPos(pos.getX()+x,  heightMap != null ? pos.getY() + heightMap[y][x] : pos.getY(), pos.getZ()+y);
        Block block = Registries.BLOCK.get(blockId);
        world.setBlockState(blockPos, block.getDefaultState(), 2);

        placeScaffoldingIfNeeded(world, block, blockPos);
    }

    private static void addBlockToHeightMap(BlockState blockState, ServerWorld world, Vec3i pos, int x, int z, int yOffset) {
        BlockPos blockPos = new BlockPos(pos.getX()+x, pos.getY()+yOffset, pos.getZ()+z);
        Block block = blockState.getBlock();
        world.setBlockState(blockPos, blockState, 2);

        placeScaffoldingIfNeeded(world, block, blockPos);
    }

    private static void addBlockToHeightMap(Color imagePixelColor, ServerWorld world, BlockState blockState, Vec3i pos, double scaleY, int x, int y, int depth) {
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

    private static void placeScaffoldingIfNeeded(ServerWorld world, Block block, BlockPos blockPos) {
        if (block instanceof FallingBlock && world.getBlockState(blockPos.down(1)).getBlock() instanceof AirBlock) {
            world.setBlockState(blockPos.down(1), Blocks.BLACK_CONCRETE.getDefaultState(), 2);
        }
    }
}