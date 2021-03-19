package net.ccc.library.clink.box;

import net.ccc.library.clink.core.ReceivePacket;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;

public class FileReceivePacket extends ReceivePacket<FileOutputStream, File> {
    private File file;

    public FileReceivePacket(long len, File file) {
        super(len);
        this.file = file;
    }

    @Override
    public byte type() {
        return TYPE_STREAM_FILE;
    }

    @Override
    protected FileOutputStream createStream() {
        try {
            return new FileOutputStream(file);
        } catch (FileNotFoundException e) {
            return null;
        }
    }

    @Override
    protected File buildEntity(FileOutputStream stream) {
        return file;
    }
}
