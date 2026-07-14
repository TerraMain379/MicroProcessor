package ru.terramain.microprocessor.logic;


import dev.ryanhcode.sable.Sable;
import ru.terramain.microprocessor.block.MicroProcessorBlockEntity;
import ru.terramain.microprocessor.deps.Integration;

public class MicroProcessorContext {
    public MicroProcessorBlockEntity be;
    public boolean externalLocked;
    public boolean inContraption;

    public MicroProcessorContext(MicroProcessorBlockEntity be, boolean externalLocked, boolean inContraption) {
        this.be = be;
        this.externalLocked = externalLocked;
        this.inContraption = inContraption;
    }
    public MicroProcessorContext(MicroProcessorBlockEntity be, boolean externalLocked) {
        this.be = be;
        this.externalLocked = externalLocked;
        if (Integration.IS_CREATE_LOADED) {
            this.inContraption = Sable.HELPER.getContaining(be) != null;
        }
        else this.inContraption = false;
    }
    public MicroProcessorContext(MicroProcessorBlockEntity be) {
        this(be, false);
    }
}
