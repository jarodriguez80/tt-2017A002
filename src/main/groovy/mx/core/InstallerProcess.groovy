package mx.core

import mx.retriever.RemoteHost
import mx.retriever.RemoteUser
import org.hidetake.groovy.ssh.Ssh
import org.hidetake.groovy.ssh.core.Remote
import org.hidetake.groovy.ssh.core.Service
import org.hidetake.groovy.ssh.session.BadExitStatusException

class InstallerProcess {

    ProcessType processType = ProcessType.UNDEFINED
    Boolean excludeInstallationProcessKeystone = false
    Boolean excludeConfigurationProcessKeystone = false
    Boolean excludeInitializationProcessKeystone = false
    Boolean excludeInstallationProcessSwift = false
    Boolean excludeConfigurationProcessSwift = false
    Boolean excludeInitializationProcessSwift = false

    /**
     * @param authenticationNode The data related to the authentication node. and has next structure.
     * [
     *      uiComponnent: ... , sshKey: ... , remoteUser: ... , remoteHost: ... , devicesSelected: ...
     * ]
     * @param storageNodes is a Map of Maps each key names the storage nodes availables.
     * */
    void installOpenStackSwift(Map authenticationNode, Map centralStorageNode, List<Map> storageNodes) {
        if (isUndefinedProcessType())
            throw new Exception("Process type on Installer Process can't be undefined.")

        installKeystone(authenticationNode)

        if (isDividedProcessType()) {
            // Copy keys to centralNode
            List<Map> serversRunningSwiftProxy = storageNodes + authenticationNode
            copyKeysToServer(serversRunningSwiftProxy, centralStorageNode)
        }

        installSwiftIntoCentralNode(centralStorageNode, storageNodes, authenticationNode)

        /**(storageNodes + centralStorageNode).each { Map storageNode ->
         installSwift(storageNode, storageNodes, [])}*/

        if (this.isDividedProcessType()) {
            Remote authenticationRemote = buildRemoteFrom(authenticationNode.remoteUser as RemoteUser, authenticationNode.remoteHost as RemoteHost)
            startSwiftProxyServiceOnNode(authenticationRemote)
        }
    }

    /**
     * Start the Swift Proxy service installed on authentication server.
     *
     * @param remote The remote in which Swift Proxy service it's installed.
     *
     * */
    void startSwiftProxyServiceOnNode(Remote remote) {
        Service service = Ssh.newService()
        service.runInOrder {
            session(remote) {
                execute("service swift-proxy start")
            }
        }
    }

    /**
     * Install Swift Project for Object Storage into Central Storage node.
     *
     * @param centralStorageNode The credential about Central Storage node.
     * @param storageNodes Other Storage nodes that runs Swift Proxy Service.
     * @param authenticationNode Authentication node that runs Swift Proxy Service.
     * */
    def installSwiftIntoCentralNode(Map centralStorageNode, List<Map> storageNodes = [], Map authenticationNode) {
        Remote storageRemote = buildRemoteFrom(centralStorageNode.remoteUser as RemoteUser, centralStorageNode.remoteHost as RemoteHost)

        List replacementsForStorage = replacementsOfConfigurationsForStorage(storageRemote.host)
        CommandInitializator commandInitializator = new CommandInitializator(
                excludeInstallationCommands: this.excludeInstallationProcessSwift,
                excludeConfigurationCommands: this.excludeConfigurationProcessSwift,
                excludeInitializationCommands: this.excludeInitializationProcessSwift
        )

        commandInitializator.initializeMandatoryCommandsForStorage(replacementsForStorage, centralStorageNode)
        List mandatoryCommands = commandInitializator.getMandatoryCommandsForStorage()

        // Get commands to be executed by the central node like ring creation and additions, and secure copy.

        if (isAllInOneProcessType()) {
            commandInitializator.initializeCommandsForCentralStorage(storageNodes + centralStorageNode, [])
        } else if (isDividedProcessType()) {
            commandInitializator.initializeCommandsForCentralStorage(storageNodes + centralStorageNode, storageNodes + authenticationNode)
        }

        List centralNodeCommands = commandInitializator.getCommandsForCentralStorage()

        // TODO Refactor this when the installation try to be used on more that two nodes. Due to te finish installation require that centralStorageNode has files generated on central node (of storage). I suggeste get this stpe out of this method and execute after installSwiftProxy in another method called finish installation on storage nodes.

        commandInitializator.initializeCommandsForFinishInstallationOnStorage()
        List commandsForStartStorageServiceOnNode = commandInitializator.getCommandsForFinishStorageInstallation()

        List commands = mandatoryCommands + centralNodeCommands + commandsForStartStorageServiceOnNode


        ProgressProperty progressPropertyForCentralStorageNode = centralStorageNode.progress as ProgressProperty
        if (isAllInOneProcessType()) {
            progressPropertyForCentralStorageNode.stepsExecuted = 0
        }
        progressPropertyForCentralStorageNode.totalSteps = commands.size() as Double
        executeCommandsIntoRemote(storageRemote, commandInitializator, commands, progressPropertyForCentralStorageNode)
    }

    /**
     * Install Keystone Project for Authentication node
     * .
     * @param authenticationNode The data for connect to Authentication node.
     * */
    void installKeystone(Map authenticationNode) {
        Remote remote = buildRemoteFrom(authenticationNode.remoteUser as RemoteUser, authenticationNode.remoteHost as RemoteHost)
        List replacementsForAuthentication = getConfigurationForAuthentication(remote.host)
        CommandInitializator commandInitializator = new CommandInitializator(
                excludeInstallationCommands: this.excludeInstallationProcessKeystone,
                excludeConfigurationCommands: this.excludeConfigurationProcessKeystone,
                excludeInitializationCommands: this.excludeInitializationProcessKeystone
        )
        commandInitializator.initializeCommandsForAuthentication(replacementsForAuthentication)
        List commands = commandInitializator.buildCommandsForAuthentication()


        ProgressProperty progressPropertyForAuthenticationNode = authenticationNode.progress as ProgressProperty
        progressPropertyForAuthenticationNode.totalSteps = commands.size() as Double
        executeCommandsIntoRemote(remote, commandInitializator, commands, progressPropertyForAuthenticationNode)
    }

    /**
     * Execute a set of commands into a server, that's indicated by the remote object.
     *
     * @param remote The server where the commands will be executed..
     * @param commandInitializator
     * @Paran commands The commandds to be executed.
     * @Paran progress The execution progrss for the commands.
     * */
    private executeCommandsIntoRemote(Remote remote, CommandInitializator commandInitializator, List<String> commands, ProgressProperty progress) {
        Service authenticationService = Ssh.newService()
        authenticationService.runInOrder {
            session(remote) {
                // Copy files and set execution rights
                put from: new File("src/resources/${commandInitializator.installerDirectory}"), into: "/"
                execute "chmod 700 ${commandInitializator.authenticationScriptsDirectory}/*.sh ${commandInitializator.installerDirectory}/Scripts/*.sh ${commandInitializator.authenticationScriptsDirectory}/Keystone/*.sh"
                execute "chmod 700 ${commandInitializator.storageScriptsDirectory}/*.sh ${commandInitializator.installerDirectory}/Scripts/*.sh"
                execute "${commandInitializator.installerDirectory}/Scripts/createDirectories.sh"

                commands.each {
                    String output = ""
                    try {
                        println it
                        output = execute "$it"
                        progress.addStepExecuted()

                    } catch (BadExitStatusException badExitStatusException) {
                        println "Exception to execute: ${it}"
                        println badExitStatusException.message
                        println badExitStatusException.exitStatus
                        println output
                    }
                }
            }
        }
    }

    /**
     * Buil a Remote object for use by Groovy SSH.
     * @param remoteUser An RemoteUser object that contains the data about a user.
     * @param remoteHost An RemoteHost object that contains the data about a host.
     * @retun An instance of Remote class builded from params.
     * */
    Remote buildRemoteFrom(RemoteUser remoteUser, RemoteHost remoteHost) {
        Remote remote = new Remote(host: remoteHost.ipAddress, user: "root", port: 22, identity: remoteUser.sshKey)
        remote
    }

    /**
     * Build the data that gonna be used for create commands that make replacement over configuration files.
     *
     * @param ipAddressForStorageServer The ip address of the server that will be used for run Swift.
     */
    private List replacementsOfConfigurationsForStorage(String ipAddressForStorageServer) {
        List replacementsOfConfigurationsForStorage = [
                [pattern: "ipAddressForStorageNode?", replacement: ipAddressForStorageServer, filesToBeReplaced: ["account-server.conf", "container-server.conf", "object-server.conf", "rsyncd.conf"], fileType: "storage-conf"],
                [pattern: "portForSwiftAccountRequests?", replacement: "6002", filesToBeReplaced: ["account-server.conf"], fileType: "storage-conf"],
                [pattern: "portForSwiftContainerRequests?", replacement: "6001", filesToBeReplaced: ["container-server.conf"], fileType: "storage-conf"],
                [pattern: "portForSwiftObjectRequests?", replacement: "6000", filesToBeReplaced: ["object-server.conf"], fileType: "storage-conf"],
        ]
        replacementsOfConfigurationsForStorage
    }

    /**
     *  Build the data that gonna be used to create commands of replacement over configuration files.
     *
     * @param ipAddressForAuthenticationServer The ip address of the server that will be used for run Keystone.
     * */
    private List getConfigurationForAuthentication(String ipAddressForAuthenticationServer) {
        def configurationForAuthentication = [
                [pattern: "ipAddressForAuthenticationNode?", replacement: ipAddressForAuthenticationServer, filesToBeReplaced: ["proxy-server.conf", "keystone.conf", "memcached.conf"], fileType: "authentication-conf"],
                [pattern: "portForAdminRequests?", replacement: "35357", filesToBeReplaced: ["proxy-server.conf", "wsgi-keystone.conf"], fileType: "authentication-conf"],
                [pattern: "portForPublicRequests?", replacement: "5000", filesToBeReplaced: ["proxy-server.conf", "wsgi-keystone.conf"], fileType: "authentication-conf"],
                [pattern: "ipAddressForMysqlServer?", replacement: ipAddressForAuthenticationServer, filesToBeReplaced: ["openstack.cnf"], fileType: "authentication-conf"],

                [pattern: "portForSwiftRequests?", replacement: "8080", filesToBeReplaced: ["Keystone/*.sh", "initializations.sh"], fileType: "authentication-script"],
                [pattern: "portForAdminRequests?", replacement: "35357", filesToBeReplaced: ["Keystone/*.sh", "initializations.sh", "createIdentityElements.sh"], fileType: "authentication-script"],
                [pattern: "portForPublicRequests?", replacement: "5000", filesToBeReplaced: ["Keystone/*.sh", "initializations.sh", "createIdentityElements.sh"], fileType: "authentication-script"],
                [pattern: "ipAddressForAuthenticationNode?", replacement: ipAddressForAuthenticationServer, filesToBeReplaced: ["Keystone/*.sh", "initializations.sh", "createIdentityElements.sh"], fileType: "authentication-script"],

                [pattern: "ipAddressForMysqlServer?", replacement: ipAddressForAuthenticationServer, filesToBeReplaced: ["keystoneDatabase.sql"], fileType: "authentication-sql"],
        ]
        configurationForAuthentication
    }

    /**
     * Copy keys of nodes which runs Swift Proxy service.
     *
     * @param servers Server that runs Swift Proxy service.
     * @param destination Server where the keys will be copied.
     * */
    void copyKeysToServer(List<Map> servers, Map destination) {
        Service sshService = Ssh.newService()
        sshService.runInOrder {
            session(buildRemoteFrom(destination.remoteUser as RemoteUser, destination.remoteHost as RemoteHost)) {
                // Copy keys for Controller Node
                execute "mkdir -p /installer_files/.secretKeys"
                for (Map serverMap : servers) {
                    put from: new FileInputStream(serverMap.remoteUser.sshKey as File), into: "/installer_files/.secretKeys/${serverMap.remoteHost.ipAddress.replace(".", "@")}"
                }

                execute "chmod 400 /installer_files/.secretKeys/*"
            }
        }
    }

    /**
     * Check if the state of the installer process is for put Authentication Service and Storage Service into one server.
     *
     * @return true If the installation process, gonna to install Authentication Service and Object Storage Service in the same node.
     * */
    Boolean isAllInOneProcessType() {
        this.processType == ProcessType.ALL_IN_ONE
    }

    /**
     * Check if the state of the installer process is for put Authentication Service in one node and Storage Service on another.
     *
     * @return true If the installation process, gonna to install Authentication Service and Object Storage Service in separated nodes.
     * */
    Boolean isDividedProcessType() {
        this.processType == ProcessType.DIVIDED
    }

    /**
     * Check if the state of the installer process is not defined.
     *
     * @return true If the installation process it's not defined.
     * */
    Boolean isUndefinedProcessType() {
        this.processType == ProcessType.UNDEFINED
    }

}