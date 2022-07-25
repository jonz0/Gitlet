package gitlet;

/** General exception indicating a Gitlet error.  For fatal errors, the
 *  result of .getMessage() is the error message to be printed.
 *  @author P. N. Hilfinger
 */
class GitletException extends RuntimeException {
    /** A GitletException MSG as its message. */
    GitletException(String msg) {
        super(msg);
    }
}
