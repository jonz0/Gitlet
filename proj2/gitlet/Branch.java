package gitlet;

import java.io.File;
import java.io.Serializable;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;

import static gitlet.Utils.*;
import static gitlet.Utils.readObject;

/** Represents a gitlet branch object.
 *  Used for storing branches made in the Gitlet repository.
 *  @author Jonathan Lu
 */
public class Branch implements Serializable {

    private final String name;
    private final Commit head;
    private final File branchFile;

    /** Initializes the Branch object and creates an instance of the
     * associated file in BRANCHES_DIR.  */
    public Branch(String name, Commit head) {
        this.name = name;
        this.head = head;
        /* Used for saving. If remote is null, the branch is stored locally.
         * If remote points to a directory, the branch is saved in that directory. */
//        this.branchFile = Utils.join(Objects.requireNonNullElse(remote,
//                Repository.BRANCHES_DIR), name);

        this.branchFile = Utils.join(Repository.BRANCHES_DIR, name);
    }

    /** Returns the Branch object stored in file id. */
    public static Branch getBranch(String name, File remote) {
        File branchDir = Utils.join(Objects.requireNonNullElse(remote,
                Repository.BRANCHES_DIR), name);

        File file = Utils.join(branchDir, name);
        return Utils.readObject(file, Branch.class);
    }

    /** Saves the Branch object to a file (titled name) in the branches folder. */
    public void save() {
        Utils.writeObject(branchFile, this);
    }

    public String getName() {
        return name;
    }

    public Commit getHead() {
        return head;
    }
    
    public File getBranchFile() {
        return branchFile;
    }
}
