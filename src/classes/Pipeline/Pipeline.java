package classes.Pipeline;

import classes.*;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.IOException;
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
     * Calls {@link FormChecker#checkTriangleForm(BufferedImage, ArrayList, int)} ,
     * {@link FormChecker#checkRectangleForm(BufferedImage, ArrayList, int)} ,
     * {@link FormChecker#checkOctagonForm(BufferedImage, ArrayList, int)}
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

        // copy for displaying lines
        BufferedImage lineImage = new BufferedImage(preProcessedImage.getWidth(), preProcessedImage.getHeight(), preProcessedImage.getType());
        Graphics2D g = lineImage.createGraphics();
        g.setColor(Color.WHITE);
        g.setStroke(new java.awt.BasicStroke(3));

        ArrayList<HoughLine> lines = new ArrayList<>();
        int[][] accumulator = EdgeDetection.houghTransformation(preProcessedImage);
        int diagonal = (int) Math.ceil(Math.sqrt(Math.pow(preProcessedImage.getHeight(), 2) + Math.pow(preProcessedImage.getWidth(), 2)));
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

        // sort lines
        lines.sort((line1, line2) -> Integer.compare(line2.votes, line1.votes));
        int lineCount = Math.min(30, lines.size()); //TODO: abhängigkeitskriterium für mindestanzahl

        // check intersection angle for pairs of lines
        ArrayList<HoughLinePair> pairs = new ArrayList<>();
        ArrayList<HoughLine> validLines = new ArrayList<>();
        int tolerance = 15;
        for (int i = 0; i < lineCount; i++){
            for (int j = i + 1; j < lineCount; j++){

                HoughLine line1 = lines.get(i);
                HoughLine line2 = lines.get(j);
                int angleOfIntersection = PipelineHelper.getAngleOfIntersection(line1, line2, tolerance);

                if (angleOfIntersection != 0) {
                    pairs.add(new HoughLinePair(line1, line2, angleOfIntersection));
                    validLines.add(line1);
                    validLines.add(line2);
                }

            }
        }

        DrawingAndFillingPipeline.drawLines(g, validLines, width, height, diagonal);
        ImageIO.displayImage(lineImage);

        FormChecker.checkTriangleForm(originalImage, pairs, tolerance);

        FormChecker.checkOctagonForm(originalImage, pairs, tolerance);

        FormChecker.checkRectangleForm(originalImage, pairs, tolerance);

        g.dispose();
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
