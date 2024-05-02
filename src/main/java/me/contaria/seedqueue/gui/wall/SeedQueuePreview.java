package me.contaria.seedqueue.gui.wall;

import me.contaria.seedqueue.SeedQueue;
import me.contaria.seedqueue.SeedQueueEntry;
import me.contaria.seedqueue.compat.WorldPreviewProperties;
import me.contaria.seedqueue.mixin.accessor.WorldRendererAccessor;
import me.voidxwalker.worldpreview.WorldPreview;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.Element;
import net.minecraft.client.gui.screen.LevelLoadingScreen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.render.WorldRenderer;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.text.Text;
import net.minecraft.text.TranslatableText;
import org.mcsr.speedrunapi.config.SpeedrunConfigAPI;

import java.util.Arrays;
import java.util.Objects;

public class SeedQueuePreview extends LevelLoadingScreen {

    public final SeedQueueWallScreen wallScreen;
    private final SeedQueueEntry seedQueueEntry;
    private final WorldPreviewProperties worldPreviewProperties;
    private WorldRenderer worldRenderer;

    protected SeedQueueWallScreen.LockTexture lock;

    protected int firstRenderFrame = Integer.MAX_VALUE;
    protected int lastRenderFrame;
    protected long firstRenderTime;

    public SeedQueuePreview(SeedQueueWallScreen wallScreen, SeedQueueEntry seedQueueEntry) {
        super(seedQueueEntry.getWorldGenerationProgressTracker());
        this.wallScreen = wallScreen;
        this.seedQueueEntry = seedQueueEntry;
        this.worldPreviewProperties = Objects.requireNonNull(seedQueueEntry.getWorldPreviewProperties());

        if (this.worldPreviewProperties.getSettingsCache() == null) {
            this.worldPreviewProperties.setSettingsCache(this.wallScreen.settingsCache);
        }

        this.initScreen();
    }

    private void initScreen() {
        try {
            WorldPreview.inPreview = true;

            MinecraftClient client = MinecraftClient.getInstance();
            if (SeedQueue.config.hasSimulatedWindowSize()) {
                // forceUnicodeFont is not being loaded from the settings cache because it is not included in SeedQueueSettingsCache.PREVIEW_SETTINGS
                int scale = SeedQueue.config.calculateSimulatedScaleFactor((int) this.worldPreviewProperties.getSettingsCache().getValue("guiScale"), client.options.forceUnicodeFont);
                this.init(client, SeedQueue.config.simulatedWindowWidth / scale, SeedQueue.config.simulatedWindowHeight / scale);
            } else {
                this.init(client, this.wallScreen.width, this.wallScreen.height);
            }

            if (Boolean.TRUE.equals(SpeedrunConfigAPI.getConfigValue("standardsettings", "autoF3Esc"))) {
                Text backToGame = new TranslatableText("menu.returnToGame");
                for (Element e : this.children()) {
                    if (!(e instanceof ButtonWidget)) {
                        continue;
                    }
                    ButtonWidget button = (ButtonWidget) e;
                    if (backToGame.equals(button.getMessage())) {
                        button.onPress();
                        break;
                    }
                }
            }
        } finally {
            WorldPreview.inPreview = false;
        }
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        return false;
    }

    @Override
    public void render(MatrixStack matrices, int mouseX, int mouseY, float delta) {
        assert this.client != null;

        this.runAsPreview(() -> super.render(matrices, mouseX, mouseY, delta));

        if (!this.hasBeenRendered()) {
            this.firstRenderFrame = this.wallScreen.frame;
        }
        this.lastRenderFrame = this.wallScreen.frame;
    }

    public void buildChunks() {
        this.runAsPreview(() -> WorldPreview.runAsPreview(() -> {
            WorldPreview.tickPackets();
            WorldPreview.tickEntities();
            this.worldPreviewProperties.buildChunks();
        }));
    }

    private void runAsPreview(Runnable runnable) {
        WorldRenderer worldPreviewRenderer = WorldPreview.worldRenderer;
        WorldPreview.worldRenderer = this.getWorldRenderer();
        this.worldPreviewProperties.apply();
        WorldPreview.inPreview = true;

        try {
            runnable.run();
        } finally {
            WorldPreview.worldRenderer = worldPreviewRenderer;
            WorldPreview.clear();
            WorldPreview.inPreview = false;
        }
    }

    public void printDebug() {
        WorldRenderer worldRenderer = this.getWorldRenderer();
        SeedQueue.LOGGER.info("SeedQueue-DEBUG | " + "Instance: " + this.seedQueueEntry.getSession().getDirectoryName() + ", Seed: " + this.seedQueueEntry.getServer().getOverworld().getSeed() + ", Chunks: " + worldRenderer.getChunksDebugString() + " (" + worldRenderer.isTerrainRenderComplete() + "), locked: " + this.seedQueueEntry.isLocked() + ", paused: " + this.seedQueueEntry.isPaused() + ", ready: " + this.seedQueueEntry.isReady());
    }

    public void printStacktrace() {
        SeedQueue.LOGGER.info("SeedQueue-DEBUG | " + "Instance: " + this.seedQueueEntry.getSession().getDirectoryName() + ", Stacktrace: " + Arrays.toString(this.seedQueueEntry.getServer().getThread().getStackTrace()));
    }

    public boolean shouldRender() {
        if (SeedQueue.config.doNotWaitForChunksToBuild) {
            return true;
        }
        if (this.hasBeenRendered()) {
            return true;
        }
        WorldRenderer worldRenderer = this.getWorldRenderer();
        if (((WorldRendererAccessor) worldRenderer).seedQueue$getCompletedChunkCount() == 0) {
            // this checks for instances that are ready to be loaded but do not have any chunks built, to avoid keeping them invisible forever we have to flush them through the system
            return this.seedQueueEntry.isPaused() && worldRenderer.isTerrainRenderComplete() && this.worldPreviewProperties.getPacketQueue().isEmpty();
        }
        return true;
    }

    public boolean hasBeenRendered() {
        return this.firstRenderFrame < this.wallScreen.frame;
    }

    public SeedQueueEntry getSeedQueueEntry() {
        return this.seedQueueEntry;
    }

    public WorldPreviewProperties getWorldPreviewProperties() {
        return this.worldPreviewProperties;
    }

    public WorldRenderer getWorldRenderer() {
        if (this.worldRenderer == null) {
            this.worldRenderer = SeedQueueWallScreen.getOrCreateWorldRenderer(this.worldPreviewProperties.getWorld());
        }
        return this.worldRenderer;
    }
}
