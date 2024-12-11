package com.enterprise.common.models;

public class PathResolution {
    public final Path path;
    public final PathResolutionStatus status;
    public final String message;
    
    public PathResolution(Path path, PathResolutionStatus status) {
        this(path, status, null);
    }
    
    public PathResolution(Path path, PathResolutionStatus status, String message) {
        this.path = path;
        this.status = status;
        this.message = message;
    }

    public PathResolutionStatus getStatus() {
        return status;
    }

    public Path getPath() {
        return path;
    }
}
