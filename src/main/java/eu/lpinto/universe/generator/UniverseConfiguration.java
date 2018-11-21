package eu.lpinto.universe.generator;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

/**
 * System configuration based on a properties file.
 *
 * @author Luis Pinto <code>- mail@lpinto.eu</code>
 */
public class UniverseConfiguration {

    static public final String UNIVERSE_FOLDER = ".universe-framework";
    static public final String FILE_NAME = "universe.properties";

    private final List<String> generatorFilesToSlip = new ArrayList<>(10);

    public UniverseConfiguration(final String filePath) {
        try {
            Files.lines(Paths.get(filePath)).forEach(line -> {
                if (line.startsWith("generator.skip:")) {
                    String value = line.split(":")[1].trim();

                    if (value.contains(",")) {
                        for (String s : line.split(":")[1].split(",")) {
                            generatorFilesToSlip.add(s.trim());
                        }
                    } else {
                        generatorFilesToSlip.add(value);
                    }
                }
            });

        } catch (IOException | UncheckedIOException ex) {
            System.out.println("Cannot parse file: " + filePath);
        }
    }

    public List<String> getGeneratorFilesToSlip() {
        return generatorFilesToSlip;
    }
}
