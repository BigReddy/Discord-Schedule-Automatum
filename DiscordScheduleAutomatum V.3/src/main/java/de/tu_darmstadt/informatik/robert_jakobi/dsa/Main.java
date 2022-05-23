package de.tu_darmstadt.informatik.robert_jakobi.dsa;

import java.io.IOException;
import java.io.PrintStream;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.attribute.BasicFileAttributes;
import java.nio.file.attribute.FileAttribute;

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
        setupFileStructure();
        System.setOut(new PrintStream(Files.createFile(Paths.get("rec", "logs", System.currentTimeMillis() + ".log")).toFile()));
        new Bot(Files.readString(Paths.get("rec", "key")));
    }

    /**
     * Creates nessesary file structure for Bot.
     * 
     * @throws IOException
     *             Thrown if issues on the creation of the file structure
     */
    private static void setupFileStructure() throws IOException {
        Files.createDirectories(Paths.get("rec", "logs"));
        Files.createDirectories(Paths.get("rec", "polls"));
        var key = Paths.get("rec", "key");
        if (!Files.exists(key)) {
            Files.createFile(key);
            System.out.println("Bot-Key needed! Location: " + key.toString());
            System.exit(0);
        }
        var invite_data = Paths.get("rec", "invite.data");
        if (!Files.exists(invite_data)) {
            Files.createFile(invite_data);
        }
    }
}
