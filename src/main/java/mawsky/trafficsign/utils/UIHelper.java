package main.java.mawsky.trafficsign.utils;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.util.ArrayList;

public class UIHelper {

    public static BufferedImage createHoughSpaceImage(int[][] houghArray){
        int width = houghArray.length;
        int heigth = houghArray[0].length;
        BufferedImage houghSpaceImage = new BufferedImage(width, heigth, BufferedImage.TYPE_BYTE_GRAY);

        int maxVotes = Integer.MIN_VALUE;
        for (int x = 0; x < width; x++){
            for (int y = 0; y < heigth; y++){
                if (houghArray[x][y] > maxVotes) maxVotes = houghArray[x][y];
            }
        }

        if (maxVotes == 0) return houghSpaceImage;

        for (int x = 0; x < width; x++) {
            for (int y = 0; y < heigth; y++) {
                int votes = houghArray[x][y];

                int gray = (int) (((double) votes / maxVotes) * 255.0);

                int rgb = (gray >> 16) | (gray >> 8) | gray;
                houghSpaceImage.setRGB(x, y, rgb);
            }
        }

        return houghSpaceImage;
    }

    public static BufferedImage createPyramidFromSmallestToFound(ArrayList<BufferedImage> pyramid, int foundIndex, BufferedImage found) {
        if (pyramid == null || pyramid.isEmpty()) return null;

        // Sicherheitshalber den Index im gültigen Bereich halten
        if (foundIndex < 0) foundIndex = 0;
        if (foundIndex >= pyramid.size()) foundIndex = pyramid.size() - 1;

        int padding = 20;
        int totalWidth = 0;
        int maxHeight = 0;

        // 1. Maße berechnen: Von 0 (kleinstes Bild) bis foundIndex
        for (int i = 0; i <= foundIndex; i++) {
            BufferedImage img = pyramid.get(i);
            totalWidth += img.getWidth() + padding;
            maxHeight = Math.max(maxHeight, img.getHeight() + 40); // 40px Platz für Beschriftung
        }

        // 2. Ziel-Canvas mit ALPHA-Kanal (Transparenz) erstellen
        BufferedImage canvas = new BufferedImage(totalWidth, maxHeight, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g2d = canvas.createGraphics();

        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        // 3. Bilder von Index 0 bis foundIndex nebeneinander zeichnen
        int currentX = 10;
        for (int i = 0; i <= foundIndex; i++) {
            BufferedImage img = pyramid.get(i);
            boolean isFoundLevel = (i == foundIndex);

            // Bild zeichnen
            if (isFoundLevel) g2d.drawImage(found, currentX, 35, null);
            else g2d.drawImage(img, currentX, 35, null);

            // Rahmen zeichnen (Grün für den Treffer!)
            if (isFoundLevel) {
                g2d.setColor(Color.GREEN);
                g2d.setStroke(new BasicStroke(3));
            } else {
                g2d.setColor(Color.GRAY);
                g2d.setStroke(new BasicStroke(1));
            }
            g2d.drawRect(currentX - 1, 34, img.getWidth() + 2, img.getHeight() + 2);

            // Beschriftung
            g2d.setColor(isFoundLevel ? Color.GREEN : Color.BLACK);
            g2d.setFont(new Font("SansSerif", Font.BOLD, 12));

            String label = "Index " + i + " (" + img.getWidth() + "x" + img.getHeight() + ")";
            if (isFoundLevel) label += " [GEFUNDEN]";

            g2d.drawString(label, currentX, 25);

            currentX += img.getWidth() + padding;
        }

        g2d.dispose();
        return canvas;
    }
}
