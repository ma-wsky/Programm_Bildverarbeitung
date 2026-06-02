import java.awt.image.BufferedImage;

void main() {
    ImageIO io = new ImageIO();
    BufferedImage input = io.readImageToPPM("pics/obama.jpg", "obama.ppm");
    ColorManipulation colorManipulation = new ColorManipulation();
    colorManipulation.grayScale(input);
    io.displayImage(input);

    ImageManipulation manipulation = new ImageManipulation(input);
    //manipulation.equidensityFirstOrderGrayImage();
    //manipulation.equidensitySecondOrderGrayImage();

    //manipulation.histogramEqualization(input);
    manipulation.gammaCorrection(0.3, input);

    BufferedImage gamma = io.readPPM(new File("gammaCorrected.ppm"));
    DescriptiveStatistics stats = new DescriptiveStatistics(gamma);
    stats.calculateAllStatistics();
    stats.printStatistics();
}