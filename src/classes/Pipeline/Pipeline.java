package classes.Pipeline;

import classes.*;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.util.*;

public class Pipeline {

    public static void findSign(String filename){
        long progStart = System.nanoTime();
        long start = progStart;
        // 1. read image
        BufferedImage originalImage = ImageIO.readImage(filename);
        if (originalImage == null) {
            System.err.println("Error reading image " + filename);
            return;
        }
        BufferedImage scaledImage = null;
        long end = System.nanoTime();
        System.out.println("<<< Image read and converted in " + (end - start) / 1000000 + " ms.");

        // construct pyramid
        start = System.nanoTime();
        BufferedImage level1 = originalImage;
        BufferedImage level2 = PipelineHelper.scaleColorImageBoxDownsampling(level1, 0.5);
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
        pyramid.add(level1);
        end = System.nanoTime();
        System.out.println("<<< Pyramid constructed in " + (end - start) / 1000000 + " ms");

        // traverse pyramid
        int pyramidNum = pyramid.size();
        for (int i = 0; i < pyramid.size(); i++){
            BufferedImage image = pyramid.get(i);
            BufferedImage copy = ImageIO.copyBufferedImage(image);
            long levelStart = System.nanoTime();

            // moving window
            // TODO: check edges (run once from different edge)
            int width = image.getWidth();
            int height = image.getHeight();
            int windowSize = Math.min(200, Math.min(originalImage.getWidth(), originalImage.getHeight()));
            int stepSize = 25;

            if (image.getHeight() < windowSize || image.getWidth() < windowSize) continue;

            System.out.println("--------------------------------------");
            System.out.println("\n\nChecking pyramid level " + pyramidNum + "...");
            ImageIO.displayImage(image);

            boolean endOfLevel = false;

            // check image as a whole
            System.out.println("check whole image...");
            // 2. preprocess image
            BufferedImage preProcessedImage = Pipeline.imagePreprocessing(image);

            // 3. perform checks
            boolean signFound = Pipeline.checkForSign(preProcessedImage, image, copy, 0, 0);

            if (signFound) {
                System.out.println("breaking...");
                ImageIO.displayImage(copy);
                break;
            }

            for (int y = 0; y <= height - windowSize; y += stepSize){
                for (int x = 0; x <= width - windowSize; x += stepSize) {

                    // paint window
                    BufferedImage windowImage = DrawingAndFillingPipeline.drawWindow(image, x, y, windowSize);
                    ImageIO.displayImage(windowImage);

                    // create mask of window
                    BufferedImage windowMask = new BufferedImage(width, height, BufferedImage.TYPE_BYTE_BINARY);
                    Graphics2D g = windowMask.createGraphics();
                    ArrayList<Point> windowPoints = new ArrayList<>();
                    windowPoints.add(new Point(x, y));
                    windowPoints.add(new Point(x + windowSize, y));
                    windowPoints.add(new Point(x + windowSize, y + windowSize));
                    windowPoints.add(new Point(x, y +windowSize));
                    DrawingAndFillingPipeline.drawEdgesAndFill(g, windowPoints);

                    // crop and mask sign from original image
                    BufferedImage maskedWindow = PipelineHelper.cropAndMaskSign(image, windowMask);
                    if (maskedWindow == null) continue;

                    // 2. preprocess image
                    start = System.nanoTime();
                    preProcessedImage = Pipeline.imagePreprocessing(maskedWindow);
                    //ImageIO.displayImage(preProcessedImage);
                    end = System.nanoTime();
                    //System.out.println("<<< Image preprocessed in " + (end - start) / 1000000 + " ms.");

                    // 3. perform checks
                    start = System.nanoTime();
                    signFound = Pipeline.checkForSign(preProcessedImage, maskedWindow, copy, x, y) || signFound;
                    end = System.nanoTime();
                    System.out.println("\n<<< Window checked in " + (end - start) / 1000000 + " ms.");

                    endOfLevel = (y+1 + stepSize > height - windowSize) && (x+1 + stepSize > width - windowSize);

                    // TODO: determine best sign of all found signs in level
                    // TODO: if multiple signs of same type get detected at the same global location -> sign found with enough certainty -> break
                    if (signFound && endOfLevel) {
                        System.out.println("Sign found, breaking...");
                        break;
                    }
                }
                if (signFound && endOfLevel) break;
            }

            long levelEnd = System.nanoTime();
            System.out.println("<<< level " + pyramidNum + " checked in " + (levelEnd - levelStart) / 1000000 + " ms.");
            if (signFound) {
                ImageIO.displayImage(copy);
                break;
            }
            pyramidNum--;
            if (i == 5){
                BufferedImage level0 = PipelineHelper.upscaleColorImageNearestNeighbour(originalImage, 2);
                pyramid.add(level0);
            }
        }

        System.out.println("\nCalculations ended.");
        long progEnd = System.nanoTime();
        System.out.println("Total time: " + (progEnd - progStart) / 1000000 + " ms.");
        System.out.println("----------------------------------------------\n\n");

    }

    /**
     * Calls {@link EdgeDetection#houghTransformation(BufferedImage)} to determine Hough room.
     * Calls {@link PipelineHelper#isLocalMaximum(int[][], int, int, int)} to determine local maximum of possible line.
     * Sorts found lines and checks for intersection angles.
     * Calls {@link FormChecker#checkTriangleForm(BufferedImage, ArrayList, BufferedImage, int, int)} ,
     * {@link FormChecker#checkRectangleForm(BufferedImage, ArrayList, BufferedImage, int, int)} ,
     * {@link FormChecker#checkOctagonForm(BufferedImage, ArrayList, BufferedImage, int, int)}
     * @param preProcessedImage BufferedImage preprocessed
     * @param maskedWindow BufferedImage original input
     * @return boolean if sign found
     */
    private static boolean checkForSign(BufferedImage preProcessedImage, BufferedImage maskedWindow, BufferedImage originalImage, int windowX, int windowY){
        //make edge black
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

        ArrayList<HoughLine> lines = new ArrayList<>();
        int[][] accumulator = EdgeDetection.houghTransformation(preProcessedImage);
        int threshold = 30; //TODO: abhängig von bild größe

        // accumulate lines
        for (int phi = 0; phi < accumulator.length; phi++){
            for (int r = 0; r < accumulator[phi].length; r++){
                int votes = accumulator[phi][r];
                if (votes > threshold){
                    int minLength = 20;//TODO: abhängig von bild größe
                    int maxAllowedGap = 5;//TODO: abhängig von bild größe
                    int sizeOfNeighbourhood = 3;//TODO: abhängig von bild größe
                    if (PipelineHelper.isLocalMaximum(accumulator, phi, r, sizeOfNeighbourhood) && PipelineHelper.isLineSolid(preProcessedImage, new HoughLine(phi, r, votes), minLength, maxAllowedGap)){
                        lines.add(new HoughLine(phi, r, votes));
                    }
                }
            }
        }

        BufferedImage lineImage = new BufferedImage(maskedWindow.getWidth(), maskedWindow.getHeight(), maskedWindow.getType());
        Graphics2D g2 = lineImage.createGraphics();
        g2.setColor(Color.BLUE);
        g2.setStroke(new java.awt.BasicStroke(1));
        DrawingAndFillingPipeline.drawLines(g2, lines, maskedWindow.getWidth(), maskedWindow.getHeight());
        //ImageIO.displayImage(lineImage);

        // sort
        lines.sort((line1, line2) -> Integer.compare(line2.votes, line1.votes));

        // remove similar lines
        ArrayList<HoughLine> noSimilarLines = new ArrayList<>();
        int angleTolerance = 5; // TODO: abhängig von bildgröße
        int radiusTolerance = 10; // TODO: abhängig von bildgröße

        for (HoughLine newLine : lines){
            boolean isSimilar = false;

            for (HoughLine acceptedLine : noSimilarLines){
                int dr = Math.abs(newLine.r - acceptedLine.r);
                int dPhi = Math.abs(newLine.phi - acceptedLine.phi);
                if (dPhi > 90) {
                    dPhi = 180 - dPhi;
                }

                if (dPhi <= angleTolerance && dr <= radiusTolerance){
                    isSimilar = true;
                    break;
                }
            }

            if (!isSimilar){
                noSimilarLines.add(newLine);
            }
        }

        BufferedImage noSomilarImage = new BufferedImage(maskedWindow.getWidth(), maskedWindow.getHeight(), maskedWindow.getType());
        Graphics2D gs = noSomilarImage.createGraphics();
        gs.setColor(Color.BLUE);
        gs.setStroke(new java.awt.BasicStroke(1));
        DrawingAndFillingPipeline.drawLines(gs, noSimilarLines, maskedWindow.getWidth(), maskedWindow.getHeight());
        //ImageIO.displayImage(noSomilarImage);

        // sort lines and keep best
        noSimilarLines.sort((line1, line2) -> Integer.compare(line2.votes, line1.votes));
        int amountToKeep = 25; //TODO: abhängigkeitskriterium für mindestanzahl
        ArrayList<HoughLine> bestLines = new ArrayList<>();
        int limit = Math.min(amountToKeep, noSimilarLines.size());
        for (int i = 0; i < limit; i++) {
            bestLines.add(noSimilarLines.get(i));
        }

        BufferedImage bestLinesImage = new BufferedImage(maskedWindow.getWidth(), maskedWindow.getHeight(), maskedWindow.getType());
        Graphics2D g3 = bestLinesImage.createGraphics();
        g3.setColor(Color.BLUE);
        g3.setStroke(new java.awt.BasicStroke(1));
        DrawingAndFillingPipeline.drawLines(g3, bestLines, maskedWindow.getWidth(), maskedWindow.getHeight());
        //ImageIO.displayImage(bestLinesImage);

//        boolean TsignFound = FormChecker.checkTriangleForm(maskedWindow, bestLines, originalImage, windowX, windowY);
//        boolean RsignFound = false;
//        boolean OsignFound = false;
//
//        if (!TsignFound) RsignFound = FormChecker.checkRectangleForm(maskedWindow, bestLines, originalImage, windowX, windowY);
//
//        //TODO: octagon check is very slow (40% of total runtime)
//        if (!TsignFound && !RsignFound) OsignFound = FormChecker.checkOctagonForm(maskedWindow, bestLines);
//
//        return TsignFound || RsignFound || OsignFound;

        boolean signFound = false;
        signFound = FormChecker.checkTriangleForm(maskedWindow, bestLines, originalImage, windowX, windowY) || signFound;
        signFound = FormChecker.checkRectangleForm(maskedWindow, bestLines, originalImage, windowX, windowY) || signFound;
        signFound = FormChecker.checkOctagonForm(maskedWindow, bestLines, originalImage, windowX, windowY) || signFound;

        return signFound;
    }

    /**
     * Performs preprocessing on given BufferedImage:
     * Calls {@link EdgeDetection#gaussianLowPass(BufferedImage, int)}
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
        BufferedImage lowpass = EdgeDetection.gaussianLowPassSeperated(image, 5);
        //ImageIO.displayImage(lowpass);

        // hist equal
        BufferedImage histOrNot = lowpass;
        DescriptiveStatistics stats = new DescriptiveStatistics(lowpass);
        stats.calculateEntropy();
        if (stats.getEntropy() > 5.5) {
            histOrNot = ImageManipulation.histogramEqualization(lowpass);
        }
        //ImageIO.displayImage(histOrNot);

        // 2. sobel
        BufferedImage sobel = EdgeDetection.sobelFilter(histOrNot, 3);
        //ImageIO.displayImage(sobel);

        // 3. equidensity
        BufferedImage equidensity = ImageManipulation.equidensityFirstOrderGrayImageCustomBounds(sobel, 50, 200, 255, 0, 0);
        //ImageIO.displayImage(equidensity);

        // 4. closing with dilation and erosion
        boolean[][] mask = {{false, true, false}, {true, true, true}, {false, true, false}};
        BufferedImage erosion = MorphologicalOperations.erosion(equidensity, mask, 1);
        //ImageIO.displayImage(erosion);

        BufferedImage dilation = MorphologicalOperations.dilation(erosion, mask, 1);
        //ImageIO.displayImage(dilation);

        // 5. negative
        BufferedImage negative = ColorManipulation.negative(dilation);
        //ImageIO.displayImage(negative);

        return negative;
    }

}
