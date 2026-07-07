package classes.Pipeline;

import classes.ImageIO;
import classes.RotatedImage;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;

public class FormChecker {

    /**
     * Validates lines of rectangle based on intersection angles of pairs.
     * Validates form of rectangle by calling {@link FormChecker#detectRectangleForm(ArrayList, int, int)}.
     * Calls {@link CharacteristicsChecker#isVorfahrtsstrasseColorsAndStats(BufferedImage)}.
     * Draws bounds of sign on original if found.
     * @param originalImage BufferedImage original
     * @param validLines ArrayList<HoughLine> valid lines
     */
    public static void checkRectangleForm(BufferedImage originalImage, ArrayList<HoughLine> validLines) {
        // rectangle check
        ArrayList<HoughLine> validRectangleLines = getLines(validLines, 90);

        // copy for displaying lines
        BufferedImage lineImage = new BufferedImage(originalImage.getWidth(), originalImage.getHeight(), originalImage.getType());
        Graphics2D g2 = lineImage.createGraphics();
        g2.setColor(Color.BLUE);
        g2.setStroke(new java.awt.BasicStroke(1));
        DrawingAndFillingPipeline.drawLines(g2, validRectangleLines, originalImage.getWidth(), originalImage.getHeight());
        ImageIO.displayImage(lineImage);

        // check all found rectangles
        System.out.println("checking rectangle form...");

        ArrayList<ArrayList<Point>> allFoundRectangles = FormChecker.detectRectangleForm(validRectangleLines, originalImage.getWidth(), originalImage.getHeight());

        if (!allFoundRectangles.isEmpty()){
            System.out.println(allFoundRectangles.size() + " rectangles found...");

            for (int i = 0; i < allFoundRectangles.size(); i++){

                ArrayList<Point> currentRectangle = allFoundRectangles.get(i);
                System.out.println("checking rectangle " + (i+1) + "...");

                // check center for early exit
                if (!PipelineHelper.isValidRectangleCenterColor(originalImage, currentRectangle)) continue;

                // create mask of rectangle
                BufferedImage rectangleMask = new BufferedImage(originalImage.getWidth(), originalImage.getHeight(), BufferedImage.TYPE_BYTE_BINARY);
                Graphics2D g = rectangleMask.createGraphics();
                DrawingAndFillingPipeline.drawEdgesAndFill(g, allFoundRectangles.get(i));

                // crop and mask sign from original image
                BufferedImage maskedSign = PipelineHelper.cropAndMaskSign(originalImage, rectangleMask);
                if (maskedSign == null) continue;
                classes.ImageIO.displayImage(maskedSign);

                // check the mask for the right colors
                if (CharacteristicsChecker.isVorfahrtsstrasseColorsAndStats(maskedSign)){

                    // valid sign found
                    System.err.println("valid vorfahrtstraße-sign found!");

                    // draw outline of found sign on original image
                    Graphics2D gOriginal = originalImage.createGraphics();
                    gOriginal.setColor(Color.GREEN);
                    gOriginal.setStroke(new java.awt.BasicStroke(4));

                    for (int j = 0; j < 4; j++) {
                        Point pStart = currentRectangle.get(j);
                        Point pEnd = currentRectangle.get((j + 1) % 4);
                        gOriginal.drawLine(pStart.x, pStart.y, pEnd.x, pEnd.y);
                    }

                    gOriginal.dispose();

                    ImageIO.displayImage(originalImage);
                    break;
                }

                g.dispose();
            }
        } else {
            System.out.println("no rectangle detected!");
        }
        g2.dispose();
    }

    /**
     * Accumulates all lines in validLines that have an intersection angle of angle.
     * Uses {@link PipelineHelper#getAngleOfIntersection(HoughLine, HoughLine)}.
     * @param validLines ArrayList<HoughLine> valid lines
     * @param angle int angle
     * @return ArrayList<HoughLine> valid lines of angle
     */
    private static ArrayList<HoughLine> getLines(ArrayList<HoughLine> validLines, int angle) {
        int tolerance = 15;
        ArrayList<HoughLine> validRectangleLines = new ArrayList<>();

        for (int i = 0; i < validLines.size(); i++){
            for (int j = 0; j < validLines.size(); j++){
                if (i == j) continue;
                HoughLine line1 = validLines.get(i);
                HoughLine line2 = validLines.get(j);

                int angleOfIntersection = PipelineHelper.getAngleOfIntersection(line1, line2);

                if (angleOfIntersection >= (angle - tolerance) && angleOfIntersection <= (angle + tolerance)) {
                    if (!validRectangleLines.contains(line1)) validRectangleLines.add(line1);
                    if (!validRectangleLines.contains(line2)) validRectangleLines.add(line2);
                }
            }
        }
        return validRectangleLines;
    }

    /**
     * Validates lines of triangle based on intersection angles of pairs.
     * Validates form of triangle by calling {@link FormChecker#detectTriangleForm(ArrayList, int, int)}.
     * Calls {@link CharacteristicsChecker#isVorfahrtAchtenColorsAndStats(BufferedImage)} and {@link CharacteristicsChecker#isVorfahrtColorsAndStats(BufferedImage)}.
     * Draws bounds of sign on original if found.
     * @param originalImage BufferedImage original
     * @param validLines ArrayList<HoughLine> valid lines
     */
    public static void checkTriangleForm(BufferedImage originalImage, ArrayList<HoughLine> validLines) {
        // triangle check
        ArrayList<HoughLine> validTriangleLines = FormChecker.getLines(validLines, 60);

        // copy for displaying lines
        BufferedImage lineImage = new BufferedImage(originalImage.getWidth(), originalImage.getHeight(), originalImage.getType());
        Graphics2D g2 = lineImage.createGraphics();
        g2.setColor(Color.RED);
        g2.setStroke(new java.awt.BasicStroke(1));
        DrawingAndFillingPipeline.drawLines(g2, validTriangleLines, originalImage.getWidth(), originalImage.getHeight());
        ImageIO.displayImage(lineImage);

        // check all found triangles
        System.out.println("checking triangle form...");

        ArrayList<ArrayList<Point>> allFoundTriangles = FormChecker.detectTriangleForm(validTriangleLines, originalImage.getWidth(), originalImage.getHeight());

        if (!allFoundTriangles.isEmpty()){
            System.out.println(allFoundTriangles.size() + " triangles found...");

            for (int i = 0; i < allFoundTriangles.size(); i++){

                ArrayList<Point> currentTriangle = allFoundTriangles.get(i);
                System.out.println("checking triangle " + (i+1) + "...");

                // check center for early exit
                if (!PipelineHelper.isValidTriangleCenterColor(originalImage, currentTriangle)) continue;

                // create mask of triangle
                BufferedImage triangleMask = new BufferedImage(originalImage.getWidth(), originalImage.getHeight(), BufferedImage.TYPE_BYTE_BINARY);
                Graphics2D g = triangleMask.createGraphics();
                DrawingAndFillingPipeline.drawEdgesAndFill(g, currentTriangle);

                // crop and mask sign from original image
                BufferedImage maskedSign = PipelineHelper.cropAndMaskSign(originalImage, triangleMask);
                if (maskedSign == null) continue;
                ImageIO.displayImage(maskedSign);

                // check the mask for the right colors
                String foundSign = "";
                if (CharacteristicsChecker.isVorfahrtAchtenColorsAndStats(maskedSign)){
                    foundSign = "vorfahrt-achten";
                } else if (CharacteristicsChecker.isVorfahrtColorsAndStats(maskedSign)){
                    foundSign = "vorfahrt";
                }

                if (!foundSign.isEmpty()){

                    // valid sign found
                    System.err.println("valid " + foundSign + "-sign found!");

                    // draw outline of found sign on original image
                    Graphics2D gOriginal = originalImage.createGraphics();
                    gOriginal.setColor(Color.GREEN);
                    gOriginal.setStroke(new java.awt.BasicStroke(4));

                    for (int j = 0; j < 3; j++) {
                        Point pStart = currentTriangle.get(j);
                        Point pEnd = currentTriangle.get((j + 1) % 3);
                        gOriginal.drawLine(pStart.x, pStart.y, pEnd.x, pEnd.y);
                    }

                    gOriginal.dispose();

                    ImageIO.displayImage(originalImage);
                    break;
                }

                g.dispose();
            }
        } else {
            System.out.println("no triangle detected!");
        }
        g2.dispose();
    }


    /**
     * Validates lines of octagon based on intersection angles of pairs.
     * Validates form of octagon by calling {@link FormChecker#detectOctagonForm(ArrayList, int, int)}.
     * Calls {@link CharacteristicsChecker#isStoppColorAndStats(BufferedImage)}.
     * Draws bounds of sign on original if found.
     * @param originalImage BufferedImage original
     * @param validLines ArrayList<HoughLine> valid lines
     */
    public static void checkOctagonForm(BufferedImage originalImage, ArrayList<HoughLine> validLines) {
        // octagon check
        ArrayList<HoughLine> validOctagonLines = FormChecker.getLines(validLines, 45);
        ArrayList<HoughLine> lines90 = FormChecker.getLines(validLines, 90);

        // link with no duplicates
        for (HoughLine line : lines90) {
            if (!validOctagonLines.contains(line)) {
                validOctagonLines.add(line);
            }
        }

        // copy for displaying lines
        BufferedImage lineImage = new BufferedImage(originalImage.getWidth(), originalImage.getHeight(), originalImage.getType());
        Graphics2D g2 = lineImage.createGraphics();
        g2.setColor(Color.GREEN);
        g2.setStroke(new java.awt.BasicStroke(1));
        DrawingAndFillingPipeline.drawLines(g2, validOctagonLines, originalImage.getWidth(), originalImage.getHeight());
        ImageIO.displayImage(lineImage);

        // check all found octagons
        System.out.println("checking octagon form...");

        ArrayList<ArrayList<Point>> allFoundOctagons = FormChecker.detectOctagonForm(validOctagonLines, originalImage.getWidth(), originalImage.getHeight());

        if (!allFoundOctagons.isEmpty()){
            System.out.println(allFoundOctagons.size() + " octagons found...");

            for (int i = 0; i < allFoundOctagons.size(); i++){

                ArrayList<Point> currentOctagon = allFoundOctagons.get(i);
                System.out.println("checking octagon " + (i+1) + "...");

                //check center for early exit
                if (!PipelineHelper.isValidOctagonCenterColor(originalImage, currentOctagon)) continue;

                // create mask of octagon
                BufferedImage octagonMask = new BufferedImage(originalImage.getWidth(), originalImage.getHeight(), BufferedImage.TYPE_BYTE_BINARY);
                Graphics2D g = octagonMask.createGraphics();
                DrawingAndFillingPipeline.drawEdgesAndFill(g, allFoundOctagons.get(i));

                // crop and mask sign from original image
                BufferedImage maskedSign = PipelineHelper.cropAndMaskSign(originalImage, octagonMask);
                ImageIO.displayImage(maskedSign);

                // check the mask for the right colors
                if (CharacteristicsChecker.isStoppColorAndStats(maskedSign)){

                    // valid sign
                    System.out.println("valid stopp-sign found!");

                    // draw outline of found sign on original image
                    Graphics2D gOriginal = originalImage.createGraphics();
                    gOriginal.setColor(Color.GREEN);
                    gOriginal.setStroke(new java.awt.BasicStroke(4));

                    for (int j = 0; j < 8; j++) {
                        Point pStart = currentOctagon.get(j);
                        Point pEnd = currentOctagon.get((j + 1) % 8);
                        gOriginal.drawLine(pStart.x, pStart.y, pEnd.x, pEnd.y);
                    }

                    gOriginal.dispose();

                    ImageIO.displayImage(originalImage);
                    break;
                }

                g.dispose();
            }
        } else {
            System.out.println("no octagon detected!");
        }
        g2.dispose();
    }

    /**
     * Function for validating that validOctagonLines construct an octagon.
     * Sorts candidates into parallel groups and checks for 2 members each.
     * Groups are sorted by angle to ensure correct cyclical intersection calculation.
     * Checks cyclical intersections of adjacent groups to find all 16 potential vertices.
     * Filters out invalid or distant intersections to isolate the 8 true corner points.
     * Sorts the 8 true corner points by polar angle to establish a proper circular order.
     * Checks if the sorted vertices are inside the image with tolerance.
     * Checks ratio of shortest and longest sidelengths with tolerance.
     * @param validOctagonLines ArrayList<classes.Pipeline.HoughLine>
     * @param width int width of image
     * @param height int height of image
     * @return ArrayList<ArrayList<Point>> all found octagons
     */
    private static ArrayList<ArrayList<Point>> detectOctagonForm(ArrayList<HoughLine> validOctagonLines, int width, int height) {
        int size = validOctagonLines.size();
        int diagonal = (int) Math.ceil(Math.sqrt(Math.pow(height, 2) + Math.pow(width, 2)));
        int minSideLength = 30; // TODO: abhängig von bild größe
        ArrayList<ArrayList<Point>> allFoundOctagons = new ArrayList<>();

        // TODO: bei performanz-einbrüchen erst in winkelgruppen sortieren
        for (int i = 0; i < size; i++) {
            for (int j = i + 1; j < size; j++) {
                for (int k = j + 1; k < size; k++) {
                    for (int l = k + 1; l < size; l++) {
                        for (int m = l + 1; m < size; m++){
                            for (int n = m + 1; n < size; n++){
                                for (int o = n + 1; o < size; o++){
                                    for (int p = o + 1; p < size; p++){
                                        HoughLine a = validOctagonLines.get(i);
                                        HoughLine b = validOctagonLines.get(j);
                                        HoughLine c = validOctagonLines.get(k);
                                        HoughLine d = validOctagonLines.get(l);
                                        HoughLine e = validOctagonLines.get(m);
                                        HoughLine f = validOctagonLines.get(n);
                                        HoughLine g = validOctagonLines.get(o);
                                        HoughLine h = validOctagonLines.get(p);

                                        // sort into parallel groups
                                        ArrayList<ArrayList<HoughLine>> groups = new ArrayList<>();
                                        ArrayList<HoughLine> group1 = new ArrayList<>();
                                        group1.add(a);
                                        groups.add(group1);

                                        HoughLine[] remaining = {b, c, d, e, f, g, h};
                                        int toleranz = 10;

                                        for (HoughLine line : remaining) {
                                            boolean assigned = false;

                                            for (ArrayList<HoughLine> group : groups){
                                                int diff = Math.abs(line.phi - group.getFirst().phi);
                                                if (diff > 90) diff = 180 - diff;

                                                if (diff <= toleranz){
                                                    group.add(line);
                                                    assigned = true;
                                                    break;
                                                }
                                            }

                                            if (!assigned && groups.size() < 4){
                                                ArrayList<HoughLine> newGroup = new ArrayList<>();
                                                newGroup.add(line);
                                                groups.add(newGroup);
                                            }
                                        }

                                        if (groups.size() != 4 ||
                                                groups.get(0).size() != 2 ||
                                                groups.get(1).size() != 2 ||
                                                groups.get(2).size() != 2 ||
                                                groups.get(3).size() != 2){
                                            continue;
                                        }

                                        // sort groups by angle
                                        groups.sort(Comparator.comparingDouble(group -> group.getFirst().phi));

                                        // check intersections
                                        HoughLine[] p1 = { groups.get(0).get(0), groups.get(0).get(1) }; // 0°
                                        HoughLine[] p2 = { groups.get(1).get(0), groups.get(1).get(1) }; // 45°
                                        HoughLine[] p3 = { groups.get(2).get(0), groups.get(2).get(1) }; // 90°
                                        HoughLine[] p4 = { groups.get(3).get(0), groups.get(3).get(1) }; // 135°

                                        ArrayList<Point> intersections = new ArrayList<>();
                                        for (int q = 0; q < 2; q++) {
                                            for (int r = 0; r < 2; r++) {
                                                intersections.add(PipelineHelper.getIntersection(p1[q], p2[r], diagonal)); // 0° mit 45°
                                                intersections.add(PipelineHelper.getIntersection(p2[q], p3[r], diagonal)); // 45° mit 90°
                                                intersections.add(PipelineHelper.getIntersection(p3[q], p4[r], diagonal)); // 90° mit 135°
                                                intersections.add(PipelineHelper.getIntersection(p4[q], p1[r], diagonal)); // 135° mit 0°
                                            }
                                        }

                                        ArrayList<Point> validVertices = new ArrayList<>();
                                        int randToleranz = 40; //TODO: abhängig von größe des bildes
                                        for (Point v : intersections) {
                                            if (v != null && v.x >= -randToleranz && v.x < width + randToleranz
                                                    && v.y >= -randToleranz && v.y < height + randToleranz) {
                                                validVertices.add(v);
                                            }
                                        }

                                        if (validVertices.size() < 8) {
                                            continue;
                                        }

                                        // calculate center point
                                        double sumX = 0;
                                        double sumY = 0;
                                        for (Point v : validVertices){
                                            sumX += v.x;
                                            sumY += v.y;
                                        }
                                        double centerX = sumX / 8.0;
                                        double centerY = sumY / 8.0;

                                        // find 8 closest to center
                                        validVertices.sort(Comparator.comparingDouble(v -> v.distanceSq(centerX, centerY)));

                                        ArrayList<Point> vertices = new ArrayList<>();
                                        for (int v = 0; v < 8; v++) {
                                            vertices.add(validVertices.get(v));
                                        }

                                        // sort vertices by polar angle
                                        vertices.sort((vert1, vert2) -> {
                                            double angle1 = Math.atan2(vert1.y - centerY, vert1.x - centerX);
                                            double angle2 = Math.atan2(vert2.y - centerY, vert2.x - centerX);
                                            return Double.compare(angle1, angle2);
                                        });

                                        // check geometry
                                        int tolerance = 25; //TODO: abhängig von bild größe
                                        if (PipelineHelper.isInsideImage(vertices.get(0), width, height, tolerance) &&
                                                PipelineHelper.isInsideImage(vertices.get(1), width, height, tolerance) &&
                                                PipelineHelper.isInsideImage(vertices.get(2), width, height, tolerance) &&
                                                PipelineHelper.isInsideImage(vertices.get(3), width, height, tolerance) &&
                                                PipelineHelper.isInsideImage(vertices.get(4), width, height, tolerance) &&
                                                PipelineHelper.isInsideImage(vertices.get(5), width, height, tolerance) &&
                                                PipelineHelper.isInsideImage(vertices.get(6), width, height, tolerance) &&
                                                PipelineHelper.isInsideImage(vertices.get(7), width, height, tolerance)) {

                                            // check side length
                                            double s1 = vertices.get(0).distance(vertices.get(1));
                                            double s2 = vertices.get(1).distance(vertices.get(2));
                                            double s3 = vertices.get(2).distance(vertices.get(3));
                                            double s4 = vertices.get(3).distance(vertices.get(4));
                                            double s5 = vertices.get(4).distance(vertices.get(5));
                                            double s6 = vertices.get(5).distance(vertices.get(6));
                                            double s7 = vertices.get(6).distance(vertices.get(7));
                                            double s8 = vertices.get(7).distance(vertices.get(0));

                                            if (s1 > minSideLength && s2 > minSideLength && s3 > minSideLength && s4 > minSideLength &&
                                                    s5 > minSideLength && s6 > minSideLength && s7 > minSideLength && s8 > minSideLength) {
                                                double t = 0.2;
                                                double maxSide = Math.max(Math.max(Math.max(s1, s2), Math.max(s3, s4)), Math.max(Math.max(s5, s6), Math.max(s7, s8)));
                                                double minSide = Math.min(Math.min(Math.min(s1, s2), Math.min(s3, s4)), Math.min(Math.min(s5, s6), Math.min(s7, s8)));
                                                double ratio = minSide/maxSide;
                                                if (ratio >= 1-t && ratio <= 1+t){
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
        int minSideLength = 30;//TODO: abhängig von bild größe
        ArrayList<ArrayList<Point>> allFoundRectangles = new ArrayList<>();

        for (int i = 0; i < size; i++) {
            for (int j = i + 1; j < size; j++) {
                for (int k = j + 1; k < size; k++) {
                    for (int l = k + 1; l < size; l++) {

                        HoughLine a = validRectangleLines.get(i);
                        HoughLine b = validRectangleLines.get(j);
                        HoughLine c = validRectangleLines.get(k);
                        HoughLine d = validRectangleLines.get(l);

                        // check angles
                        if (!FormChecker.isRectangleAngles(a, b, c, d)){
                            continue;
                        }

                        // sort into parallel groups
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

                        if (group1.size() != 2 || group2.size() != 2) {
                            continue;
                        }
                        HoughLine h1 = group1.get(0);
                        HoughLine h2 = group1.get(1);
                        HoughLine v1 = group2.get(0);
                        HoughLine v2 = group2.get(1);

                        // check intersections
                        Point p1 = PipelineHelper.getIntersection(h1, v1, diagonal);
                        Point p2 = PipelineHelper.getIntersection(h1, v2, diagonal);
                        Point p3 = PipelineHelper.getIntersection(h2, v2, diagonal);
                        Point p4 = PipelineHelper.getIntersection(h2, v1, diagonal);

                        if (p1 == null || p2 == null || p3 == null || p4 == null) continue;

                        // check geometry
                        int tolerance = 25; //TODO: abhängig von bild größe
                        if (PipelineHelper.isInsideImage(p1, width, height, tolerance) &&
                                PipelineHelper.isInsideImage(p2, width, height, tolerance) &&
                                PipelineHelper.isInsideImage(p3, width, height, tolerance) &&
                                PipelineHelper.isInsideImage(p4, width, height, tolerance)) {

                            // check side length
                            double s1 = p1.distance(p2);
                            double s2 = p2.distance(p3);
                            double s3 = p3.distance(p4);
                            double s4 = p4.distance(p1);

                            if (s1 > minSideLength && s2 > minSideLength && s3 > minSideLength && s4 > minSideLength) {
                                double t = 0.2;
                                double maxSide = Math.max(Math.max(s1, s2), Math.max(s3, s4));
                                double minSide = Math.min(Math.min(s1, s2), Math.min(s3, s4));
                                double ratio = minSide/maxSide;
                                if (ratio >= 1-t && ratio <= 1+t){
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
        int diagonal = (int) Math.ceil(Math.sqrt(Math.pow(height, 2) + Math.pow(width, 2)));
        int minSideLength = 30;
        ArrayList<ArrayList<Point>> allFoundTriangles = new ArrayList<>();

        for (int i = 0; i < size; i++){
            for (int j = i + 1; j < size; j++){
                for (int k = j + 1; k < size; k++){
                    HoughLine a = validTriangleLines.get(i);
                    HoughLine b = validTriangleLines.get(j);
                    HoughLine c = validTriangleLines.get(k);

                    // check angles
                    if (!FormChecker.isTriangleAngles(a, b, c)){
                        continue;
                    }

                    // check intersections
                    Point pAB = PipelineHelper.getIntersection(a, b, diagonal);
                    Point pAC = PipelineHelper.getIntersection(a, c, diagonal);
                    Point pBC = PipelineHelper.getIntersection(b, c, diagonal);

                    if (pAB == null || pBC == null || pAC == null) continue;

                    // check geometry
                    int tolerance = 15;
                    if (PipelineHelper.isInsideImage(pAB, width, height, tolerance) &&
                            PipelineHelper.isInsideImage(pBC, width, height, tolerance) &&
                            PipelineHelper.isInsideImage(pAC, width, height, tolerance)) {

                        // check side length
                        double side1 = pAB.distance(pBC);
                        double side2 = pBC.distance(pAC);
                        double side3 = pAC.distance(pAB);

                        if (side1 >= minSideLength && side2 >= minSideLength && side3 >= minSideLength) {
                            double t = 0.3;
                            double maxSide = Math.max(Math.max(side1, side2), side3);
                            double minSide = Math.min(Math.min(side1, side2), side3);
                            double ratio = minSide/maxSide;
                            if (ratio >= 1-t && ratio <= 1+t){
                                ArrayList<Point> triangle = new ArrayList<>();
                                triangle.add(pAB);
                                triangle.add(pAC);
                                triangle.add(pBC);
                                allFoundTriangles.add(triangle);
                            }

                        }
                    }
                }
            }
        }
        return allFoundTriangles;
    }


    /**
     * Checks if four HoughLines form a rectangle.
     * @param a classes.Pipeline.HoughLine
     * @param b classes.Pipeline.HoughLine
     * @param c classes.Pipeline.HoughLine
     * @param d classes.Pipeline.HoughLine
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
     * @param a classes.Pipeline.HoughLine
     * @param b classes.Pipeline.HoughLine
     * @param c classes.Pipeline.HoughLine
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
