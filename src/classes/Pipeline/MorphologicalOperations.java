package classes.Pipeline;

import classes.Pipeline.Helper.GlobalHelperFunctions;

import java.awt.image.BufferedImage;

public class MorphologicalOperations {

    /**
     * Calls {@link MorphologicalOperations#morph(BufferedImage, boolean[][], boolean)} with dilationFlag = true.
     * @param image BufferedImage to dilate
     * @param mask marks neighbors to consider
     * @param times how often to perform dilation
     * @return dilated BufferedImage
     */
    public static BufferedImage dilation (BufferedImage image, boolean[][] mask, int times){
        for (int i = 0; i < times; i++){
            image = MorphologicalOperations.morph(image, mask, true);
        }
        return MorphologicalOperations.morph(image, mask, true);
    }

    /**
     * Calls {@link MorphologicalOperations#morph(BufferedImage, boolean[][], boolean)} with dilationFlag = false.
     * @param image BufferedImage to erode
     * @param mask marks neighbors to consider
     * @param times how often to perform erosion
     * @return eroded BufferedImage
     */
    public static BufferedImage erosion (BufferedImage image, boolean[][] mask, int times){
        for (int i = 0; i < times; i++){
            image = MorphologicalOperations.morph(image, mask, false);
        }
        return MorphologicalOperations.morph(image, mask, false);
    }

    /**
     * Calculates dilation or erosion of image based on dilationFlag.
     * @param image BufferedImage to operate on
     * @param mask marks neighbors to consider
     * @param dilationFlag true for dilation, false for erosion
     * @return dilated or eroded BufferedImage
     */
    private static BufferedImage morph(BufferedImage image, boolean[][] mask, boolean dilationFlag){

        // early exit
        if(mask.length % 2 == 0){
            // matrix has no middle
            System.err.println("The mask must have uneven number of elements.");
            return null;
        }

        // create images
        BufferedImage grayScaleImage = ColorManipulation.grayScale(image);
        BufferedImage newImage = new BufferedImage(image.getWidth(), image.getHeight(), BufferedImage.TYPE_BYTE_GRAY);

        int distance = mask.length / 2;

        // dilation
        if (dilationFlag){
            // edge case: cutting
            for (int x = distance; x < image.getWidth()-distance; x++){
                for (int y = distance; y < image.getHeight()-distance; y++){

                    double value = GlobalHelperFunctions.calculateGrayValueFromRGB(grayScaleImage.getRGB(x, y));

                    // iterate mask and determine value
                    for (int c = -distance; c <= distance; c++){
                        for (int r = -distance; r <= distance; r++){
                            if (!mask[c+distance][r+distance]) continue;

                            double grayValue = GlobalHelperFunctions.calculateGrayValueFromRGB(grayScaleImage.getRGB(x+c, y+r));

                            // dilation
                            if (grayValue > value){
                                value = grayValue;
                            }
                        }
                    }

                    int a =  (grayScaleImage.getRGB(x, y) >> 24) & 0xff;
                    int newRgb = (a << 24) | ((int)value << 16) | ((int)value << 8) | (int)value;
                    newImage.setRGB(x, y, newRgb);
                }
            }
        } else {
            // erosion
            for (int x = distance; x < image.getWidth()-distance; x++){
                for (int y = distance; y < image.getHeight()-distance; y++){

                    double value = GlobalHelperFunctions.calculateGrayValueFromRGB(grayScaleImage.getRGB(x, y));

                    // iterate mask and determine value
                    for (int c = -distance; c <= distance; c++){
                        for (int r = -distance; r <= distance; r++){
                            if (!mask[c+distance][r+distance]) continue;

                            double grayValue = GlobalHelperFunctions.calculateGrayValueFromRGB(grayScaleImage.getRGB(x+c, y+r));

                            // erosion
                            if (grayValue < value){
                                value = grayValue;
                            }
                        }
                    }

                    int a =  (grayScaleImage.getRGB(x, y) >> 24) & 0xff;
                    int newRgb = (a << 24) | ((int)value << 16) | ((int)value << 8) | (int)value;
                    newImage.setRGB(x, y, newRgb);
                }
            }
        }

        return newImage;
    }

}
