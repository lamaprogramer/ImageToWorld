package net.iamaprogrammer.util;

import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.block.MapColor;
import net.minecraft.util.Identifier;
import net.minecraft.util.Pair;

import java.awt.*;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ColorDataUtil {
    public static Map<Identifier, List<Color>> loadColorData(boolean useMapColors, boolean useAllMapColors) throws IOException {
        if (useMapColors) {
            Path path = Path.of(FabricLoader.getInstance().getConfigDir().toString(), "imagetoworld" + File.separator + "mapdata.txt");

            Map<Identifier, List<Color>> data = new HashMap<>();
            if (Files.exists(path)) {
                String[] lines = Files.readString(path).split("\n");

                for (String line : lines) {
                    String[] splitLine = line.split("#");

                    Identifier blockId = new Identifier(splitLine[0]);
                    int colorId = Integer.parseInt(splitLine[1]);

                    List<Color> colors = new ArrayList<>();
                    if (useAllMapColors) {
                        for (int brightness = 0; brightness < 3; brightness++) {
                            colors.add(colorFromMapColor(MapColor.get(colorId), brightness));
                        }
                    } else {
                        colors.add(colorFromMapColor(MapColor.get(colorId), 1));
                    }
                    data.put(blockId, colors);
                }
                return data;
            }
        } else {
            Path path = Path.of(FabricLoader.getInstance().getConfigDir().toString(), "imagetoworld" + File.separator + "texturedata.txt");
            Map<Identifier, List<Color>> data = new HashMap<>();
            if (Files.exists(path)) {
                String[] lines = Files.readString(path).split("\n");

                for (String line : lines) {
                    String[] splitLine = line.split("#");
                    Identifier blockId = new Identifier(splitLine[0]);

                    String[] rgb = splitLine[1].split(" ");
                    data.put(blockId, List.of(getColor(rgb)));
                }

                return data;
            }
        }
        return null;
    }

    public static Map<Identifier, MapColor> loadColorDataAsMapColor() throws IOException {
        Path path = Path.of(FabricLoader.getInstance().getConfigDir().toString(), "imagetoworld" + File.separator + "mapdata.txt");

        Map<Identifier, MapColor> data = new HashMap<>();
        if (Files.exists(path)) {
            String[] lines = Files.readString(path).split("\n");

            for (String line : lines) {
                String[] splitLine = line.split("#");
                Identifier blockId = new Identifier(splitLine[0]);

                int colorId = Integer.parseInt(splitLine[1]);
                data.put(blockId, MapColor.get(colorId));
            }
            return data;
        }
        return null;
    }

    private static int mapColorBrightness(int color, int multiplier) {
        return (int) Math.floor((double) (color * multiplier) / 255);
    }

    private static Color colorFromMapColor(MapColor mapColor, int brightness) {
        int[] rgb = rgbFrom8bit(mapColor.color);
        return new Color(
                mapColorBrightness(rgb[0], MapColor.Brightness.validateAndGet(brightness).brightness),
                mapColorBrightness(rgb[1], MapColor.Brightness.validateAndGet(brightness).brightness),
                mapColorBrightness(rgb[2], MapColor.Brightness.validateAndGet(brightness).brightness)
        );
    }

    private static int[] rgbFrom8bit(int color) {
        int red =   (color & 0x00ff0000) >> 16;
        int green = (color & 0x0000ff00) >> 8;
        int blue =   color & 0x000000ff;
        return new int[]{red, green, blue};
    }

    public static Identifier getBestPixelToBlockMatch(Color imagePixelColor, Map<Identifier, List<Color>> colorData) {
        int previousSumDifference = 255+255+255;
        Identifier bestMatch = null;

        for (Identifier key : colorData.keySet()) {
            List<Color> colors = colorData.get(key);
            Color color = colors.get(0);

            int sumDifference = getColorDifference(color, imagePixelColor);
            if (sumDifference < previousSumDifference) {
                previousSumDifference = sumDifference;
                bestMatch = key;
            }
        }
        return bestMatch;
    }

    public static Pair<MapColor, MapColor.Brightness> getBestPixelToMapColorMatch(Color imagePixelColor, Map<Identifier, MapColor> colorData) {
        int previousSumDifference = 255+255+255;
        MapColor bestColor = null;
        MapColor.Brightness bestBrightness = null;

        for (Identifier key : colorData.keySet()) {
            MapColor mapColor = colorData.get(key);
            for (int brightness = 0; brightness < 3; brightness++) {
                Color color = colorFromMapColor(mapColor, brightness);
                int sumDifference = getColorDifference(color, imagePixelColor);

                if (sumDifference < previousSumDifference) {
                    previousSumDifference = sumDifference;
                    bestColor = mapColor;
                    bestBrightness = MapColor.Brightness.validateAndGet(brightness);
                }
            }
        }
        return new Pair<>(bestColor, bestBrightness);
    }

    public static Pair<Identifier, Integer> getBestPixelToMapColorMatch(Color imagePixelColor, Map<Identifier, List<Color>> colorData, boolean useAllColors) {
        int previousSumDifference = 255 + 255 + 255;
        Identifier bestMatch = null;
        int mapColorId = -1;

        for (Identifier key : colorData.keySet()) {
            List<Color> colors = colorData.get(key);
            if (useAllColors) {
                for (int colorIndex = 0; colorIndex < colors.size(); colorIndex++) {
                    Color color = colors.get(colorIndex);
                    int sumDifference = getColorDifference(color, imagePixelColor);

                    if (sumDifference < previousSumDifference) {
                        previousSumDifference = sumDifference;
                        bestMatch = key;
                        mapColorId = colorIndex;
                    }
                }
            } else {
                Color color = colors.get(0);
                int sumDifference = getColorDifference(color, imagePixelColor);

                if (sumDifference < previousSumDifference) {
                    previousSumDifference = sumDifference;
                    bestMatch = key;
                    mapColorId = 1;
                }
            }
        }
        return new Pair<>(bestMatch, mapColorId);
    }
    private static Color getColor(String[] rgb) {
        int r = Integer.parseInt(rgb[0]);
        int g = Integer.parseInt(rgb[1]);
        int b = Integer.parseInt(rgb[2]);
        return new Color(r, g, b);
    }
    public static int getColorDifference(Color color1, Color color2) {
        return Math.abs(color1.getRed() - color2.getRed()) +
                Math.abs(color1.getGreen() - color2.getGreen()) +
                Math.abs(color1.getBlue() - color2.getBlue());
    }
}
