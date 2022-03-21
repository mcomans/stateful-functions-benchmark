package user

import LoggedStatefulFunction
import createJsonType
import messages.BenchmarkMessages
import mu.KotlinLogging
import org.apache.flink.statefun.sdk.java.Context
import org.apache.flink.statefun.sdk.java.StatefulFunctionSpec
import org.apache.flink.statefun.sdk.java.TypeName
import org.apache.flink.statefun.sdk.java.ValueSpec
import org.apache.flink.statefun.sdk.java.message.MessageBuilder
import types.MessageWrapper
import types.WrappedMessage
import types.user.AddCredit
import types.user.RetractCredit
import types.user.RetractCreditResponse
import utils.sendLogged
import java.util.concurrent.CompletableFuture

val logger = KotlinLogging.logger {}

class UserFn : LoggedStatefulFunction() {
    companion object {
        val TYPE = TypeName.typeNameFromString("benchmark/user")
        val USER = ValueSpec.named("user").withCustomType(User.TYPE)
        val SPEC = StatefulFunctionSpec.builder(TYPE).withValueSpec(USER).withSupplier(::UserFn).build()
    }

    override fun invoke(context: Context, requestId: String, message: WrappedMessage): CompletableFuture<Void> {
        when (message) {
            is AddCredit -> handleAddCredit(context, message)
            is RetractCredit -> handleRetractCredit(context, requestId, message)
        }

        return context.done()
    }

    private fun handleAddCredit(context: Context, message: AddCredit) {
        logger.info { "User ${context.self().id()} - Adding ${message.amount} credit" }

        val storage = context.storage()
        val user = storage.get(USER).orElse(User(0))

        user.credit = user.credit + message.amount

        logger.info { "User ${context.self().id()} - New amount of credit: ${user.credit}" }

        storage.set(USER, user)
    }

    private fun handleRetractCredit(context: Context, requestId: String, message: RetractCredit) {
        logger.info { "User ${context.self().id()} - Retracting ${message.amount} credit" }
        val storage = context.storage()
        val user = storage.get(USER).orElse(User(0))

        var success = false
        if (user.credit - message.amount >= 0) {
            user.credit = user.credit - message.amount
            success = true
        }

        logger.info { "User ${context.self().id()} - New amount of credit: ${user.credit}" }

        storage.set(USER, user)

        if (context.caller().isPresent) {
            val caller = context.caller().get()
            val response = MessageBuilder
                .forAddress(caller.type(), caller.id())
                .withCustomType(
                    BenchmarkMessages.WRAPPER_MESSAGE, MessageWrapper(requestId, RetractCreditResponse(
                        success
                    ))
                )
                .build()

            logger.info {
                "User ${
                    context.self().id()
                } - Sending ${if (success) "successful" else "failed"} response to caller ${
                    caller.type().asTypeNameString()
                }/${caller.id()}"
            }
            context.sendLogged(response, logger)
        }
    }

    class User(var credit: Int) {
        companion object {
            val TYPE = createJsonType("user", User::class)
        }
    }
}