package gitlet;

import java.io.File;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static gitlet.Utils.*;

/** Creates and represents the gitlet repository.
 * Stores all functionality of commands listed in the Main class.
 */
public class Repository {

    /** The current working directory. */
    public static final File CWD = new File(System.getProperty("user.dir"));
    /** The .gitlet directory. */
    public static final File GITLET_DIR = join(CWD, "gitlet-temp");
    public static final File HEAD = join(GITLET_DIR, "head");
    public static final File LOG = join(GITLET_DIR, "log");
    public static final File STAGING_FILE = join(GITLET_DIR, "staging");
    public static Staging staging = STAGING_FILE.exists() ? Staging.readStaging() : new Staging();
    public static final File BRANCHES_DIR = join(GITLET_DIR, "branches");
    public static final File ACTIVE_BRANCH = join(GITLET_DIR, "active branch");
    /** The .commits directory. */
    public static final File COMMITS_DIR = join(GITLET_DIR, "commits");
    /** Formatter for the timestamp passed to Commit objects. */
    private final DateTimeFormatter formatObj = DateTimeFormatter.ofPattern("EEEE, dd MMMM yyyy, HH:mm:ss");

    /** Creates a new INITIAL Commit object and saves it to a file.
     * The file is stored in the COMMITS_DIR with a preset message. */
    public void init() {
        if (GITLET_DIR.exists()) System.out.println("Gitlet version control already exists in this directory, fool");
        else {
            GITLET_DIR.mkdir();
            COMMITS_DIR.mkdir();
            BRANCHES_DIR.mkdir();

            LocalDateTime time = LocalDateTime.of(1970, 1,
                    1, 0, 0, 0);
            String timestamp = time.format(formatObj);
            Commit initial = new Commit("initial commit", null, null, timestamp);
            Branch master = new Branch("master", initial);

            initial.save();
            master.save();
            setHead(initial.getId());
            Utils.updateActiveBranch(initial);
        }
    }

    /** Stages a file for addition. */
    public void add(String name) {
        File file = Utils.getFile(name);
        if (file.exists()) {
            if (staging.add(file)) staging.save();
            else System.out.println("File " + name + " is already tracked and staged for addition.");
        } else {
            System.out.println("The specified file doesn't exist, bimbo");
            System.exit(0);
        }
    }

    /** Creates a new Commit object and saves it to a file.
     * The file is stored in the COMMITS_DIR. */
    public void commit(String message, String secondParentId) {
        if (staging.isClear()) {
            System.out.println("No changes were added to the commit, dummy");
            System.exit(0);
        }

        // Creates new tracked map and parents list to be committed
        Map<String, String> tracked = staging.commit();
        staging.save();
        List<String> parents = new ArrayList<>();
        parents.add(getHeadId());

        // Adds second parent if there is one
        if (secondParentId != null) parents.add(secondParentId);

        // Saves the new staging area and adds the new commit object
        LocalDateTime time = LocalDateTime.now();
        String timestamp = time.format(formatObj);
        Commit c = new Commit(message, parents, tracked, timestamp);
        setHead(c.getId());
        Utils.updateActiveBranch(c);
        c.save();
    }

    /** Unstages the file if it is currently staged for addition. If the file is
     * tracked in the current commit, stage it for removal and removes it from tracking. */
    public void rm(String name) {
        // If the file does not exist, print a message.
        File file = Utils.getFile(name);
        if (!file.exists()) {
            System.out.println("File " + name + " does not exist in the current working directory.");
        }

        if(staging.remove(file)) {
            staging.save();
        } else {
            System.out.println("File " + name + " is already staged for removal.");
            System.exit(0);
        }
    }

    /** Prints the String stored in the log file. */
    public void log() {
        System.out.println(Utils.readContentsAsString(LOG));
    }

    public void branch(String name) {

    }

    public void checkoutCommit(String name) {

    }

    public void checkoutFile(String name) {

    }

    public void checkoutBranch(String name) {
        File f = Utils.join(Repository.BRANCHES_DIR, name);
        if (!f.exists()) {
            System.out.println("Branch name " + name + "does not exist.");
            System.exit(0)
        }

        Branch branch = Branch.getBranch(name);
        setHead(branch.getHead().getId());
    }

    public void rmbranch(String name) {
        File f = Utils.join(Repository.BRANCHES_DIR, name);
        if (!f.exists()) {
            System.out.println("A branch with that name does not exist.");
            System.exit(0)
        }

        if (name.equals(Utils.getActiveBranch())) {
            System.out.println("Cannot remove the current branch.");
            System.exit(0);
        }

        f.delete();
    }
}
