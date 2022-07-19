package gitlet;

import java.io.File;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.*;

import static gitlet.Utils.*;
import static gitlet.Utils.writeContents;

/** Creates and represents the gitlet repository.
 * Stores all functionality of commands listed in the Main class.
 */
public class Repository {

    /** The current working directory. */
    public static final File CWD = new File(System.getProperty("user.dir"));
    /** The .gitlet directory. */
    public static final File GITLET_DIR = join(CWD, ".gitlet");
    public static final File HEAD = join(GITLET_DIR, "head");
    public static final File LOG = join(GITLET_DIR, "log");
    public static final File STAGING_FILE = join(GITLET_DIR, "staging");
    public static final File BRANCHES_DIR = join(GITLET_DIR, "branches");
    public static final File ACTIVE_BRANCH = join(BRANCHES_DIR, "active branch");
    public static final File OBJECTS_DIR = join(GITLET_DIR, "objects");
    public static final File GLOBAL_LOG = join(GITLET_DIR, "global log");
    public static Staging staging = STAGING_FILE.exists() ? Staging.readStaging() : new Staging();
    /** Commits and Blobs in the Objects directory. */
    public static final File COMMITS_DIR = join(OBJECTS_DIR, "commits");
    public static final File BLOBS_DIR = join(OBJECTS_DIR, "blobs");
    /** Formatter for the timestamp passed to Commit objects. */
    DateFormat dateFormat = new SimpleDateFormat("EEE MMM d HH:mm:ss yyyy Z");

    /** Creates a new INITIAL Commit object and saves it to a file.
     * The file is stored in the COMMITS_DIR with a preset message. */
    public void init() {
        if (GITLET_DIR.exists()) System.out.println("A Gitlet version-control system already " +
                "exists in the current directory.");
        else {
            GITLET_DIR.mkdir();
            OBJECTS_DIR.mkdir();
            COMMITS_DIR.mkdir();
            BLOBS_DIR.mkdir();
            BRANCHES_DIR.mkdir();

            String timestamp = dateFormat.format(new Date(0));
            Commit initial = new Commit("initial commit", null, null, timestamp, 0);
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
    public void commit(String message, String secondParentId, boolean merge) {
        if (!merge) if (staging.isClear()) Utils.exit("No changes were added to the staging area.");


        // Creates new tracked map and parents list to be committed
        Map<String, String> tracked = staging.commit();
        staging.save();
        List<String> parents = new ArrayList<>();
        parents.add(getHeadId());
        int parentDepth = getHeadCommit().getDepth();

        // Adds second parent if there is one
        if (secondParentId != null) parents.add(secondParentId);

        // Saves the new staging area and adds the new commit object
        String timestamp = dateFormat.format(new Date());
        Commit c = new Commit(message, parents, tracked, timestamp, parentDepth + 1);
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

        writeContents(checkout, b.getContent());
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

        for (String commitName : Objects.requireNonNull(plainFilenamesIn(COMMITS_DIR),
                "Found no commit with that message.")) {
            Commit c = Commit.getCommit(commitName);
            if (c.getMessage().equals(message)) {
                log.append(c.getLog());
            }
        }

        if (log.length() == 0) System.out.println("Found no commit with that message.");
        else {
            log.delete(0, 1);
            System.out.println(log.toString());
        }
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
        Commit c = Commit.getCommit(id);
        File commitFile = join(COMMITS_DIR, id);
        if (!commitFile.exists()) Utils.exit("No commit with that id exists.");
        Utils.checkForUntracked(c);

        staging.clear();
        staging.save();
        c.restoreTrackedFiles();
        c.deleteUntrackedFiles();
        setHead(id);
    }

    public void merge(String branch) {
        // Failure cases:
        if (!staging.isClear()) Utils.exit("You have uncommitted changes.");
        if (!Utils.join(Repository.BRANCHES_DIR, branch).exists()) Utils.exit(
                "A branch with that name does not exist.");
        if (branch.equals(getActiveBranchName())) Utils.exit("Cannot merge a branch with itself.");

        Branch otherBranch = Branch.getBranch(branch);
        Commit head = getHeadCommit();
        Commit otherHead = otherBranch.getHead();
        Utils.checkForUntracked(otherHead);

        // Find split point:
        Map<String, Integer> commonAncestors = getCommonAncestorsDepths(otherHead, getAncestorsDepths(head));
        String splitId = latestCommonAncestor(commonAncestors);
        Commit splitCommit = Commit.getCommit(splitId);

        if (splitId.equals(otherHead.getId()))
            Utils.exit("Given branch is an ancestor of the current branch.");
        if (splitId.equals(getHeadId())) {
            Utils.exit("Current branch fast-forwarded.");
        }

        Map<String, List<String>> allBlobIds = Utils.allBlobIds(head, otherHead);
        Map<String, String> splitBlobs = splitCommit.getTracked();
        Map<String, String> headBlobs = head.getTracked();
        Map<String, String> otherBlobs = otherHead.getTracked();

        for (String filePath : allBlobIds.keySet()) {
            boolean inSplit = splitBlobs.containsKey(filePath);
            boolean inHead = headBlobs.containsKey(filePath);
            boolean inOther = otherBlobs.containsKey(filePath);
            boolean modifiedHead = inHead && !headBlobs.get(filePath).equals(splitBlobs.get(filePath));
            boolean modifiedOther = inOther && !otherBlobs.get(filePath).equals(splitBlobs.get(filePath));

            Blob headBlob = null;
            if (headBlobs.get(filePath) != null) headBlob = Blob.getBlob(headBlobs.get(filePath));
            Blob otherBlob = null;
            if (otherBlobs.get(filePath) != null) otherBlob = Blob.getBlob(otherBlobs.get(filePath));

            // 1. Modified in other branch but not in HEAD: Keep other. (Stage for addition)
            if (inSplit && modifiedOther && !inHead) {
                // System.out.println("case 1");
                writeContents(otherBlob.getSource(), otherBlob.getContent());
                add(new File(filePath).getName());
            }
            // 2. Modified in HEAD but not other branch: Keep HEAD.
            else if (inSplit && modifiedHead && !inOther) {
                // System.out.println("case 2");
                assert true;
            }
            // 3. Modified in other and HEAD:
            else if (modifiedHead && modifiedOther) {
                //      Both files are the same: No changes.
                if (headBlobs.get(filePath).equals(otherBlobs.get(filePath))) {
                    // System.out.println("case 3.1");
                    assert true;
                } else {
                    // Both files are the different: Merge conflict.
                    System.out.println("case 3.2");
                    System.out.println("Encountered a merge conflict.");
                    StringBuilder contents = new StringBuilder();
                    contents.append("<<<<<<< HEAD\n");
                    contents.append(readContentsAsString(headBlob.getSource()));
                    contents.append("=======\n");
                    contents.append(otherBlob.getContentString());
                    contents.append(">>>>>>>");
                    writeContents(headBlob.getSource(), contents.toString());
                    add(new File(filePath).getName());
                }
            }
            // 4. Not in split point or other branch, but exists in HEAD: keep HEAD.
            else if (!inSplit && !inOther && inHead) {
                // System.out.println("case 4");
                assert true;
            }
            // 5. Not in split point or HEAD, but exists in other: Keep other. (Stage for addition)
            else if (!inSplit && !inHead && inOther) {
                // System.out.println("case 5");
                writeContents(otherBlob.getSource(), otherBlob.getContent());
                add(new File(filePath).getName());
            }
            // 6. Unmodified in HEAD but not present in other: Remove file. (Stage for removal)
            else if (!modifiedHead && !inOther) {
                // System.out.println("case 6");
                rm(new File(filePath).getName());
            }
            // 7. Unmodified in other but not present in HEAD: Remain removed.
            else if (!modifiedOther && !inHead) {
                // System.out.println("case 7");
                assert true;
            }

            String message = "Merged " + branch + " into " + getActiveBranchName() + ".";
            commit(message, otherHead.getId(), true);
        }
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
