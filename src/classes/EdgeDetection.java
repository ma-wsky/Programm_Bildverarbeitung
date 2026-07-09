package classes;

import java.awt.image.BufferedImage;
import java.io.IOException;

public class EdgeDetection {

    /**
     * Performs moving mean algorithm with custom mask.
     * mask can be customized in size and values.
     * Uses {@link GlobalHelperFunctions#calculateGrayValueFromRGB(int)}.
     * Checks for invalid masks.
     * @param image BufferedImage to be altered
     * @param mask mask
     * @return altered BufferedImage
     */
    public static BufferedImage movingMean(BufferedImage image, double[][] mask){
        if(mask.length != mask[0].length){
            // matrix not square
            System.err.println("The mask must be a square matrix.");
            return null;
        }
        if(mask.length % 2 == 0){
            // matrix has no middle
            System.err.println("The mask must have uneven number of elements.");
            return null;
        }

        BufferedImage grayScaleImage = ColorManipulation.grayScale(image);
        BufferedImage newImage = ImageIO.copyBufferedImage(grayScaleImage);

        int maskSize = mask.length;
        int distance = maskSize / 2;

        // edge case: cutting
        for (int x = distance; x < grayScaleImage.getWidth()-distance; x++) {
            for (int y = distance; y < grayScaleImage.getHeight()-distance; y++) {

                double mean = calculateValueAndMask(mask, grayScaleImage, distance, x, y);

                double maskSum = 0;
                for (double[] r : mask){
                    for (double c : r){
                        maskSum += c;
                    }
                }

                if (maskSum != 0){
                    mean /= maskSum;
                }

                int a =  (grayScaleImage.getRGB(x, y) >> 24) & 0xff;
                int newRgb = (a << 24) | ((int)mean << 16) | ((int)mean << 8) | (int)mean;
                newImage.setRGB(x, y, newRgb);
            }
        }
        return newImage;
    }


    /**
     * Calculates the mask for a gaussianLowPass using the two-dimensional gaussian distribution.
     * Calls {@link EdgeDetection#movingMean(BufferedImage, double[][])} to perform algorithm on image.
     * Checks for invalid maskSize.
     * @param image BufferedImage to receive low pass
     * @param maskSize size of the mask in x or y direction
     * @return BufferedImage with low pass applied
     */
    public static BufferedImage gaussianLowPass(BufferedImage image, int maskSize){
        if(maskSize % 2 == 0){
            // matrix has no middle
            System.err.println("The mask must have uneven number of elements.");
            return null;
        }
        if (maskSize < 3){
            maskSize = 3;
        }
        double[][] mask = new double[maskSize][maskSize];
        int distance = maskSize / 2;

        double sigma = maskSize / 6.0;

        for(int x = -distance; x <= distance; x++){
            for(int y = -distance; y <= distance; y++){
                // two-dimensional gaussian distribution
                mask[x+distance][y+distance] = Math.exp(-((Math.pow(x, 2) + Math.pow(y, 2)) / (2 * Math.pow(sigma, 2))));
            }
        }

        return EdgeDetection.movingMean(image, mask);
    }

    public static BufferedImage gaussianLowPassSeperated(BufferedImage image, int maskSize){
        if(maskSize % 2 == 0){
            // matrix has no middle
            System.err.println("The mask must have uneven number of elements.");
            return null;
        }
        if (maskSize < 3){
            maskSize = 3;
        }
        double[]mask = new double[maskSize];
        int distance = maskSize / 2;
        double sigma = maskSize / 6.0;

        double sum = 0;

        for(int x = -distance; x <= distance; x++){
            // one-dimensional gaussian distribution
            mask[x + distance] = Math.exp(-(Math.pow(x, 2) / (2 * Math.pow(sigma, 2))));
            sum += mask[x + distance];
        }

        for(int i = 0; i < maskSize; i++){
            mask[i] /= sum;
        }

        BufferedImage grayScaleImage = ColorManipulation.grayScale(image);
        BufferedImage horizontalImage = new BufferedImage(image.getWidth(), image.getHeight(), BufferedImage.TYPE_INT_RGB);
        BufferedImage finalImage = new BufferedImage(image.getWidth(), image.getHeight(), BufferedImage.TYPE_INT_RGB);

        // x
        for (int y = 0; y < image.getHeight(); y++){
            for (int x = 0; x < image.getWidth(); x++){
                double mean = 0;
                for (int m = -distance; m <= distance; m++){
                    int clampedX = Math.clamp(x + m, 0, image.getWidth() - 1);
                    double grayValue = GlobalHelperFunctions.calculateGrayValueFromRGB(grayScaleImage.getRGB(clampedX, y));
                    double grayXmask = grayValue * mask[m + distance];
                    mean += grayXmask;
                }

                int a =  (grayScaleImage.getRGB(x, y) >> 24) & 0xff;
                int newRgb = (a << 24) | ((int)mean << 16) | ((int)mean << 8) | (int)mean;
                horizontalImage.setRGB(x, y, newRgb);
            }
        }

        // y
        for (int y = 0; y < image.getHeight(); y++){
            for (int x = 0; x < image.getWidth(); x++){
                double mean = 0;
                for (int m = -distance; m <= distance; m++){
                    int clampedY = Math.clamp(y + m, 0, image.getHeight() - 1);
                    double grayValue = GlobalHelperFunctions.calculateGrayValueFromRGB(horizontalImage.getRGB(x, clampedY));
                    double grayXmask = grayValue * mask[m + distance];
                    mean += grayXmask;
                }

                int a =  (horizontalImage.getRGB(x, y) >> 24) & 0xff;
                int newRgb = (a << 24) | ((int)mean << 16) | ((int)mean << 8) | (int)mean;
                finalImage.setRGB(x, y, newRgb);
            }
        }

        return finalImage;
    }

    public static BufferedImage gaussianLowPassColor(BufferedImage image, int maskSize){
        if(maskSize % 2 == 0){
            // matrix has no middle
            System.err.println("The mask must have uneven number of elements.");
            return null;
        }
        if (maskSize < 3){
            maskSize = 3;
        }
        double[]mask = new double[maskSize];
        int distance = maskSize / 2;
        double sigma = maskSize / 6.0;

        double sum = 0;

        for(int x = -distance; x <= distance; x++){
            // one-dimensional gaussian distribution
            mask[x + distance] = Math.exp(-(Math.pow(x, 2) / (2 * Math.pow(sigma, 2))));
            sum += mask[x + distance];
        }

        for(int i = 0; i < maskSize; i++){
            mask[i] /= sum;
        }

        BufferedImage horizontalImage = new BufferedImage(image.getWidth(), image.getHeight(), BufferedImage.TYPE_INT_RGB);
        BufferedImage finalImage = new BufferedImage(image.getWidth(), image.getHeight(), BufferedImage.TYPE_INT_RGB);

        // x
        for (int y = 0; y < image.getHeight(); y++){
            for (int x = 0; x < image.getWidth(); x++){
                double meanR = 0;
                double meanG = 0;
                double meanB = 0;

                for (int m = -distance; m <= distance; m++){
                    int clampedX = Math.clamp(x + m, 0, image.getWidth() - 1);

                    int rgb = image.getRGB(clampedX, y);
                    int r = (rgb >> 16) & 0xff;
                    int g = (rgb >> 8) & 0xff;
                    int b = (rgb) & 0xff;

                    double maskValue = mask[m + distance];
                    meanR += r * maskValue;
                    meanG += g * maskValue;
                    meanB += b * maskValue;
                }

                int a =  (image.getRGB(x, y) >> 24) & 0xff;
                int newRgb = (a << 24) | ((int)meanR << 16) | ((int)meanG << 8) | (int)meanB;
                horizontalImage.setRGB(x, y, newRgb);
            }
        }

        // y
        for (int y = 0; y < image.getHeight(); y++){
            for (int x = 0; x < image.getWidth(); x++){
                double meanR = 0;
                double meanG = 0;
                double meanB = 0;

                for (int m = -distance; m <= distance; m++){
                    int clampedY = Math.clamp(y + m, 0, image.getHeight() - 1);

                    int rgb = horizontalImage.getRGB(x, clampedY);
                    int r = (rgb >> 16) & 0xff;
                    int g = (rgb >> 8) & 0xff;
                    int b = (rgb) & 0xff;

                    double maskValue = mask[m + distance];
                    meanR += r * maskValue;
                    meanG += g * maskValue;
                    meanB += b * maskValue;
                }

                int a =  (horizontalImage.getRGB(x, y) >> 24) & 0xff;
                int newRgb = (a << 24) | ((int)meanR << 16) | ((int)meanG << 8) | (int)meanB;
                finalImage.setRGB(x, y, newRgb);
            }
        }

        return finalImage;
    }

    /**
     * Operates on the given image with a difference operator mask.
     * Sum of mask elements must be zero.
     * @param image BufferedImage to operate on
     * @param mask difference operator
     * @return BufferedImage
     */
    public static BufferedImage differenceOperator(BufferedImage image, double[][] mask){
        //checks
        if(mask.length != mask[0].length){
            // matrix not square
            System.err.println("The mask must be a square matrix.");
            return null;
        }
        if(mask.length % 2 == 0){
            // matrix has no middle
            System.err.println("The mask must have uneven number of elements.");
            return null;
        }

        double positiveSum = 0;
        for (double[] r : mask){
            for (double c : r){
                if (c > 0){
                    positiveSum += c;
                }
            }
        }

        BufferedImage grayScaleImage = ColorManipulation.grayScale(image);
        BufferedImage newImage = ImageIO.copyBufferedImage(grayScaleImage);

        int maskSize = mask.length;
        int distance = maskSize / 2;

        // edge case: cutting
        for (int x = distance; x < grayScaleImage.getWidth()-distance; x++) {
            for (int y = distance; y < grayScaleImage.getHeight()-distance; y++) {

                double value = calculateValueAndMask(mask, grayScaleImage, distance, x, y);

                value = Math.abs(value);

                //TODO: dividing by too much for laplace
                value = value / positiveSum;

                if (value > 255.0) value = 255.0;
                if (value < 0.0) value = 0.0;

                int a =  (grayScaleImage.getRGB(x, y) >> 24) & 0xff;
                int newRgb = (a << 24) | ((int)value << 16) | ((int)value << 8) | (int)value;
                newImage.setRGB(x, y, newRgb);
            }
        }
        try {
            ImageIO.saveBufferedImageAsPPM(newImage, "generated/differenceOperator.ppm");
        } catch (IOException e) {
            System.err.println("Error while saving the difference operator ppm file.");
        }
        return newImage;
    }

    /**
     * Finds edges in a BufferedImage by using sobel filters for horizontal and/or vertical edge detection.
     * @param image BufferedImage to find edges in
     * @param flag 1 for horizontal, 2 for vertical, 3 for both
     * @return BufferedImage with edges
     */
    public static BufferedImage sobelFilter(BufferedImage image, int flag){
        double[][] maskV = {{1, 0, -1}, {2, 0, -2}, {1, 0, -1}};
        double[][] maskH = {{1, 2, 1}, {0, 0, 0}, {-1, -2, -1}};

        BufferedImage grayScaleImage = ColorManipulation.grayScale(image);
        BufferedImage newImage = ImageIO.copyBufferedImage(grayScaleImage);

        int maskSize = 3;
        int distance = maskSize / 2;

        // edge case: cutting
        for (int x = distance; x < grayScaleImage.getWidth()-distance; x++) {
            for (int y = distance; y < grayScaleImage.getHeight()-distance; y++) {

                double valueH = 0;
                double valueV = 0;

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

                if (flag == 1){
                    finalValue = Math.abs(valueH) / 4.0;
                }else if (flag == 2){
                    finalValue = Math.abs(valueV) / 4.0;
                }else if (flag == 3){
                    finalValue = Math.sqrt(Math.pow(valueH, 2) + Math.pow(valueV, 2)) / 4.0;
                }

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
     * Find edges in image using laplace operator.
     * Calls {@link EdgeDetection#differenceOperator(BufferedImage, double[][])} with the laplace operator.
     * @param image BufferedImage to find edges in
     * @return BufferedImage with found edges
     */
    public static BufferedImage laplaceFilter(BufferedImage image){
        double[][] mask = {{1, 1, 1}, {1, -8, 1}, {1, 1, 1}};

        return EdgeDetection.differenceOperator(image, mask);
    }

    /**
     * Performs the Hough transformation on an image.
     * Calls {@link GlobalHelperFunctions#calculateGrayValueFromRGB(int)}
     * @param image BufferedImage to perform on
     * @return int[][] Hough room matrix
     */
    public static int[][] houghTransformation(BufferedImage image){
        int diagonal = (int) Math.ceil(Math.sqrt(Math.pow(image.getHeight(), 2) + Math.pow(image.getWidth(), 2)));
        int[][] accumulator = new int[180][2*diagonal];

        for (int x = 0; x < image.getWidth(); x++){
            for (int y = 0; y < image.getHeight(); y++){
                int grayValue = GlobalHelperFunctions.calculateGrayValueFromRGB(image.getRGB(x, y));
                if (grayValue == 255){
                    for (int phi = 0; phi < 180; phi++){
                        int r = (int) (x * Math.cos(Math.toRadians(phi)) + y * Math.sin(Math.toRadians(phi)));
                        r += diagonal;
                        accumulator[phi][r] += 1;
                    }
                }
            }
        }
        return accumulator;
    }


    // Helper
    private static double calculateValueAndMask(double[][] mask, BufferedImage grayScaleImage, int distance, int x, int y) {
        double value = 0;

        for (int c = -distance; c <= distance; c++) {
            for (int r = -distance; r <= distance; r++) {
                double grayValue = GlobalHelperFunctions.calculateGrayValueFromRGB(grayScaleImage.getRGB(x+c, y+r));
                double grayXmask = grayValue * mask[c+distance][r+distance];
                value += grayXmask;

            }
        }
        return  value;
    }
}
