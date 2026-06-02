import java.awt.image.BufferedImage;

public class ImageManipulation {

    private final BufferedImage image;

    private final int[] LUT;

    /**
     * initializes look up table and buffered image
     * @param image image
     */
    ImageManipulation(BufferedImage image) {
        this.image = image;
        this.LUT = new int[256];
        for (int i = 0; i < 256; i++) {
            this.LUT[i] = -1;
        }
    }

    /**
     * performs linear scale of gray values with equation f(g)=c2*g+c1*c2
     * @param c1 c1
     * @param c2 c2
     */
    void linearScaleGrayImage(int c1, double c2){
        // c2 = 1, c1 > 0 => brighter
        // c2 = 1, c1 < 0 => darker
        // 0 < c2 < 1 , c1 = 0 => lower contrast
        // c2 > 1 , c1 = 0 => higher contrast

        double c = c1*c2;

        for (int x = 0; x < image.getWidth(); x++) {
            for (int y = 0; y < image.getHeight(); y++) {
                int rgb = image.getRGB(x, y);

                int a = (rgb >> 24) & 0xff;
                int r = (rgb >> 16) & 0xff;
                int g = (rgb >> 8) & 0xff;
                int b = rgb & 0xff;

                int gray = (r + g + b) / 3;

                if (this.LUT[gray] == -1){
                    //calc
                    int value = (int) (c2*gray+c);
                    if (value > 255) {
                        value = 255;
                    }else if (value < 0){
                        value = 0;
                    }
                    this.LUT[gray] = value;
                }

                int newGray = this.LUT[gray];

                int newRgb = (a << 24) | (newGray << 16) | (newGray << 8) | newGray;

                image.setRGB(x, y, newRgb);
            }
        }
    }
}
