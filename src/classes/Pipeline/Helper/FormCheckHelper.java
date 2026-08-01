package classes.Pipeline.Helper;

import classes.GlobalHelperFunctions;
import classes.Pipeline.HoughLine;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;

public class FormCheckHelper {

    //------------------------------------------------------------------------------------------------------------------
    // helper for rectangle, triangle and octagon
    //------------------------------------------------------------------------------------------------------------------

    /**
     * Checks if shape formed by currentShape has side lengths > minLength.
     * @param currentShape ArrayList<Point>
     * @param minLength double
     * @return boolean
     */
    public static boolean isShapeBigEnough(ArrayList<Point> currentShape, double minLength){
        double minLengthSq = minLength * minLength;

        for (int i = 0; i < currentShape.size(); i++){
            Point p1 = currentShape.get(i);
            Point p2 = currentShape.get((i + 1) % currentShape.size());

            double dx = p2.x - p1.x;
            double dy = p2.y - p1.y;
            double lengthSq = dx * dx + dy * dy;

            if (lengthSq < minLengthSq) return false;
        }
        return true;
    }

    /**
     * Calculates sides from vertices array and checks lengths against minSideLength.
     * Checks ratio of shortest and longest sides against sideRatioTolerance.
     * @param vertices ArrayList<Point>
     * @param minSideLength int
     * @param sideRatioTolerance int
     * @return boolean is sides and ratio are valid
     */
    public static boolean isValidSideLengthAndRatio(ArrayList<Point> vertices, int minSideLength, double sideRatioTolerance){
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
     * Checks if points from vertices array are invalid (== null) or outside of image using {@link FormCheckHelper#isInsideImage(Point, int, int, int)}.
     * @param vertices ArrayList<Point>
     * @param width int
     * @param height int
     * @param tolerance int
     * @return boolean if points are invalid or outside image
     */
    public static boolean arePointsInvalidOrOutsideImage(ArrayList<Point> vertices, int width, int height, int tolerance){
        for (Point v : vertices){
            if (v == null || !FormCheckHelper.isInsideImage(v, width, height, tolerance)){
                return true;
            }
        }
        return false;
    }

    /**
     * Helper to check if p is inside the image with tolerance.
     * @param p Point
     * @param width int
     * @param height int
     * @param t int
     * @return boolean if p is inside the image
     */
    public static boolean isInsideImage(Point p, int width, int height, int t) {
        return (p.x < (width+t) && p.x > (-t)) && (p.y < (height+t) && p.y > -t);
    }

    /**
     * Returns intersection point of lines a and b
     * @param a classes.Pipeline.HoughLine
     * @param b classes.Pipeline.HoughLine
     * @param diagonal image diagonal
     * @return Point intersection point
     */
    public static Point getIntersection(HoughLine a, HoughLine b, int diagonal) {
        double r1 = a.r() - diagonal;
        double r2 = b.r() - diagonal;

        double phi1 = Math.toRadians(a.phi());
        double phi2 = Math.toRadians(b.phi());

        double denominator = Math.cos(phi1) * Math.sin(phi2) - Math.sin(phi1) * Math.cos(phi2);

        // return if parallel
        if (Math.abs(denominator) < 0.0001) {
            return null;
        }

        int x = (int) Math.round((r1 * Math.sin(phi2) - r2 * Math.sin(phi1)) / denominator);
        int y = (int) Math.round((r2 * Math.cos(phi1) - r1 * Math.cos(phi2)) / denominator);

        return new Point(x, y);
    }

    //------------------------------------------------------------------------------------------------------------------
    // helper for rectangle
    //------------------------------------------------------------------------------------------------------------------

    /**
     * Sorts the given HoughLines into parallel groups. Checks if each group has 2 members.
     * @param a HoughLines
     * @param b HoughLines
     * @param c HoughLines
     * @param d HoughLines
     * @param angleTolerance int
     * @return HoughLines[] sorted: horizontal, horizontal, vertical, vertical
     */
    public static HoughLine[] sortIntoParallelGroups(HoughLine a, HoughLine b, HoughLine c, HoughLine d, int angleTolerance) {
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

    /**
     * Checks central pixels color against blue
     * @param originalImage BufferedImage
     * @param currentRectangle ArrayList<Point>
     * @return boolean if center color is valid
     */
    public static boolean isValidRectangleCenterColor(BufferedImage originalImage, ArrayList<Point> currentRectangle) {
        int centerX = (currentRectangle.get(0).x + currentRectangle.get(1).x + currentRectangle.get(2).x + currentRectangle.get(3).x) / 4;
        int centerY = (currentRectangle.get(0).y + currentRectangle.get(1).y + currentRectangle.get(2).y + currentRectangle.get(3).y) / 4;

        if (centerX < 0 || centerX >= originalImage.getWidth() || centerY < 0 || centerY >= originalImage.getHeight()) return true;

        int centerRGB = originalImage.getRGB(centerX, centerY);
        int cR = (centerRGB >> 16) & 0xFF;
        int cG = (centerRGB >> 8) & 0xFF;
        int cB = centerRGB & 0xFF;

        // no blue
        return cB <= cR || cB <= cG || cB <= 80;
    }

    //------------------------------------------------------------------------------------------------------------------
    // helper for rectangle and triangle
    //------------------------------------------------------------------------------------------------------------------

    /**
     * Checks if four HoughLines form a rectangle based on angles with tolerance.
     * @param a HoughLine
     * @param b HoughLine
     * @param c HoughLine
     * @param d HoughLine
     * @return boolean if lines form a rectangle
     */
    public static boolean isRectangleAngles(HoughLine a, HoughLine b, HoughLine c, HoughLine d) {
        int[] phi = {a.phi(), b.phi(), c.phi(), d.phi()};
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
    public static boolean isTriangleAngles(HoughLine a, HoughLine b, HoughLine c) {
        int[] phi = {a.phi(), b.phi(), c.phi()};
        Arrays.sort(phi);

        int tolerance = 30;

        int diff1 = phi[1] - phi[0];
        int diff2 = phi[2] - phi[1];
        int diff3 = 180 - (phi[2] - phi[0]);

        return Math.abs(diff1 - 60) <= tolerance &&
                Math.abs(diff2 - 60) <= tolerance &&
                Math.abs(diff3 - 60) <= tolerance;
    }

    //------------------------------------------------------------------------------------------------------------------
    // helper for octagon
    //------------------------------------------------------------------------------------------------------------------

    /**
     * Sorts angle groups based on their pixel positions from left to right, top to bottom.
     * @param g0 ArrayList<HoughLine>
     * @param g45 ArrayList<HoughLine>
     * @param g90 ArrayList<HoughLine>
     * @param g135 ArrayList<HoughLine>
     * @param width int
     * @param height int
     * @param diagonal int
     */
    public static void sortAngleGroupsByPosition(ArrayList<HoughLine> g0, ArrayList<HoughLine> g45, ArrayList<HoughLine> g90, ArrayList<HoughLine> g135, int width, int height, int diagonal){

        // sort by x-position
        g0.sort(Comparator.comparingDouble(line -> {
            double rad = Math.toRadians(line.phi());
            double cos = Math.cos(rad);
            if (Math.abs(cos) < 0.0001) cos = 0.0001;
            double realR = line.r() - diagonal;
            return (realR - (height / 2.0) * Math.sin(rad)) / cos;
        }));

        // sort by Achsenabschnitt
        g45.sort(Comparator.comparingDouble(line -> {
            double rad = Math.toRadians(line.phi());
            double denominator = Math.cos(rad) + Math.sin(rad);
            if (Math.abs(denominator) < 0.0001) denominator = 0.0001;
            return line.r() / denominator;
        }));

        // sort by y-position
        g90.sort(Comparator.comparingDouble(line -> {
            double rad = Math.toRadians(line.phi());
            double sin = Math.sin(rad);
            if (Math.abs(sin) < 0.0001) sin = 0.0001;
            return (line.r() - (width / 2.0) * Math.cos(rad)) / sin;
        }));

        // sort by Achsenabschnitt
        g135.sort(Comparator.comparingDouble(line -> {
            double rad = Math.toRadians(line.phi());
            double denominator = Math.cos(rad) - Math.sin(rad);
            if (Math.abs(denominator) < 0.0001) denominator = 0.0001;
            return line.r() / denominator;
        }));
    }

    /**
     * Selects two HoughLines from group based on index1 and index2.
     * @param group ArrayList<HoughLine>
     * @param index1 int
     * @param index2 int
     * @return ArrayList<HoughLine> with one or two HoughLines
     */
    public static ArrayList<HoughLine> selectPair(ArrayList<HoughLine> group, int index1, int index2){
        ArrayList<HoughLine> pair = new ArrayList<>();
        pair.add(group.get(index1));
        if (index1 != index2) {
            pair.add(group.get(index2));
        }
        return pair;
    }

    public record SignWidthResult(int width, int validGroup1, int validGroup2) {}
    public record GroupDistance(int groupId, int dist) {}
    /**
     * Calculates width of sign by checking each groups lines distances from another.
     * Determines best distance by checking wich groups got roughly the same distance.
     * The groups that contributed to the width get marked as valid.
     * @param A ArrayList<HoughLine>
     * @param B ArrayList<HoughLine>
     * @param C ArrayList<HoughLine>
     * @param D ArrayList<HoughLine>
     * @return record class SignWidthResult
     */
    public static SignWidthResult calculateSignWidth(ArrayList<HoughLine> A, ArrayList<HoughLine> B, ArrayList<HoughLine> C, ArrayList<HoughLine> D){
        ArrayList<GroupDistance> distances = new ArrayList<>();

        // calculate distances for lines of every group
        if (A.size() == 2){
            int r1 = A.get(0).phi() >= 90 ? -A.get(0).r() : A.get(0).r();
            int r2 = A.get(1).phi() >= 90 ? -A.get(1).r() : A.get(1).r();
            distances.add(new GroupDistance(1, Math.abs(r1 - r2)));
        }
        if (B.size() == 2) distances.add(new GroupDistance(2, Math.abs(B.get(0).r() - B.get(1).r())));
        if (C.size() == 2) distances.add(new GroupDistance(3, Math.abs(C.get(0).r() - C.get(1).r())));
        if (D.size() == 2) distances.add(new GroupDistance(4, Math.abs(D.get(0).r() - D.get(1).r())));

        if (distances.size() < 2) return new SignWidthResult(0, 0, 0);

        // determine width
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

    /**
     * Approximates the center point of four groups.
     * Checks intersections of lines of groups that have two lines. {@link FormCheckHelper#getIntersection(HoughLine, HoughLine, int)}
     * Sums x and y coordinates for center point.
     * @param A ArrayList<HoughLine>
     * @param B ArrayList<HoughLine>
     * @param C ArrayList<HoughLine>
     * @param D ArrayList<HoughLine>
     * @param diagonal int
     * @return Point approximated center point
     */
    public static Point calculateApproxCenter(ArrayList<HoughLine> A, ArrayList<HoughLine> B, ArrayList<HoughLine> C, ArrayList<HoughLine> D, int diagonal){
        ArrayList<Point> intersections = new ArrayList<>();

        if (A.size() == 2 || B.size() == 2){
            for (HoughLine line1 : A) {
                for (HoughLine line2 : B) {
                    Point intersection = FormCheckHelper.getIntersection(line1, line2, diagonal);
                    if (intersection != null) intersections.add(intersection);
                }
            }
        }
        if (B.size() == 2 || C.size() == 2){
            for (HoughLine line1 : B) {
                for (HoughLine line2 : C) {
                    Point intersection = FormCheckHelper.getIntersection(line1, line2, diagonal);
                    if (intersection != null) intersections.add(intersection);
                }
            }
        }
        if (C.size() == 2 || D.size() == 2){
            for (HoughLine line1 : C) {
                for (HoughLine line2 : D) {
                    Point intersection = FormCheckHelper.getIntersection(line1, line2, diagonal);
                    if (intersection != null) intersections.add(intersection);
                }
            }
        }
        if (D.size() == 2 || A.size() == 2){
            for (HoughLine line1 : D) {
                for (HoughLine line2 : A) {
                    Point intersection = FormCheckHelper.getIntersection(line1, line2, diagonal);
                    if (intersection != null) intersections.add(intersection);
                }
            }
        }

        if (intersections.isEmpty()) return null;

        // sum x and y for center point
        int sumX = 0;
        int sumY = 0;
        for (Point p : intersections){
            sumX += p.x;
            sumY += p.y;
        }

        return new Point(sumX / intersections.size(), sumY / intersections.size());
    }

    /**
     * Removes a HoughLine from group based on its distance to approxCenter.
     * Removes the HoughLine with the bigger error.
     * @param group ArrayList<HoughLine>
     * @param approxCenter Point
     * @param signWidth int
     * @param diagonal int
     */
    public static void cleanGarbageLines(ArrayList<HoughLine> group, Point approxCenter, int signWidth, int diagonal) {

        int distanceCenter1 = (int) Math.abs((approxCenter.x * Math.cos(Math.toRadians(group.getFirst().phi())) + approxCenter.y * Math.sin(Math.toRadians(group.getFirst().phi())) - (group.getFirst().r() - diagonal)));
        int distanceCenter2 = (int) Math.abs((approxCenter.x * Math.cos(Math.toRadians(group.getLast().phi())) + approxCenter.y * Math.sin(Math.toRadians(group.getLast().phi())) - (group.getLast().r() - diagonal)));

        int error1 = Math.abs(distanceCenter1 - (signWidth / 2));
        int error2 = Math.abs(distanceCenter2 - (signWidth / 2));

        if (error1 < error2){
            group.removeLast();
        } else {
            group.removeFirst();
        }
    }

    /**
     * Calls {@link FormCheckHelper#completeSingleGroup(ArrayList, Point, int, int)} for A, B, C and D.
     * @param A ArrayList<HoughLine>
     * @param B ArrayList<HoughLine>
     * @param C ArrayList<HoughLine>
     * @param D ArrayList<HoughLine>
     * @param approxCenter Point
     * @param signWidth int
     * @param diagonal int
     */
    public static void addSecondLines(ArrayList<HoughLine> A, ArrayList<HoughLine> B, ArrayList<HoughLine> C, ArrayList<HoughLine> D, Point approxCenter, int signWidth, int diagonal){
        FormCheckHelper.completeSingleGroup(A, approxCenter, signWidth, diagonal);
        FormCheckHelper.completeSingleGroup(B, approxCenter, signWidth, diagonal);
        FormCheckHelper.completeSingleGroup(C, approxCenter, signWidth, diagonal);
        FormCheckHelper.completeSingleGroup(D, approxCenter, signWidth, diagonal);
    }

    /**
     * Checks if group needs an additional line to reach size 2.
     * Calculates missing line by mirroring line at approxCenter.
     * @param group ArrayList<HoughLine>
     * @param approxCenter Point
     * @param signWidth int
     * @param diagonal int
     */
    private static void completeSingleGroup(ArrayList<HoughLine> group, Point approxCenter, int signWidth, int diagonal){
        if (group.size() == 1) {
            HoughLine line = group.getFirst();
            int rCenter = (int) (approxCenter.x * Math.cos(Math.toRadians(line.phi()))
                    + approxCenter.y * Math.sin(Math.toRadians(line.phi()))
                    + diagonal);

            if (rCenter > line.r()) {
                group.add(new HoughLine(line.phi(), line.r() + signWidth, 100));
            } else {
                group.addFirst(new HoughLine(line.phi(), line.r() - signWidth, 100));
            }
        }
    }

    /**
     * Calculates octagon points.
     * Calculates intersections between groups {@link FormCheckHelper#calculateIntersectionsBetweenGroups(ArrayList, ArrayList, int, ArrayList)}.
     * Calculates center point by summing all x and y coordinates.
     * Sorts intersection points based on distance to center point.
     * Returns array of 8 points closest to center point.
     * @param A ArrayList<HoughLine>
     * @param B ArrayList<HoughLine>
     * @param C ArrayList<HoughLine>
     * @param D ArrayList<HoughLine>
     * @param diagonal int
     * @return ArrayList<Point> octagon points
     */
    public static ArrayList<Point> calculateOctagonPoints(ArrayList<HoughLine> A, ArrayList<HoughLine> B, ArrayList<HoughLine> C, ArrayList<HoughLine> D, int diagonal) {
        ArrayList<Point> intersections = new ArrayList<>();

        // calculate intersections between groups
        calculateIntersectionsBetweenGroups(A, B, diagonal, intersections);
        calculateIntersectionsBetweenGroups(B, C, diagonal, intersections);
        calculateIntersectionsBetweenGroups(C, D, diagonal, intersections);
        calculateIntersectionsBetweenGroups(D, A, diagonal, intersections);

        if (intersections.size() < 8) return null;

        // calculate center point
        double[] center = GlobalHelperFunctions.calculateCenterCoordinates(intersections);
        double centerX = center[0];
        double centerY = center[1];

        // sort intersection points based on distance to center point
        intersections.sort(Comparator.comparingDouble(p -> p.distanceSq(centerX, centerY)));

        // grab 8 points closest to center point
        ArrayList<Point> vertices = new ArrayList<>();
        for (int i = 0; i < 8; i++) {
            vertices.add(intersections.get(i));
        }

        return vertices;
    }

    /**
     * Calculates intersections between HoughLines of two groups.
     * {@link FormCheckHelper#getIntersection(HoughLine, HoughLine, int)}
     * @param group1 ArrayList<HoughLine>
     * @param group2 ArrayList<HoughLine>
     * @param diagonal int
     * @param outIntersections ArrayList<Point> where intersections are saved
     */
    private static void calculateIntersectionsBetweenGroups(ArrayList<HoughLine> group1, ArrayList<HoughLine> group2, int diagonal, ArrayList<Point> outIntersections) {
        for (HoughLine line1 : group1) {
            for (HoughLine line2 : group2) {
                Point p = FormCheckHelper.getIntersection(line1, line2, diagonal);
                if (p != null) {
                    outIntersections.add(p);
                }
            }
        }
    }

    /**
     * Sorts points array by their polar angles.
     * Calculates center by summing all x and y coordinates.
     * @param points ArrayList<Point>
     */
    public static void sortPointsByPolarAngle(ArrayList<Point> points) {
        if (points == null || points.isEmpty()) return;

        // calculate center
        double[] center = GlobalHelperFunctions.calculateCenterCoordinates(points);
        double centerX = center[0];
        double centerY = center[1];

        // sort by polar angle
        points.sort((p1, p2) -> {
            double angle1 = Math.atan2((p1.y - centerY), p1.x - centerX);
            double angle2 = Math.atan2((p2.y - centerY), p2.x - centerX);
            return Double.compare(angle1, angle2);
        });
    }

}
