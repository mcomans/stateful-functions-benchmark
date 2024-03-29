package order

import com.google.common.util.concurrent.ListenableFuture
import com.google.protobuf.Empty
import io.cloudstate.javasupport.eventsourced.CommandContext
import io.cloudstate.kotlinsupport.annotations.EntityId
import io.cloudstate.kotlinsupport.annotations.eventsourced.*
import io.grpc.ManagedChannelBuilder
import mu.KotlinLogging
import mu.withLoggingContext
import order.persistence.Domain
import product.Product
import product.ProductServiceGrpc
import shoppingcart.ShoppingCartServiceGrpc
import shoppingcart.Shoppingcart
import user.User
import user.UserServiceGrpc

private val logger = KotlinLogging.logger { }

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
        withLoggingContext(
            "requestId" to checkoutMessage.requestId,
            "function" to "checkout",
            "entityType" to "order",
            "entityId" to entityId,
        ) {
            logger.debug { "Checkout started" }
            val contents = asyncCartStub.getCartContents(
                Shoppingcart.GetCartContentsMessage.newBuilder()
                    .setCartId(checkoutMessage.cartId)
                    .setRequestId(checkoutMessage.requestId).build()
            ).get()

            logger.debug { "Order $entityId - Cart contents received: ${contents.productsList}" }

            ctx.emit(Domain.StatusChanged.newBuilder().setStatus("CART_CONTENTS_RECEIVED").build())

            val retractStockCalls = contents.productsList.map {
                RetractStockFuture(asyncProductStub.retractStock(
                    Product.RetractStockMessage.newBuilder()
                        .setProductId(it.productId)
                        .setAmount(it.amount)
                        .setRequestId(checkoutMessage.requestId)
                        .build()
                ), it.productId, it.amount)
            }

            val retractStockResponses = retractStockCalls.map {
                val response = it.future.get()
                RetractStockCompleted(response.success, response.price, it.productId, it.amount)
            }

            if (retractStockResponses.any { !it.success }) {
                logger.debug { "One or more products did not have enough stock. Adding stock to the other products to roll back" }

                rollback(retractStockResponses, checkoutMessage.requestId)

                ctx.emit(Domain.StatusChanged.newBuilder().setStatus("FAILED_NOT_ENOUGH_STOCK").build())
                return Empty.getDefaultInstance()
            }

            logger.debug { "All stock retracted" }
            ctx.emit(Domain.StatusChanged.newBuilder().setStatus("STOCK_RETRACTED").build())

            val totalPrice = retractStockResponses.fold(0) { acc, completed -> acc + completed.amount * completed.price }

            val retractCreditsResponse = asyncUserStub.retractCredits(
                User.RetractCreditsMessage.newBuilder().setAmount(totalPrice).setRequestId(checkoutMessage.requestId).build()
            ).get()

            if (!retractCreditsResponse.success) {
                logger.debug { "User did not have enough credit. Adding stock to the products to roll back" }

                rollback(retractStockResponses, checkoutMessage.requestId)

                ctx.emit(Domain.StatusChanged.newBuilder().setStatus("FAILED_NOT_ENOUGH_CREDIT").build())
                return Empty.getDefaultInstance()
            }

            logger.debug { "Credits retracted, checkout complete" }
            ctx.emit(Domain.StatusChanged.newBuilder().setStatus("COMPLETE").build())

            // Send update frequent item message after checkout is completed
            val productIds = contents.productsList.map { it.productId };
            productIds.map {
                asyncProductStub.updateFrequentItems(
                    Product.UpdateFrequentItemsMessage.newBuilder().setProductId(it).addAllProducts(
                        productIds.filterNot { p -> p == it }
                    ).setRequestId(checkoutMessage.requestId).build()
                )
            }

            return Empty.getDefaultInstance()
        }
    }

    private fun rollback(retractStockResponses: Iterable<RetractStockCompleted>, requestId: String) {
        val futures = retractStockResponses.filter { it.success }.map { asyncProductStub.addStock(
            Product.AddStockMessage.newBuilder().setProductId(it.productId).setAmount(it.amount).setRequestId(requestId).build()
        ) }

        futures.map { it.get() }
    }

    private data class RetractStockFuture(val future: ListenableFuture<Product.RetractStockResponse>, val productId: String, val amount: Int)
    private data class RetractStockCompleted(val success: Boolean, val price: Int, val productId: String, val amount: Int)
}