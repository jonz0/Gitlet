package gitlet;

import java.io.File;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
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
    public static final File STAGING_FILE = join(GITLET_DIR, "staging");
    public static Staging staging = STAGING_FILE.exists() ? Staging.readStaging() : new Staging();

    public static final File BRANCHES_DIR = join(GITLET_DIR, "branches");

    public static final File HEAD = join(GITLET_DIR, "head");
    public static final File LOG = join(GITLET_DIR, "log");
    private final DateTimeFormatter formatObj = DateTimeFormatter.ofPattern("EEEE, dd MMMM yyyy, HH:mm:ss");

    public void init() {
        if (GITLET_DIR.exists()) System.out.println("Gitlet version control already exists in this directory, fool");
        else {
            LocalDateTime time = LocalDateTime.of(1970, 1,
                    1, 0, 0, 0);
            String timestamp = time.format(formatObj);
            Commit initial = new Commit("initial commit", null, null, timestamp);
            GITLET_DIR.mkdir();
            COMMITS_DIR.mkdir();
            setHead(initial.getId());
            initial.save();
        }
    }

    public void add(String name) {
        File file = Utils.getFile(name);
        if (file.exists()) {
            if (staging.add(file)) staging.save();
            else System.out.println("File " + name + " is already tracked and staged for addition.");
        } else {
            System.out.println("The specified file doesn't exist, bimbo");
            System.exit(0);
        }
    }

    public void commit(String message, String secondParentId) {
        if (staging.isClear()) {
            System.out.println("No changes were added to the commit, dummy");
            System.exit(0);
        }

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
        setHead(c.getId());
        c.save();
    }

    /**
     * Unstages the file if it is currently staged for addition. If the file is
     * tracked in the current commit, stage it for removal and remove the file
     * from the working directory if the user has not already done so.
     * */

    public void rm(String name) {
        // If the file does not exist, print a message.
        File file = Utils.getFile(name);
        if (!file.exists()) {
            System.out.println("File " + name + " does not exist in the current working directory.");
        }

        if(staging.remove(file)) {
            staging.save();
        } else {
            System.out.println("File " + name + " is already staged for removal.");
            System.exit(0);
        }
    }
    public void log() {
        System.out.println(Utils.readContentsAsString(LOG));
    }

    public void branch(String name) {

    }

    public void rmbranch(String name) {

    }
}
