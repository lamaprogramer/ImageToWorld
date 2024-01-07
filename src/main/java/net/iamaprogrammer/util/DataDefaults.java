package net.iamaprogrammer.util;

import net.fabricmc.loader.api.FabricLoader;
import net.iamaprogrammer.ImageToWorld;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;

public class DataDefaults {
    public static void loadDefaults() {
        Path configFolder = FabricLoader.getInstance().getConfigDir();

        Path textureDataPath = Path.of(configFolder.toString(), "imagetoworld" + File.separator + "texturedata.txt");
        Path mapDataPath = Path.of(configFolder.toString(), "imagetoworld" + File.separator + "mapdata.txt");

        if (!(Files.exists(textureDataPath) && Files.exists(mapDataPath))) {
            InputStream textureData = ImageToWorld.class.getResourceAsStream("/defaultTextureData.txt");
            writeToFile(textureDataPath, textureData);

            InputStream mapData = ImageToWorld.class.getResourceAsStream("/defaultMapData.txt");
            writeToFile(mapDataPath, mapData);
        }
    }

    private static void writeToFile(Path path, InputStream fileData) {
        if (!Files.exists(path)) {
            try {
                Files.createDirectories(path.getParent());
                copyInputStreamToFile(fileData, path.toFile());
            } catch (IOException ignored) {}
        }
    }

    private static void copyInputStreamToFile(InputStream inputStream, File file) throws IOException {
        try (FileOutputStream outputStream = new FileOutputStream(file, false)) {
            int read;
            byte[] bytes = new byte[1024];
            while ((read = inputStream.read(bytes)) != -1) {
                outputStream.write(bytes, 0, read);
            }
        }
    }
}
