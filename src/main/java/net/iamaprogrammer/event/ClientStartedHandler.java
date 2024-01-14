package net.iamaprogrammer.event;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientLifecycleEvents;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.block.MapColor;
import net.minecraft.client.MinecraftClient;
import net.minecraft.util.registry.Registry;
import net.minecraft.resource.Resource;
import net.minecraft.util.Identifier;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.*;

public class ClientStartedHandler implements ClientLifecycleEvents.ClientStarted {
    @Override
    public void onClientStarted(MinecraftClient client) {
        this.generateColorData(client);
    }

    private void generateColorData(MinecraftClient client) {
        Map<Identifier, Resource> resources = client.getResourceManager().findResources("models/block", path -> true);
        List<Identifier> blockIds = Registry.BLOCK.getIds().stream().toList();
        Path configFolder = FabricLoader.getInstance().getConfigDir();

        List<Identifier> blockIdBlacklist = new ArrayList<>();
        List<MapColor> mapColorBlacklist = new ArrayList<>();

        StringBuilder fileData = new StringBuilder();
        StringBuilder mapData = new StringBuilder();

        Path textureDataPath = Path.of(configFolder.toString(), "imagetoworld" + File.separator + "texturedata.txt");
        Path mapDataPath = Path.of(configFolder.toString(), "imagetoworld" + File.separator + "mapdata.txt");

        if (!(Files.exists(textureDataPath) && Files.exists(mapDataPath))) {
            for (Identifier blockId : blockIds) {
                for (Identifier modelId : resources.keySet()) {
                    Resource resource = resources.get(modelId);
                    try {
                        String modelName = Path.of(modelId.getPath()).getFileName().toString();
                        modelName = modelName.substring(0, modelName.indexOf("."));
                        if (modelId.getPath().contains(blockId.getPath()) && modelId.getNamespace().equals(blockId.getNamespace())) {
                            if (this.filterBlockModels(blockIds, blockId, modelId, modelName)) {
                                if (!blockIdBlacklist.contains(blockId)) {
                                    blockIdBlacklist.add(blockId);
                                    Map<Identifier, Identifier> textureIds = this.getTextureIds(resource, blockId);
                                    if (textureIds != null) {
                                        if (textureIds.get(blockId) != null) {
                                            if (blockId.getNamespace().equals("minecraft")) {
                                                MapColor color = Registry.BLOCK.get(blockId).getDefaultMapColor();
                                                if (!mapColorBlacklist.contains(color) && color != MapColor.CLEAR) {
                                                    mapColorBlacklist.add(color);

                                                    String colorData = this.mapColors(blockId, color) + "\n";
                                                    mapData.append(colorData);
                                                }
                                            }
                                            Optional<Resource> imageResource = client.getResourceManager().getResource(textureIds.get(blockId));
                                            if (imageResource.isPresent()) {
                                                BufferedImage image = ImageIO.read(imageResource.get().getInputStream());
                                                Color average = this.calculateImageAverage(image);

                                                String data = this.averageImageColor(blockId, average) + "\n";
                                                fileData.append(data);
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    } catch (Exception e) {
                        throw new RuntimeException(e);
                    }
                }
            }

            this.writeToFile(mapDataPath, mapData);
            this.writeToFile(textureDataPath, fileData);
        }
    }

    private String averageImageColor(Identifier blockId, Color averageColor) {
        return blockId + "#" + averageColor.getRed() + " " + averageColor.getGreen() + " " + averageColor.getBlue();
    }
    private String mapColors(Identifier blockId, MapColor color) {
        int colorId = color.id;
        return blockId + "#" + colorId;
    }
    private boolean filterBlockModels(List<Identifier> allBlockIds, Identifier blockId, Identifier modelId, String modelName) {
        String[] strings = modelName.contains("_") ? modelName.split("_") : new String[]{modelName};

        String temp = "";
        String largestCombination = "";
        int combinations = 0;
        for (int i = 0; i < strings.length; i++) {
            for (int j = i; j < strings.length; j++) {
                temp += !temp.isEmpty() ? "_" + strings[j] : strings[j];

                if (temp.equals(blockId.getPath()) || allBlockIds.contains(new Identifier(modelId.getNamespace(), temp))) {
                    if (temp.length() > largestCombination.length()) {
                        largestCombination = temp;
                    }
                    combinations++;
                }
            }
            temp = "";
            if (largestCombination.equals(blockId.getPath())) {
                break;
            }
        }
        return combinations == 1 || largestCombination.equals(blockId.getPath());
    }

    private Map<Identifier, Identifier> getTextureIds(Resource resource, Identifier blockId) throws IOException {
        Gson gson = new Gson();
        JsonObject data = gson.fromJson(resource.getReader(), JsonObject.class);
        if (data.has("parent")) {
            String modelType = this.idToName(data.get("parent").getAsString());
            if (this.isValidModelType(modelType)) {
                JsonObject textures = data.get("textures").getAsJsonObject();
                Set<String> textureKeys = textures.keySet();

                // change to map for block specification
                Map<Identifier, Identifier> textureIds = new HashMap<>();
                for (String textureKey : textureKeys) {
                    String id = textures.get(textureKey).getAsString()
                            .replace("block/", "textures/block/")
                            .replace("item/", "textures/item/") + ".png";

                    if (!id.contains("#")) {
                        textureIds.put(blockId, new Identifier(id));
                    }
                }
                return textureIds;
            }

        }
        return null;
    }

    private boolean isValidModelType(String model) {
        return model.contains("cube") ||
                model.contains("orientable") ||
                model.equals("leaves");
    }
    private String idToPath(String id) {
        return id.contains(":") ? id.split(":")[1] : id;
    }
    private String pathToName(String path) {
        return path.contains("/") ? path.substring(path.lastIndexOf("/"), path.length()-1) : path;
    }
    private String idToName(String id) {
        return this.pathToName(this.idToPath(id));
    }

    private void writeToFile(Path path, StringBuilder fileData) {
        if (!Files.exists(path)) {
            try {
                Files.createDirectories(path.getParent());
                Files.writeString(path, fileData.toString());
            } catch (IOException ignored) {}
        }
    }
    private Color calculateImageAverage(BufferedImage image) {
        int resolutionX = 1;
        int resolutionY = 1;

        int redSum = 0;
        int greenSum = 0;
        int blueSum = 0;
        int blankPixels = 0;
        for (int y = 0; y < image.getHeight()-1; y+=resolutionY) {
            for (int x = 0; x < image.getWidth()-1; x+=resolutionX) {
                Color color = new Color(image.getRGB(x, y), true);
                int alpha = color.getAlpha();

                if (alpha != 0) {
                    redSum += color.getRed();
                    greenSum += color.getGreen();
                    blueSum += color.getBlue();
                } else {
                    blankPixels++;
                }
            }
        }
        redSum = redSum / this.averageResolution(image, resolutionX, resolutionY, blankPixels);
        greenSum = greenSum / this.averageResolution(image, resolutionX, resolutionY, blankPixels);
        blueSum = blueSum / this.averageResolution(image, resolutionX, resolutionY, blankPixels);
        return new Color(redSum, greenSum, blueSum);
    }
    private int averageResolution(BufferedImage image, int resolutionX, int resolutionY, int blankPixels) {
        return (image.getHeight() / resolutionY) * (image.getWidth() / resolutionX) - blankPixels;
    }
}
