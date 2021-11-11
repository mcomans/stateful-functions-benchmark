package order

import com.google.common.util.concurrent.ListenableFuture
import com.google.protobuf.Empty
import io.cloudstate.javasupport.eventsourced.CommandContext
import io.cloudstate.kotlinsupport.annotations.EntityId
import io.cloudstate.kotlinsupport.annotations.eventsourced.*
import io.grpc.ManagedChannelBuilder
import order.persistence.Domain
import product.Product
import product.ProductServiceGrpc
import shoppingcart.ShoppingCartServiceGrpc
import shoppingcart.Shoppingcart
import user.User
import user.UserServiceGrpc

@EventSourcedEntity
class OrderEntity(@EntityId private val entityId: String) {
    private var status = "CREATED"

    private val asyncCartStub = ShoppingCartServiceGrpc.newFutureStub(
        ManagedChannelBuilder.forAddress("shoppingcart-service", 80).usePlaintext().build()
    )

    private val asyncProductStub = ProductServiceGrpc.newFutureStub(
        ManagedChannelBuilder.forAddress("product-service", 80).usePlaintext().build()
    )

    private val asyncUserStub = UserServiceGrpc.newFutureStub(
        ManagedChannelBuilder.forAddress("user-service", 80).usePlaintext().build()
    )

    @Snapshot
    fun snapshot(): Domain.Order = Domain.Order.newBuilder().setStatus(status).build()

    @SnapshotHandler
    fun snapshotHandler(order: Domain.Order) {
        status = order.status
    }

    @EventHandler
    fun statusChanged(statusChanged: Domain.StatusChanged) {
        status = statusChanged.status
    }

    @CommandHandler
    fun checkout(checkoutMessage: Order.CheckoutMessage, ctx: CommandContext): Empty {
        println("Order $entityId - Checkout started")
        val contents = asyncCartStub.getCartContents(
            Shoppingcart.GetCartContentsMessage.newBuilder().setCartId(checkoutMessage.cartId).build()
        ).get()

        println("Order $entityId - Cart contents received: ${contents.productsList}")

        ctx.emit(Domain.StatusChanged.newBuilder().setStatus("CART_CONTENTS_RECEIVED").build())

        val retractStockCalls = contents.productsList.map {
            RetractStockFuture(asyncProductStub.retractStock(
                Product.RetractStockMessage.newBuilder().setProductId(it.productId).setAmount(it.amount).build()
            ), it.productId, it.amount)
        }

        val retractStockResponses = retractStockCalls.map {
            val response = it.future.get()
            RetractStockCompleted(response.success, response.price, it.productId, it.amount)
        }

        if (retractStockResponses.any { !it.success }) {
            println("Order $entityId - One or more products did not have enough stock. Adding stock to the other products to roll back")

            rollback(retractStockResponses)

            ctx.emit(Domain.StatusChanged.newBuilder().setStatus("FAILED_NOT_ENOUGH_STOCK").build())
            return Empty.getDefaultInstance()
        }

        println("Order $entityId - All stock retracted")
        ctx.emit(Domain.StatusChanged.newBuilder().setStatus("STOCK_RETRACTED").build())

        val totalPrice = retractStockResponses.fold(0) { acc, completed -> acc + completed.amount * completed.price }

        val retractCreditsResponse = asyncUserStub.retractCredits(
            User.RetractCreditsMessage.newBuilder().setAmount(totalPrice).build()
        ).get()

        if (!retractCreditsResponse.success) {
            println("Order $entityId - User did not have enough credit. Adding stock to the products to roll back")

            rollback(retractStockResponses)

            ctx.emit(Domain.StatusChanged.newBuilder().setStatus("FAILED_NOT_ENOUGH_CREDIT").build())
            return Empty.getDefaultInstance()
        }

        println("Order $entityId - Credits retracted, checkout complete")
        ctx.emit(Domain.StatusChanged.newBuilder().setStatus("COMPLETE").build())

        return Empty.getDefaultInstance()
    }

    private fun rollback(retractStockResponses: Iterable<RetractStockCompleted>) {
        val futures = retractStockResponses.filter { it.success }.map { asyncProductStub.addStock(
            Product.AddStockMessage.newBuilder().setProductId(it.productId).setAmount(it.amount).build()
        ) }

        futures.map { it.get() }
    }

    private data class RetractStockFuture(val future: ListenableFuture<Product.RetractStockResponse>, val productId: String, val amount: Int)
    private data class RetractStockCompleted(val success: Boolean, val price: Int, val productId: String, val amount: Int)
}