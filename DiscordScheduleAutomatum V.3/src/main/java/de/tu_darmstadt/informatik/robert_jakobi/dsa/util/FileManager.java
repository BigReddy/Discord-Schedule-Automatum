package de.tu_darmstadt.informatik.robert_jakobi.dsa.util;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.stream.Collectors;

public class FileManager implements SystemProperties {

    static final String ICAL_FORMAT_FILE = "template.ical";
    static final String ICAL_DATA_FILE = "invite.data";

    static {
        try {
            setupFileStructure();
        } catch (IOException e) {
            e.printStackTrace();
            System.exit(-1);
        }
    }

    private FileManager() {}

    public static Path getPath(String... elements) {
        return Path.of(resourcePath, elements);
    }

    public static String loadFromFile(String... elements) {
        try {
            return Files.lines(getPath(elements)).collect(Collectors.joining(System.lineSeparator()));
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
    }

    public static void saveToFile(String subFolder, String fileName, String fileContent) {
        try {
            Files.write(Path.of(resourcePath, subFolder, fileName), fileContent.getBytes());
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * Creates nessesary file structure for Bot.
     * 
     * @throws IOException
     *             Thrown if issues on the creation of the file structure
     *             occured
     */
    private static void setupFileStructure() throws IOException {
        Files.createDirectories(getPath(logPath));
        Files.createDirectories(getPath(pollsPath));

        validateRequiredFile(keyPath, "Bot-Key needed! Location: ", true);
        validateRequiredFile(ICAL_FORMAT_FILE, "Please provide required data for ICal invite in", false);
        validateRequiredFile(ICAL_DATA_FILE, "Please provide required data for ICal invite in", false);
    }

    private static void validateRequiredFile(String fileName, String errorMessage, boolean required) throws IOException {
        var path = getPath(fileName);
        if (!Files.exists(path)) {
            Files.createFile(path);
            System.out.println(errorMessage + path.toString());
            if (required) System.exit(0);
        }
    }
}
