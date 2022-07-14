package gitlet;
import java.io.File;
import java.io.Serializable;

// TODO: any imports you need here

import java.util.*;

import static gitlet.Utils.writeContents;

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
    private final List<String> parents;
    private Map<String, String> tracked;
    private final String id;
    private final File commitFile;

    /** Creates the Commit object.
     * if parents and tracked are null, creates the initial commit. */
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

    public String getId() {
        return id;
    }

    /** Returns a List of IDs of this object's parents. */
    public List<String> getParents() {
        return parents;
    }

    /** Returns the Commit object stored in file id. */
    public static Commit getCommit(String id) {
        File file = Utils.join(Repository.COMMITS_DIR, id);
        return Utils.readObject(file, Commit.class);
    }

    /** Constructs the log fot this commit object. */
    public String getLog() {
        StringBuilder log = new StringBuilder();
        log.append("\nCommit ").append(id);
        log.append("\nDate: ").append(timestamp);
        log.append("\n").append(message).append("\n===");

        return log.toString();
    }

    /** Saves the Commit object to the OBJECTS file.
     * Also calls buildLog, which saves a new log ot he LOG file. */
    public void save() {
        Utils.writeObject(commitFile, this);
        Utils.buildLog();
    }
}
