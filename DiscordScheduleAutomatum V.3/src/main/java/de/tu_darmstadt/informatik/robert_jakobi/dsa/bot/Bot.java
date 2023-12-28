package de.tu_darmstadt.informatik.robert_jakobi.dsa.bot;

import java.time.temporal.TemporalAccessor;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import javax.security.auth.login.LoginException;

import de.tu_darmstadt.informatik.robert_jakobi.dsa.struc.Poll;
import de.tu_darmstadt.informatik.robert_jakobi.dsa.util.DateFormat;
import de.tu_darmstadt.informatik.robert_jakobi.dsa.util.DateHelper;
import de.tu_darmstadt.informatik.robert_jakobi.dsa.util.ICalConstructor;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.JDABuilder;
import net.dv8tion.jda.api.OnlineStatus;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import net.dv8tion.jda.api.hooks.EventListener;
import net.dv8tion.jda.api.requests.GatewayIntent;
import net.dv8tion.jda.api.requests.RestAction;
import net.dv8tion.jda.api.utils.ChunkingFilter;
import net.dv8tion.jda.api.utils.MemberCachePolicy;

/**
 * Core of the DSA-Bot. Handles poll management and command calls.
 *
 * @author Big_Reddy
 * @since 17
 * @version 3.1
 */
public class Bot {
    /**
     * API interface
     */
    private final JDA jda;

    /**
     * Currently running polls
     */
    private final Map<String, Poll> runningPolls = Poll.loadPolls();

    /**
     * A list with all currently available commands with their parameters and
     * description.
     */
    private static final List<String[]> commands;

    static {
        commands = List.of(//
                new String[] { "!newpoll", "{name};{question};{options}+", "Creates a new poll" }, //
                new String[] { "!endpoll", "{name}<;keep>", "Evaluates <and deletes> given poll" }, //
                new String[] { "!delpoll", "{name}", "Deletes a existing poll" }, //
                new String[] { "!poke", "{name} <{emote}>", "Mentions all, that hadn't reacted to the poll <with emote>" }, //
                new String[] { "!who", "{id} <{emote}>", "Lists all, that reacted to the message <with emote>" }, //
                new String[] { "!help", "", "This :eyes:" }, //
                new String[] { "!ping", "", "Tests if bot is up and running" } //
        );
    }

    /**
     * Constructor of {@link Bot}. <br>
     * Initialises vital bot API and fail, if not possible.
     *
     * @param botToken
     *            Token of application this bot shall connect to
     */
    public Bot(final String botToken) {
        JDABuilder builder = JDABuilder.createDefault(botToken) //
                .setChunkingFilter(ChunkingFilter.ALL) //
                .setMemberCachePolicy(MemberCachePolicy.ALL) //
                .enableIntents(GatewayIntent.GUILD_MEMBERS) //
                .setAutoReconnect(true) //
                .addEventListeners((EventListener) (event -> {
                    if (event instanceof MessageReceivedEvent msgEvent) {
                        if (msgEvent.getAuthor().isBot()) return;
                        if (msgEvent.isFromGuild()) Bot.this.onServerMessageReceived(msgEvent);
                    }
                })) //
                .setStatus(OnlineStatus.ONLINE);
        // Login attempt: if it fails end program
        try {
            this.jda = builder.build();
        } catch (LoginException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Handles all command calls by user.
     *
     * @param event
     *            Event containing all data needed
     */
    private void onServerMessageReceived(final MessageReceivedEvent event) {
        if (!event.getChannel().getName().equals("schedule")) return;

        var typing = event.getChannel().sendTyping().submit();
        final String message = event.getMessage().getContentRaw();

        final List<String> answer = new ArrayList<>();
        System.out.printf("Request received by \"%s\" (%s): \"%s\"\n", //
                event.getAuthor().getName(), //
                event.getAuthor().getId(), //
                message);

        String[] message_elem = message.replaceFirst("[\n]", " ").split(" ", 2);
        if (message_elem.length < 2) {
            answer.add(switch (message_elem[0]) {
                case "!ping" -> "pong";
                case "!help" -> Bot.help();
                default -> "???";
            });
        } else {
            String[] elements = message_elem[1].split(";", 3);
            answer.add(switch (message_elem[0]) {
                case "!newpoll" -> this.newPoll(elements, event);
                case "!delpoll" -> this.deletePoll(elements[0], event);
                case "!endpoll" -> this.endPoll(elements[0], elements.length > 1 && elements[1].equals("keep"), event);
                case "!poke" -> this.poke(elements, event);
                case "!who" -> Bot.who(elements, event).stream() //
                        .filter(u -> !u.isBot()) //
                        .map(User::getName) //
                        .collect(Collectors.joining(" "));
                default -> "";
            });
        }
        final String reply = answer.stream() //
                .filter(Objects::nonNull) //
                .filter(Predicate.not(String::isBlank)) //
                .collect(Collectors.joining(System.lineSeparator()));

        // Fail-safe
        if (!event.getAuthor().isBot()) event.getMessage().delete().queue();
        if (!reply.isEmpty()) event.getChannel().sendMessage(reply).queue();
        typing.cancel(true);
    }

    /**
     * Send given message to Discord-User with given id.
     *
     * @param id
     *            ID of Discord-User
     * @param message
     *            Message to send
     */
    public void sendMessage(final String id, final String message) {
        this.jda.retrieveUserById(id) //
                .submit() //
                .thenCompose(u -> u.openPrivateChannel().submit()) //
                .thenCompose(c -> c.sendMessage(message).submit()) //
                .whenComplete((msg, err) -> System.out.println(err != null ? "Could not send message to user: " + id : ""));
    }

    /**
     * Return help message with all available commands.
     *
     * @return Message contend for help command
     */
    private static String help() {
        return """
                Hi :wave:,
                I am DSA, the **D**iscord **S**chedule **A**utomatum.

                My commands are:
                """ +  //
                commands.stream() //
                        .map("**%s** %s\n\t*\\~ %s*"::formatted) //
                        .collect(Collectors.joining("\n"));
    }

    /**
     * !newpoll command<br>
     * Creates a new poll from given data.
     * 
     * @param elements
     *            <br>
     *            [0]: poll name<br>
     *            [1]: poll question<br>
     *            [2]: poll options
     * @param event
     *            Event that triggered this response
     * @return An empty string
     */
    private String newPoll(String[] elements, MessageReceivedEvent event) {
        if (this.runningPolls.containsKey(elements[0])) return "Poll already exists";

        boolean next = elements[0].equals("next");
        Poll poll = new Poll(elements[0], //
                next ? "Wann habt ihr Zeit für die nächste Session?" : elements[1], //
                next ? DateHelper.nextWeekEnds() : elements[2].split(";"));
        this.runningPolls.put(poll.getName(), poll);
        Message message = event.getChannel().sendMessage("@everyone\n" + poll.toString()).complete();
        poll.setUUID(message.getId());
        Stream.generate(Poll.getReactions()) //
                .limit(Math.min(10, poll.getOptionCount())) //
                .map(message::addReaction) //
                .forEach(RestAction::queue);
        poll.saveToFile();
        return "";
    }

    /**
     * !delpoll command<br>
     * Deletes all data about given poll without concluding it.
     * 
     * @param pollName
     *            Name of the poll to delete
     * @param event
     *            Event that triggered this response
     * @return If poll got deleted
     */
    private String deletePoll(String pollName, MessageReceivedEvent event) {
        if (!this.runningPolls.containsKey(pollName)) return "Poll does not exist";
        Poll poll = this.runningPolls.remove(pollName);
        event.getChannel().retrieveMessageById(poll.getUUID()).complete().delete().queue();
        poll.delete();
        return "Poll deleted";
    }

    /**
     * !endpoll command<br>
     * Concludes given poll and generates an .ics file.
     * 
     * @param poll
     *            Name of the poll to conclude
     * @param keep
     * @param event
     *            Event that triggered this response
     * @return An empty string
     */
    private String endPoll(String pollName, boolean keep, MessageReceivedEvent event) {
        if (!this.runningPolls.containsKey(pollName)) return "Poll does not exist";
        List<String> answer = new ArrayList<>();
        String pokeReturn = this.poke(new String[] { pollName }, event);
        if (pokeReturn.contains(System.lineSeparator())) return pokeReturn;
        answer.add(pokeReturn);
        int count = event.getTextChannel().getMembers().size();
        Poll poll = this.runningPolls.get(pollName);
        Message message = event.getChannel().retrieveMessageById(poll.getUUID()).complete();
        TemporalAccessor date = message.getReactions() //
                .stream() //
                .filter(reaction -> count == reaction.getCount()) //
                .peek(r -> System.out.println(r.getReactionEmote().getAsReactionCode() + " " + r.getCount())) //
                .map(message.getReactions()::indexOf) //
                .sorted().map(index -> poll.getOption(index)) //
                .map(DateFormat.DATE_DE::parse).findFirst() //
                .orElse(null);
        if (Objects.nonNull(date)) {
            event.getChannel().sendFile(ICalConstructor.getICal(date), DateFormat.DATE_DE_FILE.format(date) + ".ics").queue();
            if (!keep) message.delete().queue();
            poll.delete();
            answer.add("@everyone Nächster Termin steht fest: " + DateFormat.DATE_DE.format(date));
        } else {
            answer.add("```diff\n- Kein Termin konnte gefunden werden```");
        }
        return String.join(System.lineSeparator(), answer);
    }

    /**
     * !who command<br>
     * Generates a list of all {@link User users}, that have reacted to given
     * message.
     * 
     * @param elements
     *            <br>
     *            [0]: message id<br>
     *            [1]: (opt.) filter emote
     * @param event
     *            Event that triggered this response
     * @return List of all users that reacted to given message
     */
    private static List<User> who(String[] elements, MessageReceivedEvent event) {
        return event.getChannel() //
                .retrieveMessageById(elements[0]) //
                .complete() //
                .getReactions() //
                .stream() //
                .filter(r -> elements.length > 1 ? r.getReactionEmote().getName().equals(elements[1]) : true) //
                .flatMap(r -> r.retrieveUsers().complete().stream()) //
                .distinct() //
                .toList();
    }

    /**
     * !poke command<br>
     * Generates reminder for participating in a given poll for channel members
     * not yet reacted to poll.
     * 
     * @param elements
     *            <br>
     *            [0]: poll name<br>
     *            [1]: (opt.) filter emote
     * @param event
     *            Event that triggered this response
     * @return Text of an answer message
     */
    private String poke(String[] elements, MessageReceivedEvent event) {
        if (!this.runningPolls.containsKey(elements[0])) return "Poll does not exist";
        List<String> answer = new ArrayList<>();
        answer.add("Es müssen die Umfrage noch ausfüllen:");
        elements[0] = this.runningPolls.get(elements[0]).getUUID();
        List<User> filter = who(elements, event);
        event.getTextChannel() //
                .getMembers() //
                .stream() //
                .map(Member::getUser) //
                .filter(Predicate.not(filter::contains)) //
                .peek(System.out::println) //
                .map(User::getAsMention) //
                .forEach(answer::add);
        return answer.size() == 1 //
                ? "Abstimmung abgeschlossen!" //
                : String.join(System.lineSeparator(), answer);
    }

    /**
     * Shuts down the bot.
     */
    public void shutdown() {
        this.jda.shutdown();
    }
}
