package dev.zotware.roble.util.storage;

import dev.zotware.roble.RoblePlugin;
import org.jetbrains.annotations.NotNull;

public abstract class Storage {

    private final RoblePlugin INSTANCE;
    private final Type type;

    public Storage(RoblePlugin instance, @NotNull Type type) {
        this.INSTANCE = instance;
        this.type = type;
    }

    /**
     * Formulates the storage object by creating a connection or file.
     * If exists, the storage will be collapsed and re-established.
     */
    public abstract void register();

    /**
     * @return Returns if the storage is able to write or read.
     */
    public abstract boolean isReady();

    public Type getType() {return type;}

    public enum Type {FLAT, SQL}

}