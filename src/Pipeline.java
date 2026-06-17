import java.awt.*;
import java.awt.image.BufferedImage;
import java.util.Vector;

public class Pipeline {
    // Stopp, Vorfahrt Achten und Vorfahrtsstraße, Vorfahrt
    // TODO: Pipeline für die Erkennung bauen

    // 1. Bild laden (und in dein Arbeitsformat PPM konvertieren).
    // 2. Rauschen filtern mit Gauß-Tiefpass.
    // 3. Kontrast anheben via Histogrammausgleich.
    // 4. Schildfläche isolieren via Äquidensiten (Gelb- und Weißwerte filtern).
    // 5. Form säubern mit Erosion und Dilatation.
    // 6. Kanten hervorheben mit Sobel.
    // 7. Geometrie prüfen: Region um 45° rotieren und prüfen, ob ein Quadrat entsteht.
    // 8. Gegenprüfen: Entropie und Varianz der Region berechnen, um sicherzustellen, dass es eine glatte Schildoberfläche ist.

    public static void vorfahrt(){
        // 1.
        BufferedImage readImage = ImageIO.readImage("pics/sample/V.jpg");
        ImageIO.copyPPM(readImage, "V.ppm");
        BufferedImage V = ImageIO.readPPM("V.ppm");
        System.out.println("Successfully loaded image V.ppm");

        // 2.
        BufferedImage gauss = Convolution.gaussianLowPass(V, 5);
        System.out.println("Successfully calculated gauss lowpass");

        // 3.
        BufferedImage higherContrast = ImageManipulation.histogramEqualization(gauss);
        System.out.println("Successfully calculated histogram equalization");

        // 4.
        BufferedImage equidensity = ImageManipulation.equidensityFirstOrderGrayImageCustomBounds(higherContrast, 30, 200, 255, 0, 0);
        System.out.println("Successfully calculated equidensity");
        //ImageIO.displayImage(equidensity);

        // 5.
        int maskSize = 3;
        boolean[][] mask = new boolean[maskSize][maskSize];
        for (int x = 0; x < maskSize; x++){
            for (int y = 0; y < maskSize; y++){
                mask[x][y] = true;
            }
        }

        // make edge black
        for (int x = 0; x < equidensity.getWidth(); x++){
            equidensity.setRGB(x, 0, 0);
            equidensity.setRGB(x, equidensity.getHeight()-1, 0);
        }
        for (int y = 0; y < equidensity.getHeight(); y++){
            equidensity.setRGB(0, y, 0);
            equidensity.setRGB(equidensity.getWidth()-1, y, 0);
        }

        BufferedImage erosion = MorphologicalOperations.erosion(equidensity, mask, 2);
        BufferedImage dilation = MorphologicalOperations.dilation(erosion, mask, 2);
        ImageIO.displayImage(erosion);
        ImageIO.displayImage(dilation);


        // 6. find coords
        double xMin = dilation.getWidth() - 1;
        double xMax = 0;
        double yMin = dilation.getHeight() - 1;
        double yMax = 0;

        for (int x = 0; x < dilation.getWidth(); x++){
            for (int y = 0; y < dilation.getHeight(); y++){
                int rgb = dilation.getRGB(x, y);
                int grayValue = GlobalHelperFunctions.calculateGrayValueFromRGB(rgb);

                if (grayValue == 255){
                    if (x < xMin){
                        xMin = x;
                    }
                    if (x > xMax){
                        xMax = x;
                    }
                    if (y < yMin){
                        yMin = y;
                    }
                    if (y > yMax){
                        yMax = y;
                    }
                }
            }
        }

        // calc middle
        double middleX = (xMin + xMax) / 2;
        double middleY = (yMin + yMax) / 2;

        BufferedImage rotated45 = RotatedImage.rotateImageBackwardMapping(dilation, new Point((int)middleX, (int)middleY), 45);
        ImageIO.displayImage(rotated45);

        // TODO:
        // 7. check geometry seitenverhältnis auf quadrat checken, kulanz für perspektivische verzerrung beachten
        // 8. check statistics entropie sollte gering sein -> homogen, varianz des gelben kerns sollte gering sein -> homogen
        // determine if sign
    }
}
