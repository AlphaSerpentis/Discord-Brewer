package dev.alphaserpentis.bots.brewer.commands;

import dev.alphaserpentis.bots.brewer.data.brewer.BrewerServerData;
import dev.alphaserpentis.bots.brewer.data.brewer.ServiceType;
import dev.alphaserpentis.bots.brewer.handler.bot.AnalyticsHandler;
import dev.alphaserpentis.bots.brewer.handler.bot.BrewerServerDataHandler;
import dev.alphaserpentis.bots.brewer.handler.commands.audio.VoiceHandler;
import dev.alphaserpentis.bots.brewer.handler.openai.OpenAIHandler;
import dev.alphaserpentis.coffeecore.commands.ButtonCommand;
import dev.alphaserpentis.coffeecore.data.bot.CommandResponse;
import io.reactivex.rxjava3.annotations.NonNull;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.entities.MessageEmbed;
import net.dv8tion.jda.api.entities.channel.concrete.VoiceChannel;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.events.interaction.component.ButtonInteractionEvent;
import net.dv8tion.jda.api.exceptions.InsufficientPermissionException;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.commands.build.SubcommandData;
import net.dv8tion.jda.api.interactions.components.ItemComponent;
import net.dv8tion.jda.api.interactions.components.buttons.ButtonStyle;
import net.dv8tion.jda.api.managers.AudioManager;

import java.awt.Color;
import java.io.IOException;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;

public class Transcribe extends ButtonCommand<MessageEmbed, SlashCommandInteractionEvent> implements AcknowledgeableCommand<SlashCommandInteractionEvent> {

    public Transcribe() {
        super(
                new BotCommandOptions()
                        .setName("transcribe")
                        .setDescription("Transcribe audio!")
                        .setOnlyEmbed(true)
                        .setDeferReplies(true)
                        .setRatelimitLength(60)
                        .setUseRatelimits(true)
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
        if(event.getSubcommandName().equalsIgnoreCase("vc")) {
            return List.of(
                    getButton("summarize"),
                    getButton("optout")
            );
        }

        return List.of();
    }

    @SuppressWarnings("unchecked")
    @Override
    public CommandResponse<MessageEmbed> runCommand(long userId, @NonNull SlashCommandInteractionEvent event) {
        MessageEmbed[] embedsArray;
        EmbedBuilder workingEmbed;
        CommandResponse<MessageEmbed> response;

        try {
            embedsArray = checkAndHandleAcknowledgement(event);
        } catch(IOException e) {
            throw new RuntimeException(e);
        }

        if(embedsArray == null) {
            workingEmbed = new EmbedBuilder();
        } else {
            ratelimitMap.remove(userId);
            return new CommandResponse<>(isOnlyEphemeral(), embedsArray);
        }

        // Check rate limit
        response = (CommandResponse<MessageEmbed>) checkAndHandleRateLimitedUser(userId);

        if(response != null)
            return response;

        workingEmbed.setTitle("Transcribe");
        if(event.getSubcommandName().equalsIgnoreCase("vc")) {
            try {
//                handleTranscribeVC(eb, event);
                workingEmbed.setDescription("VC transcription is currently not available. We'll update you when it is!");
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        } else if(event.getSubcommandName().equalsIgnoreCase("url")) {
            handleTranscribeUrl(workingEmbed, event);
        } else {
            workingEmbed.setDescription("Invalid subcommand!");
        }

        return new CommandResponse<>(workingEmbed.build(), isOnlyEphemeral());
    }

    @Override
    public void updateCommand(@NonNull JDA jda) {
        SubcommandData vc = new SubcommandData("vc", "(BETA) Join a VC and transcribe the conversations")
                .addOption(OptionType.CHANNEL, "channel", "The channel to join", true)
                .addOption(OptionType.INTEGER, "duration", "The duration to transcribe for (in seconds)", true);
        SubcommandData url = new SubcommandData("url", "Transcribe an audio file from a URL")
                .addOption(OptionType.STRING, "url", "The URL of the audio file to transcribe", true);

        jda
                .upsertCommand(getName(), getDescription())
                .addSubcommands(vc, url)
                .queue(r -> setGlobalCommandId(r.getIdLong()));
    }

    private void handleTranscribeUrl(@NonNull EmbedBuilder eb, @NonNull SlashCommandInteractionEvent event) {
        eb.setDescription(
                OpenAIHandler.getAudioTranscription(
                        event.getOption("url").getAsString()
                ).text()
        );

        AnalyticsHandler.addUsage(event.getGuild(), ServiceType.TRANSCRIBE_ATTACHMENT);
    }

    private void handleTranscribeVC(@NonNull EmbedBuilder eb, @NonNull SlashCommandInteractionEvent event) throws InterruptedException, IOException {
        BrewerServerData data = ((BrewerServerDataHandler) core.getServerDataHandler()).getServerData(event.getGuild().getIdLong());
        VoiceChannel vc = event.getOption("channel").getAsChannel().asVoiceChannel();
        AudioManager manager = vc.getGuild().getAudioManager();
        VoiceHandler handler = new VoiceHandler(30);
        HashMap<Long, byte[]> audioData;
        byte[] combinedAudio;

        manager.setReceivingHandler(handler);
        try {
            manager.openAudioConnection(vc);

            while(handler.canReceiveEncoded()) {
                Thread.sleep(1000);
            }

            manager.closeAudioConnection();
            audioData = (HashMap<Long, byte[]>) handler.getAudioData();
            combinedAudio = handler.getCombinedAudioData();

            if(audioData.isEmpty()) {
                eb.setDescription("I was unable to pick up any audio!");
                eb.setColor(Color.RED);
            } else {
                String combinedTranscription = transcribeUser(combinedAudio, 0);

                for(long userId: audioData.keySet()) {
                    if(!data.isUserOptedOutOfVCTranscription(userId)) {

//                        eb.addField(
//                                vc.getGuild().getMember(UserSnowflake.fromId(userId)).getEffectiveName(),
//                                transcribeUser(audioData.get(userId), userId),
//                                false
//                        );
                    }
                }
            }

            AnalyticsHandler.addUsage(event.getGuild(), ServiceType.TRANSCRIBE_VC);
        } catch(InsufficientPermissionException ignored) {
            eb.setDescription("I don't have permission to join that VC!");
            eb.setColor(Color.RED);
        }
    }

    @NonNull
    private String transcribeUser(@NonNull byte[] audioData, long userId) {
        return OpenAIHandler.getVoiceTranscription(audioData, Long.toString(userId)).text();
    }
}
