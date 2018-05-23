package mx.retriever

import groovy.transform.ToString

/**
 * This class represent a Block Device, returned by the execution of lsblk command.
 *
 * */

@ToString(includeNames = true)
class BlockDevice {
    /**
     * Fields
     * name         : The name of the Block Device as sda, sdb, sdc, etc ...
     * type         : The type of the Block Device as disk or partition
     * mountpoint   : Where the Block Device is mounted.
     * fstype       : The filesystem in the Block Device
     * size         : The capacity of the Block Device
     * storageUnit  : The unity related to the Block Device's size.
     * */

    String name
    String type
    String mountpoint
    String fstype
    Double size
    String storageUnit

    @Override
    String toString() {
        """

        name: ${this.name}
        type: ${this.type}
        mountpoint: ${this.mountpoint}
        fstype: ${this.fstype}
        size: ${this.size}
        storageUnit: ${this.storageUnit}
    `   """.toString()
    }

    /**
     * Check if the mount point of device is defined, if it isn't then return a "Not Mounted" value
     * */
    String getMountpoint() {
        if (this.mountpoint == "\"\"") {
            return "N/M"
        }
    }
}
