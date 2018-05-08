package mx.gui

import javafx.fxml.FXMLLoader
import javafx.scene.layout.AnchorPane
import javafx.scene.layout.HBox
import javafx.scene.layout.Pane

@Singleton
class UiTracker {
    Map staticUiComponents = [:]
    Map customUiComponents = [:]
    Integer currentStep = 0
    Map servers = [:]
    Map serverData = [:]

    def getCurrentComponent() {
        if (currentStep <= 0) {
            currentStep = 0
            new Pane(visible: false)
        } else if (currentStep == 1) {
            customUiComponents["installationType"]
        } else if (currentStep == 2) {
            customUiComponents["serverDataUIComponent"]
        } else if (currentStep == 3) {
            customUiComponents["diskUIComponent"]
        }
    }

    HBox loadServerDataUIForm() {
        return FXMLLoader.load(this.class.classLoader.getResource("fxml/serverDataUIComponent.fxml"))
    }

    AnchorPane loadDisUIComponent() {
        return FXMLLoader.load(this.class.classLoader.getResource("fxml/diskUI.fxml"))
    }
}
