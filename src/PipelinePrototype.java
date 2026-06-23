import java.awt.*;
import java.awt.image.BufferedImage;
import java.util.*;

public class PipelinePrototype {

    public static void vorfahrt(){
        // 1. read image
        String filenamePPMImage = "vorfahrt.ppm";
        BufferedImage input = ImageIO.readImageAndConvertToPPM("pics/sample/A.jpg", filenamePPMImage);
        System.out.println("Successfully loaded image " + filenamePPMImage);
        ImageIO.displayImage(input);

        // 2. preprocess image
        BufferedImage preProcessedImage = PipelinePrototype.imagePreprocessing(input);

        // hough path
        BufferedImage lines = PipelinePrototype.lookForStraightLinesAndDraw(preProcessedImage);
        ImageIO.displayImage(lines);
        BufferedImage filled = PipelinePrototype.findSignAndFill(lines);
        ImageIO.displayImage(filled);

        // 3. look for sign-geometry
        //BufferedImage signGeometry = PipelinePrototype.lookForSignGeometry(lines);
        //ImageIO.displayImage(signGeometry);

        // 4. check signs geometry
        boolean[] isSquare = {false};
        BufferedImage rotatedSignGeometry = PipelinePrototype.rotateImageAndCheckGeometry(filled, 0.1, isSquare);
        ImageIO.displayImage(rotatedSignGeometry);
        if (isSquare[0]){
            //System.out.println("Quadrat erkannt!");
        }

        // 5. check sign statistics

        // rotate original
        Point centerPoint = PipelinePrototype.calculateCenterPointOfSign(filled);
        BufferedImage rotatedOriginal = RotatedImage.rotateImageBackwardMapping(input, centerPoint, 45);
        ImageIO.displayImage(rotatedOriginal);

        // crop original
        BufferedImage croppedRotatedOriginal = PipelinePrototype.cropAndMaskSign(rotatedOriginal, rotatedSignGeometry);
        ImageIO.displayImage(croppedRotatedOriginal);

        //stats
        DescriptiveStatistics stats = new DescriptiveStatistics(croppedRotatedOriginal);
        stats.calculateAllStatistics();

        // scoring
        // TODO: rework scoring to make it more general
        // TODO: check relativeCumulativeFrequency for two peaks with valley between, cumulate pixels in yellow and white in HSV values

    }

    /**
     * Calls {@link PipelinePrototype#findCoordsOfSign(BufferedImage)} to find the outermost corner coordinates.
     * Calls {@link PipelinePrototype#fillInterior(BufferedImage, boolean[][], Point)} to fill the shape.
     * Must only be called with an image after line detection with hough ({@link PipelinePrototype#lookForStraightLinesAndDraw(BufferedImage)}).
     * @param image BufferedImage with lines forming a closed shape
     * @return BufferedImage with filled shape
     */
    private static BufferedImage findSignAndFill(BufferedImage image) {
        Point centerPoint = PipelinePrototype.calculateCenterPointOfSign(image);
        boolean[][] visited = new boolean[image.getWidth()][image.getHeight()];
        return PipelinePrototype.fillInterior(image, visited, centerPoint);
    }

    /**
     * Looks in each direction in the (phi, r) Hough room to determine local maximum
     * @param accumulator Hough room matrix
     * @param phi angle
     * @param r distance
     * @return boolean if local maximum
     */
    private static boolean isLocalMaximum(int[][] accumulator, int phi, int r){
        for (int dPhi = -1; dPhi <= 1; dPhi++){
            for (int dR = -1; dR <= 1; dR++){
                if (dPhi == 0 && dR == 0) continue;

                int targetPhi = phi + dPhi;
                int targetR = r + dR;

                if (targetPhi < 0){
                    targetPhi = accumulator.length - 1;
                } else if (targetPhi >= accumulator.length) {
                    targetPhi = 0;
                }

                if (targetR < 0 || targetR >= accumulator[targetPhi].length){
                    continue;
                }

                if (accumulator[targetPhi][targetR] > accumulator[phi][r]){
                    return false;
                }
            }
        }
        return true;
    }

    /**
     * Calls {@link PipelinePrototype#houghTransformation(BufferedImage)} to determine Hough room.
     * Calls {@link PipelinePrototype#isLocalMaximum(int[][], int, int)} to determine local maximum of possible line.
     * Sorts found lines and draws the largest lines.
     * @param image BufferedImage
     * @return BufferedImage with the largest lines
     */
    private static BufferedImage lookForStraightLinesAndDraw(BufferedImage image){
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

        // copy for displaying lines
        BufferedImage lineImage = new BufferedImage(image.getWidth(), image.getHeight(), image.getType());
        Graphics2D g = lineImage.createGraphics();
        g.setColor(Color.WHITE);
        g.setStroke(new java.awt.BasicStroke(3));

        ArrayList<HoughLine> lines = new ArrayList<>();
        int[][] accumulator = PipelinePrototype.houghTransformation(image);
        int diagonal = (int) Math.ceil(Math.sqrt(Math.pow(image.getHeight(), 2) + Math.pow(image.getWidth(), 2)));
        int threshold = 50;

        // accumulate lines
        for (int phi = 0; phi < accumulator.length; phi++){
            for (int r = 0; r < accumulator[phi].length; r++){
                int votes = accumulator[phi][r];
                if (votes > threshold){
                    if (isLocalMaximum(accumulator, phi, r)){
                        lines.add(new HoughLine(phi, r, votes));
                    }
                }
            }
        }

        // sort lines
        lines.sort((line1, line2) -> Integer.compare(line2.votes, line1.votes));

        // classification based on angle groups
        //TODO: triangle: why sometimes inner lines instead of outer
        int amountOfDirections = getAmountOfDirections(lines);
        System.out.println(amountOfDirections + " different directions");


        ArrayList<HoughLine> differentAngledLines = new ArrayList<>();
        int tolerance = 15;

        if (amountOfDirections == 3) {

            for (HoughLine line : lines) {
                boolean angleAlreadySelected = false;
                for (HoughLine selected : differentAngledLines) {
                    if (getAngleDiff(line.phi, selected.phi) <= tolerance) {
                        angleAlreadySelected = true;
                        break;
                    }
                }
                if (!angleAlreadySelected) {
                    differentAngledLines.add(line);
                }
                if (differentAngledLines.size() == 3) break;
            }

        } else if (amountOfDirections == 2 || amountOfDirections == 4) {

            for (HoughLine line : lines) {
                int linesWithAngle = 0;

                for (HoughLine selected : differentAngledLines) {
                    if (getAngleDiff(line.phi, selected.phi) <= tolerance) {
                        linesWithAngle++;
                    }
                }

                if (linesWithAngle < 2) {
                    differentAngledLines.add(line);
                }

                if (differentAngledLines.size() == amountOfDirections*2) break;
            }
        } else {
            System.out.println("other");
        }

        // draw lines
        for (HoughLine line : differentAngledLines){
            int r = line.r;
            int phi = line.phi;
            int x1, x2, y1, y2;
            int distance = r- diagonal;
            double radPhi = Math.toRadians(phi);

            if (phi > 45 && phi < 135){
                x1 = 0;
                x2 = image.getWidth();

                y1 = (int) (distance / Math.sin(radPhi));
                y2 = (int) ((distance - x2 * Math.cos(radPhi)) / Math.sin(radPhi));
            }else {
                y1 = 0;
                y2 = image.getHeight();

                x1 = (int) (distance / Math.cos(radPhi));
                x2 = (int) ((distance - y2 * Math.sin(radPhi)) / Math.cos(radPhi));
            }

            g.drawLine(x1, y1, x2, y2);
        }

        g.dispose();

        return lineImage;
    }

    /**
     * Calculates the difference of two angles.
     * Takes into account that 179° and 1° differ by 2°
     * @param angle1 first angle
     * @param angle2 second angle
     * @return difference between angles 1 and 2
     */
    private static int getAngleDiff(int angle1, int angle2) {
        int diff = Math.abs(angle1 - angle2);
        return (diff > 90) ? (180 - diff) : diff;
    }

    /**
     * Calculates amount of directions of lines array.
     * Checks each angle and puts different angles in a list.
     * @param lines array of lines
     * @return Size of list
     */
    private static int getAmountOfDirections(ArrayList<HoughLine> lines) {
        int lineCount = Math.min(8, lines.size());
        ArrayList<Integer> angleGroups = new ArrayList<>();
        int tolerance = 15;

        for (int i = 0; i < lineCount; i++) {
            int currentAngle = lines.get(i).phi;
            boolean isPartOfGroup = false;

            for (int angle : angleGroups) {
                int diff = PipelinePrototype.getAngleDiff(currentAngle, angle);

                if (diff <= tolerance) {
                    isPartOfGroup = true;
                    break;
                }
            }

            if (!isPartOfGroup) {
                angleGroups.add(currentAngle);
            }
        }

        return angleGroups.size();
    }

    /**
     * Performs the Hough transformation on an image.
     * Calls {@link GlobalHelperFunctions#calculateGrayValueFromRGB(int)}
     * @param image BufferedImage to perform on
     * @return int[][] Hough room matrix
     */
    private static int[][] houghTransformation(BufferedImage image){
        int diagonal = (int) Math.ceil(Math.sqrt(Math.pow(image.getHeight(), 2) + Math.pow(image.getWidth(), 2)));
        int[][] accumulator = new int[180][2*diagonal];

        for (int x = 0; x < image.getWidth(); x++){
            for (int y = 0; y < image.getHeight(); y++){
                int grayValue = GlobalHelperFunctions.calculateGrayValueFromRGB(image.getRGB(x, y));
                if (grayValue == 255){
                    for (int phi = 0; phi < 180; phi++){
                        int r = (int) (x * Math.cos(Math.toRadians(phi)) + y * Math.sin(Math.toRadians(phi)));
                        r += diagonal;
                        accumulator[phi][r] += 1;
                    }
                }
            }
        }
        return accumulator;
    }

    /**
     * Calculates the centre of the sign.
     * Calls {@link PipelinePrototype#findCoordsOfSign(BufferedImage)}
     * @param image BufferedImage to calculate centre of
     * @return Point with centre coordinates
     */
    private static Point calculateCenterPointOfSign(BufferedImage image) {
        int[] coordsOfSign = PipelinePrototype.findCoordsOfSign(image);
        int centerX = (coordsOfSign[0] + coordsOfSign[1]) / 2;
        int centerY = (coordsOfSign[2] + coordsOfSign[3]) / 2;

        return new Point(centerX, centerY);
    }

    /**
     * Rotates a given BufferedImage by 45 degrees around the centerpoint of the sign.
     * Calls {@link PipelinePrototype#findCoordsOfSign(BufferedImage)} to get coordinates for the centre point
     * Calls {@link RotatedImage#rotateImageBackwardMapping(BufferedImage, Point, int)}
     * Checks for a square shape with tolerance.
     * @param image BufferedImage to rotate 45 degrees and check
     * @param squareTolerancePercent percent of tolerance for square
     * @param isSquare boolean array to fake call by reference for boolean value
     * @return 45 degrees rotated BufferedImage
     */
    private static BufferedImage rotateImageAndCheckGeometry(BufferedImage image, double squareTolerancePercent, boolean[] isSquare) {
        // rotate image
        int[] coordsOfSign = PipelinePrototype.findCoordsOfSign(image);
        int xMin = coordsOfSign[0];
        int xMax = coordsOfSign[1];
        int yMin = coordsOfSign[2];
        int yMax = coordsOfSign[3];
        int centerX = (xMin + xMax) / 2;
        int centerY = (yMin + yMax) / 2;
        BufferedImage rotatedImage = RotatedImage.rotateImageBackwardMapping(image, new Point(centerX, centerY), 45);

        // count white pixels
        int pixelCount = 0;
        for (int x = 0; x < image.getWidth(); x++) {
            for (int y = 0; y < image.getHeight(); y++) {
                int gray = GlobalHelperFunctions.calculateGrayValueFromRGB(image.getRGB(x, y));
                if (gray == 255) {
                    pixelCount++;
                }
            }
        }
        double density = pixelCount / (double) (image.getWidth() * image.getHeight());

        double distanceX = xMax - xMin;
        double distanceY = yMax - yMin;

        int minAllowedSize = 15;

        if (distanceX < minAllowedSize || distanceY < minAllowedSize) {
            isSquare[0] = false;
        } else if (density < 0.15) {
            isSquare[0] = false;
        } else {
            double ratio = distanceX / distanceY;
            isSquare[0] = (ratio < (1 + squareTolerancePercent) && ratio > (1 - squareTolerancePercent));
        }

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
        //System.out.println("Successfully calculated Gauß lowpass");
        ImageIO.displayImage(lowpass);

        // 3. histogram equalization
        //BufferedImage equalizedHistogram = ImageManipulation.histogramEqualization(lowpass);
        //System.out.println("Successfully calculated histogram equalization");
        //ImageIO.displayImage(equalizedHistogram);

        // sobel
        BufferedImage sobel = Convolution.sobelFilter(lowpass, 3);
        //System.out.println("Successfully calculated Sobel filter");
        ImageIO.displayImage(sobel);

        // 4. equidensity
        BufferedImage equidensity = ImageManipulation.equidensityFirstOrderGrayImageCustomBounds(sobel, 100, 200, 255, 0, 0);
        //System.out.println("Successfully calculated equidensity");
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
     * @param startPoint point to start from
     */
    private static void findAllConnectedNeighbours(BufferedImage image, int[][] regions, int currentRegion, Point startPoint) {
        Queue<Point> queue = new LinkedList<>();

        // start point
        regions[startPoint.x][startPoint.y] = currentRegion;
        queue.add(startPoint);

        while (!queue.isEmpty()) {
            Point currentPoint = queue.poll();

            // check 8 neighbours
            for (int x = -1; x <= 1; x++) {
                for (int y = -1; y <= 1; y++) {
                    if (x == 0 && y == 0) continue;

                    int neighbourX = currentPoint.x + x;
                    int neighbourY = currentPoint.y + y;

                    // check bounds
                    if (neighbourX < 0 || neighbourX >= image.getWidth() || neighbourY < 0 || neighbourY >= image.getHeight()) {
                        continue;
                    }

                    int neighbourValue = GlobalHelperFunctions.calculateGrayValueFromRGB(image.getRGB(neighbourX, neighbourY));

                    if (neighbourValue == 255 && regions[neighbourX][neighbourY] == 0) {
                        regions[neighbourX][neighbourY] = currentRegion;
                        queue.add(new Point(neighbourX, neighbourY));
                    }
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
     * @param startPoint point to start from
     * @return BufferedImage with filled interior
     */
    private static BufferedImage fillInterior(BufferedImage image, boolean[][] visited, Point startPoint) {
        Queue<Point> queue = new LinkedList<>();

        visited[startPoint.x][startPoint.y] = true;
        image.setRGB(startPoint.x, startPoint.y, 0xFFFFFFFF);
        queue.add(startPoint);

        // 4 cardinal directions
        int[] dx = {0, 0, -1, 1};
        int[] dy = {-1, 1, 0, 0};

        while (!queue.isEmpty()) {
            Point currentPoint = queue.poll();

            for (int i = 0; i < 4; i++) {
                int neighbourX = currentPoint.x + dx[i];
                int neighbourY = currentPoint.y + dy[i];

                // check bounds
                if (neighbourX < 0 || neighbourX >= image.getWidth() || neighbourY < 0 || neighbourY >= image.getHeight()) {
                    continue;
                }

                int neighbourValue = GlobalHelperFunctions.calculateGrayValueFromRGB(image.getRGB(neighbourX, neighbourY));

                if (neighbourValue == 0 && !visited[neighbourX][neighbourY]) {
                    visited[neighbourX][neighbourY] = true;
                    image.setRGB(neighbourX, neighbourY, 0xFFFFFFFF);

                    queue.add(new Point(neighbourX, neighbourY));
                }
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

        // failsafe for entirely black images
        int signPixelCount = 0;
        for (int x = 0; x < image.getWidth(); x++) {
            for (int y = 0; y < image.getHeight(); y++) {
                int gray = GlobalHelperFunctions.calculateGrayValueFromRGB(image.getRGB(x, y));
                if (gray == 255) {
                    signPixelCount++;
                }
            }
        }

        if (signPixelCount < 80) {
            return image;
        }

        // sort into regions
        int[][] regions = new int[width][height];
        int currentRegion = 1;

        for (int x = 0; x < width; x++){
            for (int y = 0; y < height; y++){
                int value = GlobalHelperFunctions.calculateGrayValueFromRGB(image.getRGB(x, y));

                if (value == 255 && regions[x][y] == 0){
                    // new region
                    PipelinePrototype.findAllConnectedNeighbours(image, regions, currentRegion, new Point(x, y));
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
        Point centerPoint = PipelinePrototype.calculateCenterPointOfSign(regionImage);

        // fill the found region
        boolean[][] visited = new boolean[width][height];
        BufferedImage filled = PipelinePrototype.fillInterior(regionImage, visited, centerPoint);

        return filled;
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
        int[] coordsOfSign = PipelinePrototype.findCoordsOfSign(mask);
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
