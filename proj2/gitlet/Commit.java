package gitlet;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

// TODO: any imports you need here

import java.util.Date; // TODO: You'll likely use this in this class
import java.util.Map;

/** Represents a gitlet commit object.
 *  TODO: It's a good idea to give a description here of what else this Class
 *  does at a high level.
 *
 *  @author TODO
 */
public class Commit {
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
    private Commit parent;

    private Map<String, String> tracked;

    private String id;

    public Commit(String message, Commit parent, Map<String, String> tracked) {
        this.message = message;
        this.parent = parent;
        if (parent == null) timestamp = LocalDateTime.of(1970, 1,
                1, 0, 0, 0);
        else timestamp = LocalDateTime.now();
        this.tracked = tracked;
        id = Utils.sha1(message, parent.toString(), tracked.toString());
    }

    public String getMessage() {
        return this.message;
    }

    public String getTimestamp() {
        return timestamp.format(formatObj);
    }

//    public void save() {
//        Utils.writeObject(file, this);
//    }

    public String getId() {
        return id;
    }
    public String toString() {
        String parentId;
        if (parent == null) parentId = "0";
        else parentId = parent.getId();

        return message + parentId + tracked.toString();
    }
}
