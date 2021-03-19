package net.ccc.library.clink.core;

import java.io.Closeable;
import java.io.IOException;
import java.io.OutputStream;

public abstract class Packet<Stream extends Closeable> implements Closeable {
    public static final byte TYPE_MEMORY_BYTES = 1;
    public static final byte TYPE_MEMORY_STRING = 2;
    public static final byte TYPE_STREAM_FILE = 3;
    public static final byte TYPE_STREAM_DIRECT = 4;
    protected long length;
    private Stream stream;

    public abstract byte type();

    public long length() {
        return length;
    }

    public final Stream open() {
        if (stream == null) {
            stream = createStream();
        }
        return stream;
    }

    @Override
    public final void close() throws IOException {
        if (stream != null) {
            closeStream(stream);
            stream = null;
        }
    }

    protected abstract Stream createStream();

    protected void closeStream(Stream stream) throws IOException {
        stream.close();
    }

    public byte[] headerInfo() {
        return null;
    }
}
