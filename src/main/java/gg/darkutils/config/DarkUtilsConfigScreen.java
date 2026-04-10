package gg.darkutils.config;

import gg.darkutils.DarkUtils;
import gg.darkutils.events.ConfigScreenOpenEvent;
import gg.darkutils.feat.performance.OpenGLVersionOverride;
import gg.darkutils.utils.LogLevel;
import me.shedaniel.clothconfig2.api.ConfigBuilder;
import me.shedaniel.clothconfig2.api.ConfigCategory;
import me.shedaniel.clothconfig2.api.ConfigEntryBuilder;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.text.Text;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.function.Consumer;
import java.util.function.Function;

public final class DarkUtilsConfigScreen {
    private DarkUtilsConfigScreen() {
        super();

        throw new UnsupportedOperationException("static-only class");
    }

    private static final void addSimpleIntegerSetting(@NotNull final ConfigEntryBuilder configEntryBuilder, @NotNull final ConfigCategory configCategory, @NotNull final String name, @NotNull final String desc, final int value, @NotNull final Consumer<Integer> setter, final int defaultValue, final int max) {
        DarkUtilsConfigScreen.addSimpleIntegerSetting(configEntryBuilder, configCategory, name, desc, value, setter, defaultValue, 0, max);
    }

    private static final void addSimpleIntegerSetting(@NotNull final ConfigEntryBuilder configEntryBuilder, @NotNull final ConfigCategory configCategory, @NotNull final String name, @NotNull final String desc, final int value, @NotNull final Consumer<Integer> setter, final int defaultValue, final int min, final int max) {
        configCategory.addEntry(configEntryBuilder
                .startIntField(Text.of(name), value)
                .setDefaultValue(defaultValue)
                .setMin(min).setMax(max)
                .setTooltip(Text.of(desc))
                .setSaveConsumer(setter)
                .build());
    }

    private static final void addSimpleFloatSetting(@NotNull final ConfigEntryBuilder configEntryBuilder, @NotNull final ConfigCategory configCategory, @NotNull final String name, @NotNull final String desc, final float value, @NotNull final Consumer<Float> setter, final float defaultValue, final float min, final float max) {
        configCategory.addEntry(configEntryBuilder
                .startFloatField(Text.of(name), value)
                .setDefaultValue(defaultValue)
                .setMin(min)
                .setMax(max)
                .setTooltip(Text.of(desc))
                .setSaveConsumer(setter)
                .build());
    }

    private static final void addSimpleBooleanToggle(@NotNull final ConfigEntryBuilder configEntryBuilder, @NotNull final ConfigCategory configCategory, @NotNull final String name, @NotNull final String desc, final boolean value, @NotNull final Consumer<Boolean> setter) {
        configCategory.addEntry(configEntryBuilder
                .startBooleanToggle(Text.of(name), value)
                .setDefaultValue(false)
                .setTooltip(Text.of(desc))
                .setSaveConsumer(setter)
                .build());
    }

    @SuppressWarnings("unchecked") // safe
    private static final <T extends Enum<T>> void addEnumSetting(@NotNull final ConfigEntryBuilder configEntryBuilder, @NotNull final ConfigCategory configCategory, @NotNull final String name, @NotNull final String desc, @NotNull final T value, @NotNull final Consumer<T> setter, @NotNull final T defaultValue, @NotNull final Function<T, String> namePrettifier) {
        configCategory.addEntry(configEntryBuilder
                .startEnumSelector(Text.of(name), defaultValue.getDeclaringClass(), value)
                .setDefaultValue(defaultValue)
                .setSaveConsumer(setter)
                .setTooltip(Text.of(desc))
                .setEnumNameProvider(v -> Text.of(namePrettifier.apply((T) v)))
                .build());
    }

    private static final void addQualityOfLife(@NotNull final DarkUtilsConfig config, @NotNull final ConfigBuilder builder, @NotNull final ConfigEntryBuilder entryBuilder) {
        final var qol = builder.getOrCreateCategory(Text.of("Quality of Life"));
        DarkUtilsConfigScreen.addSimpleBooleanToggle(entryBuilder, qol, "Auto Fishing",
                "Automatically fishes for you in Hypixel SkyBlock. Does not kill the fished mobs (Recommended to treasure or trophy fish if you are going to leave it unattended). Requires Skyblock Menu->Settings->Personal->Fishing Settings->Fishing Status Holograms and Fishing Timer to be enabled.",
                config.autoFishing, newValue -> config.autoFishing = newValue);

        DarkUtilsConfigScreen.addSimpleBooleanToggle(entryBuilder, qol, "Auto Fishing Recast",
                "After pulling the rod to get the catch, additionally recasts the rod for the next catch. Does nothing if auto fishing is not enabled.",
                config.autoFishingRecast, newValue -> config.autoFishingRecast = newValue);

        DarkUtilsConfigScreen.addSimpleBooleanToggle(entryBuilder, qol, "Auto Fishing Work Through Menus",
                "Makes it so pulling and recasting the rod works even if you have menus open. Does nothing if auto fishing is not enabled.",
                config.autoFishingWorkThroughMenus, newValue -> config.autoFishingWorkThroughMenus = newValue);

        DarkUtilsConfigScreen.addSimpleIntegerSetting(entryBuilder, qol, "Auto Fishing Minimum Delay", "Minimum delay in ticks before pulling or casting the rod. 1 ticks equals to 50 milliseconds real time. Affects nothing if auto fishing isn't enabled.", config.autoFishingStartingDelay, newValue -> config.autoFishingStartingDelay = newValue, 4, 10);

        DarkUtilsConfigScreen.addSimpleIntegerSetting(entryBuilder, qol, "Auto Fishing Maximum Delay", "Max delay in ticks before pulling or casting the rod. 1 ticks equals to 50 milliseconds real time. Affects nothing if auto fishing isn't enabled.", config.autoFishingMaximumDelay, newValue -> config.autoFishingMaximumDelay = newValue, 5, 10);

        DarkUtilsConfigScreen.addSimpleBooleanToggle(entryBuilder, qol, "Never Reset Cursor Position",
                "Prevents mouse cursor position from resetting to the middle of the screen when opening menus and containers, remembering the last position.",
                config.neverResetCursorPosition, newValue -> config.neverResetCursorPosition = newValue);

        DarkUtilsConfigScreen.addSimpleBooleanToggle(entryBuilder, qol, "Always Sprint",
                "Always makes you sprint, as if you were holding the button.",
                config.alwaysSprint, newValue -> config.alwaysSprint = newValue);

        DarkUtilsConfigScreen.addSimpleBooleanToggle(entryBuilder, qol, "Ghost Block Key",
                "Turns the targeted block to air when you press the configured key. Change the key in the vanilla control settings.",
                config.ghostBlockKey, newValue -> config.ghostBlockKey = newValue);

        DarkUtilsConfigScreen.addSimpleBooleanToggle(entryBuilder, qol, "Auto Tip",
                "Automatically runs the tip all boosters command while in Hypixel every 15 minutes. Checks are in place to not get throttled if you sent a command shortly before it gets triggered.",
                config.autoTip, newValue -> config.autoTip = newValue);

        DarkUtilsConfigScreen.addQualityOfLifeSecond(config, entryBuilder, qol);
    }

    private static final void addQualityOfLifeSecond(@NotNull final DarkUtilsConfig config, @NotNull final ConfigEntryBuilder entryBuilder, @NotNull final ConfigCategory qol) {
        DarkUtilsConfigScreen.addSimpleBooleanToggle(entryBuilder, qol, "Disable Welcome Message",
                "Normally, we send a cool welcome message about the mod with a button to open the settings menu quickly or learn more about the mod. If you enable this option, the message won't be sent.",
                config.disableWelcomeMessage, newValue -> config.disableWelcomeMessage = newValue);

        DarkUtilsConfigScreen.addSimpleBooleanToggle(entryBuilder, qol, "Auto Clicker",
                "Stop carpal tunnel by automatically sending clicks when you are holding down the mouse buttons when holding swords of any type, the huntaxe (for damage and ferocity swap) or the diana spades (for burrow-digging). Right-clicks are only sent for Hyperion/Astraea. This clicks every-tick e.g., 20 CPS but is safe to use.",
                config.autoClicker, newValue -> config.autoClicker = newValue);

        DarkUtilsConfigScreen.addSimpleBooleanToggle(entryBuilder, qol, "Auto Clicker Work In Levers",
                "Makes Auto Clicker work even if you are looking at a lever. It will cause the lever to flick (activate and de-activate) multiple times. This is fine for 2 levers at the gates, but you likely want to keep this disabled if you do lever flick device in Section 2 as healer.",
                config.autoClickerWorkInLevers, newValue -> config.autoClickerWorkInLevers = newValue);

        DarkUtilsConfigScreen.addSimpleBooleanToggle(entryBuilder, qol, "Auto Clicker Work With AOTV",
                "Makes Auto Clicker work when holding AOTV, causing you to teleport rapidly/faster. Might make you teleport more than you intended to even if you click a single time, even when ether warping, but it usually makes you teleport through longer distances much, much faster.",
                config.autoClickerWorkWithAOTV, newValue -> config.autoClickerWorkWithAOTV = newValue);

        DarkUtilsConfigScreen.addSimpleBooleanToggle(entryBuilder, qol, "Disable Cells Alignment",
                "Disables using the Cells Alignment ability of the Gyrokinetic Wand when you are holding it and right-click. The click will still go through if it would interact with an entity or block instead of using the item.",
                config.disableCellsAlignment, newValue -> config.disableCellsAlignment = newValue);

        DarkUtilsConfigScreen.addSimpleBooleanToggle(entryBuilder, qol, "Prevent Useless Block Hit",
                "Prevents useless block hit that emulates 1.8 behaviour on 1.8 servers that allow joining from 1.21 to slow you down when you right click with a sword that does not have a right click ability by simply not allowing you to right click with them unless the click would interact with a block or entity.",
                config.preventUselessBlockHit, newValue -> config.preventUselessBlockHit = newValue);

        DarkUtilsConfigScreen.addSimpleBooleanToggle(entryBuilder, qol, "Disable Command Confirmation",
                "Disables the \"Confirm Command Execution\" menu for invalid or unrecognized commands. Useful in servers that do not register or send all commands they handle or that have dynamic commands. This does not disable the confirmation screen if it was going to execute an elevated command.",
                config.disableCommandConfirmation, newValue -> config.disableCommandConfirmation = newValue);

        DarkUtilsConfigScreen.addSimpleBooleanToggle(entryBuilder, qol, "Rejoin Cooldown Display",
                "Shows time you have to wait before being able to rejoin SkyBlock once you get kicked while joining a server. It's usually 1 minute.",
                config.rejoinCooldownDisplay, newValue -> config.rejoinCooldownDisplay = newValue);

        DarkUtilsConfigScreen.addSimpleBooleanToggle(entryBuilder, qol, "Laggy Server Detector",
                "Shows 30 second TPS average 30 seconds after you change/join servers, telling you the expected gameplay quality of the server.",
                config.laggyServerDetector, newValue -> config.laggyServerDetector = newValue);

        DarkUtilsConfigScreen.addSimpleBooleanToggle(entryBuilder, qol, "Vanilla Mode",
                "Automatically disables certain visual tweaks when playing a singleplayer world, such as hiding the armor and food bars. Your settings will not be actually modified and will revert to default when leaving singleplayer.",
                config.vanillaMode, newValue -> config.vanillaMode = newValue);

        DarkUtilsConfigScreen.addSimpleBooleanToggle(entryBuilder, qol, "Enable Mod Announcer",
                "By default, we disable Firmament's Mod Announcer feature that silently sends all your mod list to servers you connect to without your consent. Enabling this will restore the default behaviour of Firmament, therefore compromising your privacy. It is recommended to not change this option.",
                config.enableModAnnouncer, newValue -> config.enableModAnnouncer = newValue);

        DarkUtilsConfigScreen.addSimpleBooleanToggle(entryBuilder, qol, "Disable Update Checker",
                "By default, we check for updates a single time in the background after your game has been launched. This will not slow down your startup and ensures you are using latest version of the mod, but you can opt-out by enabling this option if you want to disable it.",
                config.disableUpdateChecker, newValue -> config.disableUpdateChecker = newValue);
    }

    private static final void addForaging(@NotNull final DarkUtilsConfig config, @NotNull final ConfigBuilder builder, @NotNull final ConfigEntryBuilder entryBuilder) {
        final var foraging = builder.getOrCreateCategory(Text.of("Foraging"));
        DarkUtilsConfigScreen.addSimpleBooleanToggle(entryBuilder, foraging, "Tree Gift Confirmation",
                "Displays a title and plays a sound when you get a Tree Gift, to confirm that you can move to the next tree. Also shows if you spawned a mob from the tree or not.",
                config.treeGiftConfirmation, newValue -> config.treeGiftConfirmation = newValue);

        DarkUtilsConfigScreen.addSimpleBooleanToggle(entryBuilder, foraging, "Tree Gifts Per Hour",
                "Renders the tree gifts per hour on the left middle side of your screen. Cut at least 2 trees for it to show. Disappears when no tree is cut for over 1 minutes till the next tree is cut.",
                config.treeGiftsPerHour, newValue -> config.treeGiftsPerHour = newValue);
    }

    private static final void addFarming(@NotNull final DarkUtilsConfig config, @NotNull final ConfigBuilder builder, @NotNull final ConfigEntryBuilder entryBuilder) {
        final var farming = builder.getOrCreateCategory(Text.of("Farming"));
        DarkUtilsConfigScreen.addSimpleBooleanToggle(entryBuilder, farming, "Pest Cooldown Display",
                "Shows a display with remaining pest cooldown, e.g., time until you can spawn your next pests.",
                config.pestCooldownDisplay, newValue -> config.pestCooldownDisplay = newValue);

        DarkUtilsConfigScreen.addSimpleIntegerSetting(entryBuilder, farming, "Pest Cooldown", "Pest cooldown in seconds. Minimum cooldown currently obtainable is 135 without Finnegan and 75 with Finnegan.", config.pestCooldown, newValue -> config.pestCooldown = newValue, 135, 75, 300);

        DarkUtilsConfigScreen.addSimpleBooleanToggle(entryBuilder, farming, "Persistent Tablist while Farming",
                "Always shows tablist while you are actively farming. Tablist shows useful info while farming in Garden, such as the percentage to the next crop milestone for the crop you are farming, the visitors in your garden, and more.",
                config.persistentTabListWhileFarming, newValue -> config.persistentTabListWhileFarming = newValue);

        DarkUtilsConfigScreen.addSimpleBooleanToggle(entryBuilder, farming, "Sticky Farming Keys",
                "Turns the keys to move forward, back, left, right and the key to attack into toggle instead of hold whilst you are farming. Note: Only one sticky movement key is active at a time. You must enable them individually below. Pressing any movement key will disable other sticky keys even if that movement key's sticky setting is disabled.",
                config.stickyFarmingKeys, newValue -> config.stickyFarmingKeys = newValue);

        DarkUtilsConfigScreen.addSimpleBooleanToggle(entryBuilder, farming, "Sticky Forward",
                "Controls whether to enable sticky forward.",
                config.stickyForward, newValue -> config.stickyForward = newValue);

        DarkUtilsConfigScreen.addSimpleBooleanToggle(entryBuilder, farming, "Sticky Backward",
                "Controls whether to enable sticky backward.",
                config.stickyBackward, newValue -> config.stickyBackward = newValue);

        DarkUtilsConfigScreen.addSimpleBooleanToggle(entryBuilder, farming, "Sticky Left",
                "Controls whether to enable sticky left.",
                config.stickyLeft, newValue -> config.stickyLeft = newValue);

        DarkUtilsConfigScreen.addSimpleBooleanToggle(entryBuilder, farming, "Sticky Right",
                "Controls whether to enable sticky right.",
                config.stickyRight, newValue -> config.stickyRight = newValue);

        DarkUtilsConfigScreen.addSimpleBooleanToggle(entryBuilder, farming, "Enforce Zorro's Cape",
                "Enforces having Zorro's Cape equipped before claiming any contest. You might need to open your equipment menu the first time to let the mod know you have it equipped, but after that the mod will remember your last equipped cloak.",
                config.enforceZorrosCape, newValue -> config.enforceZorrosCape = newValue);
    }

    private static final void addMining(@NotNull final DarkUtilsConfig config, @NotNull final ConfigBuilder builder, @NotNull final ConfigEntryBuilder entryBuilder) {
        final var mining = builder.getOrCreateCategory(Text.of("Mining"));
        DarkUtilsConfigScreen.addSimpleBooleanToggle(entryBuilder, mining, "Corpses Per Shaft Display",
                "Shows corpse statistics like % of found and opened corpses per shaft. Amount of shafts and opened corpses are automatically tracked when enabled.",
                config.corpsesPerShaftDisplay, newValue -> config.corpsesPerShaftDisplay = newValue);

        DarkUtilsConfigScreen.addSimpleBooleanToggle(entryBuilder, mining, "Mineshaft Display",
                "Shows mineshaft information like time passed since last shaft spawn, avg time between shaft spawns, despawn timer when a shaft spawns before you enter it, after entering shows uptime (how long have you been in the shaft) and warp close time (time till you can warp others) instead.",
                config.mineshaftDisplay, newValue -> config.mineshaftDisplay = newValue);
    }

    private static final void addDungeons(@NotNull final DarkUtilsConfig config, @NotNull final ConfigBuilder builder, @NotNull final ConfigEntryBuilder entryBuilder) {
        final var dungeons = builder.getOrCreateCategory(Text.of("Dungeons"));
        DarkUtilsConfigScreen.addSimpleBooleanToggle(entryBuilder, dungeons, "Dialogue Skip Timer",
                "Displays a timer for when to kill blood mobs to perform a watcher dialogue skip, speeding up the blood camp.",
                config.dialogueSkipTimer, newValue -> config.dialogueSkipTimer = newValue);

        DarkUtilsConfigScreen.addSimpleBooleanToggle(entryBuilder, dungeons, "Solo Crush Timer",
                "Displays a timer for when to pad to crush storm solo in purple pad. Use wither cloak or a mask to bypass lightning while in pad. Pad when instructed on screen, after padding move to align with crusher immediately so that storm gets crushed when it moves towards you.",
                config.soloCrushTimer, newValue -> config.soloCrushTimer = newValue);

        DarkUtilsConfigScreen.addSimpleBooleanToggle(entryBuilder, dungeons, "Solo Crush Waypoint",
                "Renders a waypoint for the block you need to lower the purple crusher into so that it can crush Storm the next time you lower it. You first need to lower it all the way down and then all the way up, and once you lower it down 1 time afterwards, the lowest block at the center of crusher should be inside this waypoint, and the next time you lower it storm will be crushed if you are aligned correctly behind it.",
                config.soloCrushWaypoint, newValue -> config.soloCrushWaypoint = newValue);

        DarkUtilsConfigScreen.addSimpleBooleanToggle(entryBuilder, dungeons, "Auto Close Secret Chests",
                "Automatically closes secret chests instantly inside Dungeons, as if you were holding a bow while opening it, even if you weren't.",
                config.autoCloseSecretChests, newValue -> config.autoCloseSecretChests = newValue);

        DarkUtilsConfigScreen.addSimpleBooleanToggle(entryBuilder, dungeons, "Replace Diorite",
                "Replaces diorite in the crushers in Storm phase with stained glass of the pad color.",
                config.replaceDiorite, newValue -> config.replaceDiorite = newValue);

        DarkUtilsConfigScreen.addSimpleBooleanToggle(entryBuilder, dungeons, "Arrow Alignment Device Solver",
                "Solves the arrow alignment device in Goldor phase.",
                config.arrowAlignmentDeviceSolver, newValue -> config.arrowAlignmentDeviceSolver = newValue);

        DarkUtilsConfigScreen.addSimpleBooleanToggle(entryBuilder, dungeons, "Arrow Alignment Device Solver Predev",
                "Makes the solver work even if Phase 3 did not start yet, enable if you are Healer and do predev.",
                config.arrowAlignmentDeviceSolverPredev, newValue -> config.arrowAlignmentDeviceSolverPredev = newValue);

        DarkUtilsConfigScreen.addSimpleBooleanToggle(entryBuilder, dungeons, "Arrow Alignment Device Solver Block Incorrect Clicks",
                "Blocks incorrect clicks after the arrow has been rotated enough times. Compatible with AutoClicker so you can finish it fast with it.",
                config.arrowAlignmentDeviceSolverBlockIncorrectClicks, newValue -> config.arrowAlignmentDeviceSolverBlockIncorrectClicks = newValue);

        DarkUtilsConfigScreen.addDungeonsSecond(config, entryBuilder, dungeons);
    }

    private static final void addDungeonsSecond(@NotNull final DarkUtilsConfig config, @NotNull final ConfigEntryBuilder entryBuilder, @NotNull final ConfigCategory dungeons) {
        DarkUtilsConfigScreen.addSimpleBooleanToggle(entryBuilder, dungeons, "Arrow Stack Waypoints",
                "Displays arrow stack waypoints in the Wither King dragon fight showing where to shoot your Last Breath arrows for optimal stacking.",
                config.arrowStackWaypoints, newValue -> config.arrowStackWaypoints = newValue);

        DarkUtilsConfigScreen.addSimpleBooleanToggle(entryBuilder, dungeons, "Dungeon Timer",
                "Displays dungeon timer on left side of the screen, showing time took to finish each phase. Shows live time if not finished yet, and shows how much time is lost to server lag, with fancy item icons, separate colors, and non-phase informational splits, like Boss Total.",
                config.dungeonTimer, newValue -> config.dungeonTimer = newValue);

        DarkUtilsConfigScreen.addSimpleFloatSetting(entryBuilder, dungeons, "Dungeon Timer Scale", "Makes the Dungeon Timer HUD show smaller or bigger. Use 1.0 to keep the original size, 0.9 to make it 10% smaller, 1.1 to make it 10% bigger, 0.5 to make it half the size, 1.5 to make it 50% bigger, etc. Note: Anything other than 1.0 will cause text font to look uglier due to the way vanilla font renderer scales text.", config.dungeonTimerScale, newValue -> config.dungeonTimerScale = newValue, 1.0F, 0.5F, 1.5F);

        DarkUtilsConfigScreen.addSimpleIntegerSetting(entryBuilder, dungeons, "Dungeon Timer Offset X", "X offset for render position of the Dungeon Timer HUD. For example, 0 keeps original position which is +2px to the right of leftmost of the screen by default, while 50 moves it 50px right, and -2 makes it render at leftmost since original position is +2px.", config.dungeonTimerOffsetX, newValue -> config.dungeonTimerOffsetX = newValue, 0, -2, Integer.MAX_VALUE);

        DarkUtilsConfigScreen.addSimpleIntegerSetting(entryBuilder, dungeons, "Dungeon Timer Offset Y", "Y offset for render position of the Dungeon Timer HUD. For example, 0 keeps original position which is dynamically the middle of the screen by default, while 50 moves the timer down by 50px, and -50 moves it up by 50px.", config.dungeonTimerOffsetY, newValue -> config.dungeonTimerOffsetY = newValue, 0, Integer.MIN_VALUE, Integer.MAX_VALUE);

        DarkUtilsConfigScreen.addSimpleBooleanToggle(entryBuilder, dungeons, "Dungeon Timer No Item Icon",
                "When enabled, disables the item icons rendering before the text. Default disabled to make the HUD look cooler, but you can disable the item icons if you prefer the HUD simpler with text-only by enabling this option.",
                config.dungeonTimerNoItemIcon, newValue -> config.dungeonTimerNoItemIcon = newValue);

        DarkUtilsConfigScreen.addSimpleBooleanToggle(entryBuilder, dungeons, "Blood Cleared Notification",
                "Shows a message on screen when the blood room is cleared with how much time it took for it.",
                config.bloodClearedNotification, newValue -> config.bloodClearedNotification = newValue);
    }

    private static final void addVisualTweaks(@NotNull final DarkUtilsConfig config, @NotNull final ConfigBuilder builder, @NotNull final ConfigEntryBuilder entryBuilder) {
        final var visual = builder.getOrCreateCategory(Text.of("Visual Tweaks"));
        DarkUtilsConfigScreen.addSimpleBooleanToggle(entryBuilder, visual, "Hide Effects HUD",
                "Hides the potion/effects HUD on the top right of the screen for less visual clutter on screen. You can still see your effects when you open your inventory on the side.",
                config.hideEffectsHud, newValue -> config.hideEffectsHud = newValue);

        DarkUtilsConfigScreen.addSimpleBooleanToggle(entryBuilder, visual, "Hide Effects In Inventory",
                "Hides the potion/effects displayed on the left or right side of your player inventory.",
                config.hideEffectsInInventory, newValue -> config.hideEffectsInInventory = newValue);

        DarkUtilsConfigScreen.addSimpleBooleanToggle(entryBuilder, visual, "Transparent Player List",
                "Makes Player List (Tab List) transparent for better visuals.",
                config.transparentPlayerList, newValue -> config.transparentPlayerList = newValue);

        DarkUtilsConfigScreen.addSimpleBooleanToggle(entryBuilder, visual, "Remove Chat Scrollbar",
                "Removes the little chat scrollbar in the right side of the chat from rendering.",
                config.removeChatScrollbar, newValue -> config.removeChatScrollbar = newValue);

        DarkUtilsConfigScreen.addSimpleBooleanToggle(entryBuilder, visual, "Fullbright",
                "Makes the game appear as if everything had the best light level source.",
                config.fullbright, newValue -> config.fullbright = newValue);

        DarkUtilsConfigScreen.addSimpleBooleanToggle(entryBuilder, visual, "Night Vision",
                "Makes the game appear as if you have night vision potion effect, even if you don't.",
                config.nightVision, newValue -> config.nightVision = newValue);

        DarkUtilsConfigScreen.addSimpleBooleanToggle(entryBuilder, visual, "Hide Fire Overlay",
                "Hides the fire overlay, which is shown when you are burning. Useful for when you have permanent Fire Resistance, such as in Hypixel Skyblock.",
                config.hideFireOverlay, newValue -> config.hideFireOverlay = newValue);

        DarkUtilsConfigScreen.addSimpleBooleanToggle(entryBuilder, visual, "No Burning Entities",
                "Removes the burning fire rendered on all entities when they are on fire for a cleaner look. They will still take damage from being burned, e.g. a zombie under the sun, but the burning fire won't be rendered.",
                config.noBurningEntities, newValue -> config.noBurningEntities = newValue);

        DarkUtilsConfigScreen.addSimpleBooleanToggle(entryBuilder, visual, "Hide Armor and Food",
                "Hides the vanilla armor and food bars, as they are irrelevant in Hypixel Skyblock.",
                config.hideArmorAndFood, newValue -> config.hideArmorAndFood = newValue);

        DarkUtilsConfigScreen.addSimpleBooleanToggle(entryBuilder, visual, "Hide Mount Health",
                "Hides the vanilla mount health bar, as in most servers mounts are immune and used for animational purposes.",
                config.hideMountHealth, newValue -> config.hideMountHealth = newValue);

        DarkUtilsConfigScreen.addSimpleBooleanToggle(entryBuilder, visual, "No Lightning Bolts",
                "Hides lightning bolts.",
                config.noLightningBolts, newValue -> config.noLightningBolts = newValue);

        DarkUtilsConfigScreen.addSimpleBooleanToggle(entryBuilder, visual, "No Wither Hearts",
                "Skips making your hearts black when you have the Wither status effect, allowing you to always be able to see your Health clearly.",
                config.noWitherHearts, newValue -> config.noWitherHearts = newValue);
    }

    private static final void addPerformance(@NotNull final DarkUtilsConfig config, @NotNull final ConfigBuilder builder, @NotNull final ConfigEntryBuilder entryBuilder) {
        final var performance = builder.getOrCreateCategory(Text.of("Performance"));
        DarkUtilsConfigScreen.addSimpleBooleanToggle(entryBuilder, performance, "Disable Yield",
                "Disables thread yielding for performance. Vanilla Minecraft yields through Render thread after finishing rendering a frame before starting to render the next frame, which reduces the potential maximum FPS. Disabling the yielding improves FPS.",
                config.disableYield, newValue -> config.disableYield = newValue);

        DarkUtilsConfigScreen.addSimpleBooleanToggle(entryBuilder, performance, "Always Prioritize Render Thread",
                "Forces Render thread priority at maximum. Vanilla already does this for processors with more than or equal to 4 threads, but always prioritizing is better.",
                config.alwaysPrioritizeRenderThread, newValue -> config.alwaysPrioritizeRenderThread = newValue);

        DarkUtilsConfigScreen.addSimpleBooleanToggle(entryBuilder, performance, "Optimize Exceptions",
                "Optimizes exceptions in vanilla Minecraft with non-vanilla servers, when servers send unexpected data, improving performance and preventing log file bloat/writes to disk, which saves SSD lifetime as well.",
                config.optimizeExceptions, newValue -> config.optimizeExceptions = newValue);

        DarkUtilsConfigScreen.addSimpleBooleanToggle(entryBuilder, performance, "Always Use No Error Context",
                "Makes Sodium always use no error context, even if you are on wayland.",
                config.alwaysUseNoErrorContext, newValue -> config.alwaysUseNoErrorContext = newValue);

        DarkUtilsConfigScreen.addSimpleBooleanToggle(entryBuilder, performance, "Disable Error Checking Entirely",
                "Overrides glGetError to always return no error to disable error checking completely.",
                config.disableErrorCheckingEntirely, newValue -> config.disableErrorCheckingEntirely = newValue);

        DarkUtilsConfigScreen.addSimpleBooleanToggle(entryBuilder, performance, "Re-Enable AMD Game Optimizations",
                "Sodium disables some of the AMD driver's game optimizations for Minecraft due to bugs with a workaround. This option re-enables it. It is not guaranteed this would do anything as it depends on driver. Additionally, the workaround was not supposed to be applied in Linux systems, but is bugged, enabling this will fix it.",
                config.reEnableAmdGameOptimizations, newValue -> config.reEnableAmdGameOptimizations = newValue);

        DarkUtilsConfigScreen.addSimpleBooleanToggle(entryBuilder, performance, "Disable Campfire Smoke Particles",
                "Disables campfire smoke particles, which optimizes memory allocation rate of the game, due to smoke particles calling entity collision code for some reason.",
                config.disableCampfireSmokeParticles, newValue -> config.disableCampfireSmokeParticles = newValue);

        DarkUtilsConfigScreen.addSimpleBooleanToggle(entryBuilder, performance, "Remove Main Menu Frame Limit",
                "Removes frame limit of hardcoded 60 in the main menu, allowing the game to take advantage of your high refresh rate screen.",
                config.removeMainMenuFrameLimit, newValue -> config.removeMainMenuFrameLimit = newValue);

        DarkUtilsConfigScreen.addSimpleBooleanToggle(entryBuilder, performance, "Log Cleaner",
                "Cleans old log entries in the logs folder.",
                config.logCleaner, newValue -> config.logCleaner = newValue);

        DarkUtilsConfigScreen.addSimpleBooleanToggle(entryBuilder, performance, "Stop Light Updates",
                "Stops light updates. If you have Fullbright, this will improve performance while affecting pretty much nothing.",
                config.stopLightUpdates, newValue -> config.stopLightUpdates = newValue);

        DarkUtilsConfigScreen.addSimpleBooleanToggle(entryBuilder, performance, "No Memory Reserve",
                "Removes the 10 MB memory reserve Minecraft allocates that's never freed unless your game crashes, freeing it to be used for other stuff. This should generally have zero downsides, even if you crash or if 10 MB is too insignificant for you, it's better for the Java Garbage Collector to have more free memory to work with.",
                config.noMemoryReserve, newValue -> config.noMemoryReserve = newValue);

        DarkUtilsConfigScreen.addPerformanceSecond(config, entryBuilder, performance);
    }

    private static final void addPerformanceSecond(@NotNull final DarkUtilsConfig config, @NotNull final ConfigEntryBuilder entryBuilder, @NotNull final ConfigCategory performance) {
        DarkUtilsConfigScreen.addSimpleBooleanToggle(entryBuilder, performance, "Optimize Enum Values",
                "Optimizes memory allocation rate by eliminating enum values array copying in some places, currently a single place.",
                config.optimizeEnumValues, newValue -> config.optimizeEnumValues = newValue);

        DarkUtilsConfigScreen.addEnumSetting(entryBuilder, performance, "OpenGL Version Override", "Allows you to forcefully modify the OpenGL version Minecraft requests during Window context creation to a modern OpenGL version. By default Minecraft requests OpenGL 3.3. This does not magically make Minecraft take advantage of new additions from later OpenGL specifications, but it will ensure its future-proof. Note however, your game might become unbootable if you set this to a higher value than what your GPU supports. Generally speaking, GPUs from the last decade should all support OpenGL 4.6 with latest drivers installed.", config.openGLVersionOverride, newValue -> config.openGLVersionOverride = newValue, OpenGLVersionOverride.NO_OVERRIDE, OpenGLVersionOverride::prettyName);

        DarkUtilsConfigScreen.addSimpleBooleanToggle(entryBuilder, performance, "Use Virtual Threads for Texture Downloading",
                "Makes Minecraft use Java's new (Lightweight) Virtual Threads over Platform (OS) Threads. Normally, Minecraft uses a Cached Thread Pool which ends up creating hundreds of texture downloading threads in texture-heavy game-modes like Hypixel SkyBlock where items have a player skull model. Those hundreds of texture downloading threads all have their separate stack, and there is a limit to how many platform threads you can create in the OS level at which point it will crash. Virtual Threads are a lightweight new technology replacement that only creates threads when tasks are blocked and this also made texture loading speedier during tests due to creating a new (platform/OS) thread not being a free operation.",
                config.useVirtualThreadsForTextureDownloading, newValue -> config.useVirtualThreadsForTextureDownloading = newValue);

        DarkUtilsConfigScreen.addSimpleBooleanToggle(entryBuilder, performance, "Disable Glowing",
                "Disables all glowing, which reduce FPS a lot if your graphics card is not capable. Glowed entities are rendered behind walls, so no culling of them which reduces performance. Only enable if you do not care about: seeing your teammates glow with Hypixel rank color (e.g. green for VIP, blue for MVP+) in Dungeons, dropped items glowing with their rarity color (e.g. orange for legendary items), frogs in galatea glowing white, and possibly more glowing effects will be disabled for performance.",
                config.disableGlowing, newValue -> config.disableGlowing = newValue);

        DarkUtilsConfigScreen.addSimpleBooleanToggle(entryBuilder, performance, "Sound Lag Fix",
                "Skips playing duplicate sounds received on the same tick from misbehaving or lagging servers from causing lag in your system, preventing the audio engine from being overloaded/sound pool getting full. Only identical sounds are prevented so you can still hear everything perfectly.",
                config.soundLagFix, newValue -> config.soundLagFix = newValue);

        DarkUtilsConfigScreen.addSimpleBooleanToggle(entryBuilder, performance, "Thread Priority Tweaker",
                "Improves performance by tweaking priorities of all threads in the background regularly.",
                config.threadPriorityTweaker, newValue -> config.threadPriorityTweaker = newValue);

        DarkUtilsConfigScreen.addSimpleBooleanToggle(entryBuilder, performance, "Disable Signature Verification",
                "Completely disables signature verification. This might fix some texture errors and will improve performance by skipping RSA encryption validation. This is safe if you trust the server you join to not send unverified textures.",
                config.disableSignatureVerification, newValue -> config.disableSignatureVerification = newValue);

        DarkUtilsConfigScreen.addSimpleBooleanToggle(entryBuilder, performance, "Block Entity Unload Lag Fix",
                "Fixes a bug in Minecraft's bug tracker causing lag when unloading a large amount of block entities.",
                config.blockEntityUnloadLagFix, newValue -> config.blockEntityUnloadLagFix = newValue);

        DarkUtilsConfigScreen.addSimpleBooleanToggle(entryBuilder, performance, "Viewport Cache",
                "Caches viewport GL calls if the viewport did not change, improving performance.",
                config.viewportCache, newValue -> config.viewportCache = newValue);
    }

    private static final void addBugfixes(@NotNull final DarkUtilsConfig config, @NotNull final ConfigBuilder builder, @NotNull final ConfigEntryBuilder entryBuilder) {
        final var bugfixes = builder.getOrCreateCategory(Text.of("Bugfixes"));
        DarkUtilsConfigScreen.addSimpleBooleanToggle(entryBuilder, bugfixes, "Fix GUI Scale After Toggling Fullscreen Off",
                "Fixes GUI scale getting tiny after leaving fullscreen.",
                config.fixGuiScaleAfterFullscreen, newValue -> config.fixGuiScaleAfterFullscreen = newValue);

        DarkUtilsConfigScreen.addSimpleBooleanToggle(entryBuilder, bugfixes, "Fix Inactivity FPS Limiter",
                "Fixes inactivity FPS limiter defaulting to 10 FPS limit before the first input is received.",
                config.fixInactivityFpsLimiter, newValue -> config.fixInactivityFpsLimiter = newValue);

        DarkUtilsConfigScreen.addSimpleBooleanToggle(entryBuilder, bugfixes, "Item Frame Sound Fix",
                "Fixes a bug in Minecraft's bug tracker causing item frames to play a sound when they should not in some cases.",
                config.itemFrameSoundFix, newValue -> config.itemFrameSoundFix = newValue);

        DarkUtilsConfigScreen.addSimpleBooleanToggle(entryBuilder, bugfixes, "Cursor Fix",
                "Fixes a bug where the mouse cursor stays on screen after closing a menu that set a custom cursor but forgot to revert it.",
                config.cursorFix, newValue -> config.cursorFix = newValue);

        DarkUtilsConfigScreen.addSimpleBooleanToggle(entryBuilder, bugfixes, "Middle Click Fix",
                "Allows you to middle click when hovering over items like in 1.8, such as to disable Witherborn ability of your armor.",
                config.middleClickFix, newValue -> config.middleClickFix = newValue);

        DarkUtilsConfigScreen.addSimpleBooleanToggle(entryBuilder, bugfixes, "Cursor Pos Wayland GL Error Fix",
                "Fixes a bug where Minecraft tries to call glfwSetCursorPos in Wayland desktop environment, where setting the cursor position is not supported, by cancelling the call. This prevents the GL error while preserving behaviour, as setting the position fails with the GL error anyways.",
                config.cursorPosWaylandGLErrorFix, newValue -> config.cursorPosWaylandGLErrorFix = newValue);
    }

    private static final void addDevelopment(@NotNull final DarkUtilsConfig config, @NotNull final ConfigBuilder builder, @NotNull final ConfigEntryBuilder entryBuilder) {
        final var development = builder.getOrCreateCategory(Text.of("Development"));

        DarkUtilsConfigScreen.addEnumSetting(entryBuilder, development, "In-Game Log Level", "Allows you to change at what threshold log messages should also be printed to in-game chat for development. Do not change unless instructed or know what you are doing.", config.ingameLogLevel, newValue -> config.ingameLogLevel = newValue, LogLevel.WARN, LogLevel::prettyName);

        DarkUtilsConfigScreen.addSimpleBooleanToggle(entryBuilder, development, "Debug Mode",
                "Prints additional details like the full call stack in some code paths for debugging purposes. Might incur a performance penalty. Do not enable unless instructed to do so.",
                config.debugMode, newValue -> config.debugMode = newValue);
    }

    public static final @NotNull Screen create(@Nullable final Screen parent) {
        ConfigScreenOpenEvent.INSTANCE.trigger();

        final var config = DarkUtilsConfig.INSTANCE;

        final var version = DarkUtils.getModAndMcVersion();

        final var builder = ConfigBuilder.create()
                .setParentScreen(parent)
                .setTitle(Text.of("DarkUtils v" + version.first() + " for Minecraft " + version.second() + " Settings"))
                .setSavingRunnable(DarkUtilsConfig::save);

        final var entryBuilder = builder.entryBuilder();

        // === Quality of Life ===
        DarkUtilsConfigScreen.addQualityOfLife(config, builder, entryBuilder);

        // === Foraging ===
        DarkUtilsConfigScreen.addForaging(config, builder, entryBuilder);

        // === Farming ===
        DarkUtilsConfigScreen.addFarming(config, builder, entryBuilder);

        // === Mining ===
        DarkUtilsConfigScreen.addMining(config, builder, entryBuilder);

        // === Dungeons ===
        DarkUtilsConfigScreen.addDungeons(config, builder, entryBuilder);

        // === Visual Tweaks ===
        DarkUtilsConfigScreen.addVisualTweaks(config, builder, entryBuilder);

        // === Performance ===
        DarkUtilsConfigScreen.addPerformance(config, builder, entryBuilder);

        // === Bugfixes ===
        DarkUtilsConfigScreen.addBugfixes(config, builder, entryBuilder);

        // === Development ===
        DarkUtilsConfigScreen.addDevelopment(config, builder, entryBuilder);

        return builder.build();
    }
}

