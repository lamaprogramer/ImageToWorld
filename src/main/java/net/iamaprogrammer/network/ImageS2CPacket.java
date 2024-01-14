package net.iamaprogrammer.network;

import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.fabricmc.fabric.api.networking.v1.PacketByteBufs;
import net.fabricmc.fabric.api.networking.v1.PacketSender;
import net.fabricmc.loader.api.FabricLoader;
import net.iamaprogrammer.command.CommandExceptions;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayNetworkHandler;
import net.minecraft.network.PacketByteBuf;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

public class ImageS2CPacket {
    public static void receive(MinecraftClient client, ClientPlayNetworkHandler handler, PacketByteBuf buf, PacketSender responseSender) {
        String imagePath = buf.readString();

        Path runFolder = FabricLoader.getInstance().getGameDir();
        Path imageFolder = Path.of(runFolder.toString(), "images" + File.separator + imagePath);

        File imageFile = new File(imageFolder.toUri());

        try {
            tryThrowWithCondition(isNotImageFile(imageFile), CommandExceptions.NOT_AN_IMAGE_EXCEPTION.create(imageFile.getName()));

            BufferedImage image = ImageIO.read(imageFile);
            tryThrowWithCondition(image.getWidth() > 1024 || image.getHeight() > 1024, CommandExceptions.IMAGE_TOO_LARGE.create(imageFile.getName()));

            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            ImageIO.write(image, getFileExtension(imageFile), baos);
            byte[] imageBytes = baos.toByteArray();

            responseSender.sendPacket(NetworkingConstants.IMAGE_DATA_ID,
                    createPacketData(imageBytes, 0, ""));
        } catch (IOException | CommandSyntaxException e) {
            if (e instanceof CommandSyntaxException ex) {
                responseSender.sendPacket(NetworkingConstants.IMAGE_DATA_ID,
                        createPacketData(new byte[]{0}, 1, ex.getMessage()));
            } else {
                responseSender.sendPacket(NetworkingConstants.IMAGE_DATA_ID,
                        createPacketData(new byte[]{0}, 1, "File Not Found"));
            }
        }
    }

    private static PacketByteBuf createPacketData(byte[] image, int status, String message) {
        return PacketByteBufs.create()
                .writeByteArray(image)
                .writeVarInt(status)
                .writeString(message);
    }

    private static void tryThrowWithCondition(boolean condition, CommandSyntaxException exception) throws CommandSyntaxException {
        if (condition) {
            throw exception;
        }
    }

    public static String getFileExtension(File file) {
        String fileName = file.toString();
        int index = fileName.lastIndexOf('.');
        if(index > 0) {
            return fileName.substring(index + 1);
        }
        return "";
    }

    private static boolean isNotImageFile(File file) throws IOException {
        String mimetype = Files.probeContentType(file.toPath());
        return mimetype == null || !mimetype.split("/")[0].equals("image");
    }
}
