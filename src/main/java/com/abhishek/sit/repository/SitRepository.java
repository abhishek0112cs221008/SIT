package com.abhishek.sit.repository;

import org.springframework.stereotype.Service;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;

@Service
public class SitRepository {

    public static final String DOT_SIT = ".sit";
    public static final String OBJECTS_DIR = ".sit/objects";
    public static final String COMMITS_DIR = ".sit/commits";
    public static final String HEAD_FILE = ".sit/HEAD";

    public boolean isInitialized() {
        return new File(DOT_SIT).exists();
    }

    public void saveObject(String hash, byte[] data) throws IOException {
        Files.write(Paths.get(OBJECTS_DIR, hash), data);
    }

    public void saveCommit(String hash, String content) throws IOException {
        Files.writeString(Paths.get(COMMITS_DIR, hash), content);
    }

    public String getCurrentBranch() throws IOException {
        if (!new File(HEAD_FILE).exists())
            return null;
        String content = Files.readString(Paths.get(HEAD_FILE)).trim();
        if (content.startsWith("ref: ")) {
            return content.substring(5);
        }
        return content; // Detached HEAD state
    }

    public String getHeadCommitId() throws IOException {
        String currentBranchRef = getCurrentBranch();
        if (currentBranchRef == null)
            return null;

        File branchFile = new File(".sit/" + currentBranchRef);
        if (branchFile.exists()) {
            return Files.readString(branchFile.toPath()).trim();
        }
        return null; // Empty branch
    }

    public void updateHead(String commitId) throws IOException {
        String currentBranchRef = getCurrentBranch();
        if (currentBranchRef != null && currentBranchRef.startsWith("refs/")) {
            Files.writeString(Paths.get(".sit", currentBranchRef), commitId);
        } else {
            // Detached HEAD logic? For now assume always on branch
        }
    }
}
