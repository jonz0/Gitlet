package gitlet;

import java.io.File;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.*;

import static gitlet.Utils.*;

/** Creates and represents the gitlet repository.
 *  Stores all functionality of commands listed in the Main class.
 *  @author Jonathan Lu
 *
 *  NOTE: The Gitlet spec indicates remote branches saved to the repository should be
 *  named [remote name]/[remote branch name]. However, to prevent the forward slash
 *  from acting as a path separator, this version of Gitlet saves remote branches
 *  as [remote name]-[remote branch name] instead.
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
    public static final File REMOTES_DIR = join(GITLET_DIR, "remotes");
    public static final File BRANCHES_DIR = join(GITLET_DIR, "branches");
    public static final File ACTIVE_BRANCH = join(BRANCHES_DIR, "active branch");
    public static final File STAGING_FILE = join(GITLET_DIR, "staging");
    static Staging staging = STAGING_FILE.exists() ? Staging.readStaging() : new Staging();
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
            setHead(initial.getId(), GITLET_DIR);
            initial.save(GITLET_DIR);

            Branch master = new Branch("master", initial);
            master.save(GITLET_DIR);
            Utils.setActiveBranchName("master");
            Utils.buildGlobalLog(initial, GITLET_DIR);

            // Stores the initial commit in staging, used for rebuilding global logs.
            staging.setInitialId(initial.getId());
            staging.save();
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
        parents.add(getHeadId(GITLET_DIR));
        int parentDepth = getHeadCommit(GITLET_DIR).getDepth();

        // Adds second parent if there is one
        if (secondParentId != null) {
            parents.add(secondParentId);
        }

        // Saves the new staging area and adds the new commit object
        String timestamp = dateFormat.format(new Date());
        Commit c = new Commit(message, parents, tracked, timestamp, parentDepth + 1,
                getActiveBranchName(GITLET_DIR));
        c.save(GITLET_DIR);
        setHead(c.getId(), GITLET_DIR);
        updateActiveBranchHead(c);
        Utils.buildGlobalLog(c, GITLET_DIR);
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
        Branch b = new Branch(name, Commit.getCommit(readContentsAsString(HEAD), GITLET_DIR));

        if (join(Repository.BRANCHES_DIR, name).exists()) {
            Utils.exit("A branch with that name already exists.");
        }
        b.save(GITLET_DIR);
    }

    /** Takes all files in the commit at the head of the given branch, and puts them
     * in the working directory, overwriting the versions of the files that are already
     * there if they exist. The given branch is set as the active branch. */
    public void checkoutBranch(String name) {
        File branchFile = join(Repository.BRANCHES_DIR, name);
        if (!branchFile.exists()) {
            Utils.exit("No such branch exists.");
        }

        if (name.equals(Utils.getActiveBranchName(GITLET_DIR))) {
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
        Commit c = Commit.getCommit(commitId, GITLET_DIR);
        if (c == null) {
            Utils.exit("No commit with that id exists.");
        }
        assert c != null;
        if (!c.getTrackedNames().contains(name)) {
            Utils.exit("File does not exist in that commit.");
        }

        Blob b = Blob.getBlob(c.getTracked().get(getFile(name).getPath()), GITLET_DIR);
        assert b != null;
        Utils.checkForUntracked(Objects.requireNonNull(Commit.getCommit(commitId, GITLET_DIR)));
        writeContents(checkout, (Object) b.getContent());
    }

    /** Takes the version of the file as it exists in the head commit and puts it
     * in the working directory, overwriting the version of the file that’s already
     * there if there is one. The new version of the file is not staged. */
    public void checkoutFile(String name) {
        File checkout = join(CWD, name);

        Commit c = Commit.getCommit(readContentsAsString(Repository.HEAD), GITLET_DIR);
        assert c != null;
        if (!c.getTrackedNames().contains(name)) {
            Utils.exit("File does not exist in that commit.");
        }
        Blob b = Blob.getBlob(c.getTracked().get(getFile(name).getPath()), GITLET_DIR);
        assert b != null;
        writeContents(checkout, (Object) b.getContent());
    }

    /** Deletes the branch with the given name. */
    public void rmbranch(String name) {
        File branchFile = Utils.join(Repository.BRANCHES_DIR, name);
        if (!branchFile.exists()) {
            Utils.exit("A branch with that name does not exist.");
        }
        if (name.equals(Utils.getActiveBranchName(GITLET_DIR))) {
            Utils.exit("Cannot remove the current branch.");
        }
        branchFile.delete();
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
                Commit c = Commit.getCommit(directory.getName() + commitName, GITLET_DIR);
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
            if (branchName.equals(Utils.getActiveBranchName(GITLET_DIR))) {
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

        // Check for files that are in CWD and not being tracked.
        for (String filePath : staging.getTracked().keySet()) {
            File cwdFile = new File(filePath);
            Blob cwdBlob = new Blob(cwdFile);
            Blob trackedBlob = Blob.getBlob(staging.getTracked().get(filePath), GITLET_DIR);
            String fileName = cwdFile.getName();
            /* If the file is not in CWD but is being tracked (and not currently staged).
            * then it is appended to the status. */
            if (!Objects.requireNonNull(cwdFiles, "There are no trackable"
                    + "files stored in the working directory.").contains(fileName)) {
                if (!staging.getToRemove().contains(filePath)
                        && !staging.getToAdd().containsKey(filePath)) {
                    status.append(fileName).append(" ").append("(deleted)\n");
                }
                break;
            }
            /* If the CWD file's blob has a different id than the tracked blob id, then it also
            * has different contents and is appended to the status.. */
            assert trackedBlob != null;
            if (!cwdBlob.getId().equals(trackedBlob.getId())) {
                status.append(fileName).append(" ").append("(modified)\n");
            }
        }

        status.append("\n=== Untracked Files ===\n");
        /* Finds files in CWD that are not currently being tracked, and are also not
        * staged for addition or removal. */
        assert cwdFiles != null;
        for (String file : cwdFiles) {
            String filePath = getFile(file).getPath();
            if (!staging.getTracked().containsKey(filePath)) {
                if (!staging.getToRemove().contains(filePath)
                        && !staging.getToAdd().containsKey(filePath)) {
                    status.append(getFile(file).getName()).append("\n");
                }
            }
        }
        System.out.println(status);
    }

    /** Checks out all files tracked by the given commit, removes files not present in the commit,
     * and moves the current branch head to the commit node. */
    public void reset(String commitId) {
        overFiveCharacters(commitId);
        Commit resetCommit = Commit.getCommit(commitId, GITLET_DIR);
        if (resetCommit == null) {
            Utils.exit("No commit with that id exists.");
        }
        assert resetCommit != null;
        Utils.checkForUntracked(resetCommit);
        checkoutProcesses(resetCommit, staging);
        Branch b = new Branch(resetCommit.getBranch(), resetCommit);
        b.save(GITLET_DIR);
        setHead(resetCommit.getId(), GITLET_DIR);
    }

    /** Merges the given branch with the current one.
     * Automatically commits the merge after handling cases for staging. */
    public void merge(String branch) {
        /* Handles failure cases for when there are uncommited changes, when the given
        branch does not exist, and when attempting to merge a branch with itself. */
        mergeErrors(branch);

        Branch otherBranch = Utils.getBranch(branch, GITLET_DIR);
        Commit head = getHeadCommit(GITLET_DIR);
        Commit otherHead = otherBranch.getHead();
        Utils.checkForUntracked(otherHead);

        // Find split point:
        Map<String, Integer> commonAncestors =
                getCommonAncestorsDepths(otherHead, getAncestorsDepths(head));
        String splitId = latestCommonAncestor(commonAncestors);
        Commit splitCommit = Commit.getCommit(splitId, GITLET_DIR);

        /* Handles cases when the split point is the same commit as the given branch,
        * and when the split point is in the given branch. */
        if (splitId.equals(otherHead.getId())) {
            Utils.exit("Given branch is an ancestor of the current branch.");
        } else if (splitId.equals(getHeadId(GITLET_DIR))) {
            checkoutBranch(branch);
            Utils.exit("Current branch fast-forwarded.");
        }
        Map<String, List<String>> allBlobIds = Utils.allBlobIds(head, otherHead);
        Map<String, String> headBlobs = head.getTracked();
        Map<String, String> otherBlobs = otherHead.getTracked();
        Map<String, String> splitBlobs = Objects.requireNonNull(splitCommit,
                "Given branch is not linked to the current branch.").getTracked();

        for (String filePath : allBlobIds.keySet()) {
            Blob headBlob = null;
            if (headBlobs.get(filePath) != null) {
                headBlob = Blob.getBlob(headBlobs.get(filePath), GITLET_DIR);
            }
            Blob otherBlob = null;
            if (otherBlobs.get(filePath) != null) {
                otherBlob = Blob.getBlob(otherBlobs.get(filePath), GITLET_DIR);
            }

            // Sets up conditions for identifying different merge cases.
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
        String message = "Merged " + branch + " into " + getActiveBranchName(GITLET_DIR) + ".";
        commit(message, otherHead.getId(), true);
    }

    public void addRemote(String remoteName, String filePath) {
        File remoteFile = join(Repository.REMOTES_DIR, remoteName);
        if (remoteFile.exists()) {
            Utils.exit("A remote with that name already exists.");
        }
        writeContents(remoteFile, filePath);
    }

    /** Removes information associated with the given remote name. */
    public void rmRemote(String remoteName) {
        File remoteFile = join(REMOTES_DIR, remoteName);
        if (!remoteFile.exists()) {
            Utils.exit("A remote with that name does not exist.");
        }
        remoteFile.delete();
    }

    /** Brings commits from the remote repository into the local repository in a
     * branch [remote name]-[branch name]. */
    public void fetch(String remoteName, String branchName) {
        File remoteFile = join(REMOTES_DIR, remoteName);
        File remotePath = new File(readContentsAsString(remoteFile));
        fetchErrors(remoteName, branchName);

        // Copy over the commits and blobs:
        String remoteBranchName = remoteName + '-' + branchName;
        Branch remoteBranch = Utils.getBranch(branchName, remotePath);

        // Copies over commits and blobs from the remote branch to the current one.
        Set<Commit> remoteCommits = Utils.getAllCommits(remoteBranch.getHead(), remotePath);
        copyCommits(remoteCommits, remoteBranchName);
        copyBlobs(remoteCommits, remotePath);

        // Updates the head of the locally-stored remote branch.
        Commit localBranchHead = Commit.getCommit(getHeadId(remotePath), GITLET_DIR);
        Branch br = new Branch(remoteBranchName, localBranchHead);
        br.save(GITLET_DIR);

        // IF the current branch is the branch that was fetched, also updates HEAD.
        if (getActiveBranchName(GITLET_DIR).equals(remoteBranchName)) {
            setHead(getHeadId(GITLET_DIR), remotePath);
        }
    }

    /** Attempts to append the current branch’s commits to the end of the given
     * branch at the given remote. Only works if the remote branch’s head is in
     * the history of the current local head*/
    public void push(String remoteName, String branchName) {
        File remoteFile = join(REMOTES_DIR, remoteName);
        File remotePath = new File(readContentsAsString(remoteFile));
        if (!remoteFile.exists() || !remotePath.exists()) {
            Utils.exit("Remote directory not found.");
        }

        // Gets all local commit ids starting from the local head commit.
        Set<Commit> localCommits = getAllCommits(getHeadCommit(GITLET_DIR), GITLET_DIR);
        Set<String> localCommitIds = new HashSet<>();
        localCommits.forEach((commitId) -> localCommitIds.add(commitId.getId()));

        Branch remoteBranch = Utils.getBranch(branchName, remotePath);
        if (!localCommitIds.contains(getHeadId(remotePath))) {
            Utils.exit("Please pull down remote changes before pushing.");
        }

        // Gets all remote commit ids starting from the remote head commit.
        Set<Commit> remoteCommits = getAllCommits(remoteBranch.getHead(), remotePath);
        Set<String> remoteCommitIds = new HashSet<>();
        remoteCommits.forEach((commitId) -> remoteCommitIds.add(commitId.getId()));

        // Copies commits and blobs to the remote repository if it is not already there.
        for (String commitId : localCommitIds) {
            if (!remoteCommitIds.contains(commitId)) {
                Commit localCommit = Commit.getCommit(commitId, GITLET_DIR);
                assert localCommit != null;

                /* When copying over commits, changes file paths of tracked blobs to point to
                * the remote directory, and not the local one. */
                Map<String, String> newTracked = new HashMap<>();

                // Saves commits copied to the remote repository with updated blob paths.
                Commit remoteCommit = new Commit(localCommit.getMessage(),
                        localCommit.getParents(), newTracked, localCommit.getTimestamp(),
                        localCommit.getDepth(), branchName);
                remoteCommit.setId(localCommit.getId());
                remoteCommit.save(remotePath);

                // Wipes and rebuilds the remote repository global log.
                writeContents(join(remotePath, "global log"), "");
                Utils.buildGlobalLog(getInitialCommit(remotePath), remotePath);

                for (String filePath : localCommit.getTracked().keySet()) {
                    /* Create a new file path pointing to the remote repository for pushed commits
                    * by replacing old paths with newCommitPath. */
                    String localGitlet = GITLET_DIR.getParent();
                    String remoteGitlet = remoteBranch.getHead().getRepoDir().getPath();
                    String newCommitPath = filePath.replace(localGitlet, remoteGitlet);
                    newTracked.put(newCommitPath, localCommit.getTracked().get(filePath));

                    // Also iterates through tracked blobs to replace file paths.
                    for (String blobPath : localCommit.getTracked().keySet()) {
                        String blobId = localCommit.getTracked().get(blobPath);
                        Blob localBlob = Blob.getBlob(blobId, GITLET_DIR);
                        assert localBlob != null;

                        /* Creates a new blob where the source points to a file in the remote
                        * repository instead of the current one. */
                        if (Blob.getBlob(blobId, remotePath) == null) {
                            String oldSourcePath = localBlob.getSource().getPath();
                            String newSourcePath = oldSourcePath.replace(localGitlet, remoteGitlet);
                            Blob remoteBlob = new Blob(new File(newSourcePath));

                            // Copies the content and id of the local blob to the remote blob.
                            remoteBlob.setContent(localBlob.getContent());
                            remoteBlob.setContentString(localBlob.getContentString());
                            remoteBlob.setId(localBlob.getId());
                            remoteBlob.save(GITLET_DIR);
                        }
                    }
                }
            }
        }

        // Updates the head of the remote branch.
        Branch updatedBranch = new Branch(branchName, getHeadCommit(remotePath));
        updatedBranch.save(remotePath);

        // If the remote active branch is the same as the pushed branch, also updates HEAD.
        if (branchName.equals(getActiveBranchName(remotePath))) {
            setHead(getHeadId(GITLET_DIR), remotePath);
        }
    }

    /** Fetches the given branch from the remote repository, then merges that branch
     * with the current active branch. */
    public void pull(String remoteName, String branchName) {
        fetch(remoteName, branchName);
        merge(remoteName + '-' + branchName);
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

    public void copyCommits(Set<Commit> remoteCommits, String branchName) {
        for (Commit c : remoteCommits) {
            Map<String, String> newTracked = new HashMap<>();
            for (String filePath : c.getTracked().keySet()) {
                String newFilePath = filePath.replace(new File(filePath).getParent(),
                        GITLET_DIR.getParent());
                newTracked.put(newFilePath, c.getTracked().get(filePath));
            }
            List<String> newParents = c.getParents();
            Commit localCommit = new Commit(c.getMessage(), newParents, newTracked,
                    c.getTimestamp(), c.getDepth(), branchName);
            localCommit.setId(c.getId());
            localCommit.save(GITLET_DIR);
        }
    }

    public void copyBlobs(Set<Commit> remoteCommits, File remotePath) {
        Set<Blob> remoteBlobs = Utils.getAllBlobs(remoteCommits, remotePath);
        for (Blob b : remoteBlobs) {
            if (Blob.getBlob(b.getId(), GITLET_DIR) == null){
                String oldSourcePath = b.getSource().getParent();
                String newPath = b.getSource().getPath().replace(oldSourcePath,
                        GITLET_DIR.getParent());
                Blob localBlob = new Blob(new File(newPath));
                localBlob.setContent(b.getContent());
                localBlob.setContentString(b.getContentString());
                localBlob.setId(b.getId());
                localBlob.save(GITLET_DIR);
            }
        }
    }

    /* ERROR HANDLING */

    public void mergeErrors(String branch) {
        if (!staging.isClear()) {
            Utils.exit("You have uncommitted changes.");
        } else if (!Utils.join(Repository.BRANCHES_DIR, branch).exists()) {
            Utils.exit("A branch with that name does not exist.");
        } else if (branch.equals(getActiveBranchName(GITLET_DIR))) {
            Utils.exit("Cannot merge a branch with itself.");
        }
    }

    public void fetchErrors(String remoteName, String branchName) {
        File remoteFile = join(REMOTES_DIR, remoteName);
        File remotePath = new File(readContentsAsString(remoteFile));
        if (!remoteFile.exists() || !remotePath.exists()) {
            Utils.exit("Remote directory not found.");
        }

        File remoteBranches = Utils.join(remotePath, "branches");
        if (!Objects.requireNonNull(plainFilenamesIn(remoteBranches),
                "That remote does not have that branch.").contains(branchName)) {
            Utils.exit("That remote does not have that branch.");
        }
    }
}
