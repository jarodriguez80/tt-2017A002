package mx.core

class CommandInitializator {
    Boolean excludeConfigurationCommands = false
    Boolean excludeInitializationCommands = false
    Boolean excludeInstallationCommands = false

    String installerDirectory = "/installer_files"
    String authenticationConfDirectory = "$installerDirectory/Configuration/Authentication"
    String authenticationScriptsDirectory = "$installerDirectory/Scripts/Authentication"
    String storageConfDirectory = "$installerDirectory/Configuration/Storage"
    String storageScriptsDirectory = "$installerDirectory/Scripts/Storage"

    List installationCommands = []
    List replacementCommands = []
    List copyCommands = []
    List setupKeystoneDatabaseCommands = []
    List populateKeystoneDatabaseCommands = []

    String restartDatabaseServiceCommand = ""
    String configureWSGICommand = ""
    String restartHTTPServerCommand = ""
    String restartMemcachedServiceCommand = ""
    String notificateToTelegramBotCommand = ""

    // Variables for storage commands

    List installationCommandsForStorage = []
    List replacementCommandsForStorage = []
    List prepareDeviceCommands = []
    List copyCommandsForStorage = []
    String configureRsynForStartInDaemonModeCommand
    String changeOwnerPrivilegesCommand
    String swifInitStartCommands
    String restartSwiftProxyServiceCommand
    String notificateToTelegramBotAboutStorageCommand
    List ringSetupCommands = []
    List secureCopyCommands = []

    void initializeCommandsForAuthentication(List<Map> configurationForAuthentication) {

        // Add repositories
        installationCommands << "bash -c \"$installerDirectory/Scripts/updateRepositoresAndInstallOpenStackClient.sh\""
        // Install
        installationCommands << "bash -c \"$authenticationScriptsDirectory/installMySQL.sh\""
        installationCommands << "bash -c \"$authenticationScriptsDirectory/installMemcached.sh\""
        installationCommands << "bash -c \"$authenticationScriptsDirectory/installKeystonePackages.sh\""
        installationCommands << "bash -c \"$authenticationScriptsDirectory/installComponentsForSwiftProxy.sh\""

        // Make replacements on configurations
        createReplacementCommandsForAuthentication(configurationForAuthentication)

        copyCommands << "cp $authenticationConfDirectory/openstack.cnf /etc/mysql/conf.d/"
        copyCommands << "cp $authenticationConfDirectory/keystone.conf /etc/keystone/"
        copyCommands << "cp $authenticationConfDirectory/proxy-server.conf /etc/swift/"
        copyCommands << "cp $authenticationConfDirectory/wsgi-keystone.conf /etc/apache2/sites-available/wsgi-keystone.conf"
        copyCommands << "cp $authenticationConfDirectory/memcached.conf /etc/"
        restartDatabaseServiceCommand = "service mysql restart"



        setupKeystoneDatabaseCommands << "mysql < $installerDirectory/keystoneDatabase.sql -uroot -proot"
        setupKeystoneDatabaseCommands << "$authenticationScriptsDirectory/populateDatabase.sh"
        setupKeystoneDatabaseCommands << "$authenticationScriptsDirectory/initializeFernetKeys.sh"

        configureWSGICommand = "$authenticationScriptsDirectory/linkSitesEnabledToKeystone.sh"

        restartHTTPServerCommand = "service apache2 restart"

        populateKeystoneDatabaseCommands << "$authenticationScriptsDirectory/Keystone/createServices_1.sh"
        populateKeystoneDatabaseCommands << "$authenticationScriptsDirectory/Keystone/createEndpointsForIdentityService.sh"
        populateKeystoneDatabaseCommands << "$authenticationScriptsDirectory/Keystone/createEndpointsForObjectStorageService.sh"
        populateKeystoneDatabaseCommands << "$authenticationScriptsDirectory/Keystone/createDefaultDomain_1.sh"
        populateKeystoneDatabaseCommands << "$authenticationScriptsDirectory/Keystone/createProjects_2.sh"
        populateKeystoneDatabaseCommands << "$authenticationScriptsDirectory/Keystone/createRoles_3.sh"
        populateKeystoneDatabaseCommands << "$authenticationScriptsDirectory/Keystone/createUsers_4.sh"
        populateKeystoneDatabaseCommands << "$authenticationScriptsDirectory/Keystone/linkUsersWithProjects.sh"

        restartMemcachedServiceCommand = "service memcached restart"
        notificateToTelegramBotCommand = "$installerDirectory/Scripts/notificateToTelegram.sh ${getTelegramKey()} ${getTelegramBotId()} \"Authentication Server\""
    }

    List buildCommandsForAuthentication() {
        List commandsForAuthentication = []

        if (!excludeInstallationCommands) {
            commandsForAuthentication += installationCommands
        }
        if (!excludeConfigurationCommands) {
            commandsForAuthentication += replacementCommands
            commandsForAuthentication += copyCommands
        }
        if (!excludeInitializationCommands) {
            commandsForAuthentication += restartDatabaseServiceCommand
            commandsForAuthentication += setupKeystoneDatabaseCommands
            commandsForAuthentication += configureWSGICommand
            commandsForAuthentication += restartHTTPServerCommand
            commandsForAuthentication += populateKeystoneDatabaseCommands
            commandsForAuthentication += restartMemcachedServiceCommand
            commandsForAuthentication += notificateToTelegramBotCommand
        }

        commandsForAuthentication

    }

    void initializeMandatoryCommandsForStorage(List<Map> replacementsOfConfigurationsForStorage,
                                               Map currentStorageServer) {
        installationCommandsForStorage = []
        replacementCommandsForStorage = []
        prepareDeviceCommands = []
        copyCommandsForStorage = []
        configureRsynForStartInDaemonModeCommand = ""
        changeOwnerPrivilegesCommand = ""

        // Add repositories
        installationCommandsForStorage << "bash -c \"$installerDirectory/Scripts/updateRepositoresAndInstallOpenStackClient.sh\""
        // Install
        installationCommandsForStorage << "bash -c \"$storageScriptsDirectory/installSwiftComponents.sh\""
        installationCommandsForStorage << "bash -c \"$storageScriptsDirectory/installUtilitiesForXFSFilesystem.sh\""

        // Make replacements on configurations
        createReplacementCommandsForStorage(replacementsOfConfigurationsForStorage, replacementCommandsForStorage)

        // Copy configuration files from installer directory to final location.
        copyCommandsForStorage << "cp $storageConfDirectory/rsyncd.conf /etc"
        copyCommandsForStorage << "cp $storageConfDirectory/account-server.conf $storageConfDirectory/container-server.conf $storageConfDirectory/object-server.conf /etc/swift"
        copyCommandsForStorage << "cp $installerDirectory/Configuration/swift.conf /etc/swift"

        // Prepare devices with XFS filesystem
        builCommandsForPrepareDevices(currentStorageServer, prepareDeviceCommands)

        // Configure Rsync to start as a daemon.
        configureRsynForStartInDaemonModeCommand = "$storageScriptsDirectory/configureRsynForStartInDaemonMode.sh"

        changeOwnerPrivilegesCommand = "chown -R swift:swift /srv/node"
    }

    List getMandatoryCommandsForStorage() {
        List commands = []
        if (!excludeInstallationCommands) {
            commands += installationCommandsForStorage
        }
        if (!excludeConfigurationCommands) {
            commands += replacementCommandsForStorage
            commands += copyCommandsForStorage
            commands += configureRsynForStartInDaemonModeCommand
        }
        if (!excludeInitializationCommands) {
            commands += prepareDeviceCommands
            commands += changeOwnerPrivilegesCommand
        }
        commands
    }

    private void builCommandsForPrepareDevices(Map currentStorageServer, List prepareDeviceCommands) {
        currentStorageServer.devicesSelected.each { deviceSelected ->
            String devicePath = "/dev/$deviceSelected"
            String deviceMountpoint = "/srv/node/$deviceSelected"
            prepareDeviceCommands << "$storageScriptsDirectory/prepareDevice.sh $devicePath $deviceMountpoint"
        }
    }

    void initializeCommandsForCentralStorage(List<Map> storageNodes, List<Map> serversRunningSwiftProxy) {
        ringSetupCommands = []
        secureCopyCommands = []

        ringSetupCommands << "$storageScriptsDirectory/createRing.sh /etc/swift/account.builder 10 1 1"
        ringSetupCommands << "$storageScriptsDirectory/createRing.sh /etc/swift/container.builder 10 1 1"
        ringSetupCommands << "$storageScriptsDirectory/createRing.sh /etc/swift/object.builder 10 1 1"

        storageNodes.each { Map storageNode ->
            storageNode.devicesSelected.each { deviceSelected ->
                ringSetupCommands << "$storageScriptsDirectory/addElementToRing.sh /etc/swift/account.builder 1 1 ${storageNode.remoteHost.ipAddress} 6002 ${deviceSelected} 100"
                ringSetupCommands << "$storageScriptsDirectory/addElementToRing.sh /etc/swift/container.builder 1 1 ${storageNode.remoteHost.ipAddress} 6001 ${deviceSelected} 100"
                ringSetupCommands << "$storageScriptsDirectory/addElementToRing.sh /etc/swift/object.builder 1 1 ${storageNode.remoteHost.ipAddress} 6000 ${deviceSelected} 100"
            }
        }

        // Rebalance rings
        ringSetupCommands << "$storageScriptsDirectory/rebalanceRing.sh /etc/swift/account.builder"
        ringSetupCommands << "$storageScriptsDirectory/rebalanceRing.sh /etc/swift/container.builder"
        ringSetupCommands << "$storageScriptsDirectory/rebalanceRing.sh /etc/swift/object.builder"


        if (!serversRunningSwiftProxy.empty) {
            secureCopyCommands = buildCommandsForSecureCopy(serversRunningSwiftProxy)
        }

    }

    List buildCommandsForSecureCopy(List<Map> serverDestinations) {
        List commands = []

        serverDestinations.each { Map destination ->
            String ipAddress = destination.remoteHost.ipAddress
            String sshKeyPath = "$installerDirectory/.secretKeys/${ipAddress.replace(".", "@")}"
            commands << "scp -i ${sshKeyPath} -o StrictHostKeyChecking=no /etc/swift/*.ring.gz /etc/swift/swift.conf root@${ipAddress}:/etc/swift"
        }

        commands
    }

    List getCommandsForCentralStorage() {
        List commands = []
        if (!excludeInitializationCommands) {
            commands += ringSetupCommands
            commands += secureCopyCommands
        }
        commands
    }

    void initializeCommandsForFinishInstallationOnStorage() {

        swifInitStartCommands = "bash -c \"swift-init all start\""
        restartSwiftProxyServiceCommand = "service swift-proxy start"

        notificateToTelegramBotAboutStorageCommand = "$installerDirectory/Scripts/notificateToTelegram.sh ${getTelegramKey()} ${getTelegramBotId()} \"Storage Server\""
    }

    List getCommandsForFinishStorageInstallation() {
        List commands = []
        if (!excludeInitializationCommands) {
            commands += swifInitStartCommands
            commands += notificateToTelegramBotAboutStorageCommand
        }
        commands
    }

    private void createReplacementCommandsForAuthentication(List<Map> configurationForAuthentication) {
        configurationForAuthentication.each { replacement ->
            String files = ""
            replacement.filesToBeReplaced.each { file ->
                String path = ""
                if (replacement.fileType == "authentication-conf")
                    path = authenticationConfDirectory
                else if (replacement.fileType == "authentication-script")
                    path = authenticationScriptsDirectory
                else if (replacement.fileType == "authentication-sql")
                    path = "$installerDirectory"

                files = "$path/$file $files"
            }
            replacementCommands << "$installerDirectory/Scripts/replaceValue.sh $replacement.pattern $replacement.replacement \"$files\""
        }
    }

    private void createReplacementCommandsForStorage(List<Map> replacementsOfConfigurationsForStorage, List replacementCommandsForStorage) {
        replacementsOfConfigurationsForStorage.each { replacement ->
            String files = ""
            replacement.filesToBeReplaced.each { file ->
                files = "$storageConfDirectory/$file $files"
            }
            replacementCommandsForStorage << "$installerDirectory/Scripts/replaceValue.sh $replacement.pattern $replacement.replacement \"$files\""
        }
    }

    private String getTelegramKey() {
        new File("src/resources/telegramKey").text
    }

    private String getTelegramBotId() {
        new File("src/resources/telegramBotId").text
    }
}
