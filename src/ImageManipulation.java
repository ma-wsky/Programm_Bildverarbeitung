import java.awt.image.BufferedImage;
import java.io.IOException;

public class ImageManipulation {

    /**
     * performs linear scale of gray values with equation f(g)=c2*g+c1*c2 on a BufferedImage.
     * Saves the image in PPM format and returns it.
     * @param c1 c1
     * @param c2 c2
     * @return BufferedImage after applying linear transformation
     */
    public static BufferedImage linearScaleGrayImage(BufferedImage image, int c1, double c2){
        // c2 = 1, c1 > 0 => brighter
        // c2 = 1, c1 < 0 => darker
        // 0 < c2 < 1 , c1 = 0 => lower contrast
        // c2 > 1 , c1 = 0 => higher contrast

        BufferedImage grayScaleImage = ColorManipulation.grayScale(image);
        int[] lookUpTable = new int[grayScaleImage.getWidth() * grayScaleImage.getHeight()];
        for (int i = 0; i < lookUpTable.length; i++) {
            lookUpTable[i] = -1;
        }

        double c = c1*c2;

        for (int x = 0; x < grayScaleImage.getWidth(); x++) {
            for (int y = 0; y < grayScaleImage.getHeight(); y++) {
                int rgb = grayScaleImage.getRGB(x, y);
                int a = (rgb >> 24) & 0xff;

                int gray = GlobalHelperFunctions.calculateGrayValueFromRGB(rgb);

                if (lookUpTable[gray] == -1){
                    //calc
                    int value = (int) (c2*gray+c);
                    if (value > 255) {
                        value = 255;
                    }else if (value < 0){
                        value = 0;
                    }
                    lookUpTable[gray] = value;
                }

                int newGray = lookUpTable[gray];

                int newRgb = (a << 24) | (newGray << 16) | (newGray << 8) | newGray;

                grayScaleImage.setRGB(x, y, newRgb);
            }
        }
        String filename = "generated/linearScaleGrayImage.ppm";
        try {
            ImageIO.saveBufferedImageAsPPM(grayScaleImage, filename);
        } catch (IOException e) {
            System.err.println("Error saving " + filename + " as ppm.\n" + e.getMessage());
        }

        return grayScaleImage;
    }

    /**
     * Performs first order equidensity operation on given image.
     * Converts given image to gray scale with {@link ColorManipulation#grayScale(BufferedImage)}.
     * Saves the image in generated/firstOrderEquidensityGrayImage.ppm
     * @param image image to be operated on
     * @return BufferedImage
     */
    public static BufferedImage equidensityFirstOrderGrayImage(BufferedImage image){
        BufferedImage grayScaleImage = ColorManipulation.grayScale(image);

        DescriptiveStatistics stats = new DescriptiveStatistics(grayScaleImage);
        stats.calculateGrayValueMatrix();
        stats.calculateMean();
        stats.calculateVariance();
        int mean = (int) stats.getMean();
        int bound = (int) Math.sqrt(stats.getVariance());
        int lowerBound = mean - bound;
        int upperBound = mean + bound;

        for(int x = 0; x < grayScaleImage.getWidth(); x++){
            for(int y = 0; y < grayScaleImage.getHeight(); y++){
                int rgb = grayScaleImage.getRGB(x, y);
                int a = (rgb >> 24) & 0xff;

                int gray = GlobalHelperFunctions.calculateGrayValueFromRGB(rgb);

                if (gray < lowerBound){
                    gray = 0;
                }else if (gray < upperBound){
                    gray = 255;
                }else {
                    gray = mean;
                }

                int newRgb = (a << 24) | (gray << 16) | (gray << 8) | gray;

                grayScaleImage.setRGB(x, y, newRgb);
            }
        }
        String filename = "generated/firstOrderEquidensityGrayImage.ppm";
        try {
            ImageIO.saveBufferedImageAsPPM(grayScaleImage, filename);
        } catch (IOException e) {
            System.err.println("Error saving " + filename + " as ppm.\n" + e.getMessage());
        }

        return grayScaleImage;
    }

    /**
     * Performs second order equidensity operation on given image.
     * Converts given image to gray scale with {@link ColorManipulation#grayScale(BufferedImage)}.
     * Calls {@link #equidensityFirstOrderGrayImage(BufferedImage)} for the first order
     * Saves the image in generated/secondOrderEquidensityGrayImage.ppm
     * @param image image to be operated on
     * @return BufferedImage
     */
    public static BufferedImage equidensitySecondOrderGrayImage(BufferedImage image){
        BufferedImage grayScaleImage = ColorManipulation.grayScale(image);

        // calc first order
        BufferedImage firstOrderImage = ImageManipulation.equidensityFirstOrderGrayImage(image);

        for(int x = 1; x < firstOrderImage.getWidth()-1; x++){
            for(int y = 1; y < firstOrderImage.getHeight()-1; y++){
                int a = (firstOrderImage.getRGB(x, y) >> 24) & 0xff;

                int gray = GlobalHelperFunctions.calculateGrayValueFromRGB(firstOrderImage.getRGB(x, y));
                int grayNegY = GlobalHelperFunctions.calculateGrayValueFromRGB(firstOrderImage.getRGB(x, y-1));
                int grayPosY = GlobalHelperFunctions.calculateGrayValueFromRGB(firstOrderImage.getRGB(x, y+1));
                int grayNegX = GlobalHelperFunctions.calculateGrayValueFromRGB(firstOrderImage.getRGB(x-1, y));
                int grayPosX = GlobalHelperFunctions.calculateGrayValueFromRGB(firstOrderImage.getRGB(x+1, y));

                int diff = 4*gray - grayNegY - grayPosY - grayNegX - grayPosX;
                int newGray;

                if (diff == 0){
                    newGray = 255;
                }else{
                    newGray = 0;
                }

                int newRgb = (a << 24) | (newGray << 16) | (newGray << 8) | newGray;

                grayScaleImage.setRGB(x, y, newRgb);
            }
        }
        String filename = "generated/secondOrderEquidensityGrayImage.ppm";
        try {
            ImageIO.saveBufferedImageAsPPM(grayScaleImage, filename);
        } catch (IOException e) {
            System.err.println("Error saving " + filename + " as ppm.\n" + e.getMessage());
        }

        return grayScaleImage;
    }

    /**
     * Calculates histogram equalization for given BufferedImage.
     * Converts given image to gray scale with {@link ColorManipulation#grayScale(BufferedImage)}.
     * Uses {@link DescriptiveStatistics#calculateRelativeCumulativeFrequencyArray()} to get the cumulative frequency.
     * Saves the image in generated/equalizedHistogramGrayImage.ppm
     * @param image BufferedImage to perform on
     * @return equalized BufferedImage
     */
    public static BufferedImage histogramEqualization(BufferedImage image){
        BufferedImage grayScaleImage = ColorManipulation.grayScale(image);

        DescriptiveStatistics stats = new DescriptiveStatistics(grayScaleImage);
        stats.calculateRelativeCumulativeFrequencyArray();
        Double[] cfreq = stats.getRelativeCumulativeFrequency();
        int[] lut = new int[cfreq.length];

        for(int i = 0; i < cfreq.length; i++){
            lut[i] = (int) Math.floor(255*cfreq[i]);
        }

        for(int x = 0; x < grayScaleImage.getWidth(); x++){
            for(int y = 0; y < grayScaleImage.getHeight(); y++){
                int a =  (grayScaleImage.getRGB(x, y) >> 24) & 0xff;
                int gray = GlobalHelperFunctions.calculateGrayValueFromRGB(grayScaleImage.getRGB(x, y));

                int newGray = lut[gray];

                int newRgb = (a << 24) | (newGray << 16) | (newGray << 8) | newGray;

                grayScaleImage.setRGB(x, y, newRgb);
            }
        }
        String filename = "generated/equalizedHistogramGrayImage.ppm";
        try {
            ImageIO.saveBufferedImageAsPPM(grayScaleImage, filename);
        } catch (IOException e) {
            System.err.println("Error saving " + filename + " as ppm.\n" + e.getMessage());
        }

        return grayScaleImage;
    }


    /**
     * Performs a gamma correction on a given BufferedImage.
     * Converts given image to gray scale with {@link ColorManipulation#grayScale(BufferedImage)}.
     * @param image BufferedImage to correct
     * @param gamma gamma value
     * @return corrected BufferedImage
     */
    public static BufferedImage gammaCorrection(BufferedImage image, double gamma){
        BufferedImage grayScaleImage = ColorManipulation.grayScale(image);

        for(int x = 0; x < grayScaleImage.getWidth(); x++){
            for(int y = 0; y < grayScaleImage.getHeight(); y++){
                int a =  (grayScaleImage.getRGB(x, y) >> 24) & 0xff;
                int gray = GlobalHelperFunctions.calculateGrayValueFromRGB(grayScaleImage.getRGB(x, y));

                int newGray = (int) (Math.pow(gray/255.0, gamma) * 255);

                int newRgb = (a << 24) | (newGray << 16) | (newGray << 8) | newGray;

                grayScaleImage.setRGB(x, y, newRgb);
            }
        }
        String filename = "generated/gammaCorrectedGrayImage.ppm";
        try {
            ImageIO.saveBufferedImageAsPPM(grayScaleImage, filename);
        } catch (IOException e) {
            System.err.println("Error saving " + filename + " as ppm.\n" + e.getMessage());
        }

        return grayScaleImage;
    }

}
