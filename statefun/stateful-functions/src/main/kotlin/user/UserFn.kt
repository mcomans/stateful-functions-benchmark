package user

import LoggedStatefulFunction
import createJsonType
import mu.KotlinLogging
import org.apache.flink.statefun.sdk.java.Context
import org.apache.flink.statefun.sdk.java.StatefulFunctionSpec
import org.apache.flink.statefun.sdk.java.TypeName
import org.apache.flink.statefun.sdk.java.ValueSpec
import org.apache.flink.statefun.sdk.java.message.Message
import org.apache.flink.statefun.sdk.java.message.MessageBuilder
import types.user.RetractCreditResponse
import java.lang.Integer.max
import java.util.concurrent.CompletableFuture

val logger = KotlinLogging.logger {}

class UserFn : LoggedStatefulFunction() {
    companion object {
        val TYPE = TypeName.typeNameFromString("benchmark/user")
        val USER = ValueSpec.named("user").withCustomType(User.TYPE)
        val SPEC = StatefulFunctionSpec.builder(TYPE).withValueSpec(USER).withSupplier(::UserFn).build()
    }

    override fun invoke(context: Context, message: Message): CompletableFuture<Void> {
        if (message.`is`(UserMessages.ADD_CREDIT)) {
            val addCreditMessage = message.`as`(UserMessages.ADD_CREDIT)
            logger.info { "User ${context.self().id()} - Adding ${addCreditMessage.amount} credit"}

            val storage = context.storage()
            val user = storage.get(USER).orElse(User(0))

            user.credit = user.credit + addCreditMessage.amount

            logger.info { "User ${context.self().id()} - New amount of credit: ${user.credit}"}

            storage.set(USER, user)
            return context.done()
        }

        if (message.`is`(UserMessages.RETRACT_CREDIT)) {
            val retractCreditMessage = message.`as`(UserMessages.RETRACT_CREDIT)

            logger.info { "User ${context.self().id()} - Retracting ${retractCreditMessage.amount} credit"}
            val storage = context.storage()
            val user = storage.get(USER).orElse(User(0))

            var success = false
            if (user.credit - retractCreditMessage.amount >= 0) {
                user.credit = user.credit - retractCreditMessage.amount
                success = true
            }

            logger.info { "User ${context.self().id()} - New amount of credit: ${user.credit}"}

            storage.set(USER, user)

            if (context.caller().isPresent) {
                val caller = context.caller().get()
                val response = MessageBuilder
                    .forAddress(caller.type(), caller.id())
                    .withCustomType(UserMessages.RETRACT_CREDIT_RESPONSE, RetractCreditResponse(
                        context.self().id(),
                        success
                    ))
                    .build()

                logger.info { "User ${context.self().id()} - Sending ${if (success) "successful" else "failed"} response to caller ${caller.type().asTypeNameString()}/${caller.id()}" }
                context.send(response)
            }

            return context.done()
        }



        return context.done()
    }

    class User(var credit: Int) {
        companion object {
            val TYPE = createJsonType("user", User::class)
        }
    }
}