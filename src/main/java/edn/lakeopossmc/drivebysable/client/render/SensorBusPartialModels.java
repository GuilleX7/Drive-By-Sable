package edn.lakeopossmc.drivebysable.client.render;

import dev.engine_room.flywheel.lib.model.baked.PartialModel;
import edn.lakeopossmc.drivebysable.DriveBySableMod;

// --- THE MOVING PIECES OF THE INTEGRATED SENSOR BUS --- //
public final class SensorBusPartialModels {

    private static final String PREFIX = "block/integrated_sensor_bus/";

    public static final PartialModel FAN_WEST = of("fan_west");
    public static final PartialModel FAN_EAST = of("fan_east");
    public static final PartialModel HAND_WEST = of("hand_west");
    public static final PartialModel HAND_EAST = of("hand_east");
    public static final PartialModel GIMBAL = of("gimbal");
    public static final PartialModel COMPASS = of("compass");
    public static final PartialModel NEEDLE = of("needle");

    private SensorBusPartialModels() {
    }

    private static PartialModel of(final String name) {
        return PartialModel.of(DriveBySableMod.asResource(PREFIX + name));
    }

    // * Touching the class is enough to register them all
    public static void load() {
    }
}