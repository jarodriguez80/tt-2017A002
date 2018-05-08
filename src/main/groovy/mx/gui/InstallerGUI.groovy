package mx.gui

import javafx.application.Application
import javafx.event.EventHandler
import javafx.fxml.FXMLLoader
import javafx.geometry.Pos
import javafx.scene.Scene
import javafx.scene.control.Label
import javafx.scene.image.Image
import javafx.scene.image.ImageView
import javafx.scene.input.MouseEvent
import javafx.scene.layout.AnchorPane
import javafx.scene.layout.HBox
import javafx.scene.layout.VBox
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

    final Image image = new Image(this.class.classLoader.getResourceAsStream("images/hdd-icon.png"))

    HBox buildDiskComponent(def number) {
        HBox customPane = new HBox()
        customPane.styleClass.add("disk-wrapper")
        customPane.setOnMouseClicked(new EventHandler<MouseEvent>() {
            @Override
            void handle(MouseEvent event) {
                HBox box = (HBox) event.getSource()
                if ((box.style =~ "-fx-background-color: deepskyblue;").find())
                    box.style = ""
                else
                    ((HBox) event.getSource()).style = "-fx-background-color: deepskyblue;"
            }
        })

        ImageView diskWrapperIcon = new ImageView(image)
        VBox diskInfo = new VBox()

        diskInfo.setAlignment(Pos.CENTER)
        diskInfo.styleClass.add("disk-info")
        Label diskName = new Label("Device Name: sda$number")
        Label diskSize = new Label("Device Size: ${number * 10} Gigabytes")
        diskInfo.children.addAll(diskName, diskSize)

        customPane.children.addAll(diskWrapperIcon, diskInfo)
        customPane
    }

}
