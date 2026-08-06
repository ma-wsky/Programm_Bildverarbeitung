package main.java.mawsky.trafficsign.ui;

import java.awt.image.BufferedImage;

public record StepItem(String title, String description, BufferedImage image) {

    @Override
    public String toString() {
        return title;
    }
}