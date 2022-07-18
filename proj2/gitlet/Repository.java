package gitlet;

import java.io.File;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;

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
    public static final DateTimeFormatter formatObj = DateTimeFormatter.ofPattern("EEEE, dd MMMM yyyy, HH:mm:ss");
    public static final File GLOBAL_LOG = join(GITLET_DIR, "global log");

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
            Utils.buildGlobalLog(initial);
        }
    }

    /** Stages a file for addition. */
    public void add(String name) {
        File file = Utils.getFile(name);
        if (file.exists()) {
            if (staging.add(file)) staging.save();
            else System.out.println("File " + name + " is already tracked and staged for addition.");
        } else Utils.exit("File does not exist.");
    }

    /** Creates a new Commit object and saves it to a file.
     * The file is stored in the COMMITS_DIR. */
    public void commit(String message, String secondParentId) {
        if (staging.isClear()) Utils.exit("No changes were added to the staging area.");

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
        c.save();
        setHead(c.getId());
        updateActiveBranchHead(c);
        Utils.buildGlobalLog(c);
    }

    /** Unstages the file if it is currently staged for addition. If the file is
     * tracked in the current commit, stage it for removal and removes it from tracking. */
    public void rm(String name) {
        // If the file does not exist, print a message.
        File file = Utils.getFile(name);
        if (!file.exists()) {
            if (staging.getToRemove().contains(file.getPath()))
                Utils.exit("File " + name + " is already staged for removal.");
            Utils.exit("File " + name + " does not exist in the current working directory.");
        }

        if (!staging.isTrackingFile(file)) Utils.exit("File " + name + " is untracked in the working directory.");

        if(staging.remove(file)) {
            staging.save();
            Utils.restrictedDelete(file);
        }
    }

    /** Prints the String stored in the log file. */
    public void log() {
        Utils.buildLog();
        System.out.println(Utils.readContentsAsString(LOG));
    }

    public void branch(String name) {
        Branch b = new Branch(name, Commit.getCommit(readContentsAsString(HEAD)));
        if (b.getBranchFile().exists()) Utils.exit("A branch with that name already exists.");

        b.save();
    }

    public void checkoutBranch(String name) {
        File branchFile = join(Repository.BRANCHES_DIR, name);
        if (!branchFile.exists()) Utils.exit("No such branch exists.");

        if (name.equals(Utils.getActiveBranchName())) Utils.exit("No need to checkout the current branch.");
        staging.setTracked(Branch.getBranch(name).getHead().getTracked());
        staging.clear();
        staging.save();

        Commit branchCommit = readObject(branchFile, Branch.class).getHead();
        Utils.checkForUntracked(branchCommit);

        branchCommit.restoreTrackedFiles();
        branchCommit.deleteUntrackedFiles();

        setHead(readObject(branchFile, Branch.class).getHead().getId());
        Utils.setActiveBranchName(name);
    }

    public void checkoutCommit(String commitId, String name) {
        File checkout = join(CWD, name);

        File commitFile = join(COMMITS_DIR, commitId);
        if (!commitFile.exists()) Utils.exit("No commit with that id exists.");

        Commit c = readObject(commitFile, Commit.class);
        if (!c.getTrackedNames().contains(name)) Utils.exit("File does not exist in that commit.");
        Blob b = Blob.getBlob(c.getTracked().get(getFile(name).getPath()));

        Utils.checkForUntracked(Commit.getCommit(commitId));
        writeContents(checkout, b.getContent());
    }

    public void checkoutFile(String name) {
        File checkout = join(CWD, name);

        Commit c = Commit.getCommit(readContentsAsString(Repository.HEAD));
        if (!c.getTrackedNames().contains(name)) Utils.exit("File does not exist in that commit.");
        Blob b = Blob.getBlob(c.getTracked().get(getFile(name).getPath()));

        writeContents(checkout, (Object) b.getContent());
    }

    public void rmbranch(String name) {
        File f = Utils.join(Repository.BRANCHES_DIR, name);
        if (!f.exists()) Utils.exit("A branch with that name does not exist.");
        if (name.equals(Utils.getActiveBranchName())) Utils.exit("Cannot remove the current branch.");

        restrictedDelete(f);
    }

    public void globalLog() {
        System.out.println(readContentsAsString(GLOBAL_LOG));
    }

    public void find(String message) {
        StringBuilder log = new StringBuilder();

        for (String c : Objects.requireNonNull(plainFilenamesIn(COMMITS_DIR),
                "Found no commit with that message.")) {
            Commit commit = Commit.getCommit(c);
            if (commit.getMessage().equals(message)) {
                log.append(commit.getLog());
            }
        }

        if (log.length() == 0) System.out.println("Found no commit with that message.");
        else System.out.println(log.toString());
    }

    public void status() {
        StringBuilder status = new StringBuilder();

        status.append("=== Branches ===\n");
        for (String branchName : Objects.requireNonNull(plainFilenamesIn(BRANCHES_DIR))) {
            if (branchName.equals("active branch")) continue;
            if (branchName.equals(Utils.getActiveBranchName())) status.append("*");
            status.append(branchName).append("\n");
        }
        status.append("\n=== Staged Files ===\n");
        for (String filePath : staging.getToAdd().keySet())
            status.append(new File(filePath).getName()).append("\n");
        status.append("\n=== Removed Files ===\n");
        for (String filePath : staging.getToRemove())
            status.append(new File(filePath).getName()).append("\n");
        status.append("\n=== Modifications Not Staged For Commit ===\n");
        status.append("\n=== Untracked Files ===\n");

        System.out.println(status);
    }

    public void reset(String id) {
        Commit commit = Commit.getCommit(id);
        File commitFile = join(COMMITS_DIR, id);
        if (!commitFile.exists()) Utils.exit("No commit with that id exists.");
        Utils.checkForUntracked(commit);

        staging.clear();
        staging.save();
        commit.restoreTrackedFiles();
        commit.deleteUntrackedFiles();
        setHead(id);
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

    public void readBlob(String blobId) {
        System.out.println(Utils.readContentsAsString(join(BLOBS_DIR, blobId)));
    }
}
