import mu.KotlinLogging
import mu.withLoggingContext
import org.apache.flink.statefun.sdk.java.Context
import org.apache.flink.statefun.sdk.java.StatefulFunction
import org.apache.flink.statefun.sdk.java.message.Message
import java.util.concurrent.CompletableFuture

private val logger = KotlinLogging.logger {}

abstract class LoggedStatefulFunction : StatefulFunction {
    abstract fun invoke(context: Context, message: Message): CompletableFuture<Void>

    override fun apply(context: Context, message: Message): CompletableFuture<Void> {
        withLoggingContext("function_type" to context.self().type().asTypeNameString(), "function_id" to context.self().id(),
            "caller_type" to (context.caller().orElse(null)?.type()?.asTypeNameString() ?: "none"),
            "caller_id" to (context.caller().orElse(null)?.id() ?: "none"),
            "message_type" to message.valueTypeName().asTypeNameString()) {

            logger.info { "INCOMING_CALL" }

            val result = invoke(context, message)

            logger.info { "DONE" }

            return result
        }
    }
}