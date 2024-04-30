package net.iamaprogrammer.network.packets;

import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.networking.v1.PacketByteBufs;
import net.fabricmc.fabric.api.networking.v1.PacketSender;
import net.fabricmc.loader.api.FabricLoader;
import net.iamaprogrammer.command.CommandExceptions;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.codec.PacketCodecs;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Identifier;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

public record ImageDataPacket(String path, byte[] imageBytes, int status, String message) implements CustomPayload {
    public static final CustomPayload.Id<ImageDataPacket> PACKET_ID = new CustomPayload.Id<>(new Identifier("imagetoworld", "imagedata"));
    public static final PacketCodec<RegistryByteBuf, ImageDataPacket> PACKET_CODEC = PacketCodec.tuple(
            PacketCodecs.STRING, ImageDataPacket::path,
            PacketCodecs.BYTE_ARRAY, ImageDataPacket::imageBytes,
            PacketCodecs.VAR_INT, ImageDataPacket::status,
            PacketCodecs.STRING, ImageDataPacket::message,
            ImageDataPacket::new
    );

    public static void receiveClient(ImageDataPacket payload, ClientPlayNetworking.Context context) {
        String imagePath = payload.path();

        Path runFolder = FabricLoader.getInstance().getGameDir();
        Path imageFolder = Path.of(runFolder.toString(), "images" + File.separator + imagePath);

        File imageFile = new File(imageFolder.toUri());

        PacketSender responseSender = context.responseSender();
        try {
            tryThrowWithCondition(isNotImageFile(imageFile), CommandExceptions.NOT_AN_IMAGE_EXCEPTION.create(imageFile.getName()));

            BufferedImage image = ImageIO.read(imageFile);
            tryThrowWithCondition(image.getWidth() > 1024 || image.getHeight() > 1024, CommandExceptions.IMAGE_TOO_LARGE.create(imageFile.getName()));

            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            ImageIO.write(image, getFileExtension(imageFile), baos);
            byte[] imageBytes = baos.toByteArray();

            responseSender.sendPacket(new ImageDataPacket(imagePath, imageBytes, 0, ""));
        } catch (IOException | CommandSyntaxException e) {
            if (e instanceof CommandSyntaxException ex) {
                responseSender.sendPacket(new ImageDataPacket(imagePath, new byte[]{0}, 1, ex.getMessage()));
            } else {
                responseSender.sendPacket(new ImageDataPacket(imagePath, new byte[]{0}, 1, "File Not Found"));
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

    @Override
    public Id<? extends CustomPayload> getId() {
        return PACKET_ID;
    }
}
