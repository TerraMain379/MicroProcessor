package ru.terramain.microprocessor.logic;

import ru.terramain.microprocessor.plate.PlateActionContext;
import ru.terramain.microprocessor.plate.PlateState;

public class MicroProcessorCore {
    public MicroProcessorWorker worker;

    public MicroProcessorCore() {
        worker = new MicroProcessorWorker();
    }

    public void updateCode(String code) {
        System.out.println("updateCode! restarting...");
        if (worker.isRunningStorage.isRunning) {
            worker.stop();
        }
        worker.run(code);
    }

    public void tick(MicroProcessorContext context) {
        worker.microProcessorContext = context;


        MicroProcessorWorker.LogMessage logMessage;
        while ((logMessage = worker.pollLog()) != null) {
            context.be.pushLogs(logMessage.toString());
            if (logMessage.level == MicroProcessorWorker.LogMessage.Level.ERROR) {
                context.be.setRunningNotify(false);
            }
        }


        if (!worker.isRunningStorage.isRunning || worker.dataPool == null || worker.microProcessorJsObject == null) return;

        worker.microProcessorJsObject.update(context);
        worker.dataPool.pushS2WMessage(new MicroProcessorWorker.EventS2WMessage("tick", new Object[]{}));

        MicroProcessorWorker.W2SMessage request;
        while ((request = worker.dataPool.getW2SRequest()) != null) {
            if (request instanceof MicroProcessorWorker.RequestMicroProcessorW2SMessage reqMicroProcessor) {
                // TODO:
            }
            else if (request instanceof MicroProcessorWorker.RequestPlateW2SMessage reqPlate) {
                PlateState<?, ?> plateState = context.be.getPlateState(reqPlate.direction);
                MicroProcessorWorker.AnswerS2WMessage answer;
                if (plateState.plate != reqPlate.plate) {
                    answer = new MicroProcessorWorker.AnswerS2WMessage(reqPlate.id, true, null);
                }
                else {
                    PlateActionContext<?> plateActionContext = new PlateActionContext(plateState, reqPlate.direction, context);
                    answer = plateState.plate.request(reqPlate, plateActionContext);
                }
                worker.dataPool.pushS2WMessage(answer);
            }
            else {
                throw new RuntimeException("unknown RequestW2SMessage type: " + request.getClass().getSimpleName());
            }
        }
    }

    public boolean isRunning() {
        return this.worker.isRunningStorage.isRunning;
    }
}
