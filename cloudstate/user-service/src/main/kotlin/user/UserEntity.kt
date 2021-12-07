package user

import com.google.protobuf.Empty
import io.cloudstate.javasupport.eventsourced.CommandContext
import io.cloudstate.kotlinsupport.annotations.EntityId
import io.cloudstate.kotlinsupport.annotations.eventsourced.*
import user.persistence.Domain

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
        val newCredits = credits - retractCreditsMessage.amount;
        println("User $entityId - Retracting ${retractCreditsMessage.amount} credits. New credits: $newCredits")
        if (newCredits >= 0) {
            ctx.emit(Domain.CreditsChanged.newBuilder().setCredits(newCredits).build())
            return User.RetractCreditsResponse.newBuilder().setSuccess(true).build()
        }
        return User.RetractCreditsResponse.newBuilder().setSuccess(false).build()
    }

    @CommandHandler
    fun addCredits(addCreditsMessage: User.AddCreditsMessage, ctx: CommandContext): Empty {
        val newCredits = credits + addCreditsMessage.amount;
        println("User $entityId - Adding ${addCreditsMessage.amount} credits. New credits: $newCredits")
        ctx.emit(Domain.CreditsChanged.newBuilder().setCredits(newCredits).build())
        return Empty.getDefaultInstance()
    }

}