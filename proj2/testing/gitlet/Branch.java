package gitlet;

import java.io.File;
import java.io.Serializable;

/**
 * Represents a gitlet branch object.
 * Used for storing branches made in the Gitlet repository.
 *
 * @author Jonathan Lu
 */

public class Branch implements Serializable {

    private final String name;
    private final Commit head;

    /**
     * Initializes the Branch object and creates an instance of the
     * associated file in BRANCHES_DIR.
     */
    public Branch(String name, Commit head) {
        this.name = name;
        this.head = head;
    }

    /**
     * Saves the Branch object to a file (titled name) in the branches folder.
     */
    public void save(File gitletDir) {
        File branchFolder = Utils.join(gitletDir, "branches");
        File branchFile = Utils.join(branchFolder, name);
        Utils.writeObject(branchFile, this);
    }

    public String getName() {
        return name;
    }

    public Commit getHead() {
        return head;
    }
}
