package org.example;

import java.io.File;
import java.io.IOException;
import java.nio.file.*;
import java.util.List;

public class FileManagerController {

    private FileManagerService service;
    private String currentDirectory;
    private String cutFilePath; // Store the path of the cut file

    public FileManagerController() {
        this.service = new FileManagerService();
        this.currentDirectory = System.getProperty("user.home"); // Default to user's home directory
    }

    public List<File> listFiles(String directoryPath) {
        this.currentDirectory = directoryPath; // Update the current directory
        return service.listFiles(directoryPath);
    }

    public List<File> searchFiles(String query) {
        return service.searchFiles(query, currentDirectory);
    }

    public void createNewFolder(String folderName) throws IOException {
        String folderPath = currentDirectory + File.separator + folderName;
        service.createNewDirectory(folderPath);
    }

    public void copyFile(String sourcePath, String destinationDirectory) throws IOException {
        Path source = Paths.get(sourcePath);
        File sourceFile = source.toFile();
        String fileName = sourceFile.getName(); // Get the file name with extension
        String baseName = fileName;
        String extension = "";

        // Split the file name into base name and extension
        int dotIndex = fileName.lastIndexOf('.');
        if (dotIndex != -1) {
            baseName = fileName.substring(0, dotIndex);
            extension = fileName.substring(dotIndex); // Include the dot (e.g., ".png")
        }

        // Construct the initial destination path with file name
        Path destination = Paths.get(destinationDirectory).resolve(fileName);

        // Ensure source file exists
        if (Files.notExists(source)) {
            throw new IOException("Source file does not exist: " + sourcePath);
        }

        // Ensure destination directory exists
        if (Files.notExists(destination.getParent())) {
            Files.createDirectories(destination.getParent());
        }

        // Check if a file with the same name already exists and update the name if necessary
        int counter = 1;
        while (Files.exists(destination)) {
            String newFileName = baseName + counter + extension; // Create a new file name with the counter
            destination = Paths.get(destinationDirectory).resolve(newFileName);
            counter++;
        }

        // Perform the copy operation
        Files.copy(source, destination, StandardCopyOption.REPLACE_EXISTING);
        FileManagerLogger.logInfo("Copied file from " + sourcePath + " to " + destination.toString());
    }


    public void moveFile(String sourcePath, String destinationDirectory) throws IOException {
        Path source = Paths.get(sourcePath);
        File sourceFile = source.toFile();
        String fileName = sourceFile.getName(); // Get the file name with extension

        // Construct the destination path with file name
        Path destination = Paths.get(destinationDirectory).resolve(fileName);

        // Ensure source file exists
        if (Files.notExists(source)) {
            throw new IOException("Source file does not exist: " + sourcePath);
        }

        // Ensure destination directory exists
        if (Files.notExists(destination.getParent())) {
            Files.createDirectories(destination.getParent());
        }

        // Perform the move operation
        Files.move(source, destination, StandardCopyOption.REPLACE_EXISTING);
        FileManagerLogger.logInfo("Moved file from " + sourcePath + " to " + destination.toString());
    }

    public void deleteFile(String filePath) throws IOException {
        Path path = Paths.get(filePath);
        Files.delete(path);
        FileManagerLogger.logInfo("Deleted file: " + filePath);
    }

    public void setCutFile(String filePath) {
        this.cutFilePath = filePath;
    }

    public void pasteFile(String destinationDirectory) throws IOException {
        if (cutFilePath == null) {
            throw new IllegalStateException("No file to paste.");
        }

        // Extract the file name from the cutFilePath
        String fileName = new File(cutFilePath).getName();
        // Construct the destination path
        Path destinationPath = Paths.get(destinationDirectory).resolve(fileName);

        // Move the file using the service
        service.moveFile(cutFilePath, destinationPath.toString());
        // Clear the cutFilePath after successful move
        cutFilePath = null;
        FileManagerLogger.logInfo("Pasted file from " + cutFilePath + " to " + destinationPath.toString());
    }

    public void openFolder(String folderName) {
        String folderPath = currentDirectory + File.separator + folderName;
        if (new File(folderPath).isDirectory()) {
            currentDirectory = folderPath;
        }
    }

    public void setCurrentDirectory(String newDirectory) {
        this.currentDirectory = newDirectory;
    }

    public String getCurrentDirectory() {
        return currentDirectory;
    }
}
