package mx.core

import org.hidetake.groovy.ssh.core.Service

class CommandExecutor {

    Service sshService


    def executeCommand(String command,def sessionParams){
        if(!sshService){
            throw new Exception("Must set a ssh service")
        }
        sshService.run {
            settings {
                pty = true
            }
            session(sessionParams) {
                execute "${command}"

            }
        }
    }

}
