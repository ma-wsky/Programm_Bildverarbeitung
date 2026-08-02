package classes.Pipeline.Helper;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.util.ArrayList;

public class ColorCheckHelper {

    //------------------------------------------------------------------------------------------------------------------
    // helper for rectangle, triangle and octagon
    //------------------------------------------------------------------------------------------------------------------

    /**
     * Helper class for returning {@link ColorCheckHelper#analyzeImageColors(BufferedImage, Polygon)}
     */
    public static class ImageColorStats{
        public int totalPixels = 0;
        public int countYellow = 0;
        public int countWhite = 0;
        public int countRed = 0;
        private double sumXYellow = 0, sumYYellow = 0;
        private double sumXWhite = 0, sumYWhite = 0;
        private double sumXRed = 0, sumYRed = 0;

        public int totalCenterPixels = 0;
        public int totalEdgePixels = 0;
        public int whitePixelsInCenter = 0;
        public int blackPixelsInCenter = 0;
        public int redPixelsAtEdge = 0;

        private double getSquaredDistance(double sumX1, double sumY1, int count1, double sumX2, double sumY2, int count2) {
            if (count1 == 0 || count2 == 0) return Double.MAX_VALUE;
            double dx = (sumX1 / count1) - (sumX2 / count2);
            double dy = (sumY1 / count1) - (sumY2 / count2);
            return dx * dx + dy * dy;
        }

        public double getSquaredCenterDistanceYellowWhite() {
            return getSquaredDistance(sumXYellow, sumYYellow, countYellow, sumXWhite, sumYWhite, countWhite);
        }

        public double getSquaredCenterDistanceRedWhite() {
            return getSquaredDistance(sumXRed, sumYRed, countRed, sumXWhite, sumYWhite, countWhite);
        }

        public Point getRedCenter(){
            if (countRed == 0) return new Point(0, 0);
            double centerXRed = sumXRed / countRed;
            double centerYRed = sumYRed / countRed;
            return new Point((int) centerXRed, (int) centerYRed);
        }
    }

    /**
     * Analyzes the colors of image.
     * Checks pixels for inside innerShape, being red, yellow, white or black.
     * @param image BufferedImage
     * @param innerShape Polygon
     * @return ImageColorStats class
     */
    public static ImageColorStats analyzeImageColors(BufferedImage image, Polygon innerShape){
        ImageColorStats stats = new ImageColorStats();
        int width = image.getWidth();
        int height = image.getHeight();

        for (int x = 0; x < width; x++){
            for (int y = 0; y < height; y++){
                int rgb = image.getRGB(x, y);
                if ((rgb & 0x00FFFFFF) == 0) continue;
                stats.totalPixels++;

                double[] hsv = GlobalHelperFunctions.convertRGBToHSV(rgb);
                double h = hsv[0];
                double s = hsv[1];
                double v = hsv[2];

                boolean isRed = ColorCheckHelper.isRed(h, s, v);
                boolean isYellow = ColorCheckHelper.isYellow(h, s, v);
                boolean isWhite = ColorCheckHelper.isWhite(s, v);
                boolean isBlack = ColorCheckHelper.isBlack(s, v);

                boolean isCenterPixel = (innerShape != null) && innerShape.contains(x, y);

                if (innerShape != null){
                    if (isCenterPixel) stats.totalCenterPixels++;
                    else stats.totalEdgePixels++;
                }

                if (isYellow){
                    stats.countYellow++;
                    stats.sumXYellow += x;
                    stats.sumYYellow += y;
                } else if (isRed){
                    stats.countRed++;
                    stats.sumXRed += x;
                    stats.sumYRed += y;
                    if (innerShape != null && !isCenterPixel) stats.redPixelsAtEdge++;
                }

                if (isWhite) {
                    stats.countWhite++;
                    stats.sumXWhite += x;
                    stats.sumYWhite += y;
                    if (innerShape != null && isCenterPixel) stats.whitePixelsInCenter++;
                }

                if (isBlack && isCenterPixel) {
                    stats.blackPixelsInCenter++;
                }

            }
        }
        return stats;
    }

    public static boolean isRed(double h, double s, double v) {
        return ((h >= 0.0 && h <= 30.0) || (h >= 335.0 && h <= 360.0)) && (s >= 0.25) && (v >= 0.20);
    }

    public static boolean isYellow(double h, double s, double v) {
        return (h >= 35.0 && h <= 60.0) && (s >= 0.30) && (v >= 0.40);
    }

    public static boolean isWhite(double s, double v) {
        return (s <= 0.30) && (v >= 0.30);
    }

    public static boolean isBlack(double s, double v) {
        return (s < 0.25) && (v < 0.20);
    }

    /**
     * Returns a polygon from points scaled to scale.
     * @param points ArrayList<Point>
     * @param scale double
     * @return Polygon
     */
    public static Polygon createScaledPolygon(ArrayList<Point> points, double scale){
        // calculate top left
        int minX = Integer.MAX_VALUE;
        int minY = Integer.MAX_VALUE;
        double sumX = 0;
        double sumY = 0;

        for (Point p : points) {
            if (p.x < minX) minX = p.x;
            if (p.y < minY) minY = p.y;
            sumX += p.x;
            sumY += p.y;
        }

        // calculate local center
        int numPoints = points.size();
        double localCenterX = (sumX / numPoints) - minX;
        double localCenterY = (sumY / numPoints) - minY;

        // create polygon
        Polygon polygon = new Polygon();
        for (Point p : points){
            double localX = p.x - minX;
            double localY = p.y - minY;

            int newX = (int) Math.round(localCenterX + scale * (localX - localCenterX));
            int newY = (int) Math.round(localCenterY + scale * (localY - localCenterY));

            polygon.addPoint(newX, newY);
        }
        return polygon;
    }

    //------------------------------------------------------------------------------------------------------------------
    // helper for triangle
    //------------------------------------------------------------------------------------------------------------------

    /**
     * Checks triangle for three red edges.
     * @param maskedSign BufferedImage
     * @param outerTriangle Polygon
     * @param innerTriangle Polygon
     * @param triangle ArrayList<Point>
     * @param width int
     * @param height int
     * @return boolean if triangle has three red edges
     */
    public static boolean checkTriangleEdgesRed(BufferedImage maskedSign, Polygon outerTriangle, Polygon innerTriangle, ArrayList<Point> triangle, int width, int height) {
        // calculate global center
        double[] centerCoords = GlobalHelperFunctions.calculateCenterCoordinates(triangle);
        double centerX = centerCoords[0];
        double centerY = centerCoords[1];

        int edgeThreshold = 20;
        double minRedRatio = 0.50;
        double[] stepsToCenter = {0.10, 0.15, 0.20};
        int numPoints = triangle.size();

        // calculate top left
        int minX = Integer.MAX_VALUE;
        int minY = Integer.MAX_VALUE;
        for (Point p : triangle){
            if (p.x < minX) minX = p.x;
            if (p.y < minY) minY = p.y;
        }

        // local center
        double localCenterX = centerX - minX;
        double localCenterY = centerY - minY;

        // three edges
        for (int i = 0; i < numPoints; i++){
            Point start = triangle.get(i);
            Point end = triangle.get((i + 1) % numPoints);

            double startX = start.x - minX;
            double startY = start.y - minY;
            double endX = end.x - minX;
            double endY = end.y - minY;

            int redPixelAmount = 0;

            // accumulate red pixels over each
            for (int j = 0; j < edgeThreshold; j++){
                double t = (j + 0.5) / (double) edgeThreshold;
                double edgeX = startX + t * (endX - startX);
                double edgeY = startY + t * (endY - startY);

                // 10%, 15% and 20% from edge to center
                for (double step : stepsToCenter){
                    int sampleX = (int) Math.round(edgeX + step * (localCenterX - edgeX));
                    int sampleY = (int) Math.round(edgeY + step * (localCenterY - edgeY));

                    boolean inOuterTriangle = outerTriangle.contains(sampleX, sampleY);
                    boolean inInnerTriangle = innerTriangle.contains(sampleX, sampleY);

                    if (inOuterTriangle && !inInnerTriangle){
                        if (sampleX >= 0 && sampleX < width && sampleY >= 0 && sampleY < height){
                            int rgb = maskedSign.getRGB(sampleX, sampleY);
                            double[] hsv = GlobalHelperFunctions.convertRGBToHSV(rgb);
                            if (ColorCheckHelper.isRed(hsv[0], hsv[1], hsv[2])){
                                redPixelAmount++;
                            }
                        }
                    }
                }
            }

            // check against tolerance
            double redRatio = (double) redPixelAmount / (edgeThreshold * stepsToCenter.length);
            if (redRatio < minRedRatio){
                return false;
            }
        }
        return true;
    }

    /**
     * Sums and averages r, g, b values of maskedSign.
     * @param maskedSign BufferedImage
     * @param width int
     * @param height int
     * @return boolean if triangle has valid average color
     */
    public static boolean hasValidTriangleSignAverageColor(BufferedImage maskedSign, int width, int height) {
        // sum r, g, b
        long sumR = 0;
        long sumG = 0;
        long sumB = 0;
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
        if (sampledPixels == 0) return false;

        // average r, g, b
        double avgR = (double) sumR / sampledPixels;
        double avgG = (double) sumG / sampledPixels;
        double avgB = (double) sumB / sampledPixels;

        return !(avgB > avgR) && !(avgR < 65) && !(avgG > avgR);
    }
}
