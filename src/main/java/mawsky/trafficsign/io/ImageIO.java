package main.java.mawsky.trafficsign.io;

import javax.swing.*;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.awt.image.ColorModel;
import java.awt.image.WritableRaster;
import java.io.File;
import java.io.IOException;

public class ImageIO {

    /**
     * Reads a file as a BufferedImage.
     * Can read any image format.
     * @param filename name of file to be read
     * @return BufferedImage read file
     */
    public static BufferedImage readImage(String filename) {
        // read file
        File file = new File(filename);

        // load data into ram
        BufferedImage readImage;
        try {
            readImage = javax.imageio.ImageIO.read(file);
            return readImage;

        } catch (IOException ex) {
            SwingUtilities.invokeLater(() -> JOptionPane.showMessageDialog(null, "Error reading image file " + filename + ex.getMessage(), "Pipeline Fehler", JOptionPane.ERROR_MESSAGE));
            return null;
        }
    }

    /**
     * Displays BufferedImages inside JFrames.
     * JFrames are wrapped to the images.
     * Can display multiple images each inside the JFrame.
     * Displays the JFrame centered on the screen.
     * @param images the BufferedImages to be displayed
     */
    public static void displayImage(BufferedImage... images) {
        JFrame frame = new JFrame("Bildanzeige");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setLayout(new GridLayout(1, images.length));

        for (BufferedImage image : images) {
            if (image == null) {
                System.err.println("Image is null!");
                return;
            }
            // display image
            JLabel label = new JLabel(new ImageIcon(image));
            label.setBorder(BorderFactory.createLineBorder(Color.BLACK));
            frame.add(label);
        }

        frame.pack(); // fit window size to image size
        frame.setLocationRelativeTo(null); // center on screen
        frame.setVisible(true);
    }

    /**
     * Returns a copy of the given BufferedImage
     * @param image BufferedImage to be copied
     * @return copy of BufferedImage
     */
    public static BufferedImage copyBufferedImage(BufferedImage image){
        ColorModel cm = image.getColorModel();
        boolean alpha = cm.isAlphaPremultiplied();
        WritableRaster raster = image.copyData(null);
        return new BufferedImage(cm, raster, alpha, null);
    }
}
