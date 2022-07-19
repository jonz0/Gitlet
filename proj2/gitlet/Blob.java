package gitlet;

import java.io.File;
import java.io.Serializable;
import java.util.*;

public class Blob implements Serializable {

    private final byte[] content;
    private final String contentString;
    private final String id;
    private final File source;

    public Blob(File source) {
        this.content = Utils.readContents(source);
        this.contentString = Utils.readContentsAsString(source);
        this.id = Utils.sha1(source.getPath(), this.content);
        this.source = source;
    }

    public File getSource() {
        return source;
    }

    public String getId() {
        return id;
    }

    public byte[] getContent() { return content; }

    public String getContentString() { return contentString; }

    /** Returns the Commit object stored in file id. */
    public static Blob getBlob(String id) {
        File file = Utils.join(Repository.BLOBS_DIR, id);
        if (!file.exists()) {
            Utils.exit("No tracked file exists with that id.");
        }
        return Utils.readObject(file, Blob.class);
    }

    public void save() {
        Utils.writeObject(Utils.join(Repository.BLOBS_DIR, id), this);
    }
}
