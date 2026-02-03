package com.abhishek.sit.service;

import com.abhishek.sit.repository.SitRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.List;

@Service
public class LogService {

    private final SitRepository sitRepository;

    @Autowired
    public LogService(SitRepository sitRepository) {
        this.sitRepository = sitRepository;
    }

    public void handleLog(boolean oneline) {
        if (!sitRepository.isInitialized()) {
            System.out.println("Not a sit repository.");
            return;
        }

        try {
            String currentCommitId = sitRepository.getHeadCommitId();
            if (currentCommitId == null) {
                System.out.println("No commits yet.");
                return;
            }

            while (currentCommitId != null) {
                File commitFile = new File(SitRepository.COMMITS_DIR, currentCommitId);
                if (!commitFile.exists()) {
                    System.out.println("Error: Commit object " + currentCommitId + " missing.");
                    break;
                }

                List<String> lines = Files.readAllLines(commitFile.toPath());
                String parentId = null;
                String author = "";
                String date = "";
                String message = "";

                for (String line : lines) {
                    if (line.startsWith("parent: "))
                        parentId = line.substring(8);
                    else if (line.startsWith("author: "))
                        author = line.substring(8);
                    else if (line.startsWith("date: "))
                        date = line.substring(6);
                    else if (line.startsWith("message: "))
                        message = line.substring(9);
                    else if (line.isEmpty())
                        break; // End of header
                }

                if (oneline) {
                    System.out.println(currentCommitId.substring(0, 7) + " " + message);
                } else {
                    System.out.println("commit " + currentCommitId);
                    System.out.println("Author: " + author);
                    System.out.println("Date:   " + date);
                    System.out.println();
                    System.out.println("    " + message);
                    System.out.println();
                }

                currentCommitId = parentId;
            }

        } catch (IOException e) {
            System.err.println("Error reading log: " + e.getMessage());
        }
    }
}
