package com.astro;

import com.formdev.flatlaf.FlatDarkLaf;
import javax.swing.*;
import javax.swing.border.*;
import javax.swing.filechooser.FileNameExtensionFilter;
import java.awt.*;
import java.io.File;
import java.nio.file.Path;
import java.util.*;
import java.util.List;

public class FitsStackerApp extends JFrame {
    private final DefaultListModel<FileItem> fileListModel;
    private final JList<FileItem> fileList;
    private final JComboBox<StackingEngine.StackingMethod> methodCombo;
    private final JButton addFilesButton;
    private final JButton addFolderButton;
    private final JButton removeButton;
    private final JButton clearButton;
    private final JButton alignButton;
    private final JButton stackButton;
    private final JProgressBar progressBar;
    private final JLabel statusLabel;
    private final JTextArea logArea;
    
    private final List<FitsImage> loadedImages = new ArrayList<>();
    private boolean imagesAligned = false;

    public FitsStackerApp() {
        super("FITS Stacker - Alignement et Empilement d'Images Astronomiques");
        
        // Initialize components
        fileListModel = new DefaultListModel<>();
        fileList = new JList<>(fileListModel);
        methodCombo = new JComboBox<>(StackingEngine.StackingMethod.values());
        addFilesButton = new JButton("Ajouter Fichiers");
        addFolderButton = new JButton("Ajouter Dossier");
        removeButton = new JButton("Retirer");
        clearButton = new JButton("Tout Effacer");
        alignButton = new JButton("Aligner Images");
        stackButton = new JButton("Empiler Images");
        progressBar = new JProgressBar(0, 100);
        statusLabel = new JLabel("Prêt");
        logArea = new JTextArea(8, 50);
        
        setupUI();
        setupListeners();
        
        setSize(1000, 700);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null);
    }

    private void setupUI() {
        setLayout(new BorderLayout(15, 15));
        
        // Main container
        JPanel mainPanel = new JPanel(new BorderLayout(15, 15));
        mainPanel.setBorder(new EmptyBorder(20, 20, 20, 20));
        
        // Title panel
        JPanel titlePanel = createTitlePanel();
        mainPanel.add(titlePanel, BorderLayout.NORTH);
        
        // Center panel with file list and controls
        JPanel centerPanel = new JPanel(new BorderLayout(15, 15));
        
        // Left: File list
        JPanel filePanel = createFilePanel();
        centerPanel.add(filePanel, BorderLayout.CENTER);
        
        // Right: Controls
        JPanel controlPanel = createControlPanel();
        centerPanel.add(controlPanel, BorderLayout.EAST);
        
        mainPanel.add(centerPanel, BorderLayout.CENTER);
        
        // Bottom: Progress and status
        JPanel bottomPanel = createBottomPanel();
        mainPanel.add(bottomPanel, BorderLayout.SOUTH);
        
        add(mainPanel);
    }

    private JPanel createTitlePanel() {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBackground(new Color(40, 40, 50));
        panel.setBorder(new EmptyBorder(15, 20, 15, 20));
        
        JLabel titleLabel = new JLabel("FITS Stacker");
        titleLabel.setFont(new Font("Segoe UI", Font.BOLD, 28));
        titleLabel.setForeground(new Color(100, 180, 255));
        
        JLabel subtitleLabel = new JLabel("Alignement et empilement d'images astronomiques");
        subtitleLabel.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        subtitleLabel.setForeground(new Color(180, 180, 180));
        
        JPanel textPanel = new JPanel(new GridLayout(2, 1, 0, 5));
        textPanel.setOpaque(false);
        textPanel.add(titleLabel);
        textPanel.add(subtitleLabel);
        
        panel.add(textPanel, BorderLayout.WEST);
        
        return panel;
    }

    private JPanel createFilePanel() {
        JPanel panel = new JPanel(new BorderLayout(10, 10));
        
        JLabel label = new JLabel("Images FITS");
        label.setFont(new Font("Segoe UI", Font.BOLD, 14));
        panel.add(label, BorderLayout.NORTH);
        
        // File list
        fileList.setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);
        fileList.setCellRenderer(new FileItemRenderer());
        
        JScrollPane scrollPane = new JScrollPane(fileList);
        scrollPane.setBorder(BorderFactory.createLineBorder(new Color(70, 70, 80)));
        panel.add(scrollPane, BorderLayout.CENTER);
        
        // Button panel
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 0));
        buttonPanel.add(addFilesButton);
        buttonPanel.add(addFolderButton);
        buttonPanel.add(removeButton);
        buttonPanel.add(clearButton);
        
        panel.add(buttonPanel, BorderLayout.SOUTH);
        
        return panel;
    }

    private JPanel createControlPanel() {
        JPanel panel = new JPanel();
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
        panel.setPreferredSize(new Dimension(280, 0));
        
        // Method selection
        JPanel methodPanel = new JPanel(new BorderLayout(10, 10));
        methodPanel.setMaximumSize(new Dimension(Integer.MAX_VALUE, 100));
        
        JLabel methodLabel = new JLabel("Méthode d'empilement");
        methodLabel.setFont(new Font("Segoe UI", Font.BOLD, 14));
        methodPanel.add(methodLabel, BorderLayout.NORTH);
        
        methodCombo.setRenderer(new DefaultListCellRenderer() {
            @Override
            public Component getListCellRendererComponent(JList<?> list, Object value, 
                    int index, boolean isSelected, boolean cellHasFocus) {
                super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
                if (value instanceof StackingEngine.StackingMethod method) {
                    setText(method.getDisplayName());
                }
                return this;
            }
        });
        
        methodPanel.add(methodCombo, BorderLayout.CENTER);
        panel.add(methodPanel);
        
        panel.add(Box.createVerticalStrut(20));
        
        // Action buttons
        alignButton.setFont(new Font("Segoe UI", Font.BOLD, 14));
        alignButton.setPreferredSize(new Dimension(280, 45));
        alignButton.setMaximumSize(new Dimension(Integer.MAX_VALUE, 45));
        alignButton.setBackground(new Color(70, 130, 180));
        
        stackButton.setFont(new Font("Segoe UI", Font.BOLD, 14));
        stackButton.setPreferredSize(new Dimension(280, 45));
        stackButton.setMaximumSize(new Dimension(Integer.MAX_VALUE, 45));
        stackButton.setBackground(new Color(60, 150, 90));
        
        panel.add(alignButton);
        panel.add(Box.createVerticalStrut(15));
        panel.add(stackButton);
        
        panel.add(Box.createVerticalStrut(30));
        
        // Log area
        JPanel logPanel = new JPanel(new BorderLayout(10, 10));
        JLabel logLabel = new JLabel("Journal");
        logLabel.setFont(new Font("Segoe UI", Font.BOLD, 14));
        logPanel.add(logLabel, BorderLayout.NORTH);
        
        logArea.setEditable(false);
        logArea.setFont(new Font("Consolas", Font.PLAIN, 11));
        JScrollPane logScroll = new JScrollPane(logArea);
        logScroll.setBorder(BorderFactory.createLineBorder(new Color(70, 70, 80)));
        logPanel.add(logScroll, BorderLayout.CENTER);
        
        panel.add(logPanel);
        
        return panel;
    }

    private JPanel createBottomPanel() {
        JPanel panel = new JPanel(new BorderLayout(10, 10));
        
        statusLabel.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        panel.add(statusLabel, BorderLayout.NORTH);
        
        progressBar.setStringPainted(true);
        progressBar.setPreferredSize(new Dimension(0, 25));
        panel.add(progressBar, BorderLayout.CENTER);
        
        return panel;
    }

    private void setupListeners() {
        addFilesButton.addActionListener(e -> addFiles());
        addFolderButton.addActionListener(e -> addFolder());
        removeButton.addActionListener(e -> removeSelected());
        clearButton.addActionListener(e -> clearAll());
        alignButton.addActionListener(e -> alignImages());
        stackButton.addActionListener(e -> stackImages());
    }

    private void addFiles() {
        JFileChooser chooser = new JFileChooser();
        chooser.setFileFilter(new FileNameExtensionFilter("FITS Files", "fits", "fit", "fts"));
        chooser.setMultiSelectionEnabled(true);
        
        if (chooser.showOpenDialog(this) == JFileChooser.APPROVE_OPTION) {
            for (File file : chooser.getSelectedFiles()) {
                addFile(file.toPath());
            }
        }
    }

    private void addFolder() {
        JFileChooser chooser = new JFileChooser();
        chooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
        
        if (chooser.showOpenDialog(this) == JFileChooser.APPROVE_OPTION) {
            File folder = chooser.getSelectedFile();
            File[] files = folder.listFiles((dir, name) -> 
                name.toLowerCase().endsWith(".fits") || 
                name.toLowerCase().endsWith(".fit") ||
                name.toLowerCase().endsWith(".fts"));
            
            if (files != null) {
                for (File file : files) {
                    addFile(file.toPath());
                }
            }
        }
    }

    private void addFile(Path path) {
        fileListModel.addElement(new FileItem(path));
        log("Ajouté: " + path.getFileName());
        imagesAligned = false;
    }

    private void removeSelected() {
        int[] indices = fileList.getSelectedIndices();
        for (int i = indices.length - 1; i >= 0; i--) {
            fileListModel.remove(indices[i]);
        }
        imagesAligned = false;
    }

    private void clearAll() {
        fileListModel.clear();
        loadedImages.clear();
        imagesAligned = false;
        log("Liste effacée");
    }

    private void alignImages() {
        if (fileListModel.isEmpty()) {
            JOptionPane.showMessageDialog(this, 
                "Veuillez ajouter des images d'abord", 
                "Erreur", JOptionPane.ERROR_MESSAGE);
            return;
        }
        
        SwingWorker<Void, Void> worker = new SwingWorker<>() {
            @Override
            protected Void doInBackground() throws Exception {
                setButtonsEnabled(false);
                log("=== Début de l'alignement ===");
                
                // Load images
                loadedImages.clear();
                for (int i = 0; i < fileListModel.size(); i++) {
                    FileItem item = fileListModel.get(i);
                    updateStatus("Chargement de " + item.path.getFileName() + "...");
                    try {
                        FitsImage img = new FitsImage(item.path);
                        loadedImages.add(img);
                        log("✓ Chargé: " + item.path.getFileName() + 
                            " (" + img.getWidth() + "x" + img.getHeight() + ")");
                    } catch (IllegalArgumentException e) {
                        log("✗ ERREUR - " + item.path.getFileName() + ": " + e.getMessage());
                        SwingUtilities.invokeLater(() -> {
                            JOptionPane.showMessageDialog(FitsStackerApp.this,
                                "Erreur de chargement:\n" + 
                                item.path.getFileName() + "\n\n" +
                                e.getMessage() + "\n\n" +
                                "Vérifiez que le fichier est un FITS valide.",
                                "Erreur de format",
                                JOptionPane.ERROR_MESSAGE);
                        });
                    } catch (Exception e) {
                        log("✗ ERREUR lors du chargement de " + item.path.getFileName() + 
                            ": " + e.getMessage());
                        e.printStackTrace();
                    }
                }
                
                if (loadedImages.isEmpty()) {
                    log("✗ Aucune image n'a pu être chargée");
                    SwingUtilities.invokeLater(() -> {
                        JOptionPane.showMessageDialog(FitsStackerApp.this,
                            "Aucune image n'a pu être chargée.\n" +
                            "Vérifiez que vos fichiers sont au format FITS valide.",
                            "Erreur",
                            JOptionPane.ERROR_MESSAGE);
                    });
                    return null;
                }
                
                log("Images chargées avec succès: " + loadedImages.size() + "/" + fileListModel.size());
                
                // Align
                ImageAligner.alignImages(loadedImages, (progress, message) -> {
                    SwingUtilities.invokeLater(() -> {
                        progressBar.setValue(progress);
                        updateStatus(message);
                    });
                });
                
                imagesAligned = true;
                log("✓ Alignement terminé avec succès");
                
                return null;
            }
            
            @Override
            protected void done() {
                setButtonsEnabled(true);
                progressBar.setValue(0);
                if (!loadedImages.isEmpty()) {
                    updateStatus("Alignement terminé - Prêt pour l'empilement");
                } else {
                    updateStatus("Échec du chargement des images");
                }
            }
        };
        
        worker.execute();
    }

    private void stackImages() {
        if (!imagesAligned) {
            int result = JOptionPane.showConfirmDialog(this,
                "Les images n'ont pas été alignées. Voulez-vous continuer?",
                "Confirmation", JOptionPane.YES_NO_OPTION);
            if (result != JOptionPane.YES_OPTION) {
                return;
            }
        }
        
        if (loadedImages.isEmpty()) {
            JOptionPane.showMessageDialog(this,
                "Veuillez charger et aligner les images d'abord",
                "Erreur", JOptionPane.ERROR_MESSAGE);
            return;
        }
        
        JFileChooser chooser = new JFileChooser();
        chooser.setDialogTitle("Enregistrer l'image empilée");
        chooser.setSelectedFile(new File("stacked_image.fits"));
        
        if (chooser.showSaveDialog(this) != JFileChooser.APPROVE_OPTION) {
            return;
        }
        
        Path outputPath = chooser.getSelectedFile().toPath();
        StackingEngine.StackingMethod method = 
            (StackingEngine.StackingMethod) methodCombo.getSelectedItem();
        
        SwingWorker<Void, Void> worker = new SwingWorker<>() {
            @Override
            protected Void doInBackground() throws Exception {
                setButtonsEnabled(false);
                log("=== Début de l'empilement ===");
                log("Méthode: " + method.getDisplayName());
                log("Nombre d'images: " + loadedImages.size());
                
                FitsImage result = StackingEngine.stackImages(loadedImages, method,
                    (progress, message) -> {
                        SwingUtilities.invokeLater(() -> {
                            progressBar.setValue(progress);
                            updateStatus(message);
                        });
                    });
                
                updateStatus("Enregistrement du résultat...");
                result.saveFits(outputPath);
                
                log("Image empilée enregistrée: " + outputPath.getFileName());
                log("=== Empilement terminé ===");
                
                return null;
            }
            
            @Override
            protected void done() {
                setButtonsEnabled(true);
                progressBar.setValue(0);
                updateStatus("Empilement terminé avec succès!");
                JOptionPane.showMessageDialog(FitsStackerApp.this,
                    "Image empilée créée avec succès!",
                    "Succès", JOptionPane.INFORMATION_MESSAGE);
            }
        };
        
        worker.execute();
    }

    private void setButtonsEnabled(boolean enabled) {
        addFilesButton.setEnabled(enabled);
        addFolderButton.setEnabled(enabled);
        removeButton.setEnabled(enabled);
        clearButton.setEnabled(enabled);
        alignButton.setEnabled(enabled);
        stackButton.setEnabled(enabled);
        methodCombo.setEnabled(enabled);
    }

    private void updateStatus(String message) {
        statusLabel.setText(message);
    }

    private void log(String message) {
        SwingUtilities.invokeLater(() -> {
            logArea.append(message + "\n");
            logArea.setCaretPosition(logArea.getDocument().getLength());
        });
    }

    private static class FileItem {
        final Path path;

        FileItem(Path path) {
            this.path = path;
        }

        @Override
        public String toString() {
            return path.getFileName().toString();
        }
    }

    private static class FileItemRenderer extends DefaultListCellRenderer {
        @Override
        public Component getListCellRendererComponent(JList<?> list, Object value,
                int index, boolean isSelected, boolean cellHasFocus) {
            super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
            
            if (value instanceof FileItem item) {
                setText((index + 1) + ". " + item.path.getFileName());
                setIcon(UIManager.getIcon("FileView.fileIcon"));
            }
            
            return this;
        }
    }

    public static void main(String[] args) {
        try {
            FlatDarkLaf.setup();
            UIManager.put("Button.arc", 8);
            UIManager.put("Component.arc", 8);
            UIManager.put("TextComponent.arc", 8);
        } catch (Exception e) {
            e.printStackTrace();
        }
        
        SwingUtilities.invokeLater(() -> {
            FitsStackerApp app = new FitsStackerApp();
            app.setVisible(true);
        });
    }
}
