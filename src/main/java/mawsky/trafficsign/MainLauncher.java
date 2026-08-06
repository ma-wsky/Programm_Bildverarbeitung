package main.java.mawsky.trafficsign;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.io.File;
import main.java.mawsky.trafficsign.ui.ImageCollection;
import main.java.mawsky.trafficsign.core.Pipeline;
import main.java.mawsky.trafficsign.ui.PipelineViewer;


public class MainLauncher extends JFrame {

    private final JTextField pathTextField;
    private final JButton startButton;

    public MainLauncher() {
        setTitle("Traffic Sign Detector - Launcher");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(550, 180);
        setLocationRelativeTo(null);
        setResizable(false);

        // Main Panel Layout
        JPanel panel = new JPanel(new BorderLayout(10, 10));
        panel.setBorder(new EmptyBorder(15, 15, 15, 15));

        // Titel / Header
        JLabel headerLabel = new JLabel("Wähle ein Bild für die Verkehrszeichenerkennung:");
        headerLabel.setFont(new Font("SansSerif", Font.BOLD, 14));
        panel.add(headerLabel, BorderLayout.NORTH);

        // path input panel
        JPanel inputPanel = new JPanel(new BorderLayout(5, 5));
        pathTextField = new JTextField();
        pathTextField.setFont(new Font("SansSerif", Font.PLAIN, 13));

        JButton browseButton = new JButton("Durchsuchen...");
        inputPanel.add(pathTextField, BorderLayout.CENTER);
        inputPanel.add(browseButton, BorderLayout.EAST);
        panel.add(inputPanel, BorderLayout.CENTER);

        // start button
        startButton = new JButton("Pipeline Starten");
        startButton.setFont(new Font("SansSerif", Font.BOLD, 14));
        startButton.setPreferredSize(new Dimension(0, 40));
        panel.add(startButton, BorderLayout.SOUTH);

        add(panel);


        // open file chooser
        browseButton.addActionListener(_ -> {
            JFileChooser chooser = new JFileChooser();
            chooser.setDialogTitle("Bild auswählen");
            chooser.setFileFilter(new javax.swing.filechooser.FileNameExtensionFilter("Bilder (JPG, PNG)", "jpg", "jpeg", "png", "bmp", "ppm"));

            int result = chooser.showOpenDialog(this);
            if (result == JFileChooser.APPROVE_OPTION) {
                File selectedFile = chooser.getSelectedFile();
                pathTextField.setText(selectedFile.getAbsolutePath());
            }
        });

        startButton.addActionListener(_ -> runPipeline());
    }

    private void runPipeline() {
        String filepath = pathTextField.getText().trim();

        if (filepath.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Bitte wähle zuerst eine Datei aus!", "Fehler", JOptionPane.ERROR_MESSAGE);
            return;
        }

        File f = new File(filepath);
        if (!f.exists() || f.isDirectory()) {
            JOptionPane.showMessageDialog(this, "Die angegebene Datei existiert nicht!", "Fehler", JOptionPane.ERROR_MESSAGE);
            return;
        }

        startButton.setEnabled(false);
        startButton.setText("Verarbeite...");

        new Thread(() -> {
            try {
                ImageCollection collection = Pipeline.findSign(filepath);

                SwingUtilities.invokeLater(() -> {
                    PipelineViewer viewer = new PipelineViewer(collection);
                    viewer.setVisible(true);
                    this.dispose();
                });

            } catch (Exception ex) {
                SwingUtilities.invokeLater(() -> {
                    JOptionPane.showMessageDialog(this, "Fehler bei der Ausführung: " + ex.getMessage(), "Pipeline Fehler", JOptionPane.ERROR_MESSAGE);
                    startButton.setEnabled(true);
                    startButton.setText("Pipeline Starten");
                });
            }
        }).start();
    }

    static void main() {
        try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        } catch (Exception ignored) {}

        SwingUtilities.invokeLater(() -> new MainLauncher().setVisible(true));
    }
}