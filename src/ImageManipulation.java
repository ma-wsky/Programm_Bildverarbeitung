import java.awt.image.BufferedImage;
import java.io.IOException;

public class ImageManipulation {

    private final BufferedImage image;
    private final BufferedImage grayScale;

    private final int[] LUT;

    /**
     * initializes look up table and buffered image
     * @param image image
     */
    ImageManipulation(BufferedImage image) {
        this.image = image;
        ImageIO io = new ImageIO();
        this.grayScale = io.copyImage(this.image, "grayScale.jpg");
        ColorManipulation colorManipulation = new ColorManipulation();
        colorManipulation.grayScale(this.image);

        this.LUT = new int[256];
        for (int i = 0; i < 256; i++) {
            this.LUT[i] = -1;
        }
    }

    /**
     * performs linear scale of gray values with equation f(g)=c2*g+c1*c2
     * @param c1 c1
     * @param c2 c2
     */
    void linearScaleGrayImage(int c1, double c2){
        // c2 = 1, c1 > 0 => brighter
        // c2 = 1, c1 < 0 => darker
        // 0 < c2 < 1 , c1 = 0 => lower contrast
        // c2 > 1 , c1 = 0 => higher contrast
        ImageIO io = new ImageIO();
        BufferedImage image = io.copyImage(this.grayScale, "linearScaleGrayImage.jpg");

        double c = c1*c2;

        for (int x = 0; x < image.getWidth(); x++) {
            for (int y = 0; y < image.getHeight(); y++) {
                int rgb = image.getRGB(x, y);

                int a = (rgb >> 24) & 0xff;
                int r = (rgb >> 16) & 0xff;
                int g = (rgb >> 8) & 0xff;
                int b = rgb & 0xff;

                int gray = (r + g + b) / 3;

                if (this.LUT[gray] == -1){
                    //calc
                    int value = (int) (c2*gray+c);
                    if (value > 255) {
                        value = 255;
                    }else if (value < 0){
                        value = 0;
                    }
                    this.LUT[gray] = value;
                }

                int newGray = this.LUT[gray];

                int newRgb = (a << 24) | (newGray << 16) | (newGray << 8) | newGray;

                image.setRGB(x, y, newRgb);
            }
        }
        io.displayImage(image);
    }

    void equidensityFirstOrderGrayImage(){
        ImageIO io = new ImageIO();
        BufferedImage image = io.copyImage(this.grayScale, "equidensityFirstOrderGrayImage.jpg");

        DescriptiveStatistics stats = new DescriptiveStatistics(image);
        stats.calculateGrayValueMatrix();
        stats.calculateMean();
        stats.calculateVariance();
        int mean = (int) stats.getMean();
        int bound = (int) Math.sqrt(stats.getVariance());
        int lowerBound = mean - bound;
        int upperBound = mean + bound;

        for(int x = 0; x < image.getWidth(); x++){
            for(int y = 0; y < image.getHeight(); y++){
                int rgb = image.getRGB(x, y);
                int a = (rgb >> 24) & 0xff;
                int r = (rgb >> 16) & 0xff;
                int g = (rgb >> 8) & 0xff;
                int b = rgb & 0xff;

                int gray = (r + g + b) / 3;

                if (gray < lowerBound){
                    gray = 0;
                }else if (gray < upperBound){
                    gray = 255;
                }else {
                    gray = mean;
                }

                int newRgb = (a << 24) | (gray << 16) | (gray << 8) | gray;

                image.setRGB(x, y, newRgb);
            }
        }
        try {
            io.saveBufferedImageAsPPM(image, "equidensityFirstOrderGrayImage.ppm");
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        io.displayImage(image);
    }

    void equidensitySecondOrderGrayImage(){
        ImageIO io = new ImageIO();
        BufferedImage image = io.copyImage(this.grayScale, "equidensitySecondOrderGrayImage.jpg");

        // calc first order
        this.equidensityFirstOrderGrayImage();
        BufferedImage firstOrder = io.readPPM("equidensityFirstOrderGrayImage.ppm");

        for(int x = 1; x < firstOrder.getWidth()-1; x++){
            for(int y = 1; y < firstOrder.getHeight()-1; y++){
                int a = (firstOrder.getRGB(x, y) >> 24) & 0xff;

                int gray = this.calculateGrayValueFromRGB(firstOrder.getRGB(x, y));
                int grayNegY = this.calculateGrayValueFromRGB(firstOrder.getRGB(x, y-1));
                int grayPosY = this.calculateGrayValueFromRGB(firstOrder.getRGB(x, y+1));
                int grayNegX = this.calculateGrayValueFromRGB(firstOrder.getRGB(x-1, y));
                int grayPosX = this.calculateGrayValueFromRGB(firstOrder.getRGB(x+1, y));

                int diff = 4*gray - grayNegY - grayPosY - grayNegX - grayPosX;
                int newGray;

                if (diff == 0){
                    newGray = 255;
                }else{
                    newGray = 0;
                }

                int newRgb = (a << 24) | (newGray << 16) | (newGray << 8) | newGray;

                image.setRGB(x, y, newRgb);
            }
        }
        try {
            io.saveBufferedImageAsPPM(image, "equidensitySecondOrderGrayImage.ppm");
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        io.displayImage(image);
    }

    void histogramEqualization(BufferedImage image){
        ImageIO io = new ImageIO();
        DescriptiveStatistics stats = new DescriptiveStatistics(image);
        stats.calculateGrayValueMatrix();
        stats.calculateRelativeCumulativeFrequencyArray();
        Double[] cfreq = stats.getRelativeCumulativeFrequency();
        int[] lut = new int[cfreq.length];

        for(int i = 0; i < cfreq.length; i++){
            lut[i] = (int) Math.floor(255*cfreq[i]);
        }

        for(int x = 0; x < image.getWidth(); x++){
            for(int y = 0; y < image.getHeight(); y++){
                int a =  (image.getRGB(x, y) >> 24) & 0xff;
                int gray = this.calculateGrayValueFromRGB(image.getRGB(x, y));

                int newGray = lut[gray];

                int newRgb = (a << 24) | (newGray << 16) | (newGray << 8) | newGray;

                image.setRGB(x, y, newRgb);
            }
        }
        try {
            io.saveBufferedImageAsPPM(image, "equalizedHistogram.ppm");
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        io.displayImage(image);
    }

    void gammaCorrection(double gamma, BufferedImage image){
        ImageIO io = new ImageIO();
        for(int x = 0; x < image.getWidth(); x++){
            for(int y = 0; y < image.getHeight(); y++){
                int a =  (image.getRGB(x, y) >> 24) & 0xff;
                int gray = this.calculateGrayValueFromRGB(image.getRGB(x, y));

                int newGray = (int) (Math.pow(gray/255.0, gamma) * 255);

                int newRgb = (a << 24) | (newGray << 16) | (newGray << 8) | newGray;

                image.setRGB(x, y, newRgb);
            }
        }
        try {
            io.saveBufferedImageAsPPM(image, "gammaCorrected.ppm");
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        io.displayImage(image);
    }

    int calculateGrayValueFromRGB(int rgb){
        int r = (rgb >> 16) & 0xff;
        int g = (rgb >> 8) & 0xff;
        int b = rgb & 0xff;
        return (r + g + b) / 3;
    }
}
