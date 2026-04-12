# There's some notes which we can't fix about duplicate module-info class since we include the compileClasspath as libraryjars
-dontnote

# Let's do a good amount of passes to get the most out of optimization.
-optimizationpasses 10

# We don't want obfuscation as it complicates error stacktraces, heapdumps, profilers, etc.,
# and makes the mod look way more shady. While obfuscation can unlock some optimizations
# for shrinking (e.g by repackaging everythinig to the root package it can turn access levels to package-private and merge classes aggressively),
# it does not unlock any relevant optimization on the actual bytecode that I am aware of.
# Besides, even vanilla Minecraft is going to be not obfuscated starting from 26.1.
-dontobfuscate

# Class name read from fabric.mod.json (must not have allowshrinking)
# Constructor called reflectively (must keep public <init>() explicitly)
# Casted to interface to call the onInitializeClient method (no need for explicit keep as interface contract keeps it)
-keep,allowoptimization class gg.darkutils.DarkUtils implements net.fabricmc.api.ClientModInitializer {
    public <init>();
}

# Class name read from fabric.mod.json (must not have allowshrinking)
# Constructor called reflectively (must keep public <init>() explicitly)
# Casted to interface to call the getModConfigScreenFactory method (no need for explicit keep as interface contract keeps it)
-keep,allowoptimization class gg.darkutils.config.DarkUtilsModMenu implements com.terraformersmc.modmenu.api.ModMenuApi {
    public <init>();
}

# If allowoptimization ProGuard feels free to mess with arguments of the @Inject, @Redirect, @Overwrite methods which
# causes all sorts of errors and injection failures in runtime.
# Mixin code is also all effectively dead code as it's only referenced by darkutils.mixins.json so we don't want allowshrinking either.
-keep class gg.darkutils.mixin.** {
    *;
}

# We use it in the config with GSON serialization.
-keepclassmembers enum gg.darkutils.feat.performance.OpenGLVersionOverride {
    private static synthetic final ***[] $VALUES;
    public static ***[] values();
}

# We use it in the update checker with GSON serialization,
# which needs the canonical record constructor and all the accessor methods.
-keepclassmembers class gg.darkutils.update.UpdateChecker$GitHubRelease extends java.lang.Record {
    public <init>(...);
    public *** *();
}

# Disable some of the possibly problematic optimizations.
# Some of these either cause a crash of loom's jar remap task or a crash in runtime when fabric's class loader
# is discovering our classes (code/allocation/variable, code/simplification/variable)
# While others end up widening accessibility instead of lowering to private because ProGuard does not understand
# the concept of nestmates added in modern java that allows private members to be accessed from outer class without synthetic accessors.
-optimizations !code/allocation/variable,!method/marking/static,!method/marking/private,!field/marking/private,!code/simplification/variable

# ProGuard removes the $VALUES field and the public values() method,
# but it does not remove the private synthetic method $values(), which
# is called by the static initializer block, causing unnecessary call during class init
# that creates an unused array and does nothing. This rule optimizes it out.
-assumenosideeffects class * extends java.lang.Enum {
    private static synthetic ***[] *(...);
}

# These methods simply get a StackWalker instance. While they might
# initialize some VM state about stack walking earlier, it is of no big signifiance
# for us, thus eliminating it if unused is benefitical.
-assumenosideeffects class java.lang.StackWalker {
    public static java.lang.StackWalker getInstance(java.lang.StackWalker$Option);
    public static java.lang.StackWalker getInstance(java.util.Set);
    public static java.lang.StackWalker getInstance(java.util.Set,int);
}
