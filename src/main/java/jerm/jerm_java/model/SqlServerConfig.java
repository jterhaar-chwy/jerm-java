package jerm.jerm_java.model;

import com.fasterxml.jackson.annotation.JsonIgnore;

public class SqlServerConfig {
    private String name;
    private String host;
    private int port = 1433; // Default SQL Server port
    private String database;
    private String username;
    
    @JsonIgnore
    private String password;
    
    private boolean integratedSecurity = false;
    private boolean trustServerCertificate = true;
    private boolean encrypt = true;
    
    // Default constructor
    public SqlServerConfig() {}
    
    // Constructor
    public SqlServerConfig(String name, String host, int port, String database, String username, String password) {
        this.name = name;
        this.host = host;
        this.port = port;
        this.database = database;
        this.username = username;
        this.password = password;
    }
    
    // Getters and setters
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    
    public String getHost() { return host; }
    public void setHost(String host) { this.host = host; }
    
    public int getPort() { return port; }
    public void setPort(int port) { this.port = port; }
    
    public String getDatabase() { return database; }
    public void setDatabase(String database) { this.database = database; }
    
    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }
    
    public String getPassword() { return password; }
    public void setPassword(String password) { this.password = password; }
    
    public boolean isIntegratedSecurity() { return integratedSecurity; }
    public void setIntegratedSecurity(boolean integratedSecurity) { this.integratedSecurity = integratedSecurity; }
    
    public boolean isTrustServerCertificate() { return trustServerCertificate; }
    public void setTrustServerCertificate(boolean trustServerCertificate) { this.trustServerCertificate = trustServerCertificate; }
    
    public boolean isEncrypt() { return encrypt; }
    public void setEncrypt(boolean encrypt) { this.encrypt = encrypt; }
    
    // Generate SQL Server JDBC URL
    public String getJdbcUrl() {
        StringBuilder url = new StringBuilder();
        url.append("jdbc:sqlserver://").append(host).append(":").append(port);
        url.append(";databaseName=").append(database);
        url.append(";encrypt=").append(encrypt);
        url.append(";trustServerCertificate=").append(trustServerCertificate);
        
        if (integratedSecurity) {
            url.append(";integratedSecurity=true");
        }
        
        return url.toString();
    }
    
    @Override
    public String toString() {
        return String.format("SqlServerConfig{name='%s', host='%s', port=%d, database='%s', username='%s'}", 
                           name, host, port, database, username);
    }
} 