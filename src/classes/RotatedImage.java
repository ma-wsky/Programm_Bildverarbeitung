package classes;

import java.awt.*;
import java.awt.image.BufferedImage;

public class RotatedImage {

    /**
     * Rotates a given BufferedImage around a given pivot point for 'degrees'°.
     * Uses backward mapping to determine the color each rotated pixel had in image.
     * Uses nearest neighbour to interpolate pixel color.
     * Shifts the pivot point and new center to the middle of the pixel.
     * @param image BufferedImage image to be rotated
     * @param pivotPoint Point pivot point of rotation
     * @param degrees number of degrees image is to be rotated
     * @return BufferedImage rotated image
     */
    public static BufferedImage rotateImageBackwardMapping(BufferedImage image, Point pivotPoint, int degrees) {

        double angle = Math.toRadians(degrees);
        double cos = Math.cos(angle);
        double sin = Math.sin(angle);

        int newWidth = (int) (Math.abs(image.getWidth() * cos) + Math.abs(image.getHeight()) * sin);
        int newHeight = (int) (Math.abs(image.getWidth() * sin) + Math.abs(image.getHeight() * cos));
        BufferedImage rotatedImage = new BufferedImage(newWidth, newHeight, image.getType());

        double xPivot = pivotPoint.x - 0.5;
        double yPivot = pivotPoint.y - 0.5;

        double xNewCenter = (rotatedImage.getWidth() / 2.0)  - 0.5;
        double yNewCenter = (rotatedImage.getHeight() / 2.0) - 0.5;

        for (int x = 0; x < rotatedImage.getWidth(); x++) {
            for (int y = 0; y < rotatedImage.getHeight(); y++) {
                double newX = x -  xNewCenter;
                double newY = y -  yNewCenter;

                int origX = (int) Math.round(cos * newX + sin * newY + xPivot); // Nearest Neighbour due to Math.round
                int origY = (int) Math.round(-sin * newX + cos * newY + yPivot);

                if (origX >= 0 && origX < image.getWidth() &&
                        origY >= 0 && origY < image.getHeight()) {
                    rotatedImage.setRGB(x, y, image.getRGB(origX, origY));
                }
            }
        }

        return rotatedImage;
    }

    /**
     * Rotates a given BufferedImage around a given pivot point for 'degrees'°.
     * Uses forward mapping to determine the position each pixel takes in rotated image.
     * @param image BufferedImage to be rotated
     * @param pivotPoint Point pivot point of rotation
     * @param degrees number of degrees image is to be rotated
     * @return BufferedImage rotated image
     */
    public static BufferedImage rotateImageForwardMapping(BufferedImage image, Point pivotPoint, int degrees) {
        BufferedImage rotatedImage = new BufferedImage(image.getHeight(), image.getWidth(), image.getType());

        double angle = Math.toRadians(degrees);
        double cos = Math.cos(angle);
        double sin = Math.sin(angle);

        int xPivot = pivotPoint.x;
        int yPivot = pivotPoint.y;

        double xNewCenter = rotatedImage.getWidth() / 2.0;
        double yNewCenter = rotatedImage.getHeight() / 2.0;

        for (int x = 0; x < image.getWidth(); x++) {
            for (int y = 0; y < image.getHeight(); y++) {

                // translate relative to pivot point
                double transX = x -  xPivot;
                double transY = y -  yPivot;

                // rotate
                int newX = (int) Math.floor(transX * cos - transY * sin + xNewCenter);
                int newY = (int) Math.floor(transX * sin + transY * cos + yNewCenter);

                if (newX >= 0 && newX < rotatedImage.getWidth() &&
                        newY >= 0 && newY < rotatedImage.getHeight()) {
                    rotatedImage.setRGB(newX, newY, image.getRGB(x, y));
                }
            }
        }

        return rotatedImage;
    }

    /**
     * Rotates a given image for multiples of 90° specified with 'degrees'.
     * Flips the images axis to fit rotation angle. No rearranging of pixels.
     * @param image BufferedImage to be rotated
     * @param degrees number of degrees image is to be rotated
     * @return BufferedImage rotated image
     */
    public static BufferedImage rotateImage90s(BufferedImage image, int degrees) {

        if (degrees == 360){
            return image;
        }

        BufferedImage rotatedImage = null;

        rotatedImage = switch (degrees) {
            case 90, 270 -> new BufferedImage(image.getHeight(), image.getWidth(), image.getType());
            case 180 -> new BufferedImage(image.getWidth(), image.getHeight(), image.getType());
            default -> rotatedImage;
        };

        for (int x = 0; x < image.getWidth(); x++) {
            for (int y = 0; y < image.getHeight(); y++) {
                int rgb = image.getRGB(x, y);

                int newX = y;
                int newY = x;

                switch (degrees) {
                    case 90: {
                        newX = (image.getHeight()-1) - y;
                        break;
                    }
                    case 270: {
                        newY = (image.getWidth()-1) - x;
                        break;
                    }
                    case 180: {
                        newX = (image.getWidth()-1) - x;
                        newY = (image.getHeight()-1) - y;
                        break;
                    }
                }

                if (rotatedImage != null) {
                    rotatedImage.setRGB(newX, newY, rgb);
                }
            }
        }

        return rotatedImage;
    }
}
