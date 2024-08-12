package org.example;

public class Main {
    public static void main(String[] args) {
        javax.swing.SwingUtilities.invokeLater(() -> {
            FileManagerController controller = new FileManagerController(); // Create controller instance
            FileManagerUI ui = new FileManagerUI(controller); // Pass the controller to the UI
            ui.createAndShowGUI();
        });
    }
}


