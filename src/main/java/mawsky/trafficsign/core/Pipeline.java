package main.java.mawsky.trafficsign.core;

import main.java.mawsky.trafficsign.utils.PipelineHelper;
import main.java.mawsky.trafficsign.io.ImageIO;
import main.java.mawsky.trafficsign.detection.FormChecker;
import main.java.mawsky.trafficsign.processing.ColorManipulation;
import main.java.mawsky.trafficsign.processing.MorphologicalOperations;
import main.java.mawsky.trafficsign.processing.EdgeDetection;
import main.java.mawsky.trafficsign.processing.ImageManipulation;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class Pipeline {

    private static final ExecutorService THREAD_POOL = Executors.newFixedThreadPool(3);

    public static void shutdown(){
        THREAD_POOL.shutdown();
    }

    /**
     * Entry point for finding sign in image. Takes the filename and loads it.
     * Constructs an image pyramid with scaled images. {@link PipelineHelper#scaleColorImageBoxDownsampling(BufferedImage, double)}
     * If no sign is found inside pyramid, upscales the image to 2x using {@link PipelineHelper#upscaleColorImageNearestNeighbour(BufferedImage, double)}
     * Traverses the pyramid top to bottom. For each level:
     * Checks the whole level for a sign.
     * Traverses the image with a moving window and checks for a sign.
     * Constructs a mask and cuts the image to the window. {@link PipelineHelper#cropAndMaskSign(BufferedImage, BufferedImage)}
     * Uses  {@link Pipeline#imagePreprocessing(BufferedImage)} and {@link Pipeline#checkForSign(BufferedImage, BufferedImage, BufferedImage, int, int)}
     * to check for signs
     * @param filename name of file
     */
    public static void findSign(String filename) {

        // 1. read image
        BufferedImage originalImage = ImageIO.readImage(filename);
        if (originalImage == null) {
            System.err.println("Error reading image " + filename);
            return;
        }

        // 2. construct pyramid
        BufferedImage level2 = PipelineHelper.scaleColorImageBoxDownsampling(originalImage, 0.5);
        BufferedImage level3 = PipelineHelper.scaleColorImageBoxDownsampling(level2, 0.5);
        BufferedImage level4 = PipelineHelper.scaleColorImageBoxDownsampling(level3, 0.5);
        BufferedImage level5 = PipelineHelper.scaleColorImageBoxDownsampling(level4, 0.5);
        BufferedImage level6 = PipelineHelper.scaleColorImageBoxDownsampling(level5, 0.5);
        ArrayList<BufferedImage> pyramid = new ArrayList<>();
        pyramid.add(level6);
        pyramid.add(level5);
        pyramid.add(level4);
        pyramid.add(level3);
        pyramid.add(level2);
        pyramid.add(originalImage);

        // 3. traverse pyramid
        for (int i = 0; i < pyramid.size(); i++) {
            BufferedImage image = pyramid.get(i);
            BufferedImage copy = ImageIO.copyBufferedImage(image);

            // moving window
            int width = image.getWidth();
            int height = image.getHeight();
            int windowSize = Math.min(200, Math.min(originalImage.getWidth(), originalImage.getHeight()));
            int stepSize = 25;

            if (image.getHeight() < windowSize || image.getWidth() < windowSize) continue;

            // 4. check image as a whole
            BufferedImage preProcessedImage = Pipeline.imagePreprocessing(image);
            if (preProcessedImage == null) return;
            boolean signFound = Pipeline.checkForSign(preProcessedImage, image, copy, 0, 0);

            if (signFound) {
                ImageIO.displayImage(copy);
                break;
            }

            // 5. moving window
            for (int y = 0; y <= height - windowSize; y += stepSize){
                for (int x = 0; x <= width - windowSize; x += stepSize) {

                    // create mask of window
                    BufferedImage windowMask = new BufferedImage(width, height, BufferedImage.TYPE_BYTE_BINARY);
                    Graphics2D g = windowMask.createGraphics();
                    ArrayList<Point> windowPoints = new ArrayList<>();
                    windowPoints.add(new Point(x, y));
                    windowPoints.add(new Point(x + windowSize, y));
                    windowPoints.add(new Point(x + windowSize, y + windowSize));
                    windowPoints.add(new Point(x, y +windowSize));
                    PipelineHelper.drawEdgesAndFill(g, windowPoints);

                    // crop and mask sign from original image
                    BufferedImage maskedWindow = PipelineHelper.cropAndMaskSign(image, windowMask);
                    if (maskedWindow == null) continue;

                    // preprocess image
                    preProcessedImage = Pipeline.imagePreprocessing(maskedWindow);
                    if (preProcessedImage == null) return;

                    // perform checks
                    signFound = Pipeline.checkForSign(preProcessedImage, maskedWindow, copy, x, y);

                    if (signFound) break;
                }
                if (signFound) break;
            }

            if (signFound) {
                ImageIO.displayImage(copy);
                break;
            }

            // 6. upscale image
            if (i == 5){
                BufferedImage level0 = PipelineHelper.upscaleColorImageNearestNeighbour(originalImage, 2);
                pyramid.add(level0);
            }
        }
    }

    /**
     * Calls {@link EdgeDetection#houghTransformation(BufferedImage)} to determine Hough room.
     * Accumulates lines, uses {@link PipelineHelper#isLocalMaximum(int[][], int, int, int)} to determine local maximum of possible line,
     * {@link PipelineHelper#isLineSolid(BufferedImage, HoughLine, int, int)} to determine solidness of possible line.
     * Sorts found lines, merges similar lines and cuts the Array to the top 25 lines by votes.
     * Calls {@link FormChecker#checkForm(BufferedImage, ArrayList, BufferedImage, int, int, int)} as threads for parallel form checks of rectangle, triangle and octagon.
     * @param preProcessedImage BufferedImage preprocessed
     * @param maskedWindow BufferedImage original input
     * @return boolean if sign found
     */
    private static boolean checkForSign(BufferedImage preProcessedImage, BufferedImage maskedWindow, BufferedImage originalImage, int windowX, int windowY){

        // 1. make edge black
        int width = preProcessedImage.getWidth();
        int height = preProcessedImage.getHeight();
        int border = 5;

        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                if (x < border || x >= width - border || y < border || y >= height - border) {
                    preProcessedImage.setRGB(x, y, 0xFF000000);
                }
            }
        }

        // 2. accumulate lines
        ArrayList<HoughLine> lines = new ArrayList<>();
        int[][] accumulator = EdgeDetection.houghTransformation(preProcessedImage);
        int threshold = 30;

        for (int phi = 0; phi < accumulator.length; phi++){
            for (int r = 0; r < accumulator[phi].length; r++){
                int votes = accumulator[phi][r];
                if (votes > threshold){
                    int minLength = 20;
                    int maxAllowedGap = 5;
                    int sizeOfNeighbourhood = 3;
                    if (PipelineHelper.isLocalMaximum(accumulator, phi, r, sizeOfNeighbourhood) && PipelineHelper.isLineSolid(preProcessedImage, new HoughLine(phi, r, votes), minLength, maxAllowedGap)){
                        lines.add(new HoughLine(phi, r, votes));
                    }
                }
            }
        }

        // 3. sort accumulated lines
        lines.sort((line1, line2) -> Integer.compare(line2.votes(), line1.votes()));

        // 4. remove similar lines
        ArrayList<HoughLine> noSimilarLines = new ArrayList<>();
        int angleTolerance = 5;
        int radiusTolerance = 10;
        int diagonal = (int) Math.ceil(Math.sqrt((height * height) + (width * width)));

        for (HoughLine newLine : lines){
            boolean isSimilar = false;

            for (HoughLine acceptedLine : noSimilarLines){
                int dPhi = PipelineHelper.getAngleOfIntersection(newLine, acceptedLine);

                if (dPhi <= angleTolerance){
                    double posNew = newLine.r() - diagonal;
                    double posAcc = acceptedLine.r() - diagonal;

                    if (Math.abs(posNew - posAcc) <= radiusTolerance){
                        isSimilar = true;
                        break;
                    }
                }
            }

            if (!isSimilar){
                noSimilarLines.add(newLine);
            }
        }

        // 5. sort lines and keep best 25
        noSimilarLines.sort((line1, line2) -> Integer.compare(line2.votes(), line1.votes()));

        ArrayList<HoughLine> bestLines = new ArrayList<>();
        int amountToKeep = 25;
        int limit = Math.min(amountToKeep, noSimilarLines.size());

        for (int i = 0; i < limit; i++) {
            bestLines.add(noSimilarLines.get(i));
        }

        // 6. threads for parallel form checks

        //rectangle
        CompletableFuture<Boolean> rectangleThread = CompletableFuture.supplyAsync(() -> FormChecker.checkForm(maskedWindow, bestLines, originalImage, windowX, windowY, 0), THREAD_POOL);

        // triangle
        CompletableFuture<Boolean> triangleThread = CompletableFuture.supplyAsync(() -> FormChecker.checkForm(maskedWindow, bestLines, originalImage, windowX, windowY, 1), THREAD_POOL);

        //octagon
        CompletableFuture<Boolean> octagonThread = CompletableFuture.supplyAsync(() -> FormChecker.checkForm(maskedWindow, bestLines, originalImage, windowX, windowY, 2), THREAD_POOL);

        CompletableFuture.allOf(triangleThread, rectangleThread, octagonThread).join();

        boolean triangleFound = triangleThread.join();
        boolean rectangleFound = rectangleThread.join();
        boolean octagonFound = octagonThread.join();

        boolean signFound = triangleFound || rectangleFound || octagonFound;
        if (signFound) System.out.println(">>>>>>>>>>>>>>>>>>>>> valid sign found!");

        return signFound;
    }

    /**
     * Performs preprocessing on given BufferedImage:
     * Calls {@link EdgeDetection#gaussianLowPassSeparated(BufferedImage, int)}
     * Calls {@link ImageManipulation#histogramEqualization(BufferedImage)} if entropy is above 5.5
     * Calls {@link EdgeDetection#sobelFilter(BufferedImage, int)}
     * Calls {@link ImageManipulation#equidensityFirstOrderGrayImageCustomBounds(BufferedImage, int, int, int, int, int)}
     * Calls {@link MorphologicalOperations#erosion(BufferedImage, boolean[][], int)}
     * Calls {@link MorphologicalOperations#dilation(BufferedImage, boolean[][], int)}
     * Calls {@link ColorManipulation#negative(BufferedImage)}
     * @param image BufferedImage to preprocess
     * @return preprocessed BufferedImage
     */
    private static BufferedImage imagePreprocessing(BufferedImage image){

        // 1. lowpass
        BufferedImage lowpass = EdgeDetection.gaussianLowPassSeparated(image, 5);
        if (lowpass == null) return null;

        // hist equal
        BufferedImage histOrNot = lowpass;
        DescriptiveStatistics stats = new DescriptiveStatistics(lowpass);
        stats.calculateEntropy();
        if (stats.getEntropy() > 5.5) {
            histOrNot = ImageManipulation.histogramEqualization(lowpass);
        }

        // 2. sobel
        BufferedImage sobel = EdgeDetection.sobelFilter(histOrNot, 3);

        // 3. equidensity
        BufferedImage equidensity = ImageManipulation.equidensityFirstOrderGrayImageCustomBounds(sobel, 50, 200, 255, 0, 0);

        // 4. closing with dilation and erosion
        boolean[][] mask = {{false, true, false}, {true, true, true}, {false, true, false}};
        BufferedImage erosion = MorphologicalOperations.erosion(equidensity, mask, 1);

        BufferedImage dilation = MorphologicalOperations.dilation(erosion, mask, 1);

        // 5. negative
        return ColorManipulation.negative(dilation);
    }

}
