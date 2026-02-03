package com.abhishek.sit.service;

import com.abhishek.sit.repository.SitRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class CheckoutService {

    private final SitRepository sitRepository;
    private final IndexService indexService;

    @Autowired
    public CheckoutService(SitRepository sitRepository, IndexService indexService) {
        this.sitRepository = sitRepository;
        this.indexService = indexService;
    }

    public void handleCheckout(String branchName) {
        if (!sitRepository.isInitialized()) {
            System.out.println("Not a sit repository.");
            return;
        }

        try {
            // 1. Verify branch exists
            File branchFile = new File(SitRepository.DOT_SIT + "/refs/heads/" + branchName);
            if (!branchFile.exists()) {
                System.out.println("Error: Branch '" + branchName + "' not found.");
                return;
            }

            // 2. Get target commit ID
            String targetCommitId = Files.readString(branchFile.toPath()).trim();

            // 3. Read Commit Data
            File commitFile = new File(SitRepository.COMMITS_DIR, targetCommitId);
            if (!commitFile.exists()) {
                System.out.println("Error: Commit object " + targetCommitId + " missing.");
                return;
            }

            List<String> lines = Files.readAllLines(commitFile.toPath());
            Map<String, String> newIndexState = new HashMap<>();

            boolean headerEnded = false;
            for (String line : lines) {
                if (line.isEmpty()) {
                    headerEnded = true;
                    continue;
                }
                if (!headerEnded)
                    continue;

                // File entry: path:hash
                int separator = line.lastIndexOf(':');
                if (separator == -1)
                    continue;

                String path = line.substring(0, separator);
                String hash = line.substring(separator + 1);

                // 4. Restore File
                restoreFile(path, hash);

                // 5. Update Index Map
                newIndexState.put(path, hash);
            }

            // 6. Update .sit/index
            indexService.saveIndex(newIndexState);

            // 7. Update HEAD
            // HEAD should point to the ref: refs/heads/<branch>
            Files.writeString(Paths.get(SitRepository.HEAD_FILE), "ref: refs/heads/" + branchName);

            System.out.println("Switched to branch '" + branchName + "'");

        } catch (IOException e) {
            System.err.println("Error during checkout: " + e.getMessage());
        }
    }

    private void restoreFile(String path, String hash) throws IOException {
        File blobFile = new File(SitRepository.OBJECTS_DIR, hash);
        if (!blobFile.exists()) {
            System.err.println("Warning: Blob " + hash + " missing for file " + path);
            return;
        }

        byte[] content = Files.readAllBytes(blobFile.toPath());
        File workingFile = new File(path);

        // Ensure parent dirs exist
        if (workingFile.getParentFile() != null) {
            workingFile.getParentFile().mkdirs();
        }

        Files.write(workingFile.toPath(), content);
    }
}
