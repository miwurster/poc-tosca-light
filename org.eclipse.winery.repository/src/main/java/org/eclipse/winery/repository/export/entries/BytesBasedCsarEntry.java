package org.eclipse.winery.repository.export.entries;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

public class BytesBasedCsarEntry implements CsarEntry {

    private byte[] content;

    public BytesBasedCsarEntry(byte[] content) {
        this.content = content;
    }

    @Override
    public InputStream getInputStream() {
        return new ByteArrayInputStream(this.content);
    }

    @Override
    public void writeToOutputStream(OutputStream outputStream) throws IOException {
        outputStream.write(this.content);
    }

    public byte[] getContent() {
        return content;
    }
}
