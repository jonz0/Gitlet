package gitlet;
import java.io.File;
import java.io.Serializable;
import java.util.*;

import static gitlet.Utils.*;

/** Represents a gitlet commit object.
 *  Commits are hashed using message, parents, tracked, and timestamp.
 *
 *  @author TODO
 */
public class Commit implements Serializable {

    /** The message of this Commit. */
    private final String message;
    private final String timestamp;
    private final List<String> parents;
    /** Tracked: key is the filepath, and value is the ID of the associated Blob object. */
    private final Map<String, String> tracked;
    private final String id;
    /** Depth used for the merge command,where the shared node of highest depth
     * corresponds to the least common ancestor of two nodes. */
    private final int depth;
    private final String branch;

    /** Creates the Commit object.
     * if parents and tracked are null, creates the initial commit. */
    public Commit(String message, List<String> parents, Map<String, String> tracked,
                  String timestamp, int depth, String branch) {
        this.message = message;
        this.timestamp = timestamp;
        this.depth = depth;
        this.branch = branch;

        if (parents == null) {
            this.parents = new ArrayList<>();
        } else {
            this.parents = parents;
        }

        if (tracked == null) {
            this.tracked = new HashMap<>();
        } else {
            this.tracked = tracked;
        }

        this.id = Utils.sha1(this.message, this.parents.toString(), this.tracked.toString());
    }

    public String getMessage() {
        return message;
    }

    public int getDepth() {
        return depth;
    }

    public Map<String, String> getTracked() {
        return tracked;
    }

    public Set<String> getTrackedNames() {
        Set<String> trackedNames = new HashSet<>();
        for (String filePath : tracked.keySet()) {
            trackedNames.add(new File(filePath).getName());
        }

        return trackedNames;
    }

    public String getBranch() {
        return branch;
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
        File commitFile;
        String folderName = id.substring(0, 2);
        String fileName = id.substring(2);
        File folder = Utils.join(Repository.OBJECTS_DIR, folderName);
        if (!folder.exists()) {
            return null;
        }
        commitFile = join(folder, fileName);
        if (fileName.length() < Utils.UID_LENGTH - 2) {
            List<String> containedCommits = plainFilenamesIn(folder);
            for (String commitId : containedCommits) {
                if (commitId.startsWith(fileName)) {
                    commitFile = join(folder, commitId);
                }
            }
        }
        if (!commitFile.exists()) {
            return null;
        }
        return Utils.readObject(commitFile, Commit.class);
    }

    /** Constructs the log fot this commit object. */
    public String getLog() {
        StringBuilder log = new StringBuilder();
        log.append("\n===");
        log.append("\ncommit ").append(id);
        if (parents.size() > 1) {
            log.append("\nMerge: ");
            log.append(parents.get(0), 0, 7).append(" ");
            log.append(parents.get(1), 0, 7);
        }
        log.append("\nDate: ").append(timestamp);
        log.append("\n").append(message).append("\n");
        return log.toString();
    }

    public void restoreTrackedFiles() {
        for (String blobId : tracked.values()) {
            Blob b = Blob.getBlob(blobId);
            Utils.writeContents(b.getSource(), b.getContent());
        }
    }

    public void deleteUntrackedFiles() {
        Commit head = getCommit(readContentsAsString(Repository.HEAD));
        for (String filePath : head.getTracked().keySet()) {
            if (!getTracked().containsKey(filePath)) {
                String fileName = new File(filePath).getName();
                File f = join(Repository.CWD, fileName);
                restrictedDelete(f);
            }
        }
    }

    /** Saves the Commit object to the OBJECTS file.
     * Also calls buildLog, which saves a new log ot he LOG file. */
    public void save() {
        String folderName = id.substring(0, 2);
        String fileName = id.substring(2);

        File folder = join(Repository.OBJECTS_DIR, folderName);
        folder.mkdir();
        File commitFile = join(folder, fileName);
        Utils.writeObject(commitFile, this);
    }
}
