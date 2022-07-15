package gitlet;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.*;

import static gitlet.Utils.*;


/**
 * Created by monsg on 7/17/2017.
 */
public class Gitlet implements Serializable {
    private HashMap<String, String> branches = new HashMap<>(); // name of branch, hash of commit file
    private String head; // hash of commit file
    private String headBranch;
    private String workingDir;

    private HashMap<String, String> stageAdd = new HashMap<>(); // HASH OF BLOBS
    private ArrayList<String> stageRemove = new ArrayList<String>(); // HASH OF BLOBS
    //private String stageTracked = new String[]; // HASH OF BLOBS


    public Gitlet() throws IOException, ClassNotFoundException {
        // if file exists, load gitlet object if not create new one
        workingDir = System.getProperty("user.dir");
        File prevState = new File(workingDir, ".gitlet/gitletState");
//        System.out.println(prevState.getAbsolutePath());
        if (prevState.exists()) {
//            System.out.println("c");
            Gitlet prevGitlet = (Gitlet)deserialize(Utils.readContents(prevState));
            this.branches = prevGitlet.branches;
            this.head = prevGitlet.head;
            this.headBranch = prevGitlet.headBranch;
            this.workingDir = prevGitlet.workingDir;
            this.stageAdd = prevGitlet.stageAdd;
            this.stageRemove = prevGitlet.stageRemove;
//            saveState();
//            return;
        }
    }

    public void saveState() throws IOException {
        // This creates file pointer for a file in ./gitlet folder named gitletState
        File newGitlet = new File(workingDir, ".gitlet/gitletState");
        // This saves the entire "this" into a file
        writeContents(newGitlet, serialize(this));
    }

    public void init() throws IOException {

        Commit initialCommit = new Commit(null, "initial commit", null);

        /* Append "/.gitlet" to whatever path was created above. */
        File gitlet_dir = new File(workingDir, ".gitlet");
        if(gitlet_dir.exists()) {
            System.err.println("A gitlet version-control system already exists in the current directory.");
            // ..gitlet directory is already created so it means init has been run once;
        }
        /* Create the .gitlet directory! */
        gitlet_dir.mkdir();

        File newCommit = new File(workingDir, ".gitlet/" + initialCommit.hashName);

        writeContents(newCommit, serialize(initialCommit));

        branches.put("master", initialCommit.hashName);

        head = branches.get("master");

        headBranch = "master";

        saveState();
    }

    //Gadd
    public void add(String fileName) throws IOException  {
        File file = new File(workingDir, fileName);
//        System.out.println(file.getAbsolutePath());
//        if (!file.isFile()) {
//            throw new IllegalArgumentException("must be a normal file");
//        }
//        System.out.println(fileName);
        if (!file.exists()) {
            System.out.println("File does not exist.");
            return;
        }
        // get hash
        String hash = fileName + sha1(readContents(file));
        if (stageAdd.containsKey(fileName) && stageAdd.get(fileName).equals(hash)) {
            System.out.println("No change added.");
            return;
        }
        File blob = new File(workingDir,".gitlet/" + hash);
        // Makes copy of file andn puts it in blob with name "hash"
        writeContents(blob, readContents(file));
        // Puts staged file in staging area
        stageAdd.put(fileName, hash); // key=fileName, value=hash

        saveState();
    }

    //Gcommit
    public void commit(String args) throws IOException, ClassNotFoundException {
        // Deal with edge case File does not exist.
        if(stageAdd.isEmpty()){
            System.err.println("No changes added to the commit.");
        }


        String log = args;
        HashMap<String, String> blobMap = new HashMap<>();
        // Copy current commit's dictionary and update it with the staging area, save it to new commit
        File temp = new File(workingDir, ".gitlet/" + head);
        Commit parent = (Commit) deserialize(Utils.readContents(temp));

        if (parent.fileMap != null){
            blobMap.putAll(parent.fileMap);
        }

        blobMap.putAll(stageAdd); // TODO stageRemove
        for (String i: stageRemove) {
            blobMap.remove(i);
        }
//        System.out.println(stageAdd.keySet());
//        System.out.println(stageAdd.values());
        stageAdd = new HashMap<>();

        Commit c = new Commit(head, log, blobMap);

        File newCommit = new File(workingDir, ".gitlet/" + c.hashName);
        writeContents(newCommit, serialize(c));

//        System.out.println(blobMap.keySet());
//        System.out.println(blobMap.values());
//        System.out.println("hhh");
//        System.out.println(stageAdd.keySet());
//        System.out.println(stageAdd.values());

        head = c.hashName;

        branches.put(headBranch, head);

        saveState();
    }

    //// Grm
    public void rm(String fileName) throws IOException, ClassNotFoundException {
        // if file is alread in stageRemove, just return
        if (stageRemove.contains(fileName)){
//            System.out.println("Already in remove list");
            return;
        }
        // Get path to last commit
        File temp = new File(workingDir, ".gitlet/" + head);
        // Recover last commit
        Commit target = (Commit) deserialize(Utils.readContents(temp));

        File fileToDelete = new File(workingDir, fileName);

        if(target.fileMap.containsKey(fileName)){
            stageRemove.add(fileName);
            if(fileToDelete.exists()){
                fileToDelete.delete();
            }
            if(stageAdd.containsKey(fileName)) {
                stageAdd.remove(fileName);
            }
            saveState();
            return;
        } else if(stageAdd.containsKey(fileName)) {
            stageAdd.remove(fileName);
            saveState();
            return;
        }
        System.err.println("No reason to remove the file.");
    }

    //Gcheck
    public void checkout (String[] args) {
        try {
            switch (args.length) {
                case 2:
                    checkoutBranch(args[1]);
                    saveState();
                    break;
                case 3:
                    checkoutFile(args[2]);
                    saveState();
                    break;
                case 4:
                    checkoutCommit(args[1], args[3]);
                    saveState();
                    break;
            }
        } catch (Exception e) {

        }

    }

    // GchedkBr
    public void checkoutBranch (String branch) throws IOException, ClassNotFoundException {
        // Checks for all three failure cases
        System.out.println("suh");
        System.out.println(headBranch);

        // Get path to last commit
        File temp = new File(workingDir, ".gitlet/" + head);
        // Recover last commit
        Commit target = (Commit) deserialize(Utils.readContents(temp));

        if (!branches.containsKey(branch)) {
            System.out.println("branch doesnt exist");
            return;
            //throw new IllegalArgumentException("No such branch exists.");
        } else if (headBranch.equals(branch)) {
            System.out.println("nono need");
            return;
            //throw new IllegalArgumentException("No need to checkout the current branch.");
        } else {
            System.out.println("commit files:");

            System.out.println(target.fileMap);
            System.out.println(workingDir);

            for(String a : Utils.plainFilenamesIn(workingDir)){
                System.out.println("Files in dir:");
                System.out.println(a);
                if (!target.fileMap.containsKey(a) && !a.equals(".gitignore") && !a.equals("proj2.iml")) {
                    System.out.println("untracked");
                    return;
                    //throw new IllegalArgumentException("There is an untracked file in the way; delete it or add it first.");
                }
            }
        }

        head = branches.get(branch);
        headBranch = branch;
        System.out.println("suhhhh");
        System.out.println("suh");

        for (String a : target.getFileMap().keySet()) {
            String blobHash = target.getFileMap().get(a);
            File blobFile = new File(workingDir + "/.gitlet", blobHash);
            File fileToReplace = new File(workingDir, a);
            writeContents(fileToReplace, readContents(blobFile));

        }
        saveState();
    }


    public String[] filesInDir(String dirName) {
        File directory = new File(dirName);
        String[] fList = directory.list();
        return fList;
    }

    public void checkoutCommit (String commit, String filename) throws IOException, ClassNotFoundException {
        File temp = new File(workingDir, ".gitlet/" + commit);
        //loop through the files of the commit through Filemap
        Commit target = (Commit) deserialize(Utils.readContents(temp));

        for(String a : target.getFileMap().keySet()){
            if (!target.fileMap.containsKey(a)) {
                // TODO check if filename has path
                return;
            }
            String blobHash = target.getFileMap().get(a);


            File blobFile = new File(workingDir+"/.gitlet", blobHash);
            File fileToReplace = new File(workingDir, a);

            writeContents(fileToReplace, readContents(blobFile));

        }

    }

    ///Gcheckfile
    public void checkoutFile (String filename) throws IOException, ClassNotFoundException {
        // Get path to last commit
//        System.out.println("Checkout file: ");

        File temp = new File(workingDir, ".gitlet/" + head);
//        System.out.println(head);
        // Recover last commit
        Commit target = (Commit) deserialize(Utils.readContents(temp));

        /// DEBUGGGG
//        System.out.println("CheckoutFile current commit fileMap key = " + filename);
//        System.out.println("Key/value pairs");
//        System.out.println(target.fileMap.keySet());
//        System.out.println(target.fileMap.values());

        // Check if last commit has pointer to that file
        if (!target.fileMap.containsKey(filename)) {
            // TODO check if filename has path
            return;
        }
        // Get pointer of last commit's file version
        String blobHash = target.fileMap.get(filename);
        // Get path to commit's file version
        File blobFile = new File(workingDir+"/.gitlet", blobHash);
//        System.out.println("Blob path");
//        System.out.println(blobFile.getAbsolutePath());
        // Get path to file we want to replace
        File fileToReplace = new File(workingDir, filename);
//        System.out.println("FileReplaced path");
//        System.out.println(fileToReplace.getAbsolutePath());

        // Write contents of commit's file version to file to replace
        writeContents(fileToReplace, readContents(blobFile));
    }

    //Glog
    public void log() throws IOException, ClassNotFoundException {
        // use head to find current commit in HashMap structure
        // print SHA 1, Date, Commit Message
        //      along with other pretty shit
        // use parent SHA 1 to find parent in HashMap same way we used
        // head to find current commit. (suggests recursive method with helper)
//        System.out.println("===");

        helpLog(head);
        saveState();
    }

    public void helpLog(String currCom) throws ClassNotFoundException, IOException {
        File temp = new File(workingDir + "/.gitlet", currCom);
        Commit curr = (Commit) deserialize(Utils.readContents(temp));

        if(curr.parentHash == null) {
            System.out.println("===");
            System.out.println("Commit " + curr.hashName);
            System.out.println(curr.date);
            System.out.println(curr.log);
            System.out.println("");
            return;
        }

        System.out.println("===");
        System.out.println("Commit " + curr.hashName);
        System.out.println(curr.date);
        System.out.println(curr.log);
        System.out.println("");

        helpLog(curr.parentHash);
    }

    //Gglog
    public void globallog() throws IOException, ClassNotFoundException {
        // same as log but now we go to each branch and print history
        // until we get to SHA 1 we've already seen
        //Set<String> keyNames = branches.keySet();
        for(String a : branches.keySet()) {
            helpLog(branches.get(a));
        }
        saveState();
    }


    ArrayList<String> SHANames2 = new <String>ArrayList();
    ArrayList<String> seenCommits = new <String>ArrayList();

    // idk what the specs mean when they say
    // (The commit message is a single operand;
    // to indicate a multiword message,
    // put the operand in quotation marks,
    // as for the commit command below.)

    //Gfind
    public void find(String comMessage) throws IOException, ClassNotFoundException {
        // keyNames holds all the current branch names
        // which will be used to extract the SHA1 value
        // of the most recent commit per branch

        // iterates thru all existing branches
        // starting at the most recent commit

        for(String a : branches.keySet()) {
            helpfind(branches.get(a), comMessage);
        }
        if(seenCommits.isEmpty()){
            throw new IllegalArgumentException("Found no commit with that message.");
        }
        saveState();
    }


    public void helpfind(String currCom, String comMessage) throws IOException, ClassNotFoundException{
        File temp = new File(workingDir + "/.gitlet/", currCom);
        Commit curr = (Commit) deserialize(Utils.readContents(temp));

        // checks an ArrayList that will be populated with already
        // seen commit SHA1 values to make sure we don't go over the same
        // commit twice
        if (!SHANames2.contains(curr.getHashName())) {
            // prints out the commit SHA1 if the commit message
            // matches the message we're looking for

            if(curr.getLog().equals(comMessage)) {
                System.out.println(curr.getLog());
                seenCommits.add(curr.getHashName());
            }
            // adds current commits SHA1 value to
            // ArrayList SHANames2
            SHANames2.add(curr.getHashName());
        }
        // recursively calls helpfind on parent
        helpfind(curr.getParentHash(),comMessage);
    }

    /// Gstat
    public void status() throws IOException {
        System.out.println("=== Branches ===");
        List<String> sArray = new ArrayList<String>(branches.keySet());
        java.util.Collections.sort(sArray);

        //System.out.println(sArray);
        for (String i : sArray) {
            if (i.equals(headBranch)) {
                System.out.println("*"+ i);
            } else {
                System.out.println(i);
            }
        }
        System.out.println();


        System.out.println("=== Staged Files ===");
        sArray = new ArrayList<String>(stageAdd.keySet());
        java.util.Collections.sort(sArray);
        for (String i : sArray) {
            System.out.println(i);
        }

        System.out.println();


        System.out.println("=== Removed Files ===");
        Collections.sort(stageRemove);
        for (String i : stageRemove) {
            System.out.println(i);
        }

        System.out.println();

        // OPTIONAL BELOW
        System.out.println("=== Modifications Not Staged For Commit ===");
        System.out.println();
        System.out.println("=== Untracked Files ===");

        saveState();
    }


    // new HashMap that maps Branches to the intersection
    // point (commit) between this and the next branch
    private HashMap<String, String> branchToIntCom = new HashMap<>(); //Branch --> SHA1
    private int count;

    //Gbranch
    public void branch(String branchName) throws IOException {
        // adds element to hashMap mapping a new branchName
        // to the SHA1 of the current commit

        // does not checkout to new branch!
        branches.put(branchName, head);
        System.out.println(branchName);
        System.out.println(head);
        // populates branchToIntCom with the BranchName
        // and the corresponding intersection point (commit)
        branchToIntCom.put(branchName, head);
        saveState();
    }

    // Grmbranch
    public void rmbranch(String BranchName) throws IOException, ClassNotFoundException {
        if(head.equals(branches.get(BranchName))){
            throw new IllegalArgumentException("Cannot Remove the correct branch.");
        }
        helprmBranch(branches.get(BranchName), BranchName);
        if(count == 0){
            throw new IllegalArgumentException("A branch with that name does not exist");
        }
        saveState();
    }

    public void helprmBranch(String currCom, String BranchName) throws IOException, ClassNotFoundException {
        File temp = new File(workingDir, ".gitlet/" + currCom);
        Commit curr = (Commit) deserialize(Utils.readContents(temp));

        // checks to see if the currCommit's parent is the intersection point (commit)
        // and removes the pointer to it's parent if true,
        // recursively calls helprmBranch on it's parent if false
        if (curr.getParentHash().equals(branchToIntCom.get(BranchName))) {
            curr.setParentHash(null);
            branches.remove(BranchName);
            branchToIntCom.remove(BranchName);
            count++;
        }
        else {
            helprmBranch(curr.getParentHash(),BranchName);
        }
    }

    public void reset() throws IOException {
        saveState();
    }


    // recursive void function, history array maintained in caller
    public void getCommitHistory (ArrayList<String> history, String commitHash) throws IOException, ClassNotFoundException {
        if (commitHash == null) {
            return;
        }
        File temp = new File(workingDir, ".gitlet/" + commitHash);
        Commit currOne = (Commit) deserialize(Utils.readContents(temp));
        history.add(currOne.getHashName());
        getCommitHistory(history, currOne.getParentHash());
    }

    public void merge(String givenBranchName) throws IOException, ClassNotFoundException {

        String newBranch = branches.get(givenBranchName);
        boolean conflict = false; // Flag to check if we have a merge conflict (default is false);

        ArrayList<String> currentBranchHistory = new ArrayList<>();
        ArrayList<String> givenBranchHistory = new ArrayList<>();

        File temp = new File(workingDir, ".gitlet/" + head);
        Commit currentBranch = (Commit) deserialize(Utils.readContents(temp));

        String branchPointer = head;
        String splitPoint = head;

        /// ************************ Finding split point ***********************

        while (branchPointer!=null) {
            temp = new File(workingDir, ".gitlet/" + branches.get(branchPointer));
            Commit commit = (Commit) deserialize(Utils.readContents(temp));
            currentBranchHistory.add(commit.getHashName());
            branchPointer = commit.getParentHash();
        }

        branchPointer = newBranch;

        while (branchPointer!=null) {
            temp = new File(workingDir, ".gitlet/" + branches.get(branchPointer));
            Commit commit = (Commit) deserialize(Utils.readContents(temp));
            givenBranchHistory.add(commit.getHashName());
            branchPointer = commit.getParentHash();
        }
        boolean found = false;

        for (int i = 0; i < currentBranchHistory.size() && !found; i++) {
            for (int j = 0; j < givenBranchHistory.size() && !found; j++) {
                if (currentBranchHistory.get(i).equals(givenBranchHistory.get(j))) {
                    splitPoint = currentBranchHistory.get(i);
                    found = true;
                }

            }
        }

        /// ************************ Found split point ***********************

        // Split point If the split point is the same commit as the given branch, then we do nothing; the merge is complete, and the operation ends with the message Given branch is an ancestor of the current branch.

        if (splitPoint == newBranch) {
            System.out.println("Given branch is an ancestor of the current branch.");
            return;
        }

        // If the split point is the current branch,
        else if (splitPoint == head) {
//            If the split point is the current branch, then the current branch is set to the same commit as the given branch and the operation ends after printing the message

            head = newBranch;
            headBranch = givenBranchName;
            // TODO: if both branches become equal

            System.out.println("Current branch fast-forwarded.");
            saveState();
            return;
        }
        // Otherwise, we continue with the steps below.
        else {
            File stemp = new File(workingDir, ".gitlet/" + head);
            Commit currBranch = (Commit) deserialize(Utils.readContents(stemp));
            stemp = new File(workingDir, ".gitlet/" + branches.get(newBranch));
            Commit givenBranch = (Commit) deserialize(Utils.readContents(stemp));
            stemp = new File(workingDir, ".gitlet/" + splitPoint);
            Commit splitBranch = (Commit) deserialize(Utils.readContents(stemp));

            //1: Any files that have been modified in the given branch since the split point
            //but not modified in the current branch since the split point

            ArrayList<String> keptFiles = new ArrayList<>();
            for (String filename1 : givenBranch.getFileMap().keySet()) {
                if (!givenBranch.getFileMap().get(filename1).equals(splitBranch.getFileMap().get(filename1)) &&
                        currBranch.getFileMap().get(filename1).equals(splitBranch.getFileMap().get(filename1))) {
                    checkoutCommit(newBranch, filename1);
                    stageAdd.put(filename1, givenBranch.fileMap.get(filename1));
                }
                // These files should then all be automatically staged.
            }
            //**Step 1 Above also takes care of:
            //2: Any files that have been modified in the current branch but not in the given branch since the split point should stay as they are.
            //3: Any files that were not present at the split point and are present only in the current branch should remain as they are.
            //******

            //4: Any files that were not present at the split point and are present only in the given branch should be checked out and staged.
            for (String filename1 : givenBranch.getFileMap().keySet()) {
                if (!splitBranch.getFileMap().containsKey(filename1) && !currBranch.getFileMap().containsKey(filename1)){
                    checkoutCommit(newBranch, filename1);
                    stageAdd.put(filename1, givenBranch.fileMap.get(filename1));
                }
                // These files should then all be automatically staged.
            }

            //5: Any files present at the split point, unmodified in the current branch, and absent in the given branch should be removed (and untracked).
            for (String filename1 : splitBranch.getFileMap().keySet()) {
                if (currBranch.getFileMap().get(filename1).equals(splitBranch.getFileMap().get(filename1)) && !givenBranch.getFileMap().containsKey(filename1)){
                    //delete file from currBr
                    //add to stageremove

                    rm(filename1);

                    stageRemove.add(filename1);
                }
                // These files should then all be automatically staged.
            }
            //**Step 5 Above also takes care of:
            //6: Any files present at the split point, unmodified in the given branch, and absent in the current branch should remain absent.
            //****


            // HARD:
            // Any files modified in different ways in the current and given branches are in conflict.

//
//            for (String filename1 : givenBranch.getFileMap().keySet()) {
//                if (givenBranch.getFileMap().get(filename1).equals(currBranch.getFileMap().get(filename1)) ){
//
//
//
//                }
//                // These files should then all be automatically staged.
//            }



            // "Modified in different ways" can mean that the contents of both are changed and different from other,


            //
            // the contents of one are changed and the other is deleted,


            //
            // or the file was absent at the split point and have different contents

            // in the given and current branches.
            //
            //
            //
            //
            //
            // In this case, replace the contents of the conflicted file with

            // "Modified in different ways" can mean that the contents of both are changed and different from other, the contents
            // of one are changed and the other is deleted

            // Loop through givenBranch files
            for (String filename1 : givenBranch.getFileMap().keySet()) {
                if(currBranch.getFileMap().containsKey(filename1)){
                    // Check if common files are different
                    if(!currBranch.getFileMap().get(filename1).equals(givenBranch.getFileMap().get(filename1))){
                        /// Do ???
                        conflict = true;
                        String topDiv = "<<<<<<< HEAD\n";
                        File currFile = new File(workingDir, ".gitlet/" + givenBranch.getFileMap().get(filename1));
                        String midDiv = "=======\n";
                        File givenFile = new File(workingDir, ".gitlet/" + givenBranch.getFileMap().get(filename1));
                        String btmDiv = ">>>>>>>";


                        byte[] encoded1 = Files.readAllBytes(currFile.toPath());
                        String x = new String(encoded1);
                        byte[] encoded2 = Files.readAllBytes(givenFile.toPath());
                        String y = new String(encoded2);
                        String total = topDiv + x + midDiv + y + btmDiv;
                        File outp = new File(workingDir, filename1);
                        writeContents(outp, total.getBytes());
                    }
                } else {
                    // Check if file missing in currBranch has been changed compared to splitBranch
                    if(!currBranch.getFileMap().get(filename1).equals(splitBranch.getFileMap().get(filename1))){
                        conflict = true;
                        String topDiv = "<<<<<<< HEAD\n";
                        File currFile = new File(workingDir, ".gitlet/" + givenBranch.getFileMap().get(filename1));
                        String midDiv = "=======\n";
                        File givenFile = new File(workingDir, ".gitlet/" + givenBranch.getFileMap().get(filename1));
                        String btmDiv = ">>>>>>>";


                        byte[] encoded1 = Files.readAllBytes(currFile.toPath());
                        String x = new String(encoded1);
                        byte[] encoded2 = Files.readAllBytes(givenFile.toPath());
                        String y = new String(encoded2);
                        String total = topDiv + x + midDiv + y + btmDiv;
                        File outp = new File(workingDir, filename1);
                        writeContents(outp, total.getBytes());
                    }
                }
            }

            // Loop through currBranch files
            for (String filename1 : currBranch.getFileMap().keySet()) {
                // Check if they contain same files
                if(givenBranch.getFileMap().containsKey(filename1)){
                    // Check if common files are different
                    if(!givenBranch.getFileMap().get(filename1).equals(currBranch.getFileMap().get(filename1))){
                        /// Do ???
                        conflict = true;
                        String topDiv = "<<<<<<< HEAD\n";
                        File currFile = new File(workingDir, ".gitlet/" + givenBranch.getFileMap().get(filename1));
                        String midDiv = "=======\n";
                        File givenFile = new File(workingDir, ".gitlet/" + givenBranch.getFileMap().get(filename1));
                        String btmDiv = ">>>>>>>";


                        byte[] encoded1 = Files.readAllBytes(currFile.toPath());
                        String x = new String(encoded1);
                        byte[] encoded2 = Files.readAllBytes(givenFile.toPath());
                        String y = new String(encoded2);
                        String total = topDiv + x + midDiv + y + btmDiv;
                        File outp = new File(workingDir, filename1);
                        writeContents(outp, total.getBytes());
                    }
                } else {
                    // Check if file missing in givenBranch has been changed compared to splitBranch
                    if(!givenBranch.getFileMap().get(filename1).equals(splitBranch.getFileMap().get(filename1))){
                        conflict = true;
                        String topDiv = "<<<<<<< HEAD\n";
                        File currFile = new File(workingDir, ".gitlet/" + givenBranch.getFileMap().get(filename1));
                        String midDiv = "=======\n";
                        File givenFile = new File(workingDir, ".gitlet/" + givenBranch.getFileMap().get(filename1));
                        String btmDiv = ">>>>>>>";


                        byte[] encoded1 = Files.readAllBytes(currFile.toPath());
                        String x = new String(encoded1);
                        byte[] encoded2 = Files.readAllBytes(givenFile.toPath());
                        String y = new String(encoded2);
                        String total = topDiv + x + midDiv + y + btmDiv;
                        File outp = new File(workingDir, filename1);
                        writeContents(outp, total.getBytes());
                    }
                }
            }

        }

        /// Is there a conflict??
        if(conflict){
            System.out.println("Encountered a merge conflict.");
            return;
        }
        // No conflict -> Make and serialize the merge commit:
        commit("Merged [current branch name] with [given branch name]");

        // Create commit HERE: What to put in it?
        saveState();
    }


    public byte[] serialize (Object obj) throws IOException {
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            ObjectOutputStream os = new ObjectOutputStream(out);
            os.writeObject(obj);
            return out.toByteArray();
    }

    public Object deserialize(byte[] data) throws IOException, ClassNotFoundException {
        ByteArrayInputStream in = new ByteArrayInputStream(data);
        ObjectInputStream is = new ObjectInputStream(in);
        return is.readObject();
    }



}
