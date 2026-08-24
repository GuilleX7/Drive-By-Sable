package edn.lakeopossmc.drivebysable.client.screen;

import com.simibubi.create.AllSoundEvents;
import edn.lakeopossmc.drivebysable.CableItems;
import edn.lakeopossmc.drivebysable.client.CableKeyMappings;
import net.createmod.catnip.gui.element.GuiGameElement;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.item.ItemStack;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.function.Consumer;

// --- CHANNEL QUICK SELECT --- //
// * Channel selector opened by keybind
public class ChannelQuickSelectScreen extends Screen {

    private static final ResourceLocation TEXTURE =
            ResourceLocation.fromNamespaceAndPath("drivebysable", "textures/gui/channel_quickselect.png");

    //#region // --- SHEET LAYOUT --- //
    private static final int SHEET_SIZE = 256;
    private static final int PANEL_U = 32;
    private static final int PANEL_V = 0;
    private static final int PANEL_W = 200;
    private static final int PANEL_BORDER_W = 192;
    private static final int PANEL_H = 78;

    private static final int BTN_NORMAL_U = 154;
    private static final int BTN_HOVER_U = 172;
    private static final int BTN_PRESS_U = 190;
    private static final int BTN_V = 80;
    private static final int BTN_SIZE = 18;
    private static final int ICON_U = 208;
    private static final int ICON_V = 80;
    private static final int ICON_SIZE = 16;

    private static final int WELL_X = 23;
    private static final int WELL_Y = 24;
    private static final int WELL_W = 146;
    private static final int WELL_H = 18;
    private static final int BUTTON_X = 167;
    private static final int BUTTON_Y = 54;

    private static final int TEXT_INSET_X = 13;
    private static final int TEXT_INSET_Y = 5;
    private static final int TEXT_HEIGHT = 10;
    //#endregion

    //#region // --- SUGGESTION LIST STYLING --- //
    private static final int SUGGEST_BACKGROUND = 0xB4000000;
    private static final int SUGGEST_TEXT = 0xAAAAAA;
    private static final int SUGGEST_TEXT_SELECTED = 0xFFFF00;
    private static final int SUGGEST_MAX_ROWS = 5;
    private static final int SUGGEST_MARKER = 0xFFFFFFFF;
    private static final int SUGGEST_ROW_HEIGHT = 12;
    private static final int SUGGEST_PAD_X = 1;
    private static final int SUGGEST_PAD_Y = 2;
    //#endregion

    private static final int ITEM_GAP_X = 8;
    private static final int ITEM_RISE_Y = 42;
    private static final float ITEM_Z = -200.0F;
    private static final double ITEM_SCALE = 4.0;

    private static final int TITLE_TEXT_Y = 4;
    private static final int TITLE_COLOR = 0x3C3B47;

    private static final int TOOLTIP_HEADER = 0x5391E1;

    private static final int TEXT_COLOR = 0x545454;
    private static final int TEXT_COLOR_ERROR = 0xFF4444;

    private static final int FLASH_TICKS = 5;
    private static final int FLASH_WHITE_AT = 3;
    private static final int FLASH_COLOR = 0xFFFFFF;

    private final List<Channel> channels;
    private final String initialChannel;
    private final Consumer<String> onConfirm;

    private final List<Channel> suggestions = new ArrayList<>();
    private int highlighted = -1;
    private int offset;

    private EditBox well;
    private int left;
    private int top;

    private int flashTicks;
    private boolean buttonHeld;

    // * Channel id paired with the name the player actually sees
    public record Channel(String id, String displayName) {
    }

    public ChannelQuickSelectScreen(
            final List<Channel> channels,
            @Nullable final String initialChannel,
            final Consumer<String> onConfirm
    ) {
        super(Component.translatable("drivebysable.channel_select.title"));
        this.channels = List.copyOf(channels);
        this.initialChannel = initialChannel;
        this.onConfirm = onConfirm;
    }

    @Override
    protected void init() {
        left = (width - PANEL_W) / 2;
        top = (height - PANEL_H) / 2;

        well = new EditBox(
                font,
                left + WELL_X + TEXT_INSET_X,
                top + WELL_Y + TEXT_INSET_Y,
                WELL_W - TEXT_INSET_X * 2,
                TEXT_HEIGHT,
                Component.empty()
        );
        well.setBordered(false);
        well.setTextShadow(false);
        well.setMaxLength(64);
        well.setTextColor(TEXT_COLOR);
        well.setResponder(text -> {
            refilter(text);
            refreshWell();
        });

        addWidget(well);

        final Channel current = byId(initialChannel);
        if (current != null) {
            well.setValue(current.displayName());
            well.moveCursorToEnd(false);
        }
        refilter(well.getValue());
        refreshWell();
    }

    //#region // --- MATCHING --- //
    private void refilter(final String rawText) {
        suggestions.clear();

        final String text = rawText.trim().toLowerCase(Locale.ROOT);
        if (text.isEmpty()) {
            suggestions.addAll(channels);
        } else {
            final List<Channel> contains = new ArrayList<>();
            for (final Channel channel : channels) {
                final String name = channel.displayName().toLowerCase(Locale.ROOT);
                final String id = channel.id().toLowerCase(Locale.ROOT);

                if (name.startsWith(text) || id.startsWith(text)) {
                    suggestions.add(channel);
                } else if (name.contains(text) || id.contains(text)) {
                    contains.add(channel);
                }
            }
            suggestions.addAll(contains);
        }

        highlighted = suggestions.isEmpty() ? -1 : 0;
        offset = 0;
    }

    // * Exact name or id wins
    @Nullable
    private String resolveTyped() {
        final String text = well.getValue().trim();
        if (text.isEmpty()) {
            return null;
        }

        for (final Channel channel : channels) {
            if (channel.displayName().equalsIgnoreCase(text) || channel.id().equalsIgnoreCase(text)) {
                return channel.id();
            }
        }

        if (highlighted >= 0 && highlighted < suggestions.size()) {
            return suggestions.get(highlighted).id();
        }
        return null;
    }

    @Nullable
    private Channel byId(@Nullable final String id) {
        if (id == null) {
            return null;
        }
        for (final Channel channel : channels) {
            if (channel.id().equals(id)) {
                return channel;
            }
        }
        return null;
    }
    //#endregion

    //#region // --- CONFIRM --- //
    private void confirm() {
        final String resolved = resolveTyped();
        if (resolved == null) {
            flashTicks = FLASH_TICKS;
            return;
        }

        onConfirm.accept(resolved);
        onClose();
    }

    @Override
    public void tick() {
        super.tick();

        if (flashTicks > 0) {
            if (flashTicks == FLASH_WHITE_AT && minecraft != null && minecraft.player != null) {
                final BlockPos soundPos = minecraft.player.blockPosition();
                minecraft.player.level().playLocalSound(
                        soundPos.getX() + 0.5, soundPos.getY() + 0.5, soundPos.getZ() + 0.5,
                        AllSoundEvents.DENY.getMainEvent(), SoundSource.PLAYERS, 1.0F, 0.5F, false
                );
            }
            flashTicks--;
        }

        refreshWell();
    }

    private void refreshWell() {
        updateTextColor();
        updateInlineSuggestion();
    }

    private void updateInlineSuggestion() {
        if (!well.isFocused() || highlighted < 0 || highlighted >= suggestions.size()) {
            well.setSuggestion("");
            return;
        }

        final String typed = well.getValue();
        final String name = suggestions.get(highlighted).displayName();

        well.setSuggestion(name.toLowerCase(Locale.ROOT).startsWith(typed.toLowerCase(Locale.ROOT))
                ? name.substring(typed.length())
                : "");
    }

    private void updateTextColor() {
        if (flashTicks > 0) {
            well.setTextColor(flashTicks == FLASH_WHITE_AT ? FLASH_COLOR : TEXT_COLOR_ERROR);
            return;
        }
        well.setTextColor(matchesChannel() ? TEXT_COLOR : TEXT_COLOR_ERROR);
    }

    private boolean matchesChannel() {
        return well.getValue().trim().isEmpty() || resolveTyped() != null;
    }
    //#endregion

    //#region // --- INPUT --- //
    @Override
    public boolean keyPressed(final int keyCode, final int scanCode, final int modifiers) {
        // * The key that opened the screen closes it when not typing
        if (!well.isFocused()
                && !CableKeyMappings.CHANNEL_QUICK_SELECT.isUnbound()
                && CableKeyMappings.CHANNEL_QUICK_SELECT.matches(keyCode, scanCode)) {
            confirm();
            return true;
        }

        if (well.isFocused() && !suggestions.isEmpty()) {
            if (keyCode == 265) {
                cycleHighlight(-1);
                return true;
            }
            if (keyCode == 264) {
                cycleHighlight(1);
                return true;
            }
            if (keyCode == 258) {
                applyHighlightToWell();
                return true;
            }
        }

        if (keyCode == 257 || keyCode == 335) {
            confirm();
            return true;
        }

        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    private void cycleHighlight(final int direction) {
        final int size = suggestions.size();
        highlighted = highlighted < 0
                ? (direction > 0 ? 0 : size - 1)
                : Math.floorMod(highlighted + direction, size);
        scrollTo(highlighted);
        refreshWell();
    }

    private void scrollTo(final int index) {
        if (index < 0) {
            return;
        }
        if (index < offset) {
            offset = index;
        } else if (index >= offset + SUGGEST_MAX_ROWS) {
            offset = index - SUGGEST_MAX_ROWS + 1;
        }
        offset = Mth.clamp(offset, 0, maxOffset());
    }

    private int maxOffset() {
        return Math.max(0, suggestions.size() - SUGGEST_MAX_ROWS);
    }

    private void applyHighlightToWell() {
        if (highlighted < 0 || highlighted >= suggestions.size()) {
            return;
        }
        final Channel chosen = suggestions.get(highlighted);
        well.setValue(chosen.displayName());
        well.moveCursorToEnd(false);
    }

    @Override
    public void mouseMoved(final double mouseX, final double mouseY) {
        final int row = rowAt(mouseX, mouseY);
        if (row >= 0 && row != highlighted) {
            highlighted = row;
            refreshWell();
        }
        super.mouseMoved(mouseX, mouseY);
    }

    @Override
    public boolean mouseClicked(final double mouseX, final double mouseY, final int button) {
        if (button == 0) {
            if (overButton(mouseX, mouseY)) {
                buttonHeld = true;
                playClick();
                return true;
            }

            if (overWell(mouseX, mouseY) && !well.isFocused()) {
                setFocused(well);
                well.setFocused(true);
                selectAllInWell();
                return true;
            }

            final int row = rowAt(mouseX, mouseY);
            if (row >= 0) {
                highlighted = row;
                applyHighlightToWell();
                return true;
            }
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseReleased(final double mouseX, final double mouseY, final int button) {
        if (button == 0 && buttonHeld) {
            buttonHeld = false;
            if (overButton(mouseX, mouseY)) {
                confirm();
            }
            return true;
        }
        return super.mouseReleased(mouseX, mouseY, button);
    }

    private void selectAllInWell() {
        well.setCursorPosition(well.getValue().length());
        well.setHighlightPos(0);
    }

    private void playClick() {
        if (minecraft == null || minecraft.player == null) {
            return;
        }

        final BlockPos soundPos = minecraft.player.blockPosition();
        minecraft.player.level().playLocalSound(
                soundPos.getX() + 0.5,
                soundPos.getY() + 0.5,
                soundPos.getZ() + 0.5,
                SoundEvents.UI_BUTTON_CLICK.value(),
                SoundSource.MASTER,
                0.25F,
                1.0F,
                false
        );
    }

    private boolean overWell(final double mouseX, final double mouseY) {
        final int x = left + WELL_X;
        final int y = top + WELL_Y;
        return mouseX >= x && mouseX < x + WELL_W && mouseY >= y && mouseY < y + WELL_H;
    }

    private boolean overButton(final double mouseX, final double mouseY) {
        final int x = left + BUTTON_X;
        final int y = top + BUTTON_Y;
        return mouseX >= x && mouseX < x + BTN_SIZE && mouseY >= y && mouseY < y + BTN_SIZE;
    }

    private int rowAt(final double mouseX, final double mouseY) {
        final int rows = visibleRows();
        if (rows == 0 || !well.isFocused()) {
            return -1;
        }

        final int listX = listLeft();
        final int listY = listTop();
        if (mouseX < listX || mouseX >= listX + listWidth()) {
            return -1;
        }

        final int index = (int) ((mouseY - listY) / SUGGEST_ROW_HEIGHT);
        return index >= 0 && index < rows ? index + offset : -1;
    }
    //#endregion

    //#region // --- RENDER --- //
    private int visibleRows() {
        return Math.min(suggestions.size(), SUGGEST_MAX_ROWS);
    }

    private int listTop() {
        return top + WELL_Y + WELL_H;
    }

    private int listLeft() {
        return left + WELL_X + TEXT_INSET_X - SUGGEST_PAD_X;
    }

    @Override
    public void render(final GuiGraphics graphics, final int mouseX, final int mouseY, final float partialTick) {
        // * Blur, dim, and any registered renderable widgets. The well is not one of them
        super.render(graphics, mouseX, mouseY, partialTick);

        graphics.blit(TEXTURE, left, top, PANEL_U, PANEL_V, PANEL_W, PANEL_H, SHEET_SIZE, SHEET_SIZE);

        final boolean hovered = overButton(mouseX, mouseY);
        final int buttonU = buttonHeld && hovered ? BTN_PRESS_U : hovered ? BTN_HOVER_U : BTN_NORMAL_U;
        graphics.blit(TEXTURE, left + BUTTON_X, top + BUTTON_Y, buttonU, BTN_V, BTN_SIZE, BTN_SIZE, SHEET_SIZE, SHEET_SIZE);
        graphics.blit(
                TEXTURE,
                left + BUTTON_X + (BTN_SIZE - ICON_SIZE) / 2,
                top + BUTTON_Y + (BTN_SIZE - ICON_SIZE) / 2,
                ICON_U, ICON_V, ICON_SIZE, ICON_SIZE, SHEET_SIZE, SHEET_SIZE
        );

        graphics.drawString(
                font,
                title,
                left + (PANEL_BORDER_W - font.width(title)) / 2,
                top + TITLE_TEXT_Y,
                TITLE_COLOR,
                false
        );

        well.render(graphics, mouseX, mouseY, partialTick);

        renderFloatingCable(graphics);

        renderSuggestions(graphics);

        if (!well.isFocused() && overWell(mouseX, mouseY)) {
            graphics.renderComponentTooltip(font, wellTooltip(), mouseX, mouseY);
        }

        if (overButton(mouseX, mouseY)) {
            graphics.renderComponentTooltip(font, buttonTooltip(), mouseX, mouseY);
        }
    }

    private List<Component> buttonTooltip() {
        return resolveTyped() != null
                ? List.of(Component.translatable("drivebysable.channel_select.confirm")
                .withStyle(ChatFormatting.WHITE))
                : List.of(Component.translatable("drivebysable.channel_select.invalid")
                .withStyle(ChatFormatting.RED));
    }

    private void renderFloatingCable(final GuiGraphics graphics) {
        GuiGameElement.of(new ItemStack(CableItems.CABLE.get()))
                .<GuiGameElement.GuiRenderBuilder>at(
                        left + PANEL_BORDER_W + ITEM_GAP_X,
                        top + PANEL_H - ITEM_RISE_Y,
                        ITEM_Z
                )
                .scale(ITEM_SCALE)
                .render(graphics);
    }

    private List<Component> wellTooltip() {
        return List.of(
                Component.translatable("drivebysable.channel_select.well")
                        .withStyle(style -> style.withColor(TOOLTIP_HEADER)),
                Component.translatable("drivebysable.channel_select.well_tip")
                        .withStyle(ChatFormatting.GRAY),
                Component.translatable("drivebysable.channel_select.well_hint")
                        .withStyle(ChatFormatting.DARK_GRAY, ChatFormatting.ITALIC)
        );
    }


    // * Wide enough for the longest visible entry
    private int listWidth() {
        int widest = 0;
        // * Measured across every entry
        for (final Channel channel : suggestions) {
            widest = Math.max(widest, font.width(channel.displayName()));
        }
        return widest + SUGGEST_PAD_X * 2;
    }

    private void renderSuggestions(final GuiGraphics graphics) {
        final int rows = visibleRows();
        if (rows == 0 || !well.isFocused()) {
            return;
        }

        final int listX = listLeft();
        final int listY = listTop();

        final int listW = listWidth();
        final int listH = rows * SUGGEST_ROW_HEIGHT;

        graphics.fill(listX, listY, listX + listW, listY + listH, SUGGEST_BACKGROUND);

        for (int i = 0; i < rows; i++) {
            final int index = i + offset;
            graphics.drawString(
                    font,
                    suggestions.get(index).displayName(),
                    listX + SUGGEST_PAD_X,
                    listY + SUGGEST_PAD_Y + i * SUGGEST_ROW_HEIGHT,
                    index == highlighted ? SUGGEST_TEXT_SELECTED : SUGGEST_TEXT,
                    true
            );
        }

        renderScrollMarkers(graphics, listX, listY, listW, listH);
    }

    private void renderScrollMarkers(
            final GuiGraphics graphics,
            final int listX,
            final int listY,
            final int listW,
            final int listH
    ) {
        final boolean more = suggestions.size() > offset + visibleRows();
        if (offset <= 0 && !more) {
            return;
        }

        graphics.fill(listX, listY - 1, listX + listW, listY, SUGGEST_BACKGROUND);
        graphics.fill(listX, listY + listH, listX + listW, listY + listH + 1, SUGGEST_BACKGROUND);

        if (offset > 0) {
            dash(graphics, listX, listY - 1, listW);
        }
        if (more) {
            dash(graphics, listX, listY + listH, listW);
        }
    }

    private void dash(final GuiGraphics graphics, final int x, final int y, final int width) {
        for (int i = 0; i < width; i += 2) {
            graphics.fill(x + i, y, x + i + 1, y + 1, SUGGEST_MARKER);
        }
    }

    // * Wheel scrolls the window without moving the selection
    @Override
    public boolean mouseScrolled(final double mouseX, final double mouseY, final double scrollX, final double scrollY) {
        if (suggestions.size() > SUGGEST_MAX_ROWS) {
            offset = Mth.clamp((int) (offset - scrollY), 0, maxOffset());
            return true;
        }
        return super.mouseScrolled(mouseX, mouseY, scrollX, scrollY);
    }
    //#endregion

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}