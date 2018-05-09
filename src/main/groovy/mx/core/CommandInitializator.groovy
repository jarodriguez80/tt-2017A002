package mx.core

class CommandInitializator {

    String installerDirectory
    String authenticationScriptsDirectory
    String authenticationConfDirectory
    String storageScriptsDirectory
    String storageConfDirectory


    def commandsForAuthentication = []
    List installationCommands = []
    List replacementCommands = []
    List copyCommands = []
    List setupKeystoneDatabaseCommands = []
    List populateKeystoneDatabaseCommands = []
    List exportEnvironmentVariablesCommands = []

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
    List createRingCommands = []
    List sshCopyCommands = []

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

        // Copy openstack.cnf to /etc/mysql/conf.d/openstack.cnf
        // Copy keystone configuration to /etc/keystone/
        // Copy proxy configuration to /etc/swift/
        // Copy Apache WSGI configuration to /etc/apache2/sites-available/wsgi-keystone.conf
        // Create database for Keystone
        // Populate database
        // Initialize Fernets

        // Link apache configuration
        // Execute Initialization

        copyCommands << "cp $authenticationConfDirectory/openstack.cnf /etc/mysql/conf.d/"
        copyCommands << "cp $authenticationConfDirectory/keystone.conf /etc/keystone/"
        copyCommands << "cp $authenticationConfDirectory/proxy-server.conf /etc/swift/"
        copyCommands << "cp $authenticationConfDirectory/wsgi-keystone.conf /etc/apache2/sites-available/wsgi-keystone.conf"
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

    def initializeCommandsForStorage(List<Map> replacementsOfConfigurationsForStorage,
                                     Map currentStorageServer,
                                     List<Map> storageServers,
                                     List<Map> serversWithSwiftProxy,
                                     boolean isCentralNode,
                                     boolean thereAreManyNodesRunningSwiftProxy
    ) {
        installationCommandsForStorage = []
        replacementCommandsForStorage = []
        prepareDeviceCommands = []
        copyCommandsForStorage = []
        configureRsynForStartInDaemonModeCommand = ""
        changeOwnerPrivilegesCommand = ""
        swifInitStartCommands = ""
        notificateToTelegramBotAboutStorageCommand = ""
        createRingCommands = []
        sshCopyCommands = []

        // Add repositories
        installationCommandsForStorage << "bash -c \"$installerDirectory/Scripts/updateRepositoresAndInstallOpenStackClient.sh\""
        // Install
        installationCommandsForStorage << "bash -c \"$storageScriptsDirectory/installSwiftComponents.sh\""
        installationCommandsForStorage << "bash -c \"$storageScriptsDirectory/installUtilitiesForXFSFilesystem.sh\""

        // Make replacements on configurations
        createReplacementCommandsForStorage(replacementsOfConfigurationsForStorage, replacementCommandsForStorage)

        currentStorageServer.devicesSelected.each { deviceSelected ->
            prepareDeviceCommands << "$storageScriptsDirectory/prepareDevice.sh /dev/$deviceSelected /srv/node/$deviceSelected"
        }

        copyCommandsForStorage << "cp $storageConfDirectory/rsyncd.conf /etc"
        copyCommandsForStorage << "cp $storageConfDirectory/account-server.conf $storageConfDirectory/container-server.conf $storageConfDirectory/object-server.conf /etc/swift"
        copyCommandsForStorage << "cp $installerDirectory/Configuration/swift.conf /etc/swift"

        configureRsynForStartInDaemonModeCommand = "$storageScriptsDirectory/configureRsynForStartInDaemonMode.sh"

        if (isCentralNode) {
            currentStorageServer.devicesSelected.each { deviceSelected ->
                createRingCommands << "$storageScriptsDirectory/createRings.sh ${currentStorageServer.remoteHost.ipAddress} ${deviceSelected} 6002 6001 6000"
            }

            storageServers.each { Map otherStorageServer ->
                currentStorageServer.devicesSelected.each { deviceSelected ->
                    "$storageScriptsDirectory/createRings.sh ${otherStorageServer.remoteHost.ipAddress} $deviceSelected 6002 6001 6000"
                }
            }
        }

        changeOwnerPrivilegesCommand = "chown -R swift:swift /srv/node"


        if (thereAreManyNodesRunningSwiftProxy) {
            serversWithSwiftProxy.each { serverWithSwiftProxy ->
                sshCopyCommands << "scp -i $installerDirectory/.secretKeys/${serverWithSwiftProxy.name} -o StrictHostKeyChecking=no /etc/swift/*.ring.gz root@${serverWithSwiftProxy.ipAddress}:/etc/swift"
                sshCopyCommands << "scp -i $installerDirectory/.secretKeys/${serverWithSwiftProxy.name} -o StrictHostKeyChecking=no /etc/swift/swift.conf root@${serverWithSwiftProxy.ipAddress}:/etc/swift"
            }
        }
        swifInitStartCommands = "bash -c \"swift-init all start\""

        restartMemcachedServiceCommand = "service memcached restart"
        restartSwiftProxyServiceCommand = "service swift-proxy start"

        notificateToTelegramBotAboutStorageCommand = "$installerDirectory/Scripts/notificateToTelegram.sh ${getTelegramKey()} ${getTelegramBotId()} \"Storage Server\""
    }

    private createReplacementCommandsForAuthentication(List<Map> configurationForAuthentication) {
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

    private createReplacementCommandsForStorage(List<Map> replacementsOfConfigurationsForStorage, replacementCommandsForStorage) {
        replacementsOfConfigurationsForStorage.each { replacement ->
            String files = ""
            replacement.filesToBeReplaced.each { file ->
                files = "$storageConfDirectory/$file $files"
            }
            replacementCommandsForStorage << "$installerDirectory/Scripts/replaceValue.sh $replacement.pattern $replacement.replacement \"$files\""
        }
    }


    List buildCommandsForAuthentication() {
        List commandsForAuthentication = []

        commandsForAuthentication += installationCommands
        commandsForAuthentication += replacementCommands
        commandsForAuthentication += copyCommands
        commandsForAuthentication += restartDatabaseServiceCommand
        commandsForAuthentication += setupKeystoneDatabaseCommands
        commandsForAuthentication += configureWSGICommand
        commandsForAuthentication += restartHTTPServerCommand
        commandsForAuthentication += populateKeystoneDatabaseCommands
        commandsForAuthentication += restartMemcachedServiceCommand
        commandsForAuthentication += notificateToTelegramBotCommand

        commandsForAuthentication

    }

    List buildCommandsForStorage() {
        List commandsForStorage = []

        commandsForStorage += installationCommandsForStorage
        commandsForStorage += replacementCommandsForStorage
        commandsForStorage += prepareDeviceCommands
        commandsForStorage += copyCommandsForStorage
        commandsForStorage += configureRsynForStartInDaemonModeCommand
        commandsForStorage += createRingCommands
        commandsForStorage += changeOwnerPrivilegesCommand
        commandsForStorage += sshCopyCommands
        commandsForStorage += swifInitStartCommands
        commandsForStorage += restartMemcachedServiceCommand
        //commandsForStorage += restartSwiftProxyServiceCommand
        commandsForStorage += notificateToTelegramBotAboutStorageCommand

        commandsForStorage

    }

    String getTelegramKey() {
        new File("src/resources/telegramKey").text
    }

    String getTelegramBotId() {
        new File("src/resources/telegramBotId").text
    }
}
