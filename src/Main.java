import javax.imageio.ImageIO;
import javax.swing.*;
import java.awt.*;
import java.awt.image.BufferedImage;

void main() {
    try {
        displayImage(convertToPPM(readImage("pics/castle.jpg")));
    } catch (IOException e) {
        throw new RuntimeException(e);
    }
}

BufferedImage convertToPPM(BufferedImage input) throws IOException {
    FileWriter writer = new FileWriter("converted.ppm");

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

    return readPPM(new File("converted.ppm"));
}

BufferedImage readPPM(File file) throws IOException {
    Scanner scanner = new Scanner(file);
    scanner.useDelimiter("(\\s+|#.*\\n)+");

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

    return image;
}

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

void generateColorTestPPM() throws IOException {
    FileWriter writer = new FileWriter("colorTest.ppm");
    writer.write("""
                P3
                3 2
                255
                 255   0   0     0 255   0     0   0 255
                 255 255   0   255 255 255     0   0   0""");
    writer.close();
}

void workflow(){
    try {
        // read Image
        BufferedImage original = readImage("pics/Bild_A.jpg");

        // copy original
        assert original != null;
        BufferedImage grayScale = copyImage(original);
        BufferedImage negative = copyImage(original);
        BufferedImage negativeGray = copyImage(original);

        // manipulate
        grayScale(grayScale);
        negative(negative);
        grayScale(negativeGray);
        negative(negativeGray);

        // display
        displayImage(original, negative, grayScale, negativeGray);

    } catch (IOException e) {
        System.err.println("Fehler beim Lesen der Datei: " + e.getMessage());
    }
}

BufferedImage readImage(String filename) throws IOException {
    //Datei lesen
    File inputFile = new File(filename);

    // Bilddaten im RAM
    BufferedImage readImage = ImageIO.read(inputFile); //.read erkennt format automatisch

    if (readImage != null) {
        System.out.println("Bild erfolgreich geladen!");
        return readImage;
    } else {
        System.out.println("Format wird nicht unterstützt.");
    }
    return null;
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
