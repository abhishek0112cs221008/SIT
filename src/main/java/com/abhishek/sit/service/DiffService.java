package com.abhishek.sit.service;

import com.abhishek.sit.repository.SitRepository;
import com.abhishek.sit.util.SitUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

@Service
public class DiffService {

    private final SitRepository sitRepository;
    private final IndexService indexService;

    @Autowired
    public DiffService(SitRepository sitRepository, IndexService indexService) {
        this.sitRepository = sitRepository;
        this.indexService = indexService;
    }

    public void handleDiff() {
        if (!sitRepository.isInitialized()) {
            System.out.println("Not a sit repository.");
            return;
        }

        try {
            Map<String, String> index = indexService.loadIndex();

            for (Map.Entry<String, String> entry : index.entrySet()) {
                String path = entry.getKey();
                String stagedHash = entry.getValue();
                File workingFile = new File(path);

                if (!workingFile.exists()) {
                    System.out.println("diff --git a/" + path + " b/" + path);
                    System.out.println("deleted file mode 100644");
                    System.out.println("--- a/" + path);
                    System.out.println("+++ /dev/null");
                    // TODO: Show deleted lines?
                    continue;
                }

                byte[] workingContentBytes = Files.readAllBytes(workingFile.toPath());
                String workingHash = SitUtil.getSha1(workingContentBytes);

                if (!stagedHash.equals(workingHash)) {
                    // Content changed
                    System.out.println("diff --git a/" + path + " b/" + path);
                    System.out.println("--- a/" + path);
                    System.out.println("+++ b/" + path);

                    File stagedBlob = new File(SitRepository.OBJECTS_DIR, stagedHash);
                    List<String> stagedLines = Collections.emptyList();
                    if (stagedBlob.exists()) {
                        stagedLines = Files.readAllLines(stagedBlob.toPath());
                    }
                    List<String> workingLines = Files.readAllLines(workingFile.toPath());

                    printDiff(stagedLines, workingLines);
                }
            }
        } catch (IOException e) {
            System.err.println("Error calculating diff: " + e.getMessage());
        }
    }

    private void printDiff(List<String> original, List<String> revised) {
        // LCS Implementation (Dynamic Programming)
        int[][] dp = new int[original.size() + 1][revised.size() + 1];

        // Fill DP table
        for (int i = 1; i <= original.size(); i++) {
            for (int j = 1; j <= revised.size(); j++) {
                if (original.get(i - 1).equals(revised.get(j - 1))) {
                    dp[i][j] = dp[i - 1][j - 1] + 1;
                } else {
                    dp[i][j] = Math.max(dp[i - 1][j], dp[i][j - 1]);
                }
            }
        }

        // Backtrack to find diff
        printDiffBacktrack(dp, original, revised, original.size(), revised.size());
    }

    private void printDiffBacktrack(int[][] dp, List<String> original, List<String> revised, int i, int j) {
        if (i > 0 && j > 0 && original.get(i - 1).equals(revised.get(j - 1))) {
            printDiffBacktrack(dp, original, revised, i - 1, j - 1);
            // System.out.println(" " + original.get(i - 1)); // Context line (optional,
            // omitted for brevity)
        } else if (j > 0 && (i == 0 || dp[i][j - 1] >= dp[i - 1][j])) {
            printDiffBacktrack(dp, original, revised, i, j - 1);
            System.out.println("\u001B[32m+ " + revised.get(j - 1) + "\u001B[0m"); // Green for Add
        } else if (i > 0 && (j == 0 || dp[i][j - 1] < dp[i - 1][j])) {
            printDiffBacktrack(dp, original, revised, i - 1, j);
            System.out.println("\u001B[31m- " + original.get(i - 1) + "\u001B[0m"); // Red for Remove
        }
    }
}
