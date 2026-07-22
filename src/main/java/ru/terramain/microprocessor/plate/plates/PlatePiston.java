package ru.terramain.microprocessor.plate.plates;

import ru.terramain.microprocessor.plate.*;
import ru.terramain.microprocessor.plate.plates.piston.AbstractPlatePiston;

public class PlatePiston extends AbstractPlatePiston {
    public static String TYPE = "piston";

    protected PlatePiston() {
        super(TYPE, "block/microprocessor_piston", PlatePiston::instance, false);
    }

    private static PlatePiston inst = null;
    public static PlatePiston instance() {
        if (inst == null) {
            inst = PlateRegister.register(new PlatePiston());
        }
        return inst;
    }
}
