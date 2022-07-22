package gitlet;

import jdk.management.jfr.RemoteRecordingStream;

import java.io.File;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.*;

import static gitlet.Utils.*;

/** Creates and represents the gitlet repository.
 *  Stores all functionality of commands listed in the Main class.
 *  @author Jonathan Lu
 */
public class Repository {

    /** The current working directory. */
    public static final File CWD = new File(System.getProperty("user.dir"));
    /** The .gitlet directory. */
    public static final File GITLET_DIR = join(CWD, "gitlet-temp");
    public static final File HEAD = join(GITLET_DIR, "head");
    public static final File LOG = join(GITLET_DIR, "log");
    public static final File OBJECTS_DIR = join(GITLET_DIR, "objects");
    public static final File GLOBAL_LOG = join(GITLET_DIR, "global log");
    public static final File BRANCHES_DIR = join(GITLET_DIR, "branches");
    public static final File REMOTES_DIR = join(GITLET_DIR, "remotes");
    public static final File STAGING_FILE = join(GITLET_DIR, "staging");
    static Staging staging = STAGING_FILE.exists() ? Staging.readStaging() : new Staging();
    public static final File ACTIVE_BRANCH = join(BRANCHES_DIR, "active branch");
    /** Formatter for the timestamp passed to Commit objects. */
    DateFormat dateFormat = new SimpleDateFormat("EEE MMM d HH:mm:ss yyyy Z");

    /** Creates a new INITIAL Commit object and saves it to a file.
     * The file is stored in the OBJECTS_DIR with a preset message. */
    public void init() {
        if (GITLET_DIR.exists()) {
            System.out.println("A Gitlet version-control system already exists "
                    + "in the current directory.");
        } else {
            GITLET_DIR.mkdir();
            OBJECTS_DIR.mkdir();
            BRANCHES_DIR.mkdir();
            REMOTES_DIR.mkdir();

            String timestamp = dateFormat.format(new Date(0));
            Commit initial = new Commit("initial commit", null, null, timestamp,
                    0, "master");
            setHead(initial.getId());
            initial.save();

            Branch master = new Branch("master", initial, null);
            master.save();
            Utils.setActiveBranchName("master");
            Utils.buildGlobalLog(initial);
        }
    }

    /** Stages a file for addition. */
    public void add(String name) {
        File file = Utils.getFile(name);
        if (file.exists()) {
            staging.add(file);
        } else {
            Utils.exit("File does not exist.");
        }
    }

    /** Creates a new Commit object and saves it to a file.
     * The file is stored in the OBJECTS_DIR. */
    public void commit(String message, String secondParentId, boolean merge) {
        if (secondParentId != null) {
            overFiveCharacters(secondParentId);
        }

        if (!merge) {
            if (staging.isClear()) {
                Utils.exit("No changes added to the commit.");
            }
        }
        if (message.length() == 0) {
            Utils.exit("Please enter a commit message.");
        }
        // Creates new tracked map and parents list to be committed
        Map<String, String> tracked = staging.commit();
        staging.save();
        List<String> parents = new ArrayList<>();
        parents.add(getHeadId());
        int parentDepth = getHeadCommit().getDepth();

        // Adds second parent if there is one
        if (secondParentId != null) {
            parents.add(secondParentId);
        }

        // Saves the new staging area and adds the new commit object
        String timestamp = dateFormat.format(new Date());
        Commit c = new Commit(message, parents, tracked, timestamp, parentDepth + 1,
                getActiveBranchName());
        c.save();
        setHead(c.getId());
        updateActiveBranchHead(c);
        Utils.buildGlobalLog(c);
    }

    /** Unstages the file if it is currently staged for addition. If the file is
     * tracked in the current commit, stage it for removal and removes it from tracking. */
    public void rm(String name) {

        File file = Utils.getFile(name);
        String filePath = file.getPath();

        if (!file.exists()) {
            if (staging.getToRemove().contains(file.getPath())) {
                Utils.exit("File " + name + " is already staged for removal.");
            }
        }

        if (!staging.getToAdd().containsKey(filePath)
                && !staging.getTracked().containsKey(filePath)) {
            Utils.exit("No reason to remove the file.");
        }

        staging.remove(file);
    }

    /** Starting at the head commit, displays information about each commit backwards
     * until the initial commit, following fist parents only, ignoring merges. */
    public void log() {
        Utils.buildLog();
        System.out.println(Utils.readContentsAsString(LOG));
    }

    /** Creates a new branch with the given name, and points it at the current head commit.
     * A branch is nothing more than a name for a reference (a SHA-1 identifier) to a
     * commit node. This command does NOT immediately switch to the newly created branch */
    public void branch(String name) {
        Branch b = new Branch(name, Commit.getCommit(readContentsAsString(HEAD)), null);
        if (b.getBranchFile().exists()) {
            Utils.exit("A branch with that name already exists.");
        }
        b.save();
    }

    /** Takes all files in the commit at the head of the given branch, and puts them
     * in the working directory, overwriting the versions of the files that are already
     * there if they exist. The given branch is set as the active branch. */
    public void checkoutBranch(String name) {
        File branchFile = join(Repository.BRANCHES_DIR, name);
        if (!branchFile.exists()) {
            Utils.exit("No such branch exists.");
        }

        if (name.equals(Utils.getActiveBranchName())) {
            Utils.exit("No need to checkout the current branch.");
        }

        Commit branchCommit = Objects.requireNonNull(readObject(branchFile, Branch.class),
                "No such branch exists.").getHead();
        Utils.checkForUntracked(branchCommit);
        checkoutProcesses(branchCommit, staging);
        Utils.setActiveBranchName(name);
    }

    /** Takes the version of the file as it exists in the commit with the given id,
     * and puts it in the working directory, overwriting the version of the file that’s
     * already there if there is one. The new version of the file is not staged. */
    public static void checkoutCommit(String commitId, String name) {
        overFiveCharacters(commitId);
        File checkout = join(CWD, name);
        Commit c = Commit.getCommit(commitId);

        if (c == null) {
            Utils.exit("No commit with that id exists.");
        }
        assert c != null;
        if (!c.getTrackedNames().contains(name)) {
            Utils.exit("File does not exist in that commit.");
        }

        Blob b = Blob.getBlob(c.getTracked().get(getFile(name).getPath()));

        Utils.checkForUntracked(Objects.requireNonNull(Commit.getCommit(commitId)));
        assert b != null;
        writeContents(checkout, (Object) b.getContent());
    }

    /** Takes the version of the file as it exists in the head commit and puts it
     * in the working directory, overwriting the version of the file that’s already
     * there if there is one. The new version of the file is not staged. */
    public void checkoutFile(String name) {
        File checkout = join(CWD, name);

        Commit c = Commit.getCommit(readContentsAsString(Repository.HEAD));
        assert c != null;
        if (!c.getTrackedNames().contains(name)) {
            Utils.exit("File does not exist in that commit.");
        }
        Blob b = Blob.getBlob(c.getTracked().get(getFile(name).getPath()));
        assert b != null;
        writeContents(checkout, (Object) b.getContent());
    }

    /** Deletes the branch with the given name. */
    public void rmbranch(String name) {
        File f = Utils.join(Repository.BRANCHES_DIR, name);
        if (!f.exists()) {
            Utils.exit("A branch with that name does not exist.");
        }
        if (name.equals(Utils.getActiveBranchName())) {
            Utils.exit("Cannot remove the current branch.");
        }
        f.delete();
    }

    /** Displays information about all commits ever made in chronological order. */
    public void globalLog() {
        System.out.println(readContentsAsString(GLOBAL_LOG));
    }

    /** Prints out the ids of all commits that have the given commit message, one per line. */
    public void find(String message) {
        StringBuilder log = new StringBuilder();
        List<String> directoryNames = directoriesIn(OBJECTS_DIR);

        assert directoryNames != null;
        for (String directoryName : directoryNames) {
            File directory = join(OBJECTS_DIR, directoryName);
            for (String commitName : Objects.requireNonNull(plainFilenamesIn(directory),
                    "No objects exist in this repository.")) {
                Commit c = Commit.getCommit(directory.getName() + commitName);
                if (c != null && c.getMessage().equals(message)) {
                    log.append("\n").append(c.getId());
                }
            }
        }

        if (log.length() == 0) {
            System.out.println("Found no commit with that message.");
        } else {
            log.delete(0, 1);
            System.out.println(log.toString());
        }
    }

    /** Displays what branches currently exist, and marks the current branch with *.
     * Also displays what files have been staged for addition or removal. */
    public void status() {
        StringBuilder status = new StringBuilder();

        status.append("=== Branches ===\n");
        for (String branchName : Objects.requireNonNull(plainFilenamesIn(BRANCHES_DIR))) {
            if (branchName.equals("active branch")) {
                continue;
            }
            if (branchName.equals(Utils.getActiveBranchName())) {
                status.append("*");
            }
            status.append(branchName).append("\n");
        }
        status.append("\n=== Staged Files ===\n");
        for (String filePath : staging.getToAdd().keySet()) {
            status.append(new File(filePath).getName()).append("\n");
        }
        status.append("\n=== Removed Files ===\n");
        for (String filePath : staging.getToRemove()) {
            status.append(new File(filePath).getName()).append("\n");
        }
        status.append("\n=== Modifications Not Staged For Commit ===\n");
        status.append("\n=== Untracked Files ===\n");
        System.out.println(status);
    }

    /** Checks out all files tracked by the given commit, removes files not present in the commit,
     * and moves the current branch head to the commit node. */
    public void reset(String commitId) {
        overFiveCharacters(commitId);
        Commit resetCommit = Commit.getCommit(commitId);
        if (resetCommit == null) {
            Utils.exit("No commit with that id exists.");
        }
        assert resetCommit != null;
        Utils.checkForUntracked(resetCommit);
        checkoutProcesses(resetCommit, staging);
        Branch b = new Branch(resetCommit.getBranch(), resetCommit, null);
        b.save();
    }

    /** Merges the given branch with the current one.
     * Automatically commits the merge after handling cases for staging. */
    public void merge(String branch) {
        // Failure cases:
        if (!staging.isClear()) {
            Utils.exit("You have uncommitted changes.");
        } else if (!Utils.join(Repository.BRANCHES_DIR, branch).exists()) {
            Utils.exit("A branch with that name does not exist.");
        } else if (branch.equals(getActiveBranchName())) {
            Utils.exit("Cannot merge a branch with itself.");
        }
        Branch otherBranch = Branch.getBranch(branch);
        Commit head = getHeadCommit();
        Commit otherHead = otherBranch.getHead();
        Utils.checkForUntracked(otherHead);

        // Find split point:
        Map<String, Integer> commonAncestors =
                getCommonAncestorsDepths(otherHead, getAncestorsDepths(head));
        String splitId = latestCommonAncestor(commonAncestors);
        Commit splitCommit = Commit.getCommit(splitId);

        if (splitId.equals(otherHead.getId())) {
            Utils.exit("Given branch is an ancestor of the current branch.");
        } else if (splitId.equals(getHeadId())) {
            checkoutBranch(branch);
            Utils.exit("Current branch fast-forwarded.");
        }

        Map<String, List<String>> allBlobIds = Utils.allBlobIds(head, otherHead);
        assert splitCommit != null;
        Map<String, String> splitBlobs = splitCommit.getTracked();
        Map<String, String> headBlobs = head.getTracked();
        Map<String, String> otherBlobs = otherHead.getTracked();
        for (String filePath : allBlobIds.keySet()) {
            Blob headBlob = null;
            if (headBlobs.get(filePath) != null) {
                headBlob = Blob.getBlob(headBlobs.get(filePath));
            }
            Blob otherBlob = null;
            if (otherBlobs.get(filePath) != null) {
                otherBlob = Blob.getBlob(otherBlobs.get(filePath));
            }
            boolean inSplit = splitBlobs.containsKey(filePath);
            boolean inHead = headBlobs.containsKey(filePath);
            boolean inOther = otherBlobs.containsKey(filePath);
            boolean modifiedHead = inHead
                    && !headBlobs.get(filePath).equals(splitBlobs.get(filePath));
            boolean modifiedOther = inOther
                    && !otherBlobs.get(filePath).equals(splitBlobs.get(filePath));

            if (inSplit) {
                // 1. Modified in HEAD but not modified in other: Keep HEAD. (Do nothing)
                // 2. Modified in other but not modified in HEAD: Stage for addition.
                if (modifiedOther && !modifiedHead) {
                    assert otherBlob != null;
                    writeContents(otherBlob.getSource(), (Object) otherBlob.getContent());
                    add(new File(filePath).getName());
                } else if (modifiedHead && modifiedOther) {
                    // 3.1. Modified in other and HEAD, files are the same: keep file. (Do nothing)
                    // 3.2. MERGE CONFLICT: Modified in other and HEAD, files are different.
                    if (!headBlobs.get(filePath).equals(otherBlobs.get(filePath))) {
                        mergeConflict(filePath, headBlob, otherBlob);
                    }
                } else if (modifiedHead && !inOther || modifiedOther && !inHead) {
                    // 3.3. MERGE CONFLICT: Modified in other and deleted from other.
                    // 3.4. MERGE CONFLICT: Modified in head and deleted from other.
                    mergeConflict(filePath, headBlob, otherBlob);
                } else if (!modifiedHead && !inOther) {
                    // 4. Unmodified in HEAD but deleted from other: Stage for removal.
                    rm(new File(filePath).getName());
                } // 5. Unmodified in other but deleted from HEAD: Remain removed. (Do nothing)
            } else {
                // 6. Not in split point or other branch, but in HEAD: keep HEAD. (Do nothing)
                // 7. Not in split point or HEAD, but in other: Stage for addition.
                if (!inHead && inOther) {
                    assert otherBlob != null;
                    writeContents(otherBlob.getSource(), (Object) otherBlob.getContent());
                    add(new File(filePath).getName());
                }
            }
        }
        String message = "Merged " + branch + " into " + getActiveBranchName() + ".";
        commit(message, otherHead.getId(), true);
    }

    public void addRemote(String remoteName, String directoryName) {
        File remoteDir = join(Repository.REMOTES_DIR, remoteName);
        if (!remoteDir.exists()) {
            Utils.exit("A remote with that name already exists.");
        }
        remoteDir.mkdir();

        for (String branchName : plainFilenamesIn(BRANCHES_DIR)) {
            Branch localBranch = Branch.getBranch(branchName);
            Branch remoteBranch = new Branch(localBranch.getName(), localBranch.getHead(),
                    join(REMOTES_DIR, branchName));
            remoteBranch.save();
        }

//        Map<String, Branch> branches = new HashMap<>();
//        for (String branchName : plainFilenamesIn(BRANCHES_DIR)) {
//            Branch localBranch = Branch.getBranch(branchName);
//            Branch remoteBranch = new Branch(localBranch.getName(), localBranch.getHead(),
//                    join(REMOTES_DIR, branchName));
//            branches.put(branchName, remoteBranch);
//        }
//        Map<String, Commit> commits = new HashMap<>();
//        Map<String, Blob> blobs = new HashMap<>();
//        for (String dirName : directoriesIn(OBJECTS_DIR)) {
//            File dir = join(OBJECTS_DIR, dirName);
//            for (String fileName : plainFilenamesIn(dir)) {
//                File file = join(dir, fileName);
//                if (Commit.getCommit(file.getName()) != null) {
//                    Commit c = readObject(file, Commit.class);
//                    commits.put(c.getId(), c);
//                } else {
//                    Blob b = readObject(file, Blob.class);
//                    blobs.put(b.getId(), b);
//                }
//            }
//        }
//
//        Remote r = new Remote(branches, commits, blobs);
    }

    public void rmRemote(String remoteName) {
        File f = join(REMOTES_DIR, remoteName);
        f.delete();
    }

    public void fetch(String remoteName, String branchName) {
        // Create branch remoteName/branchName
        // Get all commits -> copy ones not in the current repository to new branch (?)
        // Get all blobs -> copy ones not in current repository to new branch (?)
        // Have this branch point to the current head commit
    }

    public void push(String remoteName, String branchName) {
        // Works if the remote branch’s head is in the history of the current local head
        // Append additional commits of the current branch to the remote branch
        // Set remote branch head to the current branch head (Fast-forwarding)
    }

    public void pull(String remoteName, String branchName) {
        // Fetches the given branch from the remote directory
        // Merges the branch into the current one
    }


    /* OTHER HELPER METHODS */

    /** Checks if the initial Gitlet directory does not exist. Prints an error message. */
    public void exists() {
        if (!GITLET_DIR.exists()) {
            Utils.exit("Not in an initialized Gitlet directory.");
        }
    }

    /** Handles file overwriting in the case of a merge conflict. */
    public void mergeConflict(String filePath, Blob headBlob, Blob otherBlob) {
        System.out.println("Encountered a merge conflict.");
        StringBuilder contents = new StringBuilder();
        contents.append("<<<<<<< HEAD\n");
        if (headBlob != null) {
            contents.append(readContentsAsString(headBlob.getSource()));
        }
        contents.append("=======\n");
        if (otherBlob != null) {
            contents.append(otherBlob.getContentString());
        }
        contents.append(">>>>>>>\n");
        assert headBlob != null;
        writeContents(headBlob.getSource(), contents.toString());
        add(new File(filePath).getName());
    }
}
