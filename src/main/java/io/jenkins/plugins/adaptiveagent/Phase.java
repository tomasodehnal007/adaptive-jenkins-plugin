package io.jenkins.plugins.adaptiveagent;

/** The moment in a build at which an entry's task runs. */
public enum Phase {
    /** Before the build steps start. */
    PRE_BUILD,
    /** Periodically while the build is running. */
    DURING_BUILD,
    /** After the build has finished. */
    POST_BUILD
}
