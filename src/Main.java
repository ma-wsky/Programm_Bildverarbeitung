import javax.imageio.ImageIO;
import javax.swing.*;
import java.awt.*;
import java.awt.image.BufferedImage;

void main() {
    Integer[][] grayMatrixTEST = {
            {0, 1, 0, 1, 2, 3},
            {0, 0, 1, 2, 2, 2},
            {0, 0, 1, 1, 1, 2},
            {0, 1, 2, 2, 3, 3},
            {0, 2, 2, 3, 3, 3},
            {2, 2, 2, 3, 3, 3}};

    try {
        BufferedImage image = readImage("pics/Bild_A.jpg");
        BufferedImage ppm = convertToPPM(image);
        Integer[][] grayMatrix = calculateGrayMatrix(image);

        Integer[][] coOccurrenceMatrix = calculateCoOccurrenceMatrix(grayMatrix, 256);
        System.out.println(Arrays.deepToString(coOccurrenceMatrix));
        displayImage(ppm);

    } catch (IOException e) {
        throw new RuntimeException(e);
    }
}

Integer[][] calculateGrayMatrix(BufferedImage image) {

    Integer[][] grayMatrix = new Integer[image.getWidth()][image.getHeight()];

    for (int y = 0; y < image.getHeight(); y++) {
        for (int x = 0; x < image.getWidth(); x++) {
            int rgb = image.getRGB(x, y);
            int r = (rgb >> 16) & 0xff;
            int g = (rgb >> 8) & 0xff;
            int b = rgb & 0xff;

            int gray = (int) Math.floor(0.299 * r + 0.587 * g + 0.114 * b);
            grayMatrix[x][y] = gray;
        }
    }

    return grayMatrix;
}

Integer[][] calculateCoOccurrenceMatrix(Integer[][] grayMatrix, int numGrays){

    int grayMatrixRows = grayMatrix.length;
    int grayMatrixCols = grayMatrix[0].length;

    Integer[][] coOccurrenceMatrix = new Integer[numGrays][numGrays];

    for(int rows = 0; rows < numGrays; rows++){
        for(int cols = 0; cols < numGrays; cols++){
            coOccurrenceMatrix[rows][cols] = 0;
        }
    }

    for(int rows = 0; rows < grayMatrixRows; rows++){
        for(int cols = 0; cols < grayMatrixCols-1; cols++){

            int grayValueCurrent = grayMatrix[rows][cols];
            int grayValueNeighbour = grayMatrix[rows][cols+1];

            coOccurrenceMatrix[grayValueCurrent][grayValueNeighbour]++;
        }
    }

    return coOccurrenceMatrix;
}

BufferedImage rotateImageBackwardMapping(BufferedImage image, Point pivotPoint, int degrees) {

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

BufferedImage rotateImageForwardMapping(BufferedImage image, Point pivotPoint, int degrees) {
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

BufferedImage rotateImage90s(BufferedImage image, int degrees) {

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
                3 3
                255
                 255   0   0     0 255   0     0   0 255
                 255 255   0   255   0 255     0 255 255
                 255 255 255     0   0   0   128 128 128""");
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
