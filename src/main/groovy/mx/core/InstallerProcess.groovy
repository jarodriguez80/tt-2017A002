package mx.core

import mx.retriever.RemoteHost
import mx.retriever.RemoteUser
import org.hidetake.groovy.ssh.Ssh
import org.hidetake.groovy.ssh.core.Remote
import org.hidetake.groovy.ssh.core.Service
import org.hidetake.groovy.ssh.session.BadExitStatusException

class InstallerProcess {

    ProcessType processType = ProcessType.UNDEFINED

    /**
     * @Param authenticationNode is a Map with the data related to the authentication node. and has next structure.
     * [
     *      uiComponnent: ... ,
     *      sshKey: ... ,
     *      remoteUser: ... ,
     *      remoteHost: ... ,
     *      devicesSelected: ...
     * ]
     * @Param storageNodes is a Map of Maps each key names the storage nodes availables.
     * */
    void installOpenStackSwift(Map authenticationNode, Map centralStorageNode, List<Map> storageNodes) {
        if (isUndefinedProcessType())
            throw new Exception("Process type on Installer Process can't be undefined.")

        Remote authenticationRemote = buildRemoteFrom(authenticationNode.remoteUser as RemoteUser, authenticationNode.remoteHost as RemoteHost)
        installKeystone(authenticationRemote)

        if (isDividedProcessType()) {
            // Copy keys to centralNode
            List<Map> serversRunningSwiftProxy = storageNodes + authenticationNode
            copyKeysToServer(serversRunningSwiftProxy, centralStorageNode)
        }

        installSwiftIntoCentralNode(centralStorageNode, storageNodes, authenticationNode)

        /**(storageNodes + centralStorageNode).each { Map storageNode ->
            installSwift(storageNode, storageNodes, [])
        }*/

        if (this.isDividedProcessType()) {
            startSwiftProxyServiceOnNode(authenticationRemote)
        }
    }

    void startSwiftProxyServiceOnNode(Remote remote) {
        Service service = Ssh.newService()
        service.runInOrder {
            session(remote) {
                execute("service swift-proxy start")
            }
        }
    }

    def installSwiftIntoCentralNode(Map centralStorageNode, List<Map> storageNodes = [], Map authenticationNode) {
        Remote storageRemote = buildRemoteFrom(centralStorageNode.remoteUser as RemoteUser, centralStorageNode.remoteHost as RemoteHost)

        List replacementsForStorage = replacementsOfConfigurationsForStorage(storageRemote.host)
        CommandInitializator commandInitializator = new CommandInitializator()

        commandInitializator.initializeMandatoryCommandsForStorage(replacementsForStorage, centralStorageNode)
        List mandatoryCommands = commandInitializator.getMandatoryCommandsForStorage()

        // Get commands to be executed by the central node like ring creation and additions, and secure copy.

        commandInitializator.initializeCommandsForCentralStorage(storageNodes + centralStorageNode, storageNodes + authenticationNode)
        List centralNodeCommands = commandInitializator.getCommandsForCentralStorage()

        // TODO Refactor this when the installation try to be used on more that two nodes. Due to te finish installation require that centralStorageNode has files generated on central node (of storage). I suggeste get this stpe out of this method and execute after installSwiftProxy in another method called finish installation on storage nodes.

        commandInitializator.initializeCommandsForFinishStorageInstallation()
        List commandsForStartStorageServiceOnNode = commandInitializator.getCommandsForFinishStorageInstallation()

        List commands = mandatoryCommands + centralNodeCommands + commandsForStartStorageServiceOnNode

        executeCommandsIntoRemote(storageRemote, commandInitializator, commands)
    }

    /*void installSwift(Map storageNode, List<Map> extraStorageNodes, List<Map> serversWithSwiftProxy) {
        Remote storageRemote = buildRemoteFrom(storageNode.remoteUser as RemoteUser, storageNode.remoteHost as RemoteHost)

        List replacementsForStorage = replacementsOfConfigurationsForStorage(storageRemote.host)
        CommandInitializator commandInitializator = new CommandInitializator()

        commandInitializator.initializeMandatoryCommandsForStorage(replacementsForStorage, storageNode)
        List mandatoryCommands = commandInitializator.getMandatoryCommandsForStorage()

        // Get commands to be executed by the central node like ring creation and additions, and secure copy.
        List centralNodeCommands = []
        if (NodeTypes.CENTRAL_NODE_STORAGE in storageNode.types) {
            commandInitializator.initializeCommandsForCentralStorage(extraStorageNodes, serversWithSwiftProxy)
            centralNodeCommands = commandInitializator.getCommandsForCentralStorage()
        }

        // TODO Refactor this when the installation try to be used on more that two nodes. Due to te finish installation require that storageNode has files generated on central node (of storage). I suggeste get this stpe out of this method and execute after installSwiftProxy in another method called finish installation on storage nodes.
        List commandsForStartStorageServiceOnNode = []
        commandInitializator.initializeCommandsForFinishStorageInstallation()
        commandsForStartStorageServiceOnNode = commandInitializator.getCommandsForFinishStorageInstallation()

        List commands = mandatoryCommands + centralNodeCommands + commandsForStartStorageServiceOnNode



        executeCommandsIntoRemote(storageRemote, commandInitializator, commands)
    }*/

    /**
     * Install an Authentication Service based on Opestack Keystone.
     * @Param Remote An object that represent the server where Keystone gonna be installed.     *
     * */
    void installKeystone(Remote remote) {
        List replacementsForAuthentication = getConfigurationForAuthentication(remote.host)
        CommandInitializator commandInitializator = new CommandInitializator()
        commandInitializator.initializeCommandsForAuthentication(replacementsForAuthentication)
        List commands = commandInitializator.buildCommandsForAuthentication()


        executeCommandsIntoRemote(remote, commandInitializator, commands)
    }

    /**
     * Execute a set of commands into a server, that's indicated by the remote object.
     * @Param remote It's the server where Keystone gonna be installed.
     * @Pram commandInitializator An object the include the data for build commands related with replacements over configuration files.
     * */
    private executeCommandsIntoRemote(Remote remote, CommandInitializator commandInitializator, List<String> commands) {
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
                        output = execute "$it"
                    } catch (BadExitStatusException badExitStatusException) {
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
     * @Param remoteUser An RemoteUser object that contains the data about a user.
     * @Param remoteHost An RemoteHost object that contains the data about a host.
     * @retun An instance of Remote class builded from params.
     * */
    Remote buildRemoteFrom(RemoteUser remoteUser, RemoteHost remoteHost) {
        Remote remote = new Remote(host: remoteHost.ipAddress, user: "root", port: 22, identity: remoteUser.sshKey)
        remote
    }

    /**
     * Build the data that gonna be used for create commands that make replacemenet over configuration files.
     * */
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
     * @Param ipAddressForAuthenticationServer The ip address of the server that will be used for run Keystone.
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

    Boolean isAllInOneProcessType() {
        this.processType == ProcessType.ALL_IN_ONE
    }

    Boolean isDividedProcessType() {
        this.processType == ProcessType.DIVIDED
    }

    Boolean isUndefinedProcessType() {
        this.processType == ProcessType.UNDEFINED
    }

}