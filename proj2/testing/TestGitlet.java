package gitlet;

import java.io.File;
import java.io.IOException;
import java.lang.Runtime;
import java.util.Map;

import org.junit.*;

import static gitlet.Utils.*;
// import static org.junit.Assert.*;

public class TestGitlet{

    @Test
    public void LCA() throws IOException {
        Runtime rt = Runtime.getRuntime();
        Process pr = rt.exec("java gitlet.Main init");

//        Commit cCommit = Commit.getCommit("9f3e48fdbe6c8b648f93e0d24492468be7473de4");
//        Commit masterCommit = Commit.getCommit("ca0e9284968b864516022e4e9f83a71601e603cf");
//        Map<String, Integer> depths = getAncestorsDepths(cCommit);
//        Map<String, Integer> common = getCommonAncestorsDepths(masterCommit, depths);
//        System.out.println(lowestCommonAncestor(common));
    }

    @Test
    public void tracked() throws IOException {
        Runtime rt = Runtime.getRuntime();
        Process pr = rt.exec("java gitlet.Main init");
        Repository r = new Repository();
        r.printTrackedInHead();
    }

    @Test
    public void currentBranch() throws IOException {
        Runtime rt = Runtime.getRuntime();
        Process pr = rt.exec("java gitlet.Main init");
        Repository r = new Repository();
        r.printCurrentBranch();
    }

    @Test
    public void head() throws IOException {
        Runtime rt = Runtime.getRuntime();
        Process pr = rt.exec("java gitlet.Main init");
        System.out.println(readContentsAsString(Repository.HEAD));
    }

    @Test
    public void ancestors() throws IOException {
        Runtime rt = Runtime.getRuntime();
        Process pr = rt.exec("java gitlet.Main init");
        Repository r = new Repository();
        for (String commitId : Utils.getAncestorsDepths(getHeadCommit()).keySet()) {
            System.out.println(commitId + ", " + Utils.getAncestorsDepths(getHeadCommit()).get(commitId));
        }
    }
}
