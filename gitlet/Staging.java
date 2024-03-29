package gitlet;

import java.io.File;
import java.io.Serializable;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * Represents a gitlet staging object. Commits are hashed using message,
 * parents, tracked, and timestamp.
 *
 * @author Jonathan Lu
 */

public class Staging implements Serializable {

    private final Map<String, String> toAdd;
    private final Set<String> toRemove;
    private Map<String, String> tracked;
    private String initialId;

    /**
     * Constructs the staging area.
     */
    public Staging() {
        this.tracked = new HashMap<>();
        this.toAdd = new HashMap<>();
        this.toRemove = new HashSet<>();
    }

    /**
     * Returns the staging object stored in the serialized staging file.
     */
    public static Staging readStaging() {
        return Utils.readObject(Repository.STAGING_FILE, Staging.class);
    }

    /**
     * Clears the staging area.
     */
    public void clear() {
        toAdd.clear();
        toRemove.clear();
    }

    /**
     * Attaches a file to the staging area and returns true if it changes.
     */
    public void add(File file) {
        Blob blob = new Blob(file);
        String blobId = blob.getId();
        String filePath = file.getPath();

        toRemove.remove(filePath);
        if (tracked.containsKey(filePath) && !toAdd.containsKey(filePath)) {
            Blob trackedBlob = Blob.getBlob(tracked.get(filePath), Repository.GITLET_DIR);
            assert trackedBlob != null;
            if (blobId.equals(trackedBlob.getId())) {
                this.save();
                System.exit(0);
            }
        }
        if (toAdd.containsKey(filePath)) {
            Blob addedBlob = Blob.getBlob(toAdd.get(filePath), Repository.GITLET_DIR);
            assert addedBlob != null;
            if (blobId.equals(addedBlob.getId())) {
                toAdd.remove(filePath);
                this.save();
                System.exit(0);
            }
        }
        toAdd.put(filePath, blobId);
        this.save();
    }

    /**
     * Removes file from the staging area and returns true if it changes.
     */
    public void remove(File file) {
        String filePath = file.getPath();

        // if file is already being tracked:
        if (tracked.containsKey(filePath)) {
            toAdd.remove(filePath);
            this.save();
            toRemove.add(filePath);
            file.delete();
        }
        toAdd.remove(filePath);
        this.save();
    }

    /**
     * Clears the add and removal stages. Updates and returns the tracked stage.
     */
    public Map<String, String> commit() {
        for (String filePath : toAdd.keySet()) {
            Blob b = new Blob(Utils.getFile(filePath));
            b.save(Repository.GITLET_DIR);
        }
        for (String filePath : toRemove) {
            tracked.remove(filePath);
        }
        tracked.putAll(toAdd);
        clear();
        return tracked;
    }

    /**
     * Returns the names of all files that are staged for addition and removal.
     */
    public Set<String> getStaged() {
        Set<String> staged = new HashSet<>();
        for (String filePath : toAdd.keySet()) {
            staged.add(new File(filePath).getName());
        }
        for (String filePath : toRemove) {
            staged.add(new File(filePath).getName());
        }
        return staged;
    }

    /**
     * Saves the current staging object to the staging file.
     */
    public void save() {
        Utils.writeObject(Repository.STAGING_FILE, this);
    }

    public boolean isClear() {
        return toAdd.isEmpty() && toRemove.isEmpty();
    }

    public Map<String, String> getTracked() {
        return tracked;
    }

    public void setTracked(Map<String, String> m) {
        tracked = m;
    }

    public Set<String> getToRemove() {
        return toRemove;
    }

    public Map<String, String> getToAdd() {
        return toAdd;
    }

    public String getInitialId() {
        return initialId;
    }

    public void setInitialId(String id) {
        initialId = id;
    }
}
