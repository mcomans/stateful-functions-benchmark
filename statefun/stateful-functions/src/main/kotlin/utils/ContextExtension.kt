package utils

import mu.KLogger
import mu.withLoggingContext
import org.apache.flink.statefun.sdk.java.Context
import org.apache.flink.statefun.sdk.java.message.Message

fun Context.sendLogged(message: Message, logger: KLogger): Unit {
    withLoggingContext(
        "to_function_type" to message.targetAddress().type().asTypeNameString(),
        "to_function_id" to message.targetAddress().id(),
        "message_type" to message.valueTypeName().asTypeNameString(),
        ) {
        logger.info { "SENDING_MESSAGE" }
    }
    this.send(message)
}