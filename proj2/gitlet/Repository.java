package gitlet;

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
    public static final File GITLET_DIR = join(CWD, ".gitlet");
    public static final File HEAD = join(GITLET_DIR, "head");
    public static final File LOG = join(GITLET_DIR, "log");
    public static final File OBJECTS_DIR = join(GITLET_DIR, "objects");
    public static final File GLOBAL_LOG = join(GITLET_DIR, "global log");
    public static final File BRANCHES_DIR = join(GITLET_DIR, "branches");
    public static final File STAGING_FILE = join(GITLET_DIR, "staging");
    static Staging staging = STAGING_FILE.exists() ? Staging.readStaging() : new Staging();
    public static final File ACTIVE_BRANCH = join(BRANCHES_DIR, "active branch");
    /** Formatter for the timestamp passed to Commit objects. */
    DateFormat dateFormat = new SimpleDateFormat("EEE MMM d HH:mm:ss yyyy Z");

    public static final File REMOTES_DIR = join(GITLET_DIR, "remotes");

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
            initial.save(null);

            Branch master = new Branch("master", initial);
            master.save(null);
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
        c.save(null);
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
        Branch b = new Branch(name, Commit.getCommit(readContentsAsString(HEAD), null));

        if (join(Repository.BRANCHES_DIR, name).exists()) {
            Utils.exit("A branch with that name already exists.");
        }
        b.save(null);
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
        Commit c = Commit.getCommit(commitId, null);

        if (c == null) {
            Utils.exit("No commit with that id exists.");
        }
        assert c != null;
        if (!c.getTrackedNames().contains(name)) {
            Utils.exit("File does not exist in that commit.");
        }

        Blob b = Blob.getBlob(c.getTracked().get(getFile(name).getPath()), null);

        Utils.checkForUntracked(Objects.requireNonNull(Commit.getCommit(commitId, null)));
        assert b != null;
        writeContents(checkout, (Object) b.getContent());
    }

    /** Takes the version of the file as it exists in the head commit and puts it
     * in the working directory, overwriting the version of the file that’s already
     * there if there is one. The new version of the file is not staged. */
    public void checkoutFile(String name) {
        File checkout = join(CWD, name);

        Commit c = Commit.getCommit(readContentsAsString(Repository.HEAD), null);
        assert c != null;
        if (!c.getTrackedNames().contains(name)) {
            Utils.exit("File does not exist in that commit.");
        }
        Blob b = Blob.getBlob(c.getTracked().get(getFile(name).getPath()), null);
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
                Commit c = Commit.getCommit(directory.getName() + commitName, null);
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
        List<String> cwdFiles = plainFilenamesIn(CWD);
        for (String filePath : staging.getTracked().keySet()) {
            File f = new File(filePath);
            String fileName = f.getName();
            if (!cwdFiles.contains(fileName)) {
                if (!staging.getToRemove().contains(filePath)) {
                    status.append(fileName).append(" ").append("(deleted)\n");
                }
                break;
            }
            Blob tempBlob = new Blob(f);
            if (!tempBlob.getId().equals(Blob.getBlob(staging.getTracked().get(filePath), null).getId())) {
                status.append(fileName).append(" ").append("(modified)\n");
            }
        }
        // for each tracked file in cwd, create a blob.
        // if blob id is not the same as blob associated with the file, then file (modified)

        // for each file in tracked, check if plain files contains the tracked file
        // if not, (deleted)
        status.append("\n=== Untracked Files ===\n");
        // get plain files in cwd. if file is not in tracked, add here.
        for (String file : cwdFiles) {
            String filePath = getFile(file).getPath();
            if (!staging.getTracked().keySet().contains(filePath)) {
                status.append(getFile(file).getName()).append("\n");
            }
        }

        System.out.println(status);
    }

    /** Checks out all files tracked by the given commit, removes files not present in the commit,
     * and moves the current branch head to the commit node. */
    public void reset(String commitId) {
        overFiveCharacters(commitId);
        Commit resetCommit = Commit.getCommit(commitId, null);
        if (resetCommit == null) {
            Utils.exit("No commit with that id exists.");
        }
        assert resetCommit != null;
        Utils.checkForUntracked(resetCommit);
        checkoutProcesses(resetCommit, staging);
        Branch b = new Branch(resetCommit.getBranch(), resetCommit);
        b.save(null);
        // setActiveBranchName(resetCommit.getBranch());
        setHead(resetCommit.getId());
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
        Branch otherBranch = Utils.getBranch(branch, null);
        Commit head = getHeadCommit();
        Commit otherHead = otherBranch.getHead();
        Utils.checkForUntracked(otherHead);

        // Find split point:
        Map<String, Integer> commonAncestors =
                getCommonAncestorsDepths(otherHead, getAncestorsDepths(head));
        String splitId = latestCommonAncestor(commonAncestors);
        Commit splitCommit = Commit.getCommit(splitId, null);

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
                headBlob = Blob.getBlob(headBlobs.get(filePath), null);
            }
            Blob otherBlob = null;
            if (otherBlobs.get(filePath) != null) {
                otherBlob = Blob.getBlob(otherBlobs.get(filePath), null);
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

    public void addRemote(String remoteName, String filePath) {
        File remoteFile = join(Repository.REMOTES_DIR, remoteName);
        if (remoteFile.exists()) {
            Utils.exit("A remote with that name already exists.");
        }
        filePath.replace('/', File.separatorChar);
        writeContents(remoteFile, filePath);
    }

    public void rmRemote(String remoteName) {
        File remoteFile = join(REMOTES_DIR, remoteName);
        if (remoteFile.exists()) {
            remoteFile.delete();
        } else {
            Utils.exit("A remote with that name does not exist.");
        }
    }

    public void fetch(String remoteName, String branchName) {
        // Error handling:
        File remoteFile = join(REMOTES_DIR, remoteName);
        if (!remoteFile.exists()) {
            Utils.exit("Remote directory not found.");
        }
        File remotePath = new File(readContentsAsString(remoteFile));

        if (!remotePath.exists()) {
            Utils.exit("Remote directory not found.");
        }
        File remoteBranches = Utils.join(remotePath, "branches");
        if (!plainFilenamesIn(remoteBranches).contains(branchName)) {
            Utils.exit("That remote does not have that branch.");
        }

        // Copy over the commits and blobs:
        String name = remoteName + "/" + branchName;
        Branch remoteBranch = Utils.getBranch(branchName, remotePath);


        Set<Commit> remoteCommits = Utils.getAllCommits(remoteBranch.getHead(), remotePath);

        for (Commit c : remoteCommits) {
            Map<String, String> newTracked = new HashMap<>();
            for (String filePath : c.getTracked().keySet()) {
                String newFilePath = filePath.replace(new File(filePath).getParent(), GITLET_DIR.getParent());
                newTracked.put(newFilePath, c.getTracked().get(filePath));
            }
            List<String> newParents = c.getParents();
            Commit localCommit = new Commit(c.getMessage(), newParents, newTracked,
                    c.getTimestamp(), c.getDepth(), name);
            localCommit.setId(c.getId());
            localCommit.save(null);
        }

        Set<Blob> remoteBlobs = Utils.getAllBlobs(remoteCommits, remotePath);
        for (Blob b : remoteBlobs) {
            if (Blob.getBlob(b.getId(), null) == null){
                String oldSourcePath = b.getSource().getParent();
                String newPath = b.getSource().getPath().replace(oldSourcePath, GITLET_DIR.getParent());
                Blob localBlob = new Blob(new File(newPath));
                localBlob.setContent(b.getContent());
                localBlob.setContentString(b.getContentString());
                localBlob.setId(b.getId());
                localBlob.save(null);
            }
        }

        String remoteId = remoteBranch.getHead().getId();
        Commit localBranchHead = Commit.getCommit(remoteId, null);
        Branch br = new Branch(name, localBranchHead);
        br.save(null);

        // IF the current branch is the branch that was fetched update HEAD.
        if (getActiveBranch().equals(name)) {
            writeContents(HEAD, remoteBranch.getHead().getId());
        }
    }

    public void push(String remoteName, String branchName) {
        // Works if the remote branch’s head is in the history of the current local head
        // Append additional commits of the current branch to the remote branch
        // Set remote branch head to the current branch head (Fast-forwarding)
        File remoteFile = join(REMOTES_DIR, remoteName);
        if (!remoteFile.exists()) {
            Utils.exit("Remote directory not found.");
        }
        File remotePath = new File(readContentsAsString(remoteFile));
        if (!remotePath.exists()) {
            Utils.exit("Remote directory not found.");
        }

        Set<String> currentCommitIds = getAllCommitIds(getHeadCommit(), null);
        Branch remoteBranch = Utils.getBranch(branchName, remotePath);
        if (!currentCommitIds.contains(remoteBranch.getHead().getId())) {
            Utils.exit("Please pull down remote changes before pushing.");
        }

        Set<String> remoteCommitIds = getAllCommitIds(remoteBranch.getHead(), remotePath);
        Set<String> localCommitIds = getAllCommitIds(getHeadCommit(), null);

        // Copies commits and blobs to the remote repository if it is not already there.
        for (String commitId : localCommitIds) {
            if (!remoteCommitIds.contains(commitId)) {
                Commit localCommit = Commit.getCommit(commitId, null);

                /* When copying over commits, changes file paths of tracked blobs to point to
                * the remote directory, and not the local one. */
                Map<String, String> newTracked = new HashMap<>();
                for (String filePath : localCommit.getTracked().keySet()) {
                    String newFilePath = filePath.replace(GITLET_DIR.getParent(), remoteBranch.getHead().getRepoDir().getPath());
                    newTracked.put(newFilePath, localCommit.getTracked().get(filePath));

                    for (String blobPath : localCommit.getTracked().keySet()) {
                        String blobId = localCommit.getTracked().get(blobPath);
                        Blob localBlob = Blob.getBlob(blobId, null);

                        if (Blob.getBlob(blobId, remotePath) == null){
                            String oldPath = localBlob.getSource().getPath();
                            String newPath = oldPath.replace(GITLET_DIR.getParent(), remoteBranch.getHead().getRepoDir().getPath());
                            Blob remoteBlob = new Blob(new File(newPath));
                            remoteBlob.setContent(localBlob.getContent());
                            remoteBlob.setContentString(localBlob.getContentString());
                            remoteBlob.setId(localBlob.getId());
                            remoteBlob.save(null);
                        }
                    }

                    Commit remoteCommit = new Commit(localCommit.getMessage(), localCommit.getParents(), newTracked,
                            localCommit.getTimestamp(), localCommit.getDepth(), branchName);
                    remoteCommit.setId(localCommit.getId());
                    remoteCommit.save(remotePath);
                }
            }
        }

        Commit newRemoteHead = Commit.getCommit(getHeadId(), remotePath);
        Branch updatedBranch = new Branch(branchName, newRemoteHead);
        updatedBranch.save(remotePath);

        if (branchName.equals(newRemoteHead.getBranch())) {
            File remoteHead = join(remotePath, "head");
            writeContents(remoteHead, getHeadId());
        }
    }

    public void pull(String remoteName, String branchName) {
        fetch(remoteName, branchName);
        merge(remoteName + "/" + branchName);
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
            contents.append(headBlob.getContentString());
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
