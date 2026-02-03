package com.abhishek.sit;

import com.abhishek.sit.controller.CommandController;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;

@SpringBootApplication
public class SitApplication implements CommandLineRunner {

    private final CommandController commandController;

    @Autowired
    public SitApplication(CommandController commandController) {
        this.commandController = commandController;
    }

    public static void main(String[] args) {
        SpringApplication.run(SitApplication.class, args);
    }

    @Override
    public void run(String... args) throws Exception {
        if (args.length > 0) {
            commandController.handleCommand(args);
        } else {
            // Interactive Mode
            Scanner scanner = new Scanner(System.in);
            System.out.println("Sit CLI (Interactive Mode) - Type 'exit' to quit");

            while (true) {
                System.out.print("sit> ");
                if (!scanner.hasNextLine())
                    break;
                String line = scanner.nextLine().trim();

                if (line.isEmpty())
                    continue;
                if ("exit".equalsIgnoreCase(line) || "quit".equalsIgnoreCase(line))
                    break;

                // Simple splitting by quotes is hard, for now simple split by space
                // Enhanced splitting to handle quotes:
                List<String> tokens = new ArrayList<>();
                java.util.regex.Matcher m = java.util.regex.Pattern.compile("([^\"]\\S*|\".+?\")\\s*").matcher(line);
                while (m.find()) {
                    tokens.add(m.group(1).replace("\"", ""));
                }

                commandController.handleCommand(tokens.toArray(new String[0]));
            }
        }
    }
}
