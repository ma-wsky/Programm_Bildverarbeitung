package classes.Pipeline;

import classes.ColorManipulation;
import classes.DescriptiveStatistics;
import classes.GlobalHelperFunctions;

import java.awt.image.BufferedImage;

public class PipelineImageManipulation {

    /**
     * Performs first order equidensity operation on given image.
     * Converts given image to gray scale with {@link ColorManipulation#grayScale(BufferedImage)}.
     * @param image image to be operated on
     * @param lowerBound value of lower bound
     * @param upperBound value of upper bound
     * @param valueLowest value pixels between 0 and lowerBound should take
     * @param valueBetween value pixels between lowerBound and upperBound should take
     * @param valueHighest value pixels between upperBound and 255 should take
     * @return BufferedImage
     */
    public static BufferedImage equidensityFirstOrderGrayImageCustomBounds(BufferedImage image, int lowerBound, int upperBound, int valueLowest, int valueBetween, int valueHighest){
        BufferedImage grayScaleImage = ColorManipulation.grayScale(image);

        // each pixel
        for(int x = 0; x < grayScaleImage.getWidth(); x++){
            for(int y = 0; y < grayScaleImage.getHeight(); y++){

                int rgb = grayScaleImage.getRGB(x, y);
                int a = (rgb >> 24) & 0xff;

                int gray = GlobalHelperFunctions.calculateGrayValueFromRGB(rgb);

                // give pixel new value
                if (gray < lowerBound){
                    gray = valueLowest;
                }else if (gray < upperBound){
                    gray = valueHighest;
                }else {
                    gray = valueBetween;
                }

                int newRgb = (a << 24) | (gray << 16) | (gray << 8) | gray;

                grayScaleImage.setRGB(x, y, newRgb);
            }
        }

        return grayScaleImage;
    }

    /**
     * Calculates histogram equalization for given BufferedImage.
     * Converts given image to gray scale with {@link ColorManipulation#grayScale(BufferedImage)}.
     * Uses {@link DescriptiveStatistics#calculateRelativeCumulativeFrequencyArray()} to get the cumulative frequency.
     * Generated a LUT based on the relative cumulative frequency and maps each pixel to a new value.
     * @param image BufferedImage to perform on
     * @return equalized BufferedImage
     */
    public static BufferedImage histogramEqualization(BufferedImage image){
        BufferedImage grayScaleImage = ColorManipulation.grayScale(image);

        // calculate relative cumulative frequency
        DescriptiveStatistics stats = new DescriptiveStatistics(grayScaleImage);
        stats.calculateRelativeCumulativeFrequencyArray();
        Double[] cumulativeFrequency = stats.getRelativeCumulativeFrequency();

        // fill look up table
        int[] lut = new int[cumulativeFrequency.length];

        for(int i = 0; i < cumulativeFrequency.length; i++){
            lut[i] = (int) Math.floor(255*cumulativeFrequency[i]);
        }

        // map pixels to new values
        for(int x = 0; x < grayScaleImage.getWidth(); x++){
            for(int y = 0; y < grayScaleImage.getHeight(); y++){
                int a =  (grayScaleImage.getRGB(x, y) >> 24) & 0xff;
                int gray = GlobalHelperFunctions.calculateGrayValueFromRGB(grayScaleImage.getRGB(x, y));

                int newGray = lut[gray];

                int newRgb = (a << 24) | (newGray << 16) | (newGray << 8) | newGray;

                grayScaleImage.setRGB(x, y, newRgb);
            }
        }

        return grayScaleImage;
    }

}
