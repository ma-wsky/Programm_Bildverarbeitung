import java.awt.*;
import java.awt.image.BufferedImage;
import java.util.Arrays;

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
        // 1. read image                                    pics/vorfahrt/images.jpg
        BufferedImage readImage = ImageIO.readImage("pics/vorfahrt/images.jpg");
        ImageIO.copyPPM(readImage, "V.ppm");
        BufferedImage V = ImageIO.readPPM("V.ppm");
        System.out.println("Successfully loaded image V.ppm");

        // 2. lowpass
        BufferedImage gauss = Convolution.gaussianLowPass(V, 5);
        System.out.println("Successfully calculated gauss lowpass");
        ImageIO.displayImage(gauss);

        // 3. histogram equalization
        BufferedImage higherContrast = ImageManipulation.histogramEqualization(gauss);
        System.out.println("Successfully calculated histogram equalization");
        ImageIO.displayImage(higherContrast);

        // sobel
        BufferedImage sobel = Convolution.sobelFilter(higherContrast, 3);
        System.out.println("Successfully calculated sobel filter");
        ImageIO.displayImage(sobel);

        // 4. equidensity
        BufferedImage equidensity = ImageManipulation.equidensityFirstOrderGrayImageCustomBounds(sobel, 100, 200, 255, 0, 0);
        System.out.println("Successfully calculated equidensity");
        ImageIO.displayImage(equidensity);

        // negative
        BufferedImage negative = ColorManipulation.negative(equidensity);
        ImageIO.displayImage(negative);


        //make edge black
        int width = negative.getWidth();
        int height = negative.getHeight();
        int border = 5;

        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                if (x < border || x >= width - border || y < border || y >= height - border) {
                    negative.setRGB(x, y, 0xFF000000);
                }
            }
        }

        BufferedImage cleanBinaryImage = Pipeline.binaryImageFromRegionGrowth(negative);
        ImageIO.displayImage(cleanBinaryImage);

//        // 5. dilation and erosion
//        int maskSize = 3;
//        boolean[][] mask = new boolean[maskSize][maskSize];
//        for (int x = 0; x < maskSize; x++){
//            for (int y = 0; y < maskSize; y++){
//                mask[x][y] = true;
//            }
//        }
//
//        // make edge black
//        for (int x = 0; x < cleanBinaryImage.getWidth(); x++){
//            cleanBinaryImage.setRGB(x, 0, 0);
//            cleanBinaryImage.setRGB(x, cleanBinaryImage.getHeight()-1, 0);
//        }
//        for (int y = 0; y < cleanBinaryImage.getHeight(); y++){
//            cleanBinaryImage.setRGB(0, y, 0);
//            cleanBinaryImage.setRGB(cleanBinaryImage.getWidth()-1, y, 0);
//        }
//
//        BufferedImage erosion = MorphologicalOperations.erosion(cleanBinaryImage, mask, 2);
//        BufferedImage dilation = MorphologicalOperations.dilation(erosion, mask, 2);
//        ImageIO.displayImage(erosion);
//        ImageIO.displayImage(dilation);


        // 6. rotate image
        int[] coords = Pipeline.findCoords(cleanBinaryImage);

        // crop image
        // TODO: crop based on cleanBinaryImage
        assert V != null;
        BufferedImage cropped = Pipeline.cropAndMaskSign(V, cleanBinaryImage);
        ImageIO.displayImage(cropped);


        // calc middle
        double middleX = (double) (coords[0] + coords[1]) / 2;
        double middleY = (double) (coords[2] + coords[3]) / 2;

        BufferedImage rotated45 = RotatedImage.rotateImageBackwardMapping(cleanBinaryImage, new Point((int)middleX, (int)middleY), 45);
        ImageIO.displayImage(rotated45);

        // 7. check geometry
        int[] coordsRotated = Pipeline.findCoords(rotated45);
        System.out.println(Arrays.toString(coordsRotated));
        double distanceX = coordsRotated[1] - coordsRotated[0];
        double distanceY = coordsRotated[3] - coordsRotated[2];
        double ratio = distanceX / distanceY;
        System.out.println(ratio);
        double ratioBoundPercent = 0.1;

        boolean isSquare = (ratio < (1 + ratioBoundPercent) || ratio > (1 - ratioBoundPercent));
        if (isSquare){
            System.out.println("Quadrat erkannt!");
        }

        // 8. check statistics

        // rotate original
        BufferedImage rotatedV = RotatedImage.rotateImageBackwardMapping(V, new Point((int)middleX, (int)middleY), 45);

        // crop original
        BufferedImage croppedV = Pipeline.cropAndMaskSign(rotatedV, rotated45);
        ImageIO.displayImage(croppedV);

        // stats
        DescriptiveStatistics stats = new DescriptiveStatistics(croppedV);
        stats.calculateAllStatistics();

        stats.printStatistics();

        // scoring

        // TODO: check relativeCumulativeFrequency for two peaks with valley between, cumulate pixels in yellow and white in HSV values

        boolean entropy = (stats.getEntropy() < 3.0);
        boolean medianValid = (stats.getMedian() > 200);

        Double[] relCumFreq = stats.getRelativeCumulativeFrequency();
        double schildFlaeche = 1.0 - relCumFreq[0];
        double gelbAnteil  = (relCumFreq[191] - relCumFreq[181]) / schildFlaeche;
        double weissAnteil = (relCumFreq[246] - relCumFreq[221]) / schildFlaeche;

        int score = 0;

// geo
        if (isSquare){
            score += 40;
        }

// 1. Check: Hat das Schild den typischen gelben Kern? (Erwartet: ca. 20-30%)
        if (gelbAnteil > 0.18 && gelbAnteil < 0.35) {
            score += 30;
        }

// 2. Check: Hat das Schild den breiten weißen Rahmen? (Erwartet: ca. 40-55%)
        if (weissAnteil > 0.35 && weissAnteil < 0.60) {
            score += 30;
        }

// 3. Check: Die Entropie (Da der Hintergrund weg ist, ist sie gesunken auf 5,47!)
        if (stats.getEntropy() > 4.5 && stats.getEntropy() < 6.2) {
            score += 15;
        }

// Wenn Geometrie (Quadrat/Verzerrung) vorher schon z.B. 25 Punkte gegeben hat:


        if (score >= 60) {
            System.out.println("score: " + score + "/ 115");
            System.out.println("Vorfahrtsschild erkannt!");
        } else{
            System.out.println("score: " + score + "/ 115");
            System.out.println("Kein Vorfahrtsschild erkannt!");
        }

    }

    private static void findAllConnectedNeighbours(BufferedImage image, int[][] regions, int currentRegion, Point point) {
        regions[point.x][point.y] = currentRegion;

        for (int x = -1; x <= 1; x++){
            for (int y = -1; y <= 1; y++){
                if (x == 0 && y == 0) continue;

                int neighbourX = x + point.x;
                int neighbourY = y + point.y;

                if (neighbourX < 0 || neighbourX >= image.getWidth() || neighbourY < 0 || neighbourY >= image.getHeight()) continue;

                int neighbourValue = GlobalHelperFunctions.calculateGrayValueFromRGB(image.getRGB(neighbourX, neighbourY));

                if (neighbourValue == 255 && regions[neighbourX][neighbourY] == 0){
                    findAllConnectedNeighbours(image, regions, currentRegion, new Point(neighbourX, neighbourY));
                }
            }
        }
    }

    private static BufferedImage floodFillInterior(BufferedImage cleanImage, boolean[][] visited, Point point) {
        // Aktuellen Pixel als besucht markieren und im Bild WEISS färben
        visited[point.x][point.y] = true;
        cleanImage.setRGB(point.x, point.y, 0xFFFFFFFF);

        int[] dx = {0, 0, -1, 1};
        int[] dy = {-1, 1, 0, 0};

        for (int i = 0; i < 4; i++) {
            int neighbourX = point.x + dx[i];
            int neighbourY = point.y + dy[i];

            // Bounds-Check
            if (neighbourX < 0 || neighbourX >= cleanImage.getWidth() || neighbourY < 0 || neighbourY >= cleanImage.getHeight()) {
                continue;
            }

            // Wert aus dem sauber gemachten Binärbild holen
            int neighbourValue = GlobalHelperFunctions.calculateGrayValueFromRGB(cleanImage.getRGB(neighbourX, neighbourY));

            // WICHTIG: Wir fluten NUR, wenn der Pixel im Bild SCHWARZ (0) ist
            // UND wir in diesem Flood-Fill-Durchlauf noch nicht hier waren!
            if (neighbourValue == 0 && !visited[neighbourX][neighbourY]) {
                floodFillInterior(cleanImage, visited, new Point(neighbourX, neighbourY));
            }
        }
        return cleanImage;
    }

    private static BufferedImage binaryImageFromRegionGrowth(BufferedImage image){
        int[][] regions = new int[image.getWidth()][image.getHeight()];
        int currentRegion = 1;

        for (int x = 0; x < image.getWidth(); x++){
            for (int y = 0; y < image.getHeight(); y++){
                int value = GlobalHelperFunctions.calculateGrayValueFromRGB(image.getRGB(x, y));

                if (value == 255 && regions[x][y] == 0){
                    // new region
                    Pipeline.findAllConnectedNeighbours(image, regions, currentRegion, new Point(x, y));
                    currentRegion++;
                }
            }
        }

        int[] regionSizes = new int[currentRegion];

        for (int x = 0; x < image.getWidth(); x++) {
            for (int y = 0; y < image.getHeight(); y++) {
                int label = regions[x][y];
                if (label > 0) {
                    regionSizes[label]++;
                }
            }
        }

        int maxPixels = 0;
        int winnerRegionID = 0;

        for (int i = 1; i < regionSizes.length; i++) {
            if (regionSizes[i] > maxPixels) {
                maxPixels = regionSizes[i];
                winnerRegionID = i;
            }
        }

        BufferedImage cleanBinaryImage = new BufferedImage(
                image.getWidth(),
                image.getHeight(),
                BufferedImage.TYPE_BYTE_GRAY
        );

        for (int x = 0; x < image.getWidth(); x++) {
            for (int y = 0; y < image.getHeight(); y++) {

                if (regions[x][y] == winnerRegionID) {
                    // Gehört zum Schild -> Weiss (Grauwert 255)
                    cleanBinaryImage.setRGB(x, y, 0xFFFFFFFF);
                } else {
                    // Artefakt oder Hintergrund -> Schwarz (Grauwert 0)
                    cleanBinaryImage.setRGB(x, y, 0xFF000000);
                }

            }
        }

        int[] coords = Pipeline.findCoords(cleanBinaryImage);
        int centerX = (coords[0] + coords[1]) / 2;
        int centerY = (coords[2] + coords[3]) / 2;

        // Ein frisches Besucht-Array NUR für das Loch-Fluten erstellen
        boolean[][] floodFillVisited = new boolean[cleanBinaryImage.getWidth()][cleanBinaryImage.getHeight()];

        // Wir starten das Fluten direkt auf dem cleanBinaryImage und fangen das Rückgabebild auf

        // Jetzt ist das geflutete Bild fertig und kann zurückgegeben werden!
        return Pipeline.floodFillInterior(cleanBinaryImage, floodFillVisited, new Point(centerX, centerY));
    }

    private static int[] findCoords(BufferedImage image){
        // xMin, xMax, yMin, yMax
        int[] values = {image.getWidth() - 1, 0, image.getHeight() - 1, 0};

        for (int x = 0; x < image.getWidth(); x++){
            for (int y = 0; y < image.getHeight(); y++){
                int rgb = image.getRGB(x, y);
                int grayValue = GlobalHelperFunctions.calculateGrayValueFromRGB(rgb);

                if (grayValue == 255){
                    if (x < values[0]){
                        values[0] = x;
                    }
                    if (x > values[1]){
                        values[1] = x;
                    }
                    if (y < values[2]){
                        values[2] = y;
                    }
                    if (y > values[3]){
                        values[3] = y;
                    }
                }
            }
        }

        return values;
    }

    private static BufferedImage cropImage(BufferedImage image, int xMin, int xMax, int yMin, int yMax){
        int width = xMax - xMin;
        int height = yMax - yMin;

        if (width < 0 || height < 0){
            return null;
        }

        BufferedImage newImage = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);

        for (int x = 0; x < width; x++){
            for (int y = 0; y < height; y++){
                newImage.setRGB(x, y, image.getRGB(x+xMin, y+yMin));
            }
        }

        return newImage;
    }

    public static BufferedImage cropAndMaskSign(BufferedImage originalImage, BufferedImage filledMask) {
        // 1. Finde die exakten äußeren Koordinaten der weißen Fläche
        int[] coords = Pipeline.findCoords(filledMask);
        int xMin = coords[0];
        int xMax = coords[1];
        int yMin = coords[2];
        int yMax = coords[3];

        // Berechne die Breite und Höhe des benötigten Ausschnitts
        int cropWidth = xMax - xMin + 1;
        int cropHeight = yMax - yMin + 1;

        // 2. Erstelle ein neues, leeres Bild exakt in dieser Größe
        // (Nutze TYPE_INT_RGB für Farbe oder TYPE_BYTE_GRAY, je nachdem was dein Original ist)
        BufferedImage croppedSign = new BufferedImage(cropWidth, cropHeight, originalImage.getType());

        // 3. Kopiere nur die Pixel, die laut Maske zum Schild gehören
        for (int y = 0; y < cropHeight; y++) {
            for (int x = 0; x < cropWidth; x++) {
                // Berechne die Position im Originalbild
                int origX = xMin + x;
                int origY = yMin + y;

                // Prüfe auf der Maske, ob dieser Pixel weiß (255) ist
                int maskValue = GlobalHelperFunctions.calculateGrayValueFromRGB(filledMask.getRGB(origX, origY));

                if (maskValue == 255) {
                    // JA -> Pixel aus dem Original eins zu eins rüberkopieren
                    int originalColor = originalImage.getRGB(origX, origY);
                    croppedSign.setRGB(x, y, originalColor);
                } else {
                    // NEIN -> Hintergrund pixel wird im Ausschnitt schwarz (0)
                    croppedSign.setRGB(x, y, 0xFF000000);
                }
            }
        }

        return croppedSign;
    }
}
