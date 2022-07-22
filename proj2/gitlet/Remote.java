//package gitlet;
//
//import java.io.File;
//import java.io.Serializable;
//import java.util.HashMap;
//import java.util.Map;
//
//import static gitlet.Utils.*;
//
//public class Remote implements Serializable {
//
//    private Map<String, Branch> branches = new HashMap<>();
//    private Map<String, Commit> commits = new HashMap<>();
//    private Map<String, Blob> blobs = new HashMap<>();
//
//    public Remote(Map<String, Branch> branches,
//                  Map<String, Commit> commits, Map<String, Blob> blobs) {
//        this.branches = branches;
//        this.commits = commits;
//        this.blobs = blobs;
//    }
//
//    public void fetch(String branchName) {
//
//    }
//
//    public void push(String branchName) {
//
//    }
//
//    public void pull(String branchName) {
//
//    }
//
//    public void save() {
//        for (Branch b : branches.values()) {
//            b.save();
//        }
//
//    }
//}
