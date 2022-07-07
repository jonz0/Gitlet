package gitlet;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static gitlet.Utils.*;

// TODO: any imports you need here

/** Represents a gitlet repository.
 *  TODO: It's a good idea to give a description here of what else this Class
 *  does at a high level.
 *
 *  @author TODO
 */
public class Repository {
    /**
     * TODO: add instance variables here.
     *
     * List all instance variables of the Repository class here with a useful
     * comment above them describing what that variable represents and how that
     * variable is used. We've provided two examples for you.
     */

    /** The current working directory. */
    public static final File CWD = new File(System.getProperty("user.dir"));
    /** The .gitlet directory. */
    public static final File GITLET_DIR = join(CWD, "gitlet-temp");
    public static final File COMMITS_DIR = join(GITLET_DIR, "commits");
    // public static final File OBJECTS_DIR = join(COMMITS_DIR, "objects");
    public static final File STAGING_FILE = join(COMMITS_DIR, "staging");
    public static Staging staging = STAGING_FILE.exists() ? Staging.readStaging() : new Staging();

    public static final File BRANCHES_DIR = join(GITLET_DIR, "branches");

    public static final File HEAD = join(GITLET_DIR, "head.txt");


    public void init() {
        if (GITLET_DIR.exists()) System.out.println("Gitlet version control already exists in this directory, fool");
        else {
            Commit initial = new Commit("initial commit", null, null);
            GITLET_DIR.mkdir();
            COMMITS_DIR.mkdir();
        }

        Commit initial = new Commit("initial commit", null, null);
        initial.save();
    }

    public void add(String name) {
        File file = Utils.getFile(name);
        if (file.exists()) {
            if (staging.add(file)) staging.save();
        } else {
            System.out.println("The specified file doesn't exist, bimbo");
            System.exit(0);
        }
    }

    public static void commit(String message, String secondParentId) {
        if (staging.isEmpty()) {
            System.out.println("No changes were added to the commit, dummy");
            System.exit(0);
        }

        // Creates new tracked map and parents list to be committed
        Map<String, String> tracked = staging.commit();
        List<String> parents = new ArrayList<>();
        parents.add(getHead().getId());

        // Adds second parent if there is one
        if (secondParentId != null) parents.add(secondParentId);

        // Saves the new staging area and adds the new commit object
        staging.save();
        Commit c = new Commit(message, parents, tracked);
        c.save();
    }

    /** Points the head object to a new head ID. */
    public static void setHead(String headId) {
        writeContents(HEAD, headId);
    }

    /** Returns the Sha-1 id of the Head object. */
    public static String getHeadId() {
        return readContentsAsString(HEAD);
    }

    /** Returns the Commit object stored in Head. */
    public static Commit getHead() {
        return Commit.readSha(getHeadId());
    }
}
