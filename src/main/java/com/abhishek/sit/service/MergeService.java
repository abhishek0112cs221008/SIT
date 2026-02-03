package com.abhishek.sit.service;

import com.abhishek.sit.repository.SitRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.*;

@Service
public class MergeService {

    private final SitRepository sitRepository;

    @Autowired
    public MergeService(SitRepository sitRepository) {
        this.sitRepository = sitRepository;
    }

    public void handleMerge(String targetBranchName) {
        if (!sitRepository.isInitialized()) {
            System.out.println("Not a sit repository.");
            return;
        }

        try {
            // 1. Resolve Target Branch
            File targetBranchFile = new File(SitRepository.DOT_SIT + "/refs/heads/" + targetBranchName);
            if (!targetBranchFile.exists()) {
                System.out.println("Error: Branch '" + targetBranchName + "' not found.");
                return;
            }
            String targetCommitId = Files.readString(targetBranchFile.toPath()).trim();

            // 2. Resolve Current Branch
            String currentBranch = sitRepository.getCurrentBranch();
            if (currentBranch == null) {
                System.out.println("Error: Not on a branch.");
                return;
            }
            if (!currentBranch.startsWith("refs/heads/")) {
                System.out.println("Error: Detached HEAD. Cannot merge.");
                return;
            }
            String currentBranchName = currentBranch.substring(11); // remove refs/heads/
            String currentCommitId = sitRepository.getHeadCommitId();

            if (currentCommitId.equals(targetCommitId)) {
                System.out.println("Already up to date.");
                return;
            }

            // 3. Check Ancestry (Fast-Forward)
            if (isAncestor(currentCommitId, targetCommitId)) {
                // Fast-Forward
                System.out
                        .println("Updating " + currentCommitId.substring(0, 7) + ".." + targetCommitId.substring(0, 7));
                System.out.println("Fast-forward");

                // Update info/refs (the file pointer for the current branch)
                File currentBranchFile = new File(SitRepository.DOT_SIT + "/refs/heads/" + currentBranchName);
                Files.writeString(currentBranchFile.toPath(), targetCommitId);

                // Restore working directory (Using CheckoutService logic would be cleaner, but
                // I'll invoke it or simulate it)
                // CheckoutService logic switches HEAD, but here we kept HEAD pointing to
                // branch, and updated the branch.
                // We just need to sync the working dir to the NEW commit of the branch.
                // We can use a helper in CheckoutService if available, or just re-implement
                // restore.
                // Since CheckoutService methods are mostly private or specific to switching
                // HEAD, I'll copy the restore logic for safety.

                restoreWorkingTree(targetCommitId);

            } else {
                System.out.println("Merge conflict or non-fast-forward merge not yet implemented.");
                System.out.println("Aborting.");
            }

        } catch (IOException e) {
            System.err.println("Error during merge: " + e.getMessage());
        }
    }

    private boolean isAncestor(String potentialAncestor, String commitId) throws IOException {
        // BFS to find ancestor
        Queue<String> queue = new LinkedList<>();
        Set<String> visited = new HashSet<>();
        queue.add(commitId);
        visited.add(commitId);

        while (!queue.isEmpty()) {
            String current = queue.poll();
            if (current.equals(potentialAncestor)) {
                return true;
            }

            // Get Parent
            List<String> parents = getParents(current);
            for (String parent : parents) {
                if (!visited.contains(parent)) {
                    visited.add(parent);
                    queue.add(parent);
                }
            }
        }
        return false;
    }

    private List<String> getParents(String commitId) throws IOException {
        List<String> parents = new ArrayList<>();
        File commitFile = new File(SitRepository.COMMITS_DIR, commitId);
        if (!commitFile.exists())
            return parents;

        List<String> lines = Files.readAllLines(commitFile.toPath());
        for (String line : lines) {
            if (line.isEmpty())
                break; // End of header
            if (line.startsWith("parent: ")) {
                parents.add(line.substring(8).trim());
            }
        }
        return parents;
    }

    // Simplified restore (overwrites files)
    private void restoreWorkingTree(String commitId) throws IOException {
        File commitFile = new File(SitRepository.COMMITS_DIR, commitId);
        List<String> lines = Files.readAllLines(commitFile.toPath());

        boolean headerEnded = false;
        for (String line : lines) {
            if (line.isEmpty()) {
                headerEnded = true;
                continue;
            }
            if (!headerEnded)
                continue;

            int separator = line.lastIndexOf(':');
            if (separator == -1)
                continue;
            String path = line.substring(0, separator);
            String hash = line.substring(separator + 1);

            File blobFile = new File(SitRepository.OBJECTS_DIR, hash);
            if (blobFile.exists()) {
                File workingFile = new File(path);
                if (workingFile.getParentFile() != null)
                    workingFile.getParentFile().mkdirs();
                Files.write(workingFile.toPath(), Files.readAllBytes(blobFile.toPath()));
            }
        }
    }
}
