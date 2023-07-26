package dev.alphaserpentis.bots.brewer.data.brewer;

import dev.alphaserpentis.bots.brewer.handler.parser.Interpreter;
import dev.alphaserpentis.bots.brewer.handler.parser.ParseActions;
import io.reactivex.rxjava3.annotations.NonNull;
import net.dv8tion.jda.api.JDA;

import java.util.ArrayList;

public class UserSession {
    public enum UserSessionType {
        NEW_BREW,
        RENAME,
        NAME_SUGGESTIONS
    }

    private final String prompt;
    private final UserSessionType type;
    private final ParseActions.ValidAction action;
    private JDA jda;
    private ArrayList<ParseActions.ExecutableAction> actionsToExecute;
    private Interpreter.InterpreterResult interpreterResult;
    private short brewCount;
    private String interactionToken = "";
    private long guildId;

    public UserSession(
            @NonNull String prompt,
            @NonNull UserSessionType type,
            @NonNull ParseActions.ValidAction action,
            @NonNull ArrayList<ParseActions.ExecutableAction> actionsToExecute
    ) {
        this.prompt = prompt;
        this.type = type;
        this.action = action;
        this.actionsToExecute = actionsToExecute;
        this.brewCount = 1;
    }

    public String getPrompt() {
        return prompt;
    }
    public UserSessionType getType() {
        return type;
    }
    public ParseActions.ValidAction getAction() {
        return action;
    }
    public JDA getJDA() {
        return jda;
    }
    public ArrayList<ParseActions.ExecutableAction> getActionsToExecute() {
        return actionsToExecute;
    }
    public Interpreter.InterpreterResult getInterpreterResult() {
        return interpreterResult;
    }
    public short getBrewCount() {
        return brewCount;
    }
    public String getInteractionToken() {
        return interactionToken;
    }
    public long getGuildId() {
        return guildId;
    }

    public UserSession setActionsToExecute(ArrayList<ParseActions.ExecutableAction> actionsToExecute) {
        this.actionsToExecute = actionsToExecute;
        return this;
    }
    public UserSession setJDA(JDA jda) {
        this.jda = jda;
        return this;
    }
    public UserSession setInterpreterResult(@NonNull Interpreter.InterpreterResult interpreterResult) {
        this.interpreterResult = interpreterResult;
        return this;
    }
    public UserSession setBrewCount(short brewCount) {
        this.brewCount = brewCount;
        return this;
    }
    public UserSession setInteractionToken(String interactionToken) {
        this.interactionToken = interactionToken;
        return this;
    }
    public UserSession setGuildId(long guildId) {
        this.guildId = guildId;
        return this;
    }
}