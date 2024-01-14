package net.iamaprogrammer.util;

import com.mojang.brigadier.context.CommandContext;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3i;

import java.awt.image.BufferedImage;

public class MapToWorldData {
    private final ServerWorld world;
    private final Direction directionX;
    private final Direction directionZ;
    private final Vec3i startPosition;
    private final Vec3i startDirection;
    private final int pixelToBlockSizeX;
    private final int pixelToBlockSizeY;
    private final int size;
    private final Vec3i centerPosition;
    private final Vec3i directionMultiplier;

    public MapToWorldData(CommandContext<ServerCommandSource> context, BufferedImage image, BlockPos blockPos, Direction direction, double scaleX, double scaleZ, boolean snapToMapGrid, boolean vertical) {
        ServerPlayerEntity player = context.getSource().getPlayer();
        this.world = context.getSource().getWorld();

        if (vertical) {
            this.directionZ = player != null ? player.getHorizontalFacing() : Direction.NORTH;
            this.directionX = this.directionZ.rotateYCounterclockwise();

            Vec3i startDirection = this.directionZ.getVector().add(this.directionX.getVector());
            this.directionMultiplier = this.directionZ.getOpposite().getVector().add(this.directionX.getOpposite().getVector());

            this.pixelToBlockSizeX = (int) (image.getWidth() * scaleX);
            this.pixelToBlockSizeY = (int) (image.getHeight() * scaleZ);
            this.size = this.pixelToBlockSizeX * this.pixelToBlockSizeY;

            this.startPosition = getStartPos(snapToMapGrid, blockPos).up(this.pixelToBlockSizeY-1);

            this.startDirection = new Vec3i(startDirection.getX(), 0, startDirection.getZ());
            Vec3i centeredPosition = calculatePositionToCenter(this.startPosition, this.startDirection, this.pixelToBlockSizeX, this.pixelToBlockSizeY);
            this.centerPosition = directionZ.getAxis() == Direction.Axis.X ? new Vec3i(this.startPosition.getX(), this.startPosition.getY(), centeredPosition.getZ()) : new Vec3i(centeredPosition.getX(), this.startPosition.getY(), this.startPosition.getZ());
        } else {
            this.directionZ = direction;
            this.directionX = this.directionZ.rotateYCounterclockwise();

            this.startDirection = this.directionZ.getVector().add(this.directionX.getVector());
            this.directionMultiplier = this.directionZ.getOpposite().getVector().add(this.directionX.getOpposite().getVector());

            this.startPosition = getStartPos(snapToMapGrid, blockPos);

            this.pixelToBlockSizeX = (int) (image.getWidth() * scaleX);
            this.pixelToBlockSizeY = (int) (image.getHeight() * scaleZ);
            this.size = this.pixelToBlockSizeX * this.pixelToBlockSizeY;

            this.centerPosition = calculatePositionToCenter(this.startPosition, this.startDirection, this.pixelToBlockSizeX, this.pixelToBlockSizeY);
        }
    }

    public ServerWorld getWorld() {
        return world;
    }

    public Direction getDirectionX() {
        return directionX;
    }

    public Direction getDirectionZ() {
        return directionZ;
    }

    public Vec3i getStartPosition() {
        return startPosition;
    }

    public Vec3i getStartDirection() {
        return startDirection;
    }

    public Vec3i getCenterPosition() {
        return centerPosition;
    }

    public Vec3i getDirectionMultiplier() {
        return directionMultiplier;
    }

    public int getSize() {
        return size;
    }

    public int getPixelToBlockSizeX() {
        return pixelToBlockSizeX;
    }

    public int getPixelToBlockSizeY() {
        return pixelToBlockSizeY;
    }

    private static Vec3i getStartPos(boolean snapToMapGrid, BlockPos blockPos) {
        Vec3i startPos = blockPos;

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

            startPos = new Vec3i(posX, startPos.getY(), posZ);
        }
        return startPos;
    }

    private static Vec3i calculatePositionToCenter(Vec3i startPos, Vec3i startDirection, int blockSizeX, int blockSizeY) {
        int directionalBlockSizeX = (blockSizeX/2) * startDirection.getX();
        int directionalBlockSizeZ = (blockSizeY/2) * startDirection.getZ();

        return new Vec3i(startPos.getX() + (directionalBlockSizeX > 0 ? directionalBlockSizeX - 1 : directionalBlockSizeX), startPos.getY(), startPos.getZ() + (directionalBlockSizeZ > 0 ? directionalBlockSizeZ - 1 : directionalBlockSizeZ));
    }
}