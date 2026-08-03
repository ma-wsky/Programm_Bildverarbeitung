package main.java.mawsky.trafficsign.utils;

import main.java.mawsky.trafficsign.core.HoughLine;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.util.ArrayList;

public class PipelineHelper {

    /**
     * Crops image using mask.
     * Copies only pixels that are 255 white in the mask.
     * @param image BufferedImage to crop
     * @param mask BufferedImage to use as mask
     * @return BufferedImage cropped with mask
     */
    public static BufferedImage cropAndMaskSign(BufferedImage image, BufferedImage mask) {
        int[] coordsOfSign = PipelineHelper.findCoordsOfSign(mask);
        if (coordsOfSign == null) return null;
        int xMin = coordsOfSign[0];
        int xMax = coordsOfSign[1];
        int yMin = coordsOfSign[2];
        int yMax = coordsOfSign[3];

        // size of mask
        int cropWidth = xMax - xMin + 1;
        int cropHeight = yMax - yMin + 1;

        BufferedImage croppedSign = new BufferedImage(cropWidth, cropHeight, image.getType());

        // copy pixels that are white in mask
        for (int y = 0; y < cropHeight; y++) {
            for (int x = 0; x < cropWidth; x++) {

                int origX = xMin + x;
                int origY = yMin + y;

                int maskValue = GlobalHelperFunctions.calculateGrayValueFromRGB(mask.getRGB(origX, origY));

                if (maskValue == 255) {
                    int rgb = image.getRGB(origX, origY);
                    croppedSign.setRGB(x, y, rgb);
                } else {
                    croppedSign.setRGB(x, y, 0xFF000000);
                }
            }
        }

        return croppedSign;
    }

    /**
     * Checks image for outermost white pixels in each cardinal direction and returns their coordinates.
     * @param image BufferedImage to find coords of sign in
     * @return int[] with xMin, xMax, yMin, yMax
     */
    public static int[] findCoordsOfSign(BufferedImage image){
        // xMin, xMax, yMin, yMax
        int[] values = {image.getWidth() - 1, 0, image.getHeight() - 1, 0};

        for (int x = 0; x < image.getWidth(); x++){
            for (int y = 0; y < image.getHeight(); y++){
                int rgb = image.getRGB(x, y);
                int grayValue = GlobalHelperFunctions.calculateGrayValueFromRGB(rgb);

                if (grayValue == 255){
                    if (x < values[0]){
                        values[0] = x;
                    }
                    if (x > values[1]){
                        values[1] = x;
                    }
                    if (y < values[2]){
                        values[2] = y;
                    }
                    if (y > values[3]){
                        values[3] = y;
                    }
                }
            }
        }

        if (values[0] == image.getWidth() - 1 && values[1] == 0 && values[2] == image.getHeight() - 1 && values[3] == 0) {
            return null;
        }

        return values;
    }

    /**
     * Checks if a line has a minimumLength of a segment and a maximumAllowedGap between segments.
     * @param image BufferedImage
     * @param line HoughLine
     * @param minLength minimum length of segment
     * @param maxAllowedGap maximum allowed gap between segments
     * @return boolean if line is solid
     */
    public static boolean isLineSolid(BufferedImage image, HoughLine line, int minLength, int maxAllowedGap) {
        int width = image.getWidth();
        int height = image.getHeight();

        int distance = line.r();
        double radPhi = Math.toRadians(line.phi());

        int longestChain = 0;
        int currentChain = 0;
        int currentGap = 0;

        if (line.phi() > 45 && line.phi() < 135){
            // more horizontal
            for (int x = 0; x < width; x++){
                int y = (int) ((distance - x * Math.cos(radPhi)) / Math.sin(radPhi));

                if (y >= 0 && y < height){
                    boolean isEdge = false;

                    // iterate neighbors
                    for (int dy = -1; dy <= 1; dy++) {
                        if (y + dy >= 0 && y + dy < height) {
                            if (GlobalHelperFunctions.calculateGrayValueFromRGB(image.getRGB(x, y + dy)) == 255) {
                                isEdge = true;
                                break;
                            }
                        }
                    }
                    if (isEdge){
                        currentChain++;
                        currentGap = 0;
                    }else{
                        currentGap++;
                        if (currentGap > maxAllowedGap){
                            if (currentChain > longestChain) longestChain = currentChain;
                            currentChain = 0;
                        }

                    }
                }
            }
        } else {
            // more vertical
            for (int y = 0; y < height; y++) {
                int x = (int) ((distance - y * Math.sin(radPhi)) / Math.cos(radPhi));

                if (x >= 0 && x < width) {
                    boolean isEdge = false;

                    // iterate neighbors
                    for (int dx = -1; dx <= 1; dx++) {
                        if (x + dx >= 0 && x + dx < width) {
                            if (GlobalHelperFunctions.calculateGrayValueFromRGB(image.getRGB(x + dx, y)) == 255) {
                                isEdge = true;
                                break;
                            }
                        }
                    }
                    if (isEdge) {
                        currentChain++;
                        currentGap = 0;
                    } else {
                        currentGap++;
                        if (currentGap > maxAllowedGap) {
                            if (currentChain > longestChain) longestChain = currentChain;
                            currentChain = 0;
                        }
                    }
                }
            }
        }

        if (currentChain > longestChain) longestChain = currentChain;
        return longestChain >= minLength;
    }

    /**
     * Calculates the angle of intersection between two HoughLines.
     * @param line1 HoughLine
     * @param line2 HoughLine
     * @return int angle of intersection
     */
    public static int getAngleOfIntersection(HoughLine line1, HoughLine line2) {
        int deltaPhi = Math.abs(line1.phi() - line2.phi());
        if (deltaPhi > 90) deltaPhi = 180 - deltaPhi;

        return deltaPhi;
    }

    /**
     * Looks in each direction in the (phi, r) Hough room to determine local maximum
     * @param accumulator Hough room matrix
     * @param phi angle
     * @param r distance
     * @return boolean if local maximum
     */
    public static boolean isLocalMaximum(int[][] accumulator, int phi, int r, int sizeOfWindow){

        for (int dPhi = -sizeOfWindow; dPhi <= sizeOfWindow; dPhi++){
            for (int dR = -sizeOfWindow; dR <= sizeOfWindow; dR++){
                if (dPhi == 0 && dR == 0) continue;

                int targetPhi = phi + dPhi;
                int targetR = r + dR;

                // closed loop for angles
                if (targetPhi < 0){
                    targetPhi = accumulator.length - 1;
                } else if (targetPhi >= accumulator.length) {
                    targetPhi = 0;
                }

                // bounds for r
                if (targetR < 0 || targetR >= accumulator[targetPhi].length){
                    continue;
                }

                // neighbor is bigger
                if (accumulator[targetPhi][targetR] > accumulator[phi][r]){
                    return false;
                }
            }
        }
        return true;
    }

    /**
     * Uses box downsampling to downscale image.
     * @param image BufferedImage
     * @param factor double
     * @return BufferedImage downscaled by factor
     */
    public static BufferedImage scaleColorImageBoxDownsampling(BufferedImage image, double factor){
        if (factor > 2 || factor < 0.1) return null;

        int newWidth = (int) (image.getWidth() * factor);
        int newHeight = (int) (image.getHeight() * factor);

        BufferedImage scaledImage = new BufferedImage(newWidth, newHeight, image.getType());

        double stepX = 1.0 / factor;
        double stepY = 1.0 / factor;

        for (int x = 0; x < newWidth; x++){
            for (int y = 0; y < newHeight; y++){

                // calc box
                int startX = (int) (x * stepX);
                int endX = Math.min((int) ((x + 1) * stepX), image.getWidth());
                int startY = (int) (y * stepY);
                int endY = Math.min((int) ((y + 1) * stepY), image.getHeight());

                long sumR = 0, sumG = 0, sumB = 0;
                int pixelCount = 0;

                // calc average of all pixels in box
                for (int oldX = startX; oldX < endX; oldX++) {
                    for (int oldY = startY; oldY < endY; oldY++) {
                        int rgb = image.getRGB(oldX, oldY);
                        sumR += (rgb >> 16) & 0xff;
                        sumG += (rgb >> 8) & 0xff;
                        sumB += rgb & 0xff;
                        pixelCount++;
                    }
                }

                // set new pixel value
                if (pixelCount > 0) {
                    int avgR = (int) (sumR / pixelCount);
                    int avgG = (int) (sumG / pixelCount);
                    int avgB = (int) (sumB / pixelCount);

                    int newRgb = (avgR << 16) | (avgG << 8) | avgB;
                    scaledImage.setRGB(x, y, newRgb);
                }
            }
        }

        return scaledImage;
    }

    /**
     * Uses nearest neighbor to upscale image.
     * @param image BufferedImage
     * @param factor double
     * @return BufferedImage upscaled by factor
     */
    public static BufferedImage upscaleColorImageNearestNeighbour(BufferedImage image, double factor) {
        if (factor <= 1.0) return image;

        int oldWidth = image.getWidth();
        int oldHeight = image.getHeight();

        int newWidth = (int) (oldWidth * factor);
        int newHeight = (int) (oldHeight * factor);

        BufferedImage upscaledImage = new BufferedImage(newWidth, newHeight, BufferedImage.TYPE_INT_RGB);

        for (int x = 0; x < newWidth; x++) {
            for (int y = 0; y < newHeight; y++) {

                int oldX = Math.clamp((int) (x / factor), 0, oldWidth - 1);
                int oldY = Math.clamp((int) (y / factor), 0, oldHeight - 1);

                upscaledImage.setRGB(x, y, image.getRGB(oldX, oldY));
            }
        }

        return upscaledImage;
    }

    /**
     * Draws a geometric form based on vertices.
     * Fills the resulting form with white
     * @param g2d Graphics2D
     * @param vertices ArrayList<Point>
     */
    public static void drawEdgesAndFill(Graphics2D g2d, ArrayList<Point> vertices) {
        int numPoints = vertices.size();
        int[] xPoints = new int[numPoints];
        int[] yPoints = new int[numPoints];

        for (int i = 0; i < numPoints; i++){
            xPoints[i] = vertices.get(i).x;
            yPoints[i] = vertices.get(i).y;
        }

        // fill
        g2d.setColor(Color.WHITE);
        g2d.setStroke(new java.awt.BasicStroke(2));
        g2d.fillPolygon(xPoints, yPoints, numPoints);
    }

}
