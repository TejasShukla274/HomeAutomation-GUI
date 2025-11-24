import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

// ==============================================================================
// 1. EXCEPTION HANDLING (Part of OOP 10 Marks)
// ==============================================================================

/**
 * Custom exception for device operation failures.
 */
class DeviceOperationException extends Exception {
    public DeviceOperationException(String message) {
        super(message);
    }
}

// ==============================================================================
// 2. INTERFACE (Part of OOP 10 Marks)
// ==============================================================================

interface IControllable {
    String turnOn() throws DeviceOperationException;
    String turnOff() throws DeviceOperationException;
    String getStatus();
    void setStatus(String status);
}

// ==============================================================================
// 3. BASE CLASSES AND INHERITANCE (Part of OOP 10 Marks)
// ==============================================================================

abstract class User {
    protected String email;
    protected String name;
    protected String role;
    protected String passwordHash; // Placeholder for security
    
    // Dependencies to be passed down for dashboard creation
    protected UserDAO userDAO;
    protected DeviceDAO deviceDAO;
    protected ConcurrentMap<String, String> systemStatus;

    // Modified Constructor to accept core dependencies
    public User(String email, String name, String passwordHash, String role, 
                UserDAO userDAO, DeviceDAO deviceDAO, ConcurrentMap<String, String> systemStatus) {
        this.email = email;
        this.name = name;
        this.passwordHash = passwordHash;
        this.role = role;
        this.userDAO = userDAO;
        this.deviceDAO = deviceDAO;
        this.systemStatus = systemStatus;
    }

    // Abstract method to be implemented by subclasses (Polymorphism)
    public abstract void createDashboard(JFrame frame);

    public String getEmail() { return email; }
    public String getName() { return name; }
    public String getRole() { return role; }
    public String getPasswordHash() { return passwordHash; }

    @Override
    public String toString() {
        return "Name: " + name + ", Role: " + role + ", Email: " + email;
    }
}

class Admin extends User {
    public Admin(String email, String name, String passwordHash, 
                 UserDAO userDAO, DeviceDAO deviceDAO, ConcurrentMap<String, String> systemStatus) {
        super(email, name, passwordHash, "Admin", userDAO, deviceDAO, systemStatus);
    }

    // Polymorphism: Admin dashboard implementation
    @Override
    public void createDashboard(JFrame frame) {
        frame.setTitle("Admin Dashboard - " + name);
        // Corrected: Pass necessary dependencies to the static AdminPanel
        new HomeAutomationGUI.AdminPanel(frame, this.email, userDAO, systemStatus); 
    }
}

class Homeowner extends User {
    public Homeowner(String email, String name, String passwordHash, 
                     UserDAO userDAO, DeviceDAO deviceDAO, ConcurrentMap<String, String> systemStatus) {
        super(email, name, passwordHash, "Homeowner", userDAO, deviceDAO, systemStatus);
    }

    // Polymorphism: Homeowner dashboard implementation
    @Override
    public void createDashboard(JFrame frame) {
        frame.setTitle("Homeowner Dashboard - " + name);
        // Corrected: Pass necessary dependencies to the static HomeownerPanel
        new HomeAutomationGUI.HomeownerPanel(frame, this.email, deviceDAO, userDAO); 
    }
}

abstract class Device implements IControllable {
    protected int deviceId;
    protected String homeownerId;
    protected String name;
    protected String type;
    protected String status = "OFF";

    public Device(int deviceId, String homeownerId, String name, String type) {
        this.deviceId = deviceId;
        this.homeownerId = homeownerId;
        this.name = name;
        this.type = type;
    }

    public int getDeviceId() { return deviceId; }
    public String getHomeownerId() { return homeownerId; }
    public String getName() { return name; }
    public String getType() { return type; }
    @Override
    public String getStatus() { return status; }
    @Override
    public void setStatus(String status) { this.status = status; }

    // Abstract method for device-specific actions (Polymorphism)
    public abstract String adjustSetting(int value) throws DeviceOperationException;
}

class Light extends Device {
    private int brightness = 0; // State specific to Light

    public Light(int deviceId, String homeownerId, String name, int brightness, String status) {
        super(deviceId, homeownerId, name, "Light");
        this.brightness = brightness;
        this.status = status;
    }

    // Polymorphism: Specific turnOn implementation
    @Override
    public String turnOn() throws DeviceOperationException {
        if (this.status.equals("ON")) {
            return name + " is already ON.";
        }
        this.status = "ON";
        this.brightness = (brightness == 0) ? 50 : brightness; // Set default brightness if 0
        return name + " switched ON. Brightness: " + brightness + "%.";
    }

    @Override
    public String turnOff() throws DeviceOperationException {
        this.status = "OFF";
        return name + " switched OFF.";
    }

    // Polymorphism: Specific setting adjustment
    @Override
    public String adjustSetting(int value) throws DeviceOperationException {
        if (value < 0 || value > 100) {
            throw new DeviceOperationException("Brightness must be between 0 and 100.");
        }
        this.brightness = value;
        if (value > 0) this.status = "ON";
        else this.status = "OFF";
        return name + " brightness set to " + brightness + "%.";
    }

    public int getBrightness() { return brightness; }
}

class Gate extends Device {
    public Gate(int deviceId, String homeownerId, String name, String status) {
        super(deviceId, homeownerId, name, "Gate");
        this.status = status;
    }

    // Polymorphism: Specific turnOn (Open) implementation
    @Override
    public String turnOn() throws DeviceOperationException {
        if (this.status.equals("OPEN")) {
            return name + " is already OPEN.";
        }
        this.status = "OPEN";
        return name + " is OPENING...";
    }

    // Polymorphism: Specific turnOff (Close) implementation
    @Override
    public String turnOff() throws DeviceOperationException {
        this.status = "CLOSED";
        return name + " is CLOSING...";
    }

    @Override
    public String adjustSetting(int value) throws DeviceOperationException {
        return name + ": Gates do not have adjustable settings.";
    }
}


// ==============================================================================
// 4. DATABASE CONNECTIVITY (JDBC 3 Marks)
// 5. CLASSES FOR DB OPERATIONS (DAO 7 Marks) & IMPLEMENT JDBC (3 Marks)
// ==============================================================================

class DBConnectionManager {
    // ⚠️ UPDATED VALUES
    private static final String DB_URL = "jdbc:mysql://localhost:3306/home_automation_db?useSSL=false&serverTimezone=UTC";
    private static final String USER = "root"; // Using the standard default username
    private static final String PASS = "Aiwa@1002"; // Placeholder/Example Password

    /**
     * Establishes a connection to the database. (JDBC Connectivity)
     */
    public static Connection getConnection() throws SQLException {
        // Explicitly load the driver (good practice, though often automatic)
        try {
            Class.forName("com.mysql.cj.jdbc.Driver");
        } catch (ClassNotFoundException e) {
            throw new SQLException("MySQL JDBC Driver not found.", e);
        }
        return DriverManager.getConnection(DB_URL, USER, PASS);
    }

    /**
     * Helper method to close resources safely.
     */
    public static void close(Connection conn, Statement stmt, ResultSet rs) {
        try {
            if (rs != null) rs.close();
            if (stmt != null) stmt.close();
            if (conn != null) conn.close();
        } catch (SQLException e) {
            // Log the error but don't stop execution
            System.err.println("Error closing DB resources: " + e.getMessage());
        }
    }
}

class UserDAO {
    // DAO uses JDBC to perform CRUD operations on the 'users' table (7 Marks: Classes for DB Ops)

    public void createUser(User user) throws SQLException {
        // Implement JDBC for database connectivity (3 Marks: Implement JDBC)
        String sql = "INSERT INTO users (email, name, password_hash, role) VALUES (?, ?, ?, ?)";
        Connection conn = null;
        PreparedStatement stmt = null;

        try {
            conn = DBConnectionManager.getConnection();
            stmt = conn.prepareStatement(sql);
            stmt.setString(1, user.getEmail());
            stmt.setString(2, user.getName());
            stmt.setString(3, user.getPasswordHash());
            stmt.setString(4, user.getRole());
            stmt.executeUpdate();
            System.out.println("User created: " + user.getEmail());
        } finally {
            DBConnectionManager.close(conn, stmt, null);
        }
    }

    public List<User> getAllUsers(UserDAO userDAO, DeviceDAO deviceDAO, ConcurrentMap<String, String> systemStatus) throws SQLException {
        // Uses Generics and Collections (6 Marks: Collections & Generics)
        List<User> userList = new ArrayList<>();
        String sql = "SELECT email, name, role, password_hash FROM users";
        Connection conn = null;
        PreparedStatement stmt = null;
        ResultSet rs = null;

        try {
            conn = DBConnectionManager.getConnection();
            stmt = conn.prepareStatement(sql);
            rs = stmt.executeQuery();

            while (rs.next()) {
                String email = rs.getString("email");
                String name = rs.getString("name");
                String role = rs.getString("role");
                String passwordHash = rs.getString("password_hash");

                // Polymorphism in action: create concrete class based on role, passing DAOs
                if ("Admin".equalsIgnoreCase(role)) {
                    userList.add(new Admin(email, name, passwordHash, userDAO, deviceDAO, systemStatus));
                } else if ("Homeowner".equalsIgnoreCase(role)) {
                    userList.add(new Homeowner(email, name, passwordHash, userDAO, deviceDAO, systemStatus));
                }
            }
        } finally {
            DBConnectionManager.close(conn, stmt, rs);
        }
        return userList;
    }

    public User getUserByEmail(String email, UserDAO userDAO, DeviceDAO deviceDAO, ConcurrentMap<String, String> systemStatus) throws SQLException {
        String sql = "SELECT email, name, role, password_hash FROM users WHERE email = ?";
        Connection conn = null;
        PreparedStatement stmt = null;
        ResultSet rs = null;

        try {
            conn = DBConnectionManager.getConnection();
            stmt = conn.prepareStatement(sql);
            stmt.setString(1, email);
            rs = stmt.executeQuery();

            if (rs.next()) {
                String name = rs.getString("name");
                String role = rs.getString("role");
                String passwordHash = rs.getString("password_hash");

                if ("Admin".equalsIgnoreCase(role)) {
                    return new Admin(email, name, passwordHash, userDAO, deviceDAO, systemStatus);
                } else if ("Homeowner".equalsIgnoreCase(role)) {
                    return new Homeowner(email, name, passwordHash, userDAO, deviceDAO, systemStatus);
                }
            }
        } finally {
            DBConnectionManager.close(conn, stmt, rs);
        }
        return null;
    }

    public void updateUser(User user) throws SQLException {
        String sql = "UPDATE users SET name = ?, password_hash = ?, role = ? WHERE email = ?";
        Connection conn = null;
        PreparedStatement stmt = null;

        try {
            conn = DBConnectionManager.getConnection();
            stmt = conn.prepareStatement(sql);
            stmt.setString(1, user.getName());
            stmt.setString(2, user.getPasswordHash());
            stmt.setString(3, user.getRole());
            stmt.setString(4, user.getEmail());
            stmt.executeUpdate();
            System.out.println("User updated: " + user.getEmail());
        } finally {
            DBConnectionManager.close(conn, stmt, null);
        }
    }

    public void deleteUser(String email) throws SQLException {
        String sql = "DELETE FROM users WHERE email = ?";
        Connection conn = null;
        PreparedStatement stmt = null;

        try {
            conn = DBConnectionManager.getConnection();
            stmt = conn.prepareStatement(sql);
            stmt.setString(1, email);
            stmt.executeUpdate();
            System.out.println("User deleted: " + email);
        } finally {
            DBConnectionManager.close(conn, stmt, null);
        }
    }
}

class DeviceDAO {
    // DAO uses JDBC to perform CRUD operations on the 'devices' table
    
    public void createDevice(Device device) throws SQLException {
        String sql = "INSERT INTO devices (homeowner_id, device_name, device_type, status, setting_value) VALUES (?, ?, ?, ?, ?)";
        Connection conn = null;
        PreparedStatement stmt = null;
        
        int settingValue = 0;
        if (device instanceof Light) {
            settingValue = ((Light) device).getBrightness();
        }

        try {
            conn = DBConnectionManager.getConnection();
            stmt = conn.prepareStatement(sql);
            stmt.setString(1, device.getHomeownerId());
            stmt.setString(2, device.getName());
            stmt.setString(3, device.getType());
            stmt.setString(4, device.getStatus());
            stmt.setInt(5, settingValue);
            stmt.executeUpdate();
            System.out.println("Device created: " + device.getName());
        } finally {
            DBConnectionManager.close(conn, stmt, null);
        }
    }

    public List<Device> getDevicesByHomeownerId(String homeownerId) throws SQLException {
        List<Device> deviceList = new ArrayList<>();
        String sql = "SELECT * FROM devices WHERE homeowner_id = ?";
        Connection conn = null;
        PreparedStatement stmt = null;
        ResultSet rs = null;

        try {
            conn = DBConnectionManager.getConnection();
            stmt = conn.prepareStatement(sql);
            stmt.setString(1, homeownerId);
            rs = stmt.executeQuery();

            while (rs.next()) {
                int id = rs.getInt("device_id");
                String name = rs.getString("device_name");
                String type = rs.getString("device_type");
                String status = rs.getString("status");
                int settingValue = rs.getInt("setting_value");

                // Polymorphism: create concrete class based on type
                if ("Light".equalsIgnoreCase(type)) {
                    deviceList.add(new Light(id, homeownerId, name, settingValue, status));
                } else if ("Gate".equalsIgnoreCase(type)) {
                    deviceList.add(new Gate(id, homeownerId, name, status));
                }
            }
        } finally {
            DBConnectionManager.close(conn, stmt, rs);
        }
        return deviceList;
    }

    public void updateDevice(Device device) throws SQLException {
        String sql = "UPDATE devices SET device_name = ?, device_type = ?, status = ?, setting_value = ? WHERE device_id = ?";
        Connection conn = null;
        PreparedStatement stmt = null;
        
        int settingValue = 0;
        if (device instanceof Light) {
            settingValue = ((Light) device).getBrightness();
        }

        try {
            conn = DBConnectionManager.getConnection();
            stmt = conn.prepareStatement(sql);
            stmt.setString(1, device.getName());
            stmt.setString(2, device.getType());
            stmt.setString(3, device.getStatus());
            stmt.setInt(4, settingValue);
            stmt.setInt(5, device.getDeviceId());
            stmt.executeUpdate();
            System.out.println("Device updated: " + device.getName());
        } finally {
            DBConnectionManager.close(conn, stmt, null);
        }
    }

    public void deleteDevice(int deviceId) throws SQLException {
        String sql = "DELETE FROM devices WHERE device_id = ?";
        Connection conn = null;
        PreparedStatement stmt = null;

        try {
            conn = DBConnectionManager.getConnection();
            stmt = conn.prepareStatement(sql);
            stmt.setInt(1, deviceId);
            stmt.executeUpdate();
            System.out.println("Device deleted: " + deviceId);
        } finally {
            DBConnectionManager.close(conn, stmt, null);
        }
    }
}


// ==============================================================================
// 6. MULTITHREADING (4 Marks)
// ==============================================================================

/**
 * Simulates background monitoring of devices.
 */
class MonitoringThread extends Thread {
    private final ConcurrentMap<String, String> statusMap; // Using ConcurrentMap for thread safety

    public MonitoringThread(ConcurrentMap<String, String> statusMap) {
        this.statusMap = statusMap;
        this.setDaemon(true); // Run in the background
    }

    @Override
    public void run() {
        System.out.println("Monitoring thread started...");
        while (true) {
            try {
                // Simulate checking security/temperature status
                Thread.sleep(5000); // Check every 5 seconds
                
                // Simulate a security alert
                if (System.currentTimeMillis() % 10000 < 5000) {
                    statusMap.put("security_status", "ALERT - Unlocked Door!");
                } else {
                    statusMap.put("security_status", "Security Normal");
                }
                
                // Simulate temperature fluctuation
                int temp = 20 + (int) (Math.random() * 5);
                statusMap.put("temperature", temp + "°C");

            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            }
        }
    }
}


// ==============================================================================
// 7. GUI & Main Application Logic
// ==============================================================================

public class HomeAutomationGUI {
    private JFrame mainFrame;
    private UserDAO userDAO = new UserDAO();
    private DeviceDAO deviceDAO = new DeviceDAO();
    private ConcurrentMap<String, String> systemStatus = new ConcurrentHashMap<>();

    public HomeAutomationGUI() {
        mainFrame = new JFrame("Home Automation System - Login");
        mainFrame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        mainFrame.setSize(500, 300);
        mainFrame.setLocationRelativeTo(null);
        
        // Initialize an Admin for first run if no users exist
        initializeDefaultAdmin(); 
        
        showLoginPanel();
        mainFrame.setVisible(true);

        // Start the monitoring thread
        new MonitoringThread(systemStatus).start();
    }
    
    // Helper to ensure at least one Admin exists for testing
    private void initializeDefaultAdmin() {
        try {
            // getUserByEmail now requires DAO/status map, which are available as fields
            if (userDAO.getUserByEmail("admin@corp.com", userDAO, deviceDAO, systemStatus) == null) {
                // createUser takes the complete User object
                userDAO.createUser(new Admin("admin@corp.com", "System Admin", "securepass", userDAO, deviceDAO, systemStatus));
                System.out.println("Default Admin created for first use.");
            }
        } catch (SQLException e) {
             System.err.println("Could not initialize default admin: " + e.getMessage());
        }
    }

    private void showLoginPanel() {
        JPanel panel = new JPanel(new GridLayout(4, 2, 10, 10));
        panel.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));

        JTextField emailField = new JTextField("admin@corp.com");
        JPasswordField passwordField = new JPasswordField("securepass");
        
        panel.add(new JLabel("Email:"));
        panel.add(emailField);
        panel.add(new JLabel("Password:"));
        panel.add(passwordField);

        JButton loginButton = new JButton("Login");
        panel.add(new JLabel()); // Placeholder
        panel.add(loginButton);

        loginButton.addActionListener(e -> {
            String email = emailField.getText().trim();
            String password = new String(passwordField.getPassword());
            
            try {
                // Pass dependencies when retrieving the user
                User user = userDAO.getUserByEmail(email, userDAO, deviceDAO, systemStatus);
                
                // Simple password check (replace with proper hashing in production)
                if (user != null && user.getPasswordHash().equals(password)) {
                    // Clear the frame and delegate to the user-specific dashboard (Polymorphism)
                    mainFrame.getContentPane().removeAll();
                    user.createDashboard(mainFrame);
                    mainFrame.revalidate();
                    mainFrame.repaint();
                } else {
                    JOptionPane.showMessageDialog(mainFrame, "Invalid Credentials or User Role.", "Login Error", JOptionPane.ERROR_MESSAGE);
                }
            } catch (SQLException ex) {
                JOptionPane.showMessageDialog(mainFrame, "Database Error: Check your DB connection and tables.\n" + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
                System.err.println("Login DB Error: " + ex.getMessage());
            }
        });

        mainFrame.setContentPane(panel);
        mainFrame.revalidate();
    }

    public static void main(String[] args) {
        // Use the Swing event dispatch thread for GUI operations
        SwingUtilities.invokeLater(HomeAutomationGUI::new);
    }

    // --- Inner Class for Admin GUI (Made static to resolve compilation error) ---
    static class AdminPanel extends JPanel { 
        private String adminEmail;
        private UserDAO userDAO;
        private ConcurrentMap<String, String> systemStatus; // Now passed explicitly

        public AdminPanel(JFrame frame, String email, UserDAO userDAO, ConcurrentMap<String, String> systemStatus) {
            this.adminEmail = email;
            this.userDAO = userDAO;
            this.systemStatus = systemStatus;
            
            setLayout(new BorderLayout());
            frame.setSize(800, 600); // Resize for dashboard
            
            JTabbedPane tabbedPane = new JTabbedPane();
            tabbedPane.addTab("User Management", createUserManagementPanel());
            tabbedPane.addTab("System Monitoring", createMonitoringPanel());
            
            add(tabbedPane, BorderLayout.CENTER);

            JButton logoutButton = new JButton("Logout");
            logoutButton.addActionListener(e -> {
                // Note: We access the HomeAutomationGUI instance method indirectly via JFrame's reference
                // This requires re-instantiating or finding a better way if this panel were truly standalone.
                // For simplicity here, we rely on HomeAutomationGUI logic in HomeAutomationGUI.showLoginPanel()
                frame.dispose();
                SwingUtilities.invokeLater(HomeAutomationGUI::new);
            });
            
            JPanel bottomPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
            bottomPanel.add(logoutButton);
            add(bottomPanel, BorderLayout.SOUTH);
            
            frame.setContentPane(this);
        }

        private JPanel createUserManagementPanel() {
            // Admin Functionality: User Management (CRUD)
            JPanel panel = new JPanel(new BorderLayout());
            JTextArea userDisplay = new JTextArea();
            userDisplay.setEditable(false);
            JScrollPane scrollPane = new JScrollPane(userDisplay);

            // Input Fields
            JTextField nameField = new JTextField(15);
            JTextField emailField = new JTextField(15);
            JPasswordField passField = new JPasswordField(15);
            JComboBox<String> roleCombo = new JComboBox<>(new String[]{"Homeowner", "Admin"});

            JPanel inputPanel = new JPanel(new FlowLayout());
            inputPanel.add(new JLabel("Name:"));
            inputPanel.add(nameField);
            inputPanel.add(new JLabel("Email:"));
            inputPanel.add(emailField);
            inputPanel.add(new JLabel("Password:"));
            inputPanel.add(passField);
            inputPanel.add(new JLabel("Role:"));
            inputPanel.add(roleCombo);

            // CRUD Buttons
            JButton addButton = new JButton("Add User");
            JButton editButton = new JButton("Edit User");
            JButton deleteButton = new JButton("Delete User");
            
            JPanel buttonPanel = new JPanel(new FlowLayout());
            buttonPanel.add(addButton);
            buttonPanel.add(editButton);
            buttonPanel.add(deleteButton);

            panel.add(inputPanel, BorderLayout.NORTH);
            panel.add(scrollPane, BorderLayout.CENTER);
            panel.add(buttonPanel, BorderLayout.SOUTH);

            // Action Listeners for CRUD
            addButton.addActionListener(e -> {
                try {
                    String role = (String) roleCombo.getSelectedItem();
                    User newUser;
                    // Note: Since UserDAO and DeviceDAO are needed for the User object, 
                    // we must pass placeholder/null values in this context if we don't 
                    // want the DAO to know about them immediately, but here we pass the DAOs
                    // from AdminPanel's fields to satisfy the User constructor.
                    if ("Admin".equals(role)) {
                        newUser = new Admin(emailField.getText(), nameField.getText(), new String(passField.getPassword()), userDAO, null, systemStatus);
                    } else {
                        newUser = new Homeowner(emailField.getText(), nameField.getText(), new String(passField.getPassword()), userDAO, null, systemStatus);
                    }
                    userDAO.createUser(newUser);
                    refreshUserDisplay(userDisplay);
                    JOptionPane.showMessageDialog(this, "User added successfully!", "Success", JOptionPane.INFORMATION_MESSAGE);
                } catch (SQLException ex) {
                    JOptionPane.showMessageDialog(this, "Failed to add user: " + ex.getMessage(), "DB Error", JOptionPane.ERROR_MESSAGE);
                }
            });

            // Delete action
            deleteButton.addActionListener(e -> {
                String email = emailField.getText();
                if (email.isEmpty()) {
                    JOptionPane.showMessageDialog(this, "Enter email to delete.", "Error", JOptionPane.WARNING_MESSAGE);
                    return;
                }
                try {
                    userDAO.deleteUser(email);
                    refreshUserDisplay(userDisplay);
                    JOptionPane.showMessageDialog(this, "User deleted successfully!", "Success", JOptionPane.INFORMATION_MESSAGE);
                } catch (SQLException ex) {
                    JOptionPane.showMessageDialog(this, "Failed to delete user: " + ex.getMessage(), "DB Error", JOptionPane.ERROR_MESSAGE);
                }
            });
            
            // Initial load of user data
            refreshUserDisplay(userDisplay);
            return panel;
        }
        
        private void refreshUserDisplay(JTextArea display) {
            try {
                // Pass DAO dependencies when retrieving all users
                List<User> users = userDAO.getAllUsers(userDAO, null, systemStatus); 
                StringBuilder sb = new StringBuilder("--- Registered Users ---\n");
                for (User user : users) {
                    sb.append(user.toString()).append("\n");
                }
                display.setText(sb.toString());
            } catch (SQLException e) {
                display.setText("ERROR: Could not load users from database: " + e.getMessage());
            }
        }
        
        private JPanel createMonitoringPanel() {
            JPanel panel = new JPanel(new GridLayout(3, 1));
            JLabel securityLabel = new JLabel("Security Status: Awaiting Update...");
            JLabel tempLabel = new JLabel("Temperature: Awaiting Update...");
            
            panel.add(securityLabel);
            panel.add(tempLabel);
            
            // Timer to update status from the background thread
            new Timer(1000, new AbstractAction() {
                @Override
                public void actionPerformed(ActionEvent e) {
                    securityLabel.setText("Security Status: " + systemStatus.getOrDefault("security_status", "N/A"));
                    tempLabel.setText("Temperature: " + systemStatus.getOrDefault("temperature", "N/A"));
                }
            }).start();
            
            return panel;
        }
    }

    // --- Inner Class for Homeowner GUI (Made static to resolve compilation error) ---
    static class HomeownerPanel extends JPanel { 
        private String homeownerEmail;
        private DeviceDAO deviceDAO; // Now passed explicitly
        private UserDAO userDAO; // Added for context/simplicity of logout

        public HomeownerPanel(JFrame frame, String email, DeviceDAO deviceDAO, UserDAO userDAO) {
            this.homeownerEmail = email;
            this.deviceDAO = deviceDAO;
            this.userDAO = userDAO;
            
            setLayout(new BorderLayout());
            frame.setSize(800, 600); // Resize for dashboard
            
            JTabbedPane tabbedPane = new JTabbedPane();
            tabbedPane.addTab("Device Control", createDeviceControlPanel());
            tabbedPane.addTab("Automation Rules", createRulesPanel());
            
            add(tabbedPane, BorderLayout.CENTER);

            JButton logoutButton = new JButton("Logout");
            logoutButton.addActionListener(e -> {
                frame.dispose();
                SwingUtilities.invokeLater(HomeAutomationGUI::new);
            });
            
            JPanel bottomPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
            bottomPanel.add(logoutButton);
            add(bottomPanel, BorderLayout.SOUTH);
            
            frame.setContentPane(this);
            frame.revalidate();
        }

        private JPanel createDeviceControlPanel() {
            JPanel panel = new JPanel(new BorderLayout());
            List<Device> connectedDevices = new ArrayList<>();
            
            try {
                connectedDevices = deviceDAO.getDevicesByHomeownerId(homeownerEmail);
            } catch (SQLException e) {
                JOptionPane.showMessageDialog(this, "Could not load devices: " + e.getMessage(), "DB Error", JOptionPane.ERROR_MESSAGE);
            }
            
            JPanel deviceListPanel = new JPanel();
            deviceListPanel.setLayout(new BoxLayout(deviceListPanel, BoxLayout.Y_AXIS));
            
            for (Device device : connectedDevices) {
                deviceListPanel.add(createDeviceControlRow(device));
            }

            JScrollPane scrollPane = new JScrollPane(deviceListPanel);
            scrollPane.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
            
            panel.add(scrollPane, BorderLayout.CENTER);
            
            // Add initial test device if none exist
            if (connectedDevices.isEmpty()) {
                JButton addTestDevice = new JButton("Add Test Devices (Reload tab or re-login)");
                addTestDevice.addActionListener(e -> {
                    try {
                        // Create test devices (Gate and Light) for Homeowner
                        deviceDAO.createDevice(new Light(0, homeownerEmail, "Kitchen Light", 75, "ON"));
                        deviceDAO.createDevice(new Gate(0, homeownerEmail, "Garage Gate", "CLOSED"));
                        
                        JOptionPane.showMessageDialog(this, "Test devices added to DB. Please click the tab or re-login to see them.", "Success", JOptionPane.INFORMATION_MESSAGE);

                    } catch (SQLException ex) {
                        JOptionPane.showMessageDialog(this, "Failed to add test device: " + ex.getMessage(), "DB Error", JOptionPane.ERROR_MESSAGE);
                    }
                });
                panel.add(addTestDevice, BorderLayout.NORTH);
            }
            
            return panel;
        }

        private JPanel createDeviceControlRow(Device device) {
            // Homeowner Functionality: Device Control (switch on/off, open gate, etc.)
            JPanel row = new JPanel(new FlowLayout(FlowLayout.LEFT));
            
            JLabel nameLabel = new JLabel(device.getName() + " (" + device.getType() + "): ");
            JLabel statusLabel = new JLabel(device.getStatus());
            
            JButton onButton = new JButton("ON / OPEN");
            JButton offButton = new JButton("OFF / CLOSE");
            
            row.add(nameLabel);
            row.add(statusLabel);
            row.add(onButton);
            row.add(offButton);
            
            // Add setting control for Light only
            if (device instanceof Light) {
                Light light = (Light) device;
                JSlider slider = new JSlider(0, 100, light.getBrightness());
                slider.setPreferredSize(new Dimension(100, 20));
                
                slider.addChangeListener(e -> {
                    try {
                        light.adjustSetting(slider.getValue());
                        statusLabel.setText(light.getStatus() + (light.getStatus().equals("ON") ? ", Brightness: " + light.getBrightness() + "%" : ""));
                        deviceDAO.updateDevice(light);
                    } catch (DeviceOperationException | SQLException ex) {
                        JOptionPane.showMessageDialog(row, ex.getMessage(), "Control Error", JOptionPane.ERROR_MESSAGE);
                    }
                });
                row.add(new JLabel("Brightness:"));
                row.add(slider);
            }

            // Action Listeners for ON/OFF
            onButton.addActionListener(e -> controlDevice(device, true, statusLabel));
            offButton.addActionListener(e -> controlDevice(device, false, statusLabel));
            
            return row;
        }
        
        private void controlDevice(Device device, boolean turnOn, JLabel statusLabel) {
            try {
                String message;
                if (turnOn) {
                    message = device.turnOn();
                } else {
                    message = device.turnOff();
                }
                
                // Update the status in the UI
                if (device instanceof Light) {
                    Light light = (Light) device;
                    statusLabel.setText(light.getStatus() + (light.getStatus().equals("ON") ? ", Brightness: " + light.getBrightness() + "%" : ""));
                } else {
                    statusLabel.setText(device.getStatus());
                }
                
                // Update the status in the database
                deviceDAO.updateDevice(device);
                
                // Show confirmation message
                JOptionPane.showMessageDialog(this, message, "Control Success", JOptionPane.INFORMATION_MESSAGE);
                
            } catch (DeviceOperationException | SQLException ex) {
                JOptionPane.showMessageDialog(this, ex.getMessage(), "Control Error", JOptionPane.ERROR_MESSAGE);
            }
        }

        private JPanel createRulesPanel() {
            JPanel panel = new JPanel(new BorderLayout());
            
            JTextArea ruleDisplay = new JTextArea("1. IF Temperature > 25°C THEN Kitchen Light = 10%\n2. IF Time = 10 PM THEN Garage Gate = CLOSED\n\n(This section requires further database schema and logic integration)");
            ruleDisplay.setEditable(false);
            
            JButton addRuleButton = new JButton("Add New Rule (Logic Placeholder)");
            
            panel.add(new JScrollPane(ruleDisplay), BorderLayout.CENTER);
            panel.add(addRuleButton, BorderLayout.SOUTH);
            
            return panel;
        }
    }
}