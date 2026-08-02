package main.java.mawsky.trafficsign.processing;

import main.java.mawsky.trafficsign.utils.GlobalHelperFunctions;
import main.java.mawsky.trafficsign.io.ImageIO;

import java.awt.image.BufferedImage;

public class EdgeDetection {

    /**
     * Calculates a lowpass for image with gauß.
     * Separates Gaussian algorithm into x and y directions.
     * @param image BufferedImage
     * @param maskSize int
     * @return BufferedImage with lowpass applied
     */
    public static BufferedImage gaussianLowPassSeparated(BufferedImage image, int maskSize){

        //early exit
        if(maskSize % 2 == 0){
            // matrix has no middle
            System.err.println("The mask must have uneven number of elements.");
            return null;
        }

        // make sure mask size is at least 3
        if (maskSize < 3){
            maskSize = 3;
        }

        // create mask with Gaussian distribution
        double[]mask = new double[maskSize];
        int distance = maskSize / 2;
        double sigma = maskSize / 6.0;

        double sum = 0;

        for(int x = -distance; x <= distance; x++){
            // one-dimensional Gaussian distribution
            mask[x + distance] = Math.exp(-(Math.pow(x, 2) / (2 * Math.pow(sigma, 2))));
            sum += mask[x + distance];
        }

        for(int i = 0; i < maskSize; i++){
            mask[i] /= sum;
        }

        // create images
        BufferedImage grayScaleImage = ColorManipulation.grayScale(image);
        BufferedImage horizontalImage = new BufferedImage(image.getWidth(), image.getHeight(), BufferedImage.TYPE_INT_RGB);
        BufferedImage finalImage = new BufferedImage(image.getWidth(), image.getHeight(), BufferedImage.TYPE_INT_RGB);

        // lowpass in x direction
        for (int y = 0; y < image.getHeight(); y++){
            for (int x = 0; x < image.getWidth(); x++){
                double mean = 0;

                // iterate mask and sum grayValue * maskValue
                for (int m = -distance; m <= distance; m++){
                    int clampedX = Math.clamp(x + m, 0, image.getWidth() - 1);
                    double grayValue = GlobalHelperFunctions.calculateGrayValueFromRGB(grayScaleImage.getRGB(clampedX, y));
                    double maskGrayX = grayValue * mask[m + distance];
                    mean += maskGrayX;
                }

                int a =  (grayScaleImage.getRGB(x, y) >> 24) & 0xff;
                int newRgb = (a << 24) | ((int) mean << 16) | ((int) mean << 8) | (int) mean;
                horizontalImage.setRGB(x, y, newRgb);
            }
        }

        // lowpass in y direction
        for (int y = 0; y < image.getHeight(); y++){
            for (int x = 0; x < image.getWidth(); x++){
                double mean = 0;

                // iterate mask and sum grayValue * maskValue
                for (int m = -distance; m <= distance; m++){
                    int clampedY = Math.clamp(y + m, 0, image.getHeight() - 1);
                    double grayValue = GlobalHelperFunctions.calculateGrayValueFromRGB(horizontalImage.getRGB(x, clampedY));
                    double maskGrayX = grayValue * mask[m + distance];
                    mean += maskGrayX;
                }

                int a =  (horizontalImage.getRGB(x, y) >> 24) & 0xff;
                int newRgb = (a << 24) | ((int) mean << 16) | ((int) mean << 8) | (int) mean;
                finalImage.setRGB(x, y, newRgb);
            }
        }

        return finalImage;
    }

    /**
     * Finds edges in a BufferedImage by using Sobel filters for horizontal and/or vertical edge detection.
     * @param image BufferedImage to find edges in
     * @param flag 1 for horizontal, 2 for vertical, 3 for both
     * @return BufferedImage with edges
     */
    public static BufferedImage sobelFilter(BufferedImage image, int flag){

        // create masks for vertical and horizontal Sobel algorithm
        double[][] maskV = {{1, 0, -1}, {2, 0, -2}, {1, 0, -1}};
        double[][] maskH = {{1, 2, 1}, {0, 0, 0}, {-1, -2, -1}};
        int coefficientSum = 4;
        int maskSize = 3;
        int distance = maskSize / 2;

        // create images
        BufferedImage grayScaleImage = ColorManipulation.grayScale(image);
        BufferedImage newImage = ImageIO.copyBufferedImage(grayScaleImage);

        // Sobel algorithm with edge case cutting
        for (int x = distance; x < grayScaleImage.getWidth()-distance; x++) {
            for (int y = distance; y < grayScaleImage.getHeight()-distance; y++) {

                double valueH = 0;
                double valueV = 0;

                // iterate vertical and horizontal masks
                for (int c = -distance; c <= distance; c++) {
                    for (int r = -distance; r <= distance; r++) {
                        double grayValue = GlobalHelperFunctions.calculateGrayValueFromRGB(grayScaleImage.getRGB(x+c, y+r));

                        if (flag == 1 || flag == 3){
                            valueH += grayValue * maskH[c+distance][r+distance];
                        }
                        if (flag == 2 || flag == 3){
                            valueV += grayValue * maskV[c+distance][r+distance];
                        }
                    }
                }

                double finalValue = 0;

                // determine final pixel value
                if (flag == 1){
                    finalValue = Math.abs(valueH) / coefficientSum;
                }else if (flag == 2){
                    finalValue = Math.abs(valueV) / coefficientSum;
                }else if (flag == 3){
                    finalValue = Math.sqrt(valueH * valueH + valueV * valueV) / coefficientSum;
                }

                // keep inside bitrange
                if (finalValue > 255.0) finalValue = 255.0;
                if (finalValue < 0.0) finalValue = 0.0;

                int a =  (grayScaleImage.getRGB(x, y) >> 24) & 0xff;
                int newRgb = (a << 24) | ((int)finalValue << 16) | ((int)finalValue << 8) | (int)finalValue;
                newImage.setRGB(x, y, newRgb);
            }
        }

        return newImage;
    }

    /**
     * Performs the Hough transformation on an image.
     * Calls {@link GlobalHelperFunctions#calculateGrayValueFromRGB(int)}
     * @param image BufferedImage to perform on
     * @return int[][] Hough room matrix
     */
    public static int[][] houghTransformation(BufferedImage image){
        int diagonal = (int) Math.ceil(Math.sqrt(image.getHeight() * image.getHeight() + image.getWidth() * image.getWidth()));
        int[][] accumulator = new int[180][2*diagonal];

        for (int x = 0; x < image.getWidth(); x++){
            for (int y = 0; y < image.getHeight(); y++){

                int grayValue = GlobalHelperFunctions.calculateGrayValueFromRGB(image.getRGB(x, y));

                if (grayValue == 255){
                    for (int phi = 0; phi < 180; phi++){
                        int r = (int) (x * Math.cos(Math.toRadians(phi)) + y * Math.sin(Math.toRadians(phi)));
                        // + diagonal to fit into array indexes
                        r += diagonal;
                        accumulator[phi][r] += 1;
                    }
                }
            }
        }
        return accumulator;
    }

}
