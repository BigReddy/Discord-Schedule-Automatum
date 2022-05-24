package de.tu_darmstadt.informatik.robert_jakobi.dsa.bot;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import javax.security.auth.login.LoginException;

import de.tu_darmstadt.informatik.robert_jakobi.dsa.struc.Poll;
import de.tu_darmstadt.informatik.robert_jakobi.dsa.util.DateHelper;
import de.tu_darmstadt.informatik.robert_jakobi.dsa.util.ICalConstructor;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.JDABuilder;
import net.dv8tion.jda.api.OnlineStatus;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.PrivateChannel;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.events.GenericEvent;
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
 * @version 3
 */
public class Bot {
    /**
     * API interface
     */
    private final JDA jda;

    /**
     * Currently running polls
     */
    private final List<Poll> runningPolls = Poll.loadPolls();

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
                .addEventListeners(new EventListener() {
                    @Override
                    public void onEvent(GenericEvent event) {
                        if (event instanceof MessageReceivedEvent msgEvent) {
                            if (msgEvent.isFromGuild())
                                Bot.this.onMessageReceived(msgEvent);
                            else if (!msgEvent.getAuthor().isBot())
                                msgEvent.getChannel().sendMessage("REDRUM REDRUM REDRUM").queue();
                        }
                    }
                }) //
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
    public void onMessageReceived(final MessageReceivedEvent event) {
        if (!event.getChannel().getName().equals("schedule")) return;
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
                default -> "";
            });
        } else {
            String[] elements = message_elem[1].split(";", 3);
            answer.add(switch (message_elem[0]) {
                case "!newpoll" -> this.newPoll(elements, event);
                case "!delpoll" -> this.deletePoll(elements[0], event);
                case "!endpoll" -> this.endPoll(elements[0], elements.length > 1 && elements[1].equals("keep"), event);
                case "!poke" -> this.poke(elements, event);
                case "!who" -> Bot.who(elements, event).stream() //
                        .map(User::getName) //
                        .collect(Collectors.joining(" "));
                default -> "";
            });
        }
        final String reply = answer.stream() //
                .filter(s -> !s.isBlank()) //
                .collect(Collectors.joining("\n"));

        // Fail save
        if (!event.getAuthor().isBot() && event.getChannel().getName().equals("schedule")) event.getMessage().delete().queue();
        if (!reply.isEmpty()) event.getChannel().sendMessage(reply).queue();
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
        try {
            final PrivateChannel channel = this.jda.getUserById(id).openPrivateChannel().complete();
            // if
            // (!channel.retrieveMessageById(channel.getLatestMessageId()).complete().getContentRaw().equals(message))
            channel.sendMessage(message).queue();
        } catch (NullPointerException e) {
            e.printStackTrace();
            System.out.println("Could not send message to user: " + id);
        }
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
        if (this.runningPolls.stream().map(Poll::getName).anyMatch(elements[0]::equals)) return "Poll already exists";

        boolean next = elements[0].equals("next");
        Poll poll = new Poll(elements[0], //
                next ? "Wann habt ihr Zeit für die nächste Session?" : elements[1], //
                next ? DateHelper.nextWeekEnds() : elements[2].split(";"));
        this.runningPolls.add(poll);
        Message message = event.getChannel().sendMessage("@everyone\n" + poll.toString()).complete();
        poll.setPoll(message.getId());
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
     * @param poll
     *            Name of the poll to delete
     * @param event
     *            Event that triggered this response
     * @return If poll got deleted
     */
    private String deletePoll(String poll, MessageReceivedEvent event) {
        if (!this.runningPolls.stream().map(Poll::getName).anyMatch(poll::equals)) return "Poll does not exist";
        this.runningPolls.removeAll(this.runningPolls.stream() //
                .filter(p -> p.getName().equals(poll)) //
                .filter(Poll::isReady) //
                .peek(p -> event.getChannel().retrieveMessageById(p.getPoll()).complete().delete().queue()) //
                .peek(Poll::delete) //
                .toList());
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
    private String endPoll(String poll, boolean keep, MessageReceivedEvent event) {
        if (!this.runningPolls.stream().map(Poll::getName).anyMatch(poll::equals)) return "Poll does not exist";
        this.runningPolls.removeAll( //
                this.runningPolls.stream() //
                        .filter(p -> p.getName().equals(poll)) //
                        .filter(Poll::isReady) //
                        .peek(p -> {
                            Message m = event.getChannel().retrieveMessageById(p.getPoll()).complete();
                            String date = m.getReactions() //
                                    .stream() //
                                    .sorted((a, b) -> (b.getCount() - a.getCount())) //
                                    .peek(r -> System.out.println(r.getReactionEmote().getAsReactionCode() + " " + r.getCount())) //
                                    .reduce((t, u) -> t) //
                                    .map(r -> m.getReactions().indexOf(r)) //
                                    .map(index -> p.getOption(index)) //
                                    .get();
                            event.getChannel().sendFile(ICalConstructor.getICal(date), date.replaceAll("[.]", "_") + ".ics")
                                    .queue();
                            if (!keep) m.delete().queue();
                        }) //
                        .peek(Poll::delete) //
                        .toList());
        return "";
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
        event.getGuild().findMembers(m -> m.hasPermission(event.getGuildChannel(), Permission.MESSAGE_HISTORY)).onSuccess(System.out::println);

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
        List<String> answer = new ArrayList<>();
        answer.add("Es müssen die Umfrage noch ausfüllen:");
        elements[0] = this.runningPolls.stream() //
                .filter(p -> p.getName().equals(elements[0])) //
                .filter(Poll::isReady) //
                .findAny() //
                .map(Poll::getPoll) //
                .orElse("0");
        List<User> filter = who(elements, event);
        event.getTextChannel() //
                .getMembers() //
                .stream() //
                .map(Member::getUser) //
                .peek(System.out::println) //
                .filter(u -> !filter.contains(u)) //
                .peek(System.out::println) //
                .map(User::getAsMention) //
                .forEach(answer::add);
        if (answer.size() == 1) answer.replaceAll(str -> "Abstimmung abgeschlossen!");
        return answer.stream().collect(Collectors.joining("\n"));
    }

    /**
     * Shuts down the bot.
     */
    public void shutdown() {
        this.jda.shutdown();
    }
}
