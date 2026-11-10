# Overgeared Mod

An unofficial, fan-made Minecraft mod (NeoForge 1.21.1) based on the web novel and manhwa *Overgeared* (템빨) by Park Saenal. Free, open source and non-commercial.

> **Unofficial fan project.** This mod is not affiliated with, endorsed by, sponsored by or approved by Park Saenal, the publishers or licensors of *Overgeared*, Mojang or Microsoft. *Overgeared*, its characters, places and names belong to their respective owners. See [Fan project notice](#fan-project-notice).

**Status: in development (0.x).** There is no public release yet; saves from development builds are not guaranteed to load in later ones.

## What it is

The overworld is the real world. With a VR capsule you connect to **Satisfy**, the game inside the story, which is its own dimension. You start where the novel starts: an ordinary player with a debt to pay, who becomes the successor of the legendary blacksmith Pagma.

What the first public version aims to include (already built, being play-tested):

- **Capsule and two worlds**: connect from a capsule, play in Satisfy with a separate character and inventory, log out safely. Three capsule tiers.
- **Fatigue**: energy that drains while you are connected and recovers in the real world, to give sessions a rhythm.
- **Levels, stats, titles and skills** with their own profile, quest and journal screens.
- **Blacksmithing**: smelting and forging with minigames, hammers, new metals (jaffa, mithril, blue orichalcum) and equipment with ratings from Normal to Myth and rolled options.
- **Bairan and its quests**: the starting village, Smith's quest chain, the North End Cave, Count Ashur and Pagma's Rare Book.
- **Winston**: Khan's smithy and the production contest.
- **Economy**: Won, the starter debt, a terminal and phone to work, sell equipment and buy, and apprentice blacksmiths.
- **Leveled mobs and elites** with rated loot.

More classes, the full continent of the novel and multiplayer features (parties, guilds, market) are planned for later versions.

## Requirements

- Minecraft 1.21.1
- NeoForge 21.1 or newer

## Installation

1. Download the mod jar (no public release yet; build it with `./gradlew build`, the jar is in `build/libs`).
2. Put it in the `mods` folder of a NeoForge 1.21.1 instance.
3. Start a **new world**: the Satisfy dimension and its places are only generated in new worlds.

## Building and testing

- `./gradlew build`: compiles and runs the unit tests.
- `./gradlew runGameTestServer`: runs the game tests on a headless server.
- `./gradlew runClient`: starts the game with the mod.

## Fan project notice

- This is a fan work made out of appreciation for *Overgeared*. It is and will stay **free**: no paid downloads, no paid early access, no monetized content tied to the mod.
- The code, textures and writing in this repository are the work of the project's authors; the names of characters, places, items and concepts come from *Overgeared* and belong to their owners.
- If you are a rights holder and want something changed or removed, please open an issue in this repository.
- Not an official Minecraft product. Not approved by or associated with Mojang or Microsoft.

## AI usage notice

This project is developed with the assistance of AI tools (code, tests, documentation and design ideas). The author directs, reviews and is responsible for what is published.

## License

The code and original content of this repository are under the [MIT License](LICENSE.md). The license does not cover, and grants no rights over, the *Overgeared* names, characters and setting, nor any third-party asset that may be credited separately.

## Credits

- Project author: Orijua
- *Overgeared* (템빨) was created by Park Saenal.
