package Client;

import javafx.application.Application;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.VBox;

import java.io.*;
import java.net.*;

public class ClientGui extends Application {

    private static final String SERVER_ADDRESS = "localhost";
    private static final int SERVER_PORT = 1234;
    private static Socket socket;
    private static PrintWriter writer;
    private static BufferedReader reader;

    private File selectedFile;
    private TextField fileNameField;

    public static void main(String[] args) {
        launch(args);
    }

    @Override
    public void start(Stage primaryStage) {
        // Crearea unui buton pentru alegerea fișierului
        Button selectFileButton = new Button("Alege Fișierul");
        Button sendFileButton = new Button("Trimite Fișierul");

        // Câmp text pentru a modifica numele fișierului
        fileNameField = new TextField();
        fileNameField.setPromptText("Introduceți numele fișierului");

        // Label pentru mesaje
        Label statusLabel = new Label("Așteaptă conexiunea...");

        // Inițializare conexiune la server
        try {
            socket = new Socket(SERVER_ADDRESS, SERVER_PORT);
            writer = new PrintWriter(socket.getOutputStream(), true);
            reader = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            statusLabel.setText("Conectat la server!");
        } catch (IOException e) {
            statusLabel.setText("Conexiune eșuată!");
            e.printStackTrace();
        }

        // Alege fișierul
        FileChooser fileChooser = new FileChooser();
        fileChooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("Toate fișierele", "*.*"));

        selectFileButton.setOnAction(e -> {
            selectedFile = fileChooser.showOpenDialog(primaryStage);
            if (selectedFile != null) {
                // Actualizează câmpul cu numele fișierului selectat
                fileNameField.setText(selectedFile.getName());
            }
        });

        // Trimite fișierul
        sendFileButton.setOnAction(e -> {
            if (selectedFile != null && selectedFile.exists()) {
                sendFile(selectedFile);
            } else {
                statusLabel.setText("Fișierul nu este valid!");
            }
        });

        // Layout
        VBox layout = new VBox(10, selectFileButton, fileNameField, sendFileButton, statusLabel);
        Scene scene = new Scene(layout, 400, 300);
        primaryStage.setTitle("Client - Trimite Fișiere");
        primaryStage.setScene(scene);
        primaryStage.show();
    }

    // Trimite fișierul la server
    private void sendFile(File file) {
        try {
            // Modifică numele fișierului conform câmpului de text
            String fileNameToSend = fileNameField.getText().trim();
            if (fileNameToSend.isEmpty()) {
                fileNameToSend = file.getName();
            }

            // Trimite fișierul
            BufferedReader fileReader = new BufferedReader(new FileReader(file));
            writer.println("sendfile");
            writer.println(fileNameToSend);
            writer.println(file.length());

            byte[] buffer = new byte[1024];
            FileInputStream fileInputStream = new FileInputStream(file);
            int bytesRead;

            while ((bytesRead = fileInputStream.read(buffer)) != -1) {
                socket.getOutputStream().write(buffer, 0, bytesRead);
            }

            fileInputStream.close();
            System.out.println("Fișierul a fost trimis cu succes.");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
