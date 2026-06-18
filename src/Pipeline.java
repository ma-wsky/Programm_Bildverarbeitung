import java.awt.*;
import java.awt.image.BufferedImage;

public class Pipeline {
    // Stopp, Vorfahrt Achten und Vorfahrtsstraße, Vorfahrt

    public static void vorfahrt(){
        // 1. read image
        String filenamePPMImage = "vorfahrt.ppm";
        BufferedImage input = ImageIO.readImageAndConvertToPPM("pics/vorfahrt/images (1).jpg", filenamePPMImage);
        System.out.println("Successfully loaded image " + filenamePPMImage);

        // 2. preprocess image
        BufferedImage preProcessedImage = Pipeline.imagePreprocessing(input);

        // 3. look for sign-geometry
        BufferedImage signGeometry = Pipeline.lookForSignGeometry(preProcessedImage);
        ImageIO.displayImage(signGeometry);

        // 4. check signs geometry
        boolean[] isSquare = {false};
        BufferedImage rotatedSignGeometry = Pipeline.rotateImageAndCheckGeometry(signGeometry, 0.1, isSquare);
        ImageIO.displayImage(rotatedSignGeometry);
        if (isSquare[0]){
            System.out.println("Quadrat erkannt!");
        }

        // 5. check sign statistics

        // rotate original
        Point centerPoint = Pipeline.calculateCenterPointOfSign(signGeometry);
        BufferedImage rotatedOriginal = RotatedImage.rotateImageBackwardMapping(input, centerPoint, 45);
        ImageIO.displayImage(rotatedOriginal);

        // crop original
        BufferedImage croppedRotatedOriginal = Pipeline.cropAndMaskSign(rotatedOriginal, rotatedSignGeometry);
        ImageIO.displayImage(croppedRotatedOriginal);

        // stats
        DescriptiveStatistics stats = new DescriptiveStatistics(croppedRotatedOriginal);
        stats.calculateAllStatistics();
        //stats.printStatistics();

        // scoring
        // TODO: rework scoring to make it more general
        // TODO: check relativeCumulativeFrequency for two peaks with valley between, cumulate pixels in yellow and white in HSV values

        // isSquare
        // entropy
        // median
        // white and yellow proportions of sign area -> and ratio
        // white and yellow centerpoints should be the same +/- tolerance

    }

    /**
     * Calculates the centre of the sign.
     * Calls {@link Pipeline#findCoordsOfSign(BufferedImage)}
     * @param image BufferedImage to calculate centre of
     * @return Point with centre coordinates
     */
    private static Point calculateCenterPointOfSign(BufferedImage image) {
        int[] coordsOfSign = Pipeline.findCoordsOfSign(image);
        int centerX = (coordsOfSign[0] + coordsOfSign[1]) / 2;
        int centerY = (coordsOfSign[2] + coordsOfSign[3]) / 2;

        return new Point(centerX, centerY);
    }

    /**
     * Rotates a given BufferedImage by 45 degrees around the centerpoint of the sign.
     * Calls {@link Pipeline#findCoordsOfSign(BufferedImage)} to get coordinates for the centre point
     * Calls {@link RotatedImage#rotateImageBackwardMapping(BufferedImage, Point, int)}
     * Checks for a square shape with tolerance.
     * @param image BufferedImage to rotate 45 degrees and check
     * @param squareTolerancePercent percent of tolerance for square
     * @param isSquare boolean array to fake call by reference for boolean value
     * @return 45 degrees rotated BufferedImage
     */
    private static BufferedImage rotateImageAndCheckGeometry(BufferedImage image, double squareTolerancePercent, boolean[] isSquare) {
        // rotate image
        int[] coordsOfSign = Pipeline.findCoordsOfSign(image);
        int xMin = coordsOfSign[0];
        int xMax = coordsOfSign[1];
        int yMin = coordsOfSign[2];
        int yMax = coordsOfSign[3];
        int centerX = (xMin + xMax) / 2;
        int centerY = (yMin + yMax) / 2;
        BufferedImage rotatedImage = RotatedImage.rotateImageBackwardMapping(image, new Point(centerX, centerY), 45);

        double distanceX = xMax - xMin;
        double distanceY = yMax - yMin;
        double ratio = distanceX / distanceY;

        isSquare[0] = (ratio < (1 + squareTolerancePercent) || ratio > (1 - squareTolerancePercent));

        return rotatedImage;
    }

    /**
     * Performs preprocessing on given BufferedImage:
     * Calls {@link Convolution#gaussianLowPass(BufferedImage, int)}
     * Calls {@link ImageManipulation#histogramEqualization(BufferedImage)}
     * Calls {@link Convolution#sobelFilter(BufferedImage, int)}
     * Calls {@link ImageManipulation#equidensityFirstOrderGrayImageCustomBounds(BufferedImage, int, int, int, int, int)}
     * Calls {@link ColorManipulation#negative(BufferedImage)}
     * @param image BufferedImage to preprocess
     * @return preprocessed BufferedImage
     */
    private static BufferedImage imagePreprocessing(BufferedImage image){

        // 2. lowpass
        BufferedImage lowpass = Convolution.gaussianLowPass(image, 5);
        System.out.println("Successfully calculated Gauß lowpass");
        ImageIO.displayImage(lowpass);

        // 3. histogram equalization
        BufferedImage equalizedHistogram = ImageManipulation.histogramEqualization(lowpass);
        System.out.println("Successfully calculated histogram equalization");
        ImageIO.displayImage(equalizedHistogram);

        // sobel
        BufferedImage sobel = Convolution.sobelFilter(equalizedHistogram, 3);
        System.out.println("Successfully calculated Sobel filter");
        ImageIO.displayImage(sobel);

        // 4. equidensity
        BufferedImage equidensity = ImageManipulation.equidensityFirstOrderGrayImageCustomBounds(sobel, 100, 200, 255, 0, 0);
        System.out.println("Successfully calculated equidensity");
        ImageIO.displayImage(equidensity);

        // negative
        BufferedImage negative = ColorManipulation.negative(equidensity);
        ImageIO.displayImage(negative);

        return negative;
    }

    /**
     * Performs region growing algorithm for a single region.
     * Looks for all neighbouring pixels that share the colour 255 white and have yet to be sorted into a region.
     * Marks the neighbouring white pixel with currentRegion.
     * Calls itself recursively with each neighbouring pixel in the region.
     * @param image BufferedImage to search in
     * @param regions pixelarray marking a region for each pixel
     * @param currentRegion current region
     * @param point point to start from
     */
    private static void findAllConnectedNeighbours(BufferedImage image, int[][] regions, int currentRegion, Point point) {
        regions[point.x][point.y] = currentRegion;

        // check 8 neighbours
        for (int x = -1; x <= 1; x++){
            for (int y = -1; y <= 1; y++){
                if (x == 0 && y == 0) continue;

                int neighbourX = point.x + x;
                int neighbourY = point.y + y;

                // check bounds
                if (neighbourX < 0 || neighbourX >= image.getWidth() || neighbourY < 0 || neighbourY >= image.getHeight()){
                    continue;
                }

                int neighbourValue = GlobalHelperFunctions.calculateGrayValueFromRGB(image.getRGB(neighbourX, neighbourY));

                if (neighbourValue == 255 && regions[neighbourX][neighbourY] == 0){
                    findAllConnectedNeighbours(image, regions, currentRegion, new Point(neighbourX, neighbourY));
                }
            }
        }
    }

    /**
     * Fills the interior of a region with white. Operates recursively on a binary image and fills all black with white.
     * Stops when reaching white pixels or pixels that have been visited.
     * Calls itself recursively with each black neighbouring pixel that is yet unvisited.
     * @param image binary BufferedImage to fill interior in
     * @param visited array of visited pixels
     * @param point point to start from
     * @return BufferedImage with filled interior
     */
    private static BufferedImage fillInterior(BufferedImage image, boolean[][] visited, Point point) {
        visited[point.x][point.y] = true;
        image.setRGB(point.x, point.y, 0xFFFFFFFF);

        // 4 cardinal neighbours
        int[] dx = {0, 0, -1, 1};
        int[] dy = {-1, 1, 0, 0};

        for (int i = 0; i < 4; i++) {
            int neighbourX = point.x + dx[i];
            int neighbourY = point.y + dy[i];

            // check bounds
            if (neighbourX < 0 || neighbourX >= image.getWidth() || neighbourY < 0 || neighbourY >= image.getHeight()) {
                continue;
            }

            int neighbourValue = GlobalHelperFunctions.calculateGrayValueFromRGB(image.getRGB(neighbourX, neighbourY));

            if (neighbourValue == 0 && !visited[neighbourX][neighbourY]) {
                fillInterior(image, visited, new Point(neighbourX, neighbourY));
            }
        }
        return image;
    }

    /**
     * Sorts a binary image into regions.
     * Checks for the biggest region and produces a BufferedImage with that region highlighted.
     * Calls {@link #findAllConnectedNeighbours(BufferedImage, int[][], int, Point)} for region growing and
     * {@link #fillInterior(BufferedImage, boolean[][], Point)} for filling the resulting biggest region.
     * Introduces a black border beforehand to mitigate false positives.
     * @param image BufferedImage to look for sign geometry in
     * @return BufferedImage with sign geometry highlighted
     */
    private static BufferedImage lookForSignGeometry(BufferedImage image) {

        //make edge black
        int width = image.getWidth();
        int height = image.getHeight();
        int border = 5;

        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                if (x < border || x >= width - border || y < border || y >= height - border) {
                    image.setRGB(x, y, 0xFF000000);
                }
            }
        }

        // sort into regions
        int[][] regions = new int[width][height];
        int currentRegion = 1;

        for (int x = 0; x < width; x++){
            for (int y = 0; y < height; y++){
                int value = GlobalHelperFunctions.calculateGrayValueFromRGB(image.getRGB(x, y));

                if (value == 255 && regions[x][y] == 0){
                    // new region
                    Pipeline.findAllConnectedNeighbours(image, regions, currentRegion, new Point(x, y));
                    currentRegion++;
                }
            }
        }

        // count region sizes
        int[] regionSizes = new int[currentRegion];

        for (int x = 0; x < width; x++){
            for (int y = 0; y < height; y++){
                int region = regions[x][y];
                if (region > 0){
                    regionSizes[region]++;
                }
            }
        }

        // determine largest region
        int maxPixelAmount = 0;
        int largestRegion = 0;

        for (int i = 1; i < regionSizes.length; i++) {
            if (regionSizes[i] > maxPixelAmount) {
                maxPixelAmount = regionSizes[i];
                largestRegion = i;
            }
        }

        // new image with only biggest region
        BufferedImage regionImage = new BufferedImage(width, height, BufferedImage.TYPE_BYTE_GRAY);

        for (int x = 0; x < width; x++) {
            for (int y = 0; y < height; y++) {

                if (regions[x][y] == largestRegion) {
                    regionImage.setRGB(x, y, 0xFFFFFFFF);
                } else {
                    regionImage.setRGB(x, y, 0xFF000000);
                }
            }
        }

        // calculate mid-point of region
        Point centerPoint = Pipeline.calculateCenterPointOfSign(regionImage);

        // fill the found region
        boolean[][] visited = new boolean[width][height];

        return Pipeline.fillInterior(regionImage, visited, centerPoint);
    }

    /**
     * Checks a given BufferedImage for outermost white pixels in each cardinal direction and returns their coordinates.
     * @param image BufferedImage to find coords of sign in
     * @return int[] with xMin, xMax, yMin, yMax
     */
    private static int[] findCoordsOfSign(BufferedImage image){
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

    /**
     * Crops a given BufferedImage using a BufferedImage as a mask.
     * Copies only pixels that are 255 white in the mask.
     * @param image BufferedImage to crop
     * @param mask BufferedImage to use as mask
     * @return BufferedImage cropped with mask
     */
    public static BufferedImage cropAndMaskSign(BufferedImage image, BufferedImage mask) {
        int[] coordsOfSign = Pipeline.findCoordsOfSign(mask);
        int xMin = coordsOfSign[0];
        int xMax = coordsOfSign[1];
        int yMin = coordsOfSign[2];
        int yMax = coordsOfSign[3];

        // size of mask
        int cropWidth = xMax - xMin + 1;
        int cropHeight = yMax - yMin + 1;

        BufferedImage croppedSign = new BufferedImage(cropWidth, cropHeight, image.getType());

        // copy pixels that are white in mask
        for (int y = 0; y < cropHeight; y++) {
            for (int x = 0; x < cropWidth; x++) {

                int origX = xMin + x;
                int origY = yMin + y;

                int maskValue = GlobalHelperFunctions.calculateGrayValueFromRGB(mask.getRGB(origX, origY));

                if (maskValue == 255) {
                    int rgb = image.getRGB(origX, origY);
                    croppedSign.setRGB(x, y, rgb);
                } else {
                    croppedSign.setRGB(x, y, 0xFF000000);
                }
            }
        }

        return croppedSign;
    }
}
