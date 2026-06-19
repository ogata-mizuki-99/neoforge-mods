package com.ogatamizuki.guide.model;

import com.ogatamizuki.guide.GuideLibMod;
import com.ogatamizuki.guide.GuideSounds;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.world.entity.player.Player;

import java.util.List;

public record GuideTheme(
        Identifier id,
        int colorTitle,
        int colorSubtitle,
        int colorBody,
        int colorMuted,
        int colorError,
        int colorAccent,
        int colorLink,
        int colorLinkHover,
        int colorPanelBg,
        int colorInnerBg,
        int colorDimOverlay,
        int colorSlotInner,
        FrameStyle frameStyle,
        Identifier openSound,
        Identifier pageTurnSound
) {
    public static final Identifier BOOK_ID = Identifier.fromNamespaceAndPath(GuideLibMod.MODID, "book");
    public static final Identifier TABLET_ID = Identifier.fromNamespaceAndPath(GuideLibMod.MODID, "tablet");

    public enum FrameStyle {
        MINECRAFT_BEVEL,
        PAPER,
        TABLET
    }

    public static GuideTheme bookBuiltin() {
        return new GuideTheme(
                BOOK_ID,
                0xFFB8860B,
                0xFF7A5C44,
                0xFF3D2914,
                0xFF8B7355,
                0xFFB33A3A,
                0xFFC9A227,
                0xFF1E5A8E,
                0xFFB8860B,
                0xFF5C4033,
                0xFFF2E0C4,
                0xB0282018,
                0xFFE0CFA8,
                FrameStyle.PAPER,
                Identifier.fromNamespaceAndPath(GuideLibMod.MODID, "codex_open"),
                Identifier.fromNamespaceAndPath(GuideLibMod.MODID, "codex_page")
        );
    }

    public static GuideTheme tabletBuiltin() {
        return new GuideTheme(
                TABLET_ID,
                0xFF00E5FF,
                0xFF80CFFF,
                0xFFB0E0FF,
                0xFF6699BB,
                0xFFFF6666,
                0xFF00AAFF,
                0xFF00CCFF,
                0xFFFFFFFF,
                0xFF0A1628,
                0xFF050D18,
                0xC0081018,
                0xFF020810,
                FrameStyle.TABLET,
                Identifier.fromNamespaceAndPath(GuideLibMod.MODID, "tablet_open"),
                Identifier.fromNamespaceAndPath(GuideLibMod.MODID, "tablet_beep")
        );
    }

    public GuideTheme withId(Identifier themeId) {
        return new GuideTheme(
                themeId,
                colorTitle,
                colorSubtitle,
                colorBody,
                colorMuted,
                colorError,
                colorAccent,
                colorLink,
                colorLinkHover,
                colorPanelBg,
                colorInnerBg,
                colorDimOverlay,
                colorSlotInner,
                frameStyle,
                openSound,
                pageTurnSound
        );
    }

    /** 目次一覧はタブレット UI のみスクロール。本風 UI はページ送り。 */
    public boolean usesListScroll() {
        return frameStyle == FrameStyle.TABLET;
    }

    public static final class Builder {
        private Identifier id = BOOK_ID;
        private int colorTitle;
        private int colorSubtitle;
        private int colorBody;
        private int colorMuted;
        private int colorError;
        private int colorAccent;
        private int colorLink;
        private int colorLinkHover;
        private int colorPanelBg;
        private int colorInnerBg;
        private int colorDimOverlay;
        private int colorSlotInner;
        private FrameStyle frameStyle = FrameStyle.MINECRAFT_BEVEL;
        private Identifier openSound;
        private Identifier pageTurnSound;

        private Builder() {}

        public static Builder from(GuideTheme theme) {
            Builder builder = new Builder();
            builder.id = theme.id;
            builder.colorTitle = theme.colorTitle;
            builder.colorSubtitle = theme.colorSubtitle;
            builder.colorBody = theme.colorBody;
            builder.colorMuted = theme.colorMuted;
            builder.colorError = theme.colorError;
            builder.colorAccent = theme.colorAccent;
            builder.colorLink = theme.colorLink;
            builder.colorLinkHover = theme.colorLinkHover;
            builder.colorPanelBg = theme.colorPanelBg;
            builder.colorInnerBg = theme.colorInnerBg;
            builder.colorDimOverlay = theme.colorDimOverlay;
            builder.colorSlotInner = theme.colorSlotInner;
            builder.frameStyle = theme.frameStyle;
            builder.openSound = theme.openSound;
            builder.pageTurnSound = theme.pageTurnSound;
            return builder;
        }

        public Builder id(Identifier themeId) {
            this.id = themeId;
            return this;
        }

        public Builder colorTitle(int value) { this.colorTitle = value; return this; }
        public Builder colorSubtitle(int value) { this.colorSubtitle = value; return this; }
        public Builder colorBody(int value) { this.colorBody = value; return this; }
        public Builder colorMuted(int value) { this.colorMuted = value; return this; }
        public Builder colorError(int value) { this.colorError = value; return this; }
        public Builder colorAccent(int value) { this.colorAccent = value; return this; }
        public Builder colorLink(int value) { this.colorLink = value; return this; }
        public Builder colorLinkHover(int value) { this.colorLinkHover = value; return this; }
        public Builder colorPanelBg(int value) { this.colorPanelBg = value; return this; }
        public Builder colorInnerBg(int value) { this.colorInnerBg = value; return this; }
        public Builder colorDimOverlay(int value) { this.colorDimOverlay = value; return this; }
        public Builder colorSlotInner(int value) { this.colorSlotInner = value; return this; }
        public Builder frameStyle(FrameStyle value) { this.frameStyle = value; return this; }
        public Builder openSound(Identifier value) { this.openSound = value; return this; }
        public Builder pageTurnSound(Identifier value) { this.pageTurnSound = value; return this; }

        public GuideTheme build() {
            return new GuideTheme(
                    id,
                    colorTitle,
                    colorSubtitle,
                    colorBody,
                    colorMuted,
                    colorError,
                    colorAccent,
                    colorLink,
                    colorLinkHover,
                    colorPanelBg,
                    colorInnerBg,
                    colorDimOverlay,
                    colorSlotInner,
                    frameStyle,
                    openSound,
                    pageTurnSound
            );
        }
    }

    public void playOpenSound(Player player) {
        playSound(player, openSound);
    }

    public void playPageTurnSound(Player player) {
        playSound(player, pageTurnSound);
    }

    private static void playSound(Player player, Identifier soundId) {
        if (player == null || soundId == null) {
            return;
        }
        SoundEvent soundEvent = GuideSounds.resolve(soundId);
        if (soundEvent != null) {
            player.playSound(soundEvent, 0.8F, 1.0F);
        }
    }

    public void drawBackgroundDim(GuiGraphicsExtractor gui, int screenWidth, int screenHeight) {
        gui.fill(0, 0, screenWidth, screenHeight, colorDimOverlay);
    }

    public void drawOuterPanel(GuiGraphicsExtractor gui, int x1, int y1, int x2, int y2) {
        if (frameStyle == FrameStyle.TABLET) {
            drawTabletFrame(gui, x1, y1, x2, y2, colorPanelBg, colorAccent, false);
            return;
        }
        if (frameStyle == FrameStyle.PAPER) {
            drawPaperCover(gui, x1, y1, x2, y2);
            return;
        }
        drawBevel(gui, x1, y1, x2, y2, false, colorPanelBg);
        gui.fill(x1 + 1, y1 + 1, x2 - 1, y2 - 1, colorPanelBg);
        gui.fill(x1 + 1, y1 + 1, x2 - 1, y1 + 2, colorAccent);
    }

    public void drawInnerPanel(GuiGraphicsExtractor gui, int x1, int y1, int x2, int y2) {
        if (frameStyle == FrameStyle.TABLET) {
            drawTabletFrame(gui, x1, y1, x2, y2, colorInnerBg, colorAccent, true);
            return;
        }
        if (frameStyle == FrameStyle.PAPER) {
            drawPaperPage(gui, x1, y1, x2, y2);
            return;
        }
        drawBevel(gui, x1, y1, x2, y2, true, colorInnerBg);
    }

    public void drawTitle(Font font, GuiGraphicsExtractor gui, Component title, int centerX, int y) {
        gui.centeredText(font, title, centerX, y, colorTitle);
    }

    public void drawBodyLines(Font font, GuiGraphicsExtractor gui, List<FormattedCharSequence> lines, int left, int top) {
        int lineY = top;
        for (FormattedCharSequence line : lines) {
            gui.text(font, line, left, lineY, colorBody, false);
            lineY += font.lineHeight + 2;
        }
    }

    public void drawSubtitleLines(Font font, GuiGraphicsExtractor gui, List<FormattedCharSequence> lines, int left, int top) {
        int lineY = top;
        for (FormattedCharSequence line : lines) {
            gui.text(font, line, left, lineY, colorSubtitle, false);
            lineY += font.lineHeight + 2;
        }
    }

    public void drawItemSlot(GuiGraphicsExtractor gui, int slotX, int slotY) {
        if (frameStyle == FrameStyle.TABLET) {
            gui.fill(slotX, slotY, slotX + 18, slotY + 18, colorSlotInner);
            int border = colorAccent;
            gui.fill(slotX, slotY, slotX + 18, slotY + 1, border);
            gui.fill(slotX, slotY, slotX + 1, slotY + 18, border);
            gui.fill(slotX, slotY + 17, slotX + 18, slotY + 18, border);
            gui.fill(slotX + 17, slotY, slotX + 18, slotY + 18, border);
            return;
        }
        if (frameStyle == FrameStyle.PAPER) {
            drawPaperSlot(gui, slotX, slotY);
            return;
        }
        drawBevel(gui, slotX, slotY, slotX + 18, slotY + 18, true, colorInnerBg);
        gui.fill(slotX + 1, slotY + 1, slotX + 17, slotY + 17, colorSlotInner);
    }

    public void drawItemStack(GuiGraphicsExtractor gui, net.minecraft.world.item.ItemStack stack, int slotX, int slotY) {
        drawItemSlot(gui, slotX, slotY);
        if (!stack.isEmpty()) {
            gui.fakeItem(stack, slotX + 1, slotY + 1);
        }
    }

    private static void drawPaperCover(GuiGraphicsExtractor gui, int x1, int y1, int x2, int y2) {
        gui.fill(x1, y1, x2, y2, 0xFF3D2914);
        gui.fill(x1 + 1, y1 + 1, x2 - 1, y2 - 1, 0xFF5C4033);
        gui.fill(x1 + 1, y1 + 1, x2 - 1, y1 + 2, 0xFF8B6914);
        gui.fill(x1 + 1, y1 + 1, x1 + 2, y2 - 1, 0xFF8B6914);
        gui.fill(x1 + 1, y2 - 2, x2 - 1, y2 - 1, 0xFF2A1A10);
        gui.fill(x2 - 2, y1 + 1, x2 - 1, y2 - 1, 0xFF2A1A10);
    }

    private void drawPaperPage(GuiGraphicsExtractor gui, int x1, int y1, int x2, int y2) {
        gui.fill(x1, y1, x2, y2, colorInnerBg);
        gui.fill(x1, y1, x2, y1 + 1, 0x50C0A878);
        gui.fill(x1, y1, x1 + 1, y2, 0x50C0A878);
        gui.fill(x1, y2 - 1, x2, y2, 0x35FFFFFF);
        gui.fill(x2 - 1, y1, x2, y2, 0x35FFFFFF);

        int lineY = y1 + 14;
        int lineEnd = x2 - 6;
        int lineStart = x1 + 6;
        while (lineY < y2 - 8) {
            gui.fill(lineStart, lineY, lineEnd, lineY + 1, 0x18A08060);
            lineY += 11;
        }
    }

    private void drawPaperSlot(GuiGraphicsExtractor gui, int slotX, int slotY) {
        gui.fill(slotX, slotY, slotX + 18, slotY + 18, colorSlotInner);
        gui.fill(slotX, slotY, slotX + 18, slotY + 1, 0x70C0A878);
        gui.fill(slotX, slotY, slotX + 1, slotY + 18, 0x70C0A878);
        gui.fill(slotX, slotY + 17, slotX + 18, slotY + 18, 0x40FFFFFF);
        gui.fill(slotX + 17, slotY, slotX + 18, slotY + 18, 0x40FFFFFF);
    }

    private static void drawTabletFrame(
            GuiGraphicsExtractor gui,
            int x1,
            int y1,
            int x2,
            int y2,
            int bgColor,
            int accentColor,
            boolean inset
    ) {
        gui.fill(x1, y1, x2, y2, bgColor);
        int line = inset ? 0x6000E5FF : accentColor;
        gui.fill(x1, y1, x2, y1 + 1, line);
        gui.fill(x1, y1, x1 + 1, y2, line);
        gui.fill(x1, y2 - 1, x2, y2, line);
        gui.fill(x2 - 1, y1, x2, y2, line);
        if (!inset) {
            gui.fill(x1 + 1, y1 + 1, x2 - 1, y1 + 2, accentColor);
        }
    }

    private static void drawBevel(GuiGraphicsExtractor gui, int x1, int y1, int x2, int y2, boolean sunken, int bgColor) {
        int topLeftColor = sunken ? 0xFF1F1F1F : 0xFF5F5F5F;
        int bottomRightColor = sunken ? 0xFF4A4A4A : 0xFF1F1F1F;
        gui.fill(x1, y1, x2, y2, bgColor);
        gui.fill(x1, y1, x2, y1 + 1, topLeftColor);
        gui.fill(x1, y1, x1 + 1, y2, topLeftColor);
        gui.fill(x1, y2 - 1, x2, y2, bottomRightColor);
        gui.fill(x2 - 1, y1, x2, y2, bottomRightColor);
    }
}
