package classes.Pipeline;

import classes.DescriptiveStatistics;
import classes.EdgeDetection;
import classes.GlobalHelperFunctions;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.util.ArrayList;

public class PipelineHelper {

    /**
     * Checks a given BufferedImage for outermost white pixels in each cardinal direction and returns their coordinates.
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
     * Crops a given BufferedImage using a BufferedImage as a mask.
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
     * Helper to check if pAB is inside the image with tolerance.
     * @param pAB Point
     * @param width int
     * @param height int
     * @param t int
     * @return boolean if pAB is inside the image
     */
    public static boolean isInsideImage(Point pAB, int width, int height, int t) {
        return (pAB.x < (width+t) && pAB.x > (-t)) && (pAB.y < (height+t) && pAB.y > -t);
    }

    /**
     * Returns intersection point of lines a and b
     * @param a classes.Pipeline.HoughLine
     * @param b classes.Pipeline.HoughLine
     * @param diagonal image diagonal
     * @return Point intersection point
     */
    public static Point getIntersection(HoughLine a, HoughLine b, int diagonal) {
        double r1 = a.r - diagonal;
        double r2 = b.r - diagonal;

        double phi1 = Math.toRadians(a.phi);
        double phi2 = Math.toRadians(b.phi);

        double denominator = Math.cos(phi1) * Math.sin(phi2) - Math.sin(phi1) * Math.cos(phi2);

        // return if parallel
        if (Math.abs(denominator) < 0.0001) {
            return null;
        }

        int x = (int) Math.round((r1 * Math.sin(phi2) - r2 * Math.sin(phi1)) / denominator);
        int y = (int) Math.round((r2 * Math.cos(phi1) - r1 * Math.cos(phi2)) / denominator);

        return new Point(x, y);
    }

    /**
     * Checks if a line has a minimumLength of a segment and a maximumAllowedGap between segments.
     * @param image BufferedImage
     * @param line classes.Pipeline.HoughLine
     * @param minLength minimum length of segment
     * @param maxAllowedGap maximum allowed gap between segments
     * @return boolean if line is solid
     */
    public static boolean isLineSolid(BufferedImage image, HoughLine line, int minLength, int maxAllowedGap) {
        int width = image.getWidth();
        int height = image.getHeight();
        int diagonal = (int) Math.ceil(Math.sqrt(height*height + width*width));

        int distance = line.r - diagonal;
        double radPhi = Math.toRadians(line.phi);

        int longestChain = 0;
        int currentChain = 0;
        int currentGap = 0;

        if (line.phi > 45 && line.phi < 135){
            // more horizontal
            for (int x = 0; x < width; x++){
                int y = (int) ((distance - x * Math.cos(radPhi)) / Math.sin(radPhi));

                if (y >= 0 && y < height){
                    boolean isEdge = false;
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
     * @param line1 classes.Pipeline.HoughLine
     * @param line2 classes.Pipeline.HoughLine
     * @return int angle of intersection
     */
    public static int getAngleOfIntersection(HoughLine line1, HoughLine line2) {
        int deltaPhi = Math.abs(line1.phi - line2.phi);
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

                // neighbour is bigger
                if (accumulator[targetPhi][targetR] > accumulator[phi][r]){
                    return false;
                }
            }
        }
        return true;
    }


    /**
     * Scales a given BufferedImage by a given factor.
     * @param image BufferedImage
     * @param factor double
     * @return BufferedImage scaled image
     */
    public static BufferedImage scaleColorImageGauss(BufferedImage image, double factor){
        if (factor > 2 || factor < 0.1) return null;

        double reach = 1.0 / factor;
        int maskSize = (int) (2 * reach + 1);
        if (maskSize % 2 == 0){
            maskSize += 1;
        }

        BufferedImage lowpass = EdgeDetection.gaussianLowPassColor(image, maskSize);
        if (lowpass == null) return null;

        int newWidth = (int) (lowpass.getWidth() * factor);
        int newHeight = (int) (lowpass.getHeight() * factor);

        BufferedImage scaledImage = new BufferedImage(newWidth, newHeight, lowpass.getType());

        for (int x = 0; x < newWidth; x++){
            for (int y = 0; y < newHeight; y++){
                int oldPixelX = Math.round((float) (x / factor)); // nearest neighbour
                int oldPixelY = Math.round((float) (y / factor));

                scaledImage.setRGB(x, y, lowpass.getRGB(oldPixelX, oldPixelY));
            }
        }

        return scaledImage;
    }

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

    public static BufferedImage upscaleColorImageNearestNeighbour(BufferedImage original, double factor) {
        if (factor <= 1.0) return original;

        int oldWidth = original.getWidth();
        int oldHeight = original.getHeight();

        int newWidth = (int) (oldWidth * factor);
        int newHeight = (int) (oldHeight * factor);

        BufferedImage upscaledImage = new BufferedImage(newWidth, newHeight, BufferedImage.TYPE_INT_RGB);

        for (int x = 0; x < newWidth; x++) {
            for (int y = 0; y < newHeight; y++) {

                int oldX = Math.clamp((int) (x / factor), 0, oldWidth - 1);
                int oldY = Math.clamp((int) (y / factor), 0, oldHeight - 1);

                upscaledImage.setRGB(x, y, original.getRGB(oldX, oldY));
            }
        }

        return upscaledImage;
    }

    /**
     * Determines valid center color of triangle.
     * More red than blue and green.
     * @param originalImage BufferedImage
     * @param currentTriangle ArrayList<Point>
     * @return boolean if color is valid
     */
    public static boolean isValidTriangleCenterColor(BufferedImage originalImage, ArrayList<Point> currentTriangle) {
        int centerX = (currentTriangle.get(0).x + currentTriangle.get(1).x + currentTriangle.get(2).x) / 3;
        int centerY = (currentTriangle.get(0).y + currentTriangle.get(1).y + currentTriangle.get(2).y) / 3;

        if (centerX < 0 || centerX >= originalImage.getWidth() || centerY < 0 || centerY >= originalImage.getHeight()) return true;

        int centerRGB = originalImage.getRGB(centerX, centerY);
        int cR = (centerRGB >> 16) & 0xFF;
        int cG = (centerRGB >> 8) & 0xFF;
        int cB = centerRGB & 0xFF;

        // no blue
        if (cB > cR && cB > cG && cB > 80) {
            return false;
        }

        // no green
        if (cG > cR && cG > cB && cG > 80) {
            return false;
        }

        return true;
    }

    /**
     * Checks central pixels color against blue
     * @param originalImage BufferedImage
     * @param currentRectangle ArrayList<Point>
     * @return boolean if center color is valid
     */
    public static boolean isValidRectangleCenterColor(BufferedImage originalImage, ArrayList<Point> currentRectangle) {
        int centerX = (currentRectangle.get(0).x + currentRectangle.get(1).x + currentRectangle.get(2).x + currentRectangle.get(3).x) / 4;
        int centerY = (currentRectangle.get(0).y + currentRectangle.get(1).y + currentRectangle.get(2).y + currentRectangle.get(3).y) / 4;

        if (centerX < 0 || centerX >= originalImage.getWidth() || centerY < 0 || centerY >= originalImage.getHeight()) return true;

        int centerRGB = originalImage.getRGB(centerX, centerY);
        int cR = (centerRGB >> 16) & 0xFF;
        int cG = (centerRGB >> 8) & 0xFF;
        int cB = centerRGB & 0xFF;

        // no blue
        if (cB > cR && cB > cG && cB > 80) {
            return false;
        }

        return true;
    }

    /**
     * Checks central pixels color against blue and green
     * @param originalImage BufferedImage
     * @param currentOctagon ArrayList<Point>
     * @return boolean if center color is valid
     */
    public static boolean isValidOctagonCenterColor(BufferedImage originalImage, ArrayList<Point> currentOctagon){
        int centerX = (currentOctagon.get(0).x + currentOctagon.get(1).x + currentOctagon.get(2).x + currentOctagon.get(3).x + currentOctagon.get(4).x + currentOctagon.get(5).x + currentOctagon.get(6).x + currentOctagon.get(7).x) / 8;
        int centerY = (currentOctagon.get(0).y + currentOctagon.get(1).y + currentOctagon.get(2).y + currentOctagon.get(3).y + currentOctagon.get(4).y + currentOctagon.get(5).y + currentOctagon.get(6).y + currentOctagon.get(7).y) / 8;

        if (centerX < 0 || centerX >= originalImage.getWidth() || centerY < 0 || centerY >= originalImage.getHeight()) return true;

        int centerRGB = originalImage.getRGB(centerX, centerY);
        int cR = (centerRGB >> 16) & 0xFF;
        int cG = (centerRGB >> 8) & 0xFF;
        int cB = centerRGB & 0xFF;

        // no blue
        if (cB > cR && cB > cG && cB > 80) {
            return false;
        }

        // no green
        if (cG > cR && cG > cB && cG > 80) {
            return false;
        }

        return true;
    }

    /**
     * Determines too small a size.
     * Takes Math.min(width, height) / 20 for minimum length
     * @param originalImage BufferedImage
     * @param currentTriangle ArrayList<Point>
     * @return boolean if distance between to points is too small
     */
    public static boolean isTriangleTooSmall(BufferedImage originalImage, ArrayList<Point> currentTriangle) {
        int width = originalImage.getWidth();
        int height = originalImage.getHeight();


        // TODO: determine criterion for minimum size of shape
        double minLength = Math.min(width, height) / 10.0;

        Point p1 = currentTriangle.get(0);
        Point p2 = currentTriangle.get(1);
        Point p3 = currentTriangle.get(2);

        double d1 = Math.sqrt(Math.pow(p2.x - p1.x, 2) + Math.pow(p2.y - p1.y, 2));
        double d2 = Math.sqrt(Math.pow(p3.x - p2.x, 2) + Math.pow(p3.y - p2.y, 2));
        double d3 = Math.sqrt(Math.pow(p1.x - p3.x, 2) + Math.pow(p1.y - p3.y, 2));

        return d1 < minLength || d2 < minLength || d3 < minLength;
    }

    /**
     * Determines too small a size.
     * Takes Math.min(width, height) / 20 for minimum length
     * @param originalImage BufferedImage
     * @param currentRectangle ArrayList<Point>
     * @return boolean if distance between to points is too small
     */
    public static boolean isRectangleTooSmall(BufferedImage originalImage, ArrayList<Point> currentRectangle) {
        int width = originalImage.getWidth();
        int height = originalImage.getHeight();

        // TODO: determine criterion for minimum size of shape
        double minLength = 20;

        Point p1 = currentRectangle.get(0);
        Point p2 = currentRectangle.get(1);
        Point p3 = currentRectangle.get(2);
        Point p4 = currentRectangle.get(3);

        double d1 = Math.sqrt(Math.pow(p2.x - p1.x, 2) + Math.pow(p2.y - p1.y, 2));
        double d2 = Math.sqrt(Math.pow(p3.x - p2.x, 2) + Math.pow(p3.y - p2.y, 2));
        double d3 = Math.sqrt(Math.pow(p4.x - p3.x, 2) + Math.pow(p4.y - p3.y, 2));
        double d4 = Math.sqrt(Math.pow(p4.x - p1.x, 2) + Math.pow(p4.y - p1.y, 2));

        return d1 < minLength || d2 < minLength || d3 < minLength || d4 < minLength;
    }

    /**
     * Determines too small a size.
     * Takes Math.min(width, height) / 20 for minimum length
     * @param originalImage BufferedImage
     * @param currentOctagon ArrayList<Point>
     * @return boolean if distance between to points is too small
     */
    public static boolean isOctagonTooSmall(BufferedImage originalImage, ArrayList<Point> currentOctagon) {
        int width = originalImage.getWidth();
        int height = originalImage.getHeight();

        // TODO: determine criterion for minimum size of shape
        double minLength = 20;

        Point p1 = currentOctagon.get(0);
        Point p2 = currentOctagon.get(1);
        Point p3 = currentOctagon.get(2);
        Point p4 = currentOctagon.get(3);
        Point p5 = currentOctagon.get(4);
        Point p6 = currentOctagon.get(5);
        Point p7 = currentOctagon.get(6);
        Point p8 = currentOctagon.get(7);

        double d1 = Math.sqrt(Math.pow(p2.x - p1.x, 2) + Math.pow(p2.y - p1.y, 2));
        double d2 = Math.sqrt(Math.pow(p3.x - p2.x, 2) + Math.pow(p3.y - p2.y, 2));
        double d3 = Math.sqrt(Math.pow(p4.x - p3.x, 2) + Math.pow(p4.y - p3.y, 2));
        double d4 = Math.sqrt(Math.pow(p5.x - p4.x, 2) + Math.pow(p5.y - p4.y, 2));
        double d5 = Math.sqrt(Math.pow(p6.x - p5.x, 2) + Math.pow(p6.y - p5.y, 2));
        double d6 = Math.sqrt(Math.pow(p7.x - p6.x, 2) + Math.pow(p7.y - p6.y, 2));
        double d7 = Math.sqrt(Math.pow(p8.x - p7.x, 2) + Math.pow(p8.y - p7.y, 2));
        double d8 = Math.sqrt(Math.pow(p1.x - p8.x, 2) + Math.pow(p1.y - p8.y, 2));

        return d1 < minLength || d2 < minLength || d3 < minLength || d4 < minLength || d5 < minLength || d6 < minLength || d7 < minLength || d8 < minLength;
    }

    public static boolean isShapeEntropyTooHigh(BufferedImage mask) {
        DescriptiveStatistics stats = new DescriptiveStatistics(mask);
        stats.calculateEntropy();
        double e = stats.getEntropy();

        // TODO: fine tune threshold
        double threshold = 4.4;

        return (e > threshold);
    }
}
