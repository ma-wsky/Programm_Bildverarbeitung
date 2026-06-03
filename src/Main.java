import java.awt.image.BufferedImage;

void main() {
    /*ImageIO io = new ImageIO();
    BufferedImage input = io.readImageAndConvertToPPM("pics/obama.jpg", "generated/obama.ppm");
    BufferedImage grayScaleImage = ColorManipulation.grayScale(input);
    ImageIO.displayImage(input);
    ImageIO.displayImage(grayScaleImage);

    DescriptiveStatistics stats = new DescriptiveStatistics(input);
    stats.calculateAllStatistics();
    stats.printStatistics();

    ImageIO.displayImage(ImageManipulation.linearScaleGrayImage(input, 0, 4));
    ImageIO.displayImage(ImageManipulation.equidensityFirstOrderGrayImage(input));
    ImageIO.displayImage(ImageManipulation.equidensitySecondOrderGrayImage(input));
    ImageIO.displayImage(ImageManipulation.histogramEqualization(input));
    ImageIO.displayImage(ImageManipulation.gammaCorrection(input, 3));
    */

    BufferedImage input = ImageIO.readImage("pics/tower-bridge.jpg");
    ImageIO.copyPPM(input, "output.ppm");
    BufferedImage input2 = ImageIO.readPPM("output.ppm");

    ImageIO.displayImage(ColorManipulation.grayScale(input2));
    //ImageIO.displayImage(Convolution.sobelFilter(input2, 1));
    //ImageIO.displayImage(Convolution.sobelFilter(input2, 2));
    //ImageIO.displayImage(Convolution.sobelFilter(input2, 3));
    BufferedImage smooth = Convolution.gaussianLowPass(input2, 3);
    ImageIO.displayImage(Convolution.laplaceFilter(smooth));
    ImageIO.displayImage(Convolution.sobelFilter(smooth, 3));
    double[][] mask = new double[15][15];
    for (int i = 0; i < mask.length; i++) {
        for (int j = 0; j < mask[0].length; j++) {
            mask[i][j] = 1;
        }
    }
    //ImageIO.displayImage(Convolution.movingMean(input, mask));

    //ImageIO.displayImage(Convolution.gaussianLowPass(input, 15));
}