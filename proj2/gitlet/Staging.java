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

    /** Clears the staging area. */
    public void clear() {
        toAdd.clear();
        toRemove.clear();
    }

    /** Attaches a file to the staging area */
    public boolean add(File file) {
        // if file is being tracked:
        Blob b = new Blob(file);
        String id = b.getId();

        // if file is not being tracked:
    }

    /** Clears the staging area. */
    public boolean remove(File file) {

    }
}
