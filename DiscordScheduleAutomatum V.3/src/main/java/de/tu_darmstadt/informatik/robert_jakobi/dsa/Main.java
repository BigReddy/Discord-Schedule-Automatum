package de.tu_darmstadt.informatik.robert_jakobi.dsa;

import java.io.IOException;
import java.io.PrintStream;
import java.nio.file.Files;
import java.nio.file.Paths;

import de.tu_darmstadt.informatik.robert_jakobi.dsa.bot.Bot;

/**
 * Initialises resource paths, sets up logging and starts {@link Bot}.
 * 
 * @author Big_Reddy
 * @since 11
 * @version 2
 *
 */
public class Main {
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
        Files.createDirectories(Paths.get("rec", "logs"));
        Files.createDirectories(Paths.get("rec", "polls"));
        System.setOut(new PrintStream(Files.createFile(Paths.get("rec", "logs", System.currentTimeMillis() + ".log")).toFile()));
        new Bot(Files.readString(Paths.get("rec", "key")));
    }
}
