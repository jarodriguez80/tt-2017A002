package mx.retriever

import groovy.transform.ToString

@ToString(includeNames = true)
class BlockDevice {
    /**
     * name="sda" type="disk" mountpoint="" fstype="" size="10g"
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

    String getMountpoint() {
        if (this.mountpoint == "\"\"") {
            return "N/M"
        }
    }
}
