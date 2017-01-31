package online.pizzacrust.combinedremapper;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;

import net.techcable.srglib.format.MappingsFormat;
import net.techcable.srglib.mappings.Mappings;

import org.apache.maven.execution.MavenSession;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.BuildPluginManager;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.Component;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.project.MavenProject;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.twdata.maven.mojoexecutor.MojoExecutor.artifactId;
import static org.twdata.maven.mojoexecutor.MojoExecutor.configuration;
import static org.twdata.maven.mojoexecutor.MojoExecutor.element;
import static org.twdata.maven.mojoexecutor.MojoExecutor.executeMojo;
import static org.twdata.maven.mojoexecutor.MojoExecutor.executionEnvironment;
import static org.twdata.maven.mojoexecutor.MojoExecutor.groupId;
import static org.twdata.maven.mojoexecutor.MojoExecutor.name;
import static org.twdata.maven.mojoexecutor.MojoExecutor.plugin;
import static org.twdata.maven.mojoexecutor.MojoExecutor.version;

@Mojo(name = "applyss", defaultPhase = LifecyclePhase.PACKAGE)
public class SpecialSourceApplyMojo extends AbstractMojo {

    @Component
    private MavenProject mavenProject;

    @Component
    private MavenSession mavenSession;

    @Component
    private BuildPluginManager pluginManager;

    @Parameter(required = true)
    private String[] srgs;

    private List<File> getValidSrgs() {
        List<File> files = new ArrayList<File>();
        for (String srgPath : srgs) {
            File possibleFile = new File(srgPath);
            if (!possibleFile.isDirectory() && possibleFile.getName().endsWith(".srg")) {
                System.out.println(srgPath + " has been inputted into valid srgs.");
                files.add(possibleFile);
            }
        }
        return files;
    }

    private Mappings parsePackages(File file) {
        try {
            List<String> lines = Files.readAllLines(file.toPath());
            Map<String, String> packageMappings = new HashMap<>();
            lines.forEach((line) -> {
                if (line.startsWith("PK")) {
                    String[] splitted = line.split(" ");
                    packageMappings.put(splitted[1], splitted[2]);
                }
            });
            return Mappings.createPackageMappings(ImmutableMap.copyOf(packageMappings));
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    private Mappings getMappings(File file) {
        try {
            return MappingsFormat.SEARGE_FORMAT.parseFile(file);
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }

    private List<Mappings> getValidMappings(List<File> files) {
        List<Mappings> mappings = new ArrayList<Mappings>();
        for (File srgFile : files) {
            Mappings possibleMappings = getMappings(srgFile);
            if (possibleMappings != null) {
                mappings.add(possibleMappings);
            }
            Mappings packageMappings = parsePackages(srgFile);
            if (packageMappings != null) {
                mappings.add(packageMappings);
            }
        }
        return mappings;
    }

    private Mappings getCombinedMappings() {
        return Mappings.chain(ImmutableList.copyOf(getValidMappings(getValidSrgs())));
    }

    public void execute() throws MojoExecutionException, MojoFailureException {
        File mergedFile = new File("merged.srg");
        try {
            MappingsFormat.SEARGE_FORMAT.writeToFile(getCombinedMappings(), mergedFile);
        } catch (IOException e) {
            e.printStackTrace();
            throw new MojoFailureException(e.getMessage());
        }
        executeMojo(plugin(
                groupId("net.md-5"),
                artifactId("specialsource-maven-plugin"),
                version("1.2.1")
        ), "remap", configuration(
                element(name("remappedArtifactAttached"), "false"),
                element(name("srgIn"), "merged.srg")
        ), executionEnvironment(mavenProject, mavenSession, pluginManager));
    }

}
