import java.awt.image.BufferedImage;
import java.awt.image.ColorModel;
import java.awt.image.WritableRaster;

public class GlobalHelperFunctions {

    public static BufferedImage copyBufferedImage(BufferedImage image){
        ColorModel cm = image.getColorModel();
        boolean alpha = cm.isAlphaPremultiplied();
        WritableRaster raster = image.copyData(null);
        return new BufferedImage(cm, raster, alpha, null);
    }

    public static int calculateGrayValueFromRGB(int rgb){
        int r = (rgb >> 16) & 0xff;
        int g = (rgb >> 8) & 0xff;
        int b = rgb & 0xff;
        return (r + g + b) / 3;
    }
}
