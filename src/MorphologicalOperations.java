import java.awt.image.BufferedImage;

public class MorphologicalOperations {

    /**
     * Calls {@link MorphologicalOperations#morph(BufferedImage, boolean[][], boolean)} with dilationFlag = true.
     * @param image BufferedImage to dilate
     * @param mask marks neighbours to consider
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
     * @param mask marks neighbours to consider
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
     * @param mask marks neighbours to consider
     * @param dilationFlag true for dilation, false for erosion
     * @return dilated or eroded BufferedImage
     */
    private static BufferedImage morph(BufferedImage image, boolean[][] mask, boolean dilationFlag){
        if(mask.length % 2 == 0){
            // matrix has no middle
            System.err.println("The mask must have uneven number of elements.");
            return null;
        }

        BufferedImage grayScaleImage = ColorManipulation.grayScale(image);
        BufferedImage newImage = ImageIO.copyBufferedImage(grayScaleImage);

        int distance = mask.length / 2;

        // edge case: cutting
        for (int x = distance; x < image.getWidth()-distance; x++){
            for (int y = distance; y < image.getHeight()-distance; y++){

                double value = GlobalHelperFunctions.calculateGrayValueFromRGB(grayScaleImage.getRGB(x, y));

                // iterate mask and determine value
                for (int c = -distance; c <= distance; c++){
                    for (int r = -distance; r <= distance; r++){
                        double grayValue = GlobalHelperFunctions.calculateGrayValueFromRGB(grayScaleImage.getRGB(x+c, y+r));

                        // dilation
                        if (dilationFlag && mask[c+distance][r+distance] && grayValue > value){
                            value = grayValue;
                        }
                        // erosion
                        if (!dilationFlag && mask[c+distance][r+distance] && grayValue < value){
                            value = grayValue;
                        }
                    }
                }

                int a =  (grayScaleImage.getRGB(x, y) >> 24) & 0xff;
                int newRgb = (a << 24) | ((int)value << 16) | ((int)value << 8) | (int)value;
                newImage.setRGB(x, y, newRgb);
            }
        }

        return newImage;
    }

}
