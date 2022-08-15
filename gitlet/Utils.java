package gitlet;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.*;

/**
 * Assorted utilities here.
 * Added and modified certain utilities for ease of use.
 */

public class Utils {

    // The length of a complete SHA-1 UID as a hexadecimal numeral.
    static final int UID_LENGTH = 40;

    /* SHA-1 HASH VALUES */

    /**
     * Returns the SHA-1 hash of the concatenation of VALS, which may
     * be any mixture of byte arrays and Strings.
     */

    static String sha1(Object... vals) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-1");
            for (Object val : vals) {
                if (val instanceof byte[]) {
                    md.update((byte[]) val);
                } else if (val instanceof String) {
                    md.update(((String) val).getBytes(StandardCharsets.UTF_8));
                } else {
                    throw new IllegalArgumentException("improper type to sha1");
                }
            }
            Formatter result = new Formatter();
            for (byte b : md.digest()) {
                result.format("%02x", b);
            }
            return result.toString();
        } catch (NoSuchAlgorithmException excp) {
            throw new IllegalArgumentException("System does not support SHA-1");
        }
    }

    /* READING AND WRITING FILE CONTENTS */

    /**
     * Return the entire contents of FILE as a byte array. FILE must be a normal
     * file. Throws IllegalArgumentException in case of problems.
     */
    static byte[] readContents(File file) {
        if (!file.isFile()) {
            throw new IllegalArgumentException("must be a normal file");
        }
        try {
            return Files.readAllBytes(file.toPath());
        } catch (IOException excp) {
            throw new IllegalArgumentException(excp.getMessage());
        }
    }

    /**
     * Return the entire contents of FILE as a String. FILE must be a normal file.
     * Throws IllegalArgumentException in case of problems.
     */
    static String readContentsAsString(File file) {
        return new String(readContents(file), StandardCharsets.UTF_8);
    }

    /**
     * Write the result of concatenating the bytes in CONTENTS to FILE, creating or
     * overwriting it as needed. Each object in CONTENTS may be either a String or a
     * byte array. Throws IllegalArgumentException in case of problems.
     */
    static void writeContents(File file, Object... contents) {
        try {
            if (file.isDirectory()) {
                throw new IllegalArgumentException("cannot overwrite directory");
            }
            BufferedOutputStream str = new BufferedOutputStream(Files.newOutputStream(file.toPath()));
            for (Object obj : contents) {
                if (obj instanceof byte[]) {
                    str.write((byte[]) obj);
                } else {
                    str.write(((String) obj).getBytes(StandardCharsets.UTF_8));
                }
            }
            str.close();
        } catch (IOException | ClassCastException excp) {
            throw new IllegalArgumentException(excp.getMessage());
        }
    }

    /**
     * Return an object of type T read from FILE, casting it to EXPECTEDCLASS.
     * Returns null in case of problems.
     */
    static <T extends Serializable> T readObject(File file,
            Class<T> expectedClass) {
        try {
            ObjectInputStream in = new ObjectInputStream(new FileInputStream(file));
            T result = expectedClass.cast(in.readObject());
            in.close();
            return result;
        } catch (IOException | ClassCastException
                | ClassNotFoundException excp) {
            return null;
        }
    }

    /**
     * Write OBJ to FILE.
     */
    static void writeObject(File file, Serializable obj) {
        writeContents(file, (Object) serialize(obj));
    }

    /* DIRECTORIES */

    /**
     * Filter out all but plain files.
     */
    private static final FilenameFilter PLAIN_FILES = (dir, name) -> new File(dir, name).isFile();

    /**
     * Filter out all but directories.
     */
    private static final FilenameFilter DIRECTORIES = (dir, name) -> new File(dir, name).isDirectory();

    /**
     * Returns a list of the names of all plain files in the directory DIR, in
     * lexicographic order as Java Strings. Returns null if DIR does not denote a
     * directory.
     */
    static List<String> plainFilenamesIn(File dir) {
        String[] files = dir.list(PLAIN_FILES);
        if (files == null) {
            return null;
        } else {
            Arrays.sort(files);
            return Arrays.asList(files);
        }
    }

    /**
     * Returns a list of the names of all directories in the directory DIR, in
     * lexicographic order as Java Strings. Returns null if DIR does not denote a
     * directory.
     */
    static List<String> directoriesIn(File dir) {
        String[] files = dir.list(DIRECTORIES);
        if (files == null) {
            return null;
        } else {
            Arrays.sort(files);
            return Arrays.asList(files);
        }
    }

    /**
     * Return the concatenation of FIRST and OTHERS into a File designator.
     */
    static File join(File first, String... others) {
        return Paths.get(first.getPath(), others).toFile();
    }

    /* SERIALIZATION UTILITIES */

    /**
     * Returns a byte array containing the serialized contents of OBJ.
     */
    static byte[] serialize(Serializable obj) {
        try {
            ByteArrayOutputStream stream = new ByteArrayOutputStream();
            ObjectOutputStream objectStream = new ObjectOutputStream(stream);
            objectStream.writeObject(obj);
            objectStream.close();
            return stream.toByteArray();
        } catch (IOException excp) {
            throw error("Internal error serializing commit.");
        }
    }

    /* MESSAGES AND ERROR REPORTING */

    /**
     * Return a GitletException whose message is composed from MSG and ARGS as
     * for the String.format method.
     */
    static GitletException error(String msg, Object... args) {
        return new GitletException(String.format(msg, args));
    }

    /**
     * Used for error handling. Prints the given message, then exits the program.
     */
    static void exit(String message) {
        System.out.println(message);
        System.exit(0);
    }

    /* GITLET COMMAND UTILS */

    /**
     * Returns an instance of the filename or an instance of join(CWD, file)
     * In gitlet, used to reference the contents of a file.
     */
    static File getFile(String file) {
        if (Paths.get(file).isAbsolute()) {
            return new File(file);
        } else {
            return join(Repository.CWD, file);
        }
    }

    /**
     * Builds the log and saves it to the LOG file.
     * References the HEAD file and reads the stored Commit object,
     * adds builds the log of that object, then points to it's parent Commit.
     * Repeats until the parents list is empty, then saves the LOG file.
     */
    static void buildLog() {
        Commit currentHead = getHeadCommit(Repository.GITLET_DIR);
        StringBuilder log = new StringBuilder();

        while (true) {
            assert currentHead != null;
            log.append(currentHead.getLog());
            if (currentHead.getParents().isEmpty()) {
                break;
            }
            String newHeadId = currentHead.getParents().get(0);
            currentHead = Commit.getCommit(newHeadId, Repository.GITLET_DIR);
        }
        log.delete(0, 1);
        writeContents(Repository.LOG, log.toString());
    }

    /**
     * Builds the global log of all commits in chronological order.
     */
    static void buildGlobalLog(Commit c, File gitletDir) {
        File globalLog = join(gitletDir, "global log");
        if (!globalLog.exists()) {
            writeContents(globalLog, "");
        }
        String log = readContentsAsString(globalLog)
                + c.getLog().substring(1) + "\n";
        writeContents(globalLog, log);
    }

    /**
     * Points the head object to a new Commit id.
     */
    static void setHead(String id, File gitletDir) {
        File headFile = join(gitletDir, "HEAD");
        Utils.writeContents(headFile, id);
    }

    /**
     * Returns the Sha-1 hash of the Head object in repository gitletDir.
     */
    static String getHeadId(File gitletDir) {
        File head = join(gitletDir, "HEAD");
        return Utils.readContentsAsString(head);
    }

    static Commit getHeadCommit(File gitletDir) {
        return Commit.getCommit(getHeadId(gitletDir), gitletDir);
    }

    static void setActiveBranchName(String name) {
        Utils.writeContents(Repository.ACTIVE_BRANCH, name);
    }

    static String getActiveBranchName(File gitletDir) {
        File branches = join(gitletDir, "branches");
        return readContentsAsString(join(branches, "active branch"));
    }

    static void updateActiveBranchHead(Commit c, File gitletDir) {
        Branch b = new Branch(getActiveBranchName(gitletDir), c);
        b.save(Repository.GITLET_DIR);
    }

    /**
     * Error handling for when a commit is being checked out. If an untracked file
     * will be modified or removed by the checkout, displays an error message.
     */
    static void checkForUntracked(Commit c) {
        for (String filePath : c.getTracked().keySet()) {
            if (!getHeadCommit(Repository.GITLET_DIR).getTracked().containsKey(filePath)) {
                if (new File(filePath).exists()) {
                    exit("There is an untracked file in the way; delete it, "
                            + "or add and commit it first.");
                }
            }
        }
    }

    /**
     * Returns a Map where the keys are all ancestor commits of the given commit,
     * and their values are the depth from the initial commit. The depth will be
     * used to find the latest common ancestor (ancestor with greatest depth) of two
     * commits.
     */
    static Map<String, Integer> getAncestorsDepths(Commit c) {
        Map<String, Integer> m = new HashMap<>();
        Commit currentCommit = c;

        while (true) {
            // if a Commit node was visited, no need to iterate through its ancestors.
            assert currentCommit != null;
            if (m.containsKey(currentCommit.getId())) {
                break;
            }
            // Add the current node and its depth.
            m.put(currentCommit.getId(), currentCommit.getDepth());

            // If the initial commit is visited, the iteration is finished.
            List<String> commitParents = currentCommit.getParents();
            if (commitParents.isEmpty()) {
                break;
            }
            // if the Commit node has 2 parents, add the ancestors of its second parent.
            if (commitParents.size() > 1) {
                String secondParentId = commitParents.get(1);
                Commit secondParent = Commit.getCommit(secondParentId, Repository.GITLET_DIR);
                m.putAll(getAncestorsDepths(secondParent));
            }
            // Change the current node to its first parent.
            String firstParentId = commitParents.get(0);
            currentCommit = Commit.getCommit(firstParentId, Repository.GITLET_DIR);
        }
        return m;
    }

    /**
     * Returns a map of all common ancestors, which are ancestor commits shared
     * by both the given commit and the Map iterated.
     */
    static Map<String, Integer> getCommonAncestorsDepths(Commit c, Map<String, Integer> iterated) {
        Map<String, Integer> commonAncestors = new HashMap<>();
        Commit currentCommit = c;
        while (true) {
            // If a shared Commit node is visited, put it in the returned map.
            // No need to iterate through its ancestors.
            assert currentCommit != null;
            if (iterated.containsKey(currentCommit.getId())) {
                commonAncestors.put(currentCommit.getId(), currentCommit.getDepth());
                break;
            }
            // If the initial commit is visited, the iteration is finished.
            List<String> commitParents = currentCommit.getParents();
            if (commitParents.isEmpty()) {
                break;
            }
            // if the Commit node has 2 parents, add the ancestors of its second parent.
            if (commitParents.size() > 1) {
                String secondParentId = commitParents.get(1);
                Commit secondParent = Commit.getCommit(secondParentId, Repository.GITLET_DIR);
                commonAncestors.putAll(getCommonAncestorsDepths(secondParent, iterated));
            }
            // Change the current node to its first parent.
            String firstParentId = commitParents.get(0);
            currentCommit = Commit.getCommit(firstParentId, Repository.GITLET_DIR);
        }
        return commonAncestors;
    }

    /**
     * Returns the id of the commit with the highest depth in the given commits,
     * representing the latest common ancestor.
     */
    static String latestCommonAncestor(Map<String, Integer> commonAncestorsDepths) {
        // Moves the commonAncestorsDepths HashMap to a sortable list.
        List<Map.Entry<String, Integer>> sortedList = new ArrayList<>(commonAncestorsDepths.entrySet());

        // Sorts the list by values in descending order.
        // Modifies the Collections.sort Compare function using its Comparator.
        sortedList.sort((o1, o2) -> o2.getValue() - o1.getValue());

        return sortedList.get(0).getKey();
    }

    /**
     * Returns all saved Blob ids from two separate commits.
     * Used as a helper for the merge command.
     */
    static Map<String, List<String>> allBlobIds(Commit head, Commit other) {
        Map<String, List<String>> ids = new HashMap<>();

        for (String filePath : head.getTracked().keySet()) {
            List<String> blobIds = new ArrayList<>();
            blobIds.add(head.getTracked().get(filePath));
            if (other.getTracked().containsKey(filePath)) {
                blobIds.add(other.getTracked().get(filePath));
            }
            ids.put(filePath, blobIds);
        }

        for (String filePath : other.getTracked().keySet()) {
            List<String> blobIds = new ArrayList<>();
            if (!head.getTracked().containsKey(filePath)) {
                blobIds.add(head.getTracked().get(filePath));
            }
            ids.put(filePath, blobIds);
        }
        return ids;
    }

    /**
     * Error handling: checks if the given commit id is at least 6 characters long.
     * Used to ensure multiple object ids are not referenced when using short UIDs.
     */
    static void overFiveCharacters(String commitId) {
        if (commitId.length() < 6) {
            exit("The specified commit ID must be at least 6 characters long.");
        }
    }

    /**
     * Handler for checkout: restores tracked files, deletes untracked files, clears
     * the staging area, and points the tracked map to tracked files in the given
     * commit. Saves the staging area and sets the branch head to the given commit.
     */
    static void checkoutProcesses(Commit c, Staging s) {
        c.restoreTrackedFiles();
        c.deleteUntrackedFiles();
        s.clear();
        s.setTracked(c.getTracked());
        s.save();
        setHead(c.getId(), Repository.GITLET_DIR);
    }

    /**
     * Returns a set of all parent commits starting from the commit c.
     * Used for copying over commits in a remote repository, where Commit c
     * is the head commit of a given branch.
     */
    static Set<Commit> getAllCommits(Commit c, File gitletDir) {
        Set<Commit> s = new HashSet<>();
        Commit currentCommit = c;

        while (true) {
            // If a Commit node was visited, no need to iterate through its ancestors.
            assert currentCommit != null;
            if (s.contains(currentCommit)) {
                break;
            }
            // Add the current node and its depth.
            s.add(currentCommit);

            // If the initial commit is visited, the iteration is finished.
            List<String> commitParents = currentCommit.getParents();
            if (commitParents.isEmpty()) {
                break;
            }
            // If the Commit node has 2 parents, add the ancestors of its second parent.
            if (commitParents.size() > 1) {
                String secondParentId = commitParents.get(1);
                Commit secondParent = Commit.getCommit(secondParentId, gitletDir);
                s.addAll(getAllCommits(secondParent, gitletDir));
            }
            // Change the current node to its first parent.
            String firstParentId = commitParents.get(0);
            currentCommit = Commit.getCommit(firstParentId, gitletDir);
        }
        return s;
    }

    /**
     * Returns a set of all blobs tracked by a set of commits.
     * Used for copying over blobs from a remote repository, where the commits
     * are obtained by calling getAllCommits on the head of a given branch.
     */
    static Set<Blob> getAllBlobs(Set<Commit> commits, File remoteDir) {
        Set<Blob> blobSet = new HashSet<>();
        for (Commit c : commits) {
            for (String blobId : c.getTracked().values()) {
                Blob b = Blob.getBlob(blobId, remoteDir);
                blobSet.add(b);
            }
        }
        return blobSet;
    }

    /**
     * Returns the Branch object stored in the file id and repository gitletDir.
     */
    static Branch getBranch(String name, File gitletDir) {
        File branchDir = join(gitletDir, "branches");
        File file = Utils.join(branchDir, name);
        return Utils.readObject(file, Branch.class);
    }

    static Commit getInitialCommit(File gitletDir) {
        File staging = join(gitletDir, "staging");
        Staging s = readObject(staging, Staging.class);
        return Commit.getCommit(Objects.requireNonNull(s, "The repository"
                + "has not yet been initialized.").getInitialId(), gitletDir);
    }
}
