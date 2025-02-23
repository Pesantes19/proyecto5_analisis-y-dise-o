import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.*;
import java.sql.*;
import java.util.*;
import javax.swing.*;
import javax.swing.table.DefaultTableModel;

public class Server {
    private static final int PORT = 5555;
    private static List<ClientHandler> clients = new ArrayList<>();
    private static DefaultTableModel tableModel;
    private static JTable connectionsTable;

    public static void main(String[] args) {
        JFrame frame = new JFrame("Conexiones Activas");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setSize(400, 300);

        tableModel = new DefaultTableModel();
        tableModel.addColumn("IP");
        tableModel.addColumn("Usuario");
        tableModel.addColumn("Estado");

        connectionsTable = new JTable(tableModel);
        frame.add(new JScrollPane(connectionsTable));

        frame.setVisible(true);

        try (ServerSocket serverSocket = new ServerSocket(PORT)) {
            System.out.println("Servidor iniciado en el puerto " + PORT);
            while (true) {
                Socket socket = serverSocket.accept();
                ClientHandler client = new ClientHandler(socket);
                clients.add(client);
                updateConnectionsTable();
                new Thread(client).start();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private static void updateConnectionsTable() {
        SwingUtilities.invokeLater(() -> {
            tableModel.setRowCount(0); // Limpiar la tabla antes de actualizar
            for (ClientHandler client : clients) {
                tableModel.addRow(new Object[]{client.getClientIP(), client.getUsername(), client.getConnectionStatus()});
            }
        });
    }

    static class ClientHandler implements Runnable {
        private Socket socket;
        private PrintWriter out;
        private BufferedReader in;
        private String clientIP;
        private String username;
        private String connectionStatus;

        public ClientHandler(Socket socket) {
            this.socket = socket;
            this.clientIP = socket.getInetAddress().getHostAddress();
            this.connectionStatus = "Conectado";  // Establecer estado inicial
        }

        public String getClientIP() {
            return clientIP;
        }

        public String getUsername() {
            return username;
        }

        public String getConnectionStatus() {
            return connectionStatus;
        }

        @Override
        public void run() {
            try {
                in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
                out = new PrintWriter(socket.getOutputStream(), true);

                String clientUsername = in.readLine(); // Leer el nombre de usuario
                String password = in.readLine();

                if (authenticateUser(clientUsername, password)) {
                    username = clientUsername; // Guardar el nombre de usuario
                    out.println("Autenticación exitosa.");
                } else {
                    out.println("Error: Credenciales incorrectas.");
                    return;
                }

                // Actualizar la tabla de conexiones activas
                updateConnectionsTable();

                // Esperar hasta que el cliente cierre la conexión
                String line;
                while ((line = in.readLine()) != null) {
                    // Aquí puedes manejar otros mensajes del cliente si es necesario
                }

            } catch (IOException e) {
                e.printStackTrace();
            } finally {
                // Cuando el cliente se desconecta, cambiar su estado a "Desconectado"
                connectionStatus = "Desconectado";
                updateConnectionsTable();
                try {
                    socket.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }

        private boolean authenticateUser(String username, String password) {
            try (Connection conn = DriverManager.getConnection("jdbc:mysql://localhost:3306/juego_autenticacion", "root", "1234")) {
                String query = "SELECT * FROM usuarios WHERE username = ? AND password = ?";
                try (PreparedStatement pst = conn.prepareStatement(query)) {
                    pst.setString(1, username);
                    pst.setString(2, password);
                    ResultSet rs = pst.executeQuery();
                    return rs.next();
                }
            } catch (SQLException ex) {
                ex.printStackTrace();
                return false;
            }
        }
    }
}
