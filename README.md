# DarkUtils
DarkUtils is a mod focused around quality of life, visual tweaks and performance for Hypixel SkyBlock in modern versions of Minecraft. 

Most of those features were ported from [DarkAddons](https://github.com/TheDGOfficial/DarkAddons) that was coded for 1.8.9, but some are specific to DarkUtils and MC 1.21.8.

All features are disabled by default, you must enable any features manually from the settings menu.

# Runtime Dependencies

You need all of the Required Runtime Dependency criterias met to launch the game succesfully with DarkUtils, otherwise Fabric Loader will complain. If the game launches fine, it means you already have all the dependencies.

The versions listed are the ones the mod was tested and developed against, not necessarily what you should use. Forward compatibility is often the case so latest versions are recommended.

Required Dependencies:
- Minecraft 1.21.8 or later.
- Fabric Loader 0.17.2 or later.
- Fabric API 0.133.4 or later.
- Cloth Config API 19.0.147 or later.
- Hypixel Mod API 1.0.1 or later.

Optional Dependencies:
- Mod Menu 15.0.0 or later.

# Quality of Life
## Never Reset Cursor Position
 Prevents mouse cursor from resetting to the middle of screen when a screen (menu/container) is opened, either from stratch or replacing the previous (e.g navigating through menus).
## Always Sprint
 Always acts as if sprinting was toggled, persisting between deaths, world changes, server changes and game restarts, as long as you have this feature enabled.
## Auto Tip
 Automatically run /tip all every 15 minutes taking into account command throttling if you entered a command before it would trigger, and automatically hiding no boosters available to tip responses from the server. This is a very simplified version of the AutoTip mod from 1.8.9 that does not connect to an external server and save/send statistics, everything is done locally and no network connections are made.
## Welcome Message
 Sends a cool welcome message when you first join a world confirming the mod has been loaded, welcoming you, showing the mod version and providing a quick button in chat to open the mod's settings menu.

# Foraging
## Tree Gift Confirmation
 Plays a sound and displays an on-screen message whenever you get a Tree Gift to confirm that the current tree is finished breaking fully. Also displays if a mob spawned from the tree (such as Phanpyre, Phanflare or Dreadwing), helpful for the David's Cloak Foraging Fortune (Forest Hunts) milestones.
## Tree Gifts Per Hour
 Displays a HUD element with Tree Gifts/Hour rate.
 
# Dungeons
## Dialogue Skip Timer
 Shows a timer for when to start killing blood mobs to perform a Watcher Dialogue Skip for faster Blood Camp times.
## Solo Crush Timer
 Shows a timer for when to move the crusher in the Purple Pad to perform a Solo Crush.

# Visual Tweaks
## Hide Effects HUD
 Hides the annoying Effects HUD in the top right of the screen.
## Hide Effects In Inventory
 Additionally hides effects in left and right sides of the inventory as well.
## Transparent Scoreboard
 Makes the Scoreboard fully transparent for better visuals.
## Transparent Nametags
 Makes the Nametags (Player Names, Armor Stand Nametags, etc.) fully transparent.
## Transparent Player List
 Makes the Player List (Tab List) fully transparent.
## Remove Chat Scrollbar
 Removes the Chat Scrollbar from the right side of the chat.
## Fullbright
 Makes your game look as if everything had the Light Level of 15.
## Night Vision
 Acts as if you had permanent client-side Night Vision effect.
## Hide Fire Overlay
 Hides the annoying Fire Overlay when you are burning that makes half of the screen invisible.
## No Burning Entities
 Additionally hides the burning effect rendered on yourself and on entities (e.g a zombie burning under the sun).
## Hide Armor and Food
 Hides Armor and Food bars since they are irrelevant in Hypixel SkyBlock.
## Hide Mount Health
 Hides Mount Health that appears when you are mounted to a mount. Servers usually mount you to an invisible mob to create animations such as moving you to another island smoothly, so it's often irrelevant, especially in Hypixel.
## No Lightning Bolts
 Hides Lightning Bolts from rendering, mainly useful for Storm phase of Floor 7 and Master Floor 7 of The Catacombs.
## No Wither Hearts
 Hides the black overlay rendered on top of your hearts when you have the Wither Status Effect, allowing you to see your health clearly even with Wither Status Effect.

# Performance
## Armor Stand Optimizer
 Only renders the configured amount of closest Armor Stands and their labels to you and hides the rest. Improves FPS drastically when a lot of Furniture has been used in your Private Island for example.
## Disable Yield
 Disables a Thread.yield() call in Render thread to improve FPS. Yielding is a mechanism used to signal to the CPU that you are done with your task for now and that you advise the CPU to run another task instead. Vanilla calls yield after each rendered frame to let other threads or apps run in the CPU. It often reduces FPS anywhere from 1% on high-end systems to, upto 10% in lower end systems. This feature disables calling yield entirely. In modern operating systems and processors, yield essentially is useless as your operating system will automatically fairly share CPU time between all tasks. Moreover, with a multi-core processor yielding is even more useless as the CPU can run the Render thread in parallel with other tasks easily.
## Always Prioritize Render Thread
 Always prioritizes render thread by assigning it the Thread.MAX_PRIORITY. This done by vanilla on systems with 4 or more CPU cores available automatically, but this feature ensures it always gets the maximum priority no matter the amount of CPU cores available.
## Optimize Exceptions
 Optimizes Exceptions when playing on non-vanilla servers such as Hypixel, which are actually on 1.8 and use a translation layer and non-vanilla server software, therefore sometimes send packets that cause errors to be logged. This feature currently only optimizes Signature Errors. It will still log the exception the first time it happens to not make it a silent failure, but repeated errors will not be logged for optimization.
## Always Use No Error Context
 Always uses No Error Context to improve performance. Sodium has a workaround for a bug that no longer occurs that disables No Error Context feature if it detects X11/Wayland. This feature forces Sodium to skip doing the workaround as the bug no longer occurs and performance is precious.
## Disable Campfire Smoke Particles
 Disables Campfire Smoke Particles which trigger unoptimized repeating collideMovement calls in Galatea to reduce memory allocation pressure.
## Remove Main Menu Frame Limit
 Removes the hardcoded 60 FPS limit in Main Menu to look better in modern high refresh-rate systems.
## Log Cleaner
 Cleans old log files automatically to not clutter the filesystem.
## Stop Light Updates
 Stops light updates entirely to improve performance even further after Fullbright, which is visual-only.
## No Memory Reserve
 Removes a 10 MB safety memory reserve in vanilla code. The game normally allocates 10 MB at startup and frees it when you run out of memory to make headroom to display a out of memory screen. However, if you never run out of memory, this 10 MB is wasted. This feature always frees the memory reserve so it can be utilized by other apps or the Operating System for File System Caches. Unused memory is wasted memory!
## Open GL Version Override
 Overrides OpenGL Version hinted to the GLFW during Window context initialization to a higher value than the default of OpenGL 3.3. This does not magically make Minecraft take advantage of features from OpenGL 4.6 specification, but it does ensure forward compatibility and a stricter standard, which might or might not change anything at all.
## Use Virtual Threads for Texture Downloading
Makes Minecraft use Java's new (Lightweight) Virtual Threads over Platform (OS) Threads. Normally, Minecraft uses a Cached Thread Pool which ends up creating hundreds of texture downloading threads in texture-heavy game-modes like Hypixel SkyBlock where items have a player skull model. Those hundreds of texture downloading threads all have their separate stack, and there is a limit to how many platform threads you can create in the OS level at which point it will crash. Virtual Threads are a lightweight new technology replacement that only creates threads when tasks are blocked and this also made texture loading speedier during tests due to creating a new (platform/OS) thread not being a free operation.

# Bugfixes
## Fix GUI Scale After Toggling Out Fullscreen
 Fixes two different bugs depending on Window manager. X11: Fixes the window height getting smaller everytime when going out of Fullscreen. Wayland: Fixes the window height getting bigger everytime when going out of Fullscreen.
## Fix Inactivity FPS Limiter
 Fixes the Inactivity FPS Limiter from limiting the FPS to 10 whilst the game is loading by making the last input time start with an initial value of current time instead of zero.
