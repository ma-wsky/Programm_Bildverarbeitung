package main.java.mawsky.trafficsign.utils;

import java.awt.*;
import java.util.ArrayList;

public class GlobalHelperFunctions {

    public static int calculateGrayValueFromRGB(int rgb){
        int r = (rgb >> 16) & 0xff;
        int g = (rgb >> 8) & 0xff;
        int b = rgb & 0xff;
        return (r + g + b) / 3;
    }

    public static double[] convertRGBToHSV(int rgb){
        // norm r, g, b
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

        return new double[]{h, s, v};
    }

    public static double[] calculateCenterCoordinates(ArrayList<Point> points){
        double sumX = 0;
        double sumY = 0;
        for (Point p : points) {
            sumX += p.x;
            sumY += p.y;
        }

        double centerX = sumX / points.size();
        double centerY = sumY / points.size();
        return new double[]{centerX, centerY};
    }}
