package user

import com.google.protobuf.Empty
import io.cloudstate.javasupport.eventsourced.CommandContext
import io.cloudstate.kotlinsupport.annotations.EntityId
import io.cloudstate.kotlinsupport.annotations.eventsourced.*
import mu.KotlinLogging
import mu.withLoggingContext
import user.persistence.Domain

private val logger = KotlinLogging.logger {}

@EventSourcedEntity
class UserEntity(@EntityId private val entityId: String) {
    private var credits: Int = 0;

    @Snapshot
    fun snapshot(): Domain.User = Domain.User.newBuilder().setCredits(credits).build()

    @SnapshotHandler
    fun snapshotHandler(user: Domain.User) {
        credits = user.credits
    }

    @EventHandler
    fun creditsChanged(creditsChanged: Domain.CreditsChanged) {
        credits = creditsChanged.credits
    }

    @CommandHandler
    fun retractCredits(retractCreditsMessage: User.RetractCreditsMessage, ctx: CommandContext): User.RetractCreditsResponse {
        withLoggingContext(
            "requestId" to retractCreditsMessage.requestId,
            "function" to "retractCredits",
            "entityType" to "user",
            "entityId" to entityId,
        ) {
            val newCredits = credits - retractCreditsMessage.amount;
            logger.debug {"Retracting ${retractCreditsMessage.amount} credits. New credits: $newCredits"}
            if (newCredits >= 0) {
                ctx.emit(Domain.CreditsChanged.newBuilder().setCredits(newCredits).build())
                return User.RetractCreditsResponse.newBuilder().setSuccess(true).build()
            }
            logger.debug { "Not enough credits" }
            return User.RetractCreditsResponse.newBuilder().setSuccess(false).build()
        }
    }

    @CommandHandler
    fun addCredits(addCreditsMessage: User.AddCreditsMessage, ctx: CommandContext): Empty {
        withLoggingContext(
            "requestId" to addCreditsMessage.requestId,
            "function" to "addCredits",
            "entityType" to "user",
            "entityId" to entityId,
        ) {
            val newCredits = credits + addCreditsMessage.amount;
            logger.debug { "Adding ${addCreditsMessage.amount} credits. New credits: $newCredits" }
            ctx.emit(Domain.CreditsChanged.newBuilder().setCredits(newCredits).build())
            return Empty.getDefaultInstance()
        }
    }

}