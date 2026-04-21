import java.awt.image.BufferedImage;

void main() {
    ImageIO io = new ImageIO();
    BufferedImage castlePPM = io.readImageToPPM("pics/castle.jpg", "castle.ppm");
    io.displayImage(castlePPM);

    DescriptiveStatistics stats = new DescriptiveStatistics(castlePPM);
    stats.calculateAllStatistics();
    stats.printStatistics();
}