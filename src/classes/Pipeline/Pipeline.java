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
        BufferedImage originalPPM = ImageIO.readImage(filename);
        if (originalPPM == null) {
            System.err.println("Error reading image " + filename);
            return;
        }
        BufferedImage scaledImage = null;
        long end = System.nanoTime();
        System.out.println("<<< Image read and converted in " + (end - start) / 1000000 + " milliseconds.");

        start = System.nanoTime();
        // check size and scale image
        if (originalPPM.getHeight() >= 2000 || originalPPM.getWidth() >= 2000){
            System.out.println("scaling image...");
            scaledImage = PipelineHelper.scaleImage(originalPPM, 0.25);
            if (scaledImage == null) return;

        }
        end = System.nanoTime();
        System.out.println("<<< Image scaled in " + (end - start) / 1000000 + " milliseconds.");

        System.out.println("Successfully loaded image " + filename);
        ImageIO.displayImage(scaledImage == null ? originalPPM : scaledImage);

        start = System.nanoTime();
        // 2. preprocess image
        BufferedImage preProcessedImage = Pipeline.imagePreprocessing(scaledImage == null ? originalPPM : scaledImage);
        end = System.nanoTime();
        System.out.println("<<< Image preprocessed in " + (end - start) / 1000000 + " milliseconds.");

        start = System.nanoTime();
        // 3. perform checks
        //TODO: similar lines in angle HAVE to be grouped. too many possible forms result otherwise
        // TODO: minimum size of form before checking for colors to get rid of false positives
        Pipeline.checkForSign(preProcessedImage, scaledImage == null ? originalPPM : scaledImage);
        end = System.nanoTime();
        System.out.println("<<< Image checked in " + (end - start) / 1000000 + " milliseconds.");

        System.out.println("Calculations ended.");
        long progEnd = System.nanoTime();
        System.out.println("Total time: " + (progEnd - progStart) / 1000000000 + " seconds");
    }

    /**
     * Calls {@link EdgeDetection#houghTransformation(BufferedImage)} to determine Hough room.
     * Calls {@link PipelineHelper#isLocalMaximum(int[][], int, int, int)} to determine local maximum of possible line.
     * Sorts found lines and checks for intersection angles.
     * Calls {@link FormChecker#checkTriangleForm(BufferedImage, ArrayList)} ,
     * {@link FormChecker#checkRectangleForm(BufferedImage, ArrayList)} ,
     * {@link FormChecker#checkOctagonForm(BufferedImage, ArrayList)}
     * @param preProcessedImage BufferedImage preprocessed
     * @param originalImage BufferedImage original input
     */
    private static void checkForSign(BufferedImage preProcessedImage, BufferedImage originalImage){
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
        int threshold = 50; //TODO: abhängig von bild größe

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

        // sort lines and keep best
        noSimilarLines.sort((line1, line2) -> Integer.compare(line2.votes, line1.votes));
        int amountToKeep = 30; //TODO: abhängigkeitskriterium für mindestanzahl
        ArrayList<HoughLine> bestLines = new ArrayList<>();
        int limit = Math.min(amountToKeep, noSimilarLines.size());
        for (int i = 0; i < limit; i++) {
            bestLines.add(noSimilarLines.get(i));
        }

        FormChecker.checkTriangleForm(originalImage, bestLines);

        FormChecker.checkRectangleForm(originalImage, bestLines);

        FormChecker.checkOctagonForm(originalImage, bestLines);
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
        ImageIO.displayImage(lowpass);

        // hist equal
        //BufferedImage histogramEqualization = ImageManipulation.histogramEqualization(lowpass);
        //ImageIO.displayImage(histogramEqualization);

        // 2. sobel
        BufferedImage sobel = EdgeDetection.sobelFilter(lowpass, 3);
        ImageIO.displayImage(sobel);

        // 3. equidensity
        BufferedImage equidensity = ImageManipulation.equidensityFirstOrderGrayImageCustomBounds(sobel, 50, 200, 255, 0, 0);
        ImageIO.displayImage(equidensity);

        // 4. closing with dilation and erosion
        boolean[][] mask = {{false, true, false}, {true, true, true}, {false, true, false}};
        BufferedImage erosion = MorphologicalOperations.erosion(equidensity, mask, 1);
        ImageIO.displayImage(erosion);

        BufferedImage dilation = MorphologicalOperations.dilation(erosion, mask, 1);
        ImageIO.displayImage(dilation);

        // 5. negative
        BufferedImage negative = ColorManipulation.negative(equidensity);
        ImageIO.displayImage(negative);

        return negative;
    }

}
