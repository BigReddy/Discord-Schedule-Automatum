package de.tu_darmstadt.informatik.robert_jakobi.dsa;

import java.io.IOException;
import java.io.PrintStream;
import java.nio.file.Files;
import de.tu_darmstadt.informatik.robert_jakobi.dsa.bot.Bot;
import de.tu_darmstadt.informatik.robert_jakobi.dsa.util.DateHelper;
import de.tu_darmstadt.informatik.robert_jakobi.dsa.util.FileManager;
import de.tu_darmstadt.informatik.robert_jakobi.dsa.util.SystemProperties;

/**
 * Initialises resource paths, sets up logging and starts {@link Bot}.
 * 
 * @author Big_Reddy
 * @since 11
 * @version 3
 *
 */
public class Main {
    static {
        setupLogging();
    }

    /**
     * Main method
     * 
     * @param args
     *            Arguments passed by environment
     * @throws IOException
     *             Thrown if issues on the creation of the file structure or on
     *             reading the bot key occurred
     */
    public static void main(String[] args) throws IOException {
        new Bot(FileManager.loadFromFile(SystemProperties.keyPath));
    }

    private static void setupLogging() {
        try {
            System.setOut(new PrintStream(Files
                    .createFile(FileManager.getPath(SystemProperties.logPath, DateHelper.getTimestamp() + ".log")).toFile()));
        } catch (IOException e) {
            e.printStackTrace();
        }

    }
}
