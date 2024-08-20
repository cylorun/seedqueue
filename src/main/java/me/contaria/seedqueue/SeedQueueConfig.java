package me.contaria.seedqueue;

import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.google.gson.JsonPrimitive;
import me.contaria.seedqueue.compat.ModCompat;
import me.contaria.seedqueue.gui.config.SeedQueueKeybindingsScreen;
import me.contaria.seedqueue.gui.config.SeedQueueWindowSizeWidget;
import me.contaria.seedqueue.keybindings.SeedQueueKeyBindings;
import me.contaria.seedqueue.keybindings.SeedQueueMultiKeyBinding;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.ConfirmScreen;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.screen.ScreenTexts;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.text.StringRenderable;
import net.minecraft.text.TranslatableText;
import org.jetbrains.annotations.Nullable;
import org.mcsr.speedrunapi.config.SpeedrunConfigAPI;
import org.mcsr.speedrunapi.config.SpeedrunConfigContainer;
import org.mcsr.speedrunapi.config.api.SpeedrunConfig;
import org.mcsr.speedrunapi.config.api.SpeedrunOption;
import org.mcsr.speedrunapi.config.api.annotations.Config;
import org.mcsr.speedrunapi.config.api.annotations.InitializeOn;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Config class based on SpeedrunAPI, initialized on prelaunch.
 * <p>
 * When implementing new options, make sure no Minecraft classes are loaded during initialization!
 */
@SuppressWarnings("FieldMayBeFinal")
@InitializeOn(InitializeOn.InitPoint.PRELAUNCH)
public class SeedQueueConfig implements SpeedrunConfig {
    @Config.Ignored
    static final int AUTO_THREADS = 0;

    @Config.Ignored
    static final int AUTO = -1;

    @Config.Ignored
    private static final int PROCESSORS = Runtime.getRuntime().availableProcessors();

    @Config.Ignored
    private SpeedrunConfigContainer<?> container;

    @Config.Category("queue")
    @Config.Numbers.Whole.Bounds(min = -1, max = 30, enforce = Config.Numbers.EnforceBounds.MIN_ONLY)
    private int maxCapacity = AUTO;

    @Config.Category("queue")
    @Config.Numbers.Whole.Bounds(min = -1, max = 30, enforce = Config.Numbers.EnforceBounds.MIN_ONLY)
    private int maxConcurrently = AUTO;

    @Config.Category("queue")
    @Config.Numbers.Whole.Bounds(min = -1, max = 30, enforce = Config.Numbers.EnforceBounds.MIN_ONLY)
    private int maxConcurrently_onWall = AUTO;

    @Config.Category("queue")
    @Config.Numbers.Whole.Bounds(min = -1, max = 100, enforce = Config.Numbers.EnforceBounds.MIN_ONLY)
    private int maxWorldGenerationPercentage = AUTO;

    @Config.Category("chunkmap")
    public ChunkMapVisibility chunkMapVisibility = ChunkMapVisibility.TRUE;

    @Config.Category("chunkmap")
    @Config.Numbers.Whole.Bounds(min = 1, max = 5)
    public int chunkMapScale = 2;

    @Config.Ignored
    public final boolean canUseWall = ModCompat.HAS_WORLDPREVIEW && ModCompat.HAS_STANDARDSETTINGS && ModCompat.HAS_SODIUM;

    @Config.Category("wall")
    public boolean useWall = false;

    @Config.Category("wall")
    @Config.Numbers.Whole.Bounds(min = 1, max = 10)
    public int rows = 2;

    @Config.Category("wall")
    @Config.Numbers.Whole.Bounds(min = 1, max = 10)
    public int columns = 2;

    @Config.Category("wall")
    public final WindowSize simulatedWindowSize = new WindowSize();

    @Config.Category("wall")
    @Config.Numbers.Whole.Bounds(max = 1000)
    public int resetCooldown = 150;

    @Config.Category("wall")
    public boolean waitForPreviewSetup = true;

    @Config.Category("wall")
    public boolean bypassWall = false;

    @Config.Category("performance")
    @Config.Numbers.Whole.Bounds(min = 1, max = 255)
    public int wallFPS = 60;

    @Config.Category("performance")
    @Config.Numbers.Whole.Bounds(min = 1, max = 255)
    public int previewFPS = 15;

    @Config.Category("performance")
    @Config.Numbers.Whole.Bounds(min = -1, max = 30, enforce = Config.Numbers.EnforceBounds.MIN_ONLY)
    public int backgroundPreviews = AUTO;

    @Config.Category("performance")
    public boolean freezeLockedPreviews = false;

    @Config.Category("performance")
    public boolean reduceSchedulingBudget = false;

    @Config.Category("performance")
    public boolean reduceLevelList = true;

    @Config.Category("advanced")
    public boolean showAdvancedSettings = false;

    @Config.Category("threading")
    @Config.Numbers.Whole.Bounds(min = Thread.MIN_PRIORITY, max = Thread.NORM_PRIORITY)
    public int seedQueueThreadPriority = Thread.NORM_PRIORITY;

    @Config.Category("threading")
    @Config.Numbers.Whole.Bounds(min = Thread.MIN_PRIORITY, max = Thread.NORM_PRIORITY)
    public int serverThreadPriority = 4;

    @Config.Category("threading")
    @Config.Numbers.Whole.Bounds(min = -1, max = 32, enforce = Config.Numbers.EnforceBounds.MIN_ONLY)
    protected int backgroundExecutorThreads = AUTO_THREADS;

    @Config.Category("threading")
    @Config.Numbers.Whole.Bounds(min = Thread.MIN_PRIORITY, max = Thread.NORM_PRIORITY)
    public int backgroundExecutorThreadPriority = 3;

    @Config.Category("threading")
    @Config.Numbers.Whole.Bounds(min = -1, max = 32, enforce = Config.Numbers.EnforceBounds.MIN_ONLY)
    protected int wallExecutorThreads = AUTO_THREADS;

    @Config.Category("threading")
    @Config.Numbers.Whole.Bounds(min = Thread.MIN_PRIORITY, max = Thread.NORM_PRIORITY)
    public int wallExecutorThreadPriority = 4;

    @Config.Category("threading")
    @Config.Numbers.Whole.Bounds(min = -1, max = 8, enforce = Config.Numbers.EnforceBounds.MIN_ONLY)
    private int chunkUpdateThreads = AUTO_THREADS;

    @Config.Category("threading")
    @Config.Numbers.Whole.Bounds(min = Thread.MIN_PRIORITY, max = Thread.NORM_PRIORITY)
    public int chunkUpdateThreadPriority = 3;

    @Config.Category("debug")
    public boolean showDebugMenu = false;

    @Config.Category("debug")
    @Config.Numbers.Whole.Bounds(min = 1, max = Integer.MAX_VALUE)
    @Config.Numbers.TextField
    public int benchmarkResets = 1000;

    @Config.Category("debug")
    public boolean useWatchdog = false;

    @Config.Category("wall")
    public final SeedQueueMultiKeyBinding[] keyBindings = new SeedQueueMultiKeyBinding[]{
            SeedQueueKeyBindings.play,
            SeedQueueKeyBindings.focusReset,
            SeedQueueKeyBindings.reset,
            SeedQueueKeyBindings.lock,
            SeedQueueKeyBindings.resetAll,
            SeedQueueKeyBindings.resetColumn,
            SeedQueueKeyBindings.resetRow,
            SeedQueueKeyBindings.playNextLock,
            SeedQueueKeyBindings.startBenchmark,
            SeedQueueKeyBindings.cancelBenchmark
    };

    {
        SeedQueue.config = this;
    }

    /**
     * Returns the amount of threads the Background Executor should use according to {@link SeedQueueConfig#backgroundExecutorThreads}.
     * Calculates a good default based on {@link SeedQueueConfig#maxConcurrently} if set to {@link SeedQueueConfig#AUTO_THREADS}.
     *
     * @return The parallelism to be used for the Background Executor Service.
     */
    public int getBackgroundExecutorThreads() {
        if (this.backgroundExecutorThreads == AUTO_THREADS) {
            return Math.max(1, Math.min(this.maxConcurrently + 1, PROCESSORS));
        }
        return this.backgroundExecutorThreads;
    }

    /**
     * Returns the amount of threads the Wall Executor should use according to {@link SeedQueueConfig#wallExecutorThreads}.
     * The amount of available processors is used if set to {@link SeedQueueConfig#AUTO_THREADS}.
     *
     * @return The parallelism to be used for the Background Executor Service.
     */
    public int getWallExecutorThreads() {
        if (this.wallExecutorThreads == AUTO_THREADS) {
            return Math.max(1, PROCESSORS);
        }
        return this.wallExecutorThreads;
    }

    /**
     * Returns the amount of worker threads created PER WorldRenderer on the Wall Screen according to {@link SeedQueueConfig#chunkUpdateThreads}.
     * Calculates a sane default if set to {@link SeedQueueConfig#AUTO_THREADS}.
     *
     * @return The amount of Sodium worker threads to launch on the Wall Screen.
     */
    public int getChunkUpdateThreads() {
        if (this.chunkUpdateThreads == AUTO_THREADS) {
            return Math.min(Math.max(2, (int) Math.ceil((double) PROCESSORS / this.maxConcurrently_onWall)), PROCESSORS);
        }
        return this.chunkUpdateThreads;
    }

    /**
     * Returns the amount of threads the Background Executor should use according to {@link SeedQueueConfig#backgroundPreviews}.
     * Calculates a good default based on {@link SeedQueueConfig#maxConcurrently_onWall} and {@link SeedQueueConfig#maxCapacity} if set to {@link SeedQueueConfig#AUTO_THREADS}.
     *
     * @return The amount of preview to be loaded in the background.
     */
    public int getBackgroundPreviews() {
        if (this.backgroundPreviews == AUTO) {
            return Math.min(this.maxConcurrently_onWall, this.maxCapacity - this.maxConcurrently_onWall);
        }
        return this.backgroundPreviews;
    }

    public int getMaxWorldGenerationPercentage() {
        if (this.maxWorldGenerationPercentage == AUTO) {
            return 15;
        }
        return this.maxWorldGenerationPercentage;
    }

    public int getMaxConcurrently() {
        if (this.maxConcurrently == AUTO) {

        }
        return this.maxConcurrently;
    }

    public int getMaxCapacity() {
        if (this.maxCapacity == AUTO) {

        }
        return this.maxCapacity;
    }

    public int getMaxConcurrently_onWall() {
        if (this.maxConcurrently_onWall == AUTO) {

        }
        return this.maxConcurrently_onWall;
    }


    public boolean shouldUseWall() {
        return this.canUseWall && this.maxCapacity > 0 && this.useWall;
    }

    // see Window#calculateScaleFactor
    public int calculateSimulatedScaleFactor(int guiScale, boolean forceUnicodeFont) {
        int scaleFactor = 1;
        while (scaleFactor != guiScale && scaleFactor < this.simulatedWindowSize.width() && scaleFactor < this.simulatedWindowSize.height() && this.simulatedWindowSize.width() / (scaleFactor + 1) >= 320 && this.simulatedWindowSize.height() / (scaleFactor + 1) >= 240) {
            scaleFactor++;
        }
        if (forceUnicodeFont) {
            scaleFactor += guiScale % 2;
        }
        return scaleFactor;
    }

    @Override
    public @Nullable SpeedrunOption<?> parseField(Field field, SpeedrunConfig config, String... idPrefix) {
        if ("useWall".equals(field.getName())) {
            return new SpeedrunConfigAPI.CustomOption.Builder<Boolean>(config, this, field, idPrefix)
                    .createWidget((option, config_, configStorage, optionField) -> {
                        if (!this.canUseWall) {
                            ButtonWidget button = new ButtonWidget(0, 0, 150, 20, new TranslatableText("seedqueue.menu.config.useWall.notAvailable"), b -> {
                            }, ((b, matrices, mouseX, mouseY) -> {
                                List<StringRenderable> tooltip = new ArrayList<>(MinecraftClient.getInstance().textRenderer.wrapLines(new TranslatableText("seedqueue.menu.config.useWall.notAvailable.tooltip"), 200));
                                for (int i = 1; i <= 3; i++) {
                                    tooltip.add(new TranslatableText("seedqueue.menu.config.useWall.notAvailable.tooltip." + i));
                                }
                                Objects.requireNonNull(MinecraftClient.getInstance().currentScreen).renderTooltip(matrices, tooltip, mouseX, mouseY);
                            }));
                            button.active = false;
                            return button;
                        }
                        return new ButtonWidget(0, 0, 150, 20, ScreenTexts.getToggleText(option.get()), button -> {
                            option.set(!option.get());
                            button.setMessage(ScreenTexts.getToggleText(option.get()));
                        });
                    })
                    .build();
        }
        if ("showAdvancedSettings".equals(field.getName())) {
            return new SpeedrunConfigAPI.CustomOption.Builder<Boolean>(config, this, field, idPrefix)
                    .createWidget((option, config_, configStorage, optionField) -> new ButtonWidget(0, 0, 150, 20, ScreenTexts.getToggleText(option.get()), button -> {
                        if (!option.get()) {
                            Screen configScreen = MinecraftClient.getInstance().currentScreen;
                            MinecraftClient.getInstance().openScreen(new ConfirmScreen(confirm -> {
                                option.set(confirm);
                                MinecraftClient.getInstance().openScreen(configScreen);
                            }, new TranslatableText("seedqueue.menu.config.showAdvancedSettings.confirm.title"), new TranslatableText("seedqueue.menu.config.showAdvancedSettings.confirm.message"), ScreenTexts.YES, ScreenTexts.CANCEL));
                        } else {
                            option.set(false);
                            MinecraftClient.getInstance().openScreen(MinecraftClient.getInstance().currentScreen);
                        }
                    }))
                    .build();
        }
        if (WindowSize.class.equals(field.getType())) {
            return new SpeedrunConfigAPI.CustomOption.Builder<WindowSize>(config, this, field, idPrefix)
                    .fromJson((option, config_, configStorage, optionField, jsonElement) -> option.get().fromJson(jsonElement.getAsJsonObject()))
                    .toJson((option, config_, configStorage, optionField) -> option.get().toJson())
                    .setter((option, config_, configStorage, optionField, value) -> {
                        throw new UnsupportedOperationException();
                    })
                    .createWidget((option, config_, configStorage, optionField) -> new SeedQueueWindowSizeWidget(option.get()))
                    .build();
        }
        if (SeedQueueMultiKeyBinding[].class.equals(field.getType())) {
            return new SpeedrunConfigAPI.CustomOption.Builder<SeedQueueMultiKeyBinding[]>(config, this, field, idPrefix)
                    .fromJson((option, config_, configStorage, optionField, jsonElement) -> {
                        for (SeedQueueMultiKeyBinding keyBinding : option.get()) {
                            keyBinding.fromJson(jsonElement.getAsJsonObject().get(keyBinding.getTranslationKey()));
                        }
                    })
                    .toJson((option, config_, configStorage, optionField) -> {
                        JsonObject jsonObject = new JsonObject();
                        for (SeedQueueMultiKeyBinding keyBinding : option.get()) {
                            jsonObject.add(keyBinding.getTranslationKey(), keyBinding.toJson());
                        }
                        return jsonObject;
                    })
                    .setter((option, config_, configStorage, optionField, value) -> {
                        throw new UnsupportedOperationException();
                    })
                    .createWidget((option, config_, configStorage, optionField) -> new ButtonWidget(0, 0, 150, 20, new TranslatableText("seedqueue.menu.keys.configure"), button -> MinecraftClient.getInstance().openScreen(new SeedQueueKeybindingsScreen(MinecraftClient.getInstance().currentScreen, this.keyBindings))))
                    .build();
        }
        return SpeedrunConfig.super.parseField(field, config, idPrefix);
    }

    /**
     * Reloads the config from disk.
     */
    public void reload() throws IOException, JsonParseException {
        if (this.container != null) {
            this.container.load();
        }
    }

    @Override
    public void finishInitialization(SpeedrunConfigContainer<?> container) {
        this.container = container;
    }

    @Override
    public boolean shouldShowCategory(String category) {
        if (!this.showAdvancedSettings) {
            return !category.equals("threading") && !category.equals("experimental") && !category.equals("debug");
        }
        return true;
    }

    @Override
    public String modID() {
        return "seedqueue";
    }

    @Override
    public boolean isAvailable() {
        return !SeedQueue.isActive();
    }

    public enum ChunkMapVisibility {
        TRUE,
        TRANSPARENT,
        FALSE
    }

    public static class WindowSize {
        private int width;
        private int height;

        public int width() {
            if (this.width == 0) {
                this.width = MinecraftClient.getInstance().getWindow().getWidth();
            }
            return this.width;
        }

        public void setWidth(int width) {
            this.width = Math.max(0, Math.min(16384, width));
        }

        public int height() {
            if (this.height == 0) {
                this.height = MinecraftClient.getInstance().getWindow().getHeight();
            }
            return this.height;
        }

        public void setHeight(int height) {
            this.height = Math.max(0, Math.min(16384, height));
        }

        public void init() {
            this.width();
            this.height();
        }

        public void fromJson(JsonObject jsonObject) {
            this.setWidth(jsonObject.get("width").getAsInt());
            this.setHeight(jsonObject.get("height").getAsInt());
        }

        public JsonObject toJson() {
            JsonObject jsonObject = new JsonObject();
            jsonObject.add("width", new JsonPrimitive(this.width));
            jsonObject.add("height", new JsonPrimitive(this.height));
            return jsonObject;
        }
    }

    public static class CPUClockSpeed {

        /**
         * @return The clock speed of the local CPU in MHz, if it fails then it returns {@code -1}
         */
        public static double getCPUClockSpeed() {
            String os = System.getProperty("os.name").toLowerCase();

            if (os.contains("win")) {
                return getWindowsClockSpeed();
            } else if (os.contains("nix") || os.contains("nux")) {
                return getLinuxClockSpeed();
            } else if (os.contains("mac")) {
                return getMacClockSpeed();
            }

            return -1;
        }

        private static double getWindowsClockSpeed() {
            try {
                String command = "wmic cpu get MaxClockSpeed";
                Process process = Runtime.getRuntime().exec(command);
                BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));

                String line;
                while ((line = reader.readLine()) != null) {
                    if (line.trim().matches("\\d+")) {
                        try {
                            return Double.parseDouble(line) / 1000;
                        } catch (NumberFormatException e) {
                            return -1;
                        }
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
            return -1;
        }

        private static double getLinuxClockSpeed() {
            try {
                String[] command = {"/bin/sh", "-c", "cat /proc/cpuinfo | grep 'MHz' | awk '{print $4}' | head -n 1"};
                Process process = Runtime.getRuntime().exec(command);
                BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));

                String line;
                if ((line = reader.readLine()) != null) {
                    try {
                        return Double.parseDouble(line) / 1000;
                    } catch (NumberFormatException e) {
                        return -1;
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
            return -1;
        }

        private static double getMacClockSpeed() {
            try {
                String[] command = {"/bin/sh", "-c", "sysctl -n machdep.cpu.brand_string"};
                Process process = Runtime.getRuntime().exec(command);
                BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));

                String line;
                if ((line = reader.readLine()) != null) {
                    try {
                        return Double.parseDouble(line) / 1000;
                    } catch (NumberFormatException e) {
                        return -1;
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
            return -1;
        }
    }
}
