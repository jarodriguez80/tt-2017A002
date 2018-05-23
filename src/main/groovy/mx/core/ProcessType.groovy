package mx.core

/**
 * Define the process to be realized
 *
 * UNDEFINED    :   The process has been not defined.
 * ALL_IN_ONE   :   Perform an installation over same node.
 * DIVIDED      :   Perform an installation over several nodes.
 *
 * */

enum ProcessType {
    UNDEFINED, ALL_IN_ONE, DIVIDED
}
