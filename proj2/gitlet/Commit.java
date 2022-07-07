package gitlet;
import java.io.File;
import java.io.Serializable;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

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
    private String message;
    private LocalDateTime timestamp;
    private final DateTimeFormatter formatObj = DateTimeFormatter.ofPattern("EEEE, dd MMMM yyyy");
    private List<String> parents;
    private Map<String, String> tracked;
    private String id;

    private File commitFile;

    public Commit(String message, List<String> parents, Map<String, String> tracked) {
        this.message = message;
        this.parents = parents;
        this.tracked = tracked;
        if (parents == null) {
            timestamp = LocalDateTime.of(1970, 1,
                    1, 0, 0, 0);
            parents = new ArrayList<>();
        } else timestamp = LocalDateTime.now();
        if (tracked == null) tracked = new HashMap<>();
        id = Utils.sha1(message, parents.toString(), tracked.toString());
        commitFile = Utils.join(Repository.COMMITS_DIR, this.id);
    }

    public String getMessage() {
        return this.message;
    }

    public String getTimestamp() {
        return timestamp.format(formatObj);
    }

    public Map<String, String> getTracked() {
        return tracked;
    }

    public void save() {
        Utils.writeObject(commitFile, this);
    }

    public String getId() {
        return id;
    }

    public static Commit readSha(String id) {
        File file = Utils.join(Repository.COMMITS_DIR, id);
        return Utils.readObject(file, Commit.class);
    }

}
