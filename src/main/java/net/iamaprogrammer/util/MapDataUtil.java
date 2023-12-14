package net.iamaprogrammer.util;

import net.minecraft.util.Identifier;
import net.minecraft.util.Pair;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class MapDataUtil {

    private static List<List<Pair<Identifier, Integer>>> create2dArrayList(int sizeX, int sizeY) {
        List<List<Pair<Identifier, Integer>>> arrayList = new ArrayList<>(sizeY);
        for (int i = 0; i < sizeY; i++) {
            arrayList.add(new ArrayList<>(sizeX));
        }
        return arrayList;
    }
    public static int[][] generateHeightMap(int sizeX, int sizeY, double scaleX, double scaleY, BufferedImage image, Map<Identifier, List<Color>> colorData, ArrayList<Identifier> blacklist) {
        int[][] heightMap = new int[sizeX][sizeY];

        int blockSizeX = (int) (image.getWidth() * scaleX);
        int blockSizeY = (int) (image.getHeight() * scaleY);

        Color previousPixelColor = null;
        int previousHeight = 0;
        Map<Color, Integer> usedColors = new HashMap<>();
        int lowestHeight = 0;

        int coordinateOffsetX;
        int coordinateOffsetY;
        int pixelCoordinateX;
        int pixelCoordinateY;
        int heightFromColorId;
        Color pixelColor;

        for (int y = 0; y < blockSizeY; y++) {
            for (int x = 0; x < blockSizeX; x++) {
                coordinateOffsetX = (int) (x%scaleX);
                coordinateOffsetY = (int) (y%scaleY);
                pixelCoordinateX = (int) ((x-coordinateOffsetX)/scaleX);
                pixelCoordinateY = (int) ((y-coordinateOffsetY)/scaleY);
                pixelColor = new Color(image.getRGB(pixelCoordinateX, pixelCoordinateY));

                if (previousPixelColor != null && previousPixelColor.equals(pixelColor)) {
                    heightFromColorId = previousHeight;
                } else if (usedColors.containsKey(pixelColor)) {
                    heightFromColorId = usedColors.get(pixelColor);
                } else {
                    heightFromColorId = mapColorIDToWorldHeightOffset(MapDataUtil.getBestPixelToMapHeight(pixelColor, colorData));

                    previousPixelColor = pixelColor;
                    previousHeight = heightFromColorId;
                    usedColors.put(pixelColor, heightFromColorId);
                }

                heightMap[y][x] = heightFromColorId;
                if (heightMap[y][x] < lowestHeight) {
                    lowestHeight = heightMap[y][x];
                }

                for (int i = 1; i <= y; i++) {
                    heightMap[y-i][x] += heightFromColorId;
                    if (heightMap[y-i][x] < lowestHeight) {
                        lowestHeight = heightMap[y-i][x];
                    }
                }
            }
        }

        for (int y2 = 0; y2 < heightMap.length; y2++) {
            for (int x2 = 0; x2 < heightMap[y2].length; x2++) {
                heightMap[y2][x2] -= lowestHeight;
            }
        }
        return heightMap;
    }

    public static int getBestPixelToMapHeight(Color imagePixelColor, Map<Identifier, List<Color>> colorData) {
        int previousSumDifference = 255+255+255;
        int mapColorId = -1;

        for (List<Color> colors : colorData.values()) {
            for (int colorIndex = 0; colorIndex < colors.size(); colorIndex++) {
                Color color = colors.get(colorIndex);
                int sumDifference = ColorDataUtil.getColorDifference(color, imagePixelColor);

                if (sumDifference < previousSumDifference) {
                    previousSumDifference = sumDifference;
                    mapColorId = colorIndex;
                }
            }
        }
        return mapColorId;
    }

    public static int mapColorIDToWorldHeightOffset(int id) {
        switch (id) {
            case 0 -> {return 1;}
            case 2 -> {return -1;}
            default -> {return 0;}
        }
    }
}
