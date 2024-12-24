package com.enterprise.common.models;

public class PathResolution {
    public final Path path;
    public final PathResolutionStatus status;
    public final String message;
    public long delaySeconds;
    
    public PathResolution(Path path, PathResolutionStatus status, long delaySeconds) {
        this(path, status, null, delaySeconds);
    }
    
    public PathResolution(Path path, PathResolutionStatus status, String message,long delaySeconds) {
        this.path = path;
        this.status = status;
        this.message = message;
        this.delaySeconds=delaySeconds;

    }

    public PathResolutionStatus getStatus() {
        return status;
    }

    public Path getPath() {
        return path;
    }
}
