package dev.alphaserpentis.bots.brewer.data;

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
            @NonNull ArrayList<ParseActions.ExecutableAction> actionsToExecute,
            short brewCount
    ) {
        this.prompt = prompt;
        this.type = type;
        this.action = action;
        this.actionsToExecute = actionsToExecute;
        this.brewCount = brewCount;
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

    public void setActionsToExecute(ArrayList<ParseActions.ExecutableAction> actionsToExecute) {
        this.actionsToExecute = actionsToExecute;
    }
    public void setJDA(JDA jda) {
        this.jda = jda;
    }
    public void setInterpreterResult(@NonNull Interpreter.InterpreterResult interpreterResult) {
        this.interpreterResult = interpreterResult;
    }
    public void setBrewCount(short brewCount) {
        this.brewCount = brewCount;
    }
    public void setInteractionToken(String interactionToken) {
        this.interactionToken = interactionToken;
    }
    public void setGuildId(long guildId) {
        this.guildId = guildId;
    }
}