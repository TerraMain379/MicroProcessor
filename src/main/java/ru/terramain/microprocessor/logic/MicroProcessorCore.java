package ru.terramain.microprocessor.logic;

import ru.terramain.microprocessor.plate.PlateActionContext;
import ru.terramain.microprocessor.plate.PlateState;

import java.util.ArrayList;

public class MicroProcessorCore {
    public MicroProcessorWorker worker;
    public ArrayList<WaitTask> waitTasks;
    private boolean isScheduleRun;

    public MicroProcessorCore() {
        this.worker = new MicroProcessorWorker();
        this.waitTasks = new ArrayList<>();
        this.isScheduleRun = false;
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
        if (this.isScheduleRun) {
            if (!this.isRunning()) {
                context.be.setRunning(true, true);
            }
            this.isScheduleRun = false;
        }
        tickLogs(context);
        if (!worker.isRunningStorage.isRunning || worker.dataPool == null || worker.microProcessorJsObject == null) return;
        worker.microProcessorJsObject.update(context);
        worker.dataPool.pushS2WMessage(new MicroProcessorWorker.EventS2WMessage("tick", new Object[]{}));
        tickTimers(context);
        tickRequests(context);
    }
    protected void tickLogs(MicroProcessorContext context) {
        MicroProcessorWorker.LogMessage logMessage;
        while ((logMessage = worker.pollLog()) != null) {
            context.be.pushLogs(logMessage.toString());
            if (logMessage.level == MicroProcessorWorker.LogMessage.Level.ERROR) {
                context.be.setRunningNotify(false);
            }
        }
    }
    protected void tickTimers(MicroProcessorContext context) {
        for (WaitTask waitTask : waitTasks) {
            waitTask.time--;
            if (waitTask.time == 0) {
                worker.dataPool.pushS2WMessage(new MicroProcessorWorker.AnswerS2WMessage(
                        waitTask.id,
                        false,
                        null
                ));
            }
        }
        waitTasks.removeIf(waitTask -> waitTask.time == 0);
    }
    protected void tickRequests(MicroProcessorContext context) {

        MicroProcessorWorker.W2SMessage request;
        while ((request = worker.dataPool.getW2SRequest()) != null) {
            if (request instanceof MicroProcessorWorker.RequestMicroProcessorW2SMessage reqMicroProcessor) {
                // TODO:
            }
            else if (request instanceof MicroProcessorWorker.RequestPlateW2SMessage reqPlate) {
                PlateState<?, ?> plateState = context.be.getPlateState(reqPlate.direction);
                MicroProcessorWorker.AnswerS2WMessage answer;
                if (plateState == null || plateState.plate != reqPlate.plate) {
                    answer = new MicroProcessorWorker.AnswerS2WMessage(reqPlate.id, true, null);
                }
                else {
                    PlateActionContext<?> plateActionContext = new PlateActionContext(plateState, reqPlate.direction, context);
                    answer = plateState.plate.request(reqPlate, plateActionContext);
                }
                worker.dataPool.pushS2WMessage(answer);
            }
            else if (request instanceof MicroProcessorWorker.RequestWaitW2SMessage reqWait) {
                waitTasks.add(new WaitTask(reqWait.id, reqWait.tickTime));
            }
            else {
                throw new RuntimeException("unknown RequestW2SMessage type: " + request.getClass().getSimpleName());
            }
        }
    }

    public boolean isRunning() {
        return this.worker.isRunningStorage.isRunning;
    }
    public void scheduleRun() {
        this.isScheduleRun = true;
    }


    public static class WaitTask {
        public final long id;
        public int time;

        public WaitTask(long id, int time) {
            this.id = id;
            this.time = time;
        }
    }
}
