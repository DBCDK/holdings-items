package dk.dbc.holdingsitems.facade;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.junit.jupiter.api.Test;
import org.testcontainers.DockerClientFactory;
import org.testcontainers.images.builder.ImageFromDockerfile;
import org.testcontainers.utility.DockerImageName;

import static org.junit.jupiter.api.Assertions.assertTrue;

public class HoldingItemsUpdateServletIT {

    public static final String IMAGE = "holdings-items-update-facade-" + System.getProperty("docker.image.version", "current") +
                                       ":" + System.getProperty("docker.image.tag", "latest");

    @Test
    public void buildDockerImage() throws Exception {
        System.out.println("buildDockerImage");
        dockerPull(getDockerBaseImage());
        String imageName = new ImageFromDockerfile(IMAGE, false)
                .withFileFromPath(".", Path.of("."))
                .withFileFromPath("Dockerfile", Path.of("target/docker/Dockerfile"))
                .get();
        assertTrue(true);
    }

    public static String getDockerBaseImage() {
        try (FileInputStream fis = new FileInputStream("target/docker/Dockerfile")) {
            String content = new String(fis.readAllBytes(), StandardCharsets.UTF_8);
            Matcher matcher = Pattern.compile("^FROM\\s+(\\S+)\\s*$", Pattern.MULTILINE).matcher(content);
            if (!matcher.find())
                throw new IllegalArgumentException("Cannot find FROM line in dockerfile");
            return matcher.group(1);
        } catch (IOException ex) {
            throw new UncheckedIOException(ex);
        }
    }

    public static void dockerPull(String image) {
        try {
            DockerImageName from = DockerImageName.parse(image);
            if (!DockerClientFactory.instance().client().pullImageCmd(from.getUnversionedPart()).withTag(from.getVersionPart()).start().awaitCompletion(30, TimeUnit.SECONDS)) {
                throw new IllegalStateException("Could not pull docker image: " + image);
            }
        } catch (InterruptedException ex) {
            throw new IllegalStateException("Could not pull docker image: " + image, ex);
        }
    }
}
