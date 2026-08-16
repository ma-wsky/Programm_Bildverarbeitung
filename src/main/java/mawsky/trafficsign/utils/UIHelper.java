package main.java.mawsky.trafficsign.utils;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.util.ArrayList;

public class UIHelper {

    public static BufferedImage createHoughSpaceImage(int[][] houghArray) {
        if (houghArray == null || houghArray.length == 0 || houghArray[0].length == 0) return null;

        int phi = houghArray.length;
        int r = houghArray[0].length;

        // max votes
        int maxVotes = 0;
        for (int[] ints : houghArray) {
            for (int y = 0; y < r; y++) {
                if (ints[y] > maxVotes) {
                    maxVotes = ints[y];
                }
            }
        }

        if (maxVotes == 0) maxVotes = 1;

        // hough room in gray values
        BufferedImage rawHough = new BufferedImage(phi, r, BufferedImage.TYPE_BYTE_GRAY);
        double logMax = Math.log(1.0 + maxVotes);

        for (int x = 0; x < phi; x++) {
            for (int y = 0; y < r; y++) {
                int votes = houghArray[x][y];
                int gray = (int) ((Math.log(1.0 + votes) / logMax) * 255.0);
                rawHough.getRaster().setSample(x, y, 0, gray);
            }
        }

        // canvas
        int paddingLeft = 65;
        int paddingBottom = 40;
        int paddingTop = 20;
        int paddingRight = 20;

        int imageWidth = 800;
        int imageHeight = 500;

        int totalWidth = imageWidth + paddingLeft + paddingRight;
        int totalHeight = imageHeight + paddingTop + paddingBottom;

        BufferedImage canvas = new BufferedImage(totalWidth, totalHeight, BufferedImage.TYPE_INT_RGB);
        Graphics2D g2d = canvas.createGraphics();

        // background
        g2d.setColor(Color.BLACK);
        g2d.fillRect(0, 0, totalWidth, totalHeight);

        g2d.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC);
        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2d.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);

        // draw hough
        g2d.drawImage(rawHough, paddingLeft, paddingTop, imageWidth, imageHeight, null);

        // border
        g2d.setColor(Color.WHITE);
        g2d.drawRect(paddingLeft, paddingTop, imageWidth, imageHeight);

        g2d.setFont(new Font("SansSerif", Font.PLAIN, 12));

        // x-axis
        String[] xTicks = {"0°", "45°", "90°", "135°", "180°"};
        for (int i = 0; i < xTicks.length; i++) {
            double ratio = (double) i / (xTicks.length - 1);
            int xPos = paddingLeft + (int) (ratio * imageWidth);

            g2d.drawLine(xPos, paddingTop + imageHeight, xPos, paddingTop + imageHeight + 5);
            int textWidth = g2d.getFontMetrics().stringWidth(xTicks[i]);
            g2d.drawString(xTicks[i], xPos - (textWidth / 2), paddingTop + imageHeight + 20);
        }

        String xTitle = "Winkel θ (Theta)";
        g2d.drawString(xTitle, paddingLeft + (imageWidth / 2) - (g2d.getFontMetrics().stringWidth(xTitle) / 2), totalHeight - 5);

        // y-axis
        String[] yTicks = {"-ρ_max", "-ρ/2", "0", "+ρ/2", "+ρ_max"};
        for (int i = 0; i < yTicks.length; i++) {
            double ratio = (double) i / (yTicks.length - 1);
            int yPos = paddingTop + (int) (ratio * imageHeight);

            g2d.drawLine(paddingLeft - 5, yPos, paddingLeft, yPos);

            int textWidth = g2d.getFontMetrics().stringWidth(yTicks[i]);
            g2d.drawString(yTicks[i], paddingLeft - textWidth - 8, yPos + 4);
        }

        g2d.dispose();
        return canvas;
    }

    public static BufferedImage createPyramidImage(ArrayList<BufferedImage> pyramid, int foundIndex, BufferedImage found) {
        if (pyramid == null || pyramid.isEmpty()) return null;

        if (foundIndex < 0) foundIndex = 0;
        if (foundIndex >= pyramid.size()) foundIndex = pyramid.size() - 1;

        int minPadding = 20;

        BufferedImage dummy = new BufferedImage(1, 1, BufferedImage.TYPE_INT_ARGB);
        Graphics2D gDummy = dummy.createGraphics();
        Font labelFont = new Font("SansSerif", Font.BOLD, 12);
        gDummy.setFont(labelFont);
        FontMetrics fm = gDummy.getFontMetrics();

        int[] columnWidths = new int[pyramid.size()];
        String[] labels = new String[pyramid.size()];

        int totalWidth = 20;
        int maxHeight = 0;

        for (int i = 0; i < pyramid.size(); i++) {
            BufferedImage img = pyramid.get(i);
            boolean isFoundLevel = (i == foundIndex);

            String label = "Index " + i + " (" + img.getWidth() + "x" + img.getHeight() + ")";
            if (isFoundLevel) label += " [GEFUNDEN]";
            labels[i] = label;

            int textWidth = fm.stringWidth(label);

            int requiredWidth = Math.max(img.getWidth(), textWidth);
            columnWidths[i] = requiredWidth;

            totalWidth += requiredWidth + minPadding;
            maxHeight = Math.max(maxHeight, img.getHeight() + 45);
        }
        gDummy.dispose();

        // canvas
        BufferedImage canvas = new BufferedImage(totalWidth, maxHeight, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g2d = canvas.createGraphics();

        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2d.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);

        int currentX = 10;
        for (int i = 0; i < pyramid.size(); i++) {
            BufferedImage img = pyramid.get(i);
            boolean isFoundLevel = (i == foundIndex);
            int colWidth = columnWidths[i];

            int imgX = currentX + (colWidth - img.getWidth()) / 2;

            if (isFoundLevel) g2d.drawImage(found, imgX, 35, null);
            else g2d.drawImage(img, imgX, 35, null);

            if (isFoundLevel) {
                g2d.setColor(Color.GREEN);
                g2d.setStroke(new BasicStroke(3));
            } else {
                g2d.setColor(Color.GRAY);
                g2d.setStroke(new BasicStroke(1));
            }
            g2d.drawRect(imgX - 1, 34, img.getWidth() + 2, img.getHeight() + 2);

            g2d.setColor(isFoundLevel ? Color.GREEN : Color.WHITE);
            g2d.setFont(labelFont);
            g2d.drawString(labels[i], currentX, 25);

            currentX += colWidth + minPadding;
        }

        g2d.dispose();
        return canvas;
    }
}
