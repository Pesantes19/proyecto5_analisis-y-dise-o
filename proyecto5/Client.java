import java.awt.*;
import java.io.*;
import java.net.*;
import java.sql.*;
import javax.swing.*;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.table.DefaultTableModel;

public class Client {
    private static final String SERVER_ADDRESS = "127.0.0.1";
    private static final int SERVER_PORT = 5555;
    private static Socket socket;
    private static PrintWriter out;
    private static BufferedReader in;
    private static JFrame frame;
    private static JTextField usernameField;
    private static JPasswordField passwordField;

    public static void main(String[] args) {
        SwingUtilities.invokeLater(Client::createAuthUI);
    }

    private static void createAuthUI() {
        frame = new JFrame("Autenticación");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setSize(400, 300);
        frame.setLocationRelativeTo(null);

        JPanel panel = new JPanel(new GridLayout(3, 2, 10, 10));
        panel.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));

        panel.add(new JLabel("Usuario:"));
        usernameField = new JTextField();
        panel.add(usernameField);

        panel.add(new JLabel("Contraseña:"));
        passwordField = new JPasswordField();
        panel.add(passwordField);

        JButton loginButton = new JButton("Iniciar sesión");
        loginButton.addActionListener(e -> handleLogin());
        panel.add(loginButton);

        JButton registerButton = new JButton("Registrar nuevo usuario");
        registerButton.addActionListener(e -> showRegistrationForm());
        panel.add(registerButton);

        frame.add(panel);
        frame.setVisible(true);
    }

    private static void handleLogin() {
        String username = usernameField.getText();
        String password = new String(passwordField.getPassword());
        try {
            socket = new Socket(SERVER_ADDRESS, SERVER_PORT);
            in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            out = new PrintWriter(socket.getOutputStream(), true);

            out.println(username);
            out.println(password);

            String response = in.readLine();
            if ("Autenticación exitosa.".equals(response)) {
                JOptionPane.showMessageDialog(frame, "Bienvenido " + username);
                createMainUI();
            } else {
                JOptionPane.showMessageDialog(frame, "Credenciales incorrectas.");
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private static void showRegistrationForm() {
        JPanel panel = new JPanel(new GridLayout(3, 2, 10, 10));

        JTextField newUsernameField = new JTextField();
        JPasswordField newPasswordField = new JPasswordField();

        panel.add(new JLabel("Nuevo usuario:"));
        panel.add(newUsernameField);
        panel.add(new JLabel("Contraseña:"));
        panel.add(newPasswordField);

        int result = JOptionPane.showConfirmDialog(frame, panel, "Registrar nuevo usuario", JOptionPane.OK_CANCEL_OPTION);
        if (result == JOptionPane.OK_OPTION) {
            String newUsername = newUsernameField.getText();
            String newPassword = new String(newPasswordField.getPassword());
            registerNewUser(newUsername, newPassword);
        }
    }

    private static void registerNewUser(String username, String password) {
        try (Connection conn = DriverManager.getConnection("jdbc:mysql://localhost:3306/juego_autenticacion", "root", "1234")) {
            String query = "INSERT INTO usuarios (username, password) VALUES (?, ?)";
            try (PreparedStatement pst = conn.prepareStatement(query)) {
                pst.setString(1, username);
                pst.setString(2, password);
                pst.executeUpdate();
                JOptionPane.showMessageDialog(frame, "Usuario registrado exitosamente.");
            }
        } catch (SQLException e) {
            e.printStackTrace();
            JOptionPane.showMessageDialog(frame, "Error al registrar el usuario.");
        }
    }

    private static void createMainUI() {
        frame.getContentPane().removeAll();
        frame.setLayout(new FlowLayout());

        JButton addProductButton = new JButton("Ingresar Producto");
        addProductButton.addActionListener(e -> showProductForm());

        JButton viewProductsButton = new JButton("Consultar Producto");
        viewProductsButton.addActionListener(e -> showProductList());

        frame.add(addProductButton);
        frame.add(viewProductsButton);
        frame.revalidate();
        frame.repaint();
    }

    private static void showProductForm() {
        JTextField nameField = new JTextField();
        JTextField categoryField = new JTextField();
        JTextField shelfField = new JTextField();
        JTextField sectionField = new JTextField();

        JPanel panel = new JPanel(new GridLayout(5, 2, 10, 10));
        panel.add(new JLabel("Nombre:"));
        panel.add(nameField);
        panel.add(new JLabel("Categoría:"));
        panel.add(categoryField);
        panel.add(new JLabel("Percha:"));
        panel.add(shelfField);
        panel.add(new JLabel("Sección:"));
        panel.add(sectionField);

        int result = JOptionPane.showConfirmDialog(frame, panel, "Ingresar Producto", JOptionPane.OK_CANCEL_OPTION);
        if (result == JOptionPane.OK_OPTION) {
            saveProduct(nameField.getText(), categoryField.getText(), shelfField.getText(), sectionField.getText());
        }
    }

    private static void saveProduct(String name, String category, String shelf, String section) {
        try (Connection conn = DriverManager.getConnection("jdbc:mysql://localhost:3306/juego_autenticacion", "root", "1234")) {
            String query = "INSERT INTO productos (nombre, categoria, percha, seccion) VALUES (?, ?, ?, ?)";
            try (PreparedStatement pst = conn.prepareStatement(query)) {
                pst.setString(1, name);
                pst.setString(2, category);
                pst.setString(3, shelf);
                pst.setString(4, section);
                pst.executeUpdate();
                JOptionPane.showMessageDialog(frame, "Producto guardado.");
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    private static void showProductList() {
        JFrame productFrame = new JFrame("Lista de Productos");
        productFrame.setSize(500, 300);
        productFrame.setLocationRelativeTo(frame);

        JPanel searchPanel = new JPanel();
        JTextField searchField = new JTextField(20);
        searchPanel.add(new JLabel("Buscar por nombre:"));
        searchPanel.add(searchField);

        String[] columnNames = {"ID", "Nombre", "Categoría", "Percha", "Sección"};
        DefaultTableModel model = new DefaultTableModel(columnNames, 0);
        JTable table = new JTable(model);
        JScrollPane scrollPane = new JScrollPane(table);

        productFrame.setLayout(new BorderLayout());
        productFrame.add(searchPanel, BorderLayout.NORTH);
        productFrame.add(scrollPane, BorderLayout.CENTER);

        searchField.getDocument().addDocumentListener(new DocumentListener() {
            @Override
            public void insertUpdate(DocumentEvent e) {
                filterProductList(searchField.getText(), model);
            }

            @Override
            public void removeUpdate(DocumentEvent e) {
                filterProductList(searchField.getText(), model);
            }

            @Override
            public void changedUpdate(DocumentEvent e) {
                filterProductList(searchField.getText(), model);
            }
        });

        try (Connection conn = DriverManager.getConnection("jdbc:mysql://localhost:3306/juego_autenticacion", "root", "1234")) {
            String query = "SELECT * FROM productos";
            try (Statement stmt = conn.createStatement(); ResultSet rs = stmt.executeQuery(query)) {
                while (rs.next()) {
                    model.addRow(new Object[]{rs.getInt("id"), rs.getString("nombre"), rs.getString("categoria"), rs.getString("percha"), rs.getString("seccion")});
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }

        productFrame.setVisible(true);
    }

    private static void filterProductList(String query, DefaultTableModel model) {
        try (Connection conn = DriverManager.getConnection("jdbc:mysql://localhost:3306/juego_autenticacion", "root", "1234")) {
            String sql = "SELECT * FROM productos WHERE nombre LIKE ?";
            try (PreparedStatement pst = conn.prepareStatement(sql)) {
                pst.setString(1, "%" + query + "%");
                try (ResultSet rs = pst.executeQuery()) {
                    model.setRowCount(0); // Limpiar tabla antes de agregar los resultados
                    while (rs.next()) {
                        model.addRow(new Object[]{rs.getInt("id"), rs.getString("nombre"), rs.getString("categoria"), rs.getString("percha"), rs.getString("seccion")});
                    }
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }
}
