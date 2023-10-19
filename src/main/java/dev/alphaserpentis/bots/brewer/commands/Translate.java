package dev.alphaserpentis.bots.brewer.commands;

import dev.alphaserpentis.bots.brewer.data.brewer.ServiceType;
import dev.alphaserpentis.bots.brewer.data.openai.AudioTranslationResponse;
import dev.alphaserpentis.bots.brewer.handler.bot.AnalyticsHandler;
import dev.alphaserpentis.bots.brewer.handler.bot.ModerationHandler;
import dev.alphaserpentis.bots.brewer.handler.openai.OpenAIHandler;
import dev.alphaserpentis.coffeecore.commands.ButtonCommand;
import dev.alphaserpentis.coffeecore.data.bot.CommandResponse;
import io.reactivex.rxjava3.annotations.NonNull;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.entities.MessageEmbed;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.events.interaction.component.ButtonInteractionEvent;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.commands.build.SubcommandData;
import net.dv8tion.jda.api.interactions.components.ItemComponent;

import java.io.IOException;
import java.util.Collection;
import java.util.List;
import java.util.Objects;

public class Translate extends ButtonCommand<MessageEmbed, SlashCommandInteractionEvent>
        implements AcknowledgeableCommand<SlashCommandInteractionEvent> {

    public Translate() {
        super(
                new BotCommandOptions()
                        .setName("translate")
                        .setDescription("Translate stuff into English!")
                        .setOnlyEmbed(true)
                        .setDeferReplies(true)
                        .setRatelimitLength(60)
                        .setUseRatelimits(true)
        );

//        addButton("summarize", ButtonStyle.PRIMARY, "Summarize", false);
    }

    @Override
    public void runButtonInteraction(@NonNull ButtonInteractionEvent event) {

    }

    @Override
    @NonNull
    public Collection<ItemComponent> addButtonsToMessage(@NonNull SlashCommandInteractionEvent event) {
        return List.of();
    }

    @SuppressWarnings("unchecked")
    @Override
    @NonNull
    public CommandResponse<MessageEmbed> runCommand(long userId, @NonNull SlashCommandInteractionEvent event) {
        MessageEmbed[] embedsArray;
        EmbedBuilder workingEmbed;
        EmbedBuilder serverCheckEmbed;
        EmbedBuilder userCheckEmbed;
        CommandResponse<MessageEmbed> response;
        long guildId;

        try {
            embedsArray = checkAndHandleAcknowledgement(event);
        } catch(IOException e) {
            throw new RuntimeException(e);
        }

        if(embedsArray == null) {
            workingEmbed = new EmbedBuilder();
        } else {
            return new CommandResponse<>(isOnlyEphemeral(), true, embedsArray);
        }

        // Check rate limit
        response = (CommandResponse<MessageEmbed>) checkAndHandleRateLimitedUser(userId);

        if(response != null)
            return response;

        // Check if user/guild is restricted
        guildId = event.getGuild() == null ? 0 : event.getGuild().getIdLong();

        serverCheckEmbed = ModerationHandler.isRestricted(guildId, true);
        if(serverCheckEmbed != null)
            return new CommandResponse<>(isOnlyEphemeral(), serverCheckEmbed.build());

        userCheckEmbed = ModerationHandler.isRestricted(event.getUser().getIdLong(), false);
        if(userCheckEmbed != null)
            return new CommandResponse<>(isOnlyEphemeral(), userCheckEmbed.build());

        workingEmbed.setTitle("Translate");

        if(event.getSubcommandName().equalsIgnoreCase("url")) {
            handleTranslateUrl(workingEmbed, event);
        }

        return new CommandResponse<>(isOnlyEphemeral(), workingEmbed.build());
    }

    @Override
    public void updateCommand(@NonNull JDA jda) {
        var url = new SubcommandData("url", "Translate an audio file from a URL")
                .addOption(OptionType.STRING, "url", "The URL to the audio file", true);

        jda
                .upsertCommand(getName(), getDescription())
                .addSubcommands(url)
                .queue(r -> setGlobalCommandId(r.getIdLong()));
    }

    private void handleTranslateUrl(@NonNull EmbedBuilder eb, @NonNull SlashCommandInteractionEvent event) {
        AudioTranslationResponse response = OpenAIHandler.getAudioTranslation(
                event.getOption("url").getAsString()
        );

        if(response.isCached()) {
            eb.setTitle("Cached Translation");
        }

        eb.setDescription(response.text());

        AnalyticsHandler.addUsage(event.getGuild(), ServiceType.TRANSLATE_ATTACHMENT);
    }
}
