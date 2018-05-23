package mx.retriever

import org.hidetake.groovy.ssh.Ssh
import org.hidetake.groovy.ssh.core.Service

import java.util.regex.Matcher
import java.util.regex.Pattern

/**
 * This helper is used for retrieve Block Devices data from a remote server.
 *
 * */

class TaskHelper {

    Service sshService
    static String host
    static String user
    static int port

    /**
     * Retrieve Block Devices from a remote host.
     *
     * @param remoteHost The server where the disk will be retrieved.
     * @param remoteUser The user need it for access.
     * @return A list with de Block Devices available.
     * */

    List<BlockDevice> getBlocksFromRemoteHost(RemoteHost remoteHost, RemoteUser remoteUser) {
        List<BlockDevice> devices = []
        sshService = Ssh.newService()
        sshService.remotes {
            customRemote {
                host = remoteHost.ipAddress
                user = remoteUser.user
                port = remoteHost.sshPort
                identity = remoteUser.sshKey
            }
        }

        sshService.run {
            settings {
                pty = true
            }
            session(sshService.remotes.customRemote) {
                def output = execute "lsblk -o  name,type,mountpoint,fstype,size -iP", pty: true
                devices = parseLsblkOutput(output)
            }
        }
        devices
    }

    /**
     NAME="sda" TYPE="disk" MOUNTPOINT="" FSTYPE="" SIZE="10G"
     NAME="sda1" TYPE="part" MOUNTPOINT="/" FSTYPE="ext4" SIZE="5.4G"
     NAME="sda2" TYPE="part" MOUNTPOINT="" FSTYPE="" SIZE="1K"
     NAME="sda5" TYPE="part" MOUNTPOINT="[SWAP]" FSTYPE="swap" SIZE="4.6G"
     NAME="sdb" TYPE="disk" MOUNTPOINT="" FSTYPE="" SIZE="10G"
     NAME="sr0" TYPE="rom" MOUNTPOINT="" FSTYPE="" SIZE="1024M"
     */

    final String NAME_FOR_HARD_DRIVES = "sd"
    final List RESTRICTED_MOUNTPOINTS = ["/"]
    /**
     * Parse the lsblk output command to a list of BlockDevice. A sample of the lsblkOutput will be:
     *
     *          NAME="sda" TYPE="disk" MOUNTPOINT="" FSTYPE="" SIZE="10G"
     *          NAME="sda1" TYPE="part" MOUNTPOINT="/" FSTYPE="ext4" SIZE="5.4G"
     *
     * @param lsblkOutput The output received from lsblk command execution.
     * @return A representation of the lsblk's output as a list of BlockDevice.
     * */

    List<BlockDevice> parseLsblkOutput(String lsblkOutput) {
        List blockDevices = []
        List<String> outputLines = lsblkOutput.split("\n")

        outputLines.each { line ->
            if (isValidOutput(line)) {
                def pairs = line.split(" ")
                def map = [:]
                pairs.each { pair ->
                    def mapElements = pair.split("=")
                    String key = mapElements.first().toLowerCase()
                    def value = mapElements.last()
                    if (key == "size") {
                        parseSize(map, value)
                    } else {
                        map << ["$key": value]
                    }
                }
                blockDevices << (map as BlockDevice)
            }
        }

        blockDevices
    }

    /**
     * Provide extra filters that lsblk command can't.
     *
     * @param line The device provided by lsblk command.
     * @return true if the line belong to a valid device.
     * */
    private isValidOutput(String line) {
        boolean isAHardrive = (line =~ "NAME=\"$NAME_FOR_HARD_DRIVES").find()
        boolean isMountedInRestrictedMountPoints = false
        boolean isFormattedWithSwapFileSystem = isMountedInRestrictedMountPoints = (line =~ "FSTYPE=\"swap\"").find()
        RESTRICTED_MOUNTPOINTS.each {
            if (!isMountedInRestrictedMountPoints) {
                isMountedInRestrictedMountPoints = (line =~ "MOUNTPOINT=\"$it\"").find()
            }
        }


        isAHardrive && !isMountedInRestrictedMountPoints && !isFormattedWithSwapFileSystem
    }

    final Pattern pattern = Pattern.compile("\"(\\d+(\\.\\d+)?)(\\w)\"")

    /**
     * Parse Block Device's size
     *
     * @param capacityProperties An empty map for be filled with the size and storage unit.
     * @param sizeString Contains the capacity of a Block Device, it's used for parse by quantity and unit storage.
     * */

    void parseSize(Map capacityProperties, String sizeString) {
        Matcher matcher = pattern.matcher(sizeString)

        if (matcher.matches()) {
            capacityProperties << ["size": matcher.group(1) as Double]
            capacityProperties << ["storageUnit": matcher.group(3)]
        }
    }
}