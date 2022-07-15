package gitlet;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;

/** Driver class for Gitlet, the tiny stupid version-control system.
 *  @author
 */
public class Main {


    /** Usage: java gitlet.Main ARGS, where ARGS contains
     *  <COMMAND> <OPERAND> .... */
    public static void main(String... args) {
        if(args.length == 0){
            System.err.println("Need a subcommand");
            return;
        }
        // first load gitlet object

        try {
            Gitlet gitlet = new Gitlet ();
            //System.out.println("===");

            switch (args[0]) {
                case "init":
                    gitlet.init();
                    break;
                case "add":
                    gitlet.add(args[1]);
                    break;
                case "commit":
                    if(args.length != 2){
                        System.err.println("Please enter a commit message.");
                    }
                    gitlet.commit(args[1]);
                    break;
                case "rm":
                    gitlet.rm(args[1]);
                    break;
                case "log":
//                    System.out.println("===");

                    gitlet.log();
                    break;
                case "global-log":
                    gitlet.globallog();
                    break;
                case "find":
                    gitlet.find(args[1]);
                    break;
                case "status":
                    gitlet.status();
                    break;
                case "checkout":
                    gitlet.checkout(args);
                    break;
                case "branch":
                    gitlet.branch(args[1]);
                    break;
                case "rmbranch":
                    gitlet.rmbranch(args[1]);
                    break;
                case "reset":
                    gitlet.reset();
                    break;
                case "merge":
                    gitlet.merge(args[1]);
                    break;

            }
        gitlet.saveState();
        } catch (Exception e) {
//            System.out.println(e.getMessage());
        }

    }

}