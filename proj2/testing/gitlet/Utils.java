package gitlet;

import java.io.BufferedOutputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FilenameFilter;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.*;


/** Assorted utilities.
 *
 * Give this file a good read as it provides several useful utility functions
 * to save you some time.
 *
 *  @author P. N. Hilfinger
 */
public class Utils {

    /** The length of a complete SHA-1 UID as a hexadecimal numeral. */
    static final int UID_LENGTH = 40;

    /* SHA-1 HASH VALUES. */

    /** Returns the SHA-1 hash of the concatenation of VALS, which may
     *  be any mixture of byte arrays and Strings. */
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

    /** Returns the SHA-1 hash of the concatenation of the strings in
     *  VALS. */
    static String sha1(List<Object> vals) {
        return sha1(vals.toArray(new Object[vals.size()]));
    }


    /* FILE DELETION */

    /** Deletes FILE if it exists and is not a directory.  Returns true
     *  if FILE was deleted, and false otherwise.  Refuses to delete FILE
     *  and throws IllegalArgumentException unless the directory designated by
     *  FILE also contains a directory named .gitlet. */
    static boolean restrictedDelete(File file) {
        if (!(new File(file.getParentFile(), "gitlet-temp")).isDirectory()) {
            throw new IllegalArgumentException("not gitlet-temp working directory");
        }
        if (!file.isDirectory()) {
            return file.delete();
        } else {
            return false;
        }
    }

    /** Deletes the file named FILE if it exists and is not a directory.
     *  Returns true if FILE was deleted, and false otherwise.  Refuses
     *  to delete FILE and throws IllegalArgumentException unless the
     *  directory designated by FILE also contains a directory named .gitlet. */
    static boolean restrictedDelete(String file) {
        return restrictedDelete(new File(file));
    }


    /* READING AND WRITING FILE CONTENTS */

    /** Return the entire contents of FILE as a byte array.  FILE must
     *  be a normal file.  Throws IllegalArgumentException
     *  in case of problems. */
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

    /** Return the entire contents of FILE as a String.  FILE must
     *  be a normal file.  Throws IllegalArgumentException
     *  in case of problems. */
    static String readContentsAsString(File file) {
        return new String(readContents(file), StandardCharsets.UTF_8);
    }

    /** Write the result of concatenating the bytes in CONTENTS to FILE,
     *  creating or overwriting it as needed.  Each object in CONTENTS may be
     *  either a String or a byte array.  Throws IllegalArgumentException
     *  in case of problems. */
    static void writeContents(File file, Object... contents) {
        try {
            if (file.isDirectory()) {
                throw
                    new IllegalArgumentException("cannot overwrite directory");
            }
            BufferedOutputStream str =
                new BufferedOutputStream(Files.newOutputStream(file.toPath()));
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

    /** Return an object of type T read from FILE, casting it to EXPECTEDCLASS.
     *  Returns null in case of problems. */
    static <T extends Serializable> T readObject(File file,
                                                 Class<T> expectedClass) {
        try {
            ObjectInputStream in =
                new ObjectInputStream(new FileInputStream(file));
            T result = expectedClass.cast(in.readObject());
            in.close();
            return result;
        } catch (IOException | ClassCastException
                 | ClassNotFoundException excp) {
            return null;
        }
    }

    /** Write OBJ to FILE. */
    static void writeObject(File file, Serializable obj) {
        writeContents(file, serialize(obj));
    }


    /* DIRECTORIES */

    /** Filter out all but plain files. */
    private static final FilenameFilter PLAIN_FILES =
        new FilenameFilter() {
            @Override
            public boolean accept(File dir, String name) {
                return new File(dir, name).isFile();
            }
        };

    /** Filter out all but directories. */
    private static final FilenameFilter DIRECTORIES =
        new FilenameFilter() {
            @Override
            public boolean accept(File dir, String name) {
                return new File(dir, name).isDirectory();
            }
        };

    /** Returns a list of the names of all plain files in the directory DIR, in
     *  lexicographic order as Java Strings. Returns null if DIR does
     *  not denote a directory. */
    static List<String> plainFilenamesIn(File dir) {
        String[] files = dir.list(PLAIN_FILES);
        if (files == null) {
            return null;
        } else {
            Arrays.sort(files);
            return Arrays.asList(files);
        }
    }

    /** Returns a list of the names of all directories in the directory DIR, in
     *  lexicographic order as Java Strings. Returns null if DIR does
     *  not denote a directory. */
    static List<String> directoriesIn(File dir) {
        String[] files = dir.list(DIRECTORIES);
        if (files == null) {
            return null;
        } else {
            Arrays.sort(files);
            return Arrays.asList(files);
        }
    }

    /** Returns a list of the names of all plain files in the directory DIR, in
     *  lexicographic order as Java Strings. Returns null if DIR does
     *  not denote a directory. */
    static List<String> plainFilenamesIn(String dir) {
        return plainFilenamesIn(new File(dir));
    }


    /* OTHER FILE UTILITIES */

    /** Return the concatentation of FIRST and OTHERS into a File designator,
     *  analogous to the {@link java.nio.file.Paths.#get(String, String[])}
     *  method. */
    static File join(File first, String... others) {
        return Paths.get(first.getPath(), others).toFile();
    }


    /* SERIALIZATION UTILITIES */

    /** Returns a byte array containing the serialized contents of OBJ. */
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

    /** Return a GitletException whose message is composed from MSG and ARGS as
     *  for the String.format method. */
    static GitletException error(String msg, Object... args) {
        return new GitletException(String.format(msg, args));
    }

    /** Print a message composed from MSG and ARGS as for the String.format
     *  method, followed by a newline. */
    static void message(String msg, Object... args) {
        System.out.printf(msg, args);
        System.out.println();
    }

    static void exit(String message) {
        System.out.println(message);
        System.exit(0);
    }


    /* GITLET COMMAND UTILS */

    /** Returns an instance of the filename or an instance of join(CWD, file)
     * In gitlet, Used to reference the contents of a file. */
    static File getFile(String file) {
        if (Paths.get(file).isAbsolute()) {
            return new File(file);
        } else {
            return join(Repository.CWD, file);
        }
    }

    /** Builds the log and saves it to the LOG file.
     * References the HEAD file and reads the stored Commit object,
     * adds builds the log of that object, then points to it's parent Commit.
     * Repeats until the parents list is empty, then saves the LOG file. */
    static void buildLog() {
        Commit currentHeadCommit = Commit.getCommit(readContentsAsString(Repository.HEAD));
        StringBuilder log = new StringBuilder();

        while (true) {
            log.append(currentHeadCommit.getLog());

            if (currentHeadCommit.getParents().isEmpty()) {
                break;
            }
            String newHeadId = currentHeadCommit.getParents().get(0);
            currentHeadCommit = Commit.getCommit(newHeadId);
        }
        log.delete(0, 1);
        writeContents(Repository.LOG, log.toString());
    }

    static void buildGlobalLog(Commit c) {
        if (!Repository.GLOBAL_LOG.exists()) {
            writeContents(Repository.GLOBAL_LOG, "");
        }
        StringBuilder log = new StringBuilder();
        log.append(readContentsAsString(Repository.GLOBAL_LOG));
        log.append(c.getLog().substring(1)).append("\n");
        writeContents(Repository.GLOBAL_LOG, log.toString());
    }

    /** Points the head object to a new Commit id. */
    static void setHead(String id) {
        Utils.writeContents(Repository.HEAD, id);
    }

    /** Returns the Sha-1 id of the Head object. */
    static String getHeadId() {
        return Utils.readContentsAsString(Repository.HEAD);
    }

    static Commit getHeadCommit() {
        return Commit.getCommit(getHeadId());
    }

    static void setActiveBranchName(String name) {
        Utils.writeContents(Repository.ACTIVE_BRANCH, name);
    }

    static void updateActiveBranchHead(Commit c) {
        Branch b = new Branch(getActiveBranchName(), c);
        b.save();
    }

    static String getActiveBranchName() {
        return readContentsAsString(Repository.ACTIVE_BRANCH);
    }

    static Branch getActiveBranch() {
        return readObject(Repository.ACTIVE_BRANCH, Branch.class);
    }

    static void checkForUntracked(Commit c) {
        for (String filePath : c.getTracked().keySet()) {
            if (!getHeadCommit().getTracked().containsKey(filePath)) {
                if (new File(filePath).exists()) {
                    exit("There is an untracked file in the way; delete it, "
                            + "or add and commit it first.");
                }
            }
        }
    }

    static Map<String, Integer> getAncestorsDepths(Commit c) {
        Map<String, Integer> m = new HashMap<>();
        Commit currentCommit = c;

        while (true) {
            // if a Commit node was visited, no need to iterate through its ancestors.
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
                Commit secondParent = Commit.getCommit(secondParentId);
                m.putAll(getAncestorsDepths(secondParent));
            }

            // Change the current node to its first parent.
            String firstParentId = commitParents.get(0);
            currentCommit = Commit.getCommit(firstParentId);
        }
        return m;
    }

    static Map<String, Integer> getCommonAncestorsDepths(Commit c, Map<String, Integer> iterated) {
        Map<String, Integer> m = new HashMap<>();
        Commit currentCommit = c;
        while (true) {
            // if a shared Commit node is visited, put it in the returned map.
            // No need to iterate through its ancestors.
            if (iterated.containsKey(currentCommit.getId())) {
                m.put(currentCommit.getId(), currentCommit.getDepth());
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
                Commit secondParent = Commit.getCommit(secondParentId);
                m.putAll(getCommonAncestorsDepths(secondParent, iterated));
            }

            // Change the current node to its first parent.
            String firstParentId = commitParents.get(0);
            currentCommit = Commit.getCommit(firstParentId);
        }
        return m;
    }

    static String latestCommonAncestor(Map<String, Integer> commonAncestorsDepths) {
        // Moves the commonAncestorsDepths HashMap to a sortable list.
        List<Map.Entry<String, Integer>> sortedList =
                new ArrayList<>(commonAncestorsDepths.entrySet());

        // Sorts the list by values in descending order.
        // Modifies the Collections.sort Compare function using its Comparator.
        Collections.sort(sortedList, new Comparator<Map.Entry<String, Integer>>() {
            @Override
            public int compare(Map.Entry<String, Integer> o1, Map.Entry<String, Integer> o2) {
                return o2.getValue() - o1.getValue();
            }
        });

        return sortedList.get(0).getKey();
    }

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

    static void overFiveCharacters(String commitId) {
        if (commitId.length() < 6) {
            exit("The specified commit ID must be at least 6 characters long.");
        }
    }
}
