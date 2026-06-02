import java.awt.image.BufferedImage;

void main() {
    ImageIO io = new ImageIO();
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
}