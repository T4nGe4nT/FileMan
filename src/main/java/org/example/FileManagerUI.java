package org.example;

import javax.swing.*;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.Desktop;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.BasicFileAttributes;
import java.text.CharacterIterator;
import java.text.SimpleDateFormat;
import java.text.StringCharacterIterator;
import java.util.concurrent.ExecutionException;

public class FileManagerUI {
    private JFrame frame;
    private JTable fileTable;
    private DefaultTableModel tableModel;
    private JTextField pathField;
    private JTextField searchField;
    private FileManagerController controller;

    public FileManagerUI(FileManagerController controller) {
        this.controller = controller;
    }

    public void createAndShowGUI() {
        frame = new JFrame("File Manager");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setSize(1000, 600); // Set the width to 1000 and height to 600

        frame.setLayout(new BorderLayout());

        // Top panel for displaying the current file path
        JPanel topPanel = new JPanel(new BorderLayout());
        pathField = new JTextField();
        pathField.setEditable(false); // Make the path field read-only
        topPanel.add(pathField, BorderLayout.CENTER);
        frame.add(topPanel, BorderLayout.NORTH);

        // Update: Add a "Type" column to the table model
        tableModel = new DefaultTableModel(new String[]{"Name", "Size", "Type", "Last Modified"}, 0);
        fileTable = new JTable(tableModel);
        JScrollPane scrollPane = new JScrollPane(fileTable);
        frame.add(scrollPane, BorderLayout.CENTER);

        fileTable.getSelectionModel().addListSelectionListener(new ListSelectionListener() {
            @Override
            public void valueChanged(ListSelectionEvent event) {
                int selectedRow = fileTable.getSelectedRow();
                if (selectedRow != -1) {
                    String fileName = (String) tableModel.getValueAt(selectedRow, 0);
                    File file = new File(controller.getCurrentDirectory() + File.separator + fileName);
                    pathField.setText(file.getAbsolutePath()); // Display selected file path
                }
            }
        });

        // Bottom panel for buttons and search field
        JPanel buttonPanel = new JPanel();
        JButton backButton = new JButton("Back");
        JButton openButton = new JButton("Open");
        JButton createButton = new JButton("Create");
        JButton copyButton = new JButton("Copy");
        JButton moveButton = new JButton("Move");
        JButton deleteButton = new JButton("Delete");

        searchField = new JTextField(20); // Search field with specified width
        JButton searchButton = new JButton("Search");

        buttonPanel.add(openButton);
        buttonPanel.add(backButton);
        buttonPanel.add(createButton);
        buttonPanel.add(copyButton);
        buttonPanel.add(moveButton);
        buttonPanel.add(deleteButton);

        // Add search field and button to the button panel
        buttonPanel.add(searchField);
        buttonPanel.add(searchButton);

        frame.add(buttonPanel, BorderLayout.SOUTH);

        createButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                createNewFolder();
            }
        });

        copyButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                try {
                    copySelectedFile();
                } catch (IOException ex) {
                    JOptionPane.showMessageDialog(frame, "Error copying file: " + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
                }
            }
        });

        moveButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                try {
                    moveSelectedFile();
                } catch (IOException ex) {
                    JOptionPane.showMessageDialog(frame, "Error moving file: " + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
                }
            }
        });

        deleteButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                try {
                    deleteSelectedFile();
                } catch (IOException ex) {
                    JOptionPane.showMessageDialog(frame, "Error deleting file: " + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
                }
            }
        });

        openButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                openSelectedFile();
            }
        });

        backButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                navigateToParentDirectory();
            }
        });

        searchButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                performSearch();
            }
        });

        frame.setVisible(true);

        loadDirectoryContents(System.getProperty("user.home"));
    }

    private void loadDirectoryContents(String path) {
        controller.setCurrentDirectory(path);
        File currentDir = new File(path);
        pathField.setText(currentDir.getAbsolutePath()); // Set the path field to the current directory

        tableModel.setRowCount(0); // Clear the table

        File[] files = currentDir.listFiles();
        if (files != null) {
            for (File file : files) {
                try {
                    Path filePath = file.toPath();
                    BasicFileAttributes attrs = Files.readAttributes(filePath, BasicFileAttributes.class);
                    long lastModified = attrs.lastModifiedTime().toMillis();
                    String formattedDate = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(lastModified);
                    String size = file.isDirectory() ? "--" : humanReadableByteCountSI(attrs.size());
                    String type = file.isDirectory() ? "Directory" : "File";
                    tableModel.addRow(new Object[]{file.getName(), size, type, formattedDate});

                    // If it's a directory, you can calculate its size asynchronously if needed
                    if (file.isDirectory()) {
                        new DirectorySizeCalculator(file, tableModel.getRowCount() - 1).execute();
                    }

                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }
    }

    private class DirectorySizeCalculator extends SwingWorker<Long, Void> {
        private final File directory;
        private final int rowIndex;

        public DirectorySizeCalculator(File directory, int rowIndex) {
            this.directory = directory;
            this.rowIndex = rowIndex;
        }

        @Override
        protected Long doInBackground() throws Exception {
            return getDirectorySize(directory);
        }

        @Override
        protected void done() {
            try {
                long size = get();
                String readableSize = humanReadableByteCountSI(size);
                tableModel.setValueAt(readableSize, rowIndex, 1);
            } catch (InterruptedException | ExecutionException e) {
                e.printStackTrace();
            }
        }
    }

    private long getDirectorySize(File directory) {
        long size = 0;
        File[] files = directory.listFiles();
        if (files != null) {
            for (File file : files) {
                if (file.isFile()) {
                    size += file.length();
                } else {
                    size += getDirectorySize(file);
                }
            }
        }
        return size;
    }

    private void performSearch() {
        String query = searchField.getText().trim();

        if (query.isEmpty()) {
            // If the search field is empty, reload the current directory
            loadDirectoryContents(controller.getCurrentDirectory());
            return;
        }

        File currentDir = new File(controller.getCurrentDirectory());
        File[] files = currentDir.listFiles();

        tableModel.setRowCount(0); // Clear the table

        if (files != null) {
            for (File file : files) {
                if (file.getName().toLowerCase().contains(query.toLowerCase())) {
                    long lastModified = file.lastModified();
                    String formattedDate = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(lastModified);
                    String size = file.isDirectory() ? "" : humanReadableByteCountSI(file.length());
                    String type = file.isDirectory() ? "Directory" : "File";
                    tableModel.addRow(new Object[]{file.getName(), size, type, formattedDate});
                }
            }
        }
    }

    private void createNewFolder() {
        String folderName = JOptionPane.showInputDialog(frame, "Enter the name of the new folder:");
        if (folderName != null && !folderName.trim().isEmpty()) {
            File newFolder = new File(controller.getCurrentDirectory() + File.separator + folderName);
            if (newFolder.mkdir()) {
                JOptionPane.showMessageDialog(frame, "Folder created successfully.", "Success", JOptionPane.INFORMATION_MESSAGE);
                loadDirectoryContents(controller.getCurrentDirectory());
            } else {
                JOptionPane.showMessageDialog(frame, "Failed to create folder.", "Error", JOptionPane.ERROR_MESSAGE);
            }
        }
    }

    private void copySelectedFile() throws IOException {
        int selectedRow = fileTable.getSelectedRow();
        if (selectedRow != -1) {
            String fileName = (String) tableModel.getValueAt(selectedRow, 0);
            File sourceFile = new File(controller.getCurrentDirectory() + File.separator + fileName);

            JFileChooser fileChooser = new JFileChooser();
            fileChooser.setDialogTitle("Select Destination Directory");
            fileChooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);

            int returnValue = fileChooser.showOpenDialog(frame);
            if (returnValue == JFileChooser.APPROVE_OPTION) {
                File destinationDirectory = fileChooser.getSelectedFile();
                File destinationFile = new File(destinationDirectory, sourceFile.getName());

                if (destinationFile.exists()) {
                    int choice = JOptionPane.showConfirmDialog(frame, "File already exists. Do you want to rename it?", "File Exists", JOptionPane.YES_NO_OPTION);
                    if (choice == JOptionPane.YES_OPTION) {
                        destinationFile = getNonConflictingFile(destinationDirectory, sourceFile.getName());
                    }
                }

                controller.copyFile(sourceFile.getAbsolutePath(), destinationFile.getAbsolutePath()); // Use getAbsolutePath() to pass Strings
                JOptionPane.showMessageDialog(frame, "File copied successfully.", "Success", JOptionPane.INFORMATION_MESSAGE);
            }
        }
    }

    private File getNonConflictingFile(File directory, String fileName) {
        File file = new File(directory, fileName);
        String name = file.getName();
        String extension = "";
        int dotIndex = name.lastIndexOf('.');
        if (dotIndex > 0) {
            extension = name.substring(dotIndex); // includes the dot
            name = name.substring(0, dotIndex);
        }
        int count = 1;
        while (file.exists()) {
            file = new File(directory, name + "_" + count + extension);
            count++;
        }
        return file;
    }

    private void moveSelectedFile() throws IOException {
        int selectedRow = fileTable.getSelectedRow();
        if (selectedRow != -1) {
            String fileName = (String) tableModel.getValueAt(selectedRow, 0);
            File sourceFile = new File(controller.getCurrentDirectory() + File.separator + fileName);

            JFileChooser fileChooser = new JFileChooser();
            fileChooser.setDialogTitle("Select Destination Directory");
            fileChooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);

            int returnValue = fileChooser.showOpenDialog(frame);
            if (returnValue == JFileChooser.APPROVE_OPTION) {
                File destinationDirectory = fileChooser.getSelectedFile();
                File destinationFile = new File(destinationDirectory, sourceFile.getName());

                controller.moveFile(sourceFile.getAbsolutePath(), destinationFile.getAbsolutePath()); // Use getAbsolutePath() to pass Strings
                JOptionPane.showMessageDialog(frame, "File moved successfully.", "Success", JOptionPane.INFORMATION_MESSAGE);
                loadDirectoryContents(controller.getCurrentDirectory());
            }
        }
    }

    private void deleteSelectedFile() throws IOException {
        int selectedRow = fileTable.getSelectedRow();
        if (selectedRow != -1) {
            String fileName = (String) tableModel.getValueAt(selectedRow, 0);
            File fileToDelete = new File(controller.getCurrentDirectory() + File.separator + fileName);

            int choice = JOptionPane.showConfirmDialog(frame, "Are you sure you want to delete this file?", "Confirm Delete", JOptionPane.YES_NO_OPTION);
            if (choice == JOptionPane.YES_OPTION) {
                controller.deleteFile(fileToDelete.getAbsolutePath()); // Use getAbsolutePath() to pass Strings
                JOptionPane.showMessageDialog(frame, "File deleted successfully.", "Success", JOptionPane.INFORMATION_MESSAGE);
                loadDirectoryContents(controller.getCurrentDirectory());
            }
        }
    }

    private void openSelectedFile() {
        int selectedRow = fileTable.getSelectedRow();
        if (selectedRow != -1) {
            String fileName = (String) tableModel.getValueAt(selectedRow, 0);
            File fileToOpen = new File(controller.getCurrentDirectory() + File.separator + fileName);

            if (fileToOpen.isDirectory()) {
                // If the selected file is a directory, navigate into it
                loadDirectoryContents(fileToOpen.getAbsolutePath());
            } else {
                // If it's a file, open it using the system's default application
                if (Desktop.isDesktopSupported()) {
                    try {
                        Desktop.getDesktop().open(fileToOpen);
                    } catch (IOException ex) {
                        JOptionPane.showMessageDialog(frame, "Error opening file: " + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
                    }
                } else {
                    JOptionPane.showMessageDialog(frame, "Desktop operations are not supported on this system.", "Error", JOptionPane.ERROR_MESSAGE);
                }
            }
        }
    }

    private void navigateToParentDirectory() {
        File currentDir = new File(controller.getCurrentDirectory());
        File parentDir = currentDir.getParentFile();
        if (parentDir != null) {
            loadDirectoryContents(parentDir.getAbsolutePath());
        }
    }

    public static String humanReadableByteCountSI(long bytes) {
        if (-1000 < bytes && bytes < 1000) {
            return bytes + " B";
        }
        CharacterIterator ci = new StringCharacterIterator("kMGTPE");
        while (bytes <= -999_950 || bytes >= 999_950) {
            bytes /= 1000;
            ci.next();
        }
        return String.format("%.1f %cB", bytes / 1000.0, ci.current());
    }
}

