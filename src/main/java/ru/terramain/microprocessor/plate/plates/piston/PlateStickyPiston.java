package ru.terramain.microprocessor.plate.plates.piston;

import ru.terramain.microprocessor.plate.PlateRegister;

public class PlateStickyPiston extends AbstractPlatePiston {
    public static String TYPE = "sticky_piston";

    protected PlateStickyPiston() {
        super(TYPE, "block/microprocessor_sticky_piston", PlateStickyPiston::instance, false);
    }

    private static PlateStickyPiston inst = null;
    public static PlateStickyPiston instance() {
        if (inst == null) {
            inst = PlateRegister.register(new PlateStickyPiston());
        }
        return inst;
    }
}
