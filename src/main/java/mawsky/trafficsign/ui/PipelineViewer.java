package main.java.mawsky.trafficsign.ui;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;

public class PipelineViewer extends JFrame {

    public PipelineViewer(ImageCollection data) {
        setTitle("Traffic Sign Detection Pipeline Visualizer");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(1200, 800);
        setLocationRelativeTo(null);

        JTabbedPane mainTabs = new JTabbedPane();

        // 1. Tab: Original & Preprocessing (Ganzes Bild)
        mainTabs.addTab("Vorverarbeitung (ganzes Bild)", createPipelineTab(buildFullImageSteps(data)));

        // 2. Tab: Preprocessing (Moving Window / ROI)
        if (data.getWindowImageCollection() != null) {
            mainTabs.addTab("Vorverarbeitung (Suchfenster)", createPipelineTab(buildWindowSteps(data)));
        }

        // 3. Tab: Hough, Geometrie & Farbe
        mainTabs.addTab("Analyse & Detektion", createPipelineTab(buildAnalysisSteps(data)));

        add(mainTabs);
    }

    // --- Tab-Layout (Links Liste, Rechts Bild + Text) ---
    private JPanel createPipelineTab(DefaultListModel<StepItem> steps) {
        JPanel panel = new JPanel(new BorderLayout(10, 10));
        panel.setBorder(new EmptyBorder(10, 10, 10, 10));

        JList<StepItem> stepList = new JList<>(steps);
        stepList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        stepList.setFixedCellHeight(40);
        stepList.setFont(new Font("SansSerif", Font.BOLD, 14));

        // Rechte Seite
        JLabel imageLabel = new JLabel("", SwingConstants.CENTER);
        JLabel titleLabel = new JLabel("", SwingConstants.LEFT);
        titleLabel.setFont(new Font("SansSerif", Font.BOLD, 18));

        JTextArea descArea = new JTextArea();
        descArea.setEditable(false);
        descArea.setLineWrap(true);
        descArea.setWrapStyleWord(true);
        descArea.setBackground(panel.getBackground());
        descArea.setFont(new Font("SansSerif", Font.PLAIN, 14));

        JPanel infoPanel = new JPanel(new BorderLayout(5, 5));
        infoPanel.add(titleLabel, BorderLayout.NORTH);
        infoPanel.add(descArea, BorderLayout.CENTER);

        JPanel rightPanel = new JPanel(new BorderLayout(10, 10));
        rightPanel.add(new JScrollPane(imageLabel), BorderLayout.CENTER);
        rightPanel.add(infoPanel, BorderLayout.SOUTH);

        // Selection Listener
        stepList.addListSelectionListener(e -> {
            StepItem selected = stepList.getSelectedValue();
            if (selected != null) {
                titleLabel.setText(selected.title());
                descArea.setText(selected.description());
                if (selected.image() != null) {
                    imageLabel.setIcon(new ImageIcon(selected.image()));
                } else {
                    imageLabel.setIcon(null);
                    imageLabel.setText("Kein Bild verfügbar");
                }
            }
        });

        stepList.setSelectedIndex(0);

        JSplitPane splitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, new JScrollPane(stepList), rightPanel);
        splitPane.setDividerLocation(300);
        panel.add(splitPane, BorderLayout.CENTER);

        return panel;
    }

    // --- 1. Preprocessing-Schritte ---
    private DefaultListModel<StepItem> buildFullImageSteps(ImageCollection data) {
        DefaultListModel<StepItem> model = new DefaultListModel<>();
        var c = data.getWholeImageCollection();

        model.addElement(new StepItem("Bildpyramide", "Bildpyramide vom kleinsten Level bis zum Level auf dem das Schild gefunden wurde.", data.getImage_pyramid()));
        if (c != null) {
            model.addElement(new StepItem("Gauss-Tiefpass", "Glättet das Bild und entfernt hochfrequentes Rauschen.", c.getGauss_lowpass()));
            if (c.getHistogram_equalization() != null) model.addElement(new StepItem("Histogrammausgleich", "Erhöht den Kontrast durch gleichmäßige Verteilung der Helligkeitswerte.", c.getHistogram_equalization()));
            model.addElement(new StepItem("Sobel-Filter", "Hebt hochfrequente Bildpunkte hervor -> Hochpass", c.getSobel_filter()));
            model.addElement(new StepItem("Äquidistanten", "Schwellenwertverfahren zum isolieren bestimmter Grauwert-Bereiche.", c.getEquidensity()));
            model.addElement(new StepItem("Dilatation", "Vergrößert helle Strukturen / Schließt kleine Lücken in Kanten.", c.getDilation()));
            model.addElement(new StepItem("Erosion", "Schrumpft helle Strukturen / Entfernt isolierte Rausch-Pixel.", c.getErosion()));
            model.addElement(new StepItem("Fertige Vorverarbeitung", "Das fertige Graustufenbild für die spätere Liniensuche.", c.getPreProcessed()));
        }
        return model;
    }

    // --- 2. ROI-Schritte ---
    private DefaultListModel<StepItem> buildWindowSteps(ImageCollection data) {
        DefaultListModel<StepItem> model = new DefaultListModel<>();
        var c = data.getWindowImageCollection();

        if (c != null) {
            model.addElement(new StepItem("Fenster im Bild", "Glättet das Bild und entfernt hochfrequentes Rauschen.", data.getWindowImage()));
            model.addElement(new StepItem("Gauss-Tiefpass", "Erhöht den Kontrast durch gleichmäßige Verteilung der Helligkeitswerte.", c.getGauss_lowpass()));
            if (c.getHistogram_equalization() != null) model.addElement(new StepItem("Histogrammausgleich", "Histogrammausgleich des Fensters.", c.getHistogram_equalization()));
            model.addElement(new StepItem("Sobel-Filter", "Hebt hochfrequente Bildpunkte hervor -> Hochpass", c.getSobel_filter()));
            model.addElement(new StepItem("Äquidistanten", "Schwellenwertverfahren zum isolieren bestimmter Grauwert-Bereiche.", c.getEquidensity()));
            model.addElement(new StepItem("Dilatation", "Vergrößert helle Strukturen / Schließt kleine Lücken in Kanten.", c.getDilation()));
            model.addElement(new StepItem("Erosion", "Schrumpft helle Strukturen / Entfernt isolierte Rausch-Pixel.", c.getErosion()));
            model.addElement(new StepItem("Fertige Vorverarbeitung", "Das fertige Graustufenbild für die spätere Liniensuche.", c.getPreProcessed()));
        }
        return model;
    }

    // --- 3. Hough & Erkennung ---
    private DefaultListModel<StepItem> buildAnalysisSteps(ImageCollection data) {
        DefaultListModel<StepItem> model = new DefaultListModel<>();

        model.addElement(new StepItem("Hough-Raum", "Akkumulator-Matrix (phi/r). Helle Punkte repräsentieren dominante Geraden im Bild.", data.getHoughSpaceImage()));
        model.addElement(new StepItem("Erkannte Linien", "Die aus den Hough-Peaks gefilterten und gruppierten 25 Haupt-Kantenlinien.", data.getBestLinesImage()));
        model.addElement(new StepItem("Geometrie des Schildes", "Rekonstruiertes Polygon basierend auf Schnittpunkten und Polarwinkeln.", data.getFoundGeometryImage()));
        model.addElement(new StepItem("Farbe des Schildes", "Validierung des Schildinneren / Randes auf charakteristische Farben.", data.getFoundColorImage()));

        return model;
    }
}