package com.github.wimve.githubissuesummary;

import org.apache.commons.cli.*;
import org.eclipse.egit.github.core.Issue;
import org.eclipse.egit.github.core.Milestone;
import org.eclipse.egit.github.core.RepositoryId;
import org.eclipse.egit.github.core.client.GitHubClient;
import org.eclipse.egit.github.core.service.IssueService;
import org.eclipse.egit.github.core.service.MilestoneService;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.List;
import java.util.Properties;
import java.util.Scanner;


public class Main {

    public static final String PARAM_CONFIG = "f";

    public static final String STATUS_OPEN = "open";
    public static final String STATUS_CLOSED = "closed";

    public static final String CONFIG_USERNAME = "username";
    public static final String CONFIG_PASSWORD = "password";
    public static final String CONFIG_REPO_OWNER = "repo-owner";
    public static final String CONFIG_REPO_NAME = "repo-name";
    public static final String CONFIG_TITLE_ISSUES_CLOSED = "title-issues-closed";
    public static final String CONFIG_TITLE_ISSUES_OPEN = "title-issues-open";


    public static void main(String[] args) {

        try {

            // Parse arguments (only config file path at the moment)
            CommandLine arguments = parseArguments(args);

            // Load config file from path
            Properties config = loadConfig(arguments.getOptionValue(PARAM_CONFIG));

            // Create client to connect to GitHub
            GitHubClient client = createClient(config);

            // Create repo ID
            RepositoryId repositoryId = createRepositoryId(config);

            // Get the repo's open milestones
            List<Milestone> milestones = getMilestones(client, repositoryId, STATUS_OPEN);

            // Ask the user for the milestone interactively
            Milestone milestone = askMilestone(milestones);

            // Give the output some space
            sysOut("");

            // Output chosen milestone
            sysOutTitle("Milestone " + milestone.getTitle() + ":");

            // Output open Issues
            sysOutTitle(config.getProperty(CONFIG_TITLE_ISSUES_OPEN));
            printIssues(getIssues(client, repositoryId, milestone, STATUS_OPEN));

            // Output closed Issues
            sysOutTitle(config.getProperty(CONFIG_TITLE_ISSUES_CLOSED));
            printIssues(getIssues(client, repositoryId, milestone, STATUS_CLOSED));

            // Give the output some space
            sysOut("");

        } catch (Exception e) {
            System.err.println(e);
        }
    }

    private static void sysOutTitle(String title) {
        sysOut("");
        sysOut(title);
    }

    private static CommandLine parseArguments(String[] args) throws ParseException {
        // create the command line parser
        CommandLineParser parser = new DefaultParser();

        // create Options object
        Options options = new Options();
        options.addOption(PARAM_CONFIG, true, "set config file path");

        CommandLine arguments = parser.parse(options, args);

        // Check arguments
        if (!arguments.hasOption(PARAM_CONFIG)) {
            throw new IllegalArgumentException("Please provide a config file");
        }

        return arguments;
    }

    private static Properties loadConfig(String settingsFile) throws IOException {
        Properties config = new Properties();
        InputStream input = new FileInputStream(settingsFile);
        config.load(input);
        return config;
    }

    private static Milestone askMilestone(List<Milestone> milestones) {

        sysOut("Choose milestone:");

        for (int i = 0; i < milestones.size(); i++) {
            sysOut(i + " " + milestones.get(i).getTitle());
        }

        System.out.print("Answer: ");

        int choice = new Scanner(System.in).nextInt();

        return milestones.get(choice);
    }

    private static List<Milestone> getMilestones(GitHubClient client, RepositoryId repositoryId, String status) throws IOException {
        MilestoneService milestoneService = new MilestoneService(client);
        return milestoneService.getMilestones(repositoryId, status);
    }

    private static RepositoryId createRepositoryId(Properties config) {
        return new RepositoryId(
                config.getProperty(CONFIG_REPO_OWNER),
                config.getProperty(CONFIG_REPO_NAME)
        );
    }

    private static GitHubClient createClient(Properties config) {

        GitHubClient client = new GitHubClient();

        client.setCredentials(
                config.getProperty(CONFIG_USERNAME),
                config.getProperty(CONFIG_PASSWORD)
        );
        return client;
    }

    private static void sysOut(String message) {
        System.out.println(message);
    }

    private static void printIssues(List<Issue> issues) {
        for (Issue issue : issues) {
            sysOut(String.format("- Issue %d: %s ", issue.getNumber(), issue.getTitle()));
        }
    }

    private static List<Issue> getIssues(GitHubClient client, RepositoryId repositoryId, Milestone milestone, String status) throws IOException {
        IssueService issueService = new IssueService(client);
        HashMap<String, String> parameters = new HashMap<String, String>();
        parameters.put("filter", "all");
        parameters.put("state", status);
        parameters.put("milestone", Integer.toString(milestone.getNumber()));
        return issueService.getIssues(repositoryId, parameters);
    }
}
