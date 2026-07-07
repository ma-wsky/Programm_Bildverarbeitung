package classes.Pipeline;

import java.awt.image.BufferedImage;
import classes.DescriptiveStatistics;
import classes.GlobalHelperFunctions;

public class CharacteristicsChecker {

    /**
     * Checks entropy and colours of Rectangle to determine if it is a vorfahrtstraße-sign.
     * Checks: ratio yellow white, area coverage of yellow white, center points of yellow white, white pixels in center.
     * @param maskedSign BufferedImage rectangle to check
     * @return boolean if vorfahrtstraße-sign
     */
    public static boolean isVorfahrtsstrasseColorsAndStats(BufferedImage maskedSign) {

        // stats
//        DescriptiveStatistics stats = new DescriptiveStatistics(maskedSign);
//        stats.calculateAllStatistics();
//        boolean entropyValid = (stats.getEntropy() < 3.0); // TODO: entropy is high (6.5) for pixelated low quality images
//        boolean medianValid = (stats.getMedian() > 200); // TODO: median too low for underexposed images

        // accumulate colour pixels
        int width = maskedSign.getWidth();
        int height = maskedSign.getHeight();

        int countYellowPixels = 0;
        int countWhitePixels = 0;
        int totalPixels = 0;

        double sumXYellow = 0, sumYYellow = 0;
        double sumXWhite = 0, sumYWhite = 0;

        int innerMinX = (int) (width * 0.25);
        int innerMaxX = (int) (width * 0.75);
        int innerMinY = (int) (height * 0.25);
        int innerMaxY = (int) (height * 0.75);

        int whitePixelsInCenter = 0;

        for (int x = 0; x < width; x++){
            for (int y = 0; y < height; y++){
                int rgb = maskedSign.getRGB(x, y);
                if ((rgb & 0x00FFFFFF) == 0) continue;
                totalPixels++;

                double[] hsv = GlobalHelperFunctions.convertRGBToHSV(rgb);
                double h = hsv[0];
                double s = hsv[1];
                double v = hsv[2];

                // yellow
                boolean isHueYellow = (h >= 35.0 && h <= 60.0);
                boolean isSaturated = (s >= 0.3);
                boolean isBright = (v >= 0.4);

                // white
                boolean isWhite = (s <= 0.30);
                boolean isBrightWhite = (v >= 0.4); //TODO: prev: 0.7

                if (isHueYellow && isSaturated && isBright){
                    countYellowPixels++;
                    sumXYellow += x;
                    sumYYellow += y;
                } else if (isWhite && isBrightWhite){
                    countWhitePixels++;
                    sumXWhite += x;
                    sumYWhite += y;

                    if (x >= innerMinX && x <= innerMaxX && y >= innerMinY && y <= innerMaxY){
                        whitePixelsInCenter++;
                    }
                }
            }
        }

        // evaluate
        if (countWhitePixels == 0 || countYellowPixels == 0) return false;

        // ratio yellow white
        double ratioYellowWhite = (double) countYellowPixels / countWhitePixels;
        boolean ratioValid = (ratioYellowWhite >= 0.4 && ratioYellowWhite <= 1);

        // sign coverage
        double signCoverage = (double) (countYellowPixels + countWhitePixels) / totalPixels;
        boolean coverageValid = (signCoverage > 0.75);

        // yellow and white center points
        double centerXYellow = sumXYellow / countYellowPixels;
        double centerYYellow = sumYYellow / countYellowPixels;
        double centerXWhite = sumXWhite / countWhitePixels;
        double centerYWhite = sumYWhite / countWhitePixels;

        double centerTolerance = Math.max(width, height) * 0.10;
        double centerDistance = Math.sqrt(Math.pow(centerXYellow - centerXWhite, 2) + Math.pow(centerYYellow - centerYWhite, 2));
        boolean centersMatch = (centerDistance <= centerTolerance);

        // white pixels in center
        int totalCenterPixels = (innerMaxX - innerMinX) * (innerMaxY - innerMinY);
        boolean centerIsYellow = ((double) whitePixelsInCenter / totalCenterPixels < 0.5);

        return //entropyValid &&
               //medianValid &&
                ratioValid &&
                coverageValid &&
                centerIsYellow &&
                centersMatch;
    }

    /**
     * Checks entropy and colours of octagon to determine if it is a stopp-sign.
     * Checks: entropy, ratio red white, area coverage of red white, center point of red.
     * @param maskedSign BufferedImage octagon to check
     * @return boolean if stopp-sign
     */
    public static boolean isStoppColorAndStats(BufferedImage maskedSign) {

        // stats
//        DescriptiveStatistics stats = new DescriptiveStatistics(maskedSign);
//        stats.calculateAllStatistics();
//        boolean entropyValid = (stats.getEntropy() < 3.3);
//        boolean medianValid = (stats.getMedian() > 200); //TODO: median in photos is too low due to lighting

        // accumulate colour pixels
        int width = maskedSign.getWidth();
        int height = maskedSign.getHeight();

        int countRedPixels = 0;
        int countWhitePixels = 0;
        int totalPixels = 0;

        double sumXRed = 0, sumYRed = 0;

        for (int x = 0; x < width; x++){
            for (int y = 0; y < height; y++){
                int rgb = maskedSign.getRGB(x, y);
                if ((rgb & 0x00FFFFFF) == 0) continue;
                totalPixels++;

                double[] hsv = GlobalHelperFunctions.convertRGBToHSV(rgb);
                double h = hsv[0];
                double s = hsv[1];
                double v = hsv[2];

                // red
                boolean isHueRed = (h >= 0.0 && h <= 30.0) || (h >= 335.0 && h <= 360.0);
                boolean isSaturated = (s >= 0.3);
                boolean isBright = (v >= 0.4);

                // white
                boolean isWhite = (s <= 0.30);
                boolean isBrightWhite = (v >= 0.7);

                if (isHueRed && isSaturated && isBright){
                    countRedPixels++;
                    sumXRed += x;
                    sumYRed += y;

                } else if (isWhite && isBrightWhite){
                    countWhitePixels++;

                }
            }
        }

        // evaluate
        if (countWhitePixels == 0 || countRedPixels == 0) return false;

        // ratio red white
        double ratioRedWhite = (double) countRedPixels / countWhitePixels;
        boolean ratioValid = (ratioRedWhite >= 1.8 && ratioRedWhite <= 4.5);

        // sign coverage
        double signCoverage = (double) (countRedPixels + countWhitePixels) / totalPixels;
        boolean coverageValid = (signCoverage > 0.90);

        // center red
        double centerXRed = sumXRed / countRedPixels;
        double centerYRed = sumYRed / countRedPixels;
        double expectedCenterX = width / 2.0;
        double expectedCenterY = height / 2.0;

        double tolerance = Math.max(width, height) * 0.07;
        double distance = Math.sqrt(Math.pow(centerXRed - expectedCenterX, 2) + Math.pow(centerYRed - expectedCenterY, 2));
        boolean redIsCentered = (distance <= tolerance);

        return //entropyValid &&
                //medianValid &&
                ratioValid &&
                coverageValid &&
                redIsCentered;
    }

    /**
     * Checks entropy and colours of triangle to determine if it is a vorfahrt-sign.
     * Checks: entropy, ratio red white, area coverage of red white, center points of red white, red pixels in center, black pixels in center.
     * @param maskedSign BufferedImage octagon to check
     * @return boolean if vorfahrt-sign
     */
    public static boolean isVorfahrtColorsAndStats(BufferedImage maskedSign) {
        int width = maskedSign.getWidth();
        int height = maskedSign.getHeight();

        // check average color
        long sumR = 0, sumG = 0, sumB = 0;
        int sampledPixels = 0;

        for (int x = 0; x < width; x += 5) {
            for (int y = 0; y < height; y += 5) {
                int rgb = maskedSign.getRGB(x, y);
                if ((rgb & 0x00FFFFFF) == 0) continue;

                sumR += (rgb >> 16) & 0xFF;
                sumG += (rgb >> 8) & 0xFF;
                sumB += rgb & 0xFF;
                sampledPixels++;
            }
        }

        if (sampledPixels > 0) {
            double avgR = (double) sumR / sampledPixels;
            double avgG = (double) sumG / sampledPixels;
            double avgB = (double) sumB / sampledPixels;

            if (avgB > avgR || avgR < 80 || avgG > avgR) {
                return false;
            }
        } else {
            return false;
        }

//        // stats
//        DescriptiveStatistics stats = new DescriptiveStatistics(maskedSign);
//        stats.calculateAllStatistics();
//        boolean entropyValid = (stats.getEntropy() < 3.3);
//        //boolean medianValid = (stats.getMedian() > 200);

        // accumulate colour pixels
        int countRedPixels = 0;
        int countWhitePixels = 0;
        int totalPixels = 0;

        double sumXRed = 0, sumYRed = 0;
        double sumXWhite = 0, sumYWhite = 0;

        int innerMinX = (int) (width * 0.25);
        int innerMaxX = (int) (width * 0.75);
        int innerMinY = (int) (height * 0.25);
        int innerMaxY = (int) (height * 0.75);

        int redPixelsInCenter = 0;
        int blackPixelsInCenter = 0;

        for (int x = 0; x < width; x++){
            for (int y = 0; y < height; y++){
                int rgb = maskedSign.getRGB(x, y);
                if ((rgb & 0x00FFFFFF) == 0) continue;
                totalPixels++;

                double r = ((rgb >> 16) & 0xff) / 255.0;
                double g = ((rgb >> 8) & 0xff) / 255.0;
                double b = (rgb & 0xff) / 255.0;

                double max = Math.max(Math.max(r, g), b);
                double min = Math.min(Math.min(r, g), b);
                double delta = max-min;

                double h, s, v;

                // calc hue
                if (delta == 0) {
                    h = 0;
                } else if (max == r) {
                    h = 60 * (((g - b) / delta) % 6);
                } else if (max == g) {
                    h = 60 * (((b - r) / delta) + 2);
                } else { // max == b
                    h = 60 * (((r - g) / delta) + 4);
                }

                if (h < 0) h += 360;

                // calc saturation
                if (max == 0) {
                    s = 0;
                } else{
                    s = delta / max;
                }

                // calc value
                v = max;

                // red
                boolean isHueRed = (h >= 0.0 && h <= 30.0) || (h >= 335.0 && h <= 360.0);
                boolean isSaturated = (s >= 0.3);
                boolean isBright = (v >= 0.3); //TODO: prev: 0.4

                // white
                boolean isWhite = (s <= 0.30);
                boolean isBrightWhite = (v >= 0.4); //TODO: prev: 0.7

                // black
                boolean isBlack = (s <= 0.50); //TODO: prev: 0.3, in real photos black is never not saturated due to lighting
                boolean isPitchBlack = (v <= 0.2);

                boolean innerPixel = x >= innerMinX && x <= innerMaxX && y >= innerMinY && y <= innerMaxY;
                if (isHueRed && isSaturated && isBright){
                    countRedPixels++;
                    sumXRed += x;
                    sumYRed += y;

                    if (innerPixel){
                        redPixelsInCenter++;
                    }
                } else if (isWhite && isBrightWhite){
                    countWhitePixels++;
                    sumXWhite += x;
                    sumYWhite += y;
                } else if (isBlack && isPitchBlack) {
                    if (innerPixel){
                        blackPixelsInCenter++;
                    }
                }
            }
        }

        // evaluate
        if (countWhitePixels == 0 || countRedPixels == 0) return false;

        // ratio red white
        double ratioRedWhite = (double) countRedPixels / countWhitePixels;
        boolean ratioValid = (ratioRedWhite >= 1.0 && ratioRedWhite <= 1.5);

        // sign coverage
        double signCoverage = (double) (countRedPixels + countWhitePixels + blackPixelsInCenter) / totalPixels;
        boolean coverageValid = (signCoverage > 0.75);

        // red and white center points
        double centerXRed = sumXRed / countRedPixels;
        double centerYRed = sumYRed / countRedPixels;
        double centerXWhite = sumXWhite / countWhitePixels;
        double centerYWhite = sumYWhite / countWhitePixels;

        double centerTolerance = Math.max(width, height) * 0.10;
        double centerDistance = Math.sqrt(Math.pow(centerXRed - centerXWhite, 2) + Math.pow(centerYRed - centerYWhite, 2));
        boolean centersMatch = (centerDistance <= centerTolerance);

        // red pixels in center
        int totalCenterPixels = (innerMaxX - innerMinX) * (innerMaxY - innerMinY) / 2;
        boolean centerIsRed = ((double) redPixelsInCenter / totalCenterPixels < 0.05);

        // black pixels in center
        boolean centerHasBlack = ((double) blackPixelsInCenter / totalCenterPixels > 0.2);

        return //entropyValid &&
                //medianValid &&
                ratioValid &&
                coverageValid &&
                centersMatch &&
                !centerIsRed &&
                centerHasBlack;
    }

    /**
     * Checks entropy and colours of triangle to determine if it is a vorfahrt-achten-sign.
     * Checks: entropy, ratio red white, area coverage of red white, center points of red white, red pixels in center.
     * @param maskedSign BufferedImage octagon to check
     * @return boolean if vorfahrt-achten-sign
     */
    public static boolean isVorfahrtAchtenColorsAndStats(BufferedImage maskedSign) {
        int width = maskedSign.getWidth();
        int height = maskedSign.getHeight();

        // check average color
        long sumR = 0, sumG = 0, sumB = 0;
        int sampledPixels = 0;

        for (int x = 0; x < width; x += 5) {
            for (int y = 0; y < height; y += 5) {
                int rgb = maskedSign.getRGB(x, y);
                if ((rgb & 0x00FFFFFF) == 0) continue;

                sumR += (rgb >> 16) & 0xFF;
                sumG += (rgb >> 8) & 0xFF;
                sumB += rgb & 0xFF;
                sampledPixels++;
            }
        }

        if (sampledPixels > 0) {
            double avgR = (double) sumR / sampledPixels;
            double avgG = (double) sumG / sampledPixels;
            double avgB = (double) sumB / sampledPixels;

            if (avgB > avgR || avgR < 80 || avgG > avgR) {
                return false;
            }
        } else {
            return false;
        }

        // stats
//        DescriptiveStatistics stats = new DescriptiveStatistics(maskedSign);
//        stats.calculateAllStatistics();
//        boolean entropyValid = (stats.getEntropy() < 3.3); //TODO: entropy is high (>4) for real life photographs
//        boolean medianValid = (stats.getMedian() > 200);

        // accumulate colour pixels
        int countRedPixels = 0;
        int countWhitePixels = 0;
        int totalPixels = 0;

        double sumXRed = 0, sumYRed = 0;
        double sumXWhite = 0, sumYWhite = 0;

        int innerMinX = (int) (width * 0.25);
        int innerMaxX = (int) (width * 0.75);
        int innerMinY = (int) (height * 0.25);
        int innerMaxY = (int) (height * 0.75);

        int redPixelsInCenter = 0;

        for (int x = 0; x < width; x++){
            for (int y = 0; y < height; y++){
                int rgb = maskedSign.getRGB(x, y);
                if ((rgb & 0x00FFFFFF) == 0) continue;
                totalPixels++;

                double r = ((rgb >> 16) & 0xff) / 255.0;
                double g = ((rgb >> 8) & 0xff) / 255.0;
                double b = (rgb & 0xff) / 255.0;

                double max = Math.max(Math.max(r, g), b);
                double min = Math.min(Math.min(r, g), b);
                double delta = max-min;

                double h, s, v;

                // calc hue
                if (delta == 0) {
                    h = 0;
                } else if (max == r) {
                    h = 60 * (((g - b) / delta) % 6);
                } else if (max == g) {
                    h = 60 * (((b - r) / delta) + 2);
                } else { // max == b
                    h = 60 * (((r - g) / delta) + 4);
                }

                if (h < 0) h += 360;

                // calc saturation
                if (max == 0) {
                    s = 0;
                } else{
                    s = delta / max;
                }

                // calc value
                v = max;

                // red
                boolean isHueRed = (h >= 0.0 && h <= 30.0) || (h >= 335.0 && h <= 360.0);
                boolean isSaturated = (s >= 0.3);
                boolean isBright = (v >= 0.4);

                // white
                boolean isWhite = (s <= 0.30);
                boolean isBrightWhite = (v >= 0.4); //TODO: prev: 0.7 -> real life photos can be dark

                if (isHueRed && isSaturated && isBright){
                    countRedPixels++;
                    sumXRed += x;
                    sumYRed += y;

                    if (x >= innerMinX && x <= innerMaxX && y >= innerMinY && y <= innerMaxY){
                        redPixelsInCenter++;
                    }
                } else if (isWhite && isBrightWhite){
                    countWhitePixels++;
                    sumXWhite += x;
                    sumYWhite += y;
                }
            }
        }

        // evaluate
        if (countWhitePixels == 0 || countRedPixels == 0) return false;

        // ratio red white
        double ratioRedWhite = (double) countRedPixels / countWhitePixels;
        boolean ratioValid = (ratioRedWhite >= 0.75 && ratioRedWhite <= 1.5);

        // sign coverage
        double signCoverage = (double) (countRedPixels + countWhitePixels) / totalPixels;
        boolean coverageValid = (signCoverage > 0.75);

        // red and white center points
        double centerXRed = sumXRed / countRedPixels;
        double centerYRed = sumYRed / countRedPixels;
        double centerXWhite = sumXWhite / countWhitePixels;
        double centerYWhite = sumYWhite / countWhitePixels;

        double centerTolerance = Math.max(width, height) * 0.10;
        double centerDistance = Math.sqrt(Math.pow(centerXRed - centerXWhite, 2) + Math.pow(centerYRed - centerYWhite, 2));
        boolean centersMatch = (centerDistance <= centerTolerance);

        // red pixels in center
        int totalCenterPixels = (innerMaxX - innerMinX) * (innerMaxY - innerMinY) / 2;
        boolean centerIsRed = ((double) redPixelsInCenter / totalCenterPixels < 0.05);

        return //entropyValid &&
                //medianValid &&
                ratioValid &&
                coverageValid &&
                !centerIsRed &&
                centersMatch;
    }

}
