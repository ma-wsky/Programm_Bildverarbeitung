package main.java.mawsky.trafficsign.ui;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.awt.geom.AffineTransform;
import java.awt.image.BufferedImage;

public class ZoomableImagePanel extends JPanel {

    private BufferedImage image;
    private double zoomFactor = 1.0;
    private double prevZoomFactor = 1.0;

    private double xOffset = 0;
    private double yOffset = 0;
    private Point startDragPoint;

    public ZoomableImagePanel() {
        setBackground(new Color(40, 40, 40)); // Dunkler Hintergrund bringt Kantenbilder besser zur Geltung

        // 1. Mausrad zum Zoomen
        addMouseWheelListener(e -> {
            double zoomFactorMultiplier = (e.getWheelRotation() < 0) ? 1.15 : 0.85;

            // Zoom-Limits (max 20x vergrößern, min 0.1x verkleinern)
            double newZoom = zoomFactor * zoomFactorMultiplier;
            if (newZoom < 0.1 || newZoom > 20.0) return;

            prevZoomFactor = zoomFactor;
            zoomFactor = newZoom;

            // Zoomen an der aktuellen Mausposition fokussieren
            Point mousePt = e.getPoint();
            xOffset = mousePt.x - (mousePt.x - xOffset) * (zoomFactor / prevZoomFactor);
            yOffset = mousePt.y - (mousePt.y - yOffset) * (zoomFactor / prevZoomFactor);

            repaint();
        });

        // 2. Drag & Drop zum Verschieben
        MouseAdapter mouseHandler = new MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent e) {
                if (SwingUtilities.isLeftMouseButton(e)) {
                    startDragPoint = e.getPoint();
                }
            }

            @Override
            public void mouseDragged(MouseEvent e) {
                if (SwingUtilities.isLeftMouseButton(e) && startDragPoint != null) {
                    Point currentPoint = e.getPoint();
                    xOffset += currentPoint.x - startDragPoint.x;
                    yOffset += currentPoint.y - startDragPoint.y;
                    startDragPoint = currentPoint;
                    repaint();
                }
            }

            @Override
            public void mouseClicked(MouseEvent e) {
                // Doppelklick setzt den Zoom zurück
                if (e.getClickCount() == 2) {
                    resetZoom();
                }
            }
        };

        addMouseListener(mouseHandler);
        addMouseMotionListener(mouseHandler);
    }

    public void setImage(BufferedImage newImage) {
        this.image = newImage;
        resetZoom(); // Beim Bildwechsel automatisch zentrieren/einpassen
    }

    public void resetZoom() {
        if (image == null || getWidth() <= 0 || getHeight() <= 0) {
            zoomFactor = 1.0;
            xOffset = 0;
            yOffset = 0;
            repaint();
            return;
        }

        // Automatisch so skalieren, dass das Bild perfekt ins Panel passt
        double widthScale = (double) getWidth() / image.getWidth();
        double heightScale = (double) getHeight() / image.getHeight();
        zoomFactor = Math.min(widthScale, heightScale) * 0.95; // 95% der Fläche nutzen

        // Zentrieren
        xOffset = (getWidth() - (image.getWidth() * zoomFactor)) / 2.0;
        yOffset = (getHeight() - (image.getHeight() * zoomFactor)) / 2.0;

        repaint();
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        if (image == null) return;

        Graphics2D g2d = (Graphics2D) g.create();

        // Hohe Skalierungsqualität aktivieren
        g2d.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_NEAREST_NEIGHBOR);
        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        // Transformation anwenden (Position Offset + Skalierung)
        AffineTransform at = new AffineTransform();
        at.translate(xOffset, yOffset);
        at.scale(zoomFactor, zoomFactor);

        g2d.drawImage(image, at, null);

        // Zoom-Info unten links einblenden
        g2d.setColor(new Color(255, 255, 255, 180));
        g2d.setFont(new Font("SansSerif", Font.PLAIN, 12));
        g2d.drawString(String.format("Zoom: %.0f%% (Doppelklick = Reset)", zoomFactor * 100), 10, getHeight() - 10);

        g2d.dispose();
    }
}