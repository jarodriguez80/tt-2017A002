package mx.core
/**
 * Define the usage of a server.
 *
 * ALL_IN_ONE           :   The node will be used as central server, include all the services.
 * AUTHENTICATION       :   The node will be use for provide authentication service.
 * CENTRAL_NODE_STORAGE :   The node will be use for provide storage service and will created and distribute swift's rings.
 * STORAGE              :   The node will be use for provide storage service.
 * */
enum NodeTypes {
    ALL_IN_ONE, AUTHENTICATION, CENTRAL_NODE_STORAGE, STORAGE
}
