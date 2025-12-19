import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.logging.Level;
import java.util.logging.Logger;

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

    public DeviceOperationException(String message, Throwable cause) {
        super(message, cause);
    }
}

/**
 * Custom exception for authentication failures.
 */
class AuthenticationException extends Exception {
    public AuthenticationException(String message) {
        super(message);
    }
}

/**
 * Custom exception for validation failures.
 */
class ValidationException extends Exception {
    public ValidationException(String message) {
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

/**
 * Interface for input validation
 */
interface IValidator {
    boolean isValid() throws ValidationException;
}

// ==============================================================================
// 3. UTILITY CLASSES FOR VALIDATION AND ERROR HANDLING
// ==============================================================================

class ValidationUtil {
    private static final Logger logger = Logger.getLogger(ValidationUtil.class.getName());

    /**
     * Validates email format
     */
    public static boolean isValidEmail(String email) throws ValidationException {
        if (email == null || email.trim().isEmpty()) {
            throw new ValidationException("Email cannot be empty.");
        }
        String emailRegex = "^[A-Za-z0-9+_.-]+@(.+)$";
        if (!email.matches(emailRegex)) {
            throw new ValidationException("Invalid email format: " + email);
        }
        return true;
    }

    /**
     * Validates password strength
     */
    public static boolean isValidPassword(String password) throws ValidationException {
        if (password == null || password.isEmpty()) {
            throw new ValidationException("Password cannot be empty.");
        }
        if (password.length() < 6) {
            throw new ValidationException("Password must be at least 6 characters long.");
        }
        return true;
    }

    /**
     * Validates name format
     */
    public static boolean isValidName(String name) throws ValidationException {
        if (name == null || name.trim().isEmpty()) {
            throw new ValidationException("Name cannot be empty.");
        }
        if (name.length() < 2) {
            throw new ValidationException("Name must be at least 2 characters long.");
        }
        if (!name.matches("^[a-zA-Z\\s]+$")) {
            throw new ValidationException("Name can only contain letters and spaces.");
        }
        return true;
    }

    /**
     * Validates brightness level for lights
     */
    public static boolean isValidBrightness(int brightness) throws ValidationException {
        if (brightness < 0 || brightness > 100) {
            throw new ValidationException("Brightness must be between 0 and 100.");
        }
        return true;
    }
}

/**
 * Error handling utility
 */
class ErrorHandler {
    private static final Logger logger = Logger.getLogger(ErrorHandler.class.getName());

    public static void logError(String message, Exception ex) {
        logger.log(Level.SEVERE, message, ex);
    }

    public static void showErrorDialog(Component parent, String message, String title) {
        JOptionPane.showMessageDialog(parent, message, title, JOptionPane.ERROR_MESSAGE);
    }

    public static void showSuccessDialog(Component parent, String message, String title) {
        JOptionPane.showMessageDialog(parent, message, title, JOptionPane.INFORMATION_MESSAGE);
    }

    public static void showWarningDialog(Component parent, String message, String title) {
        JOptionPane.showMessageDialog(parent, message, title, JOptionPane.WARNING_MESSAGE);
    }

    public static int showConfirmDialog(Component parent, String message, String title) {
        return JOptionPane.showConfirmDialog(parent, message, title, JOptionPane.YES_NO_OPTION);
    }
}

// ==============================================================================
// 4. BASE CLASSES AND INHERITANCE (Part of OOP 10 Marks)
// ==============================================================================

abstract class User {
    protected String email;
    protected String name;
    protected String role;
    protected String passwordHash;
    
    protected UserDAO userDAO;
    protected DeviceDAO deviceDAO;
    protected ConcurrentMap<String, String> systemStatus;
    
    private static final Logger logger = Logger.getLogger(User.class.getName());

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

    @Override
    public void createDashboard(JFrame frame) {
        frame.setTitle("Admin Dashboard - " + name);
        new HomeAutomationGUI.AdminPanel(frame, this.email, userDAO, deviceDAO, systemStatus); 
    }
}

class Homeowner extends User {
    public Homeowner(String email, String name, String passwordHash, 
                     UserDAO userDAO, DeviceDAO deviceDAO, ConcurrentMap<String, String> systemStatus) {
        super(email, name, passwordHash, "Homeowner", userDAO, deviceDAO, systemStatus);
    }

    @Override
    public void createDashboard(JFrame frame) {
        frame.setTitle("Homeowner Dashboard - " + name);
        new HomeAutomationGUI.HomeownerPanel(frame, this.email, deviceDAO, userDAO); 
    }
}

abstract class Device implements IControllable {
    protected int deviceId;
    protected String homeownerId;
    protected String name;
    protected String type;
    protected String status = "OFF";
    protected long lastUpdated;

    public Device(int deviceId, String homeownerId, String name, String type) {
        this.deviceId = deviceId;
        this.homeownerId = homeownerId;
        this.name = name;
        this.type = type;
        this.lastUpdated = System.currentTimeMillis();
    }

    public int getDeviceId() { return deviceId; }
    public String getHomeownerId() { return homeownerId; }
    public String getName() { return name; }
    public String getType() { return type; }
    @Override
    public String getStatus() { return status; }
    @Override
    public void setStatus(String status) { 
        this.status = status;
        this.lastUpdated = System.currentTimeMillis();
    }

    public long getLastUpdated() { return lastUpdated; }

    public abstract String adjustSetting(int value) throws DeviceOperationException;
}

class Light extends Device {
    private int brightness = 0;

    public Light(int deviceId, String homeownerId, String name, int brightness, String status) {
        super(deviceId, homeownerId, name, "Light");
        this.brightness = brightness;
        this.status = status;
    }

    @Override
    public String turnOn() throws DeviceOperationException {
        if (this.status.equals("ON")) {
            return name + " is already ON.";
        }
        this.status = "ON";
        this.brightness = (brightness == 0) ? 50 : brightness;
        this.lastUpdated = System.currentTimeMillis();
        return name + " switched ON. Brightness: " + brightness + "%.";
    }

    @Override
    public String turnOff() throws DeviceOperationException {
        this.status = "OFF";
        this.lastUpdated = System.currentTimeMillis();
        return name + " switched OFF.";
    }

    @Override
    public String adjustSetting(int value) throws DeviceOperationException {
        try {
            ValidationUtil.isValidBrightness(value);
        } catch (ValidationException e) {
            throw new DeviceOperationException(e.getMessage(), e);
        }
        this.brightness = value;
        if (value > 0) this.status = "ON";
        else this.status = "OFF";
        this.lastUpdated = System.currentTimeMillis();
        return name + " brightness set to " + brightness + "%.";
    }

    public int getBrightness() { return brightness; }
}

class Gate extends Device {
    public Gate(int deviceId, String homeownerId, String name, String status) {
        super(deviceId, homeownerId, name, "Gate");
        this.status = status;
    }

    @Override
    public String turnOn() throws DeviceOperationException {
        if (this.status.equals("OPEN")) {
            return name + " is already OPEN.";
        }
        this.status = "OPEN";
        this.lastUpdated = System.currentTimeMillis();
        return name + " is OPENING...";
    }

    @Override
    public String turnOff() throws DeviceOperationException {
        this.status = "CLOSED";
        this.lastUpdated = System.currentTimeMillis();
        return name + " is CLOSING...";
    }

    @Override
    public String adjustSetting(int value) throws DeviceOperationException {
        return name + ": Gates do not have adjustable settings.";
    }
}


// ==============================================================================
// 5. DATABASE CONNECTIVITY (JDBC 3 Marks) & DAO PATTERN (7 Marks)
// ==============================================================================

class DBConnectionManager {
    private static final String DB_URL = "jdbc:mysql://localhost:3306/home_automation_db?useSSL=false&serverTimezone=UTC";
    private static final String USER = "root";
    private static final String PASS = "Aiwa@1002";
    private static final Logger logger = Logger.getLogger(DBConnectionManager.class.getName());

    public static Connection getConnection() throws SQLException {
        try {
            Class.forName("com.mysql.cj.jdbc.Driver");
        } catch (ClassNotFoundException e) {
            String msg = "MySQL JDBC Driver not found. Please ensure MySQL connector JAR is in classpath.";
            logger.log(Level.SEVERE, msg, e);
            throw new SQLException(msg, e);
        }
        try {
            return DriverManager.getConnection(DB_URL, USER, PASS);
        } catch (SQLException e) {
            String msg = "Failed to connect to database. Check database URL, credentials, and MySQL server status.";
            logger.log(Level.SEVERE, msg, e);
            throw new SQLException(msg, e);
        }
    }

    public static void close(Connection conn, Statement stmt, ResultSet rs) {
        try {
            if (rs != null) rs.close();
            if (stmt != null) stmt.close();
            if (conn != null) conn.close();
        } catch (SQLException e) {
            logger.log(Level.WARNING, "Error closing DB resources", e);
        }
    }
}

class UserDAO {
    private static final Logger logger = Logger.getLogger(UserDAO.class.getName());

    public void createUser(User user) throws SQLException {
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
            logger.log(Level.INFO, "User created: " + user.getEmail());
        } catch (SQLException e) {
            logger.log(Level.SEVERE, "Error creating user: " + user.getEmail(), e);
            throw e;
        } finally {
            DBConnectionManager.close(conn, stmt, null);
        }
    }

    public List<User> getAllUsers(UserDAO userDAO, DeviceDAO deviceDAO, ConcurrentMap<String, String> systemStatus) throws SQLException {
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

                if ("Admin".equalsIgnoreCase(role)) {
                    userList.add(new Admin(email, name, passwordHash, userDAO, deviceDAO, systemStatus));
                } else if ("Homeowner".equalsIgnoreCase(role)) {
                    userList.add(new Homeowner(email, name, passwordHash, userDAO, deviceDAO, systemStatus));
                }
            }
        } catch (SQLException e) {
            logger.log(Level.SEVERE, "Error retrieving all users", e);
            throw e;
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
        } catch (SQLException e) {
            logger.log(Level.SEVERE, "Error retrieving user: " + email, e);
            throw e;
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
            logger.log(Level.INFO, "User updated: " + user.getEmail());
        } catch (SQLException e) {
            logger.log(Level.SEVERE, "Error updating user: " + user.getEmail(), e);
            throw e;
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
            logger.log(Level.INFO, "User deleted: " + email);
        } catch (SQLException e) {
            logger.log(Level.SEVERE, "Error deleting user: " + email, e);
            throw e;
        } finally {
            DBConnectionManager.close(conn, stmt, null);
        }
    }

    public boolean userExists(String email) throws SQLException {
        return getUserByEmail(email, null, null, null) != null;
    }
}

class DeviceDAO {
    private static final Logger logger = Logger.getLogger(DeviceDAO.class.getName());
    
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
            logger.log(Level.INFO, "Device created: " + device.getName());
        } catch (SQLException e) {
            logger.log(Level.SEVERE, "Error creating device: " + device.getName(), e);
            throw e;
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

                if ("Light".equalsIgnoreCase(type)) {
                    deviceList.add(new Light(id, homeownerId, name, settingValue, status));
                } else if ("Gate".equalsIgnoreCase(type)) {
                    deviceList.add(new Gate(id, homeownerId, name, status));
                }
            }
        } catch (SQLException e) {
            logger.log(Level.SEVERE, "Error retrieving devices for homeowner: " + homeownerId, e);
            throw e;
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
            logger.log(Level.INFO, "Device updated: " + device.getName());
        } catch (SQLException e) {
            logger.log(Level.SEVERE, "Error updating device: " + device.getName(), e);
            throw e;
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
            logger.log(Level.INFO, "Device deleted: " + deviceId);
        } catch (SQLException e) {
            logger.log(Level.SEVERE, "Error deleting device: " + deviceId, e);
            throw e;
        } finally {
            DBConnectionManager.close(conn, stmt, null);
        }
    }
}


// ==============================================================================
// 6. MULTITHREADING (4 Marks) & BACKGROUND OPERATIONS
// ==============================================================================

class MonitoringThread extends Thread {
    private final ConcurrentMap<String, String> statusMap;
    private volatile boolean running = true;
    private static final Logger logger = Logger.getLogger(MonitoringThread.class.getName());

    public MonitoringThread(ConcurrentMap<String, String> statusMap) {
        this.statusMap = statusMap;
        this.setDaemon(true);
        this.setName("Device-Monitoring-Thread");
    }

    @Override
    public void run() {
        logger.log(Level.INFO, "Monitoring thread started...");
        while (running) {
            try {
                Thread.sleep(5000);
                
                if (System.currentTimeMillis() % 10000 < 5000) {
                    statusMap.put("security_status", "ALERT - Unlocked Door!");
                } else {
                    statusMap.put("security_status", "Security Normal");
                }
                
                int temp = 20 + (int) (Math.random() * 5);
                statusMap.put("temperature", temp + "°C");
                statusMap.put("last_check", System.currentTimeMillis() + "");

            } catch (InterruptedException e) {
                logger.log(Level.INFO, "Monitoring thread interrupted");
                Thread.currentThread().interrupt();
                break;
            }
        }
        logger.log(Level.INFO, "Monitoring thread stopped");
    }

    public void stopMonitoring() {
        running = false;
    }
}


// ==============================================================================
// 7. GUI & MAIN APPLICATION LOGIC
// ==============================================================================

public class HomeAutomationGUI {
    private JFrame mainFrame;
    private UserDAO userDAO = new UserDAO();
    private DeviceDAO deviceDAO = new DeviceDAO();
    private ConcurrentMap<String, String> systemStatus = new ConcurrentHashMap<>();
    private MonitoringThread monitoringThread;
    private static final Logger logger = Logger.getLogger(HomeAutomationGUI.class.getName());

    public HomeAutomationGUI() {
        mainFrame = new JFrame("Home Automation System");
        mainFrame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        mainFrame.setSize(500, 400);
        mainFrame.setLocationRelativeTo(null);
        
        try {
            initializeDefaultUsers();
        } catch (SQLException e) {
            ErrorHandler.logError("Failed to initialize default users", e);
            ErrorHandler.showErrorDialog(mainFrame, 
                "Database initialization failed: " + e.getMessage(), 
                "Initialization Error");
        }
        
        showRoleSelectionPanel();
        mainFrame.setVisible(true);

        monitoringThread = new MonitoringThread(systemStatus);
        monitoringThread.start();
    }
    
    private void initializeDefaultUsers() throws SQLException {
        try {
            if (userDAO.getUserByEmail("admin@corp.com", userDAO, deviceDAO, systemStatus) == null) {
                userDAO.createUser(new Admin("admin@corp.com", "System Admin", "securepass", userDAO, deviceDAO, systemStatus));
                logger.log(Level.INFO, "Default Admin created");
            }
            
            if (userDAO.getUserByEmail("homeowner@test.com", userDAO, deviceDAO, systemStatus) == null) {
                userDAO.createUser(new Homeowner("homeowner@test.com", "Test Homeowner", "password123", userDAO, deviceDAO, systemStatus));
                logger.log(Level.INFO, "Default Homeowner created");
            }
        } catch (SQLException e) {
            logger.log(Level.SEVERE, "Could not initialize default users", e);
            throw e;
        }
    }

    private void showRoleSelectionPanel() {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBackground(new Color(240, 240, 240));
        
        JPanel headerPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 20, 20));
        headerPanel.setBackground(new Color(52, 152, 219));
        JLabel titleLabel = new JLabel("<html><h2 style='color:white;'>Select Your Role</h2></html>");
        headerPanel.add(titleLabel);
        
        JPanel buttonPanel = new JPanel(new GridLayout(1, 2, 30, 30));
        buttonPanel.setBorder(new EmptyBorder(50, 80, 50, 80));
        buttonPanel.setBackground(new Color(240, 240, 240));
        
        JButton adminButton = new JButton("👤 Admin Login");
        adminButton.setFont(new Font("Arial", Font.BOLD, 16));
        adminButton.setBackground(new Color(230, 126, 34));
        adminButton.setForeground(Color.WHITE);
        adminButton.setFocusPainted(false);
        adminButton.setCursor(new Cursor(Cursor.HAND_CURSOR));
        
        JButton ownerButton = new JButton("🏠 Homeowner Login");
        ownerButton.setFont(new Font("Arial", Font.BOLD, 16));
        ownerButton.setBackground(new Color(52, 152, 219));
        ownerButton.setForeground(Color.WHITE);
        ownerButton.setFocusPainted(false);
        ownerButton.setCursor(new Cursor(Cursor.HAND_CURSOR));
        
        adminButton.addActionListener(e -> showLoginPanel("Admin"));
        ownerButton.addActionListener(e -> showLoginPanel("Homeowner"));
        
        buttonPanel.add(adminButton);
        buttonPanel.add(ownerButton);
        
        panel.add(headerPanel, BorderLayout.NORTH);
        panel.add(buttonPanel, BorderLayout.CENTER);
        
        mainFrame.setContentPane(panel);
        mainFrame.setTitle("Home Automation System - Role Selection");
        mainFrame.revalidate();
    }

    private void showLoginPanel(String role) {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBackground(new Color(240, 240, 240));
        
        JPanel headerPanel = new JPanel(new FlowLayout(FlowLayout.CENTER));
        headerPanel.setBackground(new Color(52, 152, 219));
        JLabel roleLabel = new JLabel("<html><h3 style='color:white;'>" + role + " Login</h3></html>");
        headerPanel.add(roleLabel);
        
        JPanel formPanel = new JPanel(new GridLayout(5, 2, 10, 10));
        formPanel.setBorder(new EmptyBorder(20, 40, 20, 40));
        formPanel.setBackground(new Color(240, 240, 240));

        String defaultEmail = role.equals("Admin") ? "admin@corp.com" : "homeowner@test.com";
        String defaultPass = role.equals("Admin") ? "securepass" : "password123";
        
        JTextField emailField = new JTextField(defaultEmail);
        JPasswordField passwordField = new JPasswordField(defaultPass);
        JButton loginButton = new JButton("✓ Login");
        JButton backButton = new JButton("← Back");
        JLabel statusLabel = new JLabel("Ready to login");
        
        loginButton.setBackground(new Color(46, 204, 113));
        loginButton.setForeground(Color.WHITE);
        loginButton.setFocusPainted(false);
        loginButton.setCursor(new Cursor(Cursor.HAND_CURSOR));
        
        backButton.setBackground(new Color(127, 140, 141));
        backButton.setForeground(Color.WHITE);
        backButton.setFocusPainted(false);
        backButton.setCursor(new Cursor(Cursor.HAND_CURSOR));
        
        formPanel.add(new JLabel("📧 Email:"));
        formPanel.add(emailField);
        formPanel.add(new JLabel("🔐 Password:"));
        formPanel.add(passwordField);
        formPanel.add(new JLabel("Status:"));
        formPanel.add(statusLabel);
        
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 15, 10));
        buttonPanel.add(loginButton);
        buttonPanel.add(backButton);
        formPanel.add(buttonPanel);

        loginButton.addActionListener(e -> handleLogin(emailField, passwordField, role, statusLabel, loginButton));
        backButton.addActionListener(e -> showRoleSelectionPanel());

        panel.add(headerPanel, BorderLayout.NORTH);
        panel.add(formPanel, BorderLayout.CENTER);
        
        mainFrame.setContentPane(panel);
        mainFrame.setTitle("Home Automation System - " + role + " Login");
        mainFrame.revalidate();
    }

    private void handleLogin(JTextField emailField, JPasswordField passwordField, String role, 
                            JLabel statusLabel, JButton loginButton) {
        try {
            String email = emailField.getText().trim();
            String password = new String(passwordField.getPassword());
            
            ValidationUtil.isValidEmail(email);
            ValidationUtil.isValidPassword(password);
            
            loginButton.setEnabled(false);
            statusLabel.setText("Authenticating...");
            
            new SwingWorker<User, Void>() {
                @Override
                protected User doInBackground() throws SQLException {
                    return userDAO.getUserByEmail(email, userDAO, deviceDAO, systemStatus);
                }
                
                @Override
                protected void done() {
                    try {
                        User user = get();
                        
                        if (user != null && user.getPasswordHash().equals(password)) {
                            if (!user.getRole().equals(role)) {
                                ErrorHandler.showWarningDialog(mainFrame, 
                                    "This user account is not a " + role + ".", 
                                    "Role Mismatch");
                                statusLabel.setText("Login failed - role mismatch");
                                loginButton.setEnabled(true);
                                return;
                            }
                            
                            mainFrame.getContentPane().removeAll();
                            user.createDashboard(mainFrame);
                            mainFrame.revalidate();
                            mainFrame.repaint();
                        } else {
                            ErrorHandler.showErrorDialog(mainFrame, 
                                "Invalid email or password.", 
                                "Authentication Failed");
                            statusLabel.setText("Login failed - invalid credentials");
                            loginButton.setEnabled(true);
                        }
                    } catch (Exception ex) {
                        ErrorHandler.logError("Login error", ex);
                        ErrorHandler.showErrorDialog(mainFrame, 
                            "Database Error: " + ex.getMessage(), 
                            "Error");
                        statusLabel.setText("Login failed - database error");
                        loginButton.setEnabled(true);
                    }
                }
            }.execute();
        } catch (ValidationException ex) {
            ErrorHandler.showWarningDialog(mainFrame, ex.getMessage(), "Validation Error");
            loginButton.setEnabled(true);
        }
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(HomeAutomationGUI::new);
    }

    static class AdminPanel extends JPanel { 
        private String adminEmail;
        private UserDAO userDAO;
        private DeviceDAO deviceDAO;
        private ConcurrentMap<String, String> systemStatus;
        private static final Logger logger = Logger.getLogger(AdminPanel.class.getName());

        public AdminPanel(JFrame frame, String email, UserDAO userDAO, DeviceDAO deviceDAO, ConcurrentMap<String, String> systemStatus) {
            this.adminEmail = email;
            this.userDAO = userDAO;
            this.deviceDAO = deviceDAO;
            this.systemStatus = systemStatus;
            
            setLayout(new BorderLayout());
            frame.setSize(950, 700);
            
            JTabbedPane tabbedPane = new JTabbedPane();
            tabbedPane.addTab("👥 User Management", createUserManagementPanel());
            tabbedPane.addTab("📊 System Monitoring", createMonitoringPanel());
            
            add(tabbedPane, BorderLayout.CENTER);

            JButton logoutButton = new JButton("🚪 Logout");
            logoutButton.setBackground(new Color(231, 76, 60));
            logoutButton.setForeground(Color.WHITE);
            logoutButton.setFocusPainted(false);
            logoutButton.setCursor(new Cursor(Cursor.HAND_CURSOR));
            
            logoutButton.addActionListener(e -> {
                int confirm = ErrorHandler.showConfirmDialog(this, 
                    "Are you sure you want to logout?", 
                    "Confirm Logout");
                if (confirm == JOptionPane.YES_OPTION) {
                    frame.dispose();
                    SwingUtilities.invokeLater(HomeAutomationGUI::new);
                }
            });
            
            JPanel bottomPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
            bottomPanel.add(new JLabel("👤 Logged in as: " + adminEmail));
            bottomPanel.add(logoutButton);
            add(bottomPanel, BorderLayout.SOUTH);
            
            frame.setContentPane(this);
        }

        private JPanel createUserManagementPanel() {
            JPanel panel = new JPanel(new BorderLayout());
            JTextArea userDisplay = new JTextArea();
            userDisplay.setEditable(false);
            userDisplay.setFont(new Font("Courier New", Font.PLAIN, 12));
            userDisplay.setBackground(new Color(245, 245, 245));
            JScrollPane scrollPane = new JScrollPane(userDisplay);

            JTextField nameField = new JTextField(12);
            JTextField emailField = new JTextField(12);
            JPasswordField passField = new JPasswordField(12);
            JComboBox<String> roleCombo = new JComboBox<>(new String[]{"Homeowner", "Admin"});

            JPanel inputPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 10));
            inputPanel.setBackground(new Color(240, 240, 240));
            inputPanel.add(new JLabel("Name:"));
            inputPanel.add(nameField);
            inputPanel.add(new JLabel("Email:"));
            inputPanel.add(emailField);
            inputPanel.add(new JLabel("Password:"));
            inputPanel.add(passField);
            inputPanel.add(new JLabel("Role:"));
            inputPanel.add(roleCombo);

            JButton addButton = new JButton("➕ Add User");
            JButton deleteButton = new JButton("🗑️ Delete");
            JButton refreshButton = new JButton("🔄 Refresh");
            JButton editButton = new JButton("✏️ Edit");
            
            styleButton(addButton, new Color(46, 204, 113));
            styleButton(deleteButton, new Color(231, 76, 60));
            styleButton(editButton, new Color(52, 152, 219));
            styleButton(refreshButton, new Color(155, 89, 182));
            
            JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
            buttonPanel.add(addButton);
            buttonPanel.add(editButton);
            buttonPanel.add(deleteButton);
            buttonPanel.add(refreshButton);

            panel.add(inputPanel, BorderLayout.NORTH);
            panel.add(scrollPane, BorderLayout.CENTER);
            panel.add(buttonPanel, BorderLayout.SOUTH);

            addButton.addActionListener(e -> handleAddUser(nameField, emailField, passField, roleCombo, userDisplay, addButton));
            editButton.addActionListener(e -> handleEditUser(nameField, emailField, passField, roleCombo, userDisplay, editButton));
            deleteButton.addActionListener(e -> handleDeleteUser(emailField, userDisplay, deleteButton));
            refreshButton.addActionListener(e -> refreshUserDisplay(userDisplay));
            
            refreshUserDisplay(userDisplay);
            return panel;
        }

        private void handleAddUser(JTextField nameField, JTextField emailField, JPasswordField passField, 
                                  JComboBox<String> roleCombo, JTextArea userDisplay, JButton addButton) {
            try {
                String name = nameField.getText().trim();
                String email = emailField.getText().trim();
                String password = new String(passField.getPassword());
                String role = (String) roleCombo.getSelectedItem();
                
                ValidationUtil.isValidName(name);
                ValidationUtil.isValidEmail(email);
                ValidationUtil.isValidPassword(password);
                
                addButton.setEnabled(false);
                
                new SwingWorker<Void, Void>() {
                    @Override
                    protected Void doInBackground() throws SQLException {
                        User newUser = "Admin".equals(role) 
                            ? new Admin(email, name, password, userDAO, deviceDAO, systemStatus)
                            : new Homeowner(email, name, password, userDAO, deviceDAO, systemStatus);
                        userDAO.createUser(newUser);
                        return null;
                    }
                    
                    @Override
                    protected void done() {
                        try {
                            get();
                            refreshUserDisplay(userDisplay);
                            ErrorHandler.showSuccessDialog(AdminPanel.this, 
                                "User '" + email + "' added successfully!", 
                                "Success");
                            nameField.setText("");
                            emailField.setText("");
                            passField.setText("");
                        } catch (Exception ex) {
                            ErrorHandler.logError("Error adding user", ex);
                            ErrorHandler.showErrorDialog(AdminPanel.this, 
                                "Failed to add user: " + ex.getMessage(), 
                                "Error");
                        } finally {
                            addButton.setEnabled(true);
                        }
                    }
                }.execute();
            } catch (ValidationException ex) {
                ErrorHandler.showWarningDialog(this, ex.getMessage(), "Validation Error");
            }
        }

        private void handleEditUser(JTextField nameField, JTextField emailField, JPasswordField passField, 
                                   JComboBox<String> roleCombo, JTextArea userDisplay, JButton editButton) {
            try {
                String email = emailField.getText().trim();
                String name = nameField.getText().trim();
                String password = new String(passField.getPassword());
                String role = (String) roleCombo.getSelectedItem();
                
                ValidationUtil.isValidEmail(email);
                if (!name.isEmpty()) ValidationUtil.isValidName(name);
                if (!password.isEmpty()) ValidationUtil.isValidPassword(password);
                
                editButton.setEnabled(false);
                
                new SwingWorker<Void, Void>() {
                    @Override
                    protected Void doInBackground() throws SQLException {
                        User editUser = "Admin".equals(role)
                            ? new Admin(email, name, password, userDAO, deviceDAO, systemStatus)
                            : new Homeowner(email, name, password, userDAO, deviceDAO, systemStatus);
                        userDAO.updateUser(editUser);
                        return null;
                    }
                    
                    @Override
                    protected void done() {
                        try {
                            get();
                            refreshUserDisplay(userDisplay);
                            ErrorHandler.showSuccessDialog(AdminPanel.this, 
                                "User '" + email + "' updated successfully!", 
                                "Success");
                            nameField.setText("");
                            emailField.setText("");
                            passField.setText("");
                        } catch (Exception ex) {
                            ErrorHandler.logError("Error editing user", ex);
                            ErrorHandler.showErrorDialog(AdminPanel.this, 
                                "Failed to edit user: " + ex.getMessage(), 
                                "Error");
                        } finally {
                            editButton.setEnabled(true);
                        }
                    }
                }.execute();
            } catch (ValidationException ex) {
                ErrorHandler.showWarningDialog(this, ex.getMessage(), "Validation Error");
            }
        }

        private void handleDeleteUser(JTextField emailField, JTextArea userDisplay, JButton deleteButton) {
            try {
                String email = emailField.getText().trim();
                ValidationUtil.isValidEmail(email);
                
                int confirm = ErrorHandler.showConfirmDialog(this, 
                    "Delete user '" + email + "'? This action cannot be undone.", 
                    "Confirm Delete");
                
                if (confirm != JOptionPane.YES_OPTION) return;
                
                deleteButton.setEnabled(false);
                
                new SwingWorker<Void, Void>() {
                    @Override
                    protected Void doInBackground() throws SQLException {
                        userDAO.deleteUser(email);
                        return null;
                    }
                    
                    @Override
                    protected void done() {
                        try {
                            get();
                            refreshUserDisplay(userDisplay);
                            ErrorHandler.showSuccessDialog(AdminPanel.this, 
                                "User '" + email + "' deleted successfully!", 
                                "Success");
                            emailField.setText("");
                        } catch (Exception ex) {
                            ErrorHandler.logError("Error deleting user", ex);
                            ErrorHandler.showErrorDialog(AdminPanel.this, 
                                "Failed to delete user: " + ex.getMessage(), 
                                "Error");
                        } finally {
                            deleteButton.setEnabled(true);
                        }
                    }
                }.execute();
            } catch (ValidationException ex) {
                ErrorHandler.showWarningDialog(this, ex.getMessage(), "Validation Error");
            }
        }
        
        private void refreshUserDisplay(JTextArea display) {
            new SwingWorker<List<User>, Void>() {
                @Override
                protected List<User> doInBackground() throws SQLException {
                    return userDAO.getAllUsers(userDAO, deviceDAO, systemStatus);
                }
                
                @Override
                protected void done() {
                    try {
                        List<User> users = get();
                        StringBuilder sb = new StringBuilder("════════════════════ REGISTERED USERS ════════════════════\n\n");
                        sb.append(String.format("%-20s | %-15s | %-30s\n", "Name", "Role", "Email"));
                        sb.append("═".repeat(70)).append("\n");
                        for (User user : users) {
                            sb.append(String.format("%-20s | %-15s | %-30s\n", 
                                user.getName(), user.getRole(), user.getEmail()));
                        }
                        sb.append("\n═".repeat(70)).append("\n");
                        sb.append("Total Users: ").append(users.size());
                        display.setText(sb.toString());
                    } catch (Exception e) {
                        ErrorHandler.logError("Error loading users", e);
                        display.setText("ERROR: Could not load users from database:\n" + e.getMessage());
                    }
                }
            }.execute();
        }
        
        private JPanel createMonitoringPanel() {
            JPanel panel = new JPanel(new GridLayout(3, 1, 10, 20));
            panel.setBorder(new EmptyBorder(30, 50, 30, 50));
            panel.setBackground(new Color(240, 240, 240));
            
            JLabel securityLabel = new JLabel("🔒 Security Status: Awaiting Update...");
            JLabel tempLabel = new JLabel("🌡️ Temperature: Awaiting Update...");
            JLabel timeLabel = new JLabel("⏰ Last Update: Initializing...");
            
            securityLabel.setFont(new Font("Arial", Font.BOLD, 16));
            tempLabel.setFont(new Font("Arial", Font.BOLD, 16));
            timeLabel.setFont(new Font("Arial", Font.PLAIN, 12));
            
            panel.add(securityLabel);
            panel.add(tempLabel);
            panel.add(timeLabel);
            
            new Timer(1000, new AbstractAction() {
                @Override
                public void actionPerformed(ActionEvent e) {
                    securityLabel.setText("🔒 Security Status: " + systemStatus.getOrDefault("security_status", "N/A"));
                    tempLabel.setText("🌡️ Temperature: " + systemStatus.getOrDefault("temperature", "N/A"));
                    timeLabel.setText("⏰ Last Update: " + new java.text.SimpleDateFormat("HH:mm:ss").format(new java.util.Date()));
                }
            }).start();
            
            return panel;
        }

        private void styleButton(JButton button, Color color) {
            button.setBackground(color);
            button.setForeground(Color.WHITE);
            button.setFocusPainted(false);
            button.setCursor(new Cursor(Cursor.HAND_CURSOR));
        }
    }

    static class HomeownerPanel extends JPanel { 
        private String homeownerEmail;
        private DeviceDAO deviceDAO;
        private UserDAO userDAO;
        private static final Logger logger = Logger.getLogger(HomeownerPanel.class.getName());

        public HomeownerPanel(JFrame frame, String email, DeviceDAO deviceDAO, UserDAO userDAO) {
            this.homeownerEmail = email;
            this.deviceDAO = deviceDAO;
            this.userDAO = userDAO;
            
            setLayout(new BorderLayout());
            frame.setSize(950, 700);
            
            JTabbedPane tabbedPane = new JTabbedPane();
            tabbedPane.addTab("🎛️ Device Control", createDeviceControlPanel());
            tabbedPane.addTab("📡 Device Monitoring", createMonitoringPanel());
            
            add(tabbedPane, BorderLayout.CENTER);

            JButton logoutButton = new JButton("🚪 Logout");
            logoutButton.setBackground(new Color(231, 76, 60));
            logoutButton.setForeground(Color.WHITE);
            logoutButton.setFocusPainted(false);
            logoutButton.setCursor(new Cursor(Cursor.HAND_CURSOR));
            
            logoutButton.addActionListener(e -> {
                int confirm = JOptionPane.showConfirmDialog(this, 
                    "Are you sure you want to logout?", 
                    "Confirm Logout", 
                    JOptionPane.YES_NO_OPTION);
                if (confirm == JOptionPane.YES_OPTION) {
                    frame.dispose();
                    SwingUtilities.invokeLater(HomeAutomationGUI::new);
                }
            });
            
            JPanel bottomPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
            bottomPanel.add(new JLabel("👤 Logged in as: " + homeownerEmail));
            bottomPanel.add(logoutButton);
            add(bottomPanel, BorderLayout.SOUTH);
            
            frame.setContentPane(this);
            frame.revalidate();
        }

        private JPanel createDeviceControlPanel() {
            JPanel panel = new JPanel(new BorderLayout());
            JPanel deviceListPanel = new JPanel();
            deviceListPanel.setLayout(new BoxLayout(deviceListPanel, BoxLayout.Y_AXIS));
            deviceListPanel.setBackground(new Color(245, 245, 245));
            JScrollPane scrollPane = new JScrollPane(deviceListPanel);
            scrollPane.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
            
            JLabel loadingLabel = new JLabel("⏳ Loading devices...");
            loadingLabel.setFont(new Font("Arial", Font.PLAIN, 14));
            JPanel centerPanel = new JPanel(new BorderLayout());
            centerPanel.add(loadingLabel, BorderLayout.NORTH);
            centerPanel.add(scrollPane, BorderLayout.CENTER);
            
            panel.add(centerPanel, BorderLayout.CENTER);
            
            new SwingWorker<List<Device>, Void>() {
                @Override
                protected List<Device> doInBackground() throws SQLException {
                    return deviceDAO.getDevicesByHomeownerId(homeownerEmail);
                }
                
                @Override
                protected void done() {
                    try {
                        List<Device> connectedDevices = get();
                        deviceListPanel.removeAll();
                        
                        if (connectedDevices.isEmpty()) {
                            JPanel addPanel = new JPanel(new FlowLayout());
                            JButton addTestDevice = new JButton("➕ Add Test Devices");
                            styleDeviceButton(addTestDevice, new Color(46, 204, 113));
                            
                            addTestDevice.addActionListener(e -> addTestDevices(deviceListPanel));
                            addPanel.add(addTestDevice);
                            deviceListPanel.add(addPanel);
                        } else {
                            for (Device device : connectedDevices) {
                                deviceListPanel.add(createDeviceControlRow(device, deviceListPanel));
                            }
                        }
                        
                        deviceListPanel.revalidate();
                        deviceListPanel.repaint();
                        loadingLabel.setText("");
                    } catch (Exception ex) {
                        logger.log(Level.SEVERE, "Error loading devices", ex);
                        loadingLabel.setText("❌ Error: " + ex.getMessage());
                    }
                }
            }.execute();
            
            return panel;
        }
        
        private void addTestDevices(JPanel deviceListPanel) {
            new SwingWorker<Void, Void>() {
                @Override
                protected Void doInBackground() throws SQLException {
                    deviceDAO.createDevice(new Light(0, homeownerEmail, "Kitchen Light", 75, "ON"));
                    deviceDAO.createDevice(new Gate(0, homeownerEmail, "Garage Gate", "CLOSED"));
                    deviceDAO.createDevice(new Light(0, homeownerEmail, "Bedroom Light", 50, "OFF"));
                    return null;
                }
                
                @Override
                protected void done() {
                    try {
                        get();
                        ErrorHandler.showSuccessDialog(HomeownerPanel.this, 
                            "Test devices added successfully!", 
                            "Success");
                        deviceListPanel.removeAll();
                        deviceListPanel.add(new JLabel("🔄 Reload tab to see devices..."));
                        deviceListPanel.revalidate();
                        deviceListPanel.repaint();
                    } catch (Exception ex) {
                        logger.log(Level.SEVERE, "Error adding test devices", ex);
                        ErrorHandler.showErrorDialog(HomeownerPanel.this, 
                            "Failed to add test devices: " + ex.getMessage(), 
                            "Error");
                    }
                }
            }.execute();
        }

        private JPanel createDeviceControlRow(Device device, JPanel parentPanel) {
            JPanel row = new JPanel(new FlowLayout(FlowLayout.LEFT, 15, 10));
            row.setBorder(BorderFactory.createLineBorder(new Color(200, 200, 200), 1));
            row.setBackground(new Color(255, 255, 255));
            
            JLabel nameLabel = new JLabel(device.getName() + " (" + device.getType() + "):  ");
            nameLabel.setFont(new Font("Arial", Font.BOLD, 12));
            
            JLabel statusLabel = new JLabel(device.getStatus());
            statusLabel.setFont(new Font("Arial", Font.PLAIN, 12));
            statusLabel.setForeground(new Color(41, 128, 185));
            
            JButton onButton = new JButton(device instanceof Gate ? "🔓 OPEN" : "💡 ON");
            JButton offButton = new JButton(device instanceof Gate ? "🔒 CLOSE" : "⚫ OFF");
            
            styleDeviceButton(onButton, new Color(46, 204, 113));
            styleDeviceButton(offButton, new Color(231, 76, 60));
            
            row.add(nameLabel);
            row.add(statusLabel);
            row.add(onButton);
            row.add(offButton);
            
            if (device instanceof Light) {
                Light light = (Light) device;
                JSlider slider = new JSlider(0, 100, light.getBrightness());
                slider.setPreferredSize(new Dimension(150, 30));
                slider.setMajorTickSpacing(10);
                slider.setMinorTickSpacing(1);
                slider.setPaintTicks(true);
                slider.setPaintLabels(true);
                
                slider.addChangeListener(e -> {
                    new SwingWorker<Void, Void>() {
                        @Override
                        protected Void doInBackground() throws DeviceOperationException, SQLException {
                            light.adjustSetting(slider.getValue());
                            deviceDAO.updateDevice(light);
                            return null;
                        }
                        
                        @Override
                        protected void done() {
                            try {
                                get();
                                statusLabel.setText(light.getStatus() + (light.getStatus().equals("ON") ? " - " + light.getBrightness() + "%" : ""));
                            } catch (Exception ex) {
                                logger.log(Level.SEVERE, "Error adjusting brightness", ex);
                                ErrorHandler.showErrorDialog(row, ex.getMessage(), "Control Error");
                            }
                        }
                    }.execute();
                });
                row.add(new JLabel("Brightness:"));
                row.add(slider);
            }

            onButton.addActionListener(e -> controlDeviceAsync(device, true, statusLabel, onButton, offButton));
            offButton.addActionListener(e -> controlDeviceAsync(device, false, statusLabel, onButton, offButton));
            
            return row;
        }
        
        private void controlDeviceAsync(Device device, boolean turnOn, JLabel statusLabel, JButton onBtn, JButton offBtn) {
            onBtn.setEnabled(false);
            offBtn.setEnabled(false);
            
            new SwingWorker<String, Void>() {
                @Override
                protected String doInBackground() throws DeviceOperationException, SQLException {
                    String message = turnOn ? device.turnOn() : device.turnOff();
                    deviceDAO.updateDevice(device);
                    return message;
                }
                
                @Override
                protected void done() {
                    try {
                        get();
                        if (device instanceof Light) {
                            Light light = (Light) device;
                            statusLabel.setText(light.getStatus() + (light.getStatus().equals("ON") ? " - " + light.getBrightness() + "%" : ""));
                        } else {
                            statusLabel.setText(device.getStatus());
                        }
                    } catch (Exception ex) {
                        logger.log(Level.SEVERE, "Error controlling device", ex);
                        ErrorHandler.showErrorDialog(HomeownerPanel.this, 
                            "Error: " + ex.getMessage(), 
                            "Control Error");
                    } finally {
                        onBtn.setEnabled(true);
                        offBtn.setEnabled(true);
                    }
                }
            }.execute();
        }

        private JPanel createMonitoringPanel() {
            JPanel panel = new JPanel(new BorderLayout());
            panel.setBackground(new Color(245, 245, 245));
            JPanel deviceStatusPanel = new JPanel();
            deviceStatusPanel.setLayout(new BoxLayout(deviceStatusPanel, BoxLayout.Y_AXIS));
            deviceStatusPanel.setBackground(new Color(245, 245, 245));
            JScrollPane scrollPane = new JScrollPane(deviceStatusPanel);
            
            JLabel loadingLabel = new JLabel("⏳ Loading device status...");
            loadingLabel.setFont(new Font("Arial", Font.PLAIN, 14));
            panel.add(loadingLabel, BorderLayout.NORTH);
            panel.add(scrollPane, BorderLayout.CENTER);
            
            new SwingWorker<List<Device>, Void>() {
                @Override
                protected List<Device> doInBackground() throws SQLException {
                    return deviceDAO.getDevicesByHomeownerId(homeownerEmail);
                }
                
                @Override
                protected void done() {
                    try {
                        List<Device> devices = get();
                        deviceStatusPanel.removeAll();
                        
                        StringBuilder sb = new StringBuilder("<html><body style='font-family:Arial;'>");
                        sb.append("<h2>📱 Device Status Monitor</h2>");
                        sb.append("<table border='1' cellpadding='10' style='border-collapse:collapse;'>");
                        sb.append("<tr style='background-color:#3498db;color:white;'>");
                        sb.append("<th>Device Name</th><th>Type</th><th>Status</th><th>Last Updated</th></tr>");
                        
                        int rowColor = 0;
                        for (Device device : devices) {
                            String status = device.getStatus();
                            if (device instanceof Light) {
                                status += " - " + ((Light) device).getBrightness() + "%";
                            }
                            String bgColor = rowColor % 2 == 0 ? "#f9f9f9" : "#ffffff";
                            sb.append("<tr style='background-color:").append(bgColor).append(";'>");
                            sb.append("<td>").append(device.getName()).append("</td>");
                            sb.append("<td>").append(device.getType()).append("</td>");
                            sb.append("<td><b>").append(status).append("</b></td>");
                            sb.append("<td>").append(new java.text.SimpleDateFormat("HH:mm:ss").format(new java.util.Date(device.getLastUpdated()))).append("</td>");
                            sb.append("</tr>");
                            rowColor++;
                        }
                        
                        sb.append("</table>");
                        sb.append("<p style='margin-top:15px;color:#555;'><i>✓ Total Devices: ").append(devices.size()).append("</i></p>");
                        sb.append("</body></html>");
                        
                        JLabel statusHtml = new JLabel(sb.toString());
                        deviceStatusPanel.add(statusHtml);
                        deviceStatusPanel.revalidate();
                        deviceStatusPanel.repaint();
                        loadingLabel.setText("");
                    } catch (Exception ex) {
                        logger.log(Level.SEVERE, "Error loading device status", ex);
                        loadingLabel.setText("❌ Error: " + ex.getMessage());
                    }
                }
            }.execute();
            
            return panel;
        }

        private void styleDeviceButton(JButton button, Color color) {
            button.setBackground(color);
            button.setForeground(Color.WHITE);
            button.setFocusPainted(false);
            button.setCursor(new Cursor(Cursor.HAND_CURSOR));
            button.setFont(new Font("Arial", Font.BOLD, 11));
        }
    }
}