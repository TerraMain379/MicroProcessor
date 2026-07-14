package ru.terramain.microprocessor.plate;

import java.util.*;

public class PlateRegister {
    private static final Map<String, Plate<?>> plates = new LinkedHashMap<>();

    public static <T extends Plate<?>> T register(T plate) {
        if (plates.containsKey(plate.type)) {
            throw new IllegalStateException("Plate type already registered: " + plate.type);
        }
        plates.put(plate.type, plate);
        return plate;
    }
    public static <T extends Plate<?>> T getPlate(Class<T> clazz) {
        return plates.values().stream()
                .filter(clazz::isInstance)
                .map(clazz::cast)
                .findFirst()
                .orElse(null);
    }

    public static Plate<?> getPlateByType(String type) {
        return plates.get(type);
    }
    public static AbstractPlateItem<?, ?> getItemByType(String type) {
        Plate<?> plate = plates.get(type);
        return plate == null ? null : plate.item.get();
    }
    public static Set<String> allTypes() {
        return Collections.unmodifiableSet(plates.keySet());
    }
}
