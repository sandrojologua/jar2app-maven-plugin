# jar2app-maven-plugin

Maven plugin that creates an `.app` by specifying `.jar` file and embedding JDK (JRE).

Example usage:

```xml
<plugins>
    <plugin>
        <artifactId>maven-assembly-plugin</artifactId>
        <executions>
            <execution>
                <id>assembly</id>
                <phase>package</phase>
                <goals>
                    <goal>single</goal>
                </goals>
            </execution>
        </executions>
        <configuration>
            <archive>
                <manifest>
                    <mainClass>com.example.project.Main</mainClass>
                </manifest>
            </archive>
            <descriptorRefs>
                <descriptorRef>jar-with-dependencies</descriptorRef>
            </descriptorRefs>
        </configuration>
    </plugin>

    <plugin>
        <groupId>com.github.sandrojologua</groupId>
        <artifactId>jar2app-maven-plugin</artifactId>
        <version>1.2.0</version>
        <executions>
            <execution>
                <phase>package</phase>
                <goals>
                    <goal>bundle</goal>
                </goals>
            </execution>
        </executions>

        <configuration>
            <name>Example</name>
            <menuBarName>Example</menuBarName>
            <icon>src/main/resources/com/example/project/view/img/icon.icns</icon>
            <dockIcon>src/main/resources/com/example/project/view/img/icon.png</dockIcon>
            <!--This is a fat .jar, generated by maven-assembly-plugin or something else-->
            <jar>${project.build.directory}/${project.name}-${project.version}-jar-with-dependencies.jar</jar>
            <!--Ensure that bin/java is that java executable in this directory-->
            <!--If not specified default Java will be used without embedding JDK-->
            <jdk>/Users/sandrojologua/Library/Java/JavaVirtualMachines/liberica-17.0.2</jdk>
            <!--This is the interpreter using which java command will be executed. Optional, default is bash. -->
            <shellPath>/bin/zsh</shellPath>
            <!--Specify here any shell command before execution. Optional-->
            <cmdBeforeExec>source ~/.zshrc</cmdBeforeExec>
        </configuration>
    </plugin>
</plugins>
```

After you specify plugins you can run `mvn clean package` and `.app` file will reside in `target/` directory. 
Note that you should install `jar2app-maven-plugin` in your local repo manually. You don't have to use maven-assembly-plugin,
as long as you have correct fat jar specified, you're fine.
