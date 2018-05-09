package mx.gui.utils

import javafx.event.ActionEvent
import javafx.event.EventHandler
import javafx.fxml.FXMLLoader
import javafx.geometry.Pos
import javafx.scene.Node
import javafx.scene.Parent
import javafx.scene.Scene
import javafx.scene.control.Label
import javafx.scene.image.Image
import javafx.scene.image.ImageView
import javafx.scene.input.MouseEvent
import javafx.scene.layout.HBox
import javafx.scene.layout.Pane
import javafx.scene.layout.VBox

class GuiUtil {
    // TODO Remove this, because it's unused
    static void loadSceneFromFXML(ActionEvent event, String fxmlFile) {
        //Parent root = FXMLLoader.load(this.class.classLoader.getResource(fxmlFile))
        Parent root = FXMLLoader.load(ClassLoader.systemClassLoader.getResource(fxmlFile))
        Scene scene = new Scene(root, 300, 275)
        ((Node) event.source).scene.window.scene = scene
    }

    static void loadNode(ActionEvent event, Node node) {
        //Parent root = FXMLLoader.load(this.class.classLoader.getResource(fxmlFile))
        Scene scene = new Scene(node, 300, 275)
        ((Node) event.source).scene.window.scene = scene
    }

    final Image image = new Image(this.class.classLoader.getResourceAsStream("images/hdd-icon.png"))

    Pane buildDiskComponent(def number, String nodeAddress, Map devicesPerNode) {
        Pane wrapper = new Pane()
        wrapper.styleClass.add("wrapper-for-disk")


        HBox customPane = new HBox()
        customPane.styleClass.add("disk-wrapper")
        customPane.properties.put("nodeAddress", "$nodeAddress")

        customPane.setOnMouseClicked(new EventHandler<MouseEvent>() {
            @Override
            void handle(MouseEvent event) {
                HBox parentPane = (HBox) event.getSource()

                String address = parentPane.properties.nodeAddress
                String deviceName = ((Label) ((VBox) parentPane.children.last()).children.first()).id

                if ((parentPane.style =~ "-fx-background-color: #3B5998;").find()) {
                    parentPane.style = ""
                    ((VBox) parentPane.children.last()).children*.style = ""
                    ((List) devicesPerNode[address]).remove(deviceName)
                } else {
                    parentPane.style = "-fx-background-color: #3B5998;"
                    ((VBox) parentPane.children.last()).children*.style = "-fx-text-fill: #d3d3d3;-fx-font-size: 16;"
                    if (!devicesPerNode[address]) {
                        devicesPerNode[address] = []
                    }
                    devicesPerNode[address] << deviceName
                }
            }
        })

        ImageView diskWrapperIcon = new ImageView(image)
        VBox diskInfo = new VBox()

        diskInfo.setAlignment(Pos.CENTER)
        diskInfo.styleClass.add("disk-info")
        Label diskName = new Label("Device Name: sda$number")
        diskName.id = "sda$number"
        diskName.styleClass.add("disk-info-label")
        Label diskSize = new Label("Device Size: ${number * 10} Gigabytes")
        diskSize.styleClass.add("disk-info-label")

        diskInfo.children.addAll(diskName, diskSize)
        customPane.children.addAll(diskWrapperIcon, diskInfo)

        def tmpLabel = new Label("Some")
        tmpLabel.styleClass.add("label")
        wrapper.children.add(customPane)

        wrapper
    }
}
