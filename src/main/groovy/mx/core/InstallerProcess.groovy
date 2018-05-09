package mx.core

import javafx.beans.property.DoubleProperty
import mx.retriever.BlockDevice
import mx.retriever.RemoteHost
import mx.retriever.RemoteUser
import org.hidetake.groovy.ssh.Ssh
import org.hidetake.groovy.ssh.core.Remote
import org.hidetake.groovy.ssh.core.Service
import org.hidetake.groovy.ssh.session.BadExitStatusException

import java.util.regex.Matcher
import java.util.regex.Pattern

class InstallerProcess {

    static Service sshService
    static String host
    static String user
    static int port

    static void main(String[] args) {
        sshService = Ssh.newService()
        sshService.runInOrder {
            session(new Remote(user: "root", host: "192.168.1.77", identity: new File("/home/jorge/sshKeys/rootKey"))) {

                String output = ""
                ["service memcached restart", "service swift-proxy restart"].each {
                    try {
                        println "Command: $it"
                        output = execute "$it"
                    } catch (BadExitStatusException badExitStatusException) {
                        println "Error on the execution of: $it"
                        println "Output: ${badExitStatusException.message}"
                    }
                }

            }
        }
    }

    static void startInstallation(String ipAddress, File rootKey) {
        initializeCommands("5000", "35357", "8080", "6002", "6001", "6000", "127.0.0.1", "127.0.0.1", "127.0.0.1", "127.0.0.1")
        CommandInitializator cmd = new CommandInitializator()

        sshService = Ssh.newService()
        sshService.remotes {
            customRemote {
                host = ipAddress
                user = "root"
                port = 22
                identity = rootKey
            }
        }

        sshService.run {
            /*settings {
                pty = true
            }*/
            session(sshService.remotes.customRemote) {
                //commands.each { command ->
                cmd.initializeCommandsForAuthentication("5000", "35357", "8080", "127.0.0.1", "127.0.0.1").each { command ->
                    println "\t\t Command : $command"
                    def output = execute "${command}"
                    println "\t\t$output"
                }
                println "Installation finished ..."

                /*def output = execute "ls"
                println "\n\n\n ${output}\n\n\n"
                println "\n\n\n ${sshService}\n\n\n"*/
            }
        }
    }
    // TODO works with fixed parameters
    static void startInstallation(String ipAddressForAuthenticationServer,
                                  File rootKeyForAuthenticationServer,
                                  String ipAddressForStorageServer,
                                  File rootKeyForStorageServer, Map options) {

        Boolean makeInstallationsOnAuthentication = options.includeAuthenticationInstallations ?: false
        Boolean makeInstallationsOnStorage = options.includeStorageInstallations ?: false

        String installerDirectory = "/installer_files"
        String authenticationConfDirectory = "$installerDirectory/Configuration/Authentication"
        String authenticationScriptsDirectory = "$installerDirectory/Scripts/Authentication"

        String storageConfDirectory = "$installerDirectory/Configuration/Storage"
        String storageScriptsDirectory = "$installerDirectory/Scripts/Storage"

        def configurationFilesForStorage = ["account-server.conf", "container-server.conf", "object-server.conf", "rsyncd.conf"]

        // Configuration module
        def configurationForAuthentication = [
                [pattern: "ipAddressForAuthenticationNode?", replacement: ipAddressForAuthenticationServer, filesToBeReplaced: ["proxy-server.conf", "keystone.conf"], fileType: "authentication-conf"],
                [pattern: "portForAdminRequests?", replacement: "35357", filesToBeReplaced: ["proxy-server.conf", "wsgi-keystone.conf"], fileType: "authentication-conf"],
                [pattern: "portForPublicRequests?", replacement: "5000", filesToBeReplaced: ["proxy-server.conf", "wsgi-keystone.conf"], fileType: "authentication-conf"],
                [pattern: "ipAddressForMysqlServer?", replacement: ipAddressForAuthenticationServer, filesToBeReplaced: ["openstack.cnf"], fileType: "authentication-conf"],

                [pattern: "portForSwiftRequests?", replacement: "8080", filesToBeReplaced: ["Keystone/*.sh", "initializations.sh"], fileType: "authentication-script"],
                [pattern: "portForAdminRequests?", replacement: "35357", filesToBeReplaced: ["Keystone/*.sh", "initializations.sh", "createIdentityElements.sh"], fileType: "authentication-script"],
                [pattern: "portForPublicRequests?", replacement: "5000", filesToBeReplaced: ["Keystone/*.sh", "initializations.sh", "createIdentityElements.sh"], fileType: "authentication-script"],
                [pattern: "ipAddressForAuthenticationNode?", replacement: ipAddressForAuthenticationServer, filesToBeReplaced: ["Keystone/*.sh", "initializations.sh", "createIdentityElements.sh"], fileType: "authentication-script"],

                [pattern: "ipAddressForMysqlServer?", replacement: ipAddressForAuthenticationServer, filesToBeReplaced: ["keystoneDatabase.sql"], fileType: "authentication-sql"],
        ]

        def replacementsOfConfigurationsForStorage = [
                [pattern: "ipAddressForStorageNode?", replacement: ipAddressForStorageServer, filesToBeReplaced: ["account-server.conf", "container-server.conf", "object-server.conf", "rsyncd.conf"], fileType: "storage-conf"],
                [pattern: "portForSwiftAccountRequests?", replacement: "6002", filesToBeReplaced: ["account-server.conf"], fileType: "storage-conf"],
                [pattern: "portForSwiftContainerRequests?", replacement: "6001", filesToBeReplaced: ["container-server.conf"], fileType: "storage-conf"],
                [pattern: "portForSwiftObjectRequests?", replacement: "6000", filesToBeReplaced: ["object-server.conf"], fileType: "storage-conf"],
        ]

        sshService = Ssh.newService()
        sshService.remotes {
            authenticationRemote {
                host = ipAddressForAuthenticationServer
                user = "root"
                port = 22
                identity = rootKeyForAuthenticationServer
            }
            storageRemote {
                host = ipAddressForStorageServer
                user = "root"
                port = 22
                identity = rootKeyForStorageServer
            }
        }

        DoubleProperty storageProgressProperty = options.storageProgressProperty
        DoubleProperty authenticationProgressProperty = options.authenticationProgressProperty
        def authenticationCounter = 0
        def storageCounter = 0
        String disks = ""
        sshService.runInOrder {
            settings {
                // overrides global settings
                //pty = true
                keepAliveSec = 120
            }
            session(sshService.remotes.authenticationRemote) {
                // Copy files and set execution rights
                put from: new File("src/resources/$installerDirectory"), into: "/"
                execute "chmod 700 $authenticationScriptsDirectory/*.sh $installerDirectory/Scripts/*.sh $authenticationScriptsDirectory/Keystone/*.sh"
                execute "$installerDirectory/Scripts/createDirectories.sh"

                if (makeInstallationsOnAuthentication) {
                    // Add repositories
                    execute "bash -c \"$installerDirectory/Scripts/updateRepositoresAndInstallOpenStackClient.sh\""
                    authenticationProgressProperty.value = ++authenticationCounter / 6
                    // Install
                    execute "bash -c \"$authenticationScriptsDirectory/installMySQL.sh\""
                    authenticationProgressProperty.value = ++authenticationCounter / 6
                    execute "bash -c \"$authenticationScriptsDirectory/installMemcached.sh\""
                    authenticationProgressProperty.value = ++authenticationCounter / 6
                    execute "bash -c \"$authenticationScriptsDirectory/installKeystonePackages.sh\""
                    authenticationProgressProperty.value = ++authenticationCounter / 6
                    execute "bash -c \"$authenticationScriptsDirectory/installComponentsForSwiftProxy.sh\""
                    authenticationProgressProperty.value = ++authenticationCounter / 6
                }

                // Make replacements on configurations
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
                    execute "$installerDirectory/Scripts/replaceValue.sh $replacement.pattern $replacement.replacement \"$files\""
                }

                // Copy openstack.cnf to /etc/mysql/conf.d/openstack.cnf
                // Copy keystone configuration to /etc/keystone/
                // Copy proxy configuration to /etc/swift/
                // Copy Apache WSGI configuration to /etc/apache2/sites-available/wsgi-keystone.conf
                // Create database for Keystone
                // Populate database
                // Initialize Fernets

                // Link apache configuration
                // Execute Initialization


                execute "cp $authenticationConfDirectory/openstack.cnf /etc/mysql/conf.d/"
                execute "cp $authenticationConfDirectory/keystone.conf /etc/keystone/"
                execute "cp $authenticationConfDirectory/proxy-server.conf /etc/swift/"
                execute "service mysql restart"

                execute "mysql < $installerDirectory/keystoneDatabase.sql -uroot -proot"
                execute "$authenticationScriptsDirectory/populateDatabase.sh"
                execute "$authenticationScriptsDirectory/initializeFernetKeys.sh"

                execute "$authenticationScriptsDirectory/linkSitesEnabledToKeystone.sh"
                //execute "service keystone stop"
                execute "service apache2 restart"
                //execute "service keystone restart"

                execute "$authenticationScriptsDirectory/Keystone/createServices_1.sh"
                execute "$authenticationScriptsDirectory/Keystone/createEndpointsForIdentityService.sh"
                execute "$authenticationScriptsDirectory/Keystone/createEndpointsForObjectStorageService.sh"
                execute "$authenticationScriptsDirectory/Keystone/createDefaultDomain_1.sh"
                execute "$authenticationScriptsDirectory/Keystone/createProjects_2.sh"
                execute "$authenticationScriptsDirectory/Keystone/createRoles_3.sh"
                execute "$authenticationScriptsDirectory/Keystone/createUsers_4.sh"
                execute "$authenticationScriptsDirectory/Keystone/linkUsersWithProjects.sh"

                def initializations = ["$authenticationScriptsDirectory/Keystone/createServices_1.sh", "$authenticationScriptsDirectory/Keystone/createEndpointsForIdentityService.sh", "$authenticationScriptsDirectory/Keystone/createEndpointsForObjectStorageService.sh", "$authenticationScriptsDirectory/Keystone/createDefaultDomain_1.sh", "$authenticationScriptsDirectory/Keystone/createProjects_2.sh", "$authenticationScriptsDirectory/Keystone/createRoles_3.sh", "$authenticationScriptsDirectory/Keystone/createUsers_4.sh", "$authenticationScriptsDirectory/Keystone/linkUsersWithProjects.sh",]
                initializations.each {
                    def output
                    try {
                        output = execute it
                        println "Output: $output \n "
                    } catch (BadExitStatusException exception) {
                        println "Code: $exception.exitStatus \n Message: $exception.message"
                    }
                }

                println "Trying initialize"
                execute "chmod +x $authenticationScriptsDirectory/initializations.sh $authenticationScriptsDirectory/createIdentityElements.sh"
                def z
                try {
                    //z = execute "$authenticationScriptsDirectory/createIdentityElements.sh"
                    z = execute "$authenticationScriptsDirectory/initializations.sh && echo tokenSet=OK >> ~/out"
                    println z
                } catch (BadExitStatusException e) {
                    println z
                }
                authenticationProgressProperty.value = ++authenticationCounter / 6

                execute "export OS_TOKEN=tokenLoko"
                execute "export OS_URL=http://$ipAddressForAuthenticationServer:35357/v3"
                execute "export OS_IDENTITY_API_VERSION=3"

                execute "openstack service create --name keystone --description \"OpenStack Identity\" identity"
                execute "openstack service create --name swift --description \"OpenStack Object Storage\" object-store"
                execute "openstack endpoint create --region RegionOne identity public http://$ipAddressForAuthenticationServer:5000/v3"
                execute "openstack endpoint create --region RegionOne identity internal http://$ipAddressForAuthenticationServer:5000/v3"
                execute "openstack endpoint create --region RegionOne identity admin http://$ipAddressForAuthenticationServer:35357/v3"
                execute "openstack endpoint create --region RegionOne object-store public http://$ipAddressForAuthenticationServer:8080/v1/AUTH_%\\(tenant_id\\)s"
                execute "openstack endpoint create --region RegionOne object-store internal http://$ipAddressForAuthenticationServer:8080/v1/AUTH_%\\(tenant_id\\)s"
                execute "openstack endpoint create --region RegionOne object-store admin http://$ipAddressForAuthenticationServer:8080/v1"

                execute "openstack domain create --description \"Default Domain\" default"
                execute "openstack project create --domain default --description \"Admin Project\" admin"
                execute "openstack user create --domain default --password adminPassword admin"
                execute "openstack role create admin"
                execute "openstack role add --project admin --user admin admin"
                execute "openstack project create --domain default --description \"Service Project\" service"
                execute "openstack user create --domain default --password swiftPassword swift"

                execute "openstack role add --project service --user swift admin"
                execute "openstack role create user"
                execute "openstack project create --domain default --description \"Demo Project\" demo"
                execute "openstack user create --domain default --password demo demo"
                execute "openstack role add --project demo --user demo user"



                execute "service memcached restart"
                execute "$installerDirectory/Scripts/notificateToTelegram.sh \"Authentication Server\""
            }
            session(sshService.remotes.storageRemote) {
                // Copy keys for Controller Node
                execute "mkdir -p $installerDirectory/.secretKeys"
                put from: rootKeyForAuthenticationServer, into: "$installerDirectory/.secretKeys/${rootKeyForAuthenticationServer.name}"
                execute "chmod 400 $installerDirectory/.secretKeys/*"
                storageProgressProperty.value = ++storageCounter / 8

                // Copy files and set execution rights
                put from: new File("src/resources/$installerDirectory"), into: "/"
                execute "chmod 700 $storageScriptsDirectory/*.sh $installerDirectory/Scripts/*.sh"
                execute "$installerDirectory/Scripts/createDirectories.sh"
                storageProgressProperty.value = ++storageCounter / 8

                if (makeInstallationsOnStorage) {
                    // Add repositories
                    execute "bash -c \"$installerDirectory/Scripts/updateRepositoresAndInstallOpenStackClient.sh\""
                    storageProgressProperty.value = ++storageCounter / 8

                    // Install
                    execute "bash -c \"$storageScriptsDirectory/installSwiftComponents.sh\""
                    storageProgressProperty.value = ++storageCounter / 8
                    execute "bash -c \"$storageScriptsDirectory/installUtilitiesForXFSFilesystem.sh\""
                    storageProgressProperty.value = ++storageCounter / 8
                }

                // Make replacements on configurations
                replacementsOfConfigurationsForStorage.each { replacement ->
                    String files = ""
                    replacement.filesToBeReplaced.each { file ->
                        files = "$storageConfDirectory/$file $files"
                    }
                    execute "$installerDirectory/Scripts/replaceValue.sh $replacement.pattern $replacement.replacement \"$files\""
                }

                // Prepare block with xfs filesystem
                // Move rsync file to /etc/rsyncd.conf
                // Enable rsync
                // Move account, container and object configuration to /etc/swift
                // Create Rings
                // Move swift configuration to /etc/swift
                try {
                    execute "$storageScriptsDirectory/prepareDevice.sh /dev/sdb /srv/node/sdb"
                } catch (BadExitStatusException e) {

                }
                storageProgressProperty.value = ++storageCounter / 8
                execute "cp $storageConfDirectory/rsyncd.conf /etc"
                execute "cp $storageConfDirectory/account-server.conf $storageConfDirectory/container-server.conf $storageConfDirectory/object-server.conf /etc/swift"
                execute "cp $installerDirectory/Configuration/swift.conf /etc/swift"

                execute "$storageScriptsDirectory/configureRsynForStartInDaemonMode.sh"
                execute "$storageScriptsDirectory/createRings.sh $ipAddressForStorageServer 6002 6001 6000"
                storageProgressProperty.value = ++storageCounter / 8

                execute "chown -R swift:swift /srv/node"

                execute "scp -i $installerDirectory/.secretKeys/$rootKeyForAuthenticationServer.name -o StrictHostKeyChecking=no /etc/swift/*.ring.gz root@$ipAddressForAuthenticationServer:/etc/swift"
                execute "scp -i $installerDirectory/.secretKeys/$rootKeyForAuthenticationServer.name -o StrictHostKeyChecking=no /etc/swift/swift.conf root@$ipAddressForAuthenticationServer:/etc/swift"
                def output = execute "bash -c \"swift-init all start\""
                storageProgressProperty.value = ++storageCounter / 8
                println output
                execute "$installerDirectory/Scripts/notificateToTelegram.sh \"Storage Server\""
            }

            session(sshService.remotes.authenticationRemote) {
                execute "service memcached restart"
                execute "service swift-proxy restart"
            }

            /*session(sshService.remotes.authenticationRemote) {
                disks = execute "lsblk -e 1 -ln -o NAME,SIZE,MOUNTPOINT"
            }*/
            println "Finished"
        }


        def disksList = []
        disks.readLines()*.replaceAll("\\ +", ",").each {
            def diskValues = it.split(",")
            if (diskValues) {
                def newDisk = new BlockDevice()
                diskValues.eachWithIndex { item, index ->
                    if (index == 0)
                        newDisk.name = item
                    else if (index == 1)
                        matchSizeDisk(item, newDisk)
                    else if (index == 2)
                        newDisk.mountpoint = item
                }
                disksList << newDisk
            }
        }
        disksList.each {
            println "Wow: $it"
        }
    }


    String installerDirectory = "/installer_files"
    String authenticationConfDirectory = "$installerDirectory/Configuration/Authentication"
    String authenticationScriptsDirectory = "$installerDirectory/Scripts/Authentication"

    String storageConfDirectory = "$installerDirectory/Configuration/Storage"
    String storageScriptsDirectory = "$installerDirectory/Scripts/Storage"

    void installOpenStackSwift(Map authenticationServer, Map... storageServers) {
        RemoteHost authenticationRemoteHost = (authenticationServer.remoteHost as RemoteHost)
        RemoteHost storageRemoteHost = (storageServers.first().remoteHost as RemoteHost)
        RemoteUser authenticationRemoteUser = (authenticationServer.remoteUser as RemoteUser)
        RemoteUser storageRemoteUser = (storageServers.first().remoteUser as RemoteUser)

        Remote authenticationRemote = new Remote(host: authenticationRemoteHost.ipAddress, user: "root", port: 22, identity: authenticationRemoteUser.sshKey)
        Remote storageRemote = new Remote(host: storageRemoteHost.ipAddress, user: "root", port: 22, identity: storageRemoteUser.sshKey)

        String ipAddressForAuthenticationServer = authenticationRemoteHost.ipAddress

        CommandInitializator commandInitializator = new CommandInitializator(
                installerDirectory: installerDirectory,
                authenticationScriptsDirectory: authenticationScriptsDirectory,
                authenticationConfDirectory: authenticationConfDirectory,
                storageScriptsDirectory: storageScriptsDirectory,
                storageConfDirectory: storageConfDirectory

        )

        def configurationForAuthentication = getConfigurationForAuthentication(ipAddressForAuthenticationServer)
        commandInitializator.initializeCommandsForAuthentication(configurationForAuthentication)

        List<Map> replacementsOfConfigurationsForStorage = replacementsOfConfigurationsForStorage(storageRemoteHost.ipAddress)
        Map currentStorageServer = storageServers.first()
        List<Map> remainingStorageServers = []
        List<Map> serversWithSwiftProxy = [authenticationServer]
        boolean isCentralNode = true
        boolean thereAreManyNodesRunningSwiftProxy = false
        commandInitializator.initializeCommandsForStorage(replacementsOfConfigurationsForStorage, currentStorageServer, remainingStorageServers, serversWithSwiftProxy, isCentralNode, thereAreManyNodesRunningSwiftProxy)

        List commandsForKeystone = commandInitializator.buildCommandsForAuthentication()
        List commandsForStorage = commandInitializator.buildCommandsForStorage()

        sshService = Ssh.newService()

        sshService.runInOrder {
            settings {
                keepAliveSec = 120
            }

            session(authenticationRemote) {
                // Copy files and set execution rights
                put from: new File("src/resources/$installerDirectory"), into: "/"
                execute "chmod 700 $authenticationScriptsDirectory/*.sh $installerDirectory/Scripts/*.sh $authenticationScriptsDirectory/Keystone/*.sh"
                execute "$installerDirectory/Scripts/createDirectories.sh"

                String output = ""
                commandsForKeystone.each {
                    try {
                        println "Command: $it"
                        output = execute "$it"
                    } catch (BadExitStatusException badExitStatusException) {
                        println "Error on the execution of: $it"
                        println "Output: ${output}"
                    }
                }
            }

            session(storageRemote) {
                // Copy files and set execution rights
                put from: new File("src/resources/$installerDirectory"), into: "/"
                execute "chmod 700 $storageScriptsDirectory/*.sh $installerDirectory/Scripts/*.sh"
                execute "$installerDirectory/Scripts/createDirectories.sh"

                String output = ""
                commandsForStorage.each {
                    try {
                        println "Command: $it"
                        if (it == "service swift-proxy start")
                            output = execute "$it >> rebootSwiftProxy", pty: true
                        else
                            output = execute "$it"

                    } catch (BadExitStatusException badExitStatusException) {
                        println "Error on the execution of: $it"
                        println "Output: ${badExitStatusException.message}"
                    }
                }
            }

            /*session(sshService.remotes.authenticationRemote) {
                execute "service memcached restart"
                execute "service swift-proxy restart"
            }*/
        }
    }

    private static List replacementsOfConfigurationsForStorage(String ipAddressForStorageServer) {
        List replacementsOfConfigurationsForStorage = [
                [pattern: "ipAddressForStorageNode?", replacement: ipAddressForStorageServer, filesToBeReplaced: ["account-server.conf", "container-server.conf", "object-server.conf", "rsyncd.conf"], fileType: "storage-conf"],
                [pattern: "portForSwiftAccountRequests?", replacement: "6002", filesToBeReplaced: ["account-server.conf"], fileType: "storage-conf"],
                [pattern: "portForSwiftContainerRequests?", replacement: "6001", filesToBeReplaced: ["container-server.conf"], fileType: "storage-conf"],
                [pattern: "portForSwiftObjectRequests?", replacement: "6000", filesToBeReplaced: ["object-server.conf"], fileType: "storage-conf"],
        ]
        replacementsOfConfigurationsForStorage
    }

    private static getConfigurationForAuthentication(String ipAddressForAuthenticationServer) {
        def configurationForAuthentication = [
                [pattern: "ipAddressForAuthenticationNode?", replacement: ipAddressForAuthenticationServer, filesToBeReplaced: ["proxy-server.conf", "keystone.conf"], fileType: "authentication-conf"],
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


    static void matchSizeDisk(String output, BlockDevice blockDevice) {
        Pattern pattern = Pattern.compile("(\\d+(\\.\\d+)?)")
        Pattern patternScaler = Pattern.compile("([A-z]+)")
        Matcher matcher = pattern.matcher(output)
        Matcher matcherScaler = patternScaler.matcher(output)
        if (matcher.find() && matcherScaler.find()) {
            blockDevice.size = matcher.group(1) as Double
            blockDevice.storageUnit = matcherScaler.group(1)
        }
    }

}