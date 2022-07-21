package gitlet;

import java.io.File;
import java.io.Serializable;
import java.util.*;

import static gitlet.Utils.join;
import static gitlet.Utils.plainFilenamesIn;

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

    public byte[] getContent() {
        return content;
    }

    public String getContentString() {
        return contentString;
    }

    /** Returns the Commit object stored in file id. */
    public static Blob getBlob(String id) {
        File blobFile;
        String folderName = id.substring(0, 2);
        String fileName = id.substring(2);
        File folder = Utils.join(Repository.OBJECTS_DIR, folderName);
        if (!folder.exists()) {
            return null;
        }
        blobFile = join(folder, fileName);
        if (fileName.length() < 38) {
            List<String> containedBlobs = plainFilenamesIn(folder);
            for (String blobId : containedBlobs) {
                if (blobId.startsWith(fileName)) {
                    blobFile = join(folder, blobId);
                }
            }
        }
        if (!blobFile.exists()) {
            return null;
        }
        return Utils.readObject(blobFile, Blob.class);
    }

    public void save() {
        String folderName = id.substring(0, 2);
        String fileName = id.substring(2);

        File folder = join(Repository.OBJECTS_DIR, folderName);
        folder.mkdir();
        File blobFile = join(folder, fileName);
        Utils.writeObject(blobFile, this);
    }
}
