package gitlet;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

// TODO: any imports you need here

import java.util.Date; // TODO: You'll likely use this in this class

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

    public Commit(String message, Commit parent) {
        this.message = message;
        this.parent = parent;
        if (parent == null) timestamp = LocalDateTime.of(1970, 1,
                1, 0, 0, 0);
        else timestamp = LocalDateTime.now();
    }

    public String getMessage() {
        return this.message;
    }

    public String getTimestamp() {
        return timestamp.format(formatObj);
    }

    public void commit() {

    }
}
