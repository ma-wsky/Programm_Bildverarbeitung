package main.java.mawsky.trafficsign.ui;

import main.java.mawsky.trafficsign.MainLauncher;

import javax.imageio.ImageIO;
import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;

public class PipelineViewer extends JFrame {

    public PipelineViewer(ImageCollection data) {
        setTitle("Traffic Sign Detection Pipeline Visualizer");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

        setExtendedState(JFrame.MAXIMIZED_BOTH);
        setMinimumSize(new Dimension(1200, 800));

        JPanel mainContainer = new JPanel(new BorderLayout());

        // Header
        JPanel headerPanel = createHeaderPanel(data);
        mainContainer.add(headerPanel, BorderLayout.NORTH);

        JTabbedPane mainTabs = new JTabbedPane();
        mainTabs.setFont(new Font("SansSerif", Font.BOLD, 18)); // Tabs-Schrift auf 18pt erhöht

        // 1. Tab: architecture and workflow
        mainTabs.addTab("Architektur & Workflow", createFlowchartTab());

        // 2. Tab: preprocessing whole image
        mainTabs.addTab("  Vorverarbeitung (ganzes Bild)  ", createPipelineTab(buildFullImageSteps(data)));

        // 3. Tab: preprocessing window
        if (data.getWindowImageCollection() != null) {
            mainTabs.addTab("  Vorverarbeitung (Suchfenster)  ", createPipelineTab(buildWindowSteps(data)));
        }

        // 4. Tab: geometry and color
        mainTabs.addTab("  Analyse & Detektion  ", createPipelineTab(buildAnalysisSteps(data)));

        mainContainer.add(mainTabs, BorderLayout.CENTER);
        add(mainContainer);
    }

    private JPanel createHeaderPanel(ImageCollection data) {
        JPanel panel = new JPanel(new BorderLayout(20, 0));
        panel.setBackground(new Color(240, 242, 245));
        panel.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(0, 0, 1, 0, Color.LIGHT_GRAY),
                new EmptyBorder(18, 25, 18, 25)
        ));

        JPanel infoPanel = getInfoPanel(data);

        panel.add(infoPanel, BorderLayout.WEST);

        // button
        JButton newImageButton = new JButton("📁 Neues Bild wählen");
        newImageButton.setFont(new Font("SansSerif", Font.BOLD, 16));
        newImageButton.setPreferredSize(new Dimension(230, 52));
        newImageButton.setFocusable(false);
        newImageButton.setCursor(new Cursor(Cursor.HAND_CURSOR));

        newImageButton.addActionListener(_ -> {
            this.dispose();
            SwingUtilities.invokeLater(() -> new MainLauncher().setVisible(true));
        });

        panel.add(newImageButton, BorderLayout.EAST);

        return panel;
    }

    private static JPanel getInfoPanel(ImageCollection data) {
        String signInfo = data.getDetectedSignName() != null ? data.getDetectedSignName() : "Kein Schild erkannt";
        long timeMs = data.getRuntimeMS();
        double timeSec = timeMs / 1000.0;

        JPanel infoPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 15, 0));
        infoPanel.setOpaque(false);

        // detected sign
        JLabel signLabel = new JLabel("Erkanntes Schild: " + signInfo);
        signLabel.setFont(new Font("SansSerif", Font.BOLD, 22));
        signLabel.setForeground(new Color(0, 122, 204));

        // separator
        JLabel sepLabel = new JLabel("|");
        sepLabel.setFont(new Font("SansSerif", Font.PLAIN, 22));
        sepLabel.setForeground(Color.GRAY);

        // runtime
        JLabel timeLabel = new JLabel(String.format("Verarbeitungszeit: %d ms (%.2f s)", timeMs, timeSec));
        timeLabel.setFont(new Font("SansSerif", Font.BOLD, 22));
        timeLabel.setForeground(Color.DARK_GRAY);

        infoPanel.add(signLabel);
        infoPanel.add(sepLabel);
        infoPanel.add(timeLabel);
        return infoPanel;
    }

    private JPanel createFlowchartTab(){
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBorder(new EmptyBorder(10, 10, 10, 10));

        ZoomableImagePanel flowchartPanel = new ZoomableImagePanel();
        flowchartPanel.setInterpolationMode(RenderingHints.VALUE_INTERPOLATION_BICUBIC);

        try {
            File file = new File("src/main/resources/pipeline_flowchart.png");
            if (file.exists()){
                BufferedImage rawImg = ImageIO.read(file);
                flowchartPanel.setImage(rawImg);
            } else {
                System.err.println("Flussdiagramm nicht im Ordner gefunden");
            }
        } catch (IOException e) {
            System.err.println("IOException");
        }

        panel.add(flowchartPanel, BorderLayout.CENTER);
        return panel;
    }

    // tab layout
    private JPanel createPipelineTab(DefaultListModel<StepItem> steps) {
        JPanel panel = new JPanel(new BorderLayout(10, 10));
        panel.setBorder(new EmptyBorder(10, 10, 10, 10));

        JList<StepItem> stepList = new JList<>(steps);
        stepList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        stepList.setFixedCellHeight(45);
        stepList.setFont(new Font("SansSerif", Font.BOLD, 15));

        // right side
        ZoomableImagePanel imagePanel = new ZoomableImagePanel();
        JLabel titleLabel = new JLabel("", SwingConstants.LEFT);
        titleLabel.setFont(new Font("SansSerif", Font.BOLD, 20));

        // bottom
        JTextArea descArea = new JTextArea();
        descArea.setEditable(false);
        descArea.setLineWrap(true);
        descArea.setWrapStyleWord(true);
        descArea.setBackground(panel.getBackground());
        descArea.setFont(new Font("SansSerif", Font.PLAIN, 15));

        JPanel infoPanel = new JPanel(new BorderLayout(8, 8));
        infoPanel.setBorder(new EmptyBorder(12, 15, 12, 15));
        infoPanel.add(titleLabel, BorderLayout.NORTH);
        infoPanel.add(descArea, BorderLayout.CENTER);

        JPanel rightPanel = new JPanel(new BorderLayout(10, 10));
        rightPanel.add(imagePanel, BorderLayout.CENTER);
        rightPanel.add(infoPanel, BorderLayout.SOUTH);

        stepList.addListSelectionListener(_ -> {
            StepItem selected = stepList.getSelectedValue();
            if (selected != null) {
                titleLabel.setText(selected.title());
                descArea.setText(selected.description());
                if (selected.image() != null) {
                    imagePanel.setImage(selected.image());
                } else {
                    imagePanel.setImage(null);
                }
            }
        });

        stepList.setSelectedIndex(0);

        JSplitPane splitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, new JScrollPane(stepList), rightPanel);
        splitPane.setDividerLocation(350);
        panel.add(splitPane, BorderLayout.CENTER);

        return panel;
    }

    // image preprocessing
    private DefaultListModel<StepItem> buildFullImageSteps(ImageCollection data) {
        DefaultListModel<StepItem> model = new DefaultListModel<>();
        var c = data.getWholeImageCollection();

        model.addElement(new StepItem("Bildpyramide", "Sammlung unterschiedlicher Skalierungsstufen. Jedes Bild ist mit dem Faktor 0.5 skaliert", data.getImage_pyramid()));
        if (c != null) {
            model.addElement(new StepItem("Gauss-Tiefpass", "Glättet das Bild und entfernt hochfrequentes Rauschen.", c.getGauss_lowpass()));
            if (c.getHistogram_equalization() != null) model.addElement(new StepItem("Histogrammausgleich", "Erhöht den Kontrast durch gleichmäßige Verteilung der Helligkeitswerte.", c.getHistogram_equalization()));
            model.addElement(new StepItem("Sobel-Filter", "Hebt hochfrequente Bildpunkte hervor -> Hochpass", c.getSobel_filter()));
            model.addElement(new StepItem("Äquidistanten", "Schwellenwertverfahren zum isolieren bestimmter Grauwert-Bereiche.", c.getEquidensity()));
            model.addElement(new StepItem("Dilatation", "Vergrößert helle Strukturen -> Schließt kleine Lücken in Kanten.", c.getDilation()));
            model.addElement(new StepItem("Erosion", "Schrumpft helle Strukturen -> Stellt ursprüngliche Dicke der Kanten wieder her.", c.getErosion()));
            model.addElement(new StepItem("Fertige Vorverarbeitung", "Das fertige Graustufenbild für die spätere Liniensuche.", c.getPreProcessed()));
        }
        return model;
    }

    // window preprocessing
    private DefaultListModel<StepItem> buildWindowSteps(ImageCollection data) {
        DefaultListModel<StepItem> model = new DefaultListModel<>();
        var c = data.getWindowImageCollection();

        if (c != null) {
            model.addElement(new StepItem("Fenster im Bild", "Glättet das Bild und entfernt hochfrequentes Rauschen.", data.getWindowImage()));
            model.addElement(new StepItem("Gauss-Tiefpass", "Erhöht den Kontrast durch gleichmäßige Verteilung der Helligkeitswerte.", c.getGauss_lowpass()));
            if (c.getHistogram_equalization() != null) model.addElement(new StepItem("Histogrammausgleich", "Histogrammausgleich des Fensters.", c.getHistogram_equalization()));
            model.addElement(new StepItem("Sobel-Filter", "Hebt hochfrequente Bildpunkte hervor -> Hochpass", c.getSobel_filter()));
            model.addElement(new StepItem("Äquidistanten", "Schwellenwertverfahren zum isolieren bestimmter Grauwert-Bereiche.", c.getEquidensity()));
            model.addElement(new StepItem("Dilatation", "Vergrößert helle Strukturen -> Schließt kleine Lücken in Kanten.", c.getDilation()));
            model.addElement(new StepItem("Erosion", "Schrumpft helle Strukturen -> Stellt ursprüngliche Dicke der Kanten wieder her.", c.getErosion()));
            model.addElement(new StepItem("Fertige Vorverarbeitung", "Das fertige Graustufenbild für die spätere Liniensuche.", c.getPreProcessed()));
        }
        return model;
    }

    // analysis and detection
    private DefaultListModel<StepItem> buildAnalysisSteps(ImageCollection data) {
        DefaultListModel<StepItem> model = new DefaultListModel<>();

        model.addElement(new StepItem("Hough-Raum", "Akkumulator-Matrix: Helle Punkte repräsentieren dominante Geraden im Bild.", data.getHoughSpaceImage()));
        model.addElement(new StepItem("Erkannte Linien", "Die aus den Hough-Peaks gefilterten und gruppierten 25 Haupt-Kantenlinien.", data.getBestLinesImage()));
        model.addElement(new StepItem("Geometrie des Schildes", "Rekonstruiertes Polygon basierend auf Schnittpunkten und Polarwinkeln.", data.getFoundGeometryImage()));
        model.addElement(new StepItem("Farbe des Schildes", "Validierung des Schildinneren / Randes auf charakteristische Farben.", data.getFoundColorImage()));

        return model;
    }
}