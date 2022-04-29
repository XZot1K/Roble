package dev.zotware.roble.util.storage.types;

import dev.zotware.roble.RoblePlugin;
import dev.zotware.roble.util.Configuration;
import dev.zotware.roble.util.storage.Storage;
import org.jetbrains.annotations.NotNull;

public class FlatFile extends Storage {

    private final String filePath;
    private Configuration configuration;

    public FlatFile(@NotNull RoblePlugin instance, @NotNull String filePath) {
        super(instance, Type.FLAT);
        this.filePath = filePath;
    }

    @Override
    public void register() {configuration = new Configuration(filePath);}

    @Override
    public boolean isReady() {return true;}

    public Configuration getConfiguration() {return configuration;}

}