package com.abhishek.sit.service;

import com.abhishek.sit.repository.SitRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

@Service
public class RepositoryInitService {

    public void init() {
        File sitDir = new File(".sit");
        if (sitDir.exists()) {
            System.out.println("Reinitialized existing Sit repository in " + sitDir.getAbsolutePath());
            return;
        }

        try {
            // Create directories
            Files.createDirectories(Paths.get(".sit"));
            Files.createDirectories(Paths.get(".sit", "objects"));
            Files.createDirectories(Paths.get(".sit", "commits"));
            Files.createDirectories(Paths.get(".sit", "refs", "heads"));

            // Create HEAD file
            Path headPath = Paths.get(".sit", "HEAD");
            Files.writeString(headPath, "ref: refs/heads/main\n");

            System.out.println("Initialized empty Sit repository in " + sitDir.getAbsolutePath());

        } catch (IOException e) {
            System.err.println("Error initializing repository: " + e.getMessage());
            e.printStackTrace();
        }
    }
}
