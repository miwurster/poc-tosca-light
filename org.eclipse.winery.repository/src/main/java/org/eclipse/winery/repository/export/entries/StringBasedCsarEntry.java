package org.eclipse.winery.repository.export.entries;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import org.apache.commons.io.IOUtils;

public class StringBasedCsarEntry implements CsarEntry {

    private String content;

    public StringBasedCsarEntry(String content) {
        this.content = content;
    }

    @Override
    public InputStream getInputStream() throws IOException {
        return IOUtils.toInputStream(this.content);
    }

    @Override
    public void writeToOutputStream(OutputStream outputStream) throws IOException {
        IOUtils.write(this.content, outputStream);
    }

    public String getContent() {
        return content;
    }
}
