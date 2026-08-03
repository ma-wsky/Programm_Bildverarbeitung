package main.java.mawsky.trafficsign.utils;

import main.java.mawsky.trafficsign.io.ImageIO;
import main.java.mawsky.trafficsign.core.HoughLine;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.util.ArrayList;

public class DebuggingLinesAndWindowHelper {

    /**
     * Draws all lines with Graphics2D g.
     * @param g Graphics2D
     * @param lines ArrayList<HoughLine>
     * @param width int width of image
     * @param height int height of image
     */
    public static void drawLines(Graphics2D g, ArrayList<HoughLine> lines, int width, int height) {
        g.setStroke(new java.awt.BasicStroke(1));

        // calculate x and y positions from Hough coordinates
        for (HoughLine line : lines){
            int r = line.r();
            int phi = line.phi();
            int x1, x2, y1, y2;
            double radPhi = Math.toRadians(phi);

            if (phi > 45 && phi < 135){
                x1 = 0;
                x2 = width;

                y1 = (int) (r / Math.sin(radPhi));
                y2 = (int) ((r - x2 * Math.cos(radPhi)) / Math.sin(radPhi));
            }else {
                y1 = 0;
                y2 = height;

                x1 = (int) (r / Math.cos(radPhi));
                x2 = (int) ((r - y2 * Math.sin(radPhi)) / Math.cos(radPhi));
            }

            g.drawLine(x1, y1, x2, y2);
        }
    }

    /**
     * Draws the moving windows position with x and y onto copy of original.
     * @param original BufferedImage
     * @param x int
     * @param y int
     * @param windowSize int
     * @return BufferedImage with window drawn
     */
    public static BufferedImage drawWindow(BufferedImage original, int x, int y, int windowSize) {
        // create copy of original
        BufferedImage image = ImageIO.copyBufferedImage(original);
        Graphics2D g2d = image.createGraphics();

        g2d.setColor(Color.RED);
        g2d.setStroke(new BasicStroke(1));

        g2d.drawRect(x, y, windowSize, windowSize);

        g2d.dispose();

        return image;
    }
}
