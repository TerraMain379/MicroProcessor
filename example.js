processor.on("tick", () => {
    if (processor.inMove()) return;
    processor.getPlates().map(plate => {
        let redstoneDistributor = plate.getRedstoneDistributor();
        if (redstoneDistributor) {
            redstoneDistributor.setActive(!redstoneDistributor.getActive());
        }
    });
});

let globalPistonActive = false;
processor.on("observerUpdate", () => {
    if (processor.inMove()) return;
    globalPistonActive = !globalPistonActive;
    processor.getPlates().map(plate => {
        let piston = plate.getPison();
        if (piston) {
            piston.setActive(globalPistonActive);
        }
    });
});





let updated1 = false;
processor.on("tick", () => {
    let upPlate = processor.plates.up;
    if (upPlate) {
        let upPlateType = upPlate.type;
        let upPlateData = upPlate.data;
        if (upPlateType === "observer") {
            let updated = upPlateData.updated;
            updated1 = updated;
        }
    }

    let downPlate = processor.plates.down;
    if (downPlate) {
        let downPlateType = downPlate.type;
        let downPlateTasker = downPlate.tasker;
        if (downPlateType === "redstone_distributor") {
            downPlateTasker.setSignal(updated1 ? 15 : 0);
        }
    }
});



let updated1 = false;
processor.on("tick", () => {
    let upPlate = processor.getPlate("up");
    if (upPlate) {
        let observerPlate = upPlate.is("observer");
        if (observerPlate) {
            let updated = observerPlate.updated;
            updated1 = updated;
        }
    }

    let downPlate = processor.getPlate("down");
    if (downPlate) {
        let redstoneDistributor = downPlate.is("redstone_distributor");
        if (redstoneDistributor) {
            redstoneDistributor.setSignal(updated1 ? 15 : 0);
        }
    }
});
processor.on("comparator_update", async direction => {
    let plate = processor.getPlate(direction);
    let comparatorPlate = plate.is("comparator");
    comparatorPlate.setLock(true);
    await processor.getPause(10);
    comparatorPlate.setLock(false);
});




{
    let plate = mp.plate("UP");
    let observerPlate = plate.is("observer");
    observerPlate.on("update", () => {
        mp.log("update")
    });
}