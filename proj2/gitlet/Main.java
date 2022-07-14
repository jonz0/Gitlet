package gitlet;


/** Driver class for Gitlet, a subset of the Git version-control system.
 *  @author TODO
 */
public class Main {

    /** Stores controls for gitlet and accesses methods via Repository object.
     *
     * Usage: java gitlet.Main ARGS, where ARGS contains
     *  <COMMAND> <OPERAND1> <OPERAND2> ...
     */
    public static void main(String[] args) {
        // TODO: what if args is empty?
        Repository r = new Repository();
        if (args.length == 0) {
            System.out.println("Please enter a command.");
        } else {
            switch (args[0]) {
                case "init":
                    System.getProperty("user.dir");
                    r.init();
                    break;
                case "add":
                    r.add(args[1]);
                    break;
                case "commit":
                    if (args.length == 2) r.commit(args[1], null);
                    else r.commit(args[1], args[2]);
                    break;
                case "rm":
                    r.rm(args[1]);
                    break;
                case "log":
                    r.log();
                    break;
                case "global-log":

                    break;
                case "find":

                    break;
                case "status":

                    break;
                case "checkout":
                    switch(args.length) {
                        case 2:
                            r.checkoutBranch(args[1]);
                        case 3:
                            if (!args[1].equals("--")) System.out.println("Not a valid command.");
                        case 4:
                            if (!args[2].equals("--")) System.out.println("Not a valid command.");
                    }
                    break;
                case "branch":
                    r.branch(args[1]);
                    break;
                case "rm-branch":
                    r.rmbranch(args[1]);
                    break;
                case "reset":

                    break;
                case "merge":

                    break;
                default:
                    System.out.println("You must enter a command.");
            }
        }
    }
}
