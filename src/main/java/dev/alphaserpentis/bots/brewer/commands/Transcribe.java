package dev.alphaserpentis.bots.brewer.commands;

import dev.alphaserpentis.bots.brewer.handler.commands.brew.AudioHandler;
import dev.alphaserpentis.bots.brewer.handler.openai.OpenAIHandler;
import dev.alphaserpentis.coffeecore.commands.ButtonCommand;
import dev.alphaserpentis.coffeecore.data.bot.CommandResponse;
import io.reactivex.rxjava3.annotations.NonNull;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.entities.MessageEmbed;
import net.dv8tion.jda.api.entities.UserSnowflake;
import net.dv8tion.jda.api.entities.channel.concrete.VoiceChannel;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.events.interaction.component.ButtonInteractionEvent;
import net.dv8tion.jda.api.exceptions.InsufficientPermissionException;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.commands.build.SubcommandData;
import net.dv8tion.jda.api.interactions.components.ItemComponent;
import net.dv8tion.jda.api.interactions.components.buttons.ButtonStyle;
import net.dv8tion.jda.api.managers.AudioManager;

import java.io.IOException;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class Transcribe extends ButtonCommand<MessageEmbed, SlashCommandInteractionEvent> {
    private static ExecutorService executorService = Executors.newCachedThreadPool();

    public Transcribe() {
        super(
                new BotCommandOptions()
                        .setName("transcribe")
                        .setDescription("Transcribe audio!")
                        .setOnlyEmbed(true)
                        .setDeferReplies(true)
        );

        addButton("summarize", ButtonStyle.PRIMARY, "Summarize", false);
        addButton("optout", ButtonStyle.DANGER, "Opt Out", false);
    }

    @Override
    public void runButtonInteraction(@NonNull ButtonInteractionEvent event) {

    }

    @Override
    @NonNull
    public Collection<ItemComponent> addButtonsToMessage(@NonNull SlashCommandInteractionEvent event) {
        return List.of();
    }

    @Override
    public CommandResponse<MessageEmbed> runCommand(long userId, @NonNull SlashCommandInteractionEvent event) {
        EmbedBuilder eb = new EmbedBuilder();

        eb.setTitle("Transcribe");
        if(event.getSubcommandName().equalsIgnoreCase("vc")) {
            try {
                handleTranscribeVC(eb, event);
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        } else if(event.getSubcommandName().equalsIgnoreCase("url")) {
            handleTranscribeUrl(eb, event);
        } else {
            eb.setDescription("Invalid subcommand!");
        }

        return new CommandResponse<>(eb.build(), isOnlyEphemeral());
    }

    @Override
    public void updateCommand(@NonNull JDA jda) {
        SubcommandData vc = new SubcommandData("vc", "Join a VC and transcribe the audio")
                .addOption(OptionType.CHANNEL, "channel", "The channel to join", true);
        SubcommandData url = new SubcommandData("url", "Transcribe an audio file from a URL")
                .addOption(OptionType.STRING, "url", "The URL of the audio file to transcribe", true);

        jda
                .upsertCommand(getName(), getDescription())
                .addSubcommands(vc, url)
                .queue(r -> setCommandId(r.getIdLong()));
    }

    private void handleTranscribeUrl(@NonNull EmbedBuilder eb, @NonNull SlashCommandInteractionEvent event) {
        eb.setDescription(
                OpenAIHandler.getAudioTranscription(
                        event.getOption("url").getAsString()
                ).text()
        );
    }

    private void handleTranscribeVC(@NonNull EmbedBuilder eb, @NonNull SlashCommandInteractionEvent event) throws InterruptedException, IOException {
        VoiceChannel vc = event.getOption("channel").getAsChannel().asVoiceChannel();

        AudioManager manager = vc.getGuild().getAudioManager();
        AudioHandler handler = new AudioHandler(30);
        HashMap<Long, byte[]> audioData;

        System.out.println(vc.getGuild().getMemberCache());

        manager.setReceivingHandler(handler);
        try {
            manager.openAudioConnection(vc);

            while(handler.canReceiveEncoded()) {
                Thread.sleep(1000);
            }

            audioData = (HashMap<Long, byte[]>) handler.getAudioData();
            manager.closeAudioConnection();

            for(long userId: audioData.keySet()) {
                eb.addField(
                        vc.getGuild().getMember(UserSnowflake.fromId(userId)).getEffectiveName(),
                        transcribeUser(audioData.get(userId), userId),
                        false
                );
            }
        } catch(InsufficientPermissionException ignored) {
            eb.setDescription("I don't have permission to join that VC!");
        }
    }

    @NonNull
    private String transcribeUser(@NonNull byte[] audioData, long userId) {
        return OpenAIHandler.getVoiceTranscription(audioData, Long.toString(userId)).text();
    }
}
