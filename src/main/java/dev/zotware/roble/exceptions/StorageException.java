package dev.zotware.roble.exceptions;

import org.jetbrains.annotations.NotNull;

public class StorageException extends Exception {
    public StorageException(@NotNull String message) {super(message);}
}