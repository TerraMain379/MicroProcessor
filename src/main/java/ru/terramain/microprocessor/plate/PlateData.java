package ru.terramain.microprocessor.plate;

public interface PlateData {
    <D extends PlateData> D copy();
}
