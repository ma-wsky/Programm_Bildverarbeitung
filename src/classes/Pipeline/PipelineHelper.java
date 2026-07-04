package classes.Pipeline;

import classes.EdgeDetection;
import classes.GlobalHelperFunctions;

import java.awt.*;
import java.awt.image.BufferedImage;

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
     * @param tolerance tolerance
     * @return int angle of intersection
     */
    public static int getAngleOfIntersection(HoughLine line1, HoughLine line2, int tolerance) {
        int angleOfIntersection = 0;

        int deltaPhi = Math.abs(line1.phi - line2.phi);
        if (deltaPhi > 90) deltaPhi = 180 - deltaPhi;

        if (deltaPhi >= 90 - tolerance){
            angleOfIntersection = 90;
        }
        else if (deltaPhi >= 60 - tolerance) {
            angleOfIntersection = 60;
        }
        else if (deltaPhi >= 45 - tolerance) {
            angleOfIntersection = 45;
        }
        return angleOfIntersection;
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

    public static BufferedImage scaleImage(BufferedImage image, double factor){
        if (factor > 2 || factor < 0.1) return null;

        double reach = 1.0 / factor;
        int maskSize = (int) (2 * reach + 1);
        if (maskSize % 2 == 0){
            maskSize += 1;
        }

        //BufferedImage lowpass = EdgeDetection.gaussianLowPass(image, maskSize);
        //if (lowpass == null) return image;

        int newWidth = (int) (image.getWidth() * factor);
        int newHeight = (int) (image.getHeight() * factor);

        BufferedImage scaledImage = new BufferedImage(newWidth, newHeight, image.getType());

        for (int x = 0; x < newWidth; x++){
            for (int y = 0; y < newHeight; y++){
                int oldPixelX = Math.round((float) (x / factor)); // nearest neighbour
                int oldPixelY = Math.round((float) (y / factor));

                scaledImage.setRGB(x, y, image.getRGB(oldPixelX, oldPixelY));
            }
        }

        return scaledImage;
    }

}
