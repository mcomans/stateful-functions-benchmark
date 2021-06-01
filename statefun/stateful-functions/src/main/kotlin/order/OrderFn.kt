package order

import createJsonType
import mu.KotlinLogging
import org.apache.flink.statefun.sdk.java.*
import org.apache.flink.statefun.sdk.java.message.Message
import org.apache.flink.statefun.sdk.java.message.MessageBuilder
import product.ProductFn
import product.ProductMessages
import shoppingcart.ShoppingCartFn
import shoppingcart.ShoppingCartMessages
import types.product.RetractStock
import types.shoppingcart.GetCart
import types.user.RetractCredit
import user.UserFn
import user.UserMessages
import java.util.concurrent.CompletableFuture

private val logger = KotlinLogging.logger {}

class OrderFn : StatefulFunction {

    companion object {
        val TYPE = TypeName.typeNameFromString("benchmark/order")
        val ORDER: ValueSpec<Order> = ValueSpec.named("order").withCustomType(Order.TYPE)
        val SPEC = StatefulFunctionSpec.builder(TYPE)
            .withValueSpec(ORDER)
            .withSupplier(::OrderFn)
    }

    override fun apply(context: Context, message: Message): CompletableFuture<Void> {
        if (message.`is`(OrderMessages.CHECKOUT)) {
            val checkoutMessage = message.`as`(OrderMessages.CHECKOUT)

            logger.info { "Order ${checkoutMessage.orderId} - Checkout starting (shopping cart: ${checkoutMessage.shoppingCartId}, user: ${checkoutMessage.userId})" }

            val getShoppingCartMessage = MessageBuilder
                .forAddress(Address(ShoppingCartFn.TYPE, checkoutMessage.shoppingCartId))
                .withCustomType(ShoppingCartMessages.GET_CART, GetCart(checkoutMessage.shoppingCartId))
                .build()

            logger.info { "Order ${checkoutMessage.orderId} - Requesting shopping cart contents from ${checkoutMessage.shoppingCartId}" }
            context.send(getShoppingCartMessage)

            val storage = context.storage()

            storage.set(ORDER, Order(checkoutMessage.userId,"STARTED", HashMap<String, Order.OrderProduct>()))
            return context.done()
        }

        if (message.`is`(ShoppingCartMessages.GET_CART_RESPONSE)) {
            val getCartResponse = message.`as`(ShoppingCartMessages.GET_CART_RESPONSE)
            logger.info { "Order ${context.self().id()} - Received shopping cart contents from ${getCartResponse.cartId}" }
            logger.info { "Order ${context.self().id()} - Contents: ${getCartResponse.contents}" }

            val storage = context.storage()
            val order = storage.get(ORDER).get()

            for (product in getCartResponse.contents) {
                order.products[product.productId] = Order.OrderProduct(product.amount)

                val retractStockMessage = MessageBuilder
                    .forAddress(ProductFn.TYPE, product.productId)
                    .withCustomType(ProductMessages.RETRACT_STOCK, RetractStock(product.productId, product.amount))
                    .build()

                logger.info { "Order ${context.self().id()} - Sending retract stock message to ${product.productId}" }
                context.send(retractStockMessage)
            }

            order.status = "CART_RETRIEVED"

            storage.set(ORDER, order)

            return context.done()
        }

        if (message.`is`(ProductMessages.RETRACT_STOCK_RESPONSE)) {
            val retractStockResponse = message.`as`(ProductMessages.RETRACT_STOCK_RESPONSE)
            logger.info { "Order ${context.self().id()} - Received retract stock response from ${retractStockResponse.productId}" }

            val storage = context.storage()
            val order = storage.get(ORDER).get()

            order.products[retractStockResponse.productId]!!.stockRetracted = true
            order.products[retractStockResponse.productId]!!.price = retractStockResponse.price

            // Check if all stock retracted
            if (order.products.values.fold(true) { acc, orderProduct -> acc && orderProduct.stockRetracted }) {
                logger.info { "Order ${context.self().id()} - All stock retracted" }
                val totalPrice = order.products.values.fold(0) { acc, orderProduct -> acc + orderProduct.amount * orderProduct.price!! }

                val retractCreditMessage = MessageBuilder.forAddress(UserFn.TYPE, order.userId)
                    .withCustomType(UserMessages.RETRACT_CREDIT, RetractCredit(order.userId, totalPrice))
                    .build()

                logger.info { "Order ${context.self().id()} - Retracting total order price ($totalPrice) from user ${order.userId}" }

                context.send(retractCreditMessage)

                order.status = "STOCK_RETRACTED"
            }

            storage.set(ORDER, order);
            return context.done();
        }

        if (message.`is`(UserMessages.RETRACT_CREDIT_RESPONSE)) {
            val retractCreditResponse = message.`as`(UserMessages.RETRACT_CREDIT_RESPONSE)
            logger.info { "Order ${context.self().id()} - Received retract credit response from ${retractCreditResponse.userId}" }
            logger.info { "Order ${context.self().id()} - Checkout completed" }

            // TODO: Send receipt egress message
            val storage = context.storage()
            val order = storage.get(ORDER).get()

            order.status = "COMPLETED"

            storage.set(ORDER, order)
        }

        return context.done()
    }

    class Order(val userId: String, var status: String, val products: MutableMap<String, OrderProduct>) {
        companion object {
            val TYPE = createJsonType("order", Order::class)
        }
        class OrderProduct(val amount: Int, var stockRetracted: Boolean = false, var price: Int? = null)
    }
}