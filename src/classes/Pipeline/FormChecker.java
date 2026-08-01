package classes.Pipeline;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.Arrays;

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
                        if (!FormCheckHelper.isRectangleAngles(a, b, c, d)) continue;

                        // 3. sort into parallel groups
                        HoughLine[] sides = FormCheckHelper.sortIntoParallelGroups(a, b, c, d, angleTolerance);
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
                        if (FormCheckHelper.arePointsInvalidOrOutsideImage(rectangle, width, height, edgeTolerance)) continue;

                        // 5. check squared side length and side ratio
                        if (FormCheckHelper.isValidSideLengthAndRatio(rectangle, minSideLength, sideRatioTolerance)){
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
                    if (!FormCheckHelper.isTriangleAngles(a, b, c)) continue;

                    // 3. check intersections
                    ArrayList<Point> triangle = new ArrayList<>(Arrays.asList(
                            PipelineHelper.getIntersection(a, b, diagonal),
                            PipelineHelper.getIntersection(a, c, diagonal),
                            PipelineHelper.getIntersection(b, c, diagonal)
                    ));
                    if (FormCheckHelper.arePointsInvalidOrOutsideImage(triangle, width, height, insideImageTolerance)) continue;

                    // 5. check squared side length and side ratio
                    if (FormCheckHelper.isValidSideLengthAndRatio(triangle, minSideLength, sideRatioTolerance)){
                        allFoundTriangles.add(triangle);
                    }
                }
            }
        }
        return allFoundTriangles;
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
        int minNumberOfLines = 6;
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
        FormCheckHelper.sortAngleGroupsByPosition(g0, g45, g90, g135, width, height, diagonal);

        // 3. check lines of groups for octagon geometry
        for (int i = 0; i < g0.size(); i++) {
            for (int j = g0.size() - 1; j >= i; j--) {

                // pull at least one line from every group
                ArrayList<HoughLine> A = FormCheckHelper.selectPair(g0, i, j);

                for (int k = 0; k < g45.size(); k++) {
                    for (int l = g45.size() - 1; l >= k; l--) {

                        ArrayList<HoughLine> B = FormCheckHelper.selectPair(g45, k, l);

                        for (int m = 0; m < g90.size(); m++) {
                            for (int n = g90.size() - 1; n >= m; n--) {

                                ArrayList<HoughLine> C = FormCheckHelper.selectPair(g90, m, n);

                                for (int o = 0; o < g135.size(); o++) {
                                    for (int p = g135.size() - 1; p >= o; p--) {

                                        ArrayList<HoughLine> D = FormCheckHelper.selectPair(g135, o, p);

                                        // early exits
                                        if (A.size() + B.size() + C.size() + D.size() < minNumberOfLines) continue;
                                        if (A.isEmpty() || B.isEmpty() || C.isEmpty() || D.isEmpty()) continue;
                                        if (!(A.size() == 2 || B.size() == 2 || C.size() == 2 || D.size() == 2)) continue;

                                        // 4. calculate sign width
                                        FormCheckHelper.SignWidthResult widthResult = FormCheckHelper.calculateSignWidth(A, B, C, D);
                                        int signWidth = widthResult.width();
                                        if (signWidth <= 0) continue;

                                        // 5. calculate approx center
                                        Point approxCenter = FormCheckHelper.calculateApproxCenter(A, B, C, D, diagonal);
                                        if (approxCenter == null) continue;

                                        // 6. clean groups of garbage lines
                                        if (widthResult.validGroup1() != 1 && widthResult.validGroup2() != 1) {
                                            FormCheckHelper.cleanGarbageLines(A, approxCenter, signWidth, diagonal);
                                        }
                                        if (widthResult.validGroup1() != 2 && widthResult.validGroup2() != 2) {
                                            FormCheckHelper.cleanGarbageLines(B, approxCenter, signWidth, diagonal);
                                        }
                                        if (widthResult.validGroup1() != 3 && widthResult.validGroup2() != 3) {
                                            FormCheckHelper.cleanGarbageLines(C, approxCenter, signWidth, diagonal);
                                        }
                                        if (widthResult.validGroup1() != 4 && widthResult.validGroup2() != 4) {
                                            FormCheckHelper.cleanGarbageLines(D, approxCenter, signWidth, diagonal);
                                        }

                                        // 7. add second lines to groups where missing
                                        FormCheckHelper.addSecondLines(A, B, C, D, approxCenter, signWidth, diagonal);

                                        // 8. determine octagon
                                        ArrayList<Point> vertices = FormCheckHelper.calculateOctagonPoints(A, B, C, D, diagonal);
                                        if (vertices == null) continue;

                                        // 9. sort vertices by polar angle
                                        FormCheckHelper.sortPointsByPolarAngle(vertices);

                                        // 10. check geometry of octagon
                                        if (FormCheckHelper.arePointsInvalidOrOutsideImage(vertices, width, height, edgeTolerance )) continue;
                                        if (FormCheckHelper.isValidSideLengthAndRatio(vertices, minSideLength, sideRatioTolerance)){
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

}
