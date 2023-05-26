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
                                                                                                                                                                                                                                                                                                                                                        
                    Your goal is to rename the categories, roles, and channels. Also, you'll be changing the color of roles and the description of channels.
                    
                    Old names and descriptions will be given for context on what the categories, channels, and roles are for.
                    
                    New names, descriptions, and colors must be related to the provided prompt.
                    
                    The user will provide you with a JSON prompt that looks like this:
                    
                    {
                        "categories": [
                            "{{OLD NAME}}": {
                                "name": "{{OLD NAME}}"
                            },
                            "{{OLD NAME}}": {
                                "name": "{{OLD NAME}}"
                            }
                        ],
                        "channels": [
                            "{{OLD NAME}}": {
                                "name": "{{OLD NAME}}",
                                "desc": "{{OLD DESCRIPTION}}"
                            },
                            "{{OLD NAME}}": {
                                "name": "{{OLD NAME}}",
                                "desc": "{{OLD DESCRIPTION}}"
                            }
                        ],
                        "roles": [
                            "{{OLD NAME}}": {
                                "name": "{{OLD NAME}}"
                            },
                            "{{OLD NAME}}": {
                                "name": "{{OLD NAME}}"
                            }
                        ],
                        "prompt": "Example prompt"
                    }
                                        
                    Here's what you can do:
                                        
                    - Give new names to categories, channels, and roles pertaining to the prompt.
                    - Give new descriptions to channels pertaining to the prompt.
                    - Change the color of roles pertaining to the prompt.
                    
                    You cannot:
                    
                    - Modify the {{OLD NAME}}
                    - The prompt
                    
                    Return the data in a JSON format as shown below:
                                        
                    {
                        "categories": [
                            "{{OLD NAME}}": {
                                "name": "{{NEW NAME RELATED TO THE PROMPT}}"
                            }
                        ],
                        "channels": [
                            "{{OLD NAME}}": {
                                "name": "{{NEW NAME RELATED TO THE PROMPT}}",
                                "desc": "{{NEW DESCRIPTION RELATED TO THE PROMPT}}"
                            },
                            "{{OLD NAME}}": {
                                "name": "{{NEW NAME RELATED TO THE PROMPT}}"
                            }
                        ],
                        "roles": [
                            "{{OLD NAME}}": {
                                "name": "{{NEW NAME RELATED TO THE PROMPT}}",
                                "color": "{{NEW COLOR RELATED TO THE PROMPT}}"
                            }
                        ],
                        "prompt": "Example prompt"
                    }
                    
                    The user's prompt is:
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

        return service.createChatCompletion(
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
                        "BREWER"
                )
        );
    }
}
