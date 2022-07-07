package gitlet;

import java.io.File;
import java.io.Serializable;
import java.util.*;

public class Staging implements Serializable {
    private Map<String, String> tracked;
    private Map<String, String> toAdd;
    private Set<String> toRemove;

    /** Constructs the staging area. */
    public Staging() {
        this.tracked = new HashMap<>();
        this.toAdd = new HashMap<>();
        this.toRemove = new HashSet<>();
    }

    public boolean isEmpty() {
        if (toAdd.isEmpty() && toRemove.isEmpty()) return true;
        return false;
    }

    /** Clears the staging area. */
    public void clear() {
        toAdd.clear();
        toRemove.clear();
    }

    /** Attaches a file to the staging area */
    public boolean add(File file) {
        Blob b = new Blob(file);
        String blobId = b.getId();
        String filePath = file.getPath();

        // if file is being tracked:
        if (tracked.containsKey(filePath)) {
            String trackedId = tracked.get(filePath);
            if (blobId == trackedId) {
                if (toAdd.containsKey(filePath)) {
                    toAdd.remove(filePath);
                    return true;
                } else toRemove.remove(filePath);
            }
        }

        // if file is being tracked:
        String prevId = tracked.put(file.getPath(), blobId);
        if (prevId != null && prevId == blobId) return false;

        return true;
    }

    /** Clears the staging area. */
    public boolean remove(File file) {
        Blob b = new Blob(file);
        String blobId = b.getId();
        String filePath = file.getPath();

        // if file is already being tracked
        if (tracked.containsKey(filePath)) {
            if (!toAdd.containsKey(filePath)) {
                toAdd.remove(filePath);
                toRemove.add(filePath);
                return true;
            }
            return false;
        }

        // if file is not being tracked
        tracked.put(filePath, blobId);
        toRemove.add(filePath);
        return true;
    }

    public static Staging fromFile() {
        return Utils.readObject(Repository.STAGING_FILE, Staging.class);
    }

    public Map<String, String> getTracked() {
        return tracked;
    }

    /** Saves the current staging object to the Staging file. */
    public void save() {
        Utils.writeObject(Repository.STAGING_FILE, this);
    }

    /** clears the add and removal stages. Updates and returns the tracked stage. */
    public Map<String, String> commit() {
        tracked.putAll(toAdd);
        for (String file : toRemove) tracked.remove(file);
        clear();
        return tracked;
    }
}
