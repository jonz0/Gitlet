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
    private final String message;
//    private LocalDateTime timestamp;
//    private final DateTimeFormatter formatObj = DateTimeFormatter.ofPattern("EEEE, dd MMMM yyyy");
    private List<String> parents;
    private final String id;
    private final File commitFile;
    private Map<String, String> tracked;

    public Commit(String message, List<String> parents, Map<String, String> tracked) {
        this.message = message;
        this.parents = parents;
        this.tracked = tracked;
//        this.timestamp = LocalDateTime.now();

        if (parents == null) {
//            this.timestamp = LocalDateTime.of(1970, 1,
//                    1, 0, 0, 0);
            this.parents = new ArrayList<>();
        }
        if (tracked == null) this.tracked = new HashMap<>();

        this.id = Utils.sha1(this.message, this.parents.toString(), this.tracked.toString());
        this.commitFile = Utils.join(Repository.COMMITS_DIR, this.id);
    }

    public String getMessage() {
        return this.message;
    }

    public void save() {
        Utils.writeObject(commitFile, this);
        Repository.setHead(id);
    }

    public String getId() {
        return id;
    }

    public static Commit readCommit(String id) {
        File file = Utils.join(Repository.COMMITS_DIR, id);
        return Utils.readObject(file, Commit.class);
    }

//    public String getTimestamp() {
//        return timestamp.format(formatObj);
//    }
//
//    public Map<String, String> getTracked() {
//        return tracked;
//    }

}
