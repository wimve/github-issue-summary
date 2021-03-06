package com.github.wimve.githubissuesummary;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.Options;
import org.eclipse.egit.github.core.Issue;
import org.eclipse.egit.github.core.Milestone;
import org.eclipse.egit.github.core.PullRequest;
import org.eclipse.egit.github.core.RepositoryId;
import org.eclipse.egit.github.core.client.GitHubClient;
import org.eclipse.egit.github.core.service.IssueService;
import org.eclipse.egit.github.core.service.MilestoneService;

import java.io.IOException;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Scanner;
import java.util.function.Predicate;
import java.util.stream.Collectors;


public class Main {


    public static final String GITHUB_STATUS_OPEN = "open";
    public static final String GITHUB_STATUS_CLOSED = "closed";

    public static void main(String[] args) {

        try {

            // Parse arguments (only config file path at the moment)
            // create the command line parser
            CommandLineParser parser = new DefaultParser();
            Options options = getOptions();

            CommandLine arguments = parser.parse(options, args);

            // Create client to connect to GitHub
            GitHubClient client = createClient(getOrAsk(arguments, Param.USERNAME), getOrAsk(arguments, Param.PASSWORD));

            // Create repo ID
            RepositoryId repositoryId = parseRepositoryId(getOrAsk(arguments, Param.REPONAME));

            // Get the repo's open milestones
            List<Milestone> milestones = getMilestones(client, repositoryId, GITHUB_STATUS_OPEN);

            // Ask the user for the milestone interactively
            Milestone milestone = askMilestone(milestones);

            // Give the output some space
            sysOut("");

            // Output chosen milestone
            sysOutTitle("Milestone " + milestone.getTitle() + ":");
            sysOutTitle("Bij deze de actuele status van de issues:");

            // Output open Issues
            sysOutTitle(getOrDefault(arguments, Param.TITLE_OPEN, "Openstaande issues:"));
            printIssues(getIssues(client, repositoryId, milestone, GITHUB_STATUS_OPEN));

            // Output closed Issues
            sysOutTitle(getOrDefault(arguments, Param.TITLE_CLOSED, "Afgewerkt en klaar voor testen:"));
            printIssues(getIssues(client, repositoryId, milestone, GITHUB_STATUS_CLOSED));

            // Give the output some space
            sysOut("");

        } catch (Exception e) {
            System.err.println(e.getMessage());
        }
    }

    private static String getOrAsk(CommandLine arguments, Param param) {

        if (arguments.hasOption(param.getOpt())) {
            return arguments.getOptionValue(param.getOpt());
        } else {

            System.out.print("Please provide " + param.getDescription() + ": ");

            if (param.isPassword()) {
                return readPassword();
            } else {
                return new Scanner(System.in).next();
            }
        }
    }

    private static String getOrDefault(CommandLine arguments, Param param, String defaultValue) {
        return arguments.getOptionValue(param.getOpt(), defaultValue);
    }

    private static void sysOutTitle(String title) {
        sysOut("");
        sysOut(title);
    }

    private static Options getOptions() {
        // create CLI options
        Options options = new Options();
        for (Param p : Param.values()) {
            options.addOption(p.getOpt(), true, p.getDescription());
        }
        return options;
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

    private static RepositoryId parseRepositoryId(String repoKey) {
        if (repoKey != null) {

            // parse owner/repository format
            String[] keyElements = repoKey.split("/");

            if (keyElements.length == 2) {
                return new RepositoryId(
                        keyElements[0],  // owner
                        keyElements[1]   // repository
                );
            }
        }
        throw new IllegalArgumentException("Repository argument should use owner/repository format");
    }

    private static GitHubClient createClient(String username, String password) {
        return (new GitHubClient()).setCredentials(username, password);
    }

    private static void sysOut(String message) {
        System.out.println(message);
    }

    private static void printIssues(List<Issue> issues) {
        for (Issue issue : issues) {
            sysOut(String.format("- Issue #%d: %s ", issue.getNumber(), issue.getTitle()));
        }
        if (issues.isEmpty()) {
            sysOut("geen");
        }
    }

    private static List<Issue> getIssues(GitHubClient client, RepositoryId repositoryId, Milestone milestone, String status) throws IOException {
        return getIssues(client, repositoryId, milestone, status, false);
    }

    private static List<Issue> getIssues(GitHubClient client, RepositoryId repositoryId, Milestone milestone, String status, boolean includePullRequests) throws IOException {
        IssueService issueService = new IssueService(client);
        HashMap<String, String> parameters = new HashMap<>();
        parameters.put("filter", "all");
        parameters.put("state", status);
        parameters.put("milestone", Integer.toString(milestone.getNumber()));
        final List<Issue> issues = issueService.getIssues(repositoryId, parameters);
        if (!includePullRequests) {
            return issues.stream()
                    .filter((Issue issue) -> !isPullRequest(issue))
                    .sorted((Issue o1, Issue o2) -> Integer.compare(o2.getNumber(), o1.getNumber()))
                    .collect(Collectors.toList());
        }
        return issues;
    }

    private static String readPassword() {
        // Check if we are invoked from within console window
        if (System.console() == null) {
            throw new RuntimeException("Console not available");
        } else {
            return new String(System.console().readPassword());
        }
    }

    /**
     * https://developer.github.com/v3/issues/#list-issues
     * Note: GitHub's REST API v3 considers every pull request an issue, but not every issue is a pull request. For this reason, "Issues" endpoints may return both issues and pull requests in the response. You can identify pull requests by the pull_request key.
     *
     * Be aware that the id of a pull request returned from "Issues" endpoints will be an issue id. To find out the pull request id, use the "List pull requests" endpoint.
     */
    private static boolean isPullRequest(Issue issue) {
        final PullRequest pullRequest = issue.getPullRequest();
        return pullRequest != null && pullRequest.getUrl() != null;
    }
}
