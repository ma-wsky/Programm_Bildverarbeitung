import java.awt.*;
import java.awt.image.BufferedImage;
import java.util.*;

public class PipelinePrototype {

    public static void vorfahrt(){
        // 1. read image
        String filenamePPMImage = "sign.ppm";
        BufferedImage input = ImageIO.readImageAndConvertToPPM("pics/sample/A.jpg", filenamePPMImage);
        System.out.println("Successfully loaded image " + filenamePPMImage);
        ImageIO.displayImage(input);

        // 2. preprocess image
        BufferedImage preProcessedImage = PipelinePrototype.imagePreprocessing(input);

        // hough path
        BufferedImage lines = PipelinePrototype.lookForStraightLinesAndDraw(preProcessedImage);
        ImageIO.displayImage(lines);
        BufferedImage filled = PipelinePrototype.findSignAndFill(lines);
        ImageIO.displayImage(filled);

        // 3. look for sign-geometry
        //BufferedImage signGeometry = PipelinePrototype.lookForSignGeometry(lines);
        //ImageIO.displayImage(signGeometry);

        // 4. check signs geometry
        boolean[] isSquare = {false};
        BufferedImage rotatedSignGeometry = PipelinePrototype.rotateImageAndCheckGeometry(filled, 0.1, isSquare);
        ImageIO.displayImage(rotatedSignGeometry);
        if (isSquare[0]){
            //System.out.println("Quadrat erkannt!");
        }

        // 5. check sign statistics

        // rotate original
        Point centerPoint = PipelinePrototype.calculateCenterPointOfSign(filled);
        BufferedImage rotatedOriginal = RotatedImage.rotateImageBackwardMapping(input, centerPoint, 45);
        ImageIO.displayImage(rotatedOriginal);

        // crop original
        BufferedImage croppedRotatedOriginal = PipelinePrototype.cropAndMaskSign(rotatedOriginal, rotatedSignGeometry);
        ImageIO.displayImage(croppedRotatedOriginal);

        //stats
        DescriptiveStatistics stats = new DescriptiveStatistics(croppedRotatedOriginal);
        stats.calculateAllStatistics();

        System.out.println("Calculations ended.");

        // scoring
        // TODO: rework scoring to make it more general
        // TODO: check relativeCumulativeFrequency for two peaks with valley between, cumulate pixels in yellow and white in HSV values

    }

    /**
     * Calls {@link PipelinePrototype#findCoordsOfSign(BufferedImage)} to find the outermost corner coordinates.
     * Calls {@link PipelinePrototype#fillInterior(BufferedImage, boolean[][], Point)} to fill the shape.
     * Must only be called with an image after line detection with hough ({@link PipelinePrototype#lookForStraightLinesAndDraw(BufferedImage)}).
     * @param image BufferedImage with lines forming a closed shape
     * @return BufferedImage with filled shape
     */
    private static BufferedImage findSignAndFill(BufferedImage image) {
        Point centerPoint = PipelinePrototype.calculateCenterPointOfSign(image);
        boolean[][] visited = new boolean[image.getWidth()][image.getHeight()];
        return PipelinePrototype.fillInterior(image, visited, centerPoint);
    }

    /**
     * Looks in each direction in the (phi, r) Hough room to determine local maximum
     * @param accumulator Hough room matrix
     * @param phi angle
     * @param r distance
     * @return boolean if local maximum
     */
    private static boolean isLocalMaximum(int[][] accumulator, int phi, int r, int sizeOfWindow){

        for (int dPhi = -sizeOfWindow; dPhi <= sizeOfWindow; dPhi++){
            for (int dR = -sizeOfWindow; dR <= sizeOfWindow; dR++){
                if (dPhi == 0 && dR == 0) continue;

                int targetPhi = phi + dPhi;
                int targetR = r + dR;

                // closed loop for angles
                if (targetPhi < 0){
                    targetPhi = accumulator.length - 1;
                } else if (targetPhi >= accumulator.length) {
                    targetPhi = 0;
                }

                // bounds for r
                if (targetR < 0 || targetR >= accumulator[targetPhi].length){
                    continue;
                }

                // neighbour is bigger
                if (accumulator[targetPhi][targetR] > accumulator[phi][r]){
                    return false;
                }
            }
        }
        return true;
    }

    /**
     * Calls {@link PipelinePrototype#houghTransformation(BufferedImage)} to determine Hough room.
     * Calls {@link PipelinePrototype#isLocalMaximum(int[][], int, int, int)} to determine local maximum of possible line.
     * Sorts found lines and draws the largest lines.
     * @param image BufferedImage
     * @return BufferedImage with the largest lines
     */
    private static BufferedImage lookForStraightLinesAndDraw(BufferedImage image){
        //make edge black
        int width = image.getWidth();
        int height = image.getHeight();
        int border = 5;

        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                if (x < border || x >= width - border || y < border || y >= height - border) {
                    image.setRGB(x, y, 0xFF000000);
                }
            }
        }

        // copy for displaying lines
        BufferedImage lineImage = new BufferedImage(image.getWidth(), image.getHeight(), image.getType());
        Graphics2D g = lineImage.createGraphics();
        g.setColor(Color.WHITE);
        g.setStroke(new java.awt.BasicStroke(3));

        ArrayList<HoughLine> lines = new ArrayList<>();
        int[][] accumulator = PipelinePrototype.houghTransformation(image);
        int diagonal = (int) Math.ceil(Math.sqrt(Math.pow(image.getHeight(), 2) + Math.pow(image.getWidth(), 2)));
        int threshold = 50; //TODO: abhängig von bild größe

        // accumulate lines
        for (int phi = 0; phi < accumulator.length; phi++){
            for (int r = 0; r < accumulator[phi].length; r++){
                int votes = accumulator[phi][r];
                if (votes > threshold){
                    int minLength = 20;//TODO: abhängig von bild größe
                    int maxAllowedGap = 5;//TODO: abhängig von bild größe
                    int sizeOfNeighbourhood = 3;//TODO: abhängig von bild größe
                    if (PipelinePrototype.isLocalMaximum(accumulator, phi, r, sizeOfNeighbourhood) && PipelinePrototype.isLineSolid(image, new HoughLine(phi, r, votes), minLength, maxAllowedGap)){
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
                int angleOfIntersection = 0;

                int deltaPhi = Math.abs(line1.phi - line2.phi);
                if (deltaPhi > 90) deltaPhi = 180 - deltaPhi;

                if (deltaPhi >= 90 - tolerance && deltaPhi <= 90 + tolerance){
                    angleOfIntersection = 90;
                }
                else if (deltaPhi >= 60 - tolerance && deltaPhi <= 60 + tolerance) {
                    angleOfIntersection = 60;
                }
                else if (deltaPhi >= 45 - tolerance && deltaPhi <= 45 + tolerance) {
                    angleOfIntersection = 45;
                }

                if (angleOfIntersection != 0) {
                    pairs.add(new HoughLinePair(line1, line2, angleOfIntersection));
                    validLines.add(line1);
                    validLines.add(line2);
                }

            }
        }

        PipelinePrototype.drawLines(g, validLines, width, height, diagonal);

        //TODO: check for each signs geometry separately by excluding unused angles

        // triangle check
        ArrayList<HoughLine> validTriangleLines = new ArrayList<>();
        for (HoughLinePair pair : pairs){
            if (pair.angleOfIntersection > 60 - tolerance && pair.angleOfIntersection < 60 + tolerance) {
                if (!validTriangleLines.contains(pair.line1)) validTriangleLines.add(pair.line1);
                if (!validTriangleLines.contains(pair.line2)) validTriangleLines.add(pair.line2);
            }
        }

        System.out.println("checking triangle form...");
        ArrayList<ArrayList<Point>> allFoundTriangles = PipelinePrototype.detectTriangleForm(validTriangleLines, image.getWidth(), image.getHeight());
        if (!allFoundTriangles.isEmpty()){
            System.out.println("triangles found - drawing triangles...");
            for (ArrayList<Point> triangle : allFoundTriangles){
                PipelinePrototype.drawForm(g, triangle);
            }
            g.dispose();
            return lineImage;
        } else {
            System.out.println("no triangle detected!");
        }

        // octagon check
        ArrayList<HoughLine> validOctagonLines = new ArrayList<>();
        for (HoughLinePair pair : pairs){
            if ((pair.angleOfIntersection > 90 - tolerance && pair.angleOfIntersection < 90 + tolerance) ||
                    (pair.angleOfIntersection > 45 - tolerance && pair.angleOfIntersection < 45 + tolerance)) {
                if (!validOctagonLines.contains(pair.line1)) validOctagonLines .add(pair.line1);
                if (!validOctagonLines.contains(pair.line2)) validOctagonLines .add(pair.line2);
            }
        }

        System.out.println("checking octagon form...");
        ArrayList<ArrayList<Point>> allFoundOctagons = PipelinePrototype.detectOctagonForm(validOctagonLines, image.getWidth(), image.getHeight());


        // rectangle check
        ArrayList<HoughLine> validRectangleLines = new ArrayList<>();
        for (HoughLinePair pair : pairs){
            if (pair.angleOfIntersection > 90 - tolerance && pair.angleOfIntersection < 90 + tolerance) {
                if (!validRectangleLines.contains(pair.line1)) validRectangleLines.add(pair.line1);
                if (!validRectangleLines.contains(pair.line2)) validRectangleLines.add(pair.line2);
            }
        }
        if (!allFoundOctagons.isEmpty()){
            System.out.println("octagons found - drawing octagons...");
            for (ArrayList<Point> octagon : allFoundOctagons){
                PipelinePrototype.drawForm(g, octagon);
            }
            g.dispose();
            return lineImage;
        } else {
            System.out.println("no octagon detected!");
        }

        System.out.println("checking rectangle form...");
        ArrayList<ArrayList<Point>> allFoundRectangles = PipelinePrototype.detectRectangleForm(validRectangleLines, image.getWidth(), image.getHeight());
        if (!allFoundRectangles.isEmpty()){
            System.out.println("rectangles found - drawing rectangles...");
            for (ArrayList<Point> rectangle : allFoundRectangles){
                PipelinePrototype.drawForm(g, rectangle);
            }
            g.dispose();
            return lineImage;
        } else {
            System.out.println("no rectangle detected!");
        }

        g.dispose();
        return lineImage;


        // determine sign geometry based on number of intersection angles
//        int count90 = 0;
//        int count60 = 0;
//        int count45 = 0;
//
//        for (HoughLinePair pair : pairs) {
//            if (pair.angleOfIntersection == 90) count90++;
//            else if (pair.angleOfIntersection == 60) count60++;
//            else if (pair.angleOfIntersection == 45) count45++;
//        }
//
//        int edgeCountSign = 0;
//
//        if (count45 >= 4 && count90 >= 2) {
//            edgeCountSign = 8;
//        }
//        else if (count90 >= 2 && count90 > count60) {
//            edgeCountSign = 4;
//        }
//        else if (count60 >= 2) {
//            edgeCountSign = 3;
//        } else {
//            // other
//            System.err.println("No valid geometry found!");
//        }

//        for (HoughLinePair pair : pairs){
//            int r = pair.line1.r;
//            int phi = pair.line1.phi;
//            int x1, x2, y1, y2;
//            int distance = r- diagonal;
//            double radPhi = Math.toRadians(phi);
//
//            if (phi > 45 && phi < 135){
//                x1 = 0;
//                x2 = image.getWidth();
//
//                y1 = (int) (distance / Math.sin(radPhi));
//                y2 = (int) ((distance - x2 * Math.cos(radPhi)) / Math.sin(radPhi));
//            }else {
//                y1 = 0;
//                y2 = image.getHeight();
//
//                x1 = (int) (distance / Math.cos(radPhi));
//                x2 = (int) ((distance - y2 * Math.sin(radPhi)) / Math.cos(radPhi));
//            }
//
//            g.drawLine(x1, y1, x2, y2);
//
//            r = pair.line2.r;
//            phi = pair.line2.phi;
//            distance = r- diagonal;
//            radPhi = Math.toRadians(phi);
//
//            if (phi > 45 && phi < 135){
//                x1 = 0;
//                x2 = image.getWidth();
//
//                y1 = (int) (distance / Math.sin(radPhi));
//                y2 = (int) ((distance - x2 * Math.cos(radPhi)) / Math.sin(radPhi));
//            }else {
//                y1 = 0;
//                y2 = image.getHeight();
//
//                x1 = (int) (distance / Math.cos(radPhi));
//                x2 = (int) ((distance - y2 * Math.sin(radPhi)) / Math.cos(radPhi));
//            }
//
//            g.drawLine(x1, y1, x2, y2);
//        }


        //ArrayList<Integer> directions = PipelinePrototype.getDirections(lines);

        // find valid lines based on distance to other lines and number of parallel lines in a sign
//        ArrayList<HoughLine> validLines = new ArrayList<>();
//
//        if (edgeCountSign == 3) {
//
//            for (HoughLinePair pair : pairs) {
//                if (pair.angleOfIntersection == 60){
//                    if (PipelinePrototype.isLineValid(tolerance, validLines, pair.line1, 1)) validLines.add(pair.line1);
//                    if (PipelinePrototype.isLineValid(tolerance, validLines, pair.line2, 1)) validLines.add(pair.line2);
//                    if (validLines.size() >= 3) break;
//                }
//            }
//
//        } else if (edgeCountSign == 4) {
//
//            for (HoughLinePair pair : pairs) {
//                if (pair.angleOfIntersection == 90){
//                    if (PipelinePrototype.isLineValid(tolerance, validLines, pair.line1, 2)) validLines.add(pair.line1);
//                    if (PipelinePrototype.isLineValid(tolerance, validLines, pair.line2, 2)) validLines.add(pair.line2);
//                    if (validLines.size() >= 4) break;
//                }
//            }
//        } else if (edgeCountSign == 8){
//
//            for (HoughLinePair pair : pairs) {
//                if (pair.angleOfIntersection == 45 || pair.angleOfIntersection == 90){
//                    if (PipelinePrototype.isLineValid(tolerance, validLines, pair.line1, 2)) validLines.add(pair.line1);
//                    if (PipelinePrototype.isLineValid(tolerance, validLines, pair.line2, 2)) validLines.add(pair.line2);
//                    if (validLines.size() >= 8) break;
//                }
//            }
//        }
//
//        // draw valid lines
//        for (HoughLine line : validLines){
//            int r = line.r;
//            int phi = line.phi;
//            int x1, x2, y1, y2;
//            int distance = r- diagonal;
//            double radPhi = Math.toRadians(phi);
//
//            if (phi > 45 && phi < 135){
//                x1 = 0;
//                x2 = image.getWidth();
//
//                y1 = (int) (distance / Math.sin(radPhi));
//                y2 = (int) ((distance - x2 * Math.cos(radPhi)) / Math.sin(radPhi));
//            }else {
//                y1 = 0;
//                y2 = image.getHeight();
//
//                x1 = (int) (distance / Math.cos(radPhi));
//                x2 = (int) ((distance - y2 * Math.sin(radPhi)) / Math.cos(radPhi));
//            }
//
//            g.drawLine(x1, y1, x2, y2);
//        }
//
//        g.dispose();
//
//        return lineImage;
    }

    /**
     * Draws all lines in validLines in red with Graphics2D g.
     * @param g Graphics2D
     * @param validLines ArrayList<HoughLine>
     * @param width int width of image
     * @param height int height of image
     * @param diagonal diagonal of image for r correction
     */
    private static void drawLines(Graphics2D g, ArrayList<HoughLine> validLines, int width, int height, int diagonal) {
        g.setColor(Color.RED);
        g.setStroke(new java.awt.BasicStroke(1));
        for (HoughLine line : validLines){
            int r = line.r;
            int phi = line.phi;
            int x1, x2, y1, y2;
            int distance = r- diagonal;
            double radPhi = Math.toRadians(phi);

            if (phi > 45 && phi < 135){
                x1 = 0;
                x2 = width;

                y1 = (int) (distance / Math.sin(radPhi));
                y2 = (int) ((distance - x2 * Math.cos(radPhi)) / Math.sin(radPhi));
            }else {
                y1 = 0;
                y2 = height;

                x1 = (int) (distance / Math.cos(radPhi));
                x2 = (int) ((distance - y2 * Math.sin(radPhi)) / Math.cos(radPhi));
            }

            g.drawLine(x1, y1, x2, y2);
        }
    }

    /**
     * Draws a geometric form based on vertices.
     * Fills the resulting form with white
     * @param g2d Graphics2D
     * @param vertices ArrayList<Point>
     */
    public static void drawForm(Graphics2D g2d, ArrayList<Point> vertices) {
        int numPoints = vertices.size();
        int[] xPoints = new int[numPoints];
        int[] yPoints = new int[numPoints];

        for (int i = 0; i < numPoints; i++){
            xPoints[i] = vertices.get(i).x;
            yPoints[i] = vertices.get(i).y;
        }

        // fill
        g2d.setColor(Color.WHITE);
        g2d.setStroke(new java.awt.BasicStroke(2));
        g2d.fillPolygon(xPoints, yPoints, numPoints);
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
     * @param validOctagonLines ArrayList<HoughLine>
     * @param width int width of image
     * @param height int height of image
     * @return ArrayList<ArrayList<Point>> all found octagons
     */
    private static ArrayList<ArrayList<Point>> detectOctagonForm(ArrayList<HoughLine> validOctagonLines, int width, int height) {
        int size = validOctagonLines.size();
        int diagonal = (int) Math.ceil(Math.sqrt(Math.pow(height, 2) + Math.pow(width, 2)));
        int minSideLength = 30; // TODO: abhängig von bild größe
        ArrayList<ArrayList<Point>> allFoundOctagons = new ArrayList<>();

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
                                                int diff = Math.abs(line.phi - group.get(0).phi);
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
                                        groups.sort(Comparator.comparingDouble(group -> group.get(0).phi));

                                        // check intersections
                                        HoughLine[] p1 = { groups.get(0).get(0), groups.get(0).get(1) }; // 0°
                                        HoughLine[] p2 = { groups.get(1).get(0), groups.get(1).get(1) }; // 45°
                                        HoughLine[] p3 = { groups.get(2).get(0), groups.get(2).get(1) }; // 90°
                                        HoughLine[] p4 = { groups.get(3).get(0), groups.get(3).get(1) }; // 135°

                                        ArrayList<Point> intersections = new ArrayList<>();
                                        for (int q = 0; q < 2; q++) {
                                            for (int r = 0; r < 2; r++) {
                                                intersections.add(PipelinePrototype.getIntersection(p1[q], p2[r], diagonal)); // 0° mit 45°
                                                intersections.add(PipelinePrototype.getIntersection(p2[q], p3[r], diagonal)); // 45° mit 90°
                                                intersections.add(PipelinePrototype.getIntersection(p3[q], p4[r], diagonal)); // 90° mit 135°
                                                intersections.add(PipelinePrototype.getIntersection(p4[q], p1[r], diagonal)); // 135° mit 0°
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
                                        if (isInsideImage(vertices.get(0), width, height, tolerance) &&
                                                isInsideImage(vertices.get(1), width, height, tolerance) &&
                                                isInsideImage(vertices.get(2), width, height, tolerance) &&
                                                isInsideImage(vertices.get(3), width, height, tolerance) &&
                                                isInsideImage(vertices.get(4), width, height, tolerance) &&
                                                isInsideImage(vertices.get(5), width, height, tolerance) &&
                                                isInsideImage(vertices.get(6), width, height, tolerance) &&
                                                isInsideImage(vertices.get(7), width, height, tolerance)) {

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

                        // sort into parallel groups
                        ArrayList<HoughLine> group1 = new ArrayList<>();
                        ArrayList<HoughLine> group2 = new ArrayList<>();
                        group1.add(a);

                        HoughLine[] remaining = {b, c, d};
                        int toleranz = 10;

                        for (HoughLine line : remaining) {
                            int diff = Math.abs(line.phi - a.phi);
                            if (diff > 90) diff = 180 - diff;

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
                        Point p1 = PipelinePrototype.getIntersection(h1, v1, diagonal);
                        Point p2 = PipelinePrototype.getIntersection(h1, v2, diagonal);
                        Point p3 = PipelinePrototype.getIntersection(h2, v2, diagonal);
                        Point p4 = PipelinePrototype.getIntersection(h2, v1, diagonal);

                        if (p1 == null || p2 == null || p3 == null || p4 == null) continue;

                        // check geometry
                        int tolerance = 25; //TODO: abhängig von bild größe
                        if (isInsideImage(p1, width, height, tolerance) &&
                                isInsideImage(p2, width, height, tolerance) &&
                                isInsideImage(p3, width, height, tolerance) &&
                                isInsideImage(p4, width, height, tolerance)) {

                            // check side length
                            double s1 = p1.distance(p2);
                            double s2 = p2.distance(p3);
                            double s3 = p3.distance(p4);
                            double s4 = p4.distance(p1);

                            if (s1 > minSideLength && s2 > minSideLength && s3 > minSideLength && s4 > minSideLength) {
                                double t = 0.1;
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
                    if (!PipelinePrototype.isTriangleAngles(a, b, c)){
                        continue;
                    }

                    // check intersections
                    Point pAB = PipelinePrototype.getIntersection(a, b, diagonal);
                    Point pAC = PipelinePrototype.getIntersection(a, c, diagonal);
                    Point pBC = PipelinePrototype.getIntersection(b, c, diagonal);

                    if (pAB == null || pBC == null || pAC == null) continue;

                    // check geometry
                    int tolerance = 15;
                    if (PipelinePrototype.isInsideImage(pAB, width, height, tolerance) &&
                            PipelinePrototype.isInsideImage(pBC, width, height, tolerance) &&
                            PipelinePrototype.isInsideImage(pAC, width, height, tolerance)) {

                        // check side length
                        double side1 = pAB.distance(pBC);
                        double side2 = pBC.distance(pAC);
                        double side3 = pAC.distance(pAB);

                        if (side1 >= minSideLength && side2 >= minSideLength && side3 >= minSideLength) {
                            double t = 0.1;
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

    private static boolean isInsideImage(Point pAB, int width, int height, int t) {
        return (pAB.x < (width+t) && pAB.x > (-t)) && (pAB.y < (height+t) && pAB.y > -t);
    }

    /**
     * Returns intersection point of lines a and b
     * @param a HoughLine
     * @param b HoughLine
     * @param diagonal image diagonal
     * @return Point intersection point
     */
    public static Point getIntersection(HoughLine a, HoughLine b, int diagonal) {
        double r1 = a.r - diagonal;
        double r2 = b.r - diagonal;

        double phi1 = Math.toRadians(a.phi);
        double phi2 = Math.toRadians(b.phi);

        double denominator = Math.cos(phi1) * Math.sin(phi2) - Math.sin(phi1) * Math.cos(phi2);

        // return if parallel
        if (Math.abs(denominator) < 0.0001) {
            return null;
        }

        int x = (int) Math.round((r1 * Math.sin(phi2) - r2 * Math.sin(phi1)) / denominator);
        int y = (int) Math.round((r2 * Math.cos(phi1) - r1 * Math.cos(phi2)) / denominator);

        return new Point(x, y);
    }

    private static boolean isRectangleAngles(HoughLine a, HoughLine b, HoughLine c, HoughLine d) {
        int[] phi = {a.phi, b.phi, c.phi, d.phi};
        Arrays.sort(phi);

        int tolerance = 5;

        int diff1 = phi[1] - phi[0];
        int diff2 = phi[2] - phi[1];
        int diff3 = phi[3] - phi[2];
        int diff4 = 180 - (phi[3] - phi[0]);

        return Math.abs(diff1 - 90) <= tolerance &&
                Math.abs(diff2 - 90) <= tolerance &&
                Math.abs(diff3 - 90) <= tolerance &&
                Math.abs(diff4 - 90) <= tolerance;
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

        int tolerance = 5;

        int diff1 = phi[1] - phi[0];
        int diff2 = phi[2] - phi[1];
        int diff3 = 180 - (phi[2] - phi[0]);

        return Math.abs(diff1 - 60) <= tolerance &&
                Math.abs(diff2 - 60) <= tolerance &&
                Math.abs(diff3 - 60) <= tolerance;
    }

    /**
     * Checks if a line has a minimumLength of a segment and a maximumAllowedGap between segments.
     * @param image BufferedImage
     * @param line HoughLine
     * @param minLength minimum length of segment
     * @param maxAllowedGap maximum allowed gap between segments
     * @return boolean if line is solid
     */
    private static boolean isLineSolid(BufferedImage image, HoughLine line, int minLength, int maxAllowedGap) {
        int width = image.getWidth();
        int height = image.getHeight();
        int diagonal = (int) Math.ceil(Math.sqrt(height*height + width*width));

        int distance = line.r - diagonal;
        double radPhi = Math.toRadians(line.phi);

        int longestChain = 0;
        int currentChain = 0;
        int currentGap = 0;

        if (line.phi > 45 && line.phi < 135){
            // more horizontal
            for (int x = 0; x < width; x++){
                int y = (int) ((distance - x * Math.cos(radPhi)) / Math.sin(radPhi));

                if (y >= 0 && y < height){
                    boolean isEdge = false;
                    for (int dy = -1; dy <= 1; dy++) {
                        if (y + dy >= 0 && y + dy < height) {
                            if (GlobalHelperFunctions.calculateGrayValueFromRGB(image.getRGB(x, y + dy)) == 255) {
                                isEdge = true;
                                break;
                            }
                        }
                    }
                    if (isEdge){
                        currentChain++;
                        currentGap = 0;
                    }else{
                        currentGap++;
                        if (currentGap > maxAllowedGap){
                            if (currentChain > longestChain) longestChain = currentChain;
                            currentChain = 0;
                        }

                    }
                }
            }
        } else {
            // more vertical
            for (int y = 0; y < height; y++) {
                int x = (int) ((distance - y * Math.sin(radPhi)) / Math.cos(radPhi));

                if (x >= 0 && x < width) {
                    boolean isEdge = false;
                    for (int dx = -1; dx <= 1; dx++) {
                        if (x + dx >= 0 && x + dx < width) {
                            if (GlobalHelperFunctions.calculateGrayValueFromRGB(image.getRGB(x + dx, y)) == 255) {
                                isEdge = true;
                                break;
                            }
                        }
                    }
                    if (isEdge) {
                        currentChain++;
                        currentGap = 0;
                    } else {
                        currentGap++;
                        if (currentGap > maxAllowedGap) {
                            if (currentChain > longestChain) longestChain = currentChain;
                            currentChain = 0;
                        }
                    }
                }
            }
        }

        if (currentChain > longestChain) longestChain = currentChain;
        return longestChain >= minLength;
    }

    /**
     * Determines validity of a line based on parallel lines and distance between lines.
     * Calls {@link PipelinePrototype#areLinesFarEnough(HoughLine, HoughLine, int)}
     * @param tolerance tolerance for angle difference
     * @param validLines list of valid lines
     * @param line HoughLine
     * @param maxParallelAllowed maximum allowed parallel lines
     * @return boolean if line is valid
     */
    private static boolean isLineValid(int tolerance, ArrayList<HoughLine> validLines, HoughLine line, int maxParallelAllowed) {
        int parallelCount = 0;
        for (HoughLine houghLine : validLines) {
            int diff = Math.abs(houghLine.phi - line.phi);
            if (diff > 90) {
                diff = 180 - diff;
            }

            if (diff <= tolerance){
                if (!PipelinePrototype.areLinesFarEnough(houghLine, line, 50)){
                    return false;
                }
                parallelCount++;
            }
        }

        if (parallelCount < maxParallelAllowed){
            if (!validLines.contains(line)) return true;
        }

        return false;
    }

    /**
     * Checks if distance between two lines is greater than minPixelDistance.
     * @param line1 first line
     * @param line2 second line
     * @param minPixelDistance minimum distance
     * @return boolean if lines are far enough apart
     */
    private static boolean areLinesFarEnough(HoughLine line1, HoughLine line2, int minPixelDistance){
        int diffR = Math.abs(line1.r - line2.r);
        return diffR >= minPixelDistance;
    }

    /**
     * Calculates the difference of two angles.
     * Takes into account that 179° and 1° differ by 2°
     * @param angle1 first angle
     * @param angle2 second angle
     * @return difference between angles 1 and 2
     */
    private static int getAngleDiff(int angle1, int angle2) {
        int diff = Math.abs(angle1 - angle2);
        return (diff > 90) ? (180 - diff) : diff;
    }

    /**
     * Calculates amount of directions of lines array.
     * Checks each angle and puts different angles in a list.
     * @param lines array of lines
     * @return Size of list
     */
    private static ArrayList<Integer> getDirections(ArrayList<HoughLine> lines) {
        int lineCount = Math.min(8, lines.size());
        ArrayList<Integer> angleGroups = new ArrayList<>();
        int tolerance = 15;

        for (int i = 0; i < lineCount; i++) {
            int currentAngle = lines.get(i).phi;
            boolean isPartOfGroup = false;

            for (int angle : angleGroups) {
                int diff = PipelinePrototype.getAngleDiff(currentAngle, angle);

                if (diff <= tolerance) {
                    isPartOfGroup = true;
                    break;
                }
            }

            if (!isPartOfGroup) {
                angleGroups.add(currentAngle);
            }
        }

        return angleGroups;
    }

    /**
     * Performs the Hough transformation on an image.
     * Calls {@link GlobalHelperFunctions#calculateGrayValueFromRGB(int)}
     * @param image BufferedImage to perform on
     * @return int[][] Hough room matrix
     */
    private static int[][] houghTransformation(BufferedImage image){
        int diagonal = (int) Math.ceil(Math.sqrt(Math.pow(image.getHeight(), 2) + Math.pow(image.getWidth(), 2)));
        int[][] accumulator = new int[180][2*diagonal];

        for (int x = 0; x < image.getWidth(); x++){
            for (int y = 0; y < image.getHeight(); y++){
                int grayValue = GlobalHelperFunctions.calculateGrayValueFromRGB(image.getRGB(x, y));
                if (grayValue == 255){
                    for (int phi = 0; phi < 180; phi++){
                        int r = (int) (x * Math.cos(Math.toRadians(phi)) + y * Math.sin(Math.toRadians(phi)));
                        r += diagonal;
                        accumulator[phi][r] += 1;
                    }
                }
            }
        }
        return accumulator;
    }

    /**
     * Calculates the centre of the sign.
     * Calls {@link PipelinePrototype#findCoordsOfSign(BufferedImage)}
     * @param image BufferedImage to calculate centre of
     * @return Point with centre coordinates
     */
    private static Point calculateCenterPointOfSign(BufferedImage image) {
        int[] coordsOfSign = PipelinePrototype.findCoordsOfSign(image);
        int centerX = (coordsOfSign[0] + coordsOfSign[1]) / 2;
        int centerY = (coordsOfSign[2] + coordsOfSign[3]) / 2;

        return new Point(centerX, centerY);
    }

    /**
     * Rotates a given BufferedImage by 45 degrees around the centerpoint of the sign.
     * Calls {@link PipelinePrototype#findCoordsOfSign(BufferedImage)} to get coordinates for the centre point
     * Calls {@link RotatedImage#rotateImageBackwardMapping(BufferedImage, Point, int)}
     * Checks for a square shape with tolerance.
     * @param image BufferedImage to rotate 45 degrees and check
     * @param squareTolerancePercent percent of tolerance for square
     * @param isSquare boolean array to fake call by reference for boolean value
     * @return 45 degrees rotated BufferedImage
     */
    private static BufferedImage rotateImageAndCheckGeometry(BufferedImage image, double squareTolerancePercent, boolean[] isSquare) {
        // rotate image
        int[] coordsOfSign = PipelinePrototype.findCoordsOfSign(image);
        int xMin = coordsOfSign[0];
        int xMax = coordsOfSign[1];
        int yMin = coordsOfSign[2];
        int yMax = coordsOfSign[3];
        int centerX = (xMin + xMax) / 2;
        int centerY = (yMin + yMax) / 2;
        BufferedImage rotatedImage = RotatedImage.rotateImageBackwardMapping(image, new Point(centerX, centerY), 45);

        // count white pixels
        int pixelCount = 0;
        for (int x = 0; x < image.getWidth(); x++) {
            for (int y = 0; y < image.getHeight(); y++) {
                int gray = GlobalHelperFunctions.calculateGrayValueFromRGB(image.getRGB(x, y));
                if (gray == 255) {
                    pixelCount++;
                }
            }
        }
        double density = pixelCount / (double) (image.getWidth() * image.getHeight());

        double distanceX = xMax - xMin;
        double distanceY = yMax - yMin;

        int minAllowedSize = 15;

        if (distanceX < minAllowedSize || distanceY < minAllowedSize) {
            isSquare[0] = false;
        } else if (density < 0.15) {
            isSquare[0] = false;
        } else {
            double ratio = distanceX / distanceY;
            isSquare[0] = (ratio < (1 + squareTolerancePercent) && ratio > (1 - squareTolerancePercent));
        }

        return rotatedImage;
    }

    /**
     * Performs preprocessing on given BufferedImage:
     * Calls {@link Convolution#gaussianLowPass(BufferedImage, int)}
     * Calls {@link ImageManipulation#histogramEqualization(BufferedImage)}
     * Calls {@link Convolution#sobelFilter(BufferedImage, int)}
     * Calls {@link ImageManipulation#equidensityFirstOrderGrayImageCustomBounds(BufferedImage, int, int, int, int, int)}
     * Calls {@link ColorManipulation#negative(BufferedImage)}
     * @param image BufferedImage to preprocess
     * @return preprocessed BufferedImage
     */
    private static BufferedImage imagePreprocessing(BufferedImage image){

        // 2. lowpass
        BufferedImage lowpass = Convolution.gaussianLowPass(image, 5);
        //System.out.println("Successfully calculated Gauß lowpass");
        ImageIO.displayImage(lowpass);

        // 3. histogram equalization
        //BufferedImage equalizedHistogram = ImageManipulation.histogramEqualization(lowpass);
        //System.out.println("Successfully calculated histogram equalization");
        //ImageIO.displayImage(equalizedHistogram);

        // sobel
        BufferedImage sobel = Convolution.sobelFilter(lowpass, 3);
        //System.out.println("Successfully calculated Sobel filter");
        ImageIO.displayImage(sobel);

        // 4. equidensity
        BufferedImage equidensity = ImageManipulation.equidensityFirstOrderGrayImageCustomBounds(sobel, 50, 200, 255, 0, 0);
        //System.out.println("Successfully calculated equidensity");
        ImageIO.displayImage(equidensity);

        // closing with dilation and erosion
        boolean[][] mask = new boolean[3][3];
        for (boolean[] bool : mask){
            Arrays.fill(bool, true);
        }
        BufferedImage erosion = MorphologicalOperations.erosion(equidensity, mask, 1);
        ImageIO.displayImage(erosion);

        BufferedImage dilation = MorphologicalOperations.dilation(erosion, mask, 1);
        ImageIO.displayImage(dilation);

        // negative
        BufferedImage negative = ColorManipulation.negative(dilation);
        ImageIO.displayImage(negative);

        return negative;
    }

    /**
     * Performs region growing algorithm for a single region.
     * Looks for all neighbouring pixels that share the colour 255 white and have yet to be sorted into a region.
     * Marks the neighbouring white pixel with currentRegion.
     * Calls itself recursively with each neighbouring pixel in the region.
     * @param image BufferedImage to search in
     * @param regions pixelarray marking a region for each pixel
     * @param currentRegion current region
     * @param startPoint point to start from
     */
    private static void findAllConnectedNeighbours(BufferedImage image, int[][] regions, int currentRegion, Point startPoint) {
        Queue<Point> queue = new LinkedList<>();

        // start point
        regions[startPoint.x][startPoint.y] = currentRegion;
        queue.add(startPoint);

        while (!queue.isEmpty()) {
            Point currentPoint = queue.poll();

            // check 8 neighbours
            for (int x = -1; x <= 1; x++) {
                for (int y = -1; y <= 1; y++) {
                    if (x == 0 && y == 0) continue;

                    int neighbourX = currentPoint.x + x;
                    int neighbourY = currentPoint.y + y;

                    // check bounds
                    if (neighbourX < 0 || neighbourX >= image.getWidth() || neighbourY < 0 || neighbourY >= image.getHeight()) {
                        continue;
                    }

                    int neighbourValue = GlobalHelperFunctions.calculateGrayValueFromRGB(image.getRGB(neighbourX, neighbourY));

                    if (neighbourValue == 255 && regions[neighbourX][neighbourY] == 0) {
                        regions[neighbourX][neighbourY] = currentRegion;
                        queue.add(new Point(neighbourX, neighbourY));
                    }
                }
            }
        }
    }

    /**
     * Fills the interior of a region with white. Operates recursively on a binary image and fills all black with white.
     * Stops when reaching white pixels or pixels that have been visited.
     * Calls itself recursively with each black neighbouring pixel that is yet unvisited.
     * @param image binary BufferedImage to fill interior in
     * @param visited array of visited pixels
     * @param startPoint point to start from
     * @return BufferedImage with filled interior
     */
    private static BufferedImage fillInterior(BufferedImage image, boolean[][] visited, Point startPoint) {
        Queue<Point> queue = new LinkedList<>();

        visited[startPoint.x][startPoint.y] = true;
        image.setRGB(startPoint.x, startPoint.y, 0xFFFFFFFF);
        queue.add(startPoint);

        // 4 cardinal directions
        int[] dx = {0, 0, -1, 1};
        int[] dy = {-1, 1, 0, 0};

        while (!queue.isEmpty()) {
            Point currentPoint = queue.poll();

            for (int i = 0; i < 4; i++) {
                int neighbourX = currentPoint.x + dx[i];
                int neighbourY = currentPoint.y + dy[i];

                // check bounds
                if (neighbourX < 0 || neighbourX >= image.getWidth() || neighbourY < 0 || neighbourY >= image.getHeight()) {
                    continue;
                }

                int neighbourValue = GlobalHelperFunctions.calculateGrayValueFromRGB(image.getRGB(neighbourX, neighbourY));

                if (neighbourValue == 0 && !visited[neighbourX][neighbourY]) {
                    visited[neighbourX][neighbourY] = true;
                    image.setRGB(neighbourX, neighbourY, 0xFFFFFFFF);

                    queue.add(new Point(neighbourX, neighbourY));
                }
            }
        }
        return image;
    }

    /**
     * Sorts a binary image into regions.
     * Checks for the biggest region and produces a BufferedImage with that region highlighted.
     * Calls {@link #findAllConnectedNeighbours(BufferedImage, int[][], int, Point)} for region growing and
     * {@link #fillInterior(BufferedImage, boolean[][], Point)} for filling the resulting biggest region.
     * Introduces a black border beforehand to mitigate false positives.
     * @param image BufferedImage to look for sign geometry in
     * @return BufferedImage with sign geometry highlighted
     */
    private static BufferedImage lookForSignGeometry(BufferedImage image) {

        //make edge black
        int width = image.getWidth();
        int height = image.getHeight();
        int border = 5;

        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                if (x < border || x >= width - border || y < border || y >= height - border) {
                    image.setRGB(x, y, 0xFF000000);
                }
            }
        }

        // failsafe for entirely black images
        int signPixelCount = 0;
        for (int x = 0; x < image.getWidth(); x++) {
            for (int y = 0; y < image.getHeight(); y++) {
                int gray = GlobalHelperFunctions.calculateGrayValueFromRGB(image.getRGB(x, y));
                if (gray == 255) {
                    signPixelCount++;
                }
            }
        }

        if (signPixelCount < 80) {
            return image;
        }

        // sort into regions
        int[][] regions = new int[width][height];
        int currentRegion = 1;

        for (int x = 0; x < width; x++){
            for (int y = 0; y < height; y++){
                int value = GlobalHelperFunctions.calculateGrayValueFromRGB(image.getRGB(x, y));

                if (value == 255 && regions[x][y] == 0){
                    // new region
                    PipelinePrototype.findAllConnectedNeighbours(image, regions, currentRegion, new Point(x, y));
                    currentRegion++;
                }
            }
        }

        // count region sizes
        int[] regionSizes = new int[currentRegion];

        for (int x = 0; x < width; x++){
            for (int y = 0; y < height; y++){
                int region = regions[x][y];
                if (region > 0){
                    regionSizes[region]++;
                }
            }
        }

        // determine largest region
        int maxPixelAmount = 0;
        int largestRegion = 0;

        for (int i = 1; i < regionSizes.length; i++) {
            if (regionSizes[i] > maxPixelAmount) {
                maxPixelAmount = regionSizes[i];
                largestRegion = i;
            }
        }

        // new image with only biggest region
        BufferedImage regionImage = new BufferedImage(width, height, BufferedImage.TYPE_BYTE_GRAY);

        for (int x = 0; x < width; x++) {
            for (int y = 0; y < height; y++) {

                if (regions[x][y] == largestRegion) {
                    regionImage.setRGB(x, y, 0xFFFFFFFF);
                } else {
                    regionImage.setRGB(x, y, 0xFF000000);
                }
            }
        }

        // calculate mid-point of region
        Point centerPoint = PipelinePrototype.calculateCenterPointOfSign(regionImage);

        // fill the found region
        boolean[][] visited = new boolean[width][height];
        BufferedImage filled = PipelinePrototype.fillInterior(regionImage, visited, centerPoint);

        return filled;
    }

    /**
     * Checks a given BufferedImage for outermost white pixels in each cardinal direction and returns their coordinates.
     * @param image BufferedImage to find coords of sign in
     * @return int[] with xMin, xMax, yMin, yMax
     */
    private static int[] findCoordsOfSign(BufferedImage image){
        // xMin, xMax, yMin, yMax
        int[] values = {image.getWidth() - 1, 0, image.getHeight() - 1, 0};

        for (int x = 0; x < image.getWidth(); x++){
            for (int y = 0; y < image.getHeight(); y++){
                int rgb = image.getRGB(x, y);
                int grayValue = GlobalHelperFunctions.calculateGrayValueFromRGB(rgb);

                if (grayValue == 255){
                    if (x < values[0]){
                        values[0] = x;
                    }
                    if (x > values[1]){
                        values[1] = x;
                    }
                    if (y < values[2]){
                        values[2] = y;
                    }
                    if (y > values[3]){
                        values[3] = y;
                    }
                }
            }
        }

        return values;
    }

    /**
     * Crops a given BufferedImage using a BufferedImage as a mask.
     * Copies only pixels that are 255 white in the mask.
     * @param image BufferedImage to crop
     * @param mask BufferedImage to use as mask
     * @return BufferedImage cropped with mask
     */
    public static BufferedImage cropAndMaskSign(BufferedImage image, BufferedImage mask) {
        int[] coordsOfSign = PipelinePrototype.findCoordsOfSign(mask);
        int xMin = coordsOfSign[0];
        int xMax = coordsOfSign[1];
        int yMin = coordsOfSign[2];
        int yMax = coordsOfSign[3];

        // size of mask
        int cropWidth = xMax - xMin + 1;
        int cropHeight = yMax - yMin + 1;

        BufferedImage croppedSign = new BufferedImage(cropWidth, cropHeight, image.getType());

        // copy pixels that are white in mask
        for (int y = 0; y < cropHeight; y++) {
            for (int x = 0; x < cropWidth; x++) {

                int origX = xMin + x;
                int origY = yMin + y;

                int maskValue = GlobalHelperFunctions.calculateGrayValueFromRGB(mask.getRGB(origX, origY));

                if (maskValue == 255) {
                    int rgb = image.getRGB(origX, origY);
                    croppedSign.setRGB(x, y, rgb);
                } else {
                    croppedSign.setRGB(x, y, 0xFF000000);
                }
            }
        }

        return croppedSign;
    }
}
