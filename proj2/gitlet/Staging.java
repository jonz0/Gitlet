package gitlet;

import java.io.File;
import java.io.Serializable;
import java.util.*;

public class Staging implements Serializable {
    private final Map<String, Blob> tracked;
    private final Map<String, Blob> toAdd;
    private final Set<String> toRemove;

    /** Constructs the staging area. */
    public Staging() {
        this.tracked = new HashMap<>();
        this.toAdd = new HashMap<>();
        this.toRemove = new HashSet<>();
    }

    public boolean isClear() {
        return toAdd.isEmpty() && toRemove.isEmpty();
    }

    /** Clears the staging area. */
    public void clear() {
        toAdd.clear();
        toRemove.clear();
    }

    /** Attaches a file to the staging area and returns true if the staging area changes. */
    public boolean add(File file) {
        Blob blob = new Blob(file);
        String blobId = blob.getId();
        String filePath = file.getPath();

        if (toRemove.contains(filePath)) {
            toRemove.remove(filePath);
            toAdd.put(filePath, blob);
            tracked.put(filePath, blob);
            return true;
        }

        if (tracked.containsKey(filePath)) {
            String trackedId = tracked.get(filePath).getId();
            if (trackedId.equals(blobId)) return false;
        }

        tracked.put(filePath, blob);
        toAdd.put(filePath, blob);
        return true;
    }

    /** Removes file from the staging area and returns true if the staging area changes. */
    public boolean remove(File file) {
        Blob b = new Blob(file);
        String filePath = file.getPath();

        // if file is already being tracked
        if (tracked.containsKey(filePath)) {
            toAdd.remove(filePath);
            toRemove.add(filePath);
            tracked.remove(filePath);
            return true;
        }

        if (toRemove.contains(filePath)) {
            toRemove.add(filePath);
            return false;
        }

        return true;
    }

    public static Staging readStaging() {
        return Utils.readObject(Repository.STAGING_FILE, Staging.class);
    }

    public Map<String, Blob> getTracked() {
        return tracked;
    }

    public Set<String> getToRemove() {
        return toRemove;
    }

    public boolean isTrackingFile(File file) {
        return tracked.containsKey(file.getPath());
    }

    /** Saves the current staging object to the Staging file. */
    public void save() {
        Utils.writeObject(Repository.STAGING_FILE, this);
    }

    /** clears the add and removal stages. Updates and returns the tracked stage. */
    public Map<String, Blob> commit() {
        for (Blob b : tracked.values()) b.save();
        for (String filePath : toRemove) tracked.remove(filePath);
        tracked.putAll(toAdd);
        for (String file : toRemove) tracked.remove(file);
        clear();
        return tracked;
    }
}
