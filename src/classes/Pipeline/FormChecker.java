package classes.Pipeline;

import classes.ImageIO;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;

public class FormChecker {

    /**
     * Uses formFlag to differentiate between rectangle (0), triangle (1), and octagon (2).
     * Validates geometry of selected shape by calling {@link FormChecker#detectRectangleForm(ArrayList, int, int)},
     * {@link FormChecker#detectTriangleForm(ArrayList, int, int)} or {@link FormChecker#detectOctagonForm(ArrayList, int, int)}.
     * For each found geometry: cuts a mask of the sign from originalImage, calls {@link CharacteristicsChecker#isVorfahrtColorsAndStats(BufferedImage, ArrayList)},
     * {@link CharacteristicsChecker#isTriangleSignColorAndStats(BufferedImage, ArrayList)},
     * or {@link CharacteristicsChecker#isStoppColorAndStats(BufferedImage)}.
     * Draws bounds of sign on original if found.
     * @param maskedWindow BufferedImage window
     * @param validLines ArrayList<HoughLine>
     * @param originalImage BufferedImage original
     * @param windowX int
     * @param windowY int
     * @param formFlag flag 0 -> rectangle, 1 -> triangle, 2-> octagon
     * @return boolean if sign is found
     */
    public static boolean checkForm(BufferedImage maskedWindow, ArrayList<HoughLine> validLines, BufferedImage originalImage, int windowX, int windowY, int formFlag){

        // 1. detect geometry in validLines
        ArrayList<ArrayList<Point>> allFoundShapes = switch (formFlag) {
            case 0 -> FormChecker.detectRectangleForm(validLines, maskedWindow.getWidth(), maskedWindow.getHeight());
            case 1 -> FormChecker.detectTriangleForm(validLines, maskedWindow.getWidth(), maskedWindow.getHeight());
            case 2 -> FormChecker.detectOctagonForm(validLines, maskedWindow.getWidth(), maskedWindow.getHeight());
            default -> throw new IllegalStateException("Unexpected value: " + formFlag);
        };

        if (allFoundShapes.isEmpty()) return false;

        // 2. for each valid shape geometry
        for (ArrayList<Point> currentShape : allFoundShapes) {

            // early exits
            boolean isValid = switch (formFlag) {
                case 0 -> PipelineHelper.isValidRectangleCenterColor(maskedWindow, currentShape)
                        && !PipelineHelper.isRectangleTooSmall(maskedWindow, currentShape);
                case 1 -> !PipelineHelper.isTriangleTooSmall(maskedWindow, currentShape);
                case 2 -> !PipelineHelper.isOctagonTooSmall(maskedWindow, currentShape);
                default -> false;
            };
            if (!isValid) continue;

            // 3. create mask of shape
            BufferedImage mask = new BufferedImage(maskedWindow.getWidth(), maskedWindow.getHeight(), BufferedImage.TYPE_BYTE_BINARY);
            Graphics2D g = mask.createGraphics();
            DrawingAndFillingPipeline.drawEdgesAndFill(g, currentShape);
            g.dispose();

            // 4. crop and mask sign from original image
            BufferedImage maskedSign = PipelineHelper.cropAndMaskSign(maskedWindow, mask);
            if (maskedSign == null) continue;

            // 5. check mask for the right colors
            boolean colorMatch = switch (formFlag) {
                case 0 -> CharacteristicsChecker.isVorfahrtsstrasseColorsAndStats(maskedSign);
                case 1 -> CharacteristicsChecker.isTriangleSignColorAndStats(maskedSign, currentShape);
                case 2 -> CharacteristicsChecker.isStoppColorAndStats(maskedSign);
                default -> false;
            };
            if (!colorMatch) continue;

            // draw outline of found sign on original image
            Graphics2D gOriginal = originalImage.createGraphics();
            gOriginal.setColor(Color.GREEN);
            gOriginal.setStroke(new BasicStroke(4));

            int numPoints = currentShape.size();
            for (int j = 0; j < numPoints; j++) {
                Point pStart = currentShape.get(j);
                Point pEnd = currentShape.get((j + 1) % numPoints);
                gOriginal.drawLine(pStart.x + windowX, pStart.y + windowY, pEnd.x + windowX, pEnd.y + windowY);
            }
            gOriginal.dispose();

            return true;
        }
        return false;
    }


    /**
     * Function for detecting octagons inside validOctagonLines.
     * Sorts validOctagonLines into angle groups. Sorts the groups based on distance.
     * Takes 6 lines from the groups, two from two groups and 1 from the other two.
     * Determines faulty lines and exchanges them with correct lines that get calculated to form an octagon.
     * Checks intersections, if they are contained in the image, and sidelengths of the 8 lines to determine if it is an octagon.
     * @param validOctagonLines ArrayList<HoughLine>
     * @param width int width of image
     * @param height int height of image
     * @return ArrayList<ArrayList<Point>> all found octagons
     */
    private static ArrayList<ArrayList<Point>> detectOctagonForm(ArrayList<HoughLine> validOctagonLines, int width, int height) {
        int diagonal = (int) Math.ceil(Math.sqrt(height * height + width * width));
        int minSideLength = 30;
        double sideRatioTolerance = 0.6;
        int edgeTolerance = 25;
        int angleTolerance = 10;
        ArrayList<ArrayList<Point>> allFoundOctagons = new ArrayList<>();

        // 1. sort validOctagonLines into angle-groups
        ArrayList<HoughLine> g0 = new ArrayList<>();
        ArrayList<HoughLine> g45 = new ArrayList<>();
        ArrayList<HoughLine> g90 = new ArrayList<>();
        ArrayList<HoughLine> g135 = new ArrayList<>();

        for (HoughLine line : validOctagonLines) {
            int angle = line.phi;

            if (angle <= 45 + angleTolerance && angle >= 45 - angleTolerance) g45.add(line);
            else if (angle <= 135 + angleTolerance && angle >= 135 - angleTolerance) g135.add(line);
            else if (angle <= 90 + angleTolerance && angle >= 90 - angleTolerance) g90.add(line);
            else if (angle <= angleTolerance || angle >= 180 - angleTolerance) g0.add(line);
        }

        if (g0.isEmpty() || g45.isEmpty() || g90.isEmpty() || g135.isEmpty()) return allFoundOctagons;

        // 2. sort angle-groups by position
        FormChecker.sortAngleGroupsByPosition(g0, g45, g90, g135, width, height, diagonal);

        // 2. check lines of groups for octagon geometry
        int minNumberOfLines = 6;

        for (int i = 0; i < g0.size(); i++) {
            for (int j = g0.size() - 1; j >= i; j--) {

                // pull at least one line from every group
                ArrayList<HoughLine> A = new ArrayList<>();
                A.add(g0.get(i));

                if (i != j) {
                    A.add(g0.get(j));
                }

                for (int k = 0; k < g45.size(); k++) {
                    for (int l = g45.size() - 1; l >= k; l--) {

                        ArrayList<HoughLine> B = new ArrayList<>();
                        B.add(g45.get(k));

                        if (k != l) {
                            B.add(g45.get(l));
                        }

                        for (int m = 0; m < g90.size(); m++) {
                            for (int n = g90.size() - 1; n >= m; n--) {

                                ArrayList<HoughLine> C = new ArrayList<>();
                                C.add(g90.get(m));

                                if (m != n) {
                                    C.add(g90.get(n));
                                }

                                for (int o = 0; o < g135.size(); o++) {
                                    for (int p = g135.size() - 1; p >= o; p--) {

                                        ArrayList<HoughLine> D = new ArrayList<>();
                                        D.add(g135.get(o));

                                        if (o != p) {
                                            D.add(g135.get(p));
                                        }

                                        // early exits
                                        if (A.size() + B.size() + C.size() + D.size() < minNumberOfLines) continue;
                                        if (A.isEmpty() || B.isEmpty() || C.isEmpty() || D.isEmpty()) continue;
                                        if (!(A.size() == 2 || B.size() == 2 || C.size() == 2 || D.size() == 2)) continue;


//                                        // 3. calculate sign width
//                                        int signWidth = 0;
//                                        record GroupDistance(int id, int dist) {}
//                                        ArrayList<GroupDistance> distances = new ArrayList<>();
//
//                                        if (A.size() == 2) {
//                                            HoughLine l1 = A.get(0);
//                                            HoughLine l2 = A.get(1);
//
//                                            int r1 = l1.r;
//                                            int r2 = l2.r;
//
//                                            if (l1.phi >= 90) {
//                                                r1 = -r1;
//                                            }
//
//                                            if (l2.phi >= 90) {
//                                                r2 = -r2;
//                                            }
//
//                                            distances.add(new GroupDistance(1,Math.abs(r1 - r2)));
//                                        } if (B.size() == 2) {
//                                            distances.add(new GroupDistance(2, Math.abs(B.get(0).r - B.get(1).r)));
//                                        } if (C.size() == 2) {
//                                            distances.add(new GroupDistance(3, Math.abs(C.get(0).r - C.get(1).r)));
//                                        } if (D.size() == 2) {
//                                            distances.add(new GroupDistance(4, Math.abs(D.get(0).r - D.get(1).r)));
//                                        }
//
//                                        int minDiff = Integer.MAX_VALUE;
//                                        int valid1 = 0;
//                                        int valid2 = 0;
//                                        for (int r = 0; r < distances.size(); r++){
//                                            for (int s = r + 1; s < distances.size(); s++){
//                                                int d1 = distances.get(r).dist;
//                                                int d2 = distances.get(s).dist;
//                                                int diff = Math.abs(d1 - d2);
//
//                                                if (diff < minDiff){
//                                                    valid1 = distances.get(r).id;
//                                                    valid2 = distances.get(s).id;
//                                                    minDiff = diff;
//                                                    signWidth = (d1 + d2) / 2;
//                                                }
//                                            }
//                                        }
//
//                                        // 4. clean groups of garbage lines
//
//                                        // approximate center with valid groups
//                                        ArrayList<Point> approxCenterIntersections = new ArrayList<>();
//                                        if (valid1 == 1 || valid2 == 1){
//                                            for (HoughLine line : A) {
//                                                for (HoughLine line2 : B) {
//                                                    Point intersection = PipelineHelper.getIntersection(line, line2, diagonal);
//                                                    if (intersection != null) approxCenterIntersections.add(intersection);
//                                                }
//                                            }
//                                        }
//
//                                        if (valid1 == 2 || valid2 == 2){
//                                            for (HoughLine line : B) {
//                                                for (HoughLine line2 : C) {
//                                                    Point intersection = PipelineHelper.getIntersection(line, line2, diagonal);
//                                                    if (intersection != null) approxCenterIntersections.add(intersection);
//                                                }
//                                            }
//                                        }
//
//                                        if (valid1 == 3 || valid2 == 3){
//                                            for (HoughLine line : C) {
//                                                for (HoughLine line2 : D) {
//                                                    Point intersection = PipelineHelper.getIntersection(line, line2, diagonal);
//                                                    if (intersection != null) approxCenterIntersections.add(intersection);
//                                                }
//                                            }
//                                        }
//
//                                        if (valid1 == 4 || valid2 == 4){
//                                            for (HoughLine line : D) {
//                                                for (HoughLine line2 : A) {
//                                                    Point intersection = PipelineHelper.getIntersection(line, line2, diagonal);
//                                                    if (intersection != null) approxCenterIntersections.add(intersection);
//                                                }
//                                            }
//                                        }
//
//                                        int sumx = 0;
//                                        int sumy = 0;
//                                        for (Point point : approxCenterIntersections) {
//                                            sumx += point.x;
//                                            sumy += point.y;
//                                        }
//                                        Point approxCenter = new Point(sumx / approxCenterIntersections.size(), sumy / approxCenterIntersections.size());
//
//                                        // clean invalid groups of garbage lines
//                                        if (valid1 != 1 && valid2 != 1){
//                                            // A
//                                            int distanceCenter1 = (int) Math.abs((approxCenter.x * Math.cos(Math.toRadians(A.getFirst().phi)) + approxCenter.y * Math.sin(Math.toRadians(A.getFirst().phi)) - (A.getFirst().r - diagonal)));
//                                            int distanceCenter2 = (int) Math.abs((approxCenter.x * Math.cos(Math.toRadians(A.getLast().phi)) + approxCenter.y * Math.sin(Math.toRadians(A.getLast().phi)) - (A.getLast().r - diagonal)));
//
//                                            int error1 = Math.abs(distanceCenter1 - (signWidth / 2));
//                                            int error2 = Math.abs(distanceCenter2 - (signWidth / 2));
//
//                                            if (error1 < error2){
//                                                A.removeLast();
//                                            } else {
//                                                A.removeFirst();
//                                            }
//                                        }
//
//                                        if (valid1 != 2 && valid2 != 2){
//                                            // B
//                                            int distanceCenter1 = (int) Math.abs((approxCenter.x * Math.cos(Math.toRadians(B.getFirst().phi)) + approxCenter.y * Math.sin(Math.toRadians(B.getFirst().phi)) - (B.getFirst().r - diagonal)));
//                                            int distanceCenter2 = (int) Math.abs((approxCenter.x * Math.cos(Math.toRadians(B.getLast().phi)) + approxCenter.y * Math.sin(Math.toRadians(B.getLast().phi)) - (B.getLast().r - diagonal)));
//
//                                            int error1 = Math.abs(distanceCenter1 - (signWidth / 2));
//                                            int error2 = Math.abs(distanceCenter2 - (signWidth / 2));
//
//                                            if (error1 < error2){
//                                                B.removeLast();
//                                            } else {
//                                                B.removeFirst();
//                                            }
//                                        }
//
//                                        if (valid1 != 3 && valid2 != 3){
//                                            // C
//                                            int distanceCenter1 = (int) Math.abs((approxCenter.x * Math.cos(Math.toRadians(C.getFirst().phi)) + approxCenter.y * Math.sin(Math.toRadians(C.getFirst().phi)) - (C.getFirst().r - diagonal)));
//                                            int distanceCenter2 = (int) Math.abs((approxCenter.x * Math.cos(Math.toRadians(C.getLast().phi)) + approxCenter.y * Math.sin(Math.toRadians(C.getLast().phi)) - (C.getLast().r - diagonal)));
//
//                                            int error1 = Math.abs(distanceCenter1 - (signWidth / 2));
//                                            int error2 = Math.abs(distanceCenter2 - (signWidth / 2));
//
//                                            if (error1 < error2){
//                                                C.removeLast();
//                                            } else {
//                                                C.removeFirst();
//                                            }
//                                        }
//
//                                        if (valid1 != 4 && valid2 != 4){
//                                            // D
//                                            int distanceCenter1 = (int) Math.abs((approxCenter.x * Math.cos(Math.toRadians(D.getFirst().phi)) + approxCenter.y * Math.sin(Math.toRadians(D.getFirst().phi)) - (D.getFirst().r - diagonal)));
//                                            int distanceCenter2 = (int) Math.abs((approxCenter.x * Math.cos(Math.toRadians(D.getLast().phi)) + approxCenter.y * Math.sin(Math.toRadians(D.getLast().phi)) - (D.getLast().r - diagonal)));
//
//                                            int error1 = Math.abs(distanceCenter1 - (signWidth / 2));
//                                            int error2 = Math.abs(distanceCenter2 - (signWidth / 2));
//
//                                            if (error1 < error2){
//                                                D.removeLast();
//                                            } else {
//                                                D.removeFirst();
//                                            }
//                                        }
//
//                                        // 5. add 2nd lines to groups
//                                        if (A.size() == 1) {
//                                            HoughLine line = A.getFirst();
//                                            int rCenter = (int) (approxCenter.x * Math.cos(Math.toRadians(line.phi)) + approxCenter.y * Math.sin(Math.toRadians(line.phi)) + diagonal);
//
//                                            if (rCenter > line.r) {
//                                                A.add(new HoughLine(line.phi, line.r + signWidth, 100));
//                                            } else {
//                                                A.add(new HoughLine(line.phi, line.r - signWidth, 100));
//                                            }
//                                        }
//                                        if (B.size() == 1) {
//                                            HoughLine line = B.getFirst();
//                                            int rCenter = (int) (approxCenter.x * Math.cos(Math.toRadians(line.phi)) + approxCenter.y * Math.sin(Math.toRadians(line.phi)) + diagonal);
//
//                                            if (rCenter > line.r) {
//                                                B.add(new HoughLine(line.phi, line.r + signWidth, 100));
//                                            } else {
//                                                B.add(new HoughLine(line.phi, line.r - signWidth, 100));
//                                            }
//                                        }
//                                        if (C.size() == 1) {
//                                            HoughLine line = C.getFirst();
//                                            int rCenter = (int) (approxCenter.x * Math.cos(Math.toRadians(line.phi)) + approxCenter.y * Math.sin(Math.toRadians(line.phi)) + diagonal);
//
//                                            if (rCenter > line.r) {
//                                                C.add(new HoughLine(line.phi, line.r + signWidth, 100));
//                                            } else {
//                                                C.add(new HoughLine(line.phi, line.r - signWidth, 100));
//                                            }
//                                        }
//                                        if (D.size() == 1) {
//                                            HoughLine line = D.getFirst();
//                                            int rCenter = (int) (approxCenter.x * Math.cos(Math.toRadians(line.phi)) + approxCenter.y * Math.sin(Math.toRadians(line.phi)) + diagonal);
//
//                                            if (rCenter > line.r) {
//                                                D.add(new HoughLine(line.phi, line.r + signWidth, 100));
//                                            } else {
//                                                D.add(new HoughLine(line.phi, line.r - signWidth, 100));
//                                            }
//                                        }
//
//
//                                        // 6. geometry checks
//
//                                        // intersections
//                                        ArrayList<Point> intersections = new ArrayList<>();
//
//                                        for (HoughLine line : A) {
//                                            for (HoughLine line2 : B) {
//                                                Point intersection = PipelineHelper.getIntersection(line, line2, diagonal);
//                                                if (intersection != null) intersections.add(intersection);
//                                            }
//                                        }
//
//                                        for (HoughLine line : B) {
//                                            for (HoughLine line2 : C) {
//                                                Point intersection = PipelineHelper.getIntersection(line, line2, diagonal);
//                                                if (intersection != null) intersections.add(intersection);
//                                            }
//                                        }
//
//                                        for (HoughLine line : C) {
//                                            for (HoughLine line2 : D) {
//                                                Point intersection = PipelineHelper.getIntersection(line, line2, diagonal);
//                                                if (intersection != null) intersections.add(intersection);
//                                            }
//                                        }
//
//                                        for (HoughLine line : D) {
//                                            for (HoughLine line2 : A) {
//                                                Point intersection = PipelineHelper.getIntersection(line, line2, diagonal);
//                                                if (intersection != null) intersections.add(intersection);
//                                            }
//                                        }
//
//                                        // calculate center point
//                                        double sumX = 0;
//                                        double sumY = 0;
//                                        for (Point v :  intersections){
//                                            sumX += v.x;
//                                            sumY += v.y;
//                                        }
//                                        double centerX = sumX / intersections.size();
//                                        double centerY = sumY / intersections.size();
//
//                                        // find 8 vertices closest to center
//                                        if (intersections.size() < 8) continue;
//                                        intersections.sort(Comparator.comparingDouble(v -> v.distanceSq(centerX, centerY)));
//                                        ArrayList<Point> vertices = new ArrayList<>();
//                                        for (int r = 0; r < 8; r++){
//                                            vertices.add(intersections.get(r));
//                                        }
//
//                                        // check if vertices are inside image with tolerance
//                                        boolean isInside = true;
//                                        int edgeTolerance = 25;
//                                        for (Point v : vertices) {
//                                            if (!PipelineHelper.isInsideImage(v, width, height, edgeTolerance)){
//                                                isInside = false;
//                                                break;
//                                            }
//                                        }
//                                        if (!isInside) continue;
//
//                                        // sort vertices by polar angle
//                                        vertices.sort((vert1, vert2) -> {
//                                            double angle1 = Math.atan2(vert1.y - centerY, vert1.x - centerX);
//                                            double angle2 = Math.atan2(vert2.y - centerY, vert2.x - centerX);
//                                            return Double.compare(angle1, angle2);
//                                        });
//
//                                        // check side length
//                                        double s1 = vertices.get(0).distance(vertices.get(1));
//                                        if (s1 < minSideLength) continue;
//                                        double s2 = vertices.get(1).distance(vertices.get(2));
//                                        if (s2 < minSideLength) continue;
//                                        double s3 = vertices.get(2).distance(vertices.get(3));
//                                        if (s3 < minSideLength) continue;
//                                        double s4 = vertices.get(3).distance(vertices.get(4));
//                                        if (s4 < minSideLength) continue;
//                                        double s5 = vertices.get(4).distance(vertices.get(5));
//                                        if (s5 < minSideLength) continue;
//                                        double s6 = vertices.get(5).distance(vertices.get(6));
//                                        if (s6 < minSideLength) continue;
//                                        double s7 = vertices.get(6).distance(vertices.get(7));
//                                        if (s7 < minSideLength) continue;
//                                        double s8 = vertices.get(7).distance(vertices.get(0));
//                                        if (s8 < minSideLength) continue;
//
//                                        double sideLengthRatioTolerance = 0.6;
//                                        double maxSide = Math.max(Math.max(Math.max(s1, s2), Math.max(s3, s4)), Math.max(Math.max(s5, s6), Math.max(s7, s8)));
//                                        double minSide = Math.min(Math.min(Math.min(s1, s2), Math.min(s3, s4)), Math.min(Math.min(s5, s6), Math.min(s7, s8)));
//                                        double ratio = minSide / maxSide;
//                                        if (ratio >= (1 - sideLengthRatioTolerance) && ratio <= (1 + sideLengthRatioTolerance)) {
//                                            ArrayList<Point> octagon = new ArrayList<>(vertices);
//                                            allFoundOctagons.add(octagon);
//                                        }

                                        // 5. calculate sign width
                                        SignWidthResult widthResult = FormChecker.calculateSignWidth(A, B, C, D);
                                        int signWidth = widthResult.width;
                                        if (signWidth <= 0) continue;

                                        // 6. calculate approx center
                                        Point approxCenter = FormChecker.calculateApproxCenter(A, B, C, D, diagonal);
                                        if (approxCenter == null) continue;

                                        //CLEAN
                                        if (widthResult.validGroup1() != 1 && widthResult.validGroup2() != 1) {
                                            cleanGarbageLines(A, approxCenter, signWidth, diagonal);
                                        }
                                        if (widthResult.validGroup1() != 2 && widthResult.validGroup2() != 2) {
                                            cleanGarbageLines(B, approxCenter, signWidth, diagonal);
                                        }
                                        if (widthResult.validGroup1() != 3 && widthResult.validGroup2() != 3) {
                                            cleanGarbageLines(C, approxCenter, signWidth, diagonal);
                                        }
                                        if (widthResult.validGroup1() != 4 && widthResult.validGroup2() != 4) {
                                            cleanGarbageLines(D, approxCenter, signWidth, diagonal);
                                        }

                                        // 7. add second lines to groups where missing
                                        FormChecker.addSecondLines(A, B, C, D, approxCenter, signWidth, diagonal);

                                        // 8. determine octagon
                                        ArrayList<Point> vertices = calculateOctagonPoints(A, B, C, D, diagonal);
                                        if (vertices == null) continue;

                                        // 9. sort vertices by polar angle
                                        FormChecker.sortPointsByPolarAngle(vertices);

                                        // 10. check geometry of octagon
                                        if (FormChecker.arePointsInvalidOrOutsideImage(vertices, width, height, edgeTolerance )) continue;
                                        if (FormChecker.isValidSideLengthAndRatio(vertices, minSideLength, sideRatioTolerance)){
                                            allFoundOctagons.add(vertices);
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
        return allFoundOctagons;
    }


    private static ArrayList<ArrayList<Point>> detectOctagonFormRework(ArrayList<HoughLine> validOctagonLines, int width, int height, BufferedImage maskedWindow){
        int diagonal = (int) Math.ceil(Math.sqrt(height * height + width * width));
        int minSideLength = 30;
        double sideRatioTolerance = 0.6;
        int edgeTolerance = 25;
        int angleTolerance = 10;
        ArrayList<ArrayList<Point>> allFoundOctagons = new ArrayList<>();

        // 1. sort validOctagonLines into angle-groups
        ArrayList<HoughLine> g0 = new ArrayList<>();
        ArrayList<HoughLine> g45 = new ArrayList<>();
        ArrayList<HoughLine> g90 = new ArrayList<>();
        ArrayList<HoughLine> g135 = new ArrayList<>();

        for (HoughLine line : validOctagonLines) {
            int angle = line.phi;

            if (angle <= 45 + angleTolerance && angle >= 45 - angleTolerance) g45.add(line);
            else if (angle <= 135 + angleTolerance && angle >= 135 - angleTolerance) g135.add(line);
            else if (angle <= 90 + angleTolerance && angle >= 90 - angleTolerance) g90.add(line);
            else if (angle <= angleTolerance || angle >= 180 - angleTolerance) g0.add(line);
        }

        if (g0.isEmpty() || g45.isEmpty() || g90.isEmpty() || g135.isEmpty()) return allFoundOctagons;

        // 2. sort angle-groups by position
        FormChecker.sortAngleGroupsByPosition(g0, g45, g90, g135, width, height, diagonal);

        // 3. generate candidate lists
        ArrayList<ArrayList<HoughLine>> candidates0 = FormChecker.generateCandidates(g0);
        ArrayList<ArrayList<HoughLine>> candidates45 = FormChecker.generateCandidates(g45);
        ArrayList<ArrayList<HoughLine>> candidates90 = FormChecker.generateCandidates(g90);
        ArrayList<ArrayList<HoughLine>> candidates135 = FormChecker.generateCandidates(g135);

        // 4. traverse candidate lists
        for (List<HoughLine> C1 : candidates0) {
            for (List<HoughLine> C2 : candidates45) {
                for (List<HoughLine> C3 : candidates90) {
                    for (List<HoughLine> C4 : candidates135) {

                        // early exits
                        int totalLines = C1.size() + C2.size() + C3.size() + C4.size();
                        if (totalLines < 6) continue;
                        if (C1.size() < 2 && C2.size() < 2 && C3.size() < 2 && C4.size() < 2) {
                            continue;
                        }

                        ArrayList<HoughLine> A = new ArrayList<>(C1);
                        ArrayList<HoughLine> B = new ArrayList<>(C2);
                        ArrayList<HoughLine> C = new ArrayList<>(C3);
                        ArrayList<HoughLine> D = new ArrayList<>(C4);

                        //debug
                        // copy for displaying lines
                        BufferedImage lineImage1 = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
                        Graphics2D g21 = lineImage1.createGraphics();
                        g21.setStroke(new java.awt.BasicStroke(1));
                        g21.setColor(Color.RED);
                        DrawingAndFillingPipeline.drawLines(g21, A, width, height);
                        g21.setColor(Color.BLUE);
                        DrawingAndFillingPipeline.drawLines(g21, B, width, height);
                        g21.setColor(Color.GREEN);
                        DrawingAndFillingPipeline.drawLines(g21, C, width, height);
                        g21.setColor(Color.YELLOW);
                        DrawingAndFillingPipeline.drawLines(g21, D, width, height);
                        ImageIO.displayImage(lineImage1);

                        // 5. calculate sign width
                        SignWidthResult widthResult = FormChecker.calculateSignWidth(A, B, C, D);
                        int signWidth = widthResult.width;
                        if (signWidth <= 0) continue;

                        // 6. calculate approx center
                        Point approxCenter = FormChecker.calculateApproxCenter(A, B, C, D, diagonal);
                        if (approxCenter == null) continue;

                        //CLEAN
                        if (widthResult.validGroup1() != 1 && widthResult.validGroup2() != 1) {
                            cleanGarbageLines(A, approxCenter, signWidth, diagonal);
                        }
                        if (widthResult.validGroup1() != 2 && widthResult.validGroup2() != 2) {
                            cleanGarbageLines(B, approxCenter, signWidth, diagonal);
                        }
                        if (widthResult.validGroup1() != 3 && widthResult.validGroup2() != 3) {
                            cleanGarbageLines(C, approxCenter, signWidth, diagonal);
                        }
                        if (widthResult.validGroup1() != 4 && widthResult.validGroup2() != 4) {
                            cleanGarbageLines(D, approxCenter, signWidth, diagonal);
                        }

                        // 7. add second lines to groups where missing
                        FormChecker.addSecondLines(A, B, C, D, approxCenter, signWidth, diagonal);

                        // 8. determine octagon
                        ArrayList<Point> vertices = calculateOctagonPoints(A, B, C, D, diagonal);
                        if (vertices == null) continue;

                        // 9. sort vertices by polar angle
                        FormChecker.sortPointsByPolarAngle(vertices);

                        // 10. check geometry of octagon
                        if (FormChecker.arePointsInvalidOrOutsideImage(vertices, width, height, edgeTolerance )) continue;
                        if (FormChecker.isValidSideLengthAndRatio(vertices, minSideLength, sideRatioTolerance)){
                            allFoundOctagons.add(vertices);
                        }
                    }
                }
            }
        }

        return allFoundOctagons;
    }

    private static void cleanGarbageLines(ArrayList<HoughLine> group, Point approxCenter, int signWidth, int diagonal) {

        int distanceCenter1 = (int) Math.abs((approxCenter.x * Math.cos(Math.toRadians(group.getFirst().phi)) + approxCenter.y * Math.sin(Math.toRadians(group.getFirst().phi)) - (group.getFirst().r - diagonal)));
        int distanceCenter2 = (int) Math.abs((approxCenter.x * Math.cos(Math.toRadians(group.getLast().phi)) + approxCenter.y * Math.sin(Math.toRadians(group.getLast().phi)) - (group.getLast().r - diagonal)));

        int error1 = Math.abs(distanceCenter1 - (signWidth / 2));
        int error2 = Math.abs(distanceCenter2 - (signWidth / 2));

        if (error1 < error2){
            group.removeLast();
        } else {
            group.removeFirst();
        }
    }

    private static void sortPointsByPolarAngle(ArrayList<Point> points) {
        if (points == null || points.isEmpty()) return;

        // calculate center
        double sumX = 0;
        double sumY = 0;
        for (Point p : points) {
            sumX += p.x;
            sumY += p.y;
        }
        double centerX = sumX / points.size();
        double centerY = sumY / points.size();

        // sort by polar angle
        points.sort((p1, p2) -> {
            double angle1 = Math.atan2((p1.y - centerY), p1.x - centerX);
            double angle2 = Math.atan2((p2.y - centerY), p2.x - centerX);
            return Double.compare(angle1, angle2);
        });
    }

    private static ArrayList<Point> calculateOctagonPoints(ArrayList<HoughLine> A, ArrayList<HoughLine> B, ArrayList<HoughLine> C, ArrayList<HoughLine> D, int diagonal) {
        ArrayList<Point> intersections = new ArrayList<>();

        // 1. Alle Schnittpunkte benachbarter Gruppen berechnen
        addIntersectionsBetweenGroups(A, B, diagonal, intersections);
        addIntersectionsBetweenGroups(B, C, diagonal, intersections);
        addIntersectionsBetweenGroups(C, D, diagonal, intersections);
        addIntersectionsBetweenGroups(D, A, diagonal, intersections);

        // Es müssen mindestens 8 Schnittpunkte vorhanden sein
        if (intersections.size() < 8) {
            return null;
        }

        // 2. Zentrum aller berechneten Schnittpunkte bestimmen
        double sumX = 0;
        double sumY = 0;
        for (Point p : intersections) {
            sumX += p.x;
            sumY += p.y;
        }
        double centerX = sumX / intersections.size();
        double centerY = sumY / intersections.size();

        // 3. Schnittpunkte nach quadratischer Distanz zum Zentrum sortieren
        intersections.sort(Comparator.comparingDouble(p -> p.distanceSq(centerX, centerY)));

        // 4. Die 8 am nächsten liegenden Punkte auswählen
        ArrayList<Point> vertices = new ArrayList<>();
        for (int i = 0; i < 8; i++) {
            vertices.add(intersections.get(i));
        }

        return vertices;
    }

    /**
     * Hilfsmethode zur Berechnung aller Schnittpunkte zwischen zwei Liniengruppen.
     */
    private static void addIntersectionsBetweenGroups(ArrayList<HoughLine> group1, ArrayList<HoughLine> group2, int diagonal, ArrayList<Point> outIntersections) {
        for (HoughLine line1 : group1) {
            for (HoughLine line2 : group2) {
                Point p = PipelineHelper.getIntersection(line1, line2, diagonal);
                if (p != null) {
                    outIntersections.add(p);
                }
            }
        }
    }


    private static void addSecondLines(ArrayList<HoughLine> A, ArrayList<HoughLine> B, ArrayList<HoughLine> C, ArrayList<HoughLine> D, Point approxCenter, int signWidth, int diagonal){
        FormChecker.completeSingleGroup(A, approxCenter, signWidth, diagonal);
        FormChecker.completeSingleGroup(B, approxCenter, signWidth, diagonal);
        FormChecker.completeSingleGroup(C, approxCenter, signWidth, diagonal);
        FormChecker.completeSingleGroup(D, approxCenter, signWidth, diagonal);
    }

    private static void completeSingleGroup(ArrayList<HoughLine> group, Point approxCenter, int signWidth, int diagonal){
        if (group.size() == 1) {
            HoughLine line = group.getFirst();
            int rCenter = (int) (approxCenter.x * Math.cos(Math.toRadians(line.phi))
                    + approxCenter.y * Math.sin(Math.toRadians(line.phi))
                    + diagonal);

            if (rCenter > line.r) {
                group.add(new HoughLine(line.phi, line.r + signWidth, 100));
            } else {
                group.add(0, new HoughLine(line.phi, line.r - signWidth, 100));
            }
        }
    }

    // Ergebnis-Klasse für die Breitenberechnung
    private record SignWidthResult(int width, int validGroup1, int validGroup2) {}

    // Hilfs-Record zur Zuordnung von Gruppe und Distanz
    private record GroupDistance(int groupId, int dist) {}

    private static SignWidthResult calculateSignWidth(ArrayList<HoughLine> A, ArrayList<HoughLine> B, ArrayList<HoughLine> C, ArrayList<HoughLine> D){
        ArrayList<GroupDistance> distances = new ArrayList<>();

        if (A.size() == 2){
            int r1 = A.get(0).phi >= 90 ? -A.get(0).r : A.get(0).r;
            int r2 = A.get(1).phi >= 90 ? -A.get(1).r : A.get(1).r;
            distances.add(new GroupDistance(1, Math.abs(r1 - r2)));
        }
        if (B.size() == 2) distances.add(new GroupDistance(2, Math.abs(B.get(0).r - B.get(1).r)));
        if (C.size() == 2) distances.add(new GroupDistance(3, Math.abs(C.get(0).r - C.get(1).r)));
        if (D.size() == 2) distances.add(new GroupDistance(4, Math.abs(D.get(0).r - D.get(1).r)));

        if (distances.size() < 2) return new SignWidthResult(0, 0, 0);

        int minDiff = Integer.MAX_VALUE;
        int signWidth = 0;
        int valid1 = 0;
        int valid2 = 0;

        for (int i = 0; i < distances.size(); i++){
            for (int j = i + 1; j < distances.size(); j++){
                int d1 = distances.get(i).dist;
                int d2 = distances.get(j).dist;
                int diff = Math.abs(d1 - d2);

                if (diff < minDiff){
                    minDiff = diff;
                    signWidth = (d1 + d2) / 2;
                    valid1 = distances.get(i).groupId();
                    valid2 = distances.get(j).groupId();
                }
            }
        }
        return new SignWidthResult(signWidth, valid1, valid2);
    }

    private static Point calculateApproxCenter(ArrayList<HoughLine> A, ArrayList<HoughLine> B, ArrayList<HoughLine> C, ArrayList<HoughLine> D, int diagonal){
        ArrayList<Point> intersections = new ArrayList<>();

        if (A.size() == 2 || B.size() == 2){
            for (HoughLine line1 : A) {
                for (HoughLine line2 : B) {
                    Point intersection = PipelineHelper.getIntersection(line1, line2, diagonal);
                    if (intersection != null) intersections.add(intersection);
                }
            }
        }
        if (B.size() == 2 || C.size() == 2){
            for (HoughLine line1 : B) {
                for (HoughLine line2 : C) {
                    Point intersection = PipelineHelper.getIntersection(line1, line2, diagonal);
                    if (intersection != null) intersections.add(intersection);
                }
            }
        }
        if (C.size() == 2 || D.size() == 2){
            for (HoughLine line1 : C) {
                for (HoughLine line2 : D) {
                    Point intersection = PipelineHelper.getIntersection(line1, line2, diagonal);
                    if (intersection != null) intersections.add(intersection);
                }
            }
        }
        if (D.size() == 2 || A.size() == 2){
            for (HoughLine line1 : D) {
                for (HoughLine line2 : A) {
                    Point intersection = PipelineHelper.getIntersection(line1, line2, diagonal);
                    if (intersection != null) intersections.add(intersection);
                }
            }
        }

        if (intersections.isEmpty()) return null;

        int sumX = 0;
        int sumY = 0;
        for (Point p : intersections){
            sumX += p.x;
            sumY += p.y;
        }

        return new Point(sumX / intersections.size(), sumY / intersections.size());
    }


    private static void sortAngleGroupsByPosition(ArrayList<HoughLine> g0, ArrayList<HoughLine> g45, ArrayList<HoughLine> g90, ArrayList<HoughLine> g135, int width, int height, int diagonal){

        // sort by x-position
        g0.sort(Comparator.comparingDouble(line -> {
            double rad = Math.toRadians(line.phi);
            double cos = Math.cos(rad);
            if (Math.abs(cos) < 0.0001) cos = 0.0001;
            double realR = line.r - diagonal;
            return (realR - (height / 2.0) * Math.sin(rad)) / cos;
        }));

        // sort by Achsenabschnitt
        g45.sort(Comparator.comparingDouble(line -> {
            double rad = Math.toRadians(line.phi);
            double denom = Math.cos(rad) + Math.sin(rad);
            if (Math.abs(denom) < 0.0001) denom = 0.0001;
            return line.r / denom;
        }));

        // sort by y-position
        g90.sort(Comparator.comparingDouble(line -> {
            double rad = Math.toRadians(line.phi);
            double sin = Math.sin(rad);
            if (Math.abs(sin) < 0.0001) sin = 0.0001;
            return (line.r - (width / 2.0) * Math.cos(rad)) / sin;
        }));

        // sort by Achsenabschnitt
        g135.sort(Comparator.comparingDouble(line -> {
            double rad = Math.toRadians(line.phi);
            double denom = Math.cos(rad) - Math.sin(rad);
            if (Math.abs(denom) < 0.0001) denom = 0.0001;
            return line.r / denom;
        }));
    }

    private static ArrayList<ArrayList<HoughLine>> generateCandidates(ArrayList<HoughLine> group){
        ArrayList<ArrayList<HoughLine>> result = new ArrayList<>();

        for (HoughLine houghLine : group) {
            ArrayList<HoughLine> list = new ArrayList<>();
            list.add(houghLine);
            result.add(list);
        }

        for (int i = 0; i < group.size(); i++){
            for (int j = group.size() - 1; j > i; j--) {
                ArrayList<HoughLine> list = new ArrayList<>();
                list.add(group.get(i));
                list.add(group.get(j));
                result.add(list);
            }
        }
        return result;
    }


    /**
     * Function for validating that validRectangleLines construct a rectangle.
     * Sorts candidates into parallel groups and checks for 2 members each.
     * Checks intersections of candidates for validating intersection points (members of different groups must intersect).
     * Checks if intersection points are inside the image with tolerance.
     * Checks ratio of shortest and longest sidelengths with tolerance.
     * @param validRectangleLines ArrayList<HoughLines>
     * @param width int width of image
     * @param height int height of image
     * @return ArrayList<ArrayList<Point>> all found rectangles
     */
    private static ArrayList<ArrayList<Point>> detectRectangleForm(ArrayList<HoughLine> validRectangleLines, int width, int height) {
        int size = validRectangleLines.size();
        int diagonal = (int) Math.ceil(Math.sqrt(height * height + width * width));
        int minSideLength = 30;
        double sideRatioTolerance = 0.2;
        int edgeTolerance = 25;
        int angleTolerance = 10;
        ArrayList<ArrayList<Point>> allFoundRectangles = new ArrayList<>();

        // 1. take four lines from validRectangleLines
        for (int i = 0; i < size; i++) {
            for (int j = i + 1; j < size; j++) {
                for (int k = j + 1; k < size; k++) {
                    for (int l = k + 1; l < size; l++) {

                        HoughLine a = validRectangleLines.get(i);
                        HoughLine b = validRectangleLines.get(j);
                        HoughLine c = validRectangleLines.get(k);
                        HoughLine d = validRectangleLines.get(l);

                        // 2. check angles
                        if (!FormChecker.isRectangleAngles(a, b, c, d)) continue;

                        // 3. sort into parallel groups
                        HoughLine[] sides = FormChecker.sortIntoParallelGroups(a, b, c, d, angleTolerance);
                        if (sides == null) continue;
                        HoughLine h1 = sides[0];
                        HoughLine h2 = sides[1];
                        HoughLine v1 = sides[2];
                        HoughLine v2 = sides[3];

                        // 4. check intersections
                        ArrayList<Point> rectangle = new ArrayList<>(Arrays.asList(
                                PipelineHelper.getIntersection(h1, v1, diagonal),
                                PipelineHelper.getIntersection(h1, v2, diagonal),
                                PipelineHelper.getIntersection(h2, v2, diagonal),
                                PipelineHelper.getIntersection(h2, v1, diagonal)
                        ));
                        if (FormChecker.arePointsInvalidOrOutsideImage(rectangle, width, height, edgeTolerance)) continue;

                        // 5. check squared side length and side ratio
                        if (FormChecker.isValidSideLengthAndRatio(rectangle, minSideLength, sideRatioTolerance)){
                            allFoundRectangles.add(rectangle);
                        }
                    }
                }
            }
        }
        return allFoundRectangles;
    }

    /**
     * Function for validating that validTriangleLines construct a triangle.
     * Checks intersections of candidates for validating intersection points.
     * Checks if intersection points are inside the image with tolerance.
     * Checks ratio of shortest and longest sidelengths with tolerance.
     * @param validTriangleLines ArrayList<HoughLines>
     * @param width int width of image
     * @param height int height of image
     * @return ArrayList<ArrayList<Point>> all found triangles
     */
    private static ArrayList<ArrayList<Point>> detectTriangleForm(ArrayList<HoughLine> validTriangleLines, int width, int height) {
        int size = validTriangleLines.size();
        int diagonal = (int) Math.ceil(Math.sqrt(height * height + width * width));
        int minSideLength = 30;
        int insideImageTolerance = 15;
        double sideRatioTolerance = 0.3;
        ArrayList<ArrayList<Point>> allFoundTriangles = new ArrayList<>();

        // 1. take three lines from validTriangleLines
        for (int i = 0; i < size; i++){
            for (int j = i + 1; j < size; j++){
                for (int k = j + 1; k < size; k++){

                    HoughLine a = validTriangleLines.get(i);
                    HoughLine b = validTriangleLines.get(j);
                    HoughLine c = validTriangleLines.get(k);

                    // 2. check angles
                    if (!FormChecker.isTriangleAngles(a, b, c)) continue;

                    // 3. check intersections
                    ArrayList<Point> triangle = new ArrayList<>(Arrays.asList(
                            PipelineHelper.getIntersection(a, b, diagonal),
                            PipelineHelper.getIntersection(a, c, diagonal),
                            PipelineHelper.getIntersection(b, c, diagonal)
                    ));
                    if (FormChecker.arePointsInvalidOrOutsideImage(triangle, width, height, insideImageTolerance)) continue;

                    // 5. check squared side length and side ratio
                    if (FormChecker.isValidSideLengthAndRatio(triangle, minSideLength, sideRatioTolerance)){
                        allFoundTriangles.add(triangle);
                    }
                }
            }
        }
        return allFoundTriangles;
    }


    /**
     * Checks if four HoughLines form a rectangle based on angles with tolerance.
     * @param a HoughLine
     * @param b HoughLine
     * @param c HoughLine
     * @param d HoughLine
     * @return boolean if lines form a rectangle
     */
    private static boolean isRectangleAngles(HoughLine a, HoughLine b, HoughLine c, HoughLine d) {
        int[] phi = {a.phi, b.phi, c.phi, d.phi};
        Arrays.sort(phi);

        int parallelTolerance = 8;
        int orthogonalTolerance = 10;

        int diffParallel1 = phi[1] - phi[0];
        int diffParallel2 = phi[3] - phi[2];

        int diffOrthogonal = phi[2] - phi[1];

        return diffParallel1 <= parallelTolerance &&
               diffParallel2 <= parallelTolerance &&
               Math.abs(diffOrthogonal - 90) <= orthogonalTolerance;
    }

    /**
     * Checks lines a, b, c for an angle difference of 60° with tolerance.
     * @param a HoughLine
     * @param b HoughLine
     * @param c HoughLine
     * @return boolean if angles match 60°
     */
    private static boolean isTriangleAngles(HoughLine a, HoughLine b, HoughLine c) {
        int[] phi = {a.phi, b.phi, c.phi};
        Arrays.sort(phi);

        int tolerance = 30;

        int diff1 = phi[1] - phi[0];
        int diff2 = phi[2] - phi[1];
        int diff3 = 180 - (phi[2] - phi[0]);

        return Math.abs(diff1 - 60) <= tolerance &&
               Math.abs(diff2 - 60) <= tolerance &&
               Math.abs(diff3 - 60) <= tolerance;
    }

    /**
     * Calculates sides from vertices array and checks lengths against minSideLength.
     * Checks ratio of shortest and longest sides against sideRatioTolerance.
     * @param vertices ArrayList<Point>
     * @param minSideLength int
     * @param sideRatioTolerance int
     * @return boolean is sides and ratio are valid
     */
    private static boolean isValidSideLengthAndRatio(ArrayList<Point> vertices, int minSideLength, double sideRatioTolerance){
        int n = vertices.size();
        double minSide = Double.MAX_VALUE;
        double maxSide = Double.MIN_VALUE;

        for (int i = 0; i < n; i++){
            Point v1 = vertices.get(i);
            Point v2 = vertices.get((i + 1) % n);

            double dx = v1.x - v2.x;
            double dy = v1.y - v2.y;
            double sideLength = dx * dx + dy * dy;

            if (sideLength < minSideLength * minSideLength) return false;

            if (sideLength < minSide) minSide = sideLength;
            if (sideLength > maxSide) maxSide = sideLength;
        }

        // check ratio
        double ratio = minSide / maxSide;
        return ratio >= (1.0 - sideRatioTolerance) && ratio <= (1.0 + sideRatioTolerance);
    }

    /**
     * Checks if points from vertices array are invalid (== null) or outside of image using {@link PipelineHelper#isInsideImage(Point, int, int, int)}.
     * @param vertices ArrayList<Point>
     * @param width int
     * @param height int
     * @param tolerance int
     * @return boolean if points are invalid or outside image
     */
    private static boolean arePointsInvalidOrOutsideImage(ArrayList<Point> vertices, int width, int height, int tolerance){
        for (Point v : vertices){
            if (v == null || !PipelineHelper.isInsideImage(v, width, height, tolerance)){
                return true;
            }
        }
        return false;
    }

    /**
     * Sorts the given HoughLines into parallel groups. Checks if each group has 2 members.
     * @param a HoughLines
     * @param b HoughLines
     * @param c HoughLines
     * @param d HoughLines
     * @param angleTolerance int
     * @return HoughLines[] sorted: horizontal, horizontal, vertical, vertical
     */
    private static HoughLine[] sortIntoParallelGroups(HoughLine a, HoughLine b, HoughLine c, HoughLine d, int angleTolerance) {
        ArrayList<HoughLine> group1 = new ArrayList<>();
        ArrayList<HoughLine> group2 = new ArrayList<>();
        group1.add(a);

        HoughLine[] remaining = {b, c, d};

        for (HoughLine line : remaining) {
            if (PipelineHelper.getAngleOfIntersection(line, a) <= angleTolerance) {
                group1.add(line);
            } else {
                group2.add(line);
            }
        }

        if (group1.size() != 2 || group2.size() != 2) {
            return null;
        }

        return new HoughLine[] {group1.get(0), group1.get(1), group2.get(0), group2.get(1)};
    }


}
