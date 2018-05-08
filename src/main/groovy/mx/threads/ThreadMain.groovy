package mx.threads

import javafx.application.Application
import javafx.beans.property.DoubleProperty
import javafx.beans.property.SimpleDoubleProperty
import javafx.beans.property.SimpleStringProperty
import javafx.beans.value.ChangeListener
import javafx.beans.value.ObservableValue
import javafx.geometry.Pos
import javafx.scene.Group
import javafx.scene.Scene
import javafx.scene.control.Label
import javafx.scene.control.ProgressBar
import javafx.scene.control.ProgressIndicator
import javafx.scene.layout.HBox
import javafx.scene.layout.VBox
import javafx.stage.Stage
import javafx.util.converter.FormatStringConverter

import java.sql.Time

class ThreadMain extends Application{
    @Override
    public void start(Stage stage){
        Group root = new Group()
        Scene scene = new Scene(root, 300, 150)
        stage.setScene(scene)
        stage.setTitle("Progress Controls")

        Label label = new Label()
        ProgressBar pb = new ProgressBar()
        ProgressIndicator pin = new ProgressIndicator()

        HBox hb = new HBox()
        hb.setSpacing(5)
        hb.setAlignment(Pos.CENTER)
        hb.getChildren().addAll(label, pb, pin)

        VBox vb = new VBox()
        vb.setSpacing(5)
        vb.getChildren().add(hb)
        scene.setRoot(vb)
        stage.show()




        DoubleProperty progress = new SimpleDoubleProperty(0)
        progress.addListener(new ChangeListener<Number>() {
            @Override
            void changed(ObservableValue<? extends Number> observable, Number oldValue, Number newValue) {
                //pin.setProgress(newValue as Double)
                def valueChanged = newValue.doubleValue() as String
                /*pb.setProgress(newValue.doubleValue())*/
                println valueChanged
            }
        })
        pb.progressProperty().bind(progress)
        pin.progressProperty().bind(progress)

        TimerRunnable timer = new TimerRunnable(progressProperty: progress)
        new Thread(timer).start()
    }
}
