package gitlet;

import java.io.File;
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
    public static final File OBJECTS_DIR = join(COMMITS_DIR, "objects");
    public static final File STAGING_FILE = join(COMMITS_DIR, "staging");
    public static Staging staging = STAGING_FILE.exists() ? Staging.fromFile() : new Staging();

    public static final File BRANCHES_DIR = join(GITLET_DIR, "branches");

    public static final File HEAD = join(GITLET_DIR, "head.txt");


    public void init() {
        if (GITLET_DIR.exists()) System.out.println("Gitlet version control already exists in this directory, fool");
        else {
            Commit initial = new Commit("initial commit", null);
            GITLET_DIR.mkdir();
            COMMITS_DIR.mkdir();
            OBJECTS_DIR.mkdir();
            STAGING_FILE.mkdir();
        }
    }

    public void add(String s) {

    }

    public static void commit() {

    }
}
