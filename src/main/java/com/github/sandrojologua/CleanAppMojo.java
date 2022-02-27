package com.github.sandrojologua;

import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.codehaus.plexus.util.FileUtils;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;

import static com.github.sandrojologua.Utils.DEFAULT_BUNDLE_NAME;

@Mojo(name = "clean", defaultPhase = LifecyclePhase.PACKAGE)
public final class CleanAppMojo extends AbstractMojo {
    @Parameter private String name;

    private final Path targetDir = Paths.get("target");

    @Override
    public void execute() {
        String bundleName = name == null ? DEFAULT_BUNDLE_NAME : name;
        try {
            FileUtils.deleteDirectory(targetDir.resolve(bundleName + ".app").toFile());
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
