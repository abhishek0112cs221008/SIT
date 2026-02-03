package com.abhishek.sit.service;

import com.abhishek.sit.repository.SitRepository;
import com.abhishek.sit.util.SitUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.*;

@Service
public class StatusService {

    private final SitRepository sitRepository;
    private final IndexService indexService;

    @Autowired
    public StatusService(SitRepository sitRepository, IndexService indexService) {
        this.sitRepository = sitRepository;
        this.indexService = indexService;
    }

    public void handleStatus() {
        if (!sitRepository.isInitialized()) {
            System.out.println("Not a sit repository (or any of the parent directories): .sit");
            return;
        }

        try {
            // 1. Get HEAD commit files (what's in the last commit)
            Map<String, String> headFiles = getHeadFiles(); // Path -> Hash

            // 2. Get staged files (what's in the index)
            Map<String, String> indexFiles = indexService.loadIndex(); // Path -> Hash

            // 3. Scan current working directory
            List<File> currentFiles = SitUtil.scanDirectory(new File("."), new HashSet<>(Arrays.asList(
                    ".sit", ".git", "build", "gradle", "target", ".gradle", ".idea")));

            Set<String> untracked = new TreeSet<>();
            Set<String> modified = new TreeSet<>();
            Set<String> staged = new TreeSet<>();
            Set<String> deleted = new TreeSet<>();

            // Track working directory files
            Map<String, String> workingFiles = new HashMap<>();
            for (File file : currentFiles) {
                String relPath = getRelativePath(file);
                byte[] content = Files.readAllBytes(file.toPath());
                String currentHash = SitUtil.getSha1(content);
                workingFiles.put(relPath, currentHash);
            }

            // Check all files in index (staged area)
            for (Map.Entry<String, String> entry : indexFiles.entrySet()) {
                String path = entry.getKey();
                String indexHash = entry.getValue();
                String workingHash = workingFiles.get(path);
                String headHash = headFiles.get(path);

                if (workingHash == null) {
                    // File is in index but not in working directory
                    if (headHash != null && !headHash.equals(indexHash)) {
                        // Staged for deletion (was in HEAD, staged as different/deleted)
                        staged.add(path);
                    }
                    deleted.add(path);
                } else {
                    // File exists in working directory
                    if (headHash == null) {
                        // New file staged for addition
                        staged.add(path);
                    } else if (!headHash.equals(indexHash)) {
                        // File modified and staged
                        staged.add(path);
                    }

                    if (!indexHash.equals(workingHash)) {
                        // File modified in working directory after staging
                        modified.add(path);
                    }
                }
            }

            // Check for untracked files (in working dir but not in index)
            for (String path : workingFiles.keySet()) {
                if (!indexFiles.containsKey(path)) {
                    untracked.add(path);
                }
            }

            // Check for deleted files (in HEAD but not in working dir and not staged)
            for (String path : headFiles.keySet()) {
                if (!workingFiles.containsKey(path) && !indexFiles.containsKey(path)) {
                    deleted.add(path);
                }
            }

            printStatus(staged, modified, deleted, untracked);

        } catch (IOException e) {
            System.err.println("Error calculating status: " + e.getMessage());
        }
    }

    private Map<String, String> getHeadFiles() throws IOException {
        Map<String, String> files = new HashMap<>();
        String headCommitId = sitRepository.getHeadCommitId();
        if (headCommitId == null) {
            return files; // No commits yet
        }

        // Parse commit file to get file list
        File commitFile = new File(SitRepository.COMMITS_DIR, headCommitId);
        if (!commitFile.exists()) {
            return files;
        }

        List<String> lines = Files.readAllLines(commitFile.toPath());

        // Commit Format:
        // parent: <id>
        // author: ...
        // date: ...
        // message: ...
        // (blank line)
        // path/to/file1:hash
        // path/to/file2:hash

        boolean parsingFiles = false;
        for (String line : lines) {
            if (line.isEmpty()) {
                parsingFiles = true;
                continue;
            }
            if (parsingFiles) {
                // FIXED: Use colon separator instead of space to match CommitService format
                int separator = line.lastIndexOf(':');
                if (separator != -1) {
                    String path = line.substring(0, separator);
                    String hash = line.substring(separator + 1).trim();
                    files.put(path, hash);
                }
            }
        }
        return files;
    }

    private String getRelativePath(File file) {
        String path = file.getPath().replace("\\", "/");
        if (path.startsWith("./")) {
            return path.substring(2);
        }
        return path;
    }

    private void printStatus(Set<String> staged, Set<String> modified,
                             Set<String> deleted, Set<String> untracked) {
        // Check if working tree is clean
        if (staged.isEmpty() && modified.isEmpty() && deleted.isEmpty() && untracked.isEmpty()) {
            System.out.println("nothing to commit, working tree clean");
            return;
        }

        // Show staged changes (changes to be committed)
        if (!staged.isEmpty()) {
            System.out.println("Changes to be committed:");
            System.out.println("  (use \"sit reset <file>...\" to unstage)");
            System.out.println();
            for (String file : staged) {
                if (deleted.contains(file)) {
                    System.out.println("\tdeleted:    " + file);
                } else {
                    System.out.println("\tnew file:   " + file);
                }
            }
            System.out.println();
        }

        // Show unstaged changes (changes not staged for commit)
        if (!modified.isEmpty() || (!deleted.isEmpty() && staged.isEmpty())) {
            System.out.println("Changes not staged for commit:");
            System.out.println("  (use \"sit add <file>...\" to update what will be committed)");
            System.out.println();
            for (String file : modified) {
                System.out.println("\tmodified:   " + file);
            }
            if (staged.isEmpty()) {
                for (String file : deleted) {
                    System.out.println("\tdeleted:    " + file);
                }
            }
            System.out.println();
        }

        // Show untracked files
        if (!untracked.isEmpty()) {
            System.out.println("Untracked files:");
            System.out.println("  (use \"sit add <file>...\" to include in what will be committed)");
            System.out.println();
            for (String file : untracked) {
                System.out.println("\t" + file);
            }
            System.out.println();
        }
    }
}