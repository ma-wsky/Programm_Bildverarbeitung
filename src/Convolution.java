import java.awt.image.BufferedImage;

public class Convolution {

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

        int maskSize = mask.length;
        int distance = maskSize / 2;

        // edge case: cutting
        for (int x = distance; x < grayScaleImage.getWidth()-distance; x++) {
            for (int y = distance; y < grayScaleImage.getHeight()-distance; y++) {

                double mean = 0;

                for (int r = -distance; r <= distance; r++) {
                    for (int c = -distance; c <= distance; c++) {
                        double grayValue = GlobalHelperFunctions.calculateGrayValueFromRGB(grayScaleImage.getRGB(x+r, y+c));
                        double grayXmask = grayValue * mask[r+distance][c+distance];
                        mean += grayXmask;

                    }
                }

                double maskSum = 0;
                for (double[] r : mask){
                    for (double c : r){
                        maskSum += c;
                    }
                }

                mean /= maskSum;

                int a =  (grayScaleImage.getRGB(x, y) >> 24) & 0xff;
                int newRgb = (a << 24) | ((int)mean << 16) | ((int)mean << 8) | (int)mean;
                grayScaleImage.setRGB(x, y, newRgb);
            }
        }
        return grayScaleImage;
    }


    /**
     * Calculates the mask for a gaussianLowPass using the two-dimensional gaussian distribution.
     * Calls {@link Convolution#movingMean(BufferedImage, double[][])} to perform algorithm on image.
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
        double[][] mask = new double[maskSize][maskSize];
        int distance = maskSize / 2;

        double sigma = maskSize / 6.0;

        for(int x = -distance; x <= distance; x++){
            for(int y = -distance; y <= distance; y++){
                // two-dimensional gaussian distribution
                mask[x+distance][y+distance] = Math.exp(-((Math.pow(x, 2) + Math.pow(y, 2)) / (2 * Math.pow(sigma, 2))));
            }
        }

        return Convolution.movingMean(image, mask);
    }

}
