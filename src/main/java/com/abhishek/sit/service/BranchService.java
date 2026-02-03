package com.abhishek.sit.service;

import com.abhishek.sit.repository.SitRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.stream.Stream;

@Service
public class BranchService {

    private final SitRepository sitRepository;

    @Autowired
    public BranchService(SitRepository sitRepository) {
        this.sitRepository = sitRepository;
    }

    public void handleBranch(String branchName) {
        if (!sitRepository.isInitialized()) {
            System.out.println("Not a sit repository.");
            return;
        }

        if (branchName == null) {
            listBranches();
        } else {
            createBranch(branchName);
        }
    }

    private void listBranches() {
        try {
            // Get current branch from HEAD
            // HEAD usually contains: ref: refs/heads/<branch_name>
            String headContent = Files.readString(Paths.get(SitRepository.HEAD_FILE)).trim();
            String currentBranch = "";
            if (headContent.startsWith("ref: refs/heads/")) {
                currentBranch = headContent.substring(16);
            }

            File headsDir = new File(SitRepository.DOT_SIT + "/refs/heads");
            if (!headsDir.exists()) {
                // If refs/heads is empty but we have HEAD, maybe we are detached or it's empty.
                // Assuming basic structure
                return;
            }

            File[] branches = headsDir.listFiles();
            if (branches != null) {
                for (File branch : branches) {
                    if (branch.getName().equals(currentBranch)) {
                        System.out.println("* " + branch.getName());
                    } else {
                        System.out.println("  " + branch.getName());
                    }
                }
            }

        } catch (IOException e) {
            System.err.println("Error listing branches: " + e.getMessage());
        }
    }

    private void createBranch(String branchName) {
        try {
            File branchFile = new File(SitRepository.DOT_SIT + "/refs/heads/" + branchName);
            if (branchFile.exists()) {
                System.out.println("Error: Branch '" + branchName + "' already exists.");
                return;
            }

            String currentCommitId = sitRepository.getHeadCommitId();
            if (currentCommitId == null) {
                System.out.println("Error: No commits yet. Cannot create branch.");
                return;
            }

            Files.writeString(branchFile.toPath(), currentCommitId);
            System.out.println("Created branch '" + branchName + "'");

        } catch (IOException e) {
            System.err.println("Error creating branch: " + e.getMessage());
        }
    }
}
