package net.iamaprogrammer.util;

import net.fabricmc.loader.api.FabricLoader;
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
    public static Map<Identifier, List<Color>> loadAverageColorData(boolean useMapColors, boolean useAllMapColors) throws IOException {
        Path path = Path.of(FabricLoader.getInstance().getConfigDir().toString(), "imagetoworld" + File.separator + "texturedata.txt");
        Map<Identifier, List<Color>> data = new HashMap<>();
        if (Files.exists(path)) {
            String[] lines = Files.readString(path).split("\n");

            for (String line : lines) {
                String[] splitLine = line.split("#");
                Identifier blockId = new Identifier(splitLine[0]);

                if (useMapColors) {
                    if (splitLine.length >= 3) {
                        String[] mapColors = splitLine[2].split(",");
                        if (useAllMapColors) {
                            data.put(blockId, getMapColors(mapColors));
                        } else {
                            if (mapColors.length >= 2) {
                                data.put(blockId, List.of(getColor(mapColors[1].split(" "))));
                            }
                        }
                    }
                } else {
                    String[] rgb = splitLine[1].split(" ");
                    data.put(blockId, List.of(getColor(rgb)));
                }
            }
            return data;
        }
        return null;
    }

    public static Identifier getBestPixelToBlockMatch(Color imagePixelColor, Map<Identifier, List<Color>> colorData, ArrayList<Identifier> blacklist) {
        int previousSumDifference = 255+255+255;
        Identifier bestMatch = null;

        for (Identifier key : colorData.keySet()) {
            if (!blacklist.contains(key)) {
                List<Color> colors = colorData.get(key);

                Color color = colors.get(0);
                int sumDifference = getColorDifference(color, imagePixelColor);

                if (sumDifference < previousSumDifference) {
                    previousSumDifference = sumDifference;
                    bestMatch = key;
                }
            }
        }
        return bestMatch;
    }

    public static Pair<Identifier, Integer> getBestPixelToMapColorMatch(Color imagePixelColor, Map<Identifier, List<Color>> colorData, ArrayList<Identifier> blacklist, boolean useAllColors) {
        int previousSumDifference = 255 + 255 + 255;
        Identifier bestMatch = null;
        int mapColorId = -1;

        for (Identifier key : colorData.keySet()) {
            if (!blacklist.contains(key)) {
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
        }
        return new Pair<>(bestMatch, mapColorId);
    }

    private static ArrayList<Color> getMapColors(String[] mapColors) {
        ArrayList<Color> colors = new ArrayList<>();
        for (String mapColor : mapColors) {
            String[] mapRGB = mapColor.split(" ");
            colors.add(getColor(mapRGB));
        }
        return colors;
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
