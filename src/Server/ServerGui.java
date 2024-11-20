package Server;

import javafx.application.Application;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

import java.io.*;
import java.net.*;
import java.util.HashMap;

public class ServerGui extends Application {

    private static final int PORT = 1234;
    private static HashMap<Socket, PrintWriter> clientConnections = new HashMap<>(); // Mapa pentru a stoca clientii conectati

    public static void main(String[] args) {
        launch(args);
    }

    @Override
    public void start(Stage primaryStage) {
        // Eticheta pentru statusul serverului
        Label statusLabel = new Label("Status: Serverul este oprit");

        // Buton pentru a porni serverul
        Button startButton = new Button("Pornește Serverul");
        startButton.setOnAction(e -> startServer(statusLabel));

        // Layout-ul principal
        VBox layout = new VBox(10, startButton, statusLabel);
        Scene scene = new Scene(layout, 300, 200);

        primaryStage.setTitle("Server - Receput Fișiere");
        primaryStage.setScene(scene);
        primaryStage.show();
    }

    public void startServer(Label statusLabel) {
        // Actualizează interfața cu statusul
        statusLabel.setText("Status: Serverul a pornit...");

        // Lansează serverul într-un thread separat
        new Thread(() -> {
            try {
                ServerSocket serverSocket = new ServerSocket(PORT);
                System.out.println("Serverul a pornit la portul " + PORT);

                // Ascultă pentru noi clienți care se conectează
                while (true) {
                    Socket clientSocket = serverSocket.accept();
                    System.out.println("Conexiune nouă: " + clientSocket.getInetAddress());

                    // Adaugă clientul la mapă
                    PrintWriter writer = new PrintWriter(clientSocket.getOutputStream(), true);
                    clientConnections.put(clientSocket, writer);

                    // Trimite un mesaj de bun venit clientului
                    writer.println("Bine ai venit pe server!");

                    // Creează un nou thread pentru a gestiona clientul
                    new Thread(new ClientHandler(clientSocket)).start();
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }).start();
    }

    // Handler pentru fiecare client
    class ClientHandler implements Runnable {
        private Socket clientSocket;
        private BufferedReader reader;
        private PrintWriter writer;

        public ClientHandler(Socket clientSocket) {
            this.clientSocket = clientSocket;
            try {
                this.reader = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
                this.writer = new PrintWriter(clientSocket.getOutputStream(), true);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        @Override
        public void run() {
            try {
                String clientMessage;
                while ((clientMessage = reader.readLine()) != null) {
                    if ("ping".equalsIgnoreCase(clientMessage)) {
                        // Dacă serverul primește ping, răspunde
                        writer.println("pong");
                    } else if ("sendfile".equalsIgnoreCase(clientMessage)) {
                        // Când clientul trimite fișierul
                        String fileName = reader.readLine(); // Citim numele fișierului
                        long fileSize = Long.parseLong(reader.readLine()); // Citim dimensiunea fișierului

                        // Creăm un fișier unde să îl salvăm pe server
                        File file = new File("server_files/" + fileName);
                        file.getParentFile().mkdirs(); // Asigură-te că directorul există
                        try (FileOutputStream fos = new FileOutputStream(file)) {
                            byte[] buffer = new byte[1024];
                            long bytesRead = 0;
                            while (bytesRead < fileSize) {
                                int byteCount = clientSocket.getInputStream().read(buffer);
                                fos.write(buffer, 0, byteCount);
                                bytesRead += byteCount;
                            }
                            System.out.println("Fișierul a fost salvat: " + file.getAbsolutePath());
                            writer.println("Fișierul a fost salvat cu succes!");
                        } catch (IOException e) {
                            e.printStackTrace();
                            writer.println("Eroare la salvarea fișierului.");
                        }
                    }
                }
            } catch (IOException e) {
                e.printStackTrace();
            } finally {
                try {
                    clientSocket.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }



    // Metoda pentru a trimite mesaje tuturor clienților conectați
    public static void sendMessageToAllClients(String message) {
        for (PrintWriter writer : clientConnections.values()) {
            writer.println(message);
        }
    }

    // Verifică periodic clienții conectați
    public static void checkClients() {
        new Thread(() -> {
            while (true) {
                try {
                    Thread.sleep(5000);  // Verifică la fiecare 5 secunde
                    for (Socket socket : clientConnections.keySet()) {
                        PrintWriter writer = clientConnections.get(socket);
                        writer.println("ping");  // Trimite un ping clientului
                    }
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }).start();
    }
}
