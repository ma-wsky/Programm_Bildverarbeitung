package classes.Pipeline;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.Random;

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


                boolean isHueRed = (h >= 0.0 && h <= 30.0) || (h >= 335.0 && h <= 360.0);
                boolean isRed = isHueRed && (s >= 0.25) && (v >= 0.20);

                boolean isWhite = (s <= 0.20) && (v >= 0.30);

                if (isRed){
                    countRedPixels++;
                    sumXRed += x;
                    sumYRed += y;
                }

                if (isWhite){
                    countWhitePixels++;
                }
            }
        }

        // evaluate
        if (countWhitePixels == 0 || countRedPixels == 0) return false;

        // ratio red white
        double ratioRedWhite = (double) countRedPixels / countWhitePixels;
        boolean ratioValid = (ratioRedWhite >= 1.8 && ratioRedWhite <= 6.0);

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

    public static boolean isTriangleSignColorAndStats(BufferedImage maskedSign, ArrayList<Point> triangle){
        int width = maskedSign.getWidth();
        int height = maskedSign.getHeight();

        // 1. check average color
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

        if (sampledPixels == 0) return false;


        double avgR = (double) sumR / sampledPixels;
        double avgG = (double) sumG / sampledPixels;
        double avgB = (double) sumB / sampledPixels;

        if (avgB > avgR || avgR < 65 || avgG > avgR) {
            return false;
        }

        // 2. calc inner and outer triangle
        int cropX = Math.min(Math.min(triangle.get(0).x, triangle.get(1).x), triangle.get(2).x);
        int cropY = Math.min(Math.min(triangle.get(0).y, triangle.get(1).y), triangle.get(2).y);

        double centerX = ((triangle.get(0).x - cropX) +
                (triangle.get(1).x - cropX) +
                (triangle.get(2).x - cropX)) / 3.0;

        double centerY = ((triangle.get(0).y - cropY) +
                (triangle.get(1).y - cropY) +
                (triangle.get(2).y - cropY)) / 3.0;

        Polygon innerTriangle = new Polygon();
        double scale = 0.50;
        for (Point p : triangle){
            int newX = (int) Math.round(centerX + scale * (p.x - cropX - centerX));
            int newY = (int) Math.round(centerY + scale * (p.y - cropY - centerY));
            innerTriangle.addPoint(newX, newY);
        }

        Polygon outerTriangle = new Polygon();
        Point[] outerTrianglePoints = new Point[3];
        for (int i = 0; i < 3; i++){
            Point p = triangle.get(i);
            int newX = p.x - cropX;
            int newY = p.y - cropY;

            outerTrianglePoints[i] = new Point(newX, newY);
            outerTriangle.addPoint(newX, newY);
        }

//        // debug red edges image
//        // Kopie des Bildes erstellen, damit wir das Original nicht überschreiben
//        BufferedImage debugImg = new BufferedImage(
//                maskedSign.getWidth(),
//                maskedSign.getHeight(),
//                BufferedImage.TYPE_INT_ARGB
//        );
//        Graphics2D gD = debugImg.createGraphics();
//        gD.drawImage(maskedSign, 0, 0, null);
//
//        width = maskedSign.getWidth();
//        height = maskedSign.getHeight();
//
//        // 1. Koordinaten & Schwerpunkte wie im echten Check berechnen
//        cropX = Math.min(Math.min(triangle.get(0).x, triangle.get(1).x), triangle.get(2).x);
//        cropY = Math.min(Math.min(triangle.get(0).y, triangle.get(1).y), triangle.get(2).y);
//
//        centerX = ((triangle.get(0).x - cropX) + (triangle.get(1).x - cropX) + (triangle.get(2).x - cropX)) / 3.0;
//        centerY = ((triangle.get(0).y - cropY) + (triangle.get(1).y - cropY) + (triangle.get(2).y - cropY)) / 3.0;
//
//        // Inneres Dreieck (50% Skalierung)
//        Polygon innerTriangleDebug = new Polygon();
//        scale = 0.50;
//        for (Point p : triangle) {
//            int newX = (int) Math.round(centerX + scale * (p.x - cropX - centerX));
//            int newY = (int) Math.round(centerY + scale * (p.y - cropY - centerY));
//            innerTriangleDebug.addPoint(newX, newY);
//        }
//
//        // Äußeres Dreieck
//        Polygon outerTriangleDebug = new Polygon();
//        Point[] outerPoints = new Point[3];
//        for (int i = 0; i < 3; i++) {
//            Point p = triangle.get(i);
//            int newX = p.x - cropX;
//            int newY = p.y - cropY;
//            outerPoints[i] = new Point(newX, newY);
//            outerTriangleDebug.addPoint(newX, newY);
//        }
//
//        // 2. Polygon-Linien einzeichnen
//        gD.setStroke(new BasicStroke(2));
//        gD.setColor(Color.BLUE);
//        gD.drawPolygon(outerTriangleDebug);
//
//        gD.setColor(Color.CYAN);
//        gD.drawPolygon(innerTriangleDebug);
//
//        // 3. Kanten-Abtastpunkte einzeichnen
//        Point[][] edges = {
//                {outerPoints[0], outerPoints[1]},
//                {outerPoints[1], outerPoints[2]},
//                {outerPoints[2], outerPoints[0]}
//        };
//
//        double[] stepsToCenter = {0.10, 0.15, 0.20};
//        int edgeThreshold = 20;
//
//        for (int i = 0; i < 3; i++) {
//            Point start = edges[i][0];
//            Point end = edges[i][1];
//
//            for (int j = 0; j < edgeThreshold; j++) {
//                double t = (j + 0.5) / (double) edgeThreshold;
//                double edgeX = start.x + t * (end.x - start.x);
//                double edgeY = start.y + t * (end.y - start.y);
//
//                for (double stepToCenter : stepsToCenter) {
//                    int sampleX = (int) Math.round(edgeX + stepToCenter * (centerX - edgeX));
//                    int sampleY = (int) Math.round(edgeY + stepToCenter * (centerY - edgeY));
//
//                    if (sampleX >= 0 && sampleX < width && sampleY >= 0 && sampleY < height) {
//                        int rgb = maskedSign.getRGB(sampleX, sampleY);
//
//                        // Prüfen, ob der Punkt rot ist
//                        if (isPixelRedHSV(rgb)) {
//                            gD.setColor(Color.GREEN); // Treffer (Rot erkannt)
//                        } else {
//                            gD.setColor(Color.RED);   // Niete (Kein Rot)
//                        }
//
//                        // Zeichne ein kleines 2x2 Rechteck für jeden Abtastpunkt
//                        gD.fillRect(sampleX, sampleY, 1, 1);
//                    }
//                }
//            }
//        }
//
//        gD.dispose();

        // 3. check for three red edges
        Point[][] edges = {
                {outerTrianglePoints[0], outerTrianglePoints[1]},
                {outerTrianglePoints[1], outerTrianglePoints[2]},
                {outerTrianglePoints[2], outerTrianglePoints[0]}
        };

        int edgeThreshold = 20;
        double minRedRatio = 0.50;// prev: 0.70
        double[] stepsToCenter = {0.10, 0.15, 0.20};
        //Random rand = new Random();//TODO: deterministisch programmieren

        for (int i = 0; i < 3; i++){
            Point start = edges[i][0];
            Point end = edges[i][1];
            int redPixelAmount = 0;

            for (int j = 0; j < edgeThreshold; j++){
                double t = (j + 0.5) / (double) edgeThreshold;//TODO: deterministisch programmieren
                double edgeX = start.x + t * (end.x - start.x);
                double edgeY = start.y + t * (end.y - start.y);

                for (double stepToCenter : stepsToCenter){
                    int sampleX = (int) Math.round(edgeX + stepToCenter * (centerX - edgeX));
                    int sampleY = (int) Math.round(edgeY + stepToCenter * (centerY - edgeY));boolean inOuterTriangle = outerTriangle.contains(sampleX, sampleY);

                    boolean inInnerTriangle = innerTriangle.contains(sampleX, sampleY);

                    if (inOuterTriangle && !inInnerTriangle){
                        if (sampleX >= 0 && sampleX < width && sampleY >= 0 && sampleY < height){
                            int rgb = maskedSign.getRGB(sampleX, sampleY);
                            if (isPixelRedHSV(rgb)){
                                redPixelAmount++;
                            }
                        }
                    }
                }
            }

            double redRatio = (double) redPixelAmount / (edgeThreshold * stepsToCenter.length);
            if (redRatio < minRedRatio){
                return false;
            }
        }

        // 4. evaluate each pixel
        int totalCenterPixels = 0;
        int whitePixelsInCenter = 0;
        int blackPixelsInCenter = 0;
        int totalEdgePixels = 0;
        int redPixelsAtEdge = 0;
        int countRedPixels = 0;
        int countWhitePixels = 0;
        int totalPixels = 0;

        double sumXRed = 0, sumYRed = 0;
        double sumXWhite = 0, sumYWhite = 0;

        for (int x = 0; x < width; x++){
            for (int y = 0; y < height; y++){
                int rgb = maskedSign.getRGB(x, y);
                if ((rgb & 0x00FFFFFF) == 0) continue;
                totalPixels++;

                double r = ((rgb >> 16) & 0xff) / 255.0;
                double g = ((rgb >> 8) & 0xff) / 255.0;
                double b = (rgb & 0xff) / 255.0;

                // is centerPixel
                boolean centerPixel = innerTriangle.contains(x, y);
                if (centerPixel){
                    totalCenterPixels++;
                } else {
                    totalEdgePixels++;
                }

                // calculate hsv
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

                boolean isHueRed = (h >= 0.0 && h <= 30.0) || (h >= 335.0 && h <= 360.0);
                boolean isRed = isHueRed && (s >= 0.25) && (v >= 0.20);

                boolean isWhite = (s <= 0.20) && (v >= 0.30);
                boolean isBlack = (s < 0.25) && (v < 0.20);

                if (isRed){
                    countRedPixels++;
                    sumXRed += x;
                    sumYRed += y;
                    if (!centerPixel) redPixelsAtEdge++;
                }

                if (isWhite){
                    countWhitePixels++;
                    sumXWhite += x;
                    sumYWhite += y;
                    if (centerPixel) whitePixelsInCenter++;
                }

                if (isBlack && centerPixel){
                    blackPixelsInCenter++;
                }
            }
        }

        // early exit for amounts
        if (countWhitePixels == 0 || countRedPixels == 0 || totalCenterPixels == 0 || totalEdgePixels == 0){
            return false;
        }

        // 5. check base criteria

        // sign coverage
        double signCoverage = (double) (countRedPixels + countWhitePixels + blackPixelsInCenter) / totalPixels;
        if (signCoverage < 0.75) return false;

        // edge red ratio
        double edgeRedRatio = (double) redPixelsAtEdge / totalEdgePixels;
        if (edgeRedRatio <= 0.50) return false;

        // center points match
        double centerXRed = sumXRed / countRedPixels;
        double centerYRed = sumYRed / countRedPixels;
        double centerXWhite = sumXWhite / countWhitePixels;
        double centerYWhite = sumYWhite / countWhitePixels;
        double centerDistance = Math.sqrt(Math.pow(centerXRed - centerXWhite, 2) + Math.pow(centerYRed - centerYWhite, 2));
        double centerTolerance = Math.max(width, height) * 0.10;
        if (centerDistance > centerTolerance) return false;

        // color ratios
        double ratioRedWhite = (double) countRedPixels / countWhitePixels;
        double centerWhiteRatio = (double) whitePixelsInCenter / totalCenterPixels;
        double blackWhiteRatio = (double) blackPixelsInCenter / whitePixelsInCenter;

        // pure white center, correct redWhiteRatio
        boolean isVorfahrtAchten = (ratioRedWhite >= 0.75 && ratioRedWhite <= 1.5) &&
                                   (centerWhiteRatio > 0.50);

        // black in center, correct redWhiteRatio, correct blackWhiteRatio, correct centerWhiteRatio
        boolean isVorfahrt = (ratioRedWhite >= 0.8 && ratioRedWhite <= 1.5) &&
                             (blackWhiteRatio >= 0.2 && blackWhiteRatio <= 0.6) &&
                             (centerWhiteRatio > 0.30) &&
                             ((double) blackPixelsInCenter / totalCenterPixels > 0.1);

        return isVorfahrtAchten || isVorfahrt;
    }

    /**
     * Checks entropy and colours of triangle to determine if it is a vorfahrt-sign.
     * Checks: entropy, ratio red white, area coverage of red white, center points of red white, red pixels in center, black pixels in center.
     * @param maskedSign BufferedImage octagon to check
     * @return boolean if vorfahrt-sign
     */
    public static boolean isVorfahrtColorsAndStats(BufferedImage maskedSign, ArrayList<Point> triangle) {
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

            if (avgB > avgR || avgR < 65 || avgG > avgR) {
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

        // calc inner triangle
        int cropX = Math.min(Math.min(triangle.get(0).x, triangle.get(1).x), triangle.get(2).x);
        int cropY = Math.min(Math.min(triangle.get(0).y, triangle.get(1).y), triangle.get(2).y);

        double centerX = ((triangle.get(0).x - cropX) +
                (triangle.get(1).x - cropX) +
                (triangle.get(2).x - cropX)) / 3.0;

        double centerY = ((triangle.get(0).y - cropY) +
                (triangle.get(1).y - cropY) +
                (triangle.get(2).y - cropY)) / 3.0;
        double scale = 0.50;

        Polygon innerTriangle = new Polygon();
        for (Point p : triangle){
            int newX = (int) Math.round(centerX + scale * (p.x - cropX - centerX));
            int newY = (int) Math.round(centerY + scale * (p.y - cropY - centerY));
            innerTriangle.addPoint(newX, newY);
        }

        // check for three red edges
        Polygon outerTriangle = new Polygon();
        Point[] outerTrianglePoints = new Point[3];
        for (int i = 0; i < 3; i++){
            Point p = triangle.get(i);
            int newX = p.x - cropX;
            int newY = p.y - cropY;

            outerTrianglePoints[i] = new Point(newX, newY);
            outerTriangle.addPoint(newX, newY);
        }

        Point[][] edges = {
                {outerTrianglePoints[0], outerTrianglePoints[1]},
                {outerTrianglePoints[1], outerTrianglePoints[2]},
                {outerTrianglePoints[2], outerTrianglePoints[0]}
        };

        int edgeThreshold = 20;
        double minRedRatio = 0.70;
        Random rand = new Random();

        for (int i = 0; i < 3; i++){
            Point start = edges[i][0];
            Point end = edges[i][1];
            int redPixelAmount = 0;

            for (int j = 0; j < edgeThreshold; j++){
                double t = rand.nextDouble();
                double edgeX = start.x + t * (end.x - start.x);
                double edgeY = start.y + t * (end.y - start.y);
                double stepToCenter = 0.15;
                int sampleX = (int) Math.round(edgeX + stepToCenter * (centerX - edgeX));
                int sampleY = (int) Math.round(edgeY + stepToCenter * (centerY - edgeY));

                boolean inOuterTriangle = outerTriangle.contains(sampleX, sampleY);
                boolean inInnerTriangle = innerTriangle.contains(sampleX, sampleY);

                if (inOuterTriangle && !inInnerTriangle){
                    if (sampleX >= 0 && sampleX < width && sampleY >= 0 && sampleY < height){
                        int rgb = maskedSign.getRGB(sampleX, sampleY);
                        if (isPixelRedHSV(rgb)){
                            redPixelAmount++;
                        }
                    }

                }
            }


            double redRatio = (double) redPixelAmount / edgeThreshold;
            if (redRatio < minRedRatio){
                return false;
            }

        }

        // draw outline of found sign on original image
        Graphics2D gOriginal = maskedSign.createGraphics();
        gOriginal.setColor(Color.BLUE);
        gOriginal.setStroke(new java.awt.BasicStroke(1));
        //gOriginal.drawPolygon(innerTriangle);
        gOriginal.dispose();

        int totalCenterPixels = 0;
        int whitePixelsInCenter = 0;
        int blackPixelsInCenter = 0;
        int totalEdgePixels = 0;
        int redPixelsAtEdge = 0;

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


                boolean innerPixel = innerTriangle.contains(x, y);
                if (innerPixel){
                    totalCenterPixels++;
                } else {
                    totalEdgePixels++;
                }

                boolean isHueRed = (h >= 0.0 && h <= 30.0) || (h >= 335.0 && h <= 360.0);
                boolean isRed = isHueRed && (s >= 0.25) && (v >= 0.20);

                boolean isWhite = (s <= 0.20) && (v >= 0.30);
                boolean isBlack = (s < 0.25) && (v < 0.20);

                if (isRed){
                    countRedPixels++;
                    sumXRed += x;
                    sumYRed += y;
                    if (!innerPixel){
                        redPixelsAtEdge++;
                    }
                }

                if (isWhite){
                    countWhitePixels++;
                    sumXWhite += x;
                    sumYWhite += y;
                    if (innerPixel) {
                        whitePixelsInCenter++;
                    }
                }

                if (isBlack){
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
        boolean ratioValid = (ratioRedWhite >= 0.8 && ratioRedWhite <= 1.5);

        // ratio black white
        double ratioBlackWhite = (double) blackPixelsInCenter / whitePixelsInCenter;
        boolean ratioBlackValid = (ratioBlackWhite >= 0.2 && ratioBlackWhite <= 0.6);

        // sign coverage
        double signCoverage = (double) (countRedPixels + countWhitePixels + blackPixelsInCenter) / totalPixels;
        boolean coverageValid = (signCoverage > 0.75);

        // red and white center points
        double centerXRed = sumXRed / countRedPixels;
        double centerYRed = sumYRed / countRedPixels;
        double centerXWhite = sumXWhite / countWhitePixels;
        double centerYWhite = sumYWhite / countWhitePixels;

        double centerTolerance = Math.max(width, height) * 0.15;
        double centerDistance = Math.sqrt(Math.pow(centerXRed - centerXWhite, 2) + Math.pow(centerYRed - centerYWhite, 2));
        boolean centersMatch = (centerDistance <= centerTolerance);

        // white pixels in center
        boolean centerIsWhite = ((double) whitePixelsInCenter / totalCenterPixels > 0.30);

        // black pixels in center
        boolean centerHasBlack = ((double) blackPixelsInCenter / totalCenterPixels > 0.1);

        // red pixels at edge
        boolean edgeIsRed = ((double) redPixelsAtEdge / totalEdgePixels > 0.50);

        if (ratioValid &&
                ratioBlackValid &&
                coverageValid &&
                centersMatch &&
                centerIsWhite &&
                centerHasBlack &&
                edgeIsRed){
            return true;
        }
        return false;
    }

    /**
     * Checks entropy and colours of triangle to determine if it is a vorfahrt-achten-sign.
     * Checks: entropy, ratio red white, area coverage of red white, center points of red white, red pixels in center.
     * @param maskedSign BufferedImage octagon to check
     * @return boolean if vorfahrt-achten-sign
     */
    public static boolean isVorfahrtAchtenColorsAndStats(BufferedImage maskedSign, ArrayList<Point> triangle) {
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

            if (avgB > avgR || avgR < 65 || avgG > avgR) {
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

        // calc inner triangle
        int cropX = Math.min(Math.min(triangle.get(0).x, triangle.get(1).x), triangle.get(2).x);
        int cropY = Math.min(Math.min(triangle.get(0).y, triangle.get(1).y), triangle.get(2).y);

        double centerX = ((triangle.get(0).x - cropX) +
                (triangle.get(1).x - cropX) +
                (triangle.get(2).x - cropX)) / 3.0;

        double centerY = ((triangle.get(0).y - cropY) +
                (triangle.get(1).y - cropY) +
                (triangle.get(2).y - cropY)) / 3.0;
        double scale = 0.50;

        Polygon innerTriangle = new Polygon();
        for (Point p : triangle){
            int newX = (int) Math.round(centerX + scale * (p.x - cropX - centerX));
            int newY = (int) Math.round(centerY + scale * (p.y - cropY - centerY));
            innerTriangle.addPoint(newX, newY);
        }

        // TODO: debug remaining false positives, tweak threshold if necessary

        // check for three red edges
        Polygon outerTriangle = new Polygon();
        Point[] outerTrianglePoints = new Point[3];
        for (int i = 0; i < 3; i++){
            Point p = triangle.get(i);
            int newX = p.x - cropX;
            int newY = p.y - cropY;

            outerTrianglePoints[i] = new Point(newX, newY);
            outerTriangle.addPoint(newX, newY);
        }

        Point[][] edges = {
                {outerTrianglePoints[0], outerTrianglePoints[1]},
                {outerTrianglePoints[1], outerTrianglePoints[2]},
                {outerTrianglePoints[2], outerTrianglePoints[0]}
        };

        int edgeThreshold = 20;
        double minRedRatio = 0.70;
        Random rand = new Random();

        for (int i = 0; i < 3; i++){
            Point start = edges[i][0];
            Point end = edges[i][1];
            int redPixelAmount = 0;

            for (int j = 0; j < edgeThreshold; j++){
                double t = rand.nextDouble();
                double edgeX = start.x + t * (end.x - start.x);
                double edgeY = start.y + t * (end.y - start.y);
                double stepToCenter = 0.15;
                int sampleX = (int) Math.round(edgeX + stepToCenter * (centerX - edgeX));
                int sampleY = (int) Math.round(edgeY + stepToCenter * (centerY - edgeY));

                boolean inOuterTriangle = outerTriangle.contains(sampleX, sampleY);
                boolean inInnerTriangle = innerTriangle.contains(sampleX, sampleY);

                if (inOuterTriangle && !inInnerTriangle){
                    if (sampleX >= 0 && sampleX < width && sampleY >= 0 && sampleY < height){
                        int rgb = maskedSign.getRGB(sampleX, sampleY);
                        if (isPixelRedHSV(rgb)){
                            redPixelAmount++;
                        }
                    }

                }
            }


            double redRatio = (double) redPixelAmount / edgeThreshold;
            if (redRatio < minRedRatio){
                return false;
            }

        }

        // draw outline of found sign on original image
        Graphics2D gOriginal = maskedSign.createGraphics();
        gOriginal.setColor(Color.BLUE);
        gOriginal.setStroke(new java.awt.BasicStroke(1));
        //gOriginal.drawPolygon(innerTriangle);
        gOriginal.dispose();

        int totalCenterPixels = 0;
        int whitePixelsInCenter = 0;
        int totalEdgePixels = 0;
        int redPixelsAtEdge = 0;

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

                boolean innerPixel = innerTriangle.contains(x, y);
                if (innerPixel){
                    totalCenterPixels++;
                } else {
                    totalEdgePixels++;
                }

                boolean isHueRed = (h >= 0.0 && h <= 30.0) || (h >= 335.0 && h <= 360.0);
                boolean isRed = isHueRed && (s >= 0.25) && (v >= 0.20);

                boolean isWhite = (s <= 0.20) && (v >= 0.30);

                if (isRed){
                    countRedPixels++;
                    sumXRed += x;
                    sumYRed += y;

                    if (!innerPixel){
                        redPixelsAtEdge++;
                    }
                }

                if (isWhite){
                    countWhitePixels++;
                    sumXWhite += x;
                    sumYWhite += y;

                    if (innerPixel){
                        whitePixelsInCenter++;
                    }
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

        // white pixels in center
        boolean centerIsWhite = ((double) whitePixelsInCenter / totalCenterPixels > 0.50);

        // red pixels at edge
        boolean edgeIsRed = ((double) redPixelsAtEdge / totalEdgePixels > 0.50);

        if (ratioValid &&
                coverageValid &&
                centerIsWhite &&
                centersMatch &&
                edgeIsRed){
            return true;
        }
        return false;
    }

    private static boolean isPixelRedHSV(int rgb) {
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

        boolean isHueRed = (h >= 0.0 && h <= 30.0) || (h >= 335.0 && h <= 360.0);
        return isHueRed && (s >= 0.25) && (v >= 0.20);
    }

}
