package gitlet;

import java.io.File;
import java.io.Serializable;
import java.util.*;

public class Blob implements Serializable {

    private final byte[] content;
    private final String id;
    private final File source;
    private final File blobFile;

    public Blob(File source) {
        this.content = Utils.readContents(source);
        this.id = Utils.sha1(source.getPath(), this.content);
        this.source = source;
        this.blobFile = Utils.join(Repository.BLOBS_DIR, id);
    }

    public File getSource() {
        return source;
    }

    public String getId() {
        return id;
    }

    public byte[] getContent() {
        return content;
    }

    public File getBlobFile() { return blobFile; }

    /** Returns the Commit object stored in file id. */
    public static Blob getBlob(String id) {
        File file = Utils.join(Repository.BLOBS_DIR, id);
        return Utils.readObject(file, Blob.class);
    }

    public void save() {
        Utils.writeObject(blobFile, this);
    }
}
