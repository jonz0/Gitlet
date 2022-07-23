package gitlet;

/** Driver class for Gitlet, a subset of the Git version-control system. */
public class Main {

    /** Stores controls for gitlet and accesses methods via Repository object.
     *
     * Usage: java gitlet.Main ARGS, where ARGS contains
     *  <COMMAND> <OPERAND1> <OPERAND2> ...
     *  @author Jonathan Lu
     */
    public static void main(String[] args) {
        // TODO: what if args is empty?
        Repository r = new Repository();
        if (args.length == 0) {
            System.out.println("Please enter a command.");
        } else {
            switch (args[0]) {
                case "init" -> {
                    r.init();
                }
                case "add" -> {
                    r.exists();
                    r.add(args[1]);
                }
                case "commit" -> {
                    r.exists();
                    if (args.length == 1) {
                        System.out.println("Please enter a commit message.");
                    } else if (args.length == 2) r.commit(args[1], null, false);
                }
                case "rm" -> {
                    r.exists();
                    r.rm(args[1]);
                }
                case "log" -> {
                    r.exists();
                    r.log();
                }
                case "global-log" -> {
                    r.exists();
                    r.globalLog();
                }
                case "find" -> {
                    r.exists();
                    if (args.length == 1) {
                        System.out.println("Enter a message to search for.");
                    } else r.find(args[1]);
                }
                case "status" -> {
                    r.exists();
                    r.status();
                }
                case "checkout" -> {
                    r.exists();
                    if (args.length == 2) {
                        r.checkoutBranch(args[1]);
                    }
                    if (args.length == 3) {
                        if (!args[1].equals("--")) System.out.println("Incorrect operands.");
                        r.checkoutFile(args[2]);
                    }
                    if (args.length == 4) {
                        if (!args[2].equals("--")) System.out.println("Incorrect operands.");
                        Repository.checkoutCommit(args[1], args[3]);
                    }
                }
                case "branch" -> {
                    r.exists();
                    r.branch(args[1]);
                }
                case "rm-branch" -> {
                    r.exists();
                    r.rmbranch(args[1]);
                }
                case "reset" -> {
                    r.exists();
                    if (args.length != 2) Utils.exit("Enter a commit id to move to.");
                    r.reset(args[1]);
                }
                case "merge" -> {
                    r.exists();
                    if (args.length != 2) Utils.exit("Enter a branch to merge.");
                    r.merge(args[1]);
                }
                case "add-remote" -> {
                    r.exists();
                    r.addRemote(args[1], args[2]);
                }
                case "rm-remote" -> {
                    r.exists();
                    r.rmRemote(args[1]);
                }
                case "fetch" -> {
                    r.exists();
                    r.fetch(args[1], args[2]);
                }
                case "push" -> {
                    r.exists();
                    r.push(args[1], args[2]);
                }
                case "pull" -> {
                    r.exists();
                    r.pull(args[1], args[2]);
                }
                default -> System.out.println("No command with that name exists.");
            }
        }
    }
}
