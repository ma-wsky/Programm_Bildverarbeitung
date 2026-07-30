package classes.Pipeline;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;

public class FormChecker {

    /**
     * Validates geometry of rectangle by calling {@link FormChecker#detectRectangleForm(ArrayList, int, int)}.
     * For each found geometry: cuts a mask of the sign from originalImage, calls {@link CharacteristicsChecker#isVorfahrtsstrasseColorsAndStats(BufferedImage)}.
     * Draws bounds of sign on original if found.
     * @param maskedWindow BufferedImage original
     * @param validLines ArrayList<HoughLine> valid lines
     * @return boolean if sign found
     */
    public static boolean checkRectangleForm(BufferedImage maskedWindow, ArrayList<HoughLine> validLines, BufferedImage originalImage, int windowX, int windowY) {

        // 1. detect rectangle geometry in validLines
        ArrayList<ArrayList<Point>> allFoundRectangles = FormChecker.detectRectangleForm(validLines, maskedWindow.getWidth(), maskedWindow.getHeight());

        if (!allFoundRectangles.isEmpty()){

            // 2. for each valid rectangle geometry
            for (ArrayList<Point> currentRectangle : allFoundRectangles) {

                // early exits
                if (!PipelineHelper.isValidRectangleCenterColor(maskedWindow, currentRectangle)) continue;
                if (PipelineHelper.isRectangleTooSmall(maskedWindow, currentRectangle)) continue;

                // 3. create mask of rectangle
                BufferedImage rectangleMask = new BufferedImage(maskedWindow.getWidth(), maskedWindow.getHeight(), BufferedImage.TYPE_BYTE_BINARY);
                Graphics2D g = rectangleMask.createGraphics();
                DrawingAndFillingPipeline.drawEdgesAndFill(g, currentRectangle);
                g.dispose();

                // 4. crop and mask sign from original image
                BufferedImage maskedSign = PipelineHelper.cropAndMaskSign(maskedWindow, rectangleMask);
                if (maskedSign == null) continue;

                // 5. check mask for the right colors
                if (CharacteristicsChecker.isVorfahrtsstrasseColorsAndStats(maskedSign)) {

                    // draw outline of found sign on original image
                    Graphics2D gOriginal = originalImage.createGraphics();
                    gOriginal.setColor(Color.GREEN);
                    gOriginal.setStroke(new BasicStroke(4));

                    for (int j = 0; j < 4; j++) {
                        Point pStart = currentRectangle.get(j);
                        Point pEnd = currentRectangle.get((j + 1) % 4);
                        gOriginal.drawLine(pStart.x + windowX, pStart.y + windowY, pEnd.x + windowX, pEnd.y + windowY);
                    }

                    gOriginal.dispose();
                    return true;
                }
            }
        }

        return false;
    }


    /**
     * Validates geometry of triangle by calling {@link FormChecker#detectTriangleForm(ArrayList, int, int)}.
     * For each found geometry: cuts a mask of the sign from originalImage, calls {@link CharacteristicsChecker#isTriangleSignColorAndStats(BufferedImage, ArrayList)}.
     * Draws bounds of sign on original if found.
     * @param maskedWindow BufferedImage original
     * @param validLines ArrayList<HoughLine> valid lines
     * @return boolean if sign found
     */
    public static boolean checkTriangleForm(BufferedImage maskedWindow, ArrayList<HoughLine> validLines, BufferedImage originalImage, int windowX, int windowY) {

        // 1. detect triangle geometry in validLines
        ArrayList<ArrayList<Point>> allFoundTriangles = FormChecker.detectTriangleForm(validLines, maskedWindow.getWidth(), maskedWindow.getHeight());

        if (!allFoundTriangles.isEmpty()){

            // 2. for each valid triangle geometry
            for (ArrayList<Point> currentTriangle : allFoundTriangles) {

                // early exits
                //if (!PipelineHelper.isValidTriangleCenterColor(maskedWindow, currentTriangle)) continue;
                if (PipelineHelper.isTriangleTooSmall(maskedWindow, currentTriangle)) continue;

                // 3. create mask of triangle
                BufferedImage triangleMask = new BufferedImage(maskedWindow.getWidth(), maskedWindow.getHeight(), BufferedImage.TYPE_BYTE_BINARY);
                Graphics2D g = triangleMask.createGraphics();
                DrawingAndFillingPipeline.drawEdgesAndFill(g, currentTriangle);
                g.dispose();

                // 4. crop and mask sign from original image
                BufferedImage maskedSign = PipelineHelper.cropAndMaskSign(maskedWindow, triangleMask);
                if (maskedSign == null) continue;

                // 5. check mask for the right colors
                if (CharacteristicsChecker.isTriangleSignColorAndStats(maskedSign, currentTriangle)) {

                    // draw outline of found sign on original image
                    Graphics2D gOriginal = originalImage.createGraphics();
                    gOriginal.setColor(Color.GREEN);
                    gOriginal.setStroke(new BasicStroke(4));

                    for (int j = 0; j < 3; j++) {
                        Point pStart = currentTriangle.get(j);
                        Point pEnd = currentTriangle.get((j + 1) % 3);
                        gOriginal.drawLine(pStart.x + windowX, pStart.y + windowY, pEnd.x + windowX, pEnd.y + windowY);
                    }

                    gOriginal.dispose();
                    return true;
                }
            }
        }
        return false;
    }


    /**
     * Validates geometry of octagon by calling {@link FormChecker#detectOctagonForm(ArrayList, int, int)}.
     * For each found geometry: cuts a mask of the sign from originalImage, calls {@link CharacteristicsChecker#isStoppColorAndStats(BufferedImage)}.
     * Draws bounds of sign on original if found.
     * @param maskedWindow BufferedImage original
     * @param validLines ArrayList<HoughLine> valid lines
     * @return boolean if sign found
     */
    public static boolean checkOctagonForm(BufferedImage maskedWindow, ArrayList<HoughLine> validLines, BufferedImage originalImage, int windowX, int windowY) {

        // 1. detect octagon geometry in validLines
        ArrayList<ArrayList<Point>> allFoundOctagons = FormChecker.detectOctagonForm(validLines, maskedWindow.getWidth(), maskedWindow.getHeight());

        if (!allFoundOctagons.isEmpty()){

            // 2. for each valid octagon geometry
            for (ArrayList<Point> currentOctagon : allFoundOctagons) {

                // early exits
                //if (!PipelineHelper.isValidOctagonCenterColor(maskedWindow, currentOctagon)) continue;
                if (PipelineHelper.isOctagonTooSmall(maskedWindow, currentOctagon)) continue;

                // 3. create mask of octagon
                BufferedImage octagonMask = new BufferedImage(maskedWindow.getWidth(), maskedWindow.getHeight(), BufferedImage.TYPE_BYTE_BINARY);
                Graphics2D g = octagonMask.createGraphics();
                DrawingAndFillingPipeline.drawEdgesAndFill(g, currentOctagon);
                g.dispose();

                // 4. crop and mask sign from original image
                BufferedImage maskedSign = PipelineHelper.cropAndMaskSign(maskedWindow, octagonMask);
                if (maskedSign == null) continue;

                // 5. check mask for the right colors
                if (CharacteristicsChecker.isStoppColorAndStats(maskedSign)) {

                    // draw outline of found sign on original image
                    Graphics2D gOriginal = originalImage.createGraphics();
                    gOriginal.setColor(Color.GREEN);
                    gOriginal.setStroke(new BasicStroke(4));

                    for (int j = 0; j < 8; j++) {
                        Point pStart = currentOctagon.get(j);
                        Point pEnd = currentOctagon.get((j + 1) % 8);
                        gOriginal.drawLine(pStart.x + windowX, pStart.y + windowY, pEnd.x + windowX, pEnd.y + windowY);
                    }

                    gOriginal.dispose();
                    return true;
                }
            }
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
        int diagonal = (int) Math.ceil(Math.sqrt(Math.pow(height, 2) + Math.pow(width, 2)));
        int minSideLength = 30;
        ArrayList<ArrayList<Point>> allFoundOctagons = new ArrayList<>();

        // 1. sort validOctagonLines into angle-groups
        ArrayList<HoughLine> angle0_180 = new ArrayList<>();
        ArrayList<HoughLine> angle45 = new ArrayList<>();
        ArrayList<HoughLine> angle90 = new ArrayList<>();
        ArrayList<HoughLine> angle135 = new ArrayList<>();

        for (HoughLine line : validOctagonLines) {
            int angle = line.phi;
            int tolerance = 10;

            if (angle <= 45 + tolerance && angle >= 45 - tolerance) {
                angle45.add(line);
            } else if (angle <= 135 + tolerance && angle >= 135 - tolerance) {
                angle135.add(line);
            } else if (angle <= 90 + tolerance && angle >= 90 - tolerance) {
                angle90.add(line);
            } else if (angle <= tolerance || angle >= 180 - tolerance) {
                angle0_180.add(line);
            }
        }

        // sort angle-groups based on distance
        angle0_180.sort(Comparator.comparingDouble(line -> {
            double rad = Math.toRadians(line.phi);
            double cos = Math.cos(rad);
            if (Math.abs(cos) < 0.0001) cos = 0.0001;
            double realR = line.r - diagonal;
            return (realR - (height / 2.0) * Math.sin(rad)) / cos;
        }));
        angle45.sort(Comparator.comparingDouble(line -> {
            double rad = Math.toRadians(line.phi);
            return line.r / (Math.cos(rad) + Math.sin(rad));
        }));
        angle90.sort(Comparator.comparingDouble(line -> {
            double rad = Math.toRadians(line.phi);
            double sin = Math.sin(rad);
            if (Math.abs(sin) < 0.0001) sin = 0.0001;
            return (line.r - (width / 2.0) * Math.cos(rad)) / sin;
        }));
        angle135.sort(Comparator.comparingDouble(line -> {
            double rad = Math.toRadians(line.phi);
            return line.r / (Math.cos(rad) - Math.sin(rad));
        }));

        // 2. check lines of groups for octagon geometry
        int minNumberOfLines = 6;

        for (int i = 0; i < angle0_180.size(); i++) {
            for (int j = angle0_180.size() - 1; j >= i; j--) {

                // pull at least one line from every group
                ArrayList<HoughLine> A = new ArrayList<>();
                A.add(angle0_180.get(i));

                if (i != j) {
                    A.add(angle0_180.get(j));
                }

                for (int k = 0; k < angle45.size(); k++) {
                    for (int l = angle45.size() - 1; l >= k; l--) {

                        ArrayList<HoughLine> B = new ArrayList<>();
                        B.add(angle45.get(k));

                        if (k != l) {
                            B.add(angle45.get(l));
                        }

                        for (int m = 0; m < angle90.size(); m++) {
                            for (int n = angle90.size() - 1; n >= m; n--) {

                                ArrayList<HoughLine> C = new ArrayList<>();
                                C.add(angle90.get(m));

                                if (m != n) {
                                    C.add(angle90.get(n));
                                }

                                for (int o = 0; o < angle135.size(); o++) {
                                    for (int p = angle135.size() - 1; p >= o; p--) {

                                        ArrayList<HoughLine> D = new ArrayList<>();
                                        D.add(angle135.get(o));

                                        if (o != p) {
                                            D.add(angle135.get(p));
                                        }

                                        // early exits
                                        if (A.size() + B.size() + C.size() + D.size() < minNumberOfLines) continue;
                                        if (A.isEmpty() || B.isEmpty() || C.isEmpty() || D.isEmpty()) continue;
                                        if (!(A.size() == 2 || B.size() == 2 || C.size() == 2 || D.size() == 2)) continue;


                                        // 3. calculate sign width
                                        int signWidth = 0;
                                        record GroupDistance(int id, int dist) {}
                                        ArrayList<GroupDistance> distances = new ArrayList<>();

                                        if (A.size() == 2) {
                                            HoughLine l1 = A.get(0);
                                            HoughLine l2 = A.get(1);

                                            int r1 = l1.r;
                                            int r2 = l2.r;

                                            if (l1.phi >= 90) {
                                                r1 = -r1;
                                            }

                                            if (l2.phi >= 90) {
                                                r2 = -r2;
                                            }

                                            distances.add(new GroupDistance(1,Math.abs(r1 - r2)));
                                        } if (B.size() == 2) {
                                            distances.add(new GroupDistance(2, Math.abs(B.get(0).r - B.get(1).r)));
                                        } if (C.size() == 2) {
                                            distances.add(new GroupDistance(3, Math.abs(C.get(0).r - C.get(1).r)));
                                        } if (D.size() == 2) {
                                            distances.add(new GroupDistance(4, Math.abs(D.get(0).r - D.get(1).r)));
                                        }

                                        int minDiff = Integer.MAX_VALUE;
                                        int valid1 = 0;
                                        int valid2 = 0;
                                        for (int r = 0; r < distances.size(); r++){
                                            for (int s = r + 1; s < distances.size(); s++){
                                                int d1 = distances.get(r).dist;
                                                int d2 = distances.get(s).dist;
                                                int diff = Math.abs(d1 - d2);

                                                if (diff < minDiff){
                                                    valid1 = distances.get(r).id;
                                                    valid2 = distances.get(s).id;
                                                    minDiff = diff;
                                                    signWidth = (d1 + d2) / 2;
                                                }
                                            }
                                        }

                                        // 4. clean groups of garbage lines

                                        // approximate center with valid groups
                                        ArrayList<Point> approxCenterIntersections = new ArrayList<>();
                                        if (valid1 == 1 || valid2 == 1){
                                            for (HoughLine line : A) {
                                                for (HoughLine line2 : B) {
                                                    Point intersection = PipelineHelper.getIntersection(line, line2, diagonal);
                                                    if (intersection != null) approxCenterIntersections.add(intersection);
                                                }
                                            }
                                        }

                                        if (valid1 == 2 || valid2 == 2){
                                            for (HoughLine line : B) {
                                                for (HoughLine line2 : C) {
                                                    Point intersection = PipelineHelper.getIntersection(line, line2, diagonal);
                                                    if (intersection != null) approxCenterIntersections.add(intersection);
                                                }
                                            }
                                        }

                                        if (valid1 == 3 || valid2 == 3){
                                            for (HoughLine line : C) {
                                                for (HoughLine line2 : D) {
                                                    Point intersection = PipelineHelper.getIntersection(line, line2, diagonal);
                                                    if (intersection != null) approxCenterIntersections.add(intersection);
                                                }
                                            }
                                        }

                                        if (valid1 == 4 || valid2 == 4){
                                            for (HoughLine line : D) {
                                                for (HoughLine line2 : A) {
                                                    Point intersection = PipelineHelper.getIntersection(line, line2, diagonal);
                                                    if (intersection != null) approxCenterIntersections.add(intersection);
                                                }
                                            }
                                        }

                                        int sumx = 0;
                                        int sumy = 0;
                                        for (Point point : approxCenterIntersections) {
                                            sumx += point.x;
                                            sumy += point.y;
                                        }
                                        Point approxCenter = new Point(sumx / approxCenterIntersections.size(), sumy / approxCenterIntersections.size());

                                        // clean invalid groups of garbage lines
                                        if (valid1 != 1 && valid2 != 1){
                                            // A
                                            int distanceCenter1 = (int) Math.abs((approxCenter.x * Math.cos(Math.toRadians(A.getFirst().phi)) + approxCenter.y * Math.sin(Math.toRadians(A.getFirst().phi)) - (A.getFirst().r - diagonal)));
                                            int distanceCenter2 = (int) Math.abs((approxCenter.x * Math.cos(Math.toRadians(A.getLast().phi)) + approxCenter.y * Math.sin(Math.toRadians(A.getLast().phi)) - (A.getLast().r - diagonal)));

                                            int error1 = Math.abs(distanceCenter1 - (signWidth / 2));
                                            int error2 = Math.abs(distanceCenter2 - (signWidth / 2));

                                            if (error1 < error2){
                                                A.removeLast();
                                            } else {
                                                A.removeFirst();
                                            }
                                        }

                                        if (valid1 != 2 && valid2 != 2){
                                            // B
                                            int distanceCenter1 = (int) Math.abs((approxCenter.x * Math.cos(Math.toRadians(B.getFirst().phi)) + approxCenter.y * Math.sin(Math.toRadians(B.getFirst().phi)) - (B.getFirst().r - diagonal)));
                                            int distanceCenter2 = (int) Math.abs((approxCenter.x * Math.cos(Math.toRadians(B.getLast().phi)) + approxCenter.y * Math.sin(Math.toRadians(B.getLast().phi)) - (B.getLast().r - diagonal)));

                                            int error1 = Math.abs(distanceCenter1 - (signWidth / 2));
                                            int error2 = Math.abs(distanceCenter2 - (signWidth / 2));

                                            if (error1 < error2){
                                                B.removeLast();
                                            } else {
                                                B.removeFirst();
                                            }
                                        }

                                        if (valid1 != 3 && valid2 != 3){
                                            // C
                                            int distanceCenter1 = (int) Math.abs((approxCenter.x * Math.cos(Math.toRadians(C.getFirst().phi)) + approxCenter.y * Math.sin(Math.toRadians(C.getFirst().phi)) - (C.getFirst().r - diagonal)));
                                            int distanceCenter2 = (int) Math.abs((approxCenter.x * Math.cos(Math.toRadians(C.getLast().phi)) + approxCenter.y * Math.sin(Math.toRadians(C.getLast().phi)) - (C.getLast().r - diagonal)));

                                            int error1 = Math.abs(distanceCenter1 - (signWidth / 2));
                                            int error2 = Math.abs(distanceCenter2 - (signWidth / 2));

                                            if (error1 < error2){
                                                C.removeLast();
                                            } else {
                                                C.removeFirst();
                                            }
                                        }

                                        if (valid1 != 4 && valid2 != 4){
                                            // D
                                            int distanceCenter1 = (int) Math.abs((approxCenter.x * Math.cos(Math.toRadians(D.getFirst().phi)) + approxCenter.y * Math.sin(Math.toRadians(D.getFirst().phi)) - (D.getFirst().r - diagonal)));
                                            int distanceCenter2 = (int) Math.abs((approxCenter.x * Math.cos(Math.toRadians(D.getLast().phi)) + approxCenter.y * Math.sin(Math.toRadians(D.getLast().phi)) - (D.getLast().r - diagonal)));

                                            int error1 = Math.abs(distanceCenter1 - (signWidth / 2));
                                            int error2 = Math.abs(distanceCenter2 - (signWidth / 2));

                                            if (error1 < error2){
                                                D.removeLast();
                                            } else {
                                                D.removeFirst();
                                            }
                                        }

                                        // 5. add 2nd lines to groups
                                        if (A.size() == 1) {
                                            HoughLine line = A.getFirst();
                                            int rCenter = (int) (approxCenter.x * Math.cos(Math.toRadians(line.phi)) + approxCenter.y * Math.sin(Math.toRadians(line.phi)) + diagonal);

                                            if (rCenter > line.r) {
                                                A.add(new HoughLine(line.phi, line.r + signWidth, 100));
                                            } else {
                                                A.add(new HoughLine(line.phi, line.r - signWidth, 100));
                                            }
                                        }
                                        if (B.size() == 1) {
                                            HoughLine line = B.getFirst();
                                            int rCenter = (int) (approxCenter.x * Math.cos(Math.toRadians(line.phi)) + approxCenter.y * Math.sin(Math.toRadians(line.phi)) + diagonal);

                                            if (rCenter > line.r) {
                                                B.add(new HoughLine(line.phi, line.r + signWidth, 100));
                                            } else {
                                                B.add(new HoughLine(line.phi, line.r - signWidth, 100));
                                            }
                                        }
                                        if (C.size() == 1) {
                                            HoughLine line = C.getFirst();
                                            int rCenter = (int) (approxCenter.x * Math.cos(Math.toRadians(line.phi)) + approxCenter.y * Math.sin(Math.toRadians(line.phi)) + diagonal);

                                            if (rCenter > line.r) {
                                                C.add(new HoughLine(line.phi, line.r + signWidth, 100));
                                            } else {
                                                C.add(new HoughLine(line.phi, line.r - signWidth, 100));
                                            }
                                        }
                                        if (D.size() == 1) {
                                            HoughLine line = D.getFirst();
                                            int rCenter = (int) (approxCenter.x * Math.cos(Math.toRadians(line.phi)) + approxCenter.y * Math.sin(Math.toRadians(line.phi)) + diagonal);

                                            if (rCenter > line.r) {
                                                D.add(new HoughLine(line.phi, line.r + signWidth, 100));
                                            } else {
                                                D.add(new HoughLine(line.phi, line.r - signWidth, 100));
                                            }
                                        }


                                        // 6. geometry checks

                                        // intersections
                                        ArrayList<Point> intersections = new ArrayList<>();

                                        for (HoughLine line : A) {
                                            for (HoughLine line2 : B) {
                                                Point intersection = PipelineHelper.getIntersection(line, line2, diagonal);
                                                if (intersection != null) intersections.add(intersection);
                                            }
                                        }

                                        for (HoughLine line : B) {
                                            for (HoughLine line2 : C) {
                                                Point intersection = PipelineHelper.getIntersection(line, line2, diagonal);
                                                if (intersection != null) intersections.add(intersection);
                                            }
                                        }

                                        for (HoughLine line : C) {
                                            for (HoughLine line2 : D) {
                                                Point intersection = PipelineHelper.getIntersection(line, line2, diagonal);
                                                if (intersection != null) intersections.add(intersection);
                                            }
                                        }

                                        for (HoughLine line : D) {
                                            for (HoughLine line2 : A) {
                                                Point intersection = PipelineHelper.getIntersection(line, line2, diagonal);
                                                if (intersection != null) intersections.add(intersection);
                                            }
                                        }

                                        // calculate center point
                                        double sumX = 0;
                                        double sumY = 0;
                                        for (Point v :  intersections){
                                            sumX += v.x;
                                            sumY += v.y;
                                        }
                                        double centerX = sumX / intersections.size();
                                        double centerY = sumY / intersections.size();

                                        // find 8 vertices closest to center
                                        if (intersections.size() < 8) continue;
                                        intersections.sort(Comparator.comparingDouble(v -> v.distanceSq(centerX, centerY)));
                                        ArrayList<Point> vertices = new ArrayList<>();
                                        for (int r = 0; r < 8; r++){
                                            vertices.add(intersections.get(r));
                                        }

                                        // check if vertices are inside image with tolerance
                                        boolean isInside = true;
                                        int edgeTolerance = 25;
                                        for (Point v : vertices) {
                                            if (!PipelineHelper.isInsideImage(v, width, height, edgeTolerance)){
                                                isInside = false;
                                                break;
                                            }
                                        }
                                        if (!isInside) continue;

                                        // sort vertices by polar angle
                                        vertices.sort((vert1, vert2) -> {
                                            double angle1 = Math.atan2(vert1.y - centerY, vert1.x - centerX);
                                            double angle2 = Math.atan2(vert2.y - centerY, vert2.x - centerX);
                                            return Double.compare(angle1, angle2);
                                        });

                                        // check side length
                                        double s1 = vertices.get(0).distance(vertices.get(1));
                                        if (s1 < minSideLength) continue;
                                        double s2 = vertices.get(1).distance(vertices.get(2));
                                        if (s2 < minSideLength) continue;
                                        double s3 = vertices.get(2).distance(vertices.get(3));
                                        if (s3 < minSideLength) continue;
                                        double s4 = vertices.get(3).distance(vertices.get(4));
                                        if (s4 < minSideLength) continue;
                                        double s5 = vertices.get(4).distance(vertices.get(5));
                                        if (s5 < minSideLength) continue;
                                        double s6 = vertices.get(5).distance(vertices.get(6));
                                        if (s6 < minSideLength) continue;
                                        double s7 = vertices.get(6).distance(vertices.get(7));
                                        if (s7 < minSideLength) continue;
                                        double s8 = vertices.get(7).distance(vertices.get(0));
                                        if (s8 < minSideLength) continue;

                                        double sideLengthRatioTolerance = 0.6;
                                        double maxSide = Math.max(Math.max(Math.max(s1, s2), Math.max(s3, s4)), Math.max(Math.max(s5, s6), Math.max(s7, s8)));
                                        double minSide = Math.min(Math.min(Math.min(s1, s2), Math.min(s3, s4)), Math.min(Math.min(s5, s6), Math.min(s7, s8)));
                                        double ratio = minSide / maxSide;
                                        if (ratio >= (1 - sideLengthRatioTolerance) && ratio <= (1 + sideLengthRatioTolerance)) {
                                            ArrayList<Point> octagon = new ArrayList<>(vertices);
                                            allFoundOctagons.add(octagon);
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
        int diagonal = (int) Math.ceil(Math.sqrt(Math.pow(height, 2) + Math.pow(width, 2)));
        int minSideLength = 30*30;
        double sideLengthRatioTolerance = 0.2;
        int edgeTolerance = 25;
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
                        ArrayList<HoughLine> group1 = new ArrayList<>();
                        ArrayList<HoughLine> group2 = new ArrayList<>();
                        group1.add(a);

                        HoughLine[] remaining = {b, c, d};
                        int toleranz = 10;

                        for (HoughLine line : remaining) {
                            int diff = PipelineHelper.getAngleOfIntersection(line, a);

                            if (diff <= toleranz) {
                                group1.add(line);
                            } else {
                                group2.add(line);
                            }
                        }

                        if (group1.size() != 2 || group2.size() != 2) continue;

                        HoughLine h1 = group1.get(0);
                        HoughLine h2 = group1.get(1);
                        HoughLine v1 = group2.get(0);
                        HoughLine v2 = group2.get(1);

                        // 4. check intersections
                        Point p1 = PipelineHelper.getIntersection(h1, v1, diagonal);
                        if (p1 == null || !PipelineHelper.isInsideImage(p1, width, height, edgeTolerance)) continue;
                        Point p2 = PipelineHelper.getIntersection(h1, v2, diagonal);
                        if (p2 == null || !PipelineHelper.isInsideImage(p2, width, height, edgeTolerance)) continue;
                        Point p3 = PipelineHelper.getIntersection(h2, v2, diagonal);
                        if (p3 == null || !PipelineHelper.isInsideImage(p3, width, height, edgeTolerance)) continue;
                        Point p4 = PipelineHelper.getIntersection(h2, v1, diagonal);
                        if (p4 == null || !PipelineHelper.isInsideImage(p4, width, height, edgeTolerance)) continue;

                        // 5. check squared side length
                        double dx1 = p1.x - p2.x;
                        double dy1 = p1.y - p2.y;
                        double side1 = dx1 * dx1 + dy1 * dy1;
                        if (side1 < minSideLength) continue;

                        double dx2 = p2.x - p3.x;
                        double dy2 = p2.y - p3.y;
                        double side2 = dx2 * dx2 + dy2 * dy2;
                        if (side2 < minSideLength) continue;

                        double dx3 = p3.x - p4.x;
                        double dy3 = p3.y - p4.y;
                        double side3 = dx3 * dx3 + dy3 * dy3;
                        if (side3 < minSideLength) continue;

                        double dx4 = p4.x - p1.x;
                        double dy4 = p4.y - p1.y;
                        double side4 = dx4 * dx4 + dy4 * dy4;
                        if (side4 < minSideLength) continue;

                        double maxSide = Math.max(Math.max(side1, side2), Math.max(side3, side4));
                        double minSide = Math.min(Math.min(side1, side2), Math.min(side3, side4));
                        double ratio = minSide/maxSide;
                        if (ratio >= (1 - sideLengthRatioTolerance) && ratio <= (1 + sideLengthRatioTolerance)){
                            ArrayList<Point> rectangle = new ArrayList<>();
                            rectangle.add(p1);
                            rectangle.add(p2);
                            rectangle.add(p3);
                            rectangle.add(p4);
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
        int minSideLength = 30*30;
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
                    Point pAB = PipelineHelper.getIntersection(a, b, diagonal);
                    if (pAB == null || !PipelineHelper.isInsideImage(pAB, width, height, insideImageTolerance)) continue;
                    Point pAC = PipelineHelper.getIntersection(a, c, diagonal);
                    if (pAC == null || !PipelineHelper.isInsideImage(pAC, width, height, insideImageTolerance)) continue;
                    Point pBC = PipelineHelper.getIntersection(b, c, diagonal);
                    if (pBC == null || !PipelineHelper.isInsideImage(pBC, width, height, insideImageTolerance)) continue;

                    // 4. check squared side length
                    double dx1 = pAB.x - pBC.x;
                    double dy1 = pAB.y - pBC.y;
                    double side1 = dx1 * dx1 + dy1 * dy1;
                    if (side1 < minSideLength) continue;

                    double dx2 = pBC.x - pAC.x;
                    double dy2 = pBC.y - pAC.y;
                    double side2 = dx2 * dx2 + dy2 * dy2;
                    if (side2 < minSideLength) continue;

                    double dx3 = pAC.x - pAB.x;
                    double dy3 = pAC.y - pAB.y;
                    double side3 = dx3 * dx3 + dy3 * dy3;
                    if (side3 < minSideLength) continue;

                    // check side ratio
                    double maxSide = Math.max(Math.max(side1, side2), side3);
                    double minSide = Math.min(Math.min(side1, side2), side3);
                    double ratio = minSide/maxSide;

                    if (ratio >= (1 - sideRatioTolerance) && ratio <= (1 + sideRatioTolerance)){
                        ArrayList<Point> triangle = new ArrayList<>();
                        triangle.add(pAB);
                        triangle.add(pAC);
                        triangle.add(pBC);
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


}
