package de.tu_darmstadt.informatik.robert_jakobi.dsa.struc;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InvalidClassException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Collectors;

import de.tu_darmstadt.informatik.robert_jakobi.dsa.util.DateHelper;
import de.tu_darmstadt.informatik.robert_jakobi.dsa.util.FileManager;
import de.tu_darmstadt.informatik.robert_jakobi.dsa.util.SystemProperties;

/**
 * Data class representing a poll. Self managed storing and loading to files for
 * persistent memory.
 * 
 * @author Big_Reddy
 * @since 17
 * @version 2
 */
public class Poll implements Serializable {

    /**
     * Serial id
     */
    private static final long serialVersionUID = -1061832211752473872L;

    /**
     * Unique message id containing poll.
     */
    private String messageId;

    private final UUID uuid = UUID.randomUUID();
    /**
     * Name of the poll
     */
    private final String name;
    /**
     * Question of the poll
     */
    private final String question;
    /**
     * Options of the poll
     */
    private final String[] options;

    /**
     * Constructor of a poll.
     * 
     * @param name
     *            Name of the poll
     * @param question
     *            Question to be polled
     * @param options
     *            Possible answer to poll
     */
    public Poll(String name, String question, String[] options) {
        this.name = name;
        this.question = question;
        this.options = options;
    }

    /**
     * Setter for {@link Poll#messageId poll}.
     * 
     * @param poll
     *            ID of associated poll message
     */
    public void setMessageId(String messageId) {
        this.messageId = messageId;
    }

    /**
     * Returns name of this poll.
     * 
     * @return Name of the poll
     */
    public String getName() {
        return this.name;
    }

    /**
     * ID of associated poll message.
     * 
     * @return Message ID of poll
     */
    public String getMessageId() {
        return this.messageId;
    }

    /**
     * Returns option of given index.
     * 
     * @param index
     *            Index of options to return
     * @return Chosen option
     */
    public String getOption(int index) {
        return this.options[index];
    }

    /**
     * Returns the amount of options available for this poll.
     * 
     * @return Amount of options
     */
    public int getOptionCount() {
        return this.options.length;
    }

    /**
     * Returns if ID of message associated is set and therefore poll is ready
     * for interactions.
     * 
     * @return If ID of message is set
     */
    public boolean isReady() {
        return this.messageId != null;
    }

    /**
     * {@inheritDoc}
     */
    public String toString() {
        Supplier<String> s = getReactions();
        return "*%s:*%n%n%s".formatted(this.question, //
                Arrays.stream(this.options) //
                        .map(opt -> s.get() + " **" + opt + "**") //
                        .collect(Collectors.joining(System.lineSeparator())));
    }

    /**
     * Returns a supplier of all number reaction symbols.
     * 
     * @return a supplier of all number reaction symbols
     */
    public static Supplier<String> getReactions() {
        return new Supplier<String>() {
            private int i;

            @Override
            public String get() {
                return this.i++ + "\uFE0F\u20E3";
            }
        };
    }

    /**
     * Deactivates this poll and removes its persistent representation.
     * 
     * @return If removal was successful
     */
    public boolean delete() {
        this.messageId = null;
        return this.getPath() //
                .toFile() //
                .renameTo(this.getDeletePath().toFile());
    }

    /**
     * Creates a persistent copy of the state of this object.
     * 
     * @return If saving was successful
     */
    public boolean saveToFile() {
        try (var out = new ObjectOutputStream(new FileOutputStream(getPath().toFile()))) {
            out.writeObject(this);
            return true;
        } catch (Exception e) {
            e.printStackTrace();
            System.out.println("Could not save poll");
            return false;
        }
    }

    /**
     * Loads a given poll from persistent storage into memory.
     * 
     * @param file
     *            Location of object file
     * @return Loaded poll
     */
    public static Poll loadFromFile(File file) {
        try (var in = new ObjectInputStream(new FileInputStream(file))) {
            if (in.readObject() instanceof Poll poll) {
                return poll;
            }
            throw new InvalidClassException("");
        } catch (IOException | ClassNotFoundException e) {
            System.out.println("Poll could not be recovered");
            return null;
        }
    }

    /**
     * Loads all active polls from persistent storage into memory.
     * 
     * @return All active polls loaded
     */
    public static Map<String, Poll> loadPolls() {
        try {
            return Files.list(FileManager.getPath(SystemProperties.pollsPath)) //
                    .map(Path::toFile) //
                    .map(Poll::loadFromFile) //
                    .filter(Objects::nonNull) //
                    .collect(Collectors.toMap(Poll::getName, Function.identity()));
        } catch (IOException e) {
            System.out.println("No polls could not be recovered");
            e.printStackTrace();
            return new HashMap<>();
        }
    }

    private Path getPath() {
        return FileManager.getPath(SystemProperties.pollsPath, this.name);
    }

    private Path getDeletePath() {
        return FileManager.getPath(SystemProperties.finishedPollsPath, DateHelper.getTimestamp());
    }

    public UUID getUuid() {
        return this.uuid;
    }
}
