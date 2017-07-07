package com.github.wimve.githubissuesummary;

public enum Param {
    USERNAME("u","GitHub username"),
    PASSWORD("p","GitHub password",true),
    REPONAME("r","GitHub repository (owner/repository)"),
    TITLE_CLOSED("tc","Summary closed title"),
    TITLE_OPEN("to","Summary open title");

    private final String opt;
    private final String description;
    private final boolean password;

    Param(final String opt, final String description, boolean password) {
        this.opt = opt;
        this.description = description;
        this.password = password;
    }

    Param(final String opt, final String description) {
        this(opt,description,false);
    }

    public String getOpt() {
        return opt;
    }

    public String getDescription() {
        return description;
    }

    public boolean isPassword() {
        return password;
    }
}
