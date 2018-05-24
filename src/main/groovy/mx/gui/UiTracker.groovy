package mx.gui

import javafx.fxml.FXMLLoader
import javafx.scene.layout.AnchorPane
import javafx.scene.layout.HBox
import javafx.scene.layout.Pane

/**
 * This class its used for keep a track of the graphic user interface components loaded, and to load. Also track the information generated by the GUI
 * as the server data (like ip address, sshKey for root user)
 *
 * */

@Singleton
class UiTracker {
    Map staticUiComponents = [:]
    Map customUiComponents = [:]
    Integer currentStep = 0
    Map serverData = [:]

    /**
     * Return the gui component indicated by the current step.
     *
     * */
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

    /**
     * Load gui component for the server data as the ip address and ssh key for root user.
     *
     * */
    HBox loadServerDataUIForm() {
        return FXMLLoader.load(this.class.classLoader.getResource("fxml/serverDataUIComponent.fxml"))
    }

    /**
     * Load gui component for the Block Device.
     *
     * */
    AnchorPane loadDiskUIComponent() {
        return FXMLLoader.load(this.class.classLoader.getResource("fxml/diskUI.fxml"))
    }

    /**
     * Load gui component for the installation progress.
     *
     * */
    HBox loadProgressComponent() {
        return FXMLLoader.load(this.class.classLoader.getResource("fxml/progresBarUIComponent.fxml"))
    }
}
