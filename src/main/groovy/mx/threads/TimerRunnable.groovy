package mx.threads

import javafx.beans.property.DoubleProperty
import javafx.scene.control.Label
import javafx.scene.control.ProgressBar
import javafx.scene.control.ProgressIndicator

class TimerRunnable implements Runnable {

    DoubleProperty progressProperty

    @Override
    void run() {
        try {
            Thread.sleep(5000)
            count()
        } catch (InterruptedException ex) {
            println "Interrupted"
        }
    }


    private void count() throws InterruptedException {
        1.upto(10, {
            def value = it / 10
            progressProperty.value = value
            Thread.sleep(1000)
        })

    }
}
