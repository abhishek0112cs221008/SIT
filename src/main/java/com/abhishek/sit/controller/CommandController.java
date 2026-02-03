package com.abhishek.sit.controller;

import com.abhishek.sit.service.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class CommandController {

    private final RepositoryInitService repositoryInitService;
    private final StatusService statusService;
    private final CommitService commitService;
    private final LogService logService;
    private final IndexService indexService;
    private final BranchService branchService;
    private final CheckoutService checkoutService;
    private final DiffService diffService;
    private final MergeService mergeService;

    @Autowired
    public CommandController(RepositoryInitService repositoryInitService,
            StatusService statusService,
            CommitService commitService,
            LogService logService,
            IndexService indexService,
            BranchService branchService,
            CheckoutService checkoutService,
            DiffService diffService,
            MergeService mergeService) {
        this.repositoryInitService = repositoryInitService;
        this.statusService = statusService;
        this.commitService = commitService;
        this.logService = logService;
        this.indexService = indexService;
        this.branchService = branchService;
        this.checkoutService = checkoutService;
        this.diffService = diffService;
        this.mergeService = mergeService;
    }

    public void handleCommand(String[] args) {
        if (args.length == 0) {
            return;
        }

        String command = args[0];

        switch (command) {
            case "help":
                printHelp();
                break;
            case "version":
                printVersion();
                break;
            case "init":
                repositoryInitService.init();
                break;
            case "status":
                statusService.handleStatus();
                break;
            case "add":
                if (args.length < 2) {
                    System.out.println("Nothing specified, nothing added.");
                } else {
                    indexService.handleAdd(args[1]);
                }
                break;
            case "log":
                // Basic flag support
                boolean oneline = args.length > 1 && "--oneline".equals(args[1]);
                logService.handleLog(oneline);
                break;
            case "commit":
                String message = null;
                if (args.length >= 3 && "-m".equals(args[1])) {
                    message = args[2];
                } else if (args.length >= 2) {
                    message = args[1]; // Fallback to old style
                }

                if (message == null) {
                    System.out.println("Error: Commit message required. Usage: sit commit -m <message>");
                } else {
                    commitService.handleCommit(message);
                }
                break;
            case "branch":
                if (args.length < 2) {
                    branchService.handleBranch(null);
                } else {
                    branchService.handleBranch(args[1]);
                }
                break;
            case "checkout":
                if (args.length < 2) {
                    System.out.println("Error: Branch name required.");
                } else {
                    checkoutService.handleCheckout(args[1]);
                }
                break;
            case "diff":
                diffService.handleDiff();
                break;
            case "merge":
                if (args.length < 2) {
                    System.out.println("Error: Branch name required.");
                } else {
                    mergeService.handleMerge(args[1]);
                }
                break;
            default:
                System.out.println("Unknown command: " + command);
                printHelp();
                break;
        }
    }

    private void printHelp() {
        System.out.println("Sit CLI - Version 0.0.1+SNAPSHOT");
        System.out.println("Usage: sit <command> [<args>]");
        System.out.println();
        System.out.println("Available commands:");
        System.out.println("  help       Show this help message");
        System.out.println("  version    Show version information");
        System.out.println("  init       Initialize a new Sit repository");
        System.out.println("  add        Add file contents to the index");
        System.out.println("  status     Show the working tree status");
        System.out.println("  commit     Record changes to the repository");
        System.out.println("  branch     List or create branches");
        System.out.println("  checkout   Switch branches or restore working tree files");
        System.out.println("  merge      Join two development histories together (Fast-Forward only)");
        System.out.println("  diff       Show changes between working tree and index");
        System.out.println("  log        Show commit history");
        System.out.println();
    }

    private void printVersion() {
        System.out.println("sit version 0.0.1-SNAPSHOT");
    }
}
