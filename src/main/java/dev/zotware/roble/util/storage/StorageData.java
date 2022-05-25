package dev.zotware.roble.util.storage;

import dev.zotware.roble.exceptions.StorageException;
import dev.zotware.roble.util.storage.types.FlatFile;
import dev.zotware.roble.util.storage.types.SQLStorage;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.Map;

public abstract class StorageData {

    private final Storage storage;

    public StorageData(@NotNull Storage storage) throws StorageException {
        this.storage = storage;

        final String[] keys = getStructure();
        if (keys.length <= 0) throw new StorageException("The structure can not be empty.");


    }

    public abstract Map<String, Object> serialize();

    /**
     * Saves the data based on existing data provided.
     */
    public void save() {
        if (getStorage() instanceof SQLStorage) {
            final SQLStorage sqlStorage = (SQLStorage) getStorage();
            switch (sqlStorage.getType()) {
                case MariaDB:
                case MySQL: {

                    final Map<String, Object> valueMap = serialize();
                    final StringBuilder keys = new StringBuilder(), values = new StringBuilder(), valuesDupe = new StringBuilder();
                    for (String key : getStructure()) {
                        final Object value = valueMap.getOrDefault(key, null);
                        if (value == null) continue;

                        // handle key string
                        if (keys.length() > 0) keys.append(", ");
                        if (key.contains(" ")) keys.append(key.split(" ")[0]);
                        else if (key.contains(":")) keys.append(key.split(":")[0]);
                        else keys.append(key);

                        // handle value string
                        if (values.length() > 0) values.append(", ");
                        values.append("'").append(value).append("'");

                        if (valuesDupe.length() > 0) valuesDupe.append(", ");
                        if (key.contains(" ")) keys.append(key.split(" ")[0]).append(" = '").append(value).append("'");
                        else keys.append(key).append(" = '").append(value).append("'");
                    }

                    try (PreparedStatement statement = sqlStorage.CONNECTION.prepareStatement("INSERT INTO " + getTable()
                            + "(" + keys + ") VALUES(" + values + ") ON DUPLICATE KEY UPDATE " + valuesDupe + ";")) {
                        statement.executeUpdate();
                    } catch (SQLException e) {e.printStackTrace();}

                }

                default: { // defaults to SQLite

                    final Map<String, Object> valueMap = serialize();
                    final StringBuilder keys = new StringBuilder(), values = new StringBuilder();
                    for (String key : getStructure()) {
                        final Object value = valueMap.getOrDefault(key, "");
                        if (value == null) continue;

                        // handle key string
                        if (keys.length() > 0) keys.append(", ");
                        if (key.contains(" ")) keys.append(key.split(" ")[0]);
                        else if (key.contains(":")) keys.append(key.split(":")[0]);
                        else keys.append(key);

                        // handle value string
                        if (values.length() > 0) values.append(", ");
                        values.append("'").append(value).append("'");
                    }

                    System.out.println("INSERT OR REPLACE INTO " + getTable() + "(" + keys + ") VALUES(" + values + ");");
                    // TODO remove


                    try (PreparedStatement statement = sqlStorage.CONNECTION.prepareStatement("INSERT OR REPLACE INTO "
                            + getTable() + "(" + keys + ") VALUES(" + values + ");")) {
                        statement.executeUpdate();
                    } catch (SQLException e) {e.printStackTrace();}

                }
            }
            return;
        }

        final FlatFile flatFile = (FlatFile) getStorage();
        for (Map.Entry<String, Object> entry : serialize().entrySet())
            flatFile.getConfiguration().set(entry.getKey(), entry.getValue());
        flatFile.getConfiguration().save();
    }

    /**
     * Deletes the primary key from the storage.
     *
     * @param primaryKey The key identifier.
     * @param value      The value the key would be set to (Only for SQL).
     */
    public void delete(@NotNull String primaryKey, @Nullable String... value) {
        if (getStorage() instanceof SQLStorage && value.length > 0) {
            final SQLStorage sqlStorage = (SQLStorage) getStorage();

            try (PreparedStatement statement = sqlStorage.CONNECTION.prepareStatement("DELETE FROM "
                    + getTable() + " WHERE " + primaryKey + "= '" + value[0] + "';")) {
                statement.executeUpdate();
            } catch (SQLException e) {e.printStackTrace();}

            return;
        }

        final FlatFile flatFile = (FlatFile) getStorage();
        flatFile.getConfiguration().set(primaryKey, null);
        flatFile.getConfiguration().save();
    }

    public abstract String[] getStructure();

    public abstract String getTable();

    // getters & setters

    public Storage getStorage() {return storage;}

}