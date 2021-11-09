package order

import com.google.common.util.concurrent.Futures
import com.google.protobuf.Empty
import io.cloudstate.kotlinsupport.annotations.EntityId
import io.cloudstate.kotlinsupport.annotations.eventsourced.CommandHandler
import io.cloudstate.kotlinsupport.annotations.eventsourced.EventSourcedEntity
import io.grpc.ManagedChannelBuilder
import product.Product
import product.ProductServiceGrpc
import shoppingcart.ShoppingCartServiceGrpc
import shoppingcart.Shoppingcart
import user.User
import user.UserServiceGrpc

@EventSourcedEntity
class OrderEntity(@EntityId private val entityId: String) {
    private val status = "CREATED"

    private val asyncCartStub = ShoppingCartServiceGrpc.newFutureStub(
        ManagedChannelBuilder.forAddress("shopping-cart", 8080).usePlaintext().build()
    )

    private val asyncProductStub = ProductServiceGrpc.newFutureStub(
        ManagedChannelBuilder.forAddress("user", 8080).usePlaintext().build()
    )

    private val asyncUserStub = UserServiceGrpc.newFutureStub(
        ManagedChannelBuilder.forAddress("user", 8080).usePlaintext().build()
    )

    @CommandHandler
    fun checkout(checkoutMessage: Order.CheckoutMessage): Empty {
        val contents = asyncCartStub.getCartContents(
            Shoppingcart.GetCartContentsMessage.newBuilder().setCartId(checkoutMessage.cartId).build()
        ).get();

        val retractStockCalls = contents.productsList.map { asyncProductStub.retractStock(
            Product.RetractStockMessage.newBuilder().setProductId(it.productId).setAmount(it.amount).build()
        )}

        val retractStockResponses = Futures.successfulAsList(retractStockCalls).get();

        if (retractStockResponses.any { !it.success }) {
            // Rollback
            print("rollback")

            return Empty.getDefaultInstance()
        }

        val retractCreditsResponse = asyncUserStub.retractCredits(
            User.RetractCreditsMessage.newBuilder().setAmount(10).build()
        )

        return Empty.getDefaultInstance()
    }
}