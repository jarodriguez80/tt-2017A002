package mx.gui

import javafx.application.Application
import javafx.fxml.FXMLLoader
import javafx.scene.Scene
import javafx.scene.image.Image
import javafx.scene.layout.AnchorPane
import javafx.scene.layout.HBox
import javafx.stage.Stage

class InstallerGUI extends Application {

    static void main(String[] args) {
        Application.launch(InstallerGUI.class, args)
    }

    @Override
    void start(Stage primaryStage) throws Exception {
        UiTracker uiTracker = UiTracker.getInstance()

        primaryStage.title = "OpenStack Swift Installer"
        primaryStage.width = 1000
        primaryStage.height = 630
        primaryStage.alwaysOnTop = true

        loadUiComponentsToTracker(uiTracker)

        Scene scene = new Scene(uiTracker.staticUiComponents.generalView, 1000, 630)

        Image imageIcon = new Image(this.class.classLoader.getResourceAsStream("images/hdd-icon.png"))
        primaryStage.icons.add(imageIcon)

        primaryStage.scene = scene
        primaryStage.show()
        primaryStage.alwaysOnTop = false
    }

    private loadUiComponentsToTracker(UiTracker uiTracker) {
        AnchorPane generalView = FXMLLoader.load(this.class.classLoader.getResource("fxml/generalView.fxml"))
        AnchorPane topPane = (AnchorPane) generalView.children.get(0)
        AnchorPane middlePane = (AnchorPane) generalView.children.get(1)
        AnchorPane bottomPane = (AnchorPane) generalView.children.get(2)
        HBox installationType = FXMLLoader.load(this.class.classLoader.getResource("fxml/installationType.fxml"))
        HBox bubbles = FXMLLoader.load(this.class.classLoader.getResource("fxml/bubbles.fxml"))

        uiTracker.staticUiComponents << ["generalView": generalView]
        uiTracker.staticUiComponents << ["topPane": topPane]
        uiTracker.staticUiComponents << ["middlePane": middlePane]
        uiTracker.staticUiComponents << ["bottomPane": bottomPane]

        uiTracker.customUiComponents << ["installationType": installationType]
        uiTracker.customUiComponents << ["bubbles": bubbles]
    }
}
