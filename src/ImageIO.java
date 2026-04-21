import javax.swing.*;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Scanner;

public class ImageIO {

    BufferedImage image;
    BufferedImage imagePPM;

    File file;
    String filename;

    ImageIO() {}

    /**
     * Reads a file as a BufferedImage.
     * Can read any image format.
     * @param filename name of file to be read
     * @return BufferedImage read file
     */
    BufferedImage readImage(String filename) {
        //Datei lesen
        this.filename = filename;
        this.file = new File(this.filename);

        // Bilddaten im RAM
        BufferedImage readImage; //.read erkennt format automatisch
        try {
            readImage = javax.imageio.ImageIO.read(this.file);
            System.out.println("Image loaded successfully!");
            this.image = readImage;
            return readImage;

        } catch (IOException e) {
            System.err.println("Error reading image file " + this.filename + e.getMessage());
            return null;
        }
    }

    /**
     * Reads a file that has the ppm format.
     * Disregards all comments and whitespaces and writes the data into a BufferedImage
     * @param file the path of the ppm file to be read
     * @return BufferedImage the ppm image
     */
    BufferedImage readPPM(File file) {
        Scanner scanner;
        try {
            scanner = new Scanner(file);
        } catch (FileNotFoundException e) {
            System.err.println("Source not Found!" + e.getMessage());
            return null;
        }

        scanner.useDelimiter("(\\s+|#.*\\n)+"); // remove comments and whitespace

        if (!scanner.nextLine().equals("P3")){
            return null;
        }

        int width = scanner.nextInt();
        int height = scanner.nextInt();
        int maxVal = scanner.nextInt();

        BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);

        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                int r = scanner.nextInt();
                int g = scanner.nextInt();
                int b = scanner.nextInt();
                int rgb = (r << 16) | (g << 8) | b;
                image.setRGB(x, y, rgb);
            }
        }

        this.imagePPM = image;
        return image;
    }

    /**
     * Converts a given BufferedImage with any format to ppm format.
     * Writes the ppm image into a new file called 'filenameOutput'.
     * Reads the ppm file with {@link #readPPM(File)}.
     * @param input BufferedImage to be converted
     * @param filenameOutput name of ppm file
     * @return BufferedImage in ppm format
     * @throws IOException if an I/O error occurs while writing the file
     */
    BufferedImage convertToPPM(BufferedImage input, String filenameOutput) throws IOException {
        FileWriter writer = new FileWriter(filenameOutput);

        int width = input.getWidth();
        int height = input.getHeight();

        // header
        writer.write("P3\n" + width + " " + height + "\n" + "255\n");

        //data
        int columnCounter = 0;
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                int rgb = input.getRGB(x, y);

                int r = (rgb >> 16) & 0xff;
                int g = (rgb >> 8) & 0xff;
                int b = rgb & 0xff;

                writer.write(" " + r + " " + g + " " + b);
                columnCounter = columnCounter + 12;

                if (columnCounter >= 70){
                    writer.write("\n");
                    columnCounter = 0;
                }
            }
        }

        writer.close();

        return readPPM(new File(filenameOutput));
    }

    /**
     * Reads an image and converts it to ppm format
     * @param filename file name of image
     * @param filenamePPM file name of ppm
     * @return BufferedImage ppm
     */
    BufferedImage readImageToPPM(String filename, String filenamePPM) {
        this.readImage(filename);
        try {
            this.convertToPPM(this.image, filenamePPM);
        } catch (IOException e) {
            System.err.println("Unable to create image file " + filenamePPM);
        }
        return this.imagePPM;
    }

    /**
     * Displays BufferedImages inside JFrames.
     * JFrames are wrapped to the images.
     * Can display multiple images each inside the JFrame.
     * Displays the JFrame centered on the screen.
     * @param images the BufferedImages to be displayed
     */
    void displayImage(BufferedImage... images) {
        JFrame frame = new JFrame("Bildanzeige");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setLayout(new GridLayout(1, images.length));

        for (BufferedImage image : images) {
            // Bild anzeigen
            JLabel label = new JLabel(new ImageIcon(image));
            label.setBorder(BorderFactory.createLineBorder(Color.BLACK));
            frame.add(label);
        }

        frame.pack(); //Fester größe an Bildgröße anpassen
        frame.setLocationRelativeTo(null); // zentrieren auf dem bildschirm
        frame.setVisible(true);
    }

    /**
     * Generates a ppm file 'output.ppm'.
     * Used for testing.
     * @throws IOException if file is a directory
     */
    void generatePPM() throws IOException {
        FileWriter writer = new FileWriter("output.ppm");
        // header
        writer.write("""
            P3
            # Maximilian was here
            255 255
            255
            """);

        //data
        int columnCounter = 0;
        for (int r = 0; r < 255; r++){
            for (int c = 0; c < 255; c++){
                writer.write(" " + c + " " + (255-c) +" "+ r);


                columnCounter = columnCounter + 12;
                if (columnCounter >= 70){
                    writer.write("\n");
                    columnCounter = 0;
                }
            }
        }
        writer.close();
    }

    /**
     * Generates a color test ppm file called 'colorTest.ppm'.
     * Reads the file using {@link #readPPM(File)}.
     * @return BufferedImage ppm
     */
    BufferedImage generateColorTestPPM() {
        FileWriter writer;
        try {
            writer = new FileWriter("colorTest.ppm");
            writer.write("""
                P3
                3 3
                255
                 255   0   0     0 255   0     0   0 255
                 255 255   0   255   0 255     0 255 255
                 255 255 255     0   0   0   128 128 128""");
            writer.close();
            return this.readPPM(new File("colorTest.ppm"));
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Generates a checkered test ppm file called 'checkeredTest.ppm'.
     * Reads the file using {@link #readPPM(File)}.
     * @return BufferedImage ppm
     */
    BufferedImage generateCheckeredTestPPM(){
        FileWriter writer;
        try {
            writer = new FileWriter("checkeredTest.ppm");
            writer.write("""
                P3
                4 4
                255
                   0   0   0   255 255 255     0   0   0   255 255 255
                 255 255 255     0   0   0   255 255 255   0   0   0
                   0   0   0   255 255 255     0   0   0   255 255 255
                 255 255 255     0   0   0   255 255 255   0   0   0""");
            writer.close();
            return this.readPPM(new File("checkeredTest.ppm"));
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Copies a given BufferedImage in ppm format ({@link #convertToPPM(BufferedImage, String)}, {@link #readPPM(File)})
     * @param ppm BufferedImage to be copied
     * @param filenameOutput filename of copy
     * @return BufferedImage copy or null
     */
    BufferedImage copyPPM(BufferedImage ppm, String filenameOutput){
        try {
            File newPPM = new File(filenameOutput);
            convertToPPM(ppm, filenameOutput);
            return readPPM(newPPM);
        } catch (IOException e) {
            System.err.println("Error copying to ppm file " + filenameOutput);
            return null;
        }
    }

    /**
     * Copies a given BufferedImage ({@link #readImage(String)})
     * @param image BufferedImage to be copied
     * @param filenameOutput filename of copy
     * @return BufferedImage copy or null
     */
    BufferedImage copyImage(BufferedImage image, String filenameOutput){
        File newImage =  new File(filenameOutput);
        try {
            javax.imageio.ImageIO.write(image, "png", newImage);
            return this.readImage(filenameOutput);
        } catch (IOException e) {
            System.err.println("Error copying to image file " + filenameOutput);
            return null;
        }
    }
}
