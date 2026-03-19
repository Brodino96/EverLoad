package dev.brodino.elysiumsync.sync;

public enum SyncState {
    IDLE,
    IN_PROGRESS,
    COMPLETED,
    FAILED,
    CANCELLED;

    public boolean isActive() {
        return this == IN_PROGRESS;
    }
}
