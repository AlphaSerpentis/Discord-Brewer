package dev.alphaserpentis.bots.brewer.commands.admin;

import dev.alphaserpentis.bots.brewer.handler.bot.ModerationHandler;
import dev.alphaserpentis.coffeecore.commands.BotCommand;
import dev.alphaserpentis.coffeecore.data.bot.CommandResponse;
import io.reactivex.rxjava3.annotations.NonNull;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.MessageEmbed;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.interactions.commands.DefaultMemberPermissions;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.commands.build.OptionData;
import net.dv8tion.jda.api.interactions.commands.build.SlashCommandData;
import net.dv8tion.jda.api.interactions.commands.build.SubcommandData;
import net.dv8tion.jda.api.interactions.commands.build.SubcommandGroupData;

import java.util.List;
import java.util.Objects;

public class Admin extends BotCommand<MessageEmbed, SlashCommandInteractionEvent> {

    public Admin(long guildId) {
        super(
                new BotCommandOptions("admin", "Admin commands for the bot")
                        .setCommandVisibility(CommandVisibility.GUILD)
                        .setGuildsToRegisterIn(List.of(guildId))
                        .setDeferReplies(true)
        );
    }

    @Override
    @NonNull
    public CommandResponse<MessageEmbed> runCommand(long userId, @NonNull SlashCommandInteractionEvent event) {
        var subcommandName = Objects.requireNonNullElse(event.getSubcommandName(), "");
        var eb = new EmbedBuilder();

        if(verifyMember(Objects.requireNonNull(event.getMember()))) {
            switch(subcommandName) {
                case "ban" -> ban(event, eb);
                case "unban" -> unban(event, eb);
                default -> throw new IllegalStateException("Unexpected value: " + subcommandName);
            }
        } else {
            eb
                    .setTitle("Error")
                    .setDescription("You do not have permission to use this command");
        }

        return new CommandResponse<>(isOnlyEphemeral(), eb.build());
    }

    @Override
    public void updateCommand(@NonNull Guild guild) {
//        var premium = new SubcommandGroupData("premium", "Premium commands");
        var id = new OptionData(
                OptionType.STRING, "id", "The ID of the user/guild", true
        );
//        var time = new OptionData(
//                OptionType.INTEGER, "time", "The amount of days", true
//        );
        var reason = new OptionData(
                OptionType.STRING, "reason", "Reason for action", false
        );
        var moderate = new SubcommandGroupData("moderate", "Moderation commands")
                .addSubcommands(
                        new SubcommandData("ban", "Bans a user/guild from using the bot")
                                .addOptions(id, reason),
                        new SubcommandData("unban", "Unbans a user/guild from using the bot")
                                .addOptions(id)
                );
        var cmdData = ((SlashCommandData) getJDACommandData(getCommandType(), getName(), getDescription()))
                .setGuildOnly(true)
                .setDefaultPermissions(DefaultMemberPermissions.enabledFor(Permission.ADMINISTRATOR))
                .addSubcommandGroups(moderate);

//        premium.addSubcommands(
//                new SubcommandData("add", "Adds time to a user's premium")
//                        .addOptions(id, time),
//                new SubcommandData("remove", "Removes time from a user's premium")
//                        .addOptions(id, time)
//        );

        guild.upsertCommand(cmdData).queue();
    }

    /**
     * Verifies the member is the bot owner.
     * @param member The member to verify
     * @return Whether the member is verified
     */
    private boolean verifyMember(@NonNull Member member) {
        return member.getIdLong() == getCore().getBotOwnerId();
    }

    private void ban(@NonNull SlashCommandInteractionEvent event, @NonNull EmbedBuilder eb) {
        var id = Objects.requireNonNull(event.getOption("id")).getAsLong();

        ModerationHandler.addRestrictedId(id);
        eb.setTitle("Banned ID").setDescription("Banned ID: `%s`".formatted(id));
    }

    private void unban(@NonNull SlashCommandInteractionEvent event, @NonNull EmbedBuilder eb) {
        var id = Objects.requireNonNull(event.getOption("id")).getAsLong();

        ModerationHandler.removeRestrictedId(id);
        eb.setTitle("Unbanned ID").setDescription("Unbanned ID: `%s`".formatted(id));
    }
}
