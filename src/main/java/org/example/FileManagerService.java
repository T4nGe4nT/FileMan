package org.example;

import java.io.File;
import java.io.IOException;
import java.nio.file.*;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

public class FileManagerService {
    private String currentDirectory;

    public FileManagerService() {
        this.currentDirectory = System.getProperty("user.home"); // Default to user's home directory
    }

    public String getCurrentDirectory() {
        return currentDirectory;
    }

    public List<File> listFiles(String directoryPath) {
        File directory = new File(directoryPath);
        if (directory.isDirectory()) {
            File[] files = directory.listFiles();
            if (files != null) {
                return List.of(files);
            }
        }
        return List.of(); // Return an empty list if the directory is invalid or an error occurs
    }

    public List<File> searchFiles(String query, String directoryPath) {
        List<File> allFiles = listFiles(directoryPath);
        return allFiles.stream()
                .filter(file -> file.getName().contains(query))
                .collect(Collectors.toList());
    }

    public void copyFile(String sourcePath, String destinationPath) throws IOException {
        Path source = Paths.get(sourcePath);
        Path destination = Paths.get(destinationPath);
        Files.copy(source, destination, StandardCopyOption.REPLACE_EXISTING);
    }

    public void moveFile(String sourcePath, String destinationPath) throws IOException {
        Path source = Paths.get(sourcePath);
        Path destination = Paths.get(destinationPath);
        Files.move(source, destination, StandardCopyOption.REPLACE_EXISTING);
    }

    public void deleteFile(String filePath) throws IOException {
        Path path = Paths.get(filePath);
        Files.delete(path);
    }

    public void createNewDirectory(String directoryPath) throws IOException {
        Path path = Paths.get(directoryPath);
        Files.createDirectory(path);
    }

    public void deleteDirectory(String directoryPath) throws IOException {
        Path path = Paths.get(directoryPath);
        Files.walk(path)
                .sorted(Comparator.reverseOrder())
                .map(Path::toFile)
                .forEach(File::delete);
    }

    public boolean changeDirectory(String newDirectoryPath) {
        File newDirectory = new File(newDirectoryPath);
        if (newDirectory.isDirectory()) {
            currentDirectory = newDirectoryPath;
            return true;
        }
        return false; // Return false if the new path is not a directory
    }
}



