package mx.core

import org.hidetake.groovy.ssh.Ssh
import org.hidetake.groovy.ssh.core.Service

class SshService {
    Service ssh

    void startService() {
        ssh = Ssh.newService()
    }

    void setRemote(def remoteHost, def remotePort, def remoteUser, File remotePrivateKey) {
        ssh.remotes {
            customRemote {
                host = remoteHost
                user = remoteUser
                port = remotePort ?: 22
                identity = remotePrivateKey
            }
        }
    }
}
