package ru.terramain.microprocessor.plate.plates;

import ru.terramain.microprocessor.plate.PlateRegister;

public class PlateStickyPiston extends AbstractPlatePiston {
    public static String TYPE = "sticky_piston";

    protected PlateStickyPiston() {
        super(TYPE, "block/microprocessor_sticky_piston", PlateStickyPiston::instance, true);
    }

    private static PlateStickyPiston inst = null;
    public static PlateStickyPiston instance() {
        if (inst == null) {
            inst = PlateRegister.register(new PlateStickyPiston());
        }
        return inst;
    }
}
