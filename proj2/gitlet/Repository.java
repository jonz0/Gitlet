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
    public static final File BRANCHES_DIR = join(GITLET_DIR, "branches");
    public static final File ACTIVE_BRANCH = join(BRANCHES_DIR, "active branch");
    public static final File OBJECTS_DIR = join(GITLET_DIR, "objects");
    public static Staging staging = STAGING_FILE.exists() ? Staging.readStaging() : new Staging();
    /** Commit and Blob objects */
    public static final File COMMITS_DIR = join(OBJECTS_DIR, "commits");
    public static final File BLOBS_DIR = join(OBJECTS_DIR, "blobs");
    /** Formatter for the timestamp passed to Commit objects. */
    private final DateTimeFormatter formatObj = DateTimeFormatter.ofPattern("EEEE, dd MMMM yyyy, HH:mm:ss");

    /** Creates a new INITIAL Commit object and saves it to a file.
     * The file is stored in the COMMITS_DIR with a preset message. */
    public void init() {
        if (GITLET_DIR.exists()) System.out.println("Gitlet version control already exists in this directory, fool");
        else {
            GITLET_DIR.mkdir();
            OBJECTS_DIR.mkdir();
            COMMITS_DIR.mkdir();
            BLOBS_DIR.mkdir();
            BRANCHES_DIR.mkdir();

            LocalDateTime time = LocalDateTime.of(1970, 1,
                    1, 0, 0, 0);
            String timestamp = time.format(formatObj);
            Commit initial = new Commit("initial commit", null, null, timestamp);
            setHead(initial.getId());
            initial.save();
            Branch master = new Branch("master", initial);
            master.save();

            Utils.setActiveBranchName("master");
        }
    }

    /** Stages a file for addition. */
    public void add(String name) {
        File file = Utils.getFile(name);
        if (file.exists()) {
            if (staging.add(file)) staging.save();
            else System.out.println("File " + name + " is already tracked and staged for addition.");
        } else {
            System.out.println("File does not exist.");
            System.exit(0);
        }
    }

    /** Creates a new Commit object and saves it to a file.
     * The file is stored in the COMMITS_DIR. */
    public void commit(String message, String secondParentId) {

        if (staging.isClear()) {
            System.out.println("No changes were added to the staging area.");
            System.exit(0);
        }
        // Creates new tracked map and parents list to be committed
        Map<String, Blob> tracked = staging.commit();
        staging.save();
        List<String> parents = new ArrayList<>();
        parents.add(getHeadId());

        // Adds second parent if there is one
        if (secondParentId != null) parents.add(secondParentId);

        // Saves the new staging area and adds the new commit object
        LocalDateTime time = LocalDateTime.now();
        String timestamp = time.format(formatObj);
        Commit c = new Commit(message, parents, tracked, timestamp);
        c.save();
        setHead(c.getId());
        updateActiveBranchHead(c);
    }

    /** Unstages the file if it is currently staged for addition. If the file is
     * tracked in the current commit, stage it for removal and removes it from tracking. */
    public void rm(String name) {
        // If the file does not exist, print a message.
        File file = Utils.getFile(name);
        if (!file.exists()) {
            if (staging.getToRemove().contains(file.getPath())) {
                System.out.println("File " + name + " is already staged for removal.");
                System.exit(0);
            }
            System.out.println("File " + name + " does not exist in the current working directory.");
            System.exit(0);
        }

        if (!staging.isTrackingFile(file)) {
            System.out.println("File " + name + " is untracked in the working directory.");
            System.exit(0);
        }

        if(staging.remove(file)) {
            staging.save();
            Utils.restrictedDelete(file);
        }
    }

    /** Prints the String stored in the log file. */
    public void log() {
        System.out.println(Utils.readContentsAsString(LOG));
    }

    public void branch(String name) {
        Branch b = new Branch(name, Commit.getCommit(readContentsAsString(HEAD)));
        if (b.getBranchFile().exists()) {
            System.out.println("A branch with that name already exists.");
            System.exit(0);
        }

        b.save();
    }

    public void checkoutBranch(String name) {
        // point active branch to branch name
        // point head to the commit stored in branch
        // add and delete CWD files as necessary

        File branchFile = join(Repository.BRANCHES_DIR, name);
        if (!branchFile.exists()) {
            System.out.println("A branch with that name does not exist.");
            System.exit(0);
        }

        if (name.equals(Utils.getActiveBranchName())) {
            System.out.println("No need to checkout the current branch.");
            System.exit(0);
        }

        staging.setTracked(Branch.getBranch(name).getHead().getTracked());
        staging.clear();
        staging.save();

        Commit branchCommit = readObject(branchFile, Branch.class).getHead();
        Commit prevHead = Commit.getCommit(readContentsAsString(Repository.HEAD));
        branchCommit.restoreTrackedFiles();

        for (String filePath : prevHead.getTracked().keySet()) {
            if (!branchCommit.getTracked().containsKey(filePath)) {
                String fileName = new File(filePath).getName();
                File f = join(CWD, fileName);
                restrictedDelete(f);
            }
        }

        setHead(readObject(branchFile, Branch.class).getHead().getId());
        Utils.setActiveBranchName(name);
    }

    public void checkoutCommit(String commitId, String name) {
        File checkout = join(CWD, name);

        File commitFile = join(COMMITS_DIR, commitId);
        if (!commitFile.exists()) {
            System.out.println("No version of commit " + commitId + " exists in the repository.");
            System.exit(0);
        }
        Commit c = readObject(commitFile, Commit.class);
        Blob b = c.getTracked().get(getFile(name).getPath());
        if (b == null) {
            System.out.println("No version of file " + name + " is being tracked in the commit.");
            System.exit(0);
        }

        writeContents(checkout, b.getContent());
    }

    public void checkoutFile(String name) {
        File checkout = join(CWD, name);

        Commit c = Commit.getCommit(readContentsAsString(Repository.HEAD));
        Blob b = c.getTracked().get(getFile(name).getPath());
        if (b == null) {
            System.out.println("No version of file " + name + " is being tracked in the head.");
            System.exit(0);
        }

        writeContents(checkout, (Object) b.getContent());
    }

    public void rmbranch(String name) {
        File f = Utils.join(Repository.BRANCHES_DIR, name);
        if (!f.exists()) {
            System.out.println("A branch with that name does not exist.");
            System.exit(0);
        }

        if (name.equals(Utils.getActiveBranchName())) {
            System.out.println("Cannot remove the current branch.");
            System.exit(0);
        }

        restrictedDelete(f);
    }

    /** Debugging purposes only */

    public void printTrackedInHead() {
        Commit c = Commit.getCommit(readContentsAsString(HEAD));
        for (String filePath : c.getTracked().keySet()) {
            System.out.println(new File(filePath).getName());
        }
    }

    public void printCurrentBranch() {
        System.out.println(getActiveBranchName());
    }
}
