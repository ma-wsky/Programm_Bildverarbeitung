import java.awt.image.BufferedImage;

public class ColorManipulation {

    /**
     * Converts a given BufferedImage to grayscale.
     * Reads each pixel and calculates grayscale value before overwriting the pixels color.
     * @param image BufferedImage to be converted
     */
    void grayScale(BufferedImage image) {
        for (int x = 0; x < image.getWidth(); x++) {
            for (int y = 0; y < image.getHeight(); y++) {
                int rgb = image.getRGB(x, y);

                int a = (rgb >> 24) & 0xff;
                int r = (rgb >> 16) & 0xff;
                int g = (rgb >> 8) & 0xff;
                int b = rgb & 0xff;

                int gray = (r + g + b) / 3;

                int newRgb = (a << 24) | (gray << 16) | (gray << 8) | gray;

                image.setRGB(x, y, newRgb);
            }
        }
    }

    /**
     * Converts a given BufferedImage to negative.
     * Reads each pixel and calculates negative value before overwriting the pixels color.
     * @param image BufferedImage to be converted
     */
    void negative(BufferedImage image) {
        for (int x = 0; x < image.getWidth(); x++) {
            for (int y = 0; y < image.getHeight(); y++) {
                int rgb = image.getRGB(x, y);
                int a = (rgb >> 24) & 0xff;
                int r = (rgb >> 16) & 0xff;
                int g = (rgb >> 8) & 0xff;
                int b = rgb & 0xff;

                int nr = 255 - r;
                int ng = 255 - g;
                int nb = 255 - b;

                int newRgb = (a << 24) | (nr << 16) | (ng << 8) | nb;
                image.setRGB(x, y, newRgb);
            }
        }
    }
}
