package main.java.mawsky.trafficsign.ui;

import java.awt.image.BufferedImage;
import java.util.ArrayList;

public class ImageCollection {

    private BufferedImage originalImage;
    private BufferedImage image_pyramid;
    private BufferedImage pyramidLevelFound;
    private BufferedImage windowImage;

    private preProcessedImageCollection wholeImageCollection;
    private preProcessedImageCollection windowImageCollection;


    // line finding
    private BufferedImage houghSpaceImage;
    private BufferedImage bestLinesImage;

    // geometry
    private BufferedImage foundGeometryImage;

    // color
    private BufferedImage foundColorImage;

    public ImageCollection(){
        this.wholeImageCollection = new preProcessedImageCollection();
        this.windowImageCollection = new preProcessedImageCollection();
    }





    public BufferedImage getOriginalImage() {return originalImage;}

    public void setOriginalImage(BufferedImage originalImage) {this.originalImage = originalImage;}

    public BufferedImage getImage_pyramid() {return image_pyramid;}

    public void setImage_pyramid(BufferedImage image_pyramid) {this.image_pyramid = image_pyramid;}

    public BufferedImage getPyramidLevelFound() {return pyramidLevelFound;}

    public void setPyramidLevelFound(BufferedImage pyramidLevelFound) {this.pyramidLevelFound = pyramidLevelFound;}

    public BufferedImage getWindowImage() {return windowImage;}

    public void setWindowImage(BufferedImage windowImage) {this.windowImage = windowImage;}

    public preProcessedImageCollection getWholeImageCollection() {return wholeImageCollection;}

    public void setWholeImageCollection(preProcessedImageCollection wholeImageCollection) {this.wholeImageCollection = wholeImageCollection;}

    public preProcessedImageCollection getWindowImageCollection() {return windowImageCollection;}

    public void setWindowImageCollection(preProcessedImageCollection windowImageCollection) {this.windowImageCollection = windowImageCollection;}

    public BufferedImage getHoughSpaceImage() {return houghSpaceImage;}

    public void setHoughSpaceImage(BufferedImage houghSpaceImage) {this.houghSpaceImage = houghSpaceImage;}

    public BufferedImage getBestLinesImage() {return bestLinesImage;}

    public void setBestLinesImage(BufferedImage bestLinesImage) {this.bestLinesImage = bestLinesImage;}

    public BufferedImage getFoundGeometryImage() {return foundGeometryImage;}

    public void setFoundGeometryImage(BufferedImage foundGeometryImage) {this.foundGeometryImage = foundGeometryImage;}

    public BufferedImage getFoundColorImage() {return foundColorImage;}

    public void setFoundColorImage(BufferedImage foundColorImage) {this.foundColorImage = foundColorImage;}
}
