# chunkstories-core

![alt text](http://chunkstories.xyz/img/github_header2.png "Header screenshot")

This repository contains the core content for the [Chunk Stories](https://github.com/Hugobros3/chunkstories) project. This repo contains (almost) all the assets that make up the base game: 3d models, textures, sounds, GLSL shaders, and all the json files defining the various blocks, entities etc.

To learn more about Chunk Stories in general, please [read the readme of the main repo.](https://github.com/Hugobros3/chunkstories)

# Building

*This is for building `chunkstories-core`, the core content. If you are only looking to write mods, you do not have to mess with this at all and should rather follow the [mods creation guide](http://chunkstories.xyz/wiki/doku.php?id=mod_setup) on the project Wiki !*

## Setup

First you need to clone  `chunkstories-api` as it is needed to compile this. You can try to build from the artifacts in the repo, but only those used in released versions of the games are guaranteed to be present.
 * `git clone` the `chunkstories-api` repo
 * in the chunkstories-api folder: `./gradlew install` or `gradlew.exe install`on Windows

The local maven repository on your computer (.m2 folder) now contains a copy of the api the core content interfaces with. It is not automatically rebuilt when building this, so keep that in mind.

## Gradle Tasks

 * `./gradlew install` builds the core content pack ( core_content.zip ) and installs it to the local maven repository.

# Links

 * To lean how to play the game and register an account, please visit http://chunkstories.xyz
 * You can find a lot more information on the game wiki, including guides to writing mods, at http://chunkstories.xyz/wiki/
 * You can find videos and dev logs on the lead developper youtube channel: http://youtube.com/Hugobros3
 * We have a discord where anyone can discuss with the devs: https://discord.gg/wudd4pe
 * You can get support either by opening a issue on this project or by visiting the subreddit over at https://reddit.com/r/chunkstories

# License

The chunkstories-core **Java Code** is released under LGPL, see LICENSE.MD

The chunkstories-core **assets** (the rest) that aren't specified otherwise in ATTRIBUTION.md are released under Creative Commons NON-COMMERCIAL ATTRIBUTION REDISTRIBUABLE, meaning you can use them with your mods, hack them up and have fun, but you can't use them in commercial projects nor claim you made them. 
