package edn.lakeopossmc.drivebysable;

import net.neoforged.neoforge.common.ModConfigSpec;
import org.apache.commons.lang3.tuple.Pair;

// --- MOD CONFIG DEF --- //
public class CableConfig {

    public static final CableConfig CONFIG;
    public static final ModConfigSpec CONFIG_SPEC;

    public final ModConfigSpec.BooleanValue shouldConsumeCables;
    public final ModConfigSpec.BooleanValue allowCableDisconnect;
    public final ModConfigSpec.BooleanValue enforceRangeLimit;
    public final ModConfigSpec.IntValue rangeLimit;
    public final ModConfigSpec.IntValue maxOutputsPerChannel;
    public final ModConfigSpec.IntValue maxSourcesPerSubLevel;
    public final ModConfigSpec.IntValue maxSourcesInWorld;

    private CableConfig(ModConfigSpec.Builder builder) {
        shouldConsumeCables = builder
                .comment(
                        "Whether making a connection consumes a Cable from the player's stack.",
                        "Cables are refunded when a connection is removed.",
                        "Creative mode players are never charged."
                )
                .translation("drivebysable.config.shouldConsumeCables")
                .define("shouldConsumeCables", true);

        allowCableDisconnect = builder
                .comment(
                        "Whether the Cable itself can remove an existing connection.",
                        "When false, pointing a Cable at a connection it already made does nothing,",
                        "and the Cable Cutter becomes the only way to remove connections.",
                        "The Cable Cutter is unaffected by this option."
                )
                .translation("drivebysable.config.allowCableDisconnect")
                .define("allowCableDisconnect", false);

        enforceRangeLimit = builder
                .comment(
                        "Whether connections are limited by distance between the source and the output.",
                        "When false, connections can be made at any distance and rangeLimit is ignored."
                )
                .translation("drivebysable.config.enforceRangeLimit")
                .define("enforceRangeLimit", false);

        rangeLimit = builder
                .comment(
                        "Furthest a connection may reach, in blocks, when enforceRangeLimit is true.",
                        "Measured as straight line distance between the two block positions.",
                        "0 blocks any connection between separate blocks. Has no effect while",
                        "enforceRangeLimit is false."
                )
                .translation("drivebysable.config.rangeLimit")
                .defineInRange("rangeLimit", 512, 0, 512);

        maxOutputsPerChannel = builder
                .comment(
                        "How many Outputs a single Channel on one Source may drive.",
                        "Counted per Source and per Channel, so a Hub with several Channels",
                        "gets this budget on each of them independently."
                )
                .translation("drivebysable.config.maxOutputsPerChannel")
                .defineInRange("maxOutputsPerChannel", 64, 0, 2048);

        maxSourcesPerSubLevel = builder
                .comment(
                        "How many distinct Cable Sources may exist inside one sublevel.",
                        "Each sublevel gets its own budget. Counted per Source block, not per",
                        "connection, so a Source already in use is never counted again."
                )
                .translation("drivebysable.config.maxSourcesPerSubLevel")
                .defineInRange("maxSourcesPerSubLevel", 64, 0, 2048);

        maxSourcesInWorld = builder
                .comment(
                        "How many distinct Cable Sources may exist loose in the world.",
                        "Everything outside a sublevel shares this single budget."
                )
                .translation("drivebysable.config.maxSourcesInWorld")
                .defineInRange("maxSourcesInWorld", 128, 0, 2048);
    }

    // * Build config and spec together
    static {
        Pair<CableConfig, ModConfigSpec> pair = new ModConfigSpec.Builder().configure(CableConfig::new);
        CONFIG = pair.getLeft();
        CONFIG_SPEC = pair.getRight();
    }

}