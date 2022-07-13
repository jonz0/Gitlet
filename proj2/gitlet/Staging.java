package gitlet;

import java.io.File;
import java.io.Serializable;
import java.util.*;

public class Staging implements Serializable {
    Map<String, String> tracked;
    Map<String, String> toAdd;
    Set<String> toRemove;

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
        Blob b = new Blob(file);
        String blobId = b.getId();
        String filePath = file.getPath();
        String trackedId = tracked.get(filePath);

        if (toRemove.contains(filePath)) {
            toRemove.remove(filePath);
            toAdd.put(filePath, blobId);
            tracked.put(filePath, blobId);
            return true;
        }

        if (tracked.containsKey(filePath)) {
            if (trackedId.equals(blobId)) {
                return false;
            } else {
                toAdd.put(filePath, blobId);
                return true;
            }
        } else {
            tracked.put(filePath, blobId);
            toAdd.put(filePath, blobId);
            return true;
        }
    }

    /** Removes file from the staging area. */
    public boolean remove(File file) {
        Blob b = new Blob(file);
        String blobId = b.getId();
        String filePath = file.getPath();

        // if file is already being tracked
        if (tracked.containsKey(filePath)) {
            toAdd.remove(filePath);
            toRemove.add(filePath);
            tracked.remove(filePath);
            return true;
        } else {
            if (toRemove.contains(filePath)) {
                return false;
            } else {
                toRemove.add(filePath);
                return true;
            }
        }
    }

    public static Staging readStaging() {
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
