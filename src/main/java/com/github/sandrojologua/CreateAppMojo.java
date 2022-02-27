package com.github.sandrojologua;

import com.dd.plist.NSDictionary;
import com.dd.plist.PropertyListParser;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.project.MavenProject;
import org.codehaus.plexus.util.FileUtils;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.attribute.PosixFilePermission;

import static com.github.sandrojologua.Utils.*;

@Mojo(name = "bundle", defaultPhase = LifecyclePhase.PACKAGE)
public final class CreateAppMojo extends AbstractMojo {
    @Parameter(defaultValue = "${project}", required = true, readonly = true)
    private MavenProject project;

    @Parameter private String name;
    @Parameter private String shellPath;
    @Parameter private String jdk;
    @Parameter private String jar;
    @Parameter private String icon;
    @Parameter private String menuBarName;
    @Parameter private String dockIcon;

    private String bundleName;
    private Path targetDir = Paths.get("target");
    private Path resourcesDir;
    private Path macOSDir;
    private Path jdkDir;
    private Path jarDir;
    private Path infoFile;

    public void execute() throws MojoExecutionException {
        bundleName = name == null ? DEFAULT_BUNDLE_NAME : name;
        targetDir = Paths.get("target");
        Path bundleDir = createDir(targetDir.resolve(bundleName + ".app"));
        Path contentsDir = createDir(bundleDir.resolve("Contents"));
        resourcesDir = createDir(contentsDir.resolve("Resources"));
        macOSDir = createDir(contentsDir.resolve("MacOS"));
        jdkDir = createDir(contentsDir.resolve("jdk"));
        jarDir = createDir(contentsDir.resolve("app"));

        infoFile = createFile(contentsDir.resolve("Info.plist"));

        writeInfoPlist();
        copyJar();
        createScript();
    }

    private Path createDir(Path path) throws MojoExecutionException {
        getLog().info("Creating directory " + path);
        try {
            return Files.createDirectory(path);
        } catch (IOException e) {
            throw new MojoExecutionException("Couldn't create directory " + path, e);
        }
    }

    private Path createFile(Path path) throws MojoExecutionException {
        getLog().info("Creating file " + path);
        try {
            return Files.createFile(path);
        } catch (IOException e) {
            throw new MojoExecutionException("Couldn't create file " + path, e);
        }
    }

    private void writeInfoPlist() throws MojoExecutionException {
        NSDictionary root = new NSDictionary();
        if (menuBarName != null) {
            root.put("CFBundleName", menuBarName);
        } else {
            getLog().warn(getNotSpecMsg("Menu bar", "Bundle name"));
            root.put("CFBundleName", bundleName);
        }

        //write into resources dir
        if (icon != null) {
            if (icon.endsWith(".icns")) {
                Path source = Paths.get(icon);
                Path destination = resourcesDir.resolve("icon.icns");
                getLog().info(getCopyInfoMsg("icon", source, destination));
                try {
                    Files.copy(source, destination);
                } catch (IOException e) {
                    throw new MojoExecutionException(getCopyErrMsg("icon", source, destination), e);
                }

                root.put("CFBundleIconFile", "icon");
                getLog().info("Saving info property list");
                try {
                    PropertyListParser.saveAsXML(root, infoFile.toFile());
                } catch (IOException e) {
                    throw new MojoExecutionException("Couldn't save info property list", e);
                }

            } else {
                getLog().warn("Icon must be .icns file. Default icon will be used");
            }

        } else {
            getLog().warn(getNotSpecMsg("Icon", "Default"));
        }
    }

    private void copyJar() throws MojoExecutionException {
        Path destination = jarDir.resolve(bundleName + ".jar");
        if (jar == null) {
            String jarFileName = project.getArtifactId() + "-" + project.getVersion() + ".jar";
            Path source = targetDir.resolve(jarFileName);

            getLog().warn(getNotSpecMsg("JAR", source.toString()));
            getLog().info(getCopyInfoMsg("JAR", source, destination));
            try {
                Files.copy(source, destination);
            } catch (IOException e) {
                throw new MojoExecutionException(getCopyErrMsg("JAR", source, destination), e);
            }
        } else {
            Path source = Paths.get(jar);
            getLog().info(getCopyInfoMsg("JAR", source, destination));
            try {
                Files.copy(source, destination);
            } catch (IOException e) {
                throw new MojoExecutionException(getCopyErrMsg("JAR", source, destination), e);
            }
        }
    }

    private void createScript() throws MojoExecutionException {
        Path scriptFile = createFile(macOSDir.resolve(bundleName));

        String java = "java";
        if (jdk != null) {
            Path jdkSource = Path.of(jdk);
            if (Files.isExecutable(jdkSource.resolve("bin").resolve("java"))) {
                getLog().info(getCopyInfoMsg("JDK", jdkSource, jdkDir));
                try {
                    FileUtils.copyDirectoryStructure(jdkSource.toFile(), jdkDir.toFile());
                    java = "\"$SCRIPTDIR\"/../jdk/bin/java";
                } catch (IOException e) {
                    throw new MojoExecutionException(getCopyErrMsg("JDK", jdkSource, jdkDir), e);
                }

            } else {
                getLog().error("Could not find Java executable in JDK. Default Java will be used without embedding it");
            }
        } else {
            getLog().warn(getNotSpecMsg("JDK", "Default Java") + " without embedding it");
        }

        String javaArgs = "";
        if (dockIcon != null) {
            Path source = Paths.get(dockIcon);
            Path destination = resourcesDir.resolve("dock-icon.png");
            getLog().info(getCopyInfoMsg("dock icon", source, destination));
            try {
                Files.copy(source, destination);
                javaArgs = "-Xdock:icon=\"$SCRIPTDIR\"/../Resources/dock-icon.png";
            } catch (IOException e) {
                throw new MojoExecutionException(getCopyErrMsg("dock icon", source, destination), e);
            }
        } else {
            getLog().warn(getNotSpecMsg("Dock icon", "Default one"));
        }

        String shell = "/bin/bash";
        if (shellPath != null) {
            if (Files.isExecutable(Paths.get(shellPath))) {
                shell = shellPath;
            } else {
                throw new MojoExecutionException("Specified shell path isn't pointing to an executable");
            }
        }

        final String script = String.format("""
                #!%s
                SCRIPTDIR=$(dirname $0)
                %s %s -jar "$SCRIPTDIR"/../app/%s.jar
                """, shell, java, javaArgs, bundleName);

        getLog().info("Writing shell script with interpreter " + shell);
        try {
            Files.writeString(scriptFile, script);
        } catch (IOException e) {
            throw new MojoExecutionException("Couldn't write a shell script", e);
        }

        getLog().info("Adding execute permission");
        try {
            var perms = Files.getPosixFilePermissions(scriptFile);
            perms.add(PosixFilePermission.OWNER_EXECUTE);
            Files.setPosixFilePermissions(scriptFile, perms);
        } catch (IOException e) {
            throw new MojoExecutionException("Couldn't add executable permission", e);
        }
    }
}
