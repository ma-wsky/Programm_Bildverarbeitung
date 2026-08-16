package main.java.mawsky.trafficsign.ui;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.awt.geom.AffineTransform;
import java.awt.image.BufferedImage;

public class ZoomableImagePanel extends JPanel {

    private Object interpolationHint = RenderingHints.VALUE_INTERPOLATION_NEAREST_NEIGHBOR;
    private BufferedImage image;
    private double zoomFactor = 1.0;
    private double prevZoomFactor = 1.0;

    private double xOffset = 0;
    private double yOffset = 0;
    private Point startDragPoint;

    public ZoomableImagePanel() {
        setBackground(new Color(40, 40, 40));

        // reset zoom on resize
        addComponentListener(new ComponentAdapter() {
            @Override
            public void componentResized(ComponentEvent e) {
                if (image != null) {
                    resetZoom();
                }
            }
        });

        // zoom
        addMouseWheelListener(e -> {
            double zoomFactorMultiplier = (e.getWheelRotation() < 0) ? 1.15 : 0.85;

            // limits
            double newZoom = zoomFactor * zoomFactorMultiplier;
            if (newZoom < 0.1 || newZoom > 20.0) return;

            prevZoomFactor = zoomFactor;
            zoomFactor = newZoom;

            // focus on cursor position
            Point mousePt = e.getPoint();
            xOffset = mousePt.x - (mousePt.x - xOffset) * (zoomFactor / prevZoomFactor);
            yOffset = mousePt.y - (mousePt.y - yOffset) * (zoomFactor / prevZoomFactor);

            repaint();
        });

        // drag to move
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
                // reset zoom level
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
        resetZoom();
    }

    public void resetZoom() {
        if (image == null || getWidth() <= 0 || getHeight() <= 0) {
            zoomFactor = 1.0;
            xOffset = 0;
            yOffset = 0;
            repaint();
            return;
        }

        // fit image to panel
        double widthScale = (double) getWidth() / image.getWidth();
        double heightScale = (double) getHeight() / image.getHeight();
        zoomFactor = Math.min(widthScale, heightScale) * 0.95;

        xOffset = (getWidth() - (image.getWidth() * zoomFactor)) / 2.0;
        yOffset = (getHeight() - (image.getHeight() * zoomFactor)) / 2.0;

        repaint();
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        if (image == null) return;

        Graphics2D g2d = (Graphics2D) g.create();

        // nearest neighbor interpolation
        g2d.setRenderingHint(RenderingHints.KEY_INTERPOLATION, interpolationHint);
        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2d.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);

        AffineTransform at = new AffineTransform();
        at.translate(xOffset, yOffset);
        at.scale(zoomFactor, zoomFactor);

        g2d.drawImage(image, at, null);

        // zoom info
        g2d.setColor(new Color(255, 255, 255, 180));
        g2d.setFont(new Font("SansSerif", Font.PLAIN, 12));
        g2d.drawString(String.format("Zoom: %.0f%% (Doppelklick = Reset)", zoomFactor * 100), 10, getHeight() - 10);

        g2d.dispose();
    }

    public void setInterpolationMode(Object hint){
        this.interpolationHint = hint;
        repaint();
    }
}