package main.java.mawsky.trafficsign.ui;

import java.awt.image.BufferedImage;

public class preProcessedImageCollection {

    private BufferedImage gauss_lowpass;
    private BufferedImage histogram_equalization;
    private BufferedImage sobel_filter;
    private BufferedImage equidensity;
    private BufferedImage dilation;
    private BufferedImage erosion;
    private BufferedImage preProcessed;


    public void removeAll(){
        this.gauss_lowpass = null;
        this.histogram_equalization = null;
        this.sobel_filter = null;
        this.equidensity = null;
        this.dilation = null;
        this.erosion = null;
        this.preProcessed = null;
    }


    public BufferedImage getGauss_lowpass() {
        return gauss_lowpass;
    }

    public void setGauss_lowpass(BufferedImage gauss_lowpass) {
        this.gauss_lowpass = gauss_lowpass;
    }

    public BufferedImage getHistogram_equalization() {
        return histogram_equalization;
    }

    public void setHistogram_equalization(BufferedImage histogram_equalization) {
        this.histogram_equalization = histogram_equalization;
    }

    public BufferedImage getSobel_filter() {
        return sobel_filter;
    }

    public void setSobel_filter(BufferedImage sobel_filter) {
        this.sobel_filter = sobel_filter;
    }

    public BufferedImage getEquidensity() {
        return equidensity;
    }

    public void setEquidensity(BufferedImage equidensity) {
        this.equidensity = equidensity;
    }

    public BufferedImage getDilation() {
        return dilation;
    }

    public void setDilation(BufferedImage dilation) {
        this.dilation = dilation;
    }

    public BufferedImage getErosion() {
        return erosion;
    }

    public void setErosion(BufferedImage erosion) {
        this.erosion = erosion;
    }

    public BufferedImage getPreProcessed() {
        return preProcessed;
    }

    public void setPreProcessed(BufferedImage preProcessed) {
        this.preProcessed = preProcessed;
    }
}
