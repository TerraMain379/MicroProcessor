package ru.terramain.microprocessor.block;

import ru.terramain.microprocessor.logic.MicroProcessorCore;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class MicroProcessorCoresManager {
    public static Map<UUID, MicroProcessorCore> cores = new HashMap<>();


    public static UUID moveCore(MicroProcessorCore core) {
        UUID uuid = UUID.randomUUID();
        cores.put(uuid, core);
        return uuid;
    }
    public static MicroProcessorCore stopMoveCore(UUID uuid) {
        return cores.remove(uuid);
    }
}
