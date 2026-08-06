package main.java.mawsky.trafficsign.ui;

import java.awt.image.BufferedImage;

public class ImageCollection {

    private BufferedImage image_pyramid;
    private BufferedImage windowImage;

    private final preProcessedImageCollection wholeImageCollection;
    private final preProcessedImageCollection windowImageCollection;


    // line finding
    private BufferedImage houghSpaceImage;
    private BufferedImage bestLinesImage;

    // geometry
    private BufferedImage foundGeometryImage;

    // color
    private BufferedImage foundColorImage;

    // additional data
    private String detectedSignName;
    private long runtimeMS;

    public ImageCollection(){
        this.wholeImageCollection = new preProcessedImageCollection();
        this.windowImageCollection = new preProcessedImageCollection();
    }


    public BufferedImage getImage_pyramid() {return image_pyramid;}

    public void setImage_pyramid(BufferedImage image_pyramid) {this.image_pyramid = image_pyramid;}

    public BufferedImage getWindowImage() {return windowImage;}

    public void setWindowImage(BufferedImage windowImage) {this.windowImage = windowImage;}

    public preProcessedImageCollection getWholeImageCollection() {return wholeImageCollection;}

    public preProcessedImageCollection getWindowImageCollection() {return windowImageCollection;}

    public BufferedImage getHoughSpaceImage() {return houghSpaceImage;}

    public void setHoughSpaceImage(BufferedImage houghSpaceImage) {this.houghSpaceImage = houghSpaceImage;}

    public BufferedImage getBestLinesImage() {return bestLinesImage;}

    public void setBestLinesImage(BufferedImage bestLinesImage) {this.bestLinesImage = bestLinesImage;}

    public BufferedImage getFoundGeometryImage() {return foundGeometryImage;}

    public void setFoundGeometryImage(BufferedImage foundGeometryImage) {this.foundGeometryImage = foundGeometryImage;}

    public BufferedImage getFoundColorImage() {return foundColorImage;}

    public void setFoundColorImage(BufferedImage foundColorImage) {this.foundColorImage = foundColorImage;}

    public String getDetectedSignName() {return detectedSignName;}

    public void setDetectedSignName(String detectedSignName) {this.detectedSignName = detectedSignName;}

    public long getRuntimeMS() {return runtimeMS;}

    public void setRuntimeMS(long runtimeMS) {this.runtimeMS = runtimeMS;}
}
