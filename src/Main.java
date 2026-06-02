import java.awt.image.BufferedImage;

void main() {
    ImageIO io = new ImageIO();
    BufferedImage input = io.readImageToPPM("pics/obama.jpg", "obama.ppm");
    ColorManipulation colorManipulation = new ColorManipulation();
    colorManipulation.grayScale(input);
    io.displayImage(input);

    ImageManipulation manipulation = new ImageManipulation(input);
    manipulation.equidensityFirstOrderGrayImage();
    manipulation.equidensitySecondOrderGrayImage();

    DescriptiveStatistics stats = new DescriptiveStatistics(input);
    stats.calculateAllStatistics();
    stats.printStatistics();
}