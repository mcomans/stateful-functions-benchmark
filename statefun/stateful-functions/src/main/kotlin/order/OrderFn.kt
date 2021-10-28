package order

import LoggedStatefulFunction
import createJsonType
import messages.BenchmarkMessages
import mu.KotlinLogging
import org.apache.flink.statefun.sdk.java.*
import org.apache.flink.statefun.sdk.java.message.MessageBuilder
import product.ProductFn
import shoppingcart.ShoppingCartFn
import types.MessageWrapper
import types.WrappedMessage
import types.order.Checkout
import types.product.AddStock
import types.product.RetractStock
import types.product.RetractStockResponse
import types.shoppingcart.GetCart
import types.shoppingcart.GetCartResponse
import types.user.RetractCredit
import types.user.RetractCreditResponse
import user.UserFn
import java.util.concurrent.CompletableFuture

private val logger = KotlinLogging.logger {}

class OrderFn : LoggedStatefulFunction() {

    companion object {
        val TYPE = TypeName.typeNameFromString("benchmark/order")
        val ORDER: ValueSpec<Order> = ValueSpec.named("order").withCustomType(Order.TYPE)
        val SPEC = StatefulFunctionSpec.builder(TYPE)
            .withValueSpec(ORDER)
            .withSupplier(::OrderFn)
    }

    override fun invoke(context: Context, requestId: String, message: WrappedMessage): CompletableFuture<Void> {
        when(message) {
            // STEP 1: Send message to shopping cart function to receive contents of cart
            is Checkout -> handleCheckout(context, requestId, message)
            // STEP 2: Receive shopping cart contents, and send retract stock messages to product functions
            is GetCartResponse -> handleGetCartResponse(context, requestId, message)
            // STEP 3: Receive responses from product functions, check if all are received, and send retract credit message to user service
            is RetractStockResponse -> handleRetractStockResponse(context, requestId, message)
            // STEP 4: Receive retract credit response from user function, and set the checkout to completed
            is RetractCreditResponse -> handleRetractCreditResponse(context, requestId, message)
        }

        return context.done()
    }

    private fun handleCheckout(context: Context, requestId: String, message: Checkout) {
        logger.info { "Order ${context.self().id()} - Checkout starting (shopping cart: ${message.shoppingCartId}, user: ${message.userId})" }

        val getShoppingCartMessage = MessageBuilder
            .forAddress(Address(ShoppingCartFn.TYPE, message.shoppingCartId))
            .withCustomType(BenchmarkMessages.WRAPPER_MESSAGE, MessageWrapper(requestId, GetCart()))
            .build()

        logger.info { "Order ${context.self().id()} - Requesting shopping cart contents from ${message.shoppingCartId}" }
        context.send(getShoppingCartMessage)

        val storage = context.storage()

        storage.set(ORDER, Order(message.userId,"STARTED", HashMap()))
    }

    private fun handleGetCartResponse(context: Context, requestId: String, message: GetCartResponse) {
        logger.info { "Order ${context.self().id()} - Received shopping cart contents from ${context.caller().get().id()}" }
        logger.info { "Order ${context.self().id()} - Contents: ${message.contents}" }

        val storage = context.storage()
        val order = storage.get(ORDER).get()

        for (product in message.contents) {
            order.products[product.productId] = Order.OrderProduct(product.amount)

            val retractStockMessage = MessageBuilder
                .forAddress(ProductFn.TYPE, product.productId)
                .withCustomType(BenchmarkMessages.WRAPPER_MESSAGE, MessageWrapper(requestId, RetractStock(product.amount)))
                .build()

            logger.info { "Order ${context.self().id()} - Sending retract stock message to ${product.productId}" }
            context.send(retractStockMessage)
        }

        order.status = "CART_RETRIEVED"

        storage.set(ORDER, order)
    }

    /**
     *
     */
    private fun handleRetractStockResponse(context: Context, requestId: String, message: RetractStockResponse) {
        val productId = context.caller().get().id()
        logger.info { "Order ${context.self().id()} - Received retract stock response from $productId" }

        val storage = context.storage()
        val order = storage.get(ORDER).get()

        order.products[productId]!!.responseReceived = true
        order.products[productId]!!.retractSuccessful = message.success
        order.products[productId]!!.price = message.price

        // Check if all stock retracted
        if (order.products.values.all { orderProduct -> orderProduct.responseReceived }) {
            logger.info { "Order ${context.self().id()} - All responses from orderProducts received" }

            if (order.products.values.any {orderProduct -> !orderProduct.retractSuccessful}) {
                // Roll back stock changes if there was not enough stock of at least one item in cart
                logger.info { "Order ${context.self().id()} - Insufficient stock for one or more products, rolling back stock changes" }
                order.products.filter {entry -> entry.value.retractSuccessful }.forEach { entry -> rollbackStock(entry, context, requestId) }
                order.status = "FAILED"
            } else {
                // Calculate total price, and send retract credit message to user
                val totalPrice = order.products.values.fold(0) { acc, orderProduct -> acc + orderProduct.amount * orderProduct.price!! }

                val retractCreditMessage = MessageBuilder.forAddress(UserFn.TYPE, order.userId)
                    .withCustomType(BenchmarkMessages.WRAPPER_MESSAGE, MessageWrapper(requestId, RetractCredit(totalPrice)))
                    .build()

                logger.info { "Order ${context.self().id()} - Retracting total order price ($totalPrice) from user ${order.userId}" }

                context.send(retractCreditMessage)
                order.status = "STOCK_RETRACTED"
            }
        }

        storage.set(ORDER, order)
    }

    /**
     * Receive retract credit response from user function, and set the checkout to completed
     */
    private fun handleRetractCreditResponse(context: Context, requestId: String, message: RetractCreditResponse) {
        logger.info { "Order ${context.self().id()} - Received retract credit response from ${context.caller().get().id()}" }

        val storage = context.storage()
        val order = storage.get(ORDER).get()

        if (message.success) {
            logger.info { "Order ${context.self().id()} - Checkout completed" }
            order.status = "COMPLETED"
        } else {
            logger.info { "Order ${context.self().id()} - Credit insufficient, rolling back stock changes" }
            order.products.forEach { entry -> rollbackStock(entry, context, requestId) }
            order.status = "FAILED"
        }

        storage.set(ORDER, order)
    }

    private fun rollbackStock(entry: Map.Entry<String, Order.OrderProduct>, context: Context, requestId: String) {
        val addStockMessage = MessageBuilder
            .forAddress(ProductFn.TYPE, entry.key)
            .withCustomType(BenchmarkMessages.WRAPPER_MESSAGE, MessageWrapper(requestId, AddStock(entry.value.amount)))
            .build()

        logger.info { "Order ${context.self().id()} - Sending add stock message to ${entry.key}" }
        context.send(addStockMessage)
    }

    class Order(val userId: String, var status: String, val products: MutableMap<String, OrderProduct>) {
        companion object {
            val TYPE = createJsonType("order", Order::class)
        }
        class OrderProduct(val amount: Int, var responseReceived: Boolean = false, var price: Int? = null, var retractSuccessful: Boolean = false)
    }
}