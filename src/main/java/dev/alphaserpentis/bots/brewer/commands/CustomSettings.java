package dev.alphaserpentis.bots.brewer.commands;

import dev.alphaserpentis.bots.brewer.data.brewer.BrewerServerData;
import dev.alphaserpentis.bots.brewer.handler.bot.BrewerServerDataHandler;
import dev.alphaserpentis.coffeecore.commands.defaultcommands.Settings;
import dev.alphaserpentis.coffeecore.data.bot.CommandResponse;
import dev.alphaserpentis.coffeecore.data.server.ServerData;
import dev.alphaserpentis.coffeecore.handler.api.discord.servers.ServerDataHandler;
import io.reactivex.rxjava3.annotations.NonNull;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.entities.MessageEmbed;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.interactions.commands.build.SubcommandData;

import java.awt.Color;
import java.io.IOException;

public class CustomSettings extends Settings {

    private static final String NO_PERMISSIONS =
            """
            You do not have permission to use this subcommand.
            
            You must have the `Manage Server` or `Administrator` permission to use this subcommand.
            """;

    public CustomSettings() {
        super();
    }

    @Override
    @NonNull
    public CommandResponse<MessageEmbed> runCommand(long userId, @NonNull SlashCommandInteractionEvent event) {
        EmbedBuilder eb = new EmbedBuilder();

        eb.setTitle("Server Settings");

        if(event.getGuild() == null) {
            eb.setDescription("This command can only be used in a server.");
        } else {
            String subcommandName = event.getSubcommandName();
            try {
                switch(subcommandName) {
                    case "ephemeral" -> {
                        if(isUserPermissioned(event.getMember())) {
                            setServerEphemeral(event.getGuild().getIdLong(), eb);
                        } else {
                            eb.setDescription(NO_PERMISSIONS);
                        }
                    }
                    case "opt-out" -> {
                        if(isUserPermissioned(event.getMember())) {
                            setServerWideOptOutOfAnalytics(event.getGuild().getIdLong(), eb);
                        } else {
                            eb.setDescription(NO_PERMISSIONS);
                        }
                    }
                    case "opt-out-vc" -> setUserDisallowVCListening(
                            event.getGuild().getIdLong(), event.getMember().getIdLong(), eb
                    );
                    case "rename-nsfw-channels" -> {
                        if(isUserPermissioned(event.getMember())) {
                            setTryRenamingNsfwChannels(event.getGuild().getIdLong(), eb);
                        } else {
                            eb.setDescription(NO_PERMISSIONS);
                        }
                    }
                    default -> eb.setDescription("Invalid subcommand.");
                }
            } catch(IOException e) {
                eb.setColor(Color.RED);
                eb.setDescription("An error occurred while trying to update the server settings. Please try again later.\n\nIf this issue persists, please reach out to us at https://asrp.dev/discord");
            }
        }

        return new CommandResponse<>(isOnlyEphemeral(), eb.build());
    }

    @Override
    public void updateCommand(@NonNull JDA jda) {
        SubcommandData ephemeral = new SubcommandData(
                "ephemeral",
                "Toggle whether the bot's responses are ephemeral"
        );
        SubcommandData optOutOfAnalytics = new SubcommandData(
                "opt-out",
                "Toggle to opt out of analytics for this server"
        );
        SubcommandData optOutOfTranscriptions = new SubcommandData(
                "opt-out-vc",
                "(USER ONLY) Toggle to opt out of VC listening for this server"
        );
        SubcommandData tryRenamingNsfwChannels = new SubcommandData(
                "rename-nsfw-channels",
                "Toggle whether the bot should try to rename NSFW channels"
        );

        jda.upsertCommand(name, description).addSubcommands(
                ephemeral, optOutOfAnalytics, optOutOfTranscriptions, tryRenamingNsfwChannels
        ).queue(
                (cmd) -> setGlobalCommandId(cmd.getIdLong())
        );
    }

    private void setServerEphemeral(long guildId, @NonNull EmbedBuilder eb) throws IOException {
        ServerDataHandler<?> sdh = (ServerDataHandler<?>) core.getServerDataHandler();
        ServerData sd = sdh.getServerData(guildId);

        if(sd.getOnlyEphemeral()) {
            sd.setOnlyEphemeral(false);
            eb.setDescription("The bot's responses are no longer ephemeral.");
        } else {
            sd.setOnlyEphemeral(true);
            eb.setDescription("The bot's responses are now ephemeral.");
        }

        sdh.updateServerData();
    }

    private void setServerWideOptOutOfAnalytics(long guildId, @NonNull EmbedBuilder eb) throws IOException {
        BrewerServerDataHandler sdh = (BrewerServerDataHandler) core.getServerDataHandler();
        BrewerServerData sd = sdh.getServerData(guildId);
        boolean currentSetting = sd.getServerWideOptOutOfAnalytics();

        sd.setServerWideOptOutOfAnalytics(!currentSetting);

        sdh.updateServerData();

        eb.setDescription("Server-wide opt-out of analytics is now " + (!currentSetting ? "enabled" : "disabled"));
    }

    private void setTryRenamingNsfwChannels(long guildId, @NonNull EmbedBuilder eb) throws IOException {
        BrewerServerDataHandler sdh = (BrewerServerDataHandler) core.getServerDataHandler();
        BrewerServerData sd = sdh.getServerData(guildId);
        boolean currentSetting = sd.getTryRenamingNsfwChannels();

        sd.setTryRenamingNsfwChannels(!currentSetting);

        sdh.updateServerData();

        eb.setDescription(
        """
        Renaming NSFW channels is now %s"
        
        **Note**: If your NSFW channel contains inappropriate names/descriptions, it will still be skipped!
        """.formatted(!currentSetting ? "enabled" : "disabled"));
    }

    private void setUserDisallowVCListening(long guildId, long userId, @NonNull EmbedBuilder eb) throws IOException {
        BrewerServerDataHandler sdh = (BrewerServerDataHandler) core.getServerDataHandler();
        BrewerServerData sd = sdh.getServerData(guildId);
        boolean isUserOptedOut = sd.isUserOptedOutOfVCTranscription(userId);

        if(isUserOptedOut)
            sd.removeUserFromVCTranscriptionOptOut(userId);
        else
            sd.addUserIntoVCTranscriptionOptOut(userId);

        sdh.updateServerData();

        eb.setDescription("User opt-out of VC transcriptions is now " + (!isUserOptedOut ? "enabled" : "disabled"));
    }
}
