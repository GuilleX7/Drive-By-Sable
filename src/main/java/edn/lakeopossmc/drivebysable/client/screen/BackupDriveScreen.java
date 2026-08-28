package edn.lakeopossmc.drivebysable.client.screen;

import com.simibubi.create.AllSoundEvents;
import com.simibubi.create.foundation.gui.AllGuiTextures;
import com.simibubi.create.foundation.gui.menu.AbstractSimiContainerScreen;
import com.simibubi.create.foundation.gui.widget.Label;
import com.simibubi.create.foundation.gui.widget.ScrollInput;
import com.simibubi.create.foundation.gui.widget.SelectionScrollInput;
import edn.lakeopossmc.drivebysable.CableBlocks;
import edn.lakeopossmc.drivebysable.client.BackupDrivePreview;
import edn.lakeopossmc.drivebysable.DriveBySableMod;
import edn.lakeopossmc.drivebysable.menu.BackupDriveMenu;
import net.createmod.catnip.gui.element.GuiGameElement;
import edn.lakeopossmc.drivebysable.network.BackupDriveLoadPacket;
import edn.lakeopossmc.drivebysable.network.BackupDriveRegionPacket;
import edn.lakeopossmc.drivebysable.network.BackupDriveResetPacket;
import edn.lakeopossmc.drivebysable.network.BackupDriveSavePacket;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.core.BlockPos;
import net.minecraft.client.renderer.Rect2i;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import org.lwjgl.glfw.GLFW;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.network.PacketDistributor;

import java.util.List;

// --- BACKUP DRIVE SCREEN --- //
public class BackupDriveScreen extends AbstractSimiContainerScreen<BackupDriveMenu> {

    private static final ResourceLocation BACKGROUND_SURVIVAL =
            ResourceLocation.fromNamespaceAndPath(DriveBySableMod.MOD_ID, "textures/gui/backup_drive.png");
    private static final ResourceLocation BACKGROUND_CREATIVE =
            ResourceLocation.fromNamespaceAndPath(DriveBySableMod.MOD_ID, "textures/gui/backup_drive_creative.png");

    private static final int SPRITE_WIDTH = 218;
    private static final int SPRITE_HEIGHT = 122;
    private static final int SHEET_SIZE = 256;

    private static final int WINDOW_WIDTH = SPRITE_WIDTH;
    private static final int INVENTORY_WIDTH = 176;
    private static final int INVENTORY_X = (WINDOW_WIDTH - INVENTORY_WIDTH) / 2;
    private static final int INVENTORY_TOP = SPRITE_HEIGHT + 2;
    private static final int WINDOW_HEIGHT = INVENTORY_TOP + 108;

    private static final int TITLE_Y = 4;

    //#region // --- WELLS --- //
    private static final int WELL_WIDTH = 38;
    private static final int WELL_HEIGHT = 18;
    private static final int VALUE_X = 35;
    private static final int VALUE_PITCH = 40;
    private static final int SIZE_ROW_Y = 22;
    private static final int OFFSET_ROW_Y = 44;

    private static final int ROTATION_BOX_X = 35;
    private static final int ROTATION_BOX_Y = 66;
    private static final int ROTATION_BOX_WIDTH = 118;

    private static final int STATUS_WELL_X = 31;
    private static final int STATUS_WELL_Y = 98;
    private static final int STATUS_WELL_WIDTH = 73;

    private static final int TEXT_INSET_X = 4;
    private static final int TEXT_INSET_Y = (WELL_HEIGHT - 8) / 2;
    //#endregion

    //#region // --- BUTTONS --- //
    private static final int BUTTON_SIZE = 18;
    private static final int BUTTON_ROW_Y = 98;
    private static final int RESET_X = 8;
    private static final int ALIGN_X = 109;
    private static final int PREVIEW_X = 132;
    private static final int SAVE_X = 162;
    private static final int LOAD_X = 185;

    private static final int INDICATOR_Y = 92;

    //#region // --- BUTTON SPRITES --- //
    private static final int BUTTON_STATE_Y = 128;
    private static final int STATE_DEFAULT_U = 0;
    private static final int STATE_HOVER_U = 18;
    private static final int STATE_PRESS_U = 36;
    private static final int STATE_TOGGLE_ON_U = 72;
    private static final int STATE_LOCKED_U = 90;

    private static final int ICON_Y = 160;
    private static final int ICON_SIZE = 16;
    private static final int ICON_RESET_U = 0;
    private static final int ICON_ALIGN_U = 16;
    private static final int ICON_PREVIEW_U = 32;
    private static final int ICON_SAVE_U = 48;
    private static final int ICON_LOAD_U = 64;

    private static final int PRESS_TICKS = 4;
    //#endregion
    private static final int INDICATOR_HEIGHT = 6;
    //#endregion

    //#region // --- CABLE SLOT --- //
    private static final int CABLE_WELL_X = 177;
    private static final int CABLE_WELL_Y = 60;
    //#endregion

    //#region // --- BLOCK RENDER --- //
    private static final int POINTER_TIP_Y = 107;
    private static final int RENDER_WIDTH = 72;
    private static final int RENDER_HEIGHT = 74;
    private static final int RENDER_OFFSET_X = 4;
    private static final int RENDER_OFFSET_Y = 3;

    private static final int BLOCK_ICON_X = SPRITE_WIDTH + 2;
    private static final int BLOCK_ICON_Y = POINTER_TIP_Y - RENDER_HEIGHT / 2 - RENDER_OFFSET_Y;
    private static final float BLOCK_ICON_Z = -200.0F;
    private static final double BLOCK_ICON_SCALE = 5.0D;
    //#endregion

    private static final int AXIS_COUNT = 3;
    private static final String[] AXIS_NAMES = {"X", "Y", "Z"};
    private static final int OFFSET_MIN = -512;
    private static final int OFFSET_MAX = 512;
    private static final int SIZE_MIN = 1;
    private static final int SIZE_MAX = 512;

    private static final int VALUE_LABEL_COLOR = 0xFFFFFF;

    private static final int TITLE_COLOR = 0x592424;
    private static final int TITLE_COLOR_CREATIVE = 0x54214F;

    private final ScrollInput[] sizeInputs = new ScrollInput[AXIS_COUNT];
    private final ScrollInput[] offsetInputs = new ScrollInput[AXIS_COUNT];
    // * Vanilla text fields
    private final EditBox[] sizeBoxes = new EditBox[AXIS_COUNT];
    private final EditBox[] offsetBoxes = new EditBox[AXIS_COUNT];
    private ScrollInput rotationInput;
    private Label rotationLabel;

    private final int[] size = {1, 1, 1};
    private final int[] offset = new int[AXIS_COUNT];
    private int rotationIndex;
    private boolean regionLoaded;

    // * Raised by any value change
    private boolean regionDirty;

    //#region // --- WELL EDITING STATE --- //
    private static final int NO_WELL = -1;
    private static final int SIZE_WELL_BASE = 0;
    private static final int OFFSET_WELL_BASE = AXIS_COUNT;
    private static final int WELL_COUNT = AXIS_COUNT * 2;
    private static final int MAX_TYPED = 5;
    private static final int TEXT_COLOR = 0xFFFFFF;
    private static final int OUT_OF_RANGE_COLOR = 0xFF4444;

    // * The field currently taking keystrokes
    private EditBox activeBox;

    //#region // --- LOCK AND STATUS --- //
    // * True once the drive holds a snapshot
    private boolean locked;

    private static final int STATUS_IDLE_COLOR = 0xB0B0B0;
    private static final int STATUS_READY_COLOR = 0x55FF55;
    private static final int LOCKED_TEXT_COLOR = 0xAAAAAA;
    private static final int CURSOR_BLINK_TICKS = 6;
    private static final String CURSOR = "_";
    private int statusTicks;

    // * Which button is showing its pressed frame
    private int pressedButtonX = -1;
    private int pressTicks;

    // * Counts down after a reset
    private static final int ERASED_MESSAGE_TICKS = 10;
    private static final int STATUS_ERASED_COLOR = 0xFF4444;

    private static final int STATUS_WAITING_COLOR = 0xFFC24A;
    private int erasedTicks;

    //#region // --- LOAD STATE --- //
    private static final int BLUE_CENTRE_X = 185;
    private static final int BLUE_TOP_Y = 19;

    // * Mirrors the Invalid Operation flash
    private static final int COST_FLASH_TICKS = 5;
    private static final int COST_FLASH_WHITE_AT = 3;
    private static final int COST_READY_COLOR = 0x55FF55;
    private static final int COST_SHORT_COLOR = 0xFF4444;
    private static final int COST_TEXT_COLOR = 0xD2DBFA;

    private static final int COST_TEXT_COLOR_CREATIVE = 0xB985CC;

    private static final int COST_FLASH_COLOR = 0xFFFFFF;

    // * Raised once the player has asked to load
    private boolean loadSent;

    // * Raised by a first press on reset
    private boolean resetArmed;
    private int costFlashTicks;
    //#endregion
    //#endregion

    //#endregion
    private SaveState saveState = SaveState.IDLE;

    //#region // --- SAVE BUTTON STATE --- //
    private enum SaveState {
        IDLE,
        SAVED
    }
    //#endregion

    private final ItemStack driveIcon = new ItemStack(CableBlocks.BACKUP_DRIVE.get());

    // * Handed to JEI
    private List<Rect2i> extraAreas = List.of();

    public BackupDriveScreen(final BackupDriveMenu menu, final Inventory inventory, final Component title) {
        super(menu, inventory, title);
    }

    @Override
    protected void init() {

        // * Seeded once from the drive
        if (!regionLoaded) {
            final BlockPos storedOffset = menu.getRegionOffset();
            final BlockPos storedSize = menu.getRegionSize();


            offset[0] = Mth.clamp(storedOffset.getX(), OFFSET_MIN, OFFSET_MAX);
            offset[1] = Mth.clamp(storedOffset.getY(), OFFSET_MIN, OFFSET_MAX);
            offset[2] = Mth.clamp(storedOffset.getZ(), OFFSET_MIN, OFFSET_MAX);
            // * Clamped, since a drive saved before sizes were bounded, or one whose
            // * tag is missing, could report a value the inputs cannot represent
            size[0] = Mth.clamp(storedSize.getX(), SIZE_MIN, SIZE_MAX);
            size[1] = Mth.clamp(storedSize.getY(), SIZE_MIN, SIZE_MAX);
            size[2] = Mth.clamp(storedSize.getZ(), SIZE_MIN, SIZE_MAX);
            rotationIndex = menu.getRegionRotation();
            locked = menu.isSaved();
            saveState = locked ? SaveState.SAVED : SaveState.IDLE;
            regionLoaded = true;
        }

        initWidgets();

        regionDirty = false;
    }

    // * Hand the region back to the drive on the way out
    @Override
    public void removed() {
        resetArmed = false;

        blurActiveBox();

        // * Anything still unsent goes now
        flushRegion();
        super.removed();
    }

    // * Called every tick while the screen is open
    @Override
    protected void containerTick() {
        super.containerTick();
        flushRegion();
        statusTicks++;
        if (pressTicks > 0) {
            pressTicks--;
        }
        if (erasedTicks > 0) {
            erasedTicks--;
        }
        if (costFlashTicks > 0) {
            if (costFlashTicks == COST_FLASH_WHITE_AT && minecraft != null && minecraft.player != null) {
                final BlockPos soundPos = minecraft.player.blockPosition();
                minecraft.player.level().playLocalSound(
                        soundPos.getX() + 0.5, soundPos.getY() + 0.5, soundPos.getZ() + 0.5,
                        AllSoundEvents.DENY.getMainEvent(), SoundSource.PLAYERS, 1.0F, 0.5F, false
                );
            }
            costFlashTicks--;
        }

        if (locked != menu.isSaved()) {
            locked = menu.isSaved();
            saveState = locked ? SaveState.SAVED : SaveState.IDLE;

            if (!locked) {
                adoptDefaults();
            }
        }

        final int valueColor = locked ? LOCKED_TEXT_COLOR : TEXT_COLOR;
        for (int axis = 0; axis < AXIS_COUNT; axis++) {
            sizeBoxes[axis].setTextColor(valueColor);
            offsetBoxes[axis].setTextColor(valueColor);
        }
        if (rotationLabel != null) {
            rotationLabel.colored(valueColor);
        }


    }

    private void flushRegion() {
        if (!regionDirty) {
            return;
        }

        regionDirty = false;
        PacketDistributor.sendToServer(
                BackupDriveRegionPacket.of(menu.getDrivePos(), offset, size, rotationIndex)
        );

        // * Keep a visible preview honest
        if (BackupDrivePreview.isActive(menu.getDrivePos()) && minecraft != null && minecraft.level != null) {
            BackupDrivePreview.show(
                    minecraft.level,
                    menu.getDrivePos(),
                    offset,
                    size,
                    rotationIndex
            );
        }
    }

    //#region // --- CLICK HANDLING --- //
    @Override
    public boolean mouseClicked(final double mouseX, final double mouseY, final int button) {
        //#region // --- WELL EDITING --- //
        final EditBox clicked = locked ? null : boxAt(mouseX, mouseY);
        if (clicked != null) {
            focusBox(clicked, mouseX, mouseY);
            return true;
        }

        blurActiveBox();

        if (isOverWell(mouseX, mouseY, ROTATION_BOX_X, ROTATION_BOX_Y, ROTATION_BOX_WIDTH)) {
            return true;
        }
        //#endregion


        if (isOverButton(mouseX, mouseY, PREVIEW_X, BUTTON_ROW_Y)) {
            pressButton(PREVIEW_X, false);
            togglePreview();
            return true;
        }

        if (isOverButton(mouseX, mouseY, SAVE_X, BUTTON_ROW_Y)) {
            if (!locked) {
                pressButton(SAVE_X);
                clickSave();
            }
            return true;
        }

        if (isOverButton(mouseX, mouseY, RESET_X, BUTTON_ROW_Y)) {
            pressButton(RESET_X);
            clickReset();
            return true;
        }

        if (isOverButton(mouseX, mouseY, ALIGN_X, BUTTON_ROW_Y)) {
            if (!locked) {
                pressButton(ALIGN_X);
                clickAlign();
            }
            return true;
        }

        if (isOverButton(mouseX, mouseY, LOAD_X, BUTTON_ROW_Y)) {
            // * Silent when there is nothing to load
            if (menu.isSaved()) {
                pressButton(LOAD_X);
                clickLoad();
            }
            return true;
        }

        return super.mouseClicked(mouseX, mouseY, button);
    }


    //#region // --- WELL EDITING --- //
    private EditBox boxAt(final double mouseX, final double mouseY) {
        for (int axis = 0; axis < AXIS_COUNT; axis++) {
            final int wellX = VALUE_X + axis * VALUE_PITCH;
            if (isOverWell(mouseX, mouseY, wellX, SIZE_ROW_Y, WELL_WIDTH)) {
                return sizeBoxes[axis];
            }
            if (isOverWell(mouseX, mouseY, wellX, OFFSET_ROW_Y, WELL_WIDTH)) {
                return offsetBoxes[axis];
            }
        }
        return null;
    }

    private boolean isOverWell(final double mouseX, final double mouseY, final int wellX, final int wellY, final int width) {
        final int x = leftPos + wellX;
        final int y = topPos + wellY;
        return mouseX >= x && mouseX < x + width && mouseY >= y && mouseY < y + WELL_HEIGHT;
    }

    private void focusBox(final EditBox box, final double mouseX, final double mouseY) {
        blurActiveBox();
        activeBox = box;
        box.setFocused(true);

        box.mouseClicked(mouseX, mouseY, 0);
    }

    private void blurActiveBox() {
        if (activeBox == null) {
            return;
        }

        final EditBox box = activeBox;
        activeBox = null;
        box.setFocused(false);

        final int well = wellOf(box);
        final Integer parsed = parse(box.getValue());
        final int settled = parsed == null ? valueOf(well) : Mth.clamp(parsed, lowOf(well), highOf(well));

        inputOf(well).setState(settled);
        inputOf(well).onChanged();
        box.setValue(String.valueOf(settled));
        box.setTextColor(TEXT_COLOR);
    }

    // * Red while the number would not be accepted
    private void refreshBoxColor(final EditBox box) {
        final int well = wellOf(box);
        final Integer parsed = parse(box.getValue());
        final boolean bad = parsed != null && (parsed < lowOf(well) || parsed > highOf(well));
        box.setTextColor(bad ? OUT_OF_RANGE_COLOR : TEXT_COLOR);
    }

    private static Integer parse(final String text) {
        if (text.isEmpty() || "-".equals(text)) {
            return null;
        }
        try {
            return Integer.valueOf(text);
        } catch (final NumberFormatException notANumber) {
            return null;
        }
    }

    private int lowOf(final int well) {
        return well < OFFSET_WELL_BASE ? SIZE_MIN : OFFSET_MIN;
    }

    private int highOf(final int well) {
        return well < OFFSET_WELL_BASE ? SIZE_MAX : OFFSET_MAX;
    }

    private int wellOf(final EditBox box) {
        for (int axis = 0; axis < AXIS_COUNT; axis++) {
            if (sizeBoxes[axis] == box) {
                return SIZE_WELL_BASE + axis;
            }
            if (offsetBoxes[axis] == box) {
                return OFFSET_WELL_BASE + axis;
            }
        }
        return SIZE_WELL_BASE;
    }

    private ScrollInput inputOf(final int well) {
        return well < OFFSET_WELL_BASE ? sizeInputs[well] : offsetInputs[well - OFFSET_WELL_BASE];
    }

    private EditBox boxOf(final int well) {
        return well < OFFSET_WELL_BASE ? sizeBoxes[well] : offsetBoxes[well - OFFSET_WELL_BASE];
    }

    private int valueOf(final int well) {
        return well < OFFSET_WELL_BASE ? size[well] : offset[well - OFFSET_WELL_BASE];
    }
    //#endregion

    // * Scrolling ends any edit in progress
    @Override
    public boolean mouseScrolled(final double mouseX, final double mouseY, final double scrollX, final double scrollY) {
        if (locked) {
            return true;
        }

        blurActiveBox();
        return super.mouseScrolled(mouseX, mouseY, scrollX, scrollY);
    }

    //#region // --- KEYBOARD, FORWARDED TO THE ACTIVE FIELD --- //
    @Override
    public boolean charTyped(final char character, final int modifiers) {
        if (activeBox == null) {
            return super.charTyped(character, modifiers);
        }

        activeBox.charTyped(character, modifiers);
        refreshBoxColor(activeBox);
        return true;
    }

    @Override
    public boolean keyPressed(final int keyCode, final int scanCode, final int modifiers) {
        if (activeBox == null) {
            return super.keyPressed(keyCode, scanCode, modifiers);
        }

        switch (keyCode) {
            case GLFW.GLFW_KEY_ENTER, GLFW.GLFW_KEY_KP_ENTER, GLFW.GLFW_KEY_TAB, GLFW.GLFW_KEY_ESCAPE -> blurActiveBox();
            default -> {
                activeBox.keyPressed(keyCode, scanCode, modifiers);
                refreshBoxColor(activeBox);
            }
        }
        return true;
    }
    //#endregion


    //#region // --- BUTTON DRAWING --- //
    private void drawButton(
            final GuiGraphics graphics,
            final int x,
            final int y,
            final int buttonX,
            final int iconU,
            final int mouseX,
            final int mouseY,
            final boolean disabled,
            final boolean toggledOn
    ) {
        graphics.blit(
                background(),
                x + buttonX,
                y + BUTTON_ROW_Y,
                stateU(buttonX, mouseX, mouseY, disabled, toggledOn),
                BUTTON_STATE_Y,
                BUTTON_SIZE,
                BUTTON_SIZE,
                SHEET_SIZE,
                SHEET_SIZE
        );

        graphics.blit(
                background(),
                x + buttonX + (BUTTON_SIZE - ICON_SIZE) / 2,
                y + BUTTON_ROW_Y + (BUTTON_SIZE - ICON_SIZE) / 2,
                iconU,
                ICON_Y,
                ICON_SIZE,
                ICON_SIZE,
                SHEET_SIZE,
                SHEET_SIZE
        );
    }

    private int stateU(
            final int buttonX,
            final int mouseX,
            final int mouseY,
            final boolean disabled,
            final boolean toggledOn
    ) {
        if (disabled) {
            return STATE_LOCKED_U;
        }
        if (pressTicks > 0 && pressedButtonX == buttonX) {
            return STATE_PRESS_U;
        }
        if (isOverButton(mouseX, mouseY, buttonX, BUTTON_ROW_Y)) {
            return STATE_HOVER_U;
        }
        return toggledOn ? STATE_TOGGLE_ON_U : STATE_DEFAULT_U;
    }

    // * Every button click comes through here
    private void pressButton(final int buttonX) {
        pressButton(buttonX, true);
    }

    private void pressButton(final int buttonX, final boolean playClick) {
        pressedButtonX = buttonX;
        pressTicks = PRESS_TICKS;

        if (!playClick) {
            return;
        }

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
    //#endregion

    private int restingCostColor() {
        return background() == BACKGROUND_CREATIVE ? COST_TEXT_COLOR_CREATIVE : COST_TEXT_COLOR;
    }

    private ResourceLocation background() {
        return minecraft != null && minecraft.player != null && minecraft.player.hasInfiniteMaterials()
                ? BACKGROUND_CREATIVE
                : BACKGROUND_SURVIVAL;
    }

    private boolean isOverButton(final double mouseX, final double mouseY, final int buttonX, final int buttonY) {
        final int x = leftPos + buttonX;
        final int y = topPos + buttonY;
        return mouseX >= x && mouseX < x + BUTTON_SIZE && mouseY >= y && mouseY < y + BUTTON_SIZE;
    }

    private void clickSave() {
        PacketDistributor.sendToServer(
                BackupDriveSavePacket.of(menu.getDrivePos(), offset, size, rotationIndex)
        );
        saveState = SaveState.SAVED;

        BackupDrivePreview.clear(menu.getDrivePos());
    }

    // * Clears the drive and unlocks the screen
    private void clickAlign() {
        blurActiveBox();

        for (int axis = 0; axis < AXIS_COUNT; axis++) {
            final int centred = Mth.clamp(-(size[axis] / 2), OFFSET_MIN, OFFSET_MAX);

            offset[axis] = centred;
            offsetInputs[axis].setState(centred);
            offsetBoxes[axis].setValue(String.valueOf(centred));
            offsetBoxes[axis].setTextColor(TEXT_COLOR);
        }

        regionDirty = true;
    }

    //#region // --- LOAD --- //
    // * Has the player put enough cables in the slot?
    private boolean cablesChargeable() {
        return minecraft != null && minecraft.player != null
                && BackupDriveMenu.cablesRequired(minecraft.player);
    }

    private boolean awaitingCables() {
        return menu.isSaved() && !loadSent && !canAffordLoad();
    }

    private boolean canAffordLoad() {
        if (minecraft == null || minecraft.player == null) {
            return false;
        }
        if (!cablesChargeable()) {
            return true;
        }
        return menu.getCableStack().getCount() >= menu.getCableCost();
    }

    private void clickLoad() {
        if (!menu.isSaved() || loadSent) {
            return;
        }

        if (!canAffordLoad()) {
            costFlashTicks = COST_FLASH_TICKS;
            return;
        }

        PacketDistributor.sendToServer(new BackupDriveLoadPacket(menu.getDrivePos()));

        // * Set before closing
        loadSent = true;

        onClose();
    }
    //#endregion

    private void adoptDefaults() {
        blurActiveBox();

        for (int axis = 0; axis < AXIS_COUNT; axis++) {
            size[axis] = SIZE_MIN;
            offset[axis] = 0;

            sizeInputs[axis].setState(SIZE_MIN);
            offsetInputs[axis].setState(0);
            sizeBoxes[axis].setValue(String.valueOf(SIZE_MIN));
            offsetBoxes[axis].setValue("0");
            sizeBoxes[axis].setTextColor(TEXT_COLOR);
            offsetBoxes[axis].setTextColor(TEXT_COLOR);
        }

        rotationIndex = 0;
        rotationInput.setState(0);
    }

    private void clickReset() {
        if (!resetArmed) {
            resetArmed = true;
            return;
        }

        resetArmed = false;
        PacketDistributor.sendToServer(new BackupDriveResetPacket(menu.getDrivePos()));

        saveState = SaveState.IDLE;
        erasedTicks = ERASED_MESSAGE_TICKS;
        BackupDrivePreview.clear(menu.getDrivePos());
        // * The backing arrays are set first
        adoptDefaults();

        // * Sent so a drive that rejected the reset
        regionDirty = true;
    }

    private void togglePreview() {
        if (minecraft == null || minecraft.level == null) {
            return;
        }

        final boolean showing = BackupDrivePreview.isActive(menu.getDrivePos());
        if (showing) {
            BackupDrivePreview.clear(menu.getDrivePos());
        } else {
            BackupDrivePreview.show(
                    minecraft.level,
                    menu.getDrivePos(),
                    offset,
                    size,
                    rotationIndex
            );
        }

        playToggleSound(!showing);
    }

    private void playToggleSound(final boolean turningOn) {
        if (minecraft == null || minecraft.player == null) {
            return;
        }

        final BlockPos soundPos = minecraft.player.blockPosition();
        minecraft.player.level().playLocalSound(
                soundPos.getX() + 0.5,
                soundPos.getY() + 0.5,
                soundPos.getZ() + 0.5,
                turningOn ? SoundEvents.COPPER_BULB_TURN_ON : SoundEvents.COPPER_BULB_TURN_OFF,
                SoundSource.PLAYERS,
                0.7F,
                1.0F,
                false
        );
    }
    //#endregion

    private void initWidgets() {
        setWindowSize(WINDOW_WIDTH, WINDOW_HEIGHT);
        super.init();

        final int x = leftPos;
        final int y = topPos;

        for (int axis = 0; axis < AXIS_COUNT; axis++) {
            final int index = axis;
            final int valueX = x + VALUE_X + axis * VALUE_PITCH;

            sizeBoxes[axis] = wellBox(valueX, y + SIZE_ROW_Y, false);
            sizeInputs[axis] = new ScrollInput(valueX, y + SIZE_ROW_Y, WELL_WIDTH, WELL_HEIGHT)
                    .withRange(SIZE_MIN, SIZE_MAX + 1)
                    .titled(Component.translatable("drivebysable.backup_drive.size_axis", AXIS_NAMES[axis]))
                    .calling(value -> {
                        size[index] = value;
                        regionDirty = true;

                        if (sizeBoxes[index] != null && sizeBoxes[index] != activeBox) {
                            sizeBoxes[index].setValue(String.valueOf(value));
                        }
                    });
            sizeInputs[axis].setState(size[axis]);
            sizeBoxes[axis].setValue(String.valueOf(size[axis]));

            offsetBoxes[axis] = wellBox(valueX, y + OFFSET_ROW_Y, true);
            offsetInputs[axis] = new ScrollInput(valueX, y + OFFSET_ROW_Y, WELL_WIDTH, WELL_HEIGHT)
                    .withRange(OFFSET_MIN, OFFSET_MAX + 1)
                    .titled(Component.translatable("drivebysable.backup_drive.offset_axis", AXIS_NAMES[axis]))
                    .calling(value -> {
                        offset[index] = value;
                        regionDirty = true;
                        if (offsetBoxes[index] != null && offsetBoxes[index] != activeBox) {
                            offsetBoxes[index].setValue(String.valueOf(value));
                        }
                    });
            offsetInputs[axis].setState(offset[axis]);
            offsetBoxes[axis].setValue(String.valueOf(offset[axis]));

            addRenderableWidgets(sizeInputs[axis], offsetInputs[axis]);
            addRenderableOnly(sizeBoxes[axis]);
            addRenderableOnly(offsetBoxes[axis]);
        }

        //#region // --- ROTATION --- //
        rotationLabel = wellLabel(x + ROTATION_BOX_X, y + ROTATION_BOX_Y);
        rotationInput = new WrappingSelectionScrollInput(x + ROTATION_BOX_X, y + ROTATION_BOX_Y, ROTATION_BOX_WIDTH, WELL_HEIGHT)
                .forOptions(List.of(
                        Component.translatable("drivebysable.backup_drive.rotation_0"),
                        Component.translatable("drivebysable.backup_drive.rotation_90"),
                        Component.translatable("drivebysable.backup_drive.rotation_180"),
                        Component.translatable("drivebysable.backup_drive.rotation_270")
                ))
                .writingTo(rotationLabel)
                .titled(Component.translatable("drivebysable.backup_drive.rotation"))
                .calling(value -> { rotationIndex = value; regionDirty = true; });
        rotationInput.setState(rotationIndex);
        addRenderableWidgets(rotationInput, rotationLabel);
        //#endregion

        extraAreas = List.of(new Rect2i(
                x + BLOCK_ICON_X + RENDER_OFFSET_X,
                y + BLOCK_ICON_Y + RENDER_OFFSET_Y,
                RENDER_WIDTH,
                RENDER_HEIGHT
        ));
    }

    @Override
    public List<Rect2i> getExtraAreas() {
        return extraAreas;
    }

    @Override
    protected void renderBg(final GuiGraphics graphics, final float partialTicks, final int mouseX, final int mouseY) {
        final int x = leftPos;
        final int y = topPos;

        graphics.blit(background(), x, y, 0, 0, SPRITE_WIDTH, SPRITE_HEIGHT, SHEET_SIZE, SHEET_SIZE);
        AllGuiTextures.PLAYER_INVENTORY.render(graphics, x + INVENTORY_X, y + INVENTORY_TOP);

        //#region // --- BUTTONS --- //
        drawButton(graphics, x, y, RESET_X, ICON_RESET_U, mouseX, mouseY, false, false);
        drawButton(graphics, x, y, ALIGN_X, ICON_ALIGN_U, mouseX, mouseY, locked, false);
        drawButton(graphics, x, y, PREVIEW_X, ICON_PREVIEW_U, mouseX, mouseY, false, BackupDrivePreview.isActive(menu.getDrivePos()));
        drawButton(graphics, x, y, SAVE_X, ICON_SAVE_U, mouseX, mouseY, locked, false);
        drawButton(graphics, x, y, LOAD_X, ICON_LOAD_U, mouseX, mouseY, !menu.isSaved(), false);
        //#endregion

        final AllGuiTextures lamp = switch (saveState) {
            case SAVED -> AllGuiTextures.INDICATOR_GREEN;
            default -> AllGuiTextures.INDICATOR;
        };
        graphics.enableScissor(
                x + SAVE_X, y + INDICATOR_Y,
                x + SAVE_X + BUTTON_SIZE, y + INDICATOR_Y + INDICATOR_HEIGHT
        );
        lamp.render(graphics, x + SAVE_X, y + INDICATOR_Y);
        graphics.disableScissor();

        //#region // --- LOAD LAMP AND COST --- //
        if (menu.isSaved()) {
            final AllGuiTextures loadLamp;
            if (loadSent) {
                loadLamp = AllGuiTextures.INDICATOR_GREEN;
            } else if (canAffordLoad()) {
                loadLamp = AllGuiTextures.INDICATOR_YELLOW;
            } else {
                loadLamp = AllGuiTextures.INDICATOR_RED;
            }
            graphics.enableScissor(
                    x + LOAD_X, y + INDICATOR_Y,
                    x + LOAD_X + BUTTON_SIZE, y + INDICATOR_Y + INDICATOR_HEIGHT
            );
            loadLamp.render(graphics, x + LOAD_X, y + INDICATOR_Y);
            graphics.disableScissor();
        }

        {
            // * Centred at the top of the blue panel
            final boolean chargeable = cablesChargeable();

            // * Nothing is owed until a save is locked in
            final int owed = locked && chargeable ? menu.getCableCost() : 0;
            final int held = chargeable ? menu.getCableStack().getCount() : owed;

            final Component cost = Component.translatable(
                    "drivebysable.backup_drive.cable_cost", held, owed);

            final int costColor;
            if (costFlashTicks > 0) {
                costColor = costFlashTicks == COST_FLASH_WHITE_AT ? COST_FLASH_COLOR : COST_SHORT_COLOR;
            } else {
                // * Green means this load can go ahead
                costColor = locked && canAffordLoad() ? COST_READY_COLOR : restingCostColor();
            }

            graphics.drawString(
                    font,
                    cost,
                    x + BLUE_CENTRE_X - font.width(cost) / 2,
                    y + BLUE_TOP_Y,
                    costColor,
                    true
            );
        }
        //#endregion

        //#region // --- STATUS WELL --- //
        final String statusKey;
        final int statusColor;
        if (erasedTicks > 0) {
            statusKey = "drivebysable.backup_drive.status_erased";
            statusColor = STATUS_ERASED_COLOR;
        } else if (resetArmed || awaitingCables()) {
            statusKey = "drivebysable.backup_drive.status_waiting";
            statusColor = STATUS_WAITING_COLOR;
        } else if (locked) {
            statusKey = "drivebysable.backup_drive.status_ready";
            statusColor = STATUS_READY_COLOR;
        } else {
            statusKey = "drivebysable.backup_drive.status_idle";
            statusColor = STATUS_IDLE_COLOR;
        }

        final Component status = Component.translatable(statusKey);
        final String cursor = (statusTicks / CURSOR_BLINK_TICKS) % 2 == 0 ? CURSOR : "";

        graphics.drawString(
                font,
                Component.literal(status.getString() + cursor),
                x + STATUS_WELL_X + TEXT_INSET_X,
                y + STATUS_WELL_Y + TEXT_INSET_Y,
                statusColor,
                true
        );
        //#endregion

        GuiGameElement.of(driveIcon)
                .<GuiGameElement.GuiRenderBuilder>at(x + BLOCK_ICON_X, y + BLOCK_ICON_Y, BLOCK_ICON_Z)
                .scale(BLOCK_ICON_SCALE)
                .render(graphics);
    }

    @Override
    protected void renderLabels(final GuiGraphics graphics, final int mouseX, final int mouseY) {
        final int titleColor = background() == BACKGROUND_CREATIVE ? TITLE_COLOR_CREATIVE : TITLE_COLOR;
        graphics.drawString(font, title, (WINDOW_WIDTH - font.width(title)) / 2, TITLE_Y, titleColor, false);
    }

    @Override
    protected void renderTooltip(final GuiGraphics graphics, final int mouseX, final int mouseY) {

        if (isOverButton(mouseX, mouseY, ALIGN_X, BUTTON_ROW_Y)) {
            graphics.renderTooltip(
                    font,
                    Component.translatable("drivebysable.backup_drive.align").withStyle(ChatFormatting.GRAY),
                    mouseX,
                    mouseY
            );
            return;
        }

        if (isOverButton(mouseX, mouseY, RESET_X, BUTTON_ROW_Y)) {
            if (resetArmed) {
                graphics.renderTooltip(
                        font,
                        Component.translatable("drivebysable.backup_drive.reset_confirm")
                                .withStyle(ChatFormatting.RED),
                        mouseX,
                        mouseY
                );
            }
            return;
        }

        if (isOverButton(mouseX, mouseY, PREVIEW_X, BUTTON_ROW_Y)) {
            graphics.renderTooltip(
                    font,
                    Component.translatable(BackupDrivePreview.isActive(menu.getDrivePos())
                                    ? "drivebysable.backup_drive.preview_hide"
                                    : "drivebysable.backup_drive.preview")
                            .withStyle(ChatFormatting.GRAY),
                    mouseX,
                    mouseY
            );
            return;
        }

        if (isOverWell(mouseX, mouseY, CABLE_WELL_X, CABLE_WELL_Y, BUTTON_SIZE)
                && cablesChargeable()
                && menu.getCableStack().isEmpty()) {
            graphics.renderTooltip(
                    font,
                    Component.translatable("drivebysable.backup_drive.cable_slot").withStyle(ChatFormatting.GRAY),
                    mouseX,
                    mouseY
            );
            return;
        }

        if (isOverButton(mouseX, mouseY, LOAD_X, BUTTON_ROW_Y)) {
            final String key;
            final ChatFormatting style;
            if (!menu.isSaved()) {
                key = "drivebysable.backup_drive.load_empty";
                style = ChatFormatting.GRAY;
            } else if (loadSent) {
                key = "drivebysable.backup_drive.load_done";
                style = ChatFormatting.GREEN;
            } else if (canAffordLoad()) {
                key = "drivebysable.backup_drive.load_ready";
                style = ChatFormatting.YELLOW;
            } else {
                key = "drivebysable.backup_drive.load_insert";
                style = ChatFormatting.RED;
            }

            graphics.renderTooltip(font, Component.translatable(key).withStyle(style), mouseX, mouseY);
            return;
        }

        if (isOverButton(mouseX, mouseY, SAVE_X, BUTTON_ROW_Y)) {
            final String key = switch (saveState) {
                case SAVED -> "drivebysable.backup_drive.save_done";
                default -> "drivebysable.backup_drive.save";
            };

            final ChatFormatting style = switch (saveState) {
                case SAVED -> ChatFormatting.GREEN;
                default -> ChatFormatting.GRAY;
            };
            graphics.renderTooltip(font, Component.translatable(key).withStyle(style), mouseX, mouseY);
            return;
        }

        super.renderTooltip(graphics, mouseX, mouseY);
    }

    // * Rotation is scroll only
    private Label wellLabel(final int wellX, final int wellY) {
        return new Label(wellX + TEXT_INSET_X, wellY + TEXT_INSET_Y, Component.empty()).withShadow();
    }

    private EditBox wellBox(final int wellX, final int wellY, final boolean allowNegative) {
        final EditBox box = new EditBox(
                font,
                wellX + TEXT_INSET_X,
                wellY + TEXT_INSET_Y,
                WELL_WIDTH - TEXT_INSET_X * 2,
                8,
                Component.empty()
        );
        box.setBordered(false);
        box.setMaxLength(MAX_TYPED);
        box.setTextColor(TEXT_COLOR);

        box.setFilter(text -> text.matches(allowNegative ? "-?\\d*" : "\\d*"));
        return box;
    }


    // --- SCROLL INPUT THAT WRAPS INSTEAD OF STOPPING --- //
    private static final class WrappingSelectionScrollInput extends SelectionScrollInput {

        private WrappingSelectionScrollInput(final int x, final int y, final int width, final int height) {
            super(x, y, width, height);
        }

        @Override
        protected void clampState() {
            final int span = max - min;
            if (span <= 0) {
                super.clampState();
                return;
            }

            // * floorMod, scrolling below the first option lands on the last
            state = Math.floorMod(state - min, span) + min;
        }
    }
}