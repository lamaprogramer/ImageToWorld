package net.iamaprogrammer.event;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientLifecycleEvents;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.block.Block;
import net.minecraft.client.MinecraftClient;
import net.minecraft.registry.Registries;
import net.minecraft.resource.Resource;
import net.minecraft.util.Identifier;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.List;

public class ClientStartedHandler implements ClientLifecycleEvents.ClientStarted {
    @Override
    public void onClientStarted(MinecraftClient client) {
        this.generateAverageColorData(client);
    }

    private void generateAverageColorData(MinecraftClient client) {
        Map<Identifier, Resource> resources = client.getResourceManager().findResources("models/block", path -> true);
        List<Identifier> blockIds = Registries.BLOCK.getIds().stream().toList();
        Map<Identifier, Block> output = new HashMap<>();
        StringBuilder fileData = new StringBuilder();
        //System.out.println(blockIds);

        Path configFolder = FabricLoader.getInstance().getConfigDir();
        Path path = Path.of(configFolder.toString(), "imagetoworld" + File.separator + "texturedata.txt");

        for (Identifier blockId : blockIds) {
            resources.forEach((modelId, resource) -> {
                try {
                    String modelName = Path.of(modelId.getPath()).getFileName().toString();
                    modelName = modelName.substring(0, modelName.indexOf("."));
//                    if (blockId.getNamespace().equals("promenade")) {
//                        System.out.println(modelId);
//                    }
                    if (modelId.getPath().contains(blockId.getPath()) && modelId.getNamespace().equals(blockId.getNamespace())) {
                        if (filterBlockModels(blockIds, blockId, modelId, modelName)) {
                            if (!output.containsKey(blockId)) {
                                output.put(blockId, Registries.BLOCK.get(blockId));
                                Map<Identifier, Identifier> textureIds = this.getTextureIds(resource, blockId);

                                if (textureIds.get(blockId) != null) {
                                    Optional<Resource> imageResource = client.getResourceManager().getResource(textureIds.get(blockId));
                                    if (imageResource.isPresent()) {
                                        BufferedImage image = ImageIO.read(imageResource.get().getInputStream());
                                        Color average = this.calculateImageAverage(image);
                                        if (average != null) {
                                            fileData.append(blockId + "#" + average.getRed() + " " + average.getGreen() + " " + average.getBlue() + "\n");
                                            //System.out.println(blockId + ": " + average);
                                        }
                                    }
                                }
                            }
                        }
                    }
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
            });
        }

        this.writeToFile(path, fileData);
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
        int likelyNonFullBlock = 0;
        for (int y = 0; y < image.getHeight()-1; y+=resolutionY) {
            for (int x = 0; x < image.getWidth()-1; x+=resolutionX) {
                Color color = new Color(image.getRGB(x, y), true);
                int alpha = color.getAlpha();

                // Check if textures could be full block
                if (x == 1 || y == 1 || x == image.getWidth()-1 || y == image.getHeight()-1) { // Bounds of image
                    if (alpha != 0) {
                        likelyNonFullBlock = 0;
                    } else {
                        likelyNonFullBlock++;
                        if (likelyNonFullBlock == 3) {
                            return null;
                        }
                    }
                }

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
