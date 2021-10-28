package shoppingcart

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
import types.shoppingcart.AddToCart
import types.shoppingcart.GetCart
import types.shoppingcart.GetCartResponse
import types.shoppingcart.RemoveFromCart
import java.util.concurrent.CompletableFuture

private val logger = KotlinLogging.logger {}

class ShoppingCartFn : LoggedStatefulFunction() {

    companion object {
        val TYPE = TypeName.typeNameFromString("benchmark/shopping-cart")
        val SHOPPING_CART: ValueSpec<ShoppingCart> = ValueSpec.named("cart").withCustomType(ShoppingCart.TYPE)
        val SPEC = StatefulFunctionSpec.builder(TYPE)
            .withValueSpec(SHOPPING_CART)
            .withSupplier(::ShoppingCartFn)
    }

    override fun invoke(context: Context, requestId: String, message: WrappedMessage): CompletableFuture<Void> {
        when (message) {
            is AddToCart -> handleAddToCart(context, message)
            is RemoveFromCart -> handleRemoveFromCart(context, message)
            is GetCart -> handleGetCart(context, requestId)
        }

        return context.done()
    }

    private fun handleAddToCart(context: Context, message: AddToCart) {
        logger.info {
            "Shopping cart ${
                context.self().id()
            } - Adding ${message.productId} to shopping cart"
        }

        val storage = context.storage()

        val cart = storage.get(SHOPPING_CART).orElse(ShoppingCart(HashMap()))
        val productAmountInCart = cart.contents[message.productId] ?: 0
        cart.contents[message.productId] = productAmountInCart + message.amount
        logger.info { "Shopping cart ${context.self().id()} - Cart contents: ${cart.contents}" }
        storage.set(SHOPPING_CART, cart)
    }

    private fun handleRemoveFromCart(context: Context, message: RemoveFromCart) {
        val storage = context.storage()
        val cart = storage.get(SHOPPING_CART).orElse(ShoppingCart(HashMap()))
        if (cart.contents.contains(message.productId)) {
            val productAmountInCart = cart.contents[message.productId] ?: 0
            val newAmount = productAmountInCart - 1

            if (newAmount > 0) {
                cart.contents[message.productId] = newAmount
            } else {
                cart.contents.remove(message.productId)
            }
            logger.info { "Shopping cart ${context.self().id()} - Cart contents: ${cart.contents}" }
            storage.set(SHOPPING_CART, cart)
        }
    }

    private fun handleGetCart(context: Context, requestId: String) {
        val storage = context.storage()
        val cart = storage.get(SHOPPING_CART)

        if (cart.isPresent && context.caller().isPresent) {
            val caller = context.caller().get()
            val response = MessageWrapper(requestId, GetCartResponse(
                cart.get().contents.map { entry -> GetCartResponse.CartResponseProduct(entry.key, entry.value) }))

            val responseMessage = MessageBuilder.forAddress(caller.type(), caller.id())
                .withCustomType(BenchmarkMessages.WRAPPER_MESSAGE, response).build()

            logger.info { "Shopping cart ${context.self().id()} - Sending GetCartResponse to caller with type ${caller.type()} and id ${caller.id()}"}
            context.send(responseMessage)
        }
    }

    class ShoppingCart(val contents: MutableMap<String, Int>) {
        companion object {
            val TYPE = createJsonType("shoppingcart", ShoppingCart::class)
        }
    }
}