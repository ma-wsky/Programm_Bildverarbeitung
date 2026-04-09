import javax.imageio.ImageIO;
import javax.swing.*;
import java.awt.*;
import java.awt.image.BufferedImage;

void main() {

    try {
        //Datei lesen
        File inputFile = new File("pics/Bild_A.jpg");

        // Bilddaten im RAM
        BufferedImage original = ImageIO.read(inputFile); //.read erkennt format automatisch

        if (original != null) {
            System.out.println("Bild erfolgreich geladen!");
        } else {
            System.out.println("Format wird nicht unterstützt.");
        }

        // Kopie erstellen
        assert original != null;
        BufferedImage grayScale = copyImage(original);
        BufferedImage negative = copyImage(original);
        BufferedImage negativeGray = copyImage(original);

        // Manipulation
        grayScale(grayScale);
        negative(negative);
        grayScale(negativeGray);
        negative(negativeGray);

        // Anzeige
        displayImage(original, negative, grayScale, negativeGray);

    } catch (IOException e) {
        System.err.println("Fehler beim Lesen der Datei: " + e.getMessage());
    }

}

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

public static BufferedImage copyImage(BufferedImage image) {
    BufferedImage newImage = new BufferedImage(image.getWidth(), image.getHeight(), image.getType());
    java.awt.Graphics g = newImage.getGraphics();
    g.drawImage(image, 0, 0, null);
    g.dispose();
    return newImage;
}

void grayScale(BufferedImage image) {
    for (int x = 0; x < image.getWidth(); x++) {
        for (int y = 0; y < image.getHeight(); y++) {
            int rgb = image.getRGB(x, y);

            int a = (rgb >> 24) & 0xff;
            int r = (rgb >> 16) & 0xff;
            int g = (rgb >> 8) & 0xff;
            int b = rgb & 0xff;

            int gray = (r + g + b) / 3;

            int newRgb = (a << 24) | (gray << 16) | (gray << 8) | gray;

            image.setRGB(x, y, newRgb);
        }
    }
}

void negative(BufferedImage image) {
    for (int x = 0; x < image.getWidth(); x++) {
        for (int y = 0; y < image.getHeight(); y++) {
           int rgb = image.getRGB(x, y);
           int a = (rgb >> 24) & 0xff;
           int r = (rgb >> 16) & 0xff;
           int g = (rgb >> 8) & 0xff;
           int b = rgb & 0xff;

           int nr = 255 - r;
           int ng = 255 - g;
           int nb = 255 - b;

           int newRgb = (a << 24) | (nr << 16) | (ng << 8) | nb;
           image.setRGB(x, y, newRgb);
        }
    }
}
