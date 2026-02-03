package com.abhishek.sit.service;

import com.abhishek.sit.repository.SitRepository;
import com.abhishek.sit.util.SitUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.time.LocalDateTime;
import java.util.*;

@Service
public class CommitService {

    private final SitRepository sitRepository;
    private final IndexService indexService;

    @Autowired
    public CommitService(SitRepository sitRepository, IndexService indexService) {
        this.sitRepository = sitRepository;
        this.indexService = indexService;
    }

    public void handleCommit(String message) {
        if (!sitRepository.isInitialized()) {
            System.out.println("Not a sit repository.");
            return;
        }

        try {
            // 1. Load from Index instead of scanning
            Map<String, String> fileHashes = indexService.loadIndex(); // Sorted path -> hash

            if (fileHashes.isEmpty()) {
                System.out.println("Nothing to commit (create/copy files and use \"sit add\" to track)");
                return;
            }

            // 2. Create Commit Content
            String parentId = sitRepository.getHeadCommitId();
            StringBuilder commitContent = new StringBuilder();
            if (parentId != null) {
                commitContent.append("parent: ").append(parentId).append("\n");
            }
            commitContent.append("author: User <user@example.com>\n");
            commitContent.append("date: ").append(LocalDateTime.now()).append("\n");
            commitContent.append("message: ").append(message).append("\n");
            commitContent.append("\n"); // Separator

            for (Map.Entry<String, String> entry : fileHashes.entrySet()) {
                // Format: path hash (as per original implementation which used path + " " +
                // hash?)
                // Wait, original: entry.getKey() + " " + entry.getValue()
                // IndexService format: path:hash (in file), loadIndex returns map.
                // Reusing the map logic is fine.
                // NOTE: Original used space separator in commit content "path hash"
                // My previous attempt assumed "path:hash". I will stick to "path hash" based on
                // line 56.
                commitContent.append(entry.getKey()).append(":").append(entry.getValue()).append("\n");
            }

            // 3. Save Commit
            String commitData = commitContent.toString();
            String commitHash = SitUtil.getSha1(commitData.getBytes());
            sitRepository.saveCommit(commitHash, commitData);

            // 4. Update HEAD
            sitRepository.updateHead(commitHash);

            System.out.println("[" + (parentId == null ? "root-commit" : "main") + " " + commitHash.substring(0, 7)
                    + "] " + message);

        } catch (IOException e) {
            System.err.println("Error creating commit: " + e.getMessage());
        }
    }

}
