package dev.brodino.everload.sync;

public class SyncContext {

    public enum Type {
        STARTUP, MANUAL
    }
    
    private final String repositoryUrl;
    private final String branch;
    private final Type type;
    private final long startTime;
    
    private SyncState state;
    private String statusMessage;
    private int currentProgress;
    private int totalProgress;
    private int filesCopied;
    private int totalFiles;
    private long bytesCopied;
    private Exception lastError;
    
    public SyncContext(String repositoryUrl, String branch, Type type) {
        this.repositoryUrl = repositoryUrl;
        this.branch = branch;
        this.type = type;
        this.startTime = System.currentTimeMillis();
        this.state = SyncState.IDLE;
        this.statusMessage = "Initializing...";
        this.currentProgress = 0;
        this.totalProgress = 100;
        this.filesCopied = 0;
        this.totalFiles = 0;
        this.bytesCopied = 0;
    }

    public long getElapsedSeconds() {
        return (System.currentTimeMillis() - startTime) / 1000;
    }

    public int getProgressPercentage() {
        if (totalProgress == 0) return 0;
        return (int) ((currentProgress / (float) totalProgress) * 100);
    }

    public void setProgress(int current, int total) {
        this.currentProgress = Math.min(current, total);
        this.totalProgress = total;
    }
    
    // Getters and setters
    
    public String getRepositoryUrl() {
        return repositoryUrl;
    }
    
    public String getBranch() {
        return branch;
    }
    
    public SyncState getState() {
        return state;
    }
    
    public void setState(SyncState state) {
        this.state = state;
    }
    
    public String getStatusMessage() {
        return statusMessage;
    }
    
    public void setStatusMessage(String statusMessage) {
        this.statusMessage = statusMessage;
    }
    
    public int getFilesCopied() {
        return filesCopied;
    }
    
    public void setFilesCopied(int filesCopied) {
        this.filesCopied = filesCopied;
    }
    
    public int getTotalFiles() {
        return totalFiles;
    }
    
    public void setTotalFiles(int totalFiles) {
        this.totalFiles = totalFiles;
    }
    
    public void setBytesCopied(long bytesCopied) {
        this.bytesCopied = bytesCopied;
    }
    
    public Exception getLastError() {
        return lastError;
    }
    
    public void setLastError(Exception lastError) {
        this.lastError = lastError;
    }
    
    @Override
    public String toString() {
        return "SyncContext{" +
                "type=" + type +
                ", state=" + state +
                ", repository='" + repositoryUrl + '\'' +
                ", branch='" + branch + '\'' +
                ", progress=" + getProgressPercentage() + "%" +
                ", files=" + filesCopied + "/" + totalFiles +
                ", elapsed=" + getElapsedSeconds() + "s" +
                '}';
    }
}
