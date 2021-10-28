import messages.BenchmarkMessages
import mu.KotlinLogging
import mu.withLoggingContext
import org.apache.flink.statefun.sdk.java.Context
import org.apache.flink.statefun.sdk.java.StatefulFunction
import org.apache.flink.statefun.sdk.java.message.Message
import types.WrappedMessage
import java.util.concurrent.CompletableFuture

private val logger = KotlinLogging.logger {}

abstract class LoggedStatefulFunction : StatefulFunction {
    abstract fun invoke(context: Context, requestId: String, message: WrappedMessage): CompletableFuture<Void>

    override fun apply(context: Context, message: Message): CompletableFuture<Void> {
        if (message.`is`(BenchmarkMessages.WRAPPER_MESSAGE)) {
            val wrapper = message.`as`(BenchmarkMessages.WRAPPER_MESSAGE)
            withLoggingContext(
                "requestId" to wrapper.requestId,
                "function_type" to context.self().type().asTypeNameString(),
                "function_id" to context.self().id(),
                "caller_type" to (context.caller().orElse(null)?.type()?.asTypeNameString() ?: "none"),
                "caller_id" to (context.caller().orElse(null)?.id() ?: "none"),
                "message_type" to wrapper.message.javaClass.name
            ) {
                logger.info { "INCOMING_CALL" }
                val result = invoke(context, wrapper.requestId, wrapper.message)
                logger.info { "DONE" }
                return result;
            }
        }

        return context.done();
    }
}