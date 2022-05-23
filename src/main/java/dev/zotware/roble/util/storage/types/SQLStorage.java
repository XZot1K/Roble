package dev.zotware.roble.util.storage.types;

import dev.zotware.roble.RoblePlugin;
import dev.zotware.roble.exceptions.StorageException;
import dev.zotware.roble.util.storage.Storage;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;

public class SQLStorage extends Storage {
    public Connection CONNECTION;
    private String filePath, host, database, username, password;
    private boolean useSSL;

    public SQLStorage(@NotNull RoblePlugin instance, @NotNull Type type, @Nullable String... properties) throws StorageException {
        super(instance, type);
        setUseSSL(false);
        setFilePath(null);

        switch (type) {
            case MariaDB:
            case MySQL: {
                if (properties.length < 5) throw new StorageException(type.name()
                        + " requires a host, port, database, username, and password to be defined.");

                setHost(properties[0] + ":" + properties[1]);
                setDatabase(properties[2]);
                setUsername(properties[3]);
                setPassword(properties[4]);
                break;
            }

            default: { // defaults to SQLite
                if (properties.length <= 0) throw new StorageException("SQLite requires the file path to be provided.");

                final String property = properties[0];
                if (property == null || property.isEmpty()) throw new StorageException("The provided file path was invalid or empty.");

                final String extension = ((property.toLowerCase().endsWith(".db")) ? "" : ".db");
                if (!property.contains("/")) this.filePath = (instance.getDataFolder() + "/" + property + extension);
                else this.filePath = (property + extension);
                break;
            }
        }

    }

    /**
     * Creates a table with the provided properties.
     *
     * @param tableName The table name.
     * @param columns   The columns and variable identifier seperated by a space.
     */
    public void createTables(@NotNull String tableName, @NotNull String... columns) {
        final StringBuilder syntax = new StringBuilder("CREATE TABLE IF NOT EXISTS " + tableName + "(");
        for (int i = -1; ++i < columns.length; ) {
            final String column = columns[i];

            if (syntax.length() > 0) syntax.append(", ");
            syntax.append(column);
        }
        syntax.append(");");

        if (syntax.length() > 0)
            try (Statement statement = CONNECTION.createStatement()) {
                statement.executeUpdate(syntax.toString());
            } catch (SQLException e) {e.printStackTrace();}
    }

    @Override
    public void register() {
        try {
            if (CONNECTION != null && !CONNECTION.isClosed()) {
                CONNECTION.close();
                CONNECTION = null;
            }

            switch (getType()) {
                case MySQL: {
                    try {
                        Class.forName("com.mysql.cj.jdbc.Driver");
                    } catch (NoClassDefFoundError | ClassNotFoundException ignored) {Class.forName("com.mysql.jdbc.Driver");}

                    CONNECTION = DriverManager.getConnection("jdbc:mysql://" + getHost() + "/" + getDatabase()
                            + "?" + (useSSL() ? "verifyServerCertificate=false&useSSL=true&requireSSL=true" : "useSSL=false")
                            + "&autoReconnect=true&useUnicode=yes", getUsername(), getPassword());
                    break;
                }

                case MariaDB: {
                    Class.forName("org.mariadb.jdbc.Driver");
                    CONNECTION = DriverManager.getConnection("jdbc:mariadb://" + getHost() + "/" + getDatabase()
                            + "?" + (useSSL() ? "verifyServerCertificate=false&useSSL=true&requireSSL=true" : "useSSL=false")
                            + "&autoReconnect=true&useUnicode=yes", getUsername(), getPassword());
                    break;
                }

                default: { // defaults to SQLite
                    Class.forName("org.sqlite.JDBC");
                    CONNECTION = DriverManager.getConnection("jdbc:sqlite:" + getFilePath());
                    break;
                }
            }

            final Statement integrityCheck = CONNECTION.createStatement();
            integrityCheck.executeUpdate("PRAGMA integrity_check;");
            integrityCheck.close();
        } catch (NoClassDefFoundError | ClassNotFoundException | SQLException e) {e.printStackTrace();}
    }

    @Override
    public boolean isReady() {
        try {
            return (CONNECTION != null && !CONNECTION.isClosed());
        } catch (SQLException e) {e.printStackTrace();}
        return false;
    }

    // TAG getters & setters

    public String getFilePath() {return filePath;}

    public void setFilePath(String filePath) {this.filePath = filePath;}

    public String getHost() {return host;}

    public void setHost(String host) {this.host = host;}

    public String getDatabase() {return database;}

    public void setDatabase(String database) {this.database = database;}

    public String getUsername() {return username;}

    public void setUsername(String username) {this.username = username;}

    public String getPassword() {return password;}

    public void setPassword(String password) {this.password = password;}

    public void setUseSSL(boolean useSSL) {this.useSSL = useSSL;}

    public boolean useSSL() {return useSSL;}

}