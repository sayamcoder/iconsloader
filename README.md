# IconsLoader v4.0

[![SpigotMC](https://img.shields.io/badge/SpigotMC-Download-orange?style=for-the-badge&logo=spigotmc)]([SPIGOT LINK HERE])
[![Discord](https://img.shields.io/badge/Discord-Join-blue?style=for-the-badge&logo=discord)](https://discord.gg/dgJza7ZrE)
[![Java Version](https://img.shields.io/badge/Java-21-red?style=for-the-badge&logo=java)](https://adoptium.net/)
[![Paper](https://img.shields.io/badge/Paper-1.21+-green?style=for-the-badge)](https://papermc.io/)

**IconsLoader** is a premium-grade, fully automated custom emoji and icon manager for Minecraft servers. It allows server administrators to drop PNG files into a folder, auto-generate a highly optimized resource pack, and use them across chat, GUIs, TAB, and Holograms without any external hosting required.

## 📖 Table of Contents
1. [Features](#-features)
2. [Commands & Permissions](#-commands--permissions)
3. [PlaceholderAPI Support](#-placeholderapi-support)
4. [Quick Start Guide](#-quick-start-guide)
5. [Building from Source](#-building-from-source)
6. [Support](#-support)

## ✨ Features

- **🌐 Built-in HTTP Server:** Host the generated resource pack directly from your server's IP. No more Dropbox or MCPacks!
- **🤝 Smart Pack Merger:** Seamlessly merge IconsLoader emojis into existing packs (like ItemsAdder or Oraxen) without conflicts.
- **⌨️ Chat Tab-Completion:** Auto-suggests emojis when players type `:hea` and press Tab.
- **🏷️ Emoji Aliases:** Map multiple names to a single emoji (e.g., `:heart:`, `:love:`, `:<3:`).
- **🖥️ Visual Emoji GUIs:**
  - `/emojis`: A player-facing GUI to browse and insert emojis directly into chat.
  - `/cl gui`: An admin menu to view all registered glyphs, permissions, and textures.
- **🛡️ Native 1.21.8 Dialog Menus:** Uses Minecraft's new screen popups for admin confirmations.
- **⚡ Async Generation:** Pack generation runs off the main thread, ensuring zero server lag.
- **🔒 Auto-SHA1 Generation:** Automatically calculates the SHA-1 hash and updates `config.yml`.
- **💬 Chat Compatibility:** Uses `EventPriority.HIGHEST` and Adventure APIs so it never breaks LuckPerms or other chat plugins.
- **🌍 12 Built-in Languages:** EN, ES, FR, DE, PT, RU, ZH, HI, AR, TR, ID, JA.
- **🎮 Version Support:** Perfect compatibility from **1.8 to 1.21.8+** (including ViaVersion support).

## 📜 Commands & Permissions

| Command | Description | Permission |
| :--- | :--- | :--- |
| `/cl reload` | Reloads configs and restarts the HTTP server. | `iconsloader.reload` |
| `/cl gui` | Opens the Admin Emoji Manager GUI. | `iconsloader.admin` |
| `/emojis` | Opens the Player Emoji Picker GUI. | `iconsloader.use` |
| `/cl pack generate` | Generates the Resource Pack ZIP and auto-updates SHA-1. | `iconsloader.pack.generate` |
| `/cl pack send <player\|@a>` | Force sends the resource pack to players. | `iconsloader.pack.send` |
| `/cl glyph list` | Lists all loaded emojis. | `iconsloader.glyph.list` |
| `/cl glyph info <name>` | Shows the texture path and CMD of an emoji. | `iconsloader.glyph.info` |

## 🔗 PlaceholderAPI Support

IconsLoader hooks into PlaceholderAPI so you can use emojis in TAB, Scoreboards, and Holograms:

- **Direct Emoji:** `%iconsloader_heart%`
- **Text Replacer:** `%iconsloader_replace_Hello_:heart:%`

## ⚙️ Quick Start Guide

1. Download the plugin and place it in your `plugins/` folder.
2. Start the server and place your transparent PNGs in `plugins/IconsLoader/icons/`.
3. Run `/cl reload`. The plugin will automatically assign Unicode characters.
4. Run `/cl pack generate`.
5. Enable the Built-in HTTP Server in `config.yml` (or upload the ZIP to a host).
6. Run `/cl reload` and enjoy!

## 🛠️ Building from Source

This project uses Maven. To build the JAR yourself:
