package org.eclipse.winery.repository.backend.filebased.model;

import java.nio.file.Path;

public class ConvertedPath {

    private Path originalPath;
    private Path convertedPath;

    public ConvertedPath(Path originalPath, Path convertedPath) {
        this.originalPath = originalPath;
        this.convertedPath = convertedPath;
    }
    
    public ConvertedPath(){}

    public Path getOriginalPath() {
        return originalPath;
    }

    public void setOriginalPath(Path originalPath) {
        this.originalPath = originalPath;
    }

    public Path getConvertedPath() {
        return convertedPath;
    }

    public void setConvertedPath(Path convertedPath) {
        this.convertedPath = convertedPath;
    }
    
    
}
