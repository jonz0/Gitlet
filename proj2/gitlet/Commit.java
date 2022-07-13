package gitlet;
import java.io.File;
import java.io.Serializable;

// TODO: any imports you need here

import java.util.*;

/** Represents a gitlet commit object.
 *  TODO: It's a good idea to give a description here of what else this Class
 *  does at a high level.
 *
 *  @author TODO
 */
public class Commit implements Serializable {
    /**
     * TODO: add instance variables here.
     *
     * List all instance variables of the Commit class here with a useful
     * comment above them describing what that variable represents and how that
     * variable is used. We've provided one example for `message`.
     */

    /** The message of this Commit. */
    private final String message;
    private final String timestamp;
    private List<String> parents;
    private Map<String, String> tracked;
    private final String id;
    private final File commitFile;


    public Commit(String message, List<String> parents, Map<String, String> tracked, String timestamp) {
        this.message = message;
        this.timestamp = timestamp;

        if (parents == null) this.parents = new ArrayList<>();
        else this.parents = parents;

        if (tracked == null) this.tracked = new HashMap<>();
        else this.tracked = tracked;

        this.id = Utils.sha1(this.message, this.parents.toString(), this.tracked.toString());
        this.commitFile = Utils.join(Repository.COMMITS_DIR, this.id);
    }

    public String getMessage() {
        return message;
    }

    public String getTimestamp() {
        return timestamp;
    }

    public void save() {
        Utils.writeObject(commitFile, this);
    }

    public String getId() {
        return id;
    }

    public static Commit getCommit(String id) {
        File file = Utils.join(Repository.COMMITS_DIR, id);
        return Utils.readObject(file, Commit.class);
    }
}
