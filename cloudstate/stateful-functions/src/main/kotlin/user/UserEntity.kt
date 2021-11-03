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
    fun retractCredits(retractCreditsMessage: User.RetractCreditsMessage, ctx: CommandContext): Empty {
        val newCredits = credits - retractCreditsMessage.amount;
        ctx.emit(Domain.CreditsChanged.newBuilder().setCredits(newCredits).build())
        return Empty.getDefaultInstance()
    }

    @CommandHandler
    fun addCredits(addCreditsMessage: User.AddCreditsMessage, ctx: CommandContext): Empty {
        val newCredits = credits + addCreditsMessage.amount;
        ctx.emit(Domain.CreditsChanged.newBuilder().setCredits(newCredits).build())
        return Empty.getDefaultInstance()
    }

}