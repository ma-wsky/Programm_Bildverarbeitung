package classes.Pipeline;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.util.ArrayList;

public class ColorChecker {

    /**
     * Checks colors of maskedSign to validate sign.
     * Calculates inner rectangle using {@link ColorCheckHelper#createScaledPolygon(ArrayList, double)}
     * Checks image colors by accumulating with {@link ColorCheckHelper#analyzeImageColors(BufferedImage, Polygon)}
     * and validating with various checks.
     * @param maskedSign BufferedImage
     * @param rectangle ArrayList<Point>
     * @return boolean if maskedSign is a valid sign
     */
    public static boolean isVorfahrtsstrasseColors(BufferedImage maskedSign, ArrayList<Point> rectangle){
        int width = maskedSign.getWidth();
        int height = maskedSign.getHeight();

        // 1. calculate inner rectangle
        Polygon innerRectangle = ColorCheckHelper.createScaledPolygon(rectangle, 0.50);

        // 2. check image colors
        ColorCheckHelper.ImageColorStats stats = ColorCheckHelper.analyzeImageColors(maskedSign, innerRectangle);

        // early exit
        if (stats.countWhite == 0 || stats.countYellow == 0) return false;

        // 3. check ratio yellow white
        double ratioYellowWhite = (double) stats.countYellow / stats.countWhite;
        if (ratioYellowWhite < 0.4 || ratioYellowWhite > 1) return false;

        // 4. check sign coverage
        double signCoverage = (double) (stats.countYellow + stats.countWhite) / stats.totalPixels;
        if (signCoverage <= 0.75) return false;

        // 5. check yellow and white center points
        double centerTolerance = Math.max(width, height) * 0.10;
        boolean centersMatch = (stats.getSquaredCenterDistanceYellowWhite() <= (centerTolerance * centerTolerance));
        if (!centersMatch) return false;

        // 6. check center is yellow
        return stats.totalCenterPixels > 0 && ((double) stats.whitePixelsInCenter / stats.totalCenterPixels < 0.50);
    }


    /**
     * Checks colors of maskedSign to validate sign.
     * Checks images average color using {@link ColorCheckHelper#hasValidTriangleSignAverageColor(BufferedImage, int, int)}
     * Calculates inner and outer triangle using {@link ColorCheckHelper#createScaledPolygon(ArrayList, double)}
     * Checks for three red edges using {@link ColorCheckHelper#checkTriangleEdgesRed(BufferedImage, Polygon, Polygon, ArrayList, int, int)}
     * Checks image colors by accumulating with {@link ColorCheckHelper#analyzeImageColors(BufferedImage, Polygon)}
     * and validating with various checks.
     * @param maskedSign BufferedImage
     * @param triangle ArrayList<Point>
     * @return boolean if maskedSign is a valid sign
     */
    public static boolean isTriangleSignColors(BufferedImage maskedSign, ArrayList<Point> triangle){
        int width = maskedSign.getWidth();
        int height = maskedSign.getHeight();

        // 1. early exit valid average color
        if (!ColorCheckHelper.hasValidTriangleSignAverageColor(maskedSign, width, height)) return false;

        // 2. calculate inner triangle
        Polygon innerTriangle = ColorCheckHelper.createScaledPolygon(triangle, 0.50);
        Polygon outerTriangle = ColorCheckHelper.createScaledPolygon(triangle, 1.0);

        // 3. early exit three red edges
        if (!ColorCheckHelper.checkTriangleEdgesRed(maskedSign, outerTriangle, innerTriangle, triangle, width, height)) return false;

        // 4. check image colors
        ColorCheckHelper.ImageColorStats stats = ColorCheckHelper.analyzeImageColors(maskedSign, innerTriangle);

        // early exit for amounts
        if (stats.countWhite == 0 || stats.countRed == 0 || stats.totalCenterPixels == 0 || stats.totalEdgePixels == 0 || stats.whitePixelsInCenter == 0){
            return false;
        }

        // 5. check base criteria

        // check sign coverage
        double signCoverage = (double) (stats.countRed + stats.countWhite + stats.blackPixelsInCenter) / stats.totalPixels;
        if (signCoverage < 0.75) return false;

        // check edge red ratio
        double edgeRedRatio = (double) stats.redPixelsAtEdge / stats.totalEdgePixels;
        if (edgeRedRatio <= 0.50) return false;

        // check center points match
        double centerDistanceSquared = stats.getSquaredCenterDistanceRedWhite();
        double centerTolerance = Math.max(width, height) * 0.10;
        if (centerDistanceSquared > (centerTolerance * centerTolerance)) return false;

        // check color ratios
        double ratioRedWhite = (double) stats.countRed / stats.countWhite;
        double centerWhiteRatio = (double) stats.whitePixelsInCenter / stats.totalCenterPixels;
        double blackWhiteRatio = (double) stats.blackPixelsInCenter / stats.whitePixelsInCenter;

        // 6. check which sign type

        // pure white center, correct redWhiteRatio
        boolean isVorfahrtAchten = (ratioRedWhite >= 0.75 && ratioRedWhite <= 1.5) &&
                (centerWhiteRatio > 0.50);

        // black in center, correct redWhiteRatio, correct blackWhiteRatio, correct centerWhiteRatio
        boolean isVorfahrt = (ratioRedWhite >= 0.8 && ratioRedWhite <= 1.5) &&
                (blackWhiteRatio >= 0.2 && blackWhiteRatio <= 0.6) &&
                (centerWhiteRatio > 0.30) &&
                ((double) stats.blackPixelsInCenter / stats.totalCenterPixels > 0.1);

        return isVorfahrtAchten || isVorfahrt;
    }


    /**
     * Checks colors of maskedSign to validate sign.
     * Checks image colors by accumulating with {@link ColorCheckHelper#analyzeImageColors(BufferedImage, Polygon)}
     * and validating with various checks.
     * @param maskedSign BufferedImage
     * @return boolean if maskedSign is a valid sign
     */
    public static boolean isStoppColors(BufferedImage maskedSign){
        int width = maskedSign.getWidth();
        int height = maskedSign.getHeight();

        // 1. check image colors
        ColorCheckHelper.ImageColorStats stats = ColorCheckHelper.analyzeImageColors(maskedSign, null);

        // early exit
        if (stats.countWhite == 0 || stats.countRed == 0) return false;

        // 2. check ratio red white
        double ratioRedWhite = (double) stats.countRed / stats.countWhite;
        if (ratioRedWhite < 1.8 || ratioRedWhite > 6.0) return false;

        // 3. check sign coverage
        double signCoverage = (double) (stats.countRed + stats.countWhite) / stats.totalPixels;
        if (signCoverage < 0.90) return false;

        // 4. check for red center
        Point redCenter = stats.getRedCenter();
        double expectedCenterX = width / 2.0;
        double expectedCenterY = height / 2.0;
        double distanceX = redCenter.x - expectedCenterX;
        double distanceY = redCenter.y - expectedCenterY;

        double tolerance = Math.max(width, height) * 0.07;
        double distanceSquared = distanceX * distanceX + distanceY * distanceY;
        return (distanceSquared <= (tolerance * tolerance));
    }

}
