package dev.brodino.everload.sync;

public enum SyncState {
    IDLE,
    IN_PROGRESS,
    AWAITING_CONFIRMATION,
    COMPLETED,
    FAILED,
    CANCELLED;

    public boolean isActive() {
        return this == IN_PROGRESS || this == AWAITING_CONFIRMATION;
    }
}
