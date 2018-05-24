package mx.gui.controller

import javafx.event.EventHandler
import javafx.fxml.FXML
import javafx.scene.control.Button
import javafx.scene.control.Label
import javafx.scene.control.ProgressBar
import javafx.scene.control.ProgressIndicator
import javafx.scene.control.RadioButton
import javafx.scene.control.Tab
import javafx.scene.control.TabPane
import javafx.scene.control.TextField
import javafx.scene.input.MouseEvent
import javafx.scene.layout.AnchorPane
import javafx.scene.layout.FlowPane
import javafx.scene.layout.HBox
import javafx.scene.layout.StackPane
import javafx.scene.layout.VBox
import javafx.scene.shape.Circle
import javafx.stage.FileChooser
import mx.core.InstallerProcess
import mx.core.NodeTypes
import mx.core.ProcessType
import mx.core.ProgressProperty
import mx.gui.UiTracker
import mx.retriever.BlockDevice
import mx.retriever.RemoteHost
import mx.retriever.RemoteUser
import mx.retriever.TaskHelper

class NavigationController {

    @FXML
    protected void changeContent(MouseEvent event) {
        UiTracker uiTracker = UiTracker.getInstance()
        Button button = (Button) event.source
        String id = button.id

        // Switching current step.
        if (id == "nextButton" && uiTracker.currentStep < 4) {
            ++uiTracker.currentStep
        } else if (id == "backButton" && uiTracker.currentStep > 0) {
            --uiTracker.currentStep
        }

        // Setting data necessary for load disks, by changeMiddlePane method.
        if (uiTracker.currentStep == 3) {
            Set serverNames = uiTracker.serverData.keySet()
            serverNames.each {
                Map serverDataMap = uiTracker.serverData["$it"] as Map

                HBox uiComponent = (HBox) serverDataMap.uiComponent
                String ipAddress = ((TextField) uiComponent.children[1]).text
                File sshKey = uiTracker.serverData["$it"]["sshKey"]
                RemoteUser remoteUser = new RemoteUser(user: "root", sshKey: sshKey)
                RemoteHost remoteHost = new RemoteHost(ipAddress: ipAddress, sshPort: 22)
                serverDataMap.remoteUser = remoteUser
                serverDataMap.remoteHost = remoteHost
            }
        }

        // Change ui for bottom and middle panes.
        changeMiddlePane(uiTracker)
        changeTopPane(uiTracker)

        // Perform installation
        InstallerProcess installerProcess = new InstallerProcess()
        if ("allInOne" in uiTracker.serverData.keySet()) {
            installerProcess.processType = ProcessType.ALL_IN_ONE
        } else {
            installerProcess.processType = ProcessType.DIVIDED
        }

        if (uiTracker.currentStep == 4) {
            try {
                Thread.start {
                    if (installerProcess.isAllInOneProcessType()) {
                        Map authenticationNode = uiTracker.serverData.allInOne
                        authenticationNode.types = [NodeTypes.ALL_IN_ONE, NodeTypes.CENTRAL_NODE_STORAGE, NodeTypes.AUTHENTICATION]

                        /*HBox progressComponent = uiTracker.serverData.allInOne.progressUiComponent
                        ProgressBar progressBar = progressComponent.children[2]*/

                        installerProcess.installOpenStackSwift(authenticationNode, authenticationNode, [])
                    } else if (installerProcess.isDividedProcessType()) {
                        Map authenticationNode = uiTracker.serverData.authentication
                        authenticationNode.types = [NodeTypes.AUTHENTICATION]

                        Map storageNode = uiTracker.serverData.centralStorage
                        storageNode.types = [NodeTypes.CENTRAL_NODE_STORAGE]

                        installerProcess.installOpenStackSwift(authenticationNode, storageNode, [])
                    }
                }
            } catch (Exception e) {
                println e
            }
        }
    }

    private void changeTopPane(UiTracker uiTracker) {
        if (uiTracker.currentStep == 0) {
            uiTracker.staticUiComponents.topPane.children.clear()
        } else if (uiTracker.currentStep in [1, 2, 3, 4]) {
            HBox bubblesComponent = (HBox) uiTracker.customUiComponents.bubbles
            List<StackPane> bubbles = bubblesComponent.children
            bubbles*.children*.first().styleClass*.clear()
            bubbles*.children*.first().styleClass*.add("bubble-unselected")
            bubbles*.children*.last().styleClass*.add("label-unselected")


            StackPane stack = bubbles.get(uiTracker.currentStep - 1)
            Circle circle = ((Circle) stack.children.first())
            Label label = ((Label) stack.children.last())
            circle.styleClass.addAll("bubble-selected")
            label.styleClass.addAll("label-selected")

            uiTracker.staticUiComponents.topPane.children.clear()
            uiTracker.staticUiComponents.topPane.children.add(bubblesComponent)
        }
    }

    private void changeMiddlePane(UiTracker uiTracker) {
        uiTracker.staticUiComponents.middlePane.children.clear()

        if (uiTracker.currentStep == 1) {
            uiTracker.staticUiComponents.middlePane.children.add(uiTracker.getCurrentComponent())
        } else if (uiTracker.currentStep == 2) {
            uiTracker.serverData = [:]
            loadContentForServerData(uiTracker)
        } else if (uiTracker.currentStep == 3) {
            // Get block devices
            loadBlockDevice(uiTracker)
        } else if (uiTracker.currentStep == 4) {
            loadProgressComponents(uiTracker)
        }
    }

    void loadProgressComponents(UiTracker uiTracker) {
        Set serverNames = uiTracker.serverData.keySet()
        VBox column = new VBox()

        serverNames.each {
            Map serverDataMap = uiTracker.serverData["$it"] as Map

            HBox progressComponent = (HBox) serverDataMap.progressUiComponent

            Label label = progressComponent.children.first() as Label
            label.text = "Server ${serverDataMap.remoteHost.ipAddress}"
            column.children.add(progressComponent)

            ProgressBar progressBar = progressComponent.children[1]
            ProgressIndicator progressIndicator = progressComponent.children.last()
            progressBar.progressProperty().bind(serverDataMap.progress as ProgressProperty)
            progressIndicator.progressProperty().bind(serverDataMap.progress as ProgressProperty)

        }
        uiTracker.staticUiComponents.middlePane.children.add(column)
    }

    private loadContentForServerData(UiTracker uiTracker) {
        uiTracker.staticUiComponents.middlePane.children.clear()
        HBox installationTypeComponent = (HBox) uiTracker.customUiComponents.get("installationType")
        RadioButton singleNode = ((VBox) installationTypeComponent.children.first()).children.first()
        RadioButton twoNodes = ((VBox) installationTypeComponent.children.first()).children.last()

        VBox column = new VBox()

        if (singleNode.selected) {
            HBox uniqueServerDataUIComponent = uiTracker.loadServerDataUIForm()
            uniqueServerDataUIComponent.id = "allInOne"

            HBox allInOneProgressUiComponent = uiTracker.loadProgressComponent()
            allInOneProgressUiComponent.id = "authentication"

            ProgressProperty progressProperty = new ProgressProperty()

            uiTracker.serverData.allInOne = [uiComponent: uniqueServerDataUIComponent, progressUiComponent: allInOneProgressUiComponent, progress: new ProgressProperty()]
            column.children.add(uniqueServerDataUIComponent)
            addListenerToAll(uiTracker, uniqueServerDataUIComponent)
        } else if (twoNodes.selected) {
            HBox authenticationServerDataUIComponent = uiTracker.loadServerDataUIForm()
            authenticationServerDataUIComponent.id = "authentication"
            HBox centralStorageServerDataUIComponent = uiTracker.loadServerDataUIForm()
            centralStorageServerDataUIComponent.id = "centralStorage"

            HBox authenticationServerProgressUiComponent = uiTracker.loadProgressComponent()
            authenticationServerProgressUiComponent.id = "authentication"
            HBox centralStorageServerProgressUiComponent = uiTracker.loadProgressComponent()
            centralStorageServerProgressUiComponent.id = "centralStorage"

            uiTracker.serverData.authentication = [uiComponent: authenticationServerDataUIComponent, progressUiComponent: authenticationServerProgressUiComponent, progress: new ProgressProperty()]
            uiTracker.serverData.centralStorage = [uiComponent: centralStorageServerDataUIComponent, progressUiComponent: centralStorageServerProgressUiComponent, progress: new ProgressProperty()]
            addListenerToAll(uiTracker, authenticationServerDataUIComponent, centralStorageServerDataUIComponent)

            column.children.addAll(authenticationServerDataUIComponent, centralStorageServerDataUIComponent)
        }

        uiTracker.staticUiComponents.middlePane.children.clear()
        uiTracker.staticUiComponents.middlePane.children.add(column)
    }

    void addListenerToAll(UiTracker uiTracker, HBox... dataUIComponents) {
        dataUIComponents.each { dataUIComponent ->
            ((Button) dataUIComponent.children.last()).setOnMouseClicked(new EventHandler<MouseEvent>() {
                @Override
                void handle(MouseEvent event) {
                    final File sshKey = FileChooser.newInstance().showOpenDialog()
                    uiTracker.serverData["${dataUIComponent.id}"]["sshKey"] = sshKey
                }
            })
        }
    }

    void loadBlockDevice(UiTracker uiTracker) {
        uiTracker.staticUiComponents.middlePane.children.clear()

        TabPane tabPane = new TabPane()

        // Get the Servers
        Set storageServersData = uiTracker.serverData.keySet().findAll { it =~ "Storage" || it =~ "allInOne" }
        storageServersData.each {
            // For each Server
            Map serverDataMap = uiTracker.serverData["$it"] as Map
            /*HBox uiComponent = (HBox) serverDataMap.uiComponent*/
            RemoteUser remoteUser = serverDataMap.remoteUser
            RemoteHost remoteHost = serverDataMap.remoteHost
            String ipAddress = remoteHost.ipAddress
            File sshKey = remoteUser.sshKey

            // Create Tab for node
            Tab tab = new Tab("$ipAddress")

            // Create General Container with [AnchorPane,FlowPane] for tab content
            AnchorPane anchorPane = new AnchorPane()
            FlowPane flowPane = new FlowPane()
            anchorPane.children.add(flowPane)
            AnchorPane.setLeftAnchor(tabPane, 0.0)
            AnchorPane.setRightAnchor(tabPane, 0.0)
            AnchorPane.setTopAnchor(tabPane, 0.0)
            AnchorPane.setBottomAnchor(tabPane, 0.0)
            tab.content = anchorPane

            AnchorPane.setLeftAnchor(flowPane, 0.0)
            AnchorPane.setRightAnchor(flowPane, 0.0)
            AnchorPane.setTopAnchor(flowPane, 0.0)
            AnchorPane.setBottomAnchor(flowPane, 0.0)

            // BlockDevice list
            List<BlockDevice> devices = []

            // new Thread for retrieve the devices
            Thread newThread = Thread.start {
                TaskHelper taskHelper = new TaskHelper()
                devices = taskHelper.getBlocksFromRemoteHost(remoteHost, remoteUser)
            }

            while (newThread.state != Thread.State.TERMINATED) {
            }

            devices.each { blockDevice ->

                // Create and fill Device Button
                AnchorPane wrapper = uiTracker.loadDiskUIComponent()
                HBox hwrapper = wrapper.children.first()
                VBox vwrapper = hwrapper.children.last()

                String deviceNameString = blockDevice.name.replace("\"", "")
                hwrapper.properties.deviceName = deviceNameString

                ((Label) vwrapper.children.get(0)).text = "Name: $deviceNameString"
                ((Label) vwrapper.children.get(1)).text = "Size: $blockDevice.size $blockDevice.storageUnit".replace("\"", "")
                ((Label) vwrapper.children.get(2)).text = "Mp: $blockDevice.mountpoint".replace("\"", "")
                ((Label) vwrapper.children.get(3)).text = "B.T.: $blockDevice.type".replace("\"", "")

                hwrapper.setOnMouseClicked(new EventHandler<MouseEvent>() {
                    @Override
                    void handle(MouseEvent event) {
                        String style = "-fx-background-color: #008CBA;"
                        String textStyle = "-fx-text-fill: white;"
                        if (!serverDataMap.devicesSelected) {
                            serverDataMap.devicesSelected = new HashSet<String>()
                        }

                        String deviceName = hwrapper.properties.deviceName
                        if (hwrapper.style =~ style) {
                            hwrapper.style = ""
                            hwrapper.children.last().children*.style = ""

                            // Remove device from devices to use
                            (serverDataMap.devicesSelected as HashSet).remove(deviceName)
                        } else {
                            hwrapper.style = style
                            hwrapper.children.last().children*.style = textStyle

                            // Add device to devices to use
                            serverDataMap.devicesSelected << deviceName
                        }
                    }
                })

                flowPane.children.add(wrapper)
            }
            tabPane.tabs.add(tab)
        }
        uiTracker.staticUiComponents.middlePane.children.add(tabPane)
    }
}
