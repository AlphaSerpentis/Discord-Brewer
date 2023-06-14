[discord-invite]: https://asrp.dev/discord

# Discord Brewer ☕️

---

## About Brewer ☕️

Revolutionize your Discord server with Brewer, a dynamic bot leveraging the power of OpenAI's ChatGPT to breathe new life into your roles, categories, and channels!

At its core, Brewer operates with ChatGPT 3.5-turbo, with potential upgrades on the horizon.

**Developed using [Coffee Core](https://github.com/AlphaSerpentis/CoffeeCore), a robust Java Discord bot framework!**

## Features

- **Prompt-based Generation**: Create fresh roles, categories, and channels, complete with permissions - all stemming from your creative prompts.
- **Easy Renaming**: Redefine existing roles, categories, and channels based on new prompts for a quick server makeover.
- **Change Control**: Not quite feeling the latest changes? You can easily revert any recent modifications to your server.

## Inviting the Bot

**Notice**: You bear all responsibility for any changes Brewer brings to your server.

**While the `Administrator` permission may be necessary for Brewer, we advise revoking it after Brewer finishes its tasks to maintain server security!**

**Required Permissions**:
- Manage Roles
- Manage Channels
- Manage Permissions

[**Invite Brewer ☕️ to your server today!**](https://discord.com/api/oauth2/authorize?client_id=1097362340468502548&permissions=268504080&scope=bot), the bot uses ChatGPT 3.5-turbo, but may change in the future.

## Discord Server
[ ![Discord Banner 4](https://discordapp.com/api/guilds/590215639785013298/widget.png?style=banner4)][discord-invite]

## Terms of Service & Privacy Policy
### [View our Privacy Policy](https://github.com/AlphaSerpentis/Discord-Brewer/blob/master/privacy.md)
### [View our Terms of Service](https://github.com/AlphaSerpentis/Discord-Brewer/blob/master/terms_of_service.md)

---

## Running it Yourself (Self-Hosting)

### Requirements
- Discord API Key
- OpenAI API Key
- Java 17+
- A computer I guess

### Create a `[file name].json` File for Storage
In order to run the bot, you need to create a JSON file to store the bot's data. The file name can be anything you want, but it must be a JSON file.

### `.env` File
Copy the `.env.example` file and rename it to `.env`.

Using the `.json` file you created earlier, attach the file path to the `SERVER_DATA_PATH` variable.

Fill in the other values with your own as specified.

### Running the Bot
Run the bot using the following command:
```shell
java -jar Brewer_X.X.X.jar
```

The file name of the jar will be different depending on the version you are running.

## How To Use

### `/brew server [prompt]`

This is the primary command of Brewer and will generate new roles, categories, and channels based on the prompt you provide.

For the prompt, you provide the bot what you desire. For example:
- You ask it to create categories containing the names of "General", "Voice", and "Text" and it will do so.
- Stylize your server with a theme (e.g., medieval, futuristic, etc.)
- Generate a server based on a topic (e.g., gaming, anime, etc.)

Being specific can help generate a more desirable result!

However, due to the nature of ChatGPT, results and accuracy may vary. We are constantly working on ways to improve the results.

---

## Support

### FAQ

#### How often can I run `/brew`?
You can run `/brew` every 3 minutes (globally). Additionally, if you generated a brew, you can retry up to 3 times in that session.

#### I got error messages after brewing!
Please send them my way by either going to our [Discord server][discord-invite] or by opening an issue [here](https://github.com/AlphaSerpentis/Discord-Brewer/issues/new).

#### I don't like the changes Brewer did to my server! Help!
If you ran the command recently, click on the **Revert** button that appears on the last message sent by the bot. This will revert all changes made by the bot.

#### The permissions are wrong/don't make sense!
Brewer might skip the permissions creation process if it is unable to add them (e.g., insufficient permissions). If it does add them, it might not be up-to-date with current available permissions. Due to the nature of ChatGPT 3.5 Turbo (the model Brewer will use by default), it may not generate accurate permissions. This is a known issue and will be tweaked over time.

#### Brewer didn't rename my NSFW channels?
Brewer by default will not rename NSFW channels due to OpenAI's restrictions.

An option will be available to try to rename NSFW channels in the future.

#### Nothing happened/it partially worked?
There are two things that could have happened:

1. You hit the rate limit and the action is paused for some time. It should automatically retry after a couple of minutes.
2. An unhandled error occurred. If you don't believe the first issue is the case, do please report it!

## Dependencies
- Coffee Core (0.4.0-alpha)
- OpenAI-Java Service (0.12.0)
