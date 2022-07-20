package gitlet;
import java.io.File;
import java.io.Serializable;
import java.util.*;


public class Branch implements Serializable {

    private final String name;
    private final Commit head;
    private final File branchFile;

    /** Initializes the Branch object and creates an instance of the
     * associated file in BRANCHES_DIR.  */
    public Branch(String name, Commit head) {
        this.name = name;
        this.head = head;
        this.branchFile = Utils.join(Repository.BRANCHES_DIR, name);
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
    
    /** Returns the Branch object stored in file id. */
    public static Branch getBranch(String name) {
        File file = Utils.join(Repository.BRANCHES_DIR, name);
        return Utils.readObject(file, Branch.class);
    }

    /** Saves the Branch object to a file (titled name) in the branches folder. */
    public void save() {
        Utils.writeObject(branchFile, this);
    }
}
