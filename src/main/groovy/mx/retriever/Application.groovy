package mx.retriever

import org.hidetake.groovy.ssh.Ssh
import org.hidetake.groovy.ssh.core.Service
import org.hidetake.groovy.ssh.session.BadExitStatusException

class Application {
    final static internalNetworkIpAddress = "192.168.1.77"

    static Service sshService
    static String host
    static String user
    static int port

    static List<String> commands = ["lsblk -o  name,type,mountpoint,fstype -iP", "netstat -tulpn | grep 3300"]


    static void main(String[] args) {

        String fileName = "rootKey"
        RemoteUser remoteUser = new RemoteUser(user:"root",sshKey: new File("src/resources/${fileName}"))
        RemoteHost remoteHost = new RemoteHost(ipAddress: "192.168.1.77",sshPort: 22)


        TaskHelper taskHelper = new TaskHelper()
        taskHelper.getBlocksFromRemoteHost(remoteHost,remoteUser)

    }

    static void validatePortUsed(Integer portToValidate) {
        def output = null
        sshService = Ssh.newService()
        sshService.remotes {
            customRemote {
                host = internalNetworkIpAddress
                user = "root"
                port = 22
                identity = new File("src/resources/${fileName}")
            }
        }

        sshService.run {
            settings {
                pty = true
            }
            session(sshService.remotes.customRemote) {
                try {
                    execute "netstat -tulpn | grep ${portToValidate}"
                    output = "0"
                } catch (BadExitStatusException exception) {
                    println "Exit Code: ${exception.exitStatus}"
                    println "Command: ${commands.last()}"
                    output = "${exception.exitStatus}"
                }
                validateOutput(output)
            }
        }
    }

    static void getBlocks() {
        sshService = Ssh.newService()
        sshService.remotes {
            customRemote {
                host = internalNetworkIpAddress
                user = "root"
                port = 22
                identity = new File("src/resources/${fileName}")
            }
        }

        sshService.run {
            settings {
                pty = true
            }
            session(sshService.remotes.customRemote) {
                def output = execute "lsblk -o  name,type,mountpoint,fstype -iP", pty: true
                parseLsblkOutput(output)
            }
        }
    }

    static parseLsblkOutput(String output) {
        List blocksAsMap = []
        List<String> blocks = output.toLowerCase().split("\n")
        blocks.each { pairsString ->
            def pairs = pairsString.toLowerCase().split(" ")
            def map = [:]
            pairs.each { pair ->
                def pairElements = pair.split("=")
                map.put(pairElements.first(), pairElements.last())
            }
            blocksAsMap << (map as BlockDevice)
        }

        println blocksAsMap
        println blocksAsMap.size

    }

    static validateOutput(String output) {
        boolean isPortUsed = (output as Integer) == 0
        println isPortUsed
    }
}
