package gitlet;

import java.io.File;
import java.io.Serializable;
import java.util.*;

public class Blob implements Serializable {

    private byte[] content;
    private String id;
    private File source;

    public Blob(File source) {
        this.content = Utils.readContents(source);
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
}
