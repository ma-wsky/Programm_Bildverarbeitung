package classes.Pipeline;

import classes.Pipeline.Helper.GlobalHelperFunctions;
import classes.ImageIO;

import java.awt.image.BufferedImage;

public class ColorManipulation {

    /**
     * Converts a copy of a given BufferedImage to grayscale.
     * Reads each pixel and calculates grayscale value before overwriting the pixels color.
     * @param image BufferedImage to be converted
     * @return copy of the image in gray scale
     */
    public static BufferedImage grayScale(BufferedImage image) {
        BufferedImage grayScaleImage = ImageIO.copyBufferedImage(image);

        for (int x = 0; x < grayScaleImage.getWidth(); x++) {
            for (int y = 0; y < grayScaleImage.getHeight(); y++) {
                int rgb = grayScaleImage.getRGB(x, y);
                int a = (rgb >> 16) & 0xff;

                int gray = GlobalHelperFunctions.calculateGrayValueFromRGB(rgb);

                int newRgb = (a << 24) | (gray << 16) | (gray << 8) | gray;

                grayScaleImage.setRGB(x, y, newRgb);
            }
        }
        return grayScaleImage;
    }

    /**
     * Converts a given BufferedImage to negative.
     * Reads each pixel and calculates negative value before overwriting the pixels color.
     * @param image BufferedImage to be converted
     * @return copy of the image as negative
     */
    public static BufferedImage negative(BufferedImage image) {
        BufferedImage negativeImage = ImageIO.copyBufferedImage(image);

        for (int x = 0; x < negativeImage.getWidth(); x++) {
            for (int y = 0; y < negativeImage.getHeight(); y++) {
                int rgb = negativeImage.getRGB(x, y);
                int a = (rgb >> 24) & 0xff;
                int r = (rgb >> 16) & 0xff;
                int g = (rgb >> 8) & 0xff;
                int b = rgb & 0xff;

                int nr = 255 - r;
                int ng = 255 - g;
                int nb = 255 - b;

                int newRgb = (a << 24) | (nr << 16) | (ng << 8) | nb;
                negativeImage.setRGB(x, y, newRgb);
            }
        }
        return negativeImage;
    }
}
