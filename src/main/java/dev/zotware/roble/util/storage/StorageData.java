package dev.zotware.roble.util.storage;

import dev.zotware.roble.exceptions.StorageException;
import dev.zotware.roble.util.storage.types.FlatFile;
import dev.zotware.roble.util.storage.types.SQLStorage;
import org.jetbrains.annotations.NotNull;

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

    public void save() {
        if (getStorage().getType() == Storage.Type.SQL) {

            final StringBuilder keys = new StringBuilder();
            for (String key : getStructure()) {
                if (keys.length() > 0) keys.append(", ");

                if (key.contains(" ")) keys.append(key.split(" ")[0]);
                else keys.append(key);
            }

            final SQLStorage sqlStorage = (SQLStorage) getStorage();
            switch (sqlStorage.getSQLType()) {
                case MariaDB:
                case MySQL: {

                    final Map<String, Object> valueMap = serialize();
                    final StringBuilder values = new StringBuilder(), valuesDupe = new StringBuilder();
                    for (String key : getStructure()) {
                        final Object value = valueMap.getOrDefault(key, null);
                        if (value == null) continue;

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
                    final StringBuilder values = new StringBuilder();
                    for (String key : getStructure()) {
                        final Object value = valueMap.getOrDefault(key, null);
                        if (value == null) continue;

                        if (values.length() > 0) values.append(", ");
                        values.append("'").append(value).append("'");
                    }

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

    public abstract String[] getStructure();

    public abstract String getTable();

    // getters & setters

    public Storage getStorage() {return storage;}

}