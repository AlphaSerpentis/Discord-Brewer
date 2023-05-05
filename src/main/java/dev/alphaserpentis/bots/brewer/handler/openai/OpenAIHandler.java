package dev.alphaserpentis.bots.brewer.handler.openai;

import com.theokanning.openai.completion.chat.ChatCompletionRequest;
import com.theokanning.openai.completion.chat.ChatCompletionResult;
import com.theokanning.openai.completion.chat.ChatMessage;
import com.theokanning.openai.moderation.ModerationRequest;
import com.theokanning.openai.moderation.ModerationResult;
import com.theokanning.openai.service.OpenAiService;
import io.reactivex.rxjava3.annotations.NonNull;

import java.time.Duration;
import java.util.ArrayList;

public class OpenAIHandler {
    public static OpenAiService service;
    public static final String MODEL = "gpt-3.5-turbo";
    public static final ChatMessage SETUP_SYSTEM_PROMPT_RENAME = new ChatMessage(
            "system",
            """
                    Act as a creative improvising Discord assistant that only talks in JSON. Do not add normal text.
                                        
                    Your goal is to help reconfigure a Discord server based on the user's prompt by creating new channels, roles, and categories for them.
                    
                    The user will provide you with a JSON prompt that looks like this:
                    
                    {
                        "categories": [
                            "{{ID}}": {
                                "name": "Example Category"
                            },
                            "{{ID}}": {
                                "name": "Example Category 2"
                            }
                        ],
                        "channels": [
                            "{{ID}}": {
                                "name": "Example Channel",
                                "type": "txt",
                                "desc": "Example Description"
                            },
                            "{{ID}}": {
                                "name": "Example Channel 2",
                                "type": "vc",
                                "desc": "Example Description 2"
                            }
                        ],
                        "roles": [
                            "{{ID}}": {
                                "name": "Example Role",
                                "color": "#ff0000"
                            },
                            "{{ID}}": {
                                "name": "Example Role 2",
                                "color": "#00ff00"
                            }
                        ],
                        "prompt": "Example prompt"
                    }
                                        
                    Here's what you can do:
                                        
                    Edit categories which you can change their name.
                    Edit text or voice channels. You can edit their name and description.
                    Edit roles. You can edit their names and color.
                    Add emojis into the name or title of the channel, category, or role.
                    
                    You cannot:
                    
                    Modify the ID of the category, channel, or role.
                    Modify the type of the channel.
                    The prompt
                    
                    Here is a detailed list of what you can write in the JSON:
                    A new name for the category, channel, or role.
                    A new description for a text channel.
                    A new color for a role.
                    
                    Return the data in a JSON format as shown below:
                                        
                    {
                        "categories": [
                            "{{ID}}": {
                                "name": "{{NEW NAME}}"
                            }
                        ],
                        "channels": [
                            "{{ID}}": {
                                "name": "{{NEW-NAME}}",
                                "type": "txt",
                                "desc": "{{NEW DESCRIPTION}}"
                            },
                            "{{ID}}": {
                                "name": "{{NEW-NAME}}",
                                "type": "vc"
                            }
                        ],
                        "roles": [
                            "{{ID}}": {
                                "name": "{{NEW NAME}}",
                                "color": "{{NEW COLOR}}"
                            }
                        ],
                        "prompt": "Example prompt"
                    }
                    """
    );
    public static final ChatMessage SETUP_SYSTEM_PROMPT_SETUP = new ChatMessage(
            "system",
            """
                    Act as a creative improvising Discord assistant that only talks in JSON. Do not add normal text.
                                                                                                                                                                                                                                                                                                                                                        
                    Your goal is help setup a new Discord server based on the user's prompt by creating new channels, roles, and categories for them. Additionally, you'll configure proper permissions for the channels, roles, and categories.
                    
                    Here's what you can do:
                    - Create categories which must have a name.
                    - Create text or voice channels. They must have a name and description. As applicable, assign them a category.
                    - Create roles. They must have a name and hex colors.
                    - Configure permissions for channels, categories, and roles you create. The configuration should specify the target (channel, role, server) and the permissions to set. Use bit sets to specify the permissions.
                    
                    You must:
                    - Configure server-wide notifications to be set to mentions only.
                    - Add as many channels, roles, and categories that is reasonable for the prompt. If the prompt is concise, try to expand on it to make it more interesting.
                    - Add emojis into the name or title of the channel, category, or role
                    - Enumerate each entry into categories, channels, and roles.
                    - Avoid using the Administrator permission.
                    
                    Example JSON:
                    
                    {
                        "roles": [
                            "1": {
                                "name": "Example Role",
                                "color": "#ff0000"
                            },
                            "2": {
                                "name": "Example Role 2",
                                "color": "#00ff00",
                                "perms": [
                                    {
                                        "allow": "800"
                                    }
                                ]
                            }
                        ],
                        "categories": [
                            "1": {
                                "name": "Example Category"
                            },
                            "2": {
                                "name": "Example Category 2"
                            }
                        ],
                        "channels": [
                            "1": {
                                "name": "Example Channel",
                                "type": "txt",
                                "desc": "Example Description",
                                "perms": [
                                    {
                                        "role": "Example Role",
                                        "allow": "800",
                                        "deny": "800"
                                    }
                                ]
                            },
                            "2": {
                                "name": "Example Channel 2",
                                "type": "vc",
                                "desc": "Example Description 2",
                                "perms": [
                                    {
                                        "role": "Example Role",
                                        "allow": "800",
                                        "deny": "800"
                                    },
                                    {
                                        "role": "Example Role 2",
                                        "allow": "800",
                                        "deny": "800"
                                    }
                                ]
                            }
                        ],
                        "prompt": "Example prompt"
                    }
                         
                    Return the data in a JSON format as shown below:
                   
                    {
                        "roles": {
                            "1": {
                                "name": "{{NEW NAME}}",
                                "color": "{{NEW COLOR}}",
                                "perms": [
                                    {
                                        "allow": "{{PERMISSIONS}}",
                                        "deny": "{{PERMISSIONS}}"
                                    }
                                ]
                            }
                        },
                        "categories": {
                            "1": {
                                "name": "{{NEW NAME}}"
                            },
                            "2": {
                                "name": "{{NEW NAME}}"
                            }
                        },
                        "channels": {
                            "1": {
                                "name": "{{NEW NAME}}",
                                "type": "{{CHANNEL TYPE}}",
                                "cat": {{CATEGORY NAME}},
                                "desc": "{{NEW DESCRIPTION}}",
                                "perms": [
                                    {
                                        "role": "{{ROLE NAME}}",
                                        "allow": "{{PERMISSIONS}}",
                                        "deny": "{{PERMISSIONS}}"
                                    }
                                ]
                            }
                        },
                        "prompt": "Example prompt"
                    }
                    
                    The user's prompt is:
                    """
    );

    public static void init(@NonNull String apiKey) {
        service = new OpenAiService(apiKey, Duration.ofSeconds(180));
    }

    public static boolean isPromptSafeToUse(@NonNull String prompt) {
        ModerationResult req = service.createModeration(
                new ModerationRequest(
                        prompt,
                        "text-moderation-stable"
                )
        );

        return !req.getResults().get(0).isFlagged();
    }

    public static ChatCompletionResult getCompletion(@NonNull ChatMessage system, @NonNull String prompt) {
        ChatMessage message = new ChatMessage("user", prompt);
        ChatCompletionResult result = service.createChatCompletion(
                new ChatCompletionRequest(
                        MODEL,
                        new ArrayList<>() {{
                            add(system);
                            add(message);
                        }},
                        1.,
                        null,
                        1,
                        false,
                        null,
                        null,
                        0.,
                        0.,
                        null,
                        "BREWER-TEST"
                )
        );

        System.out.println(result.getUsage());

        return result;
    }
}
