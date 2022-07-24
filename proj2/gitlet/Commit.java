package gitlet;

import java.io.File;
import java.io.Serializable;
import java.util.*;

import static gitlet.Utils.*;

/** Represents a gitlet commit object.
 *  Commits are hashed using message, parents, tracked, and timestamp.
 *  Used for serializing and storing commits in the Gitlet repository.
 *  @author Jonathan Lu
 */
public class Commit implements Serializable {

    /** The message of this Commit. */
    private final String message;
    private final String timestamp;
    private final List<String> parents;
    /** Tracked: key is the filepath, and value is the ID of the associated blob object. */
    private final Map<String, String> tracked;
    private final String id;
    /** Depth used for the merge command,where the shared node of highest depth
     * corresponds to the least common ancestor of two nodes. */
    private final int depth;
    private final String branch;

    /** Creates the commit object.
     * If parents and tracked are null, creates the initial commit. */
    public Commit(String message, List<String> parents, Map<String, String> tracked,
                  String timestamp, int depth, String branch) {
        this.message = message;
        this.timestamp = timestamp;
        this.depth = depth;
        this.branch = branch;

        // IF parents is null, tracked is an empty ArrayList.
        this.parents = Objects.requireNonNullElseGet(parents, ArrayList::new);

        // IF tracked is null, tracked is an empty HashMap.
        this.tracked = Objects.requireNonNullElseGet(tracked, HashMap::new);

        // This object's id is the SHA-1 hash of the message, parents, and tracked.
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

    public String getBranch() {
        return branch;
    }

    public String getId() {
        return id;
    }

    /** Returns a List of ids of this object's parents. */
    public List<String> getParents() {
        return parents;
    }

    /** Returns a List of file names tracked by this Commit. */
    public Set<String> getTrackedNames() {
        Set<String> trackedNames = new HashSet<>();
        for (String filePath : tracked.keySet()) {
            trackedNames.add(new File(filePath).getName());
        }

        return trackedNames;
    }

    /** Returns the commit object stored in the file id.
     * Returns null if the blob id does not reference an existing Commit. */
    public static Commit getCommit(String id, File remote) {
        String folderName = id.substring(0, 2);
        String fileName = id.substring(2);
        File folder = Utils.join(Repository.OBJECTS_DIR, folderName);;
        if (remote != null) {
            File objects = Utils.join(remote, "objects");
            folder = Utils.join(objects, folderName);
        }

        if (!folder.exists()) {
            return null;
        }
        File commitFile = join(folder, fileName);
        if (fileName.length() < Utils.UID_LENGTH - 2) {
            List<String> containedCommits = plainFilenamesIn(folder);
            assert containedCommits != null;
            for (String commitId : containedCommits) {
                if (commitId.startsWith(fileName)) {
                    commitFile = join(folder, commitId);
                    break;
                }
            }
        }
        if (!commitFile.exists()) {
            return null;
        }
        return Utils.readObject(commitFile, Commit.class);
    }

    /** Constructs the log for this commit object. */
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

    /** Restores the files tracked by this Commit. Used for checkout. */
    public void restoreTrackedFiles() {
        for (String blobId : tracked.values()) {
            Blob b = Blob.getBlob(blobId, null);
            assert b != null;
            Utils.writeContents(b.getSource(), (Object) b.getContent());
        }
    }

    /** Deletes any files not tracked by this Commit. Used for checkout. */
    public void deleteUntrackedFiles() {
        Commit head = getCommit(readContentsAsString(Repository.HEAD), null);
        assert head != null;
        for (String filePath : head.getTracked().keySet()) {
            if (!getTracked().containsKey(filePath)) {
                String fileName = new File(filePath).getName();
                File f = join(Repository.CWD, fileName);
                restrictedDelete(f);
            }
        }
    }

    /** Saves the commit object to the OBJECTS file in a directory named
     * the first two characters of the commit id. */
    public void save(File location) {
        String folderName = id.substring(0, 2);
        String fileName = id.substring(2);
        File folder = Utils.join(Repository.OBJECTS_DIR, folderName);;
        if (location != null) {
            File objects = Utils.join(location, "objects");
            folder = Utils.join(objects, folderName);
        }
        folder.mkdir();
        File commitFile = join(folder, fileName);
        Utils.writeObject(commitFile, this);
    }
}
