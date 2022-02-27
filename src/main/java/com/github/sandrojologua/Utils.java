package com.github.sandrojologua;

import java.nio.file.Path;

final class Utils {
    private Utils() {
        throw new AssertionError();
    }

    static final String DEFAULT_BUNDLE_NAME = "Example";

    static String getCopyInfoMsg(String name, Path source, Path destination) {
        return String.format("Copying %s from %s to %s", name, source, destination);
    }

    static String getCopyErrMsg(String name, Path source, Path destination) {
        return String.format("Couldn't copy %s from %s to %s", name, source, destination);
    }

    static String getNotSpecMsg(String missing, String def) {
        return String.format("%s not specified. %s will be used",  missing, def);
    }
}
