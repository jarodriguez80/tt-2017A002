package mx.core

import javafx.beans.property.SimpleDoubleProperty

class ProgressProperty extends SimpleDoubleProperty {
    private Double stepsExecuted = 0
    private Double totalSteps

    void addStepExecuted(double stepsAdded = 1) {
        this.stepsExecuted += stepsAdded
        this.calculateTotalProgress()
    }

    private void calculateTotalProgress() {
        this.value = (stepsExecuted / totalSteps) as Number
    }
}