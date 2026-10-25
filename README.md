[B][SIZE=6][COLOR=#8B5CF6]IconsLoader v2.1[/COLOR][/SIZE][/B]
[B][SIZE=4]The Ultimate Custom Emoji, Icon & Resource Pack Manager for Minecraft 1.21+[/SIZE][/B]

[HR][/HR]
[B][SIZE=5][COLOR=#8B5CF6]📖 Overview[/COLOR][/SIZE][/B]
Ever wanted to add custom emojis like [B]:heart:[/B], [B]:coin:[/B], or [B]:crown:[/B] to your server's chat, items, scoreboards, or holograms? 
[B]IconsLoader[/B] makes it incredibly easy! Simply drop your PNG files into a folder, and the plugin will automatically generate a fully functional Minecraft Resource Pack for you. No coding, no manual JSON editing, and no complex pack hosting setups required.

[HR][/HR]

[B][SIZE=5][COLOR=#8B5CF6]✨ Features[/COLOR][/SIZE][/B]
[LIST]
[*][B]🚀 Automatic Resource Pack Generation:[/B] Just drop PNGs in a folder and run a command. The plugin builds the ZIP, generates the JSON, and formats it perfectly for you.
[*][B]🔒 Auto-SHA1 Generation:[/B] The plugin calculates the SHA-1 hash of your resource pack automatically and saves it to your config. No more using online hash generators!
[*][B]💬 Chat Emoji Replacement:[/B] Players can type [B]:heart:[/B] in chat, and it will automatically render as your custom icon without breaking other chat plugins (PlaceholderAPI & LuckPerms compatible).
[*][B]⚔️ Anvil Support:[/B] Players can use emojis in Anvils to rename their items! Type [B]:heart:[/B] on an anvil and get the custom icon on your sword.
[*][B]🖥️ Visual Emoji GUI:[/B] Open the emoji menu with [B]/cl gui[/B]. The GUI items actually display your custom emoji textures using 1.21.4+ Item Models!
[*][B]🌍 12 Built-in Languages:[/B] Fully translatable! Comes with English, Spanish, French, German, Portuguese, Russian, Chinese, Hindi, Arabic, Turkish, Indonesian, and Japanese.
[*][B]🔗 PlaceholderAPI Support:[/B] Use your custom emojis in TAB, Scoreboards, Holograms, and DeluxeMenus.
[*][B]🛡️ Advanced Permission System:[/B] Restrict specific emojis behind custom permissions (e.g., only VIPs can use [B]:crown:[/B]).
[*][B]⚡ Async Generation:[/B] Pack generation runs in the background, so your server will never lag or freeze.
[/LIST]

[HR][/HR]

[B][SIZE=5][COLOR=#8B5CF6]📜 Commands & Permissions[/COLOR][/SIZE][/B]
[TABLE]
[TR]
[TH]Command[/TH]
[TH]Description[/TH]
[TH]Permission[/TH]
[/TR]
[TR]
[TD]/cl reload[/TD]
[TD]Reloads config.yml, glyph.yml, and lang files.[/TD]
[TD]iconsloader.reload[/TD]
[/TR]
[TR]
[TD]/cl gui[/TD]
[TD]Opens the visual emoji manager GUI.[/TD]
[TD]iconsloader.admin[/TD]
[/TR]
[TR]
[TD]/cl pack generate[/TD]
[TD]Generates the Resource Pack ZIP and auto-updates SHA-1.[/TD]
[TD]iconsloader.pack.generate[/TD]
[/TR]
[TR]
[TD]/cl pack send <player|@a>[/TD]
[TD]Force sends the resource pack to players.[/TD]
[TD]iconsloader.pack.send[/TD]
[/TR]
[TR]
[TD]/cl glyph list[/TD]
[TD]Lists all loaded emojis (renders the actual emojis in chat).[/TD]
[TD]iconsloader.glyph.list[/TD]
[/TR]
[TR]
[TD]/cl glyph info <name>[/TD]
[TD]Shows the texture path, CMD, and permission of an emoji.[/TD]
[TD]iconsloader.glyph.info[/TD]
[/TR]
[TR]
[TD]/cl support[/TD]
[TD]Get a clickable link to our Discord.[/TD]
[TD]None[/TD]
[/TR]
[/TABLE]

[HR][/HR]

[B][SIZE=5][COLOR=#8B5CF6]🔗 PlaceholderAPI Integration[/COLOR][/SIZE][/B]
IconsLoader hooks directly into PlaceholderAPI so you can use your emojis anywhere on your server!
[LIST]
[*][B]Direct Emoji:[/B] Use [B]%iconsloader_heart%[/B] (Replaces with the emoji)
[*][B]Text Replacer:[/B] Use [B]%iconsloader_replace_Hello_:heart:%[/B] (Replaces tokens inside text)
[/LIST]
[B]Works perfectly with:[/B] TAB, DecentHolograms, FancyHolograms, Scoreboard plugins, and Chat formatting plugins!

[HR][/HR]

[B][SIZE=5][COLOR=#8B5CF6]⚙️ Quick Start Guide[/COLOR][/SIZE][/B]
[LIST=1]
[*]Download the plugin and place it in your `plugins` folder. Start the server.
[*]Place your transparent PNG files inside `plugins/IconsLoader/textures/icons/`.
[*]Run [B]/cl reload[/B] in-game. (The plugin will automatically assign a Unicode character to your new PNGs).
[*]Run [B]/cl pack generate[/B]. Your Resource Pack is now ready in the `output` folder!
[*]Upload that ZIP file to a direct-download host (like Dropbox or mc-packs.net).
[*]Paste the direct link into your `config.yml` and run [B]/cl reload[/B]! (SHA-1 is updated automatically).
[/LIST]

[HR][/HR]

[B][SIZE=5][COLOR=#8B5CF6]💬 Support & Links[/COLOR][/SIZE][/B]
[LIST]
[*][B]Discord Support:[/B] [URL=https://discord.gg/dgJza7ZrE]Click here to join our Discord![/URL]
[*][B]Version:[/B] 2.1.0
[*][B]Compatibility:[/B] Paper 1.21+ (Java 21 Required)
[/LIST]

[B]If you enjoy the plugin, please leave a ⭐ rating and a review! It helps a lot![/B]
