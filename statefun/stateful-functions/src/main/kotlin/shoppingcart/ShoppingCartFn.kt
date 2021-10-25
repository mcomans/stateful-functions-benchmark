package shoppingcart

import LoggedStatefulFunction
import createJsonType
import mu.KotlinLogging
import org.apache.flink.statefun.sdk.java.*
import org.apache.flink.statefun.sdk.java.message.Message
import org.apache.flink.statefun.sdk.java.message.MessageBuilder
import types.shoppingcart.GetCartResponse
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

    override fun invoke(context: Context, message: Message): CompletableFuture<Void> {
        // Add to cart function
        if (message.`is`(ShoppingCartMessages.ADD_TO_CART)) {
            val addToCartMessage = message.`as`(ShoppingCartMessages.ADD_TO_CART)
            logger.info { "Shopping cart ${context.self().id()} - Adding ${addToCartMessage.productId} to shopping cart" }

            val storage = context.storage()

            val cart = storage.get(SHOPPING_CART).orElse(ShoppingCart(HashMap()))
            val productAmountInCart = cart.contents[addToCartMessage.productId] ?: 0
            cart.contents[addToCartMessage.productId] = productAmountInCart + addToCartMessage.amount
            logger.info { "Shopping cart ${context.self().id()} - Cart contents: ${cart.contents}" }
            storage.set(SHOPPING_CART, cart)

            return context.done()
        }

        // Remove from cart function
        if (message.`is`(ShoppingCartMessages.REMOVE_FROM_CART)) {
            val removeFromCartMessage = message.`as`(ShoppingCartMessages.REMOVE_FROM_CART)

            val storage = context.storage()
            val cart = storage.get(SHOPPING_CART).orElse(ShoppingCart(HashMap()))
            if (cart.contents.contains(removeFromCartMessage.productId)) {
                val productAmountInCart = cart.contents[removeFromCartMessage.productId] ?: 0
                val newAmount = productAmountInCart - 1

                if (newAmount > 0) {
                    cart.contents[removeFromCartMessage.productId] = newAmount
                } else {
                    cart.contents.remove(removeFromCartMessage.productId)
                }
                logger.info { "Shopping cart ${context.self().id()} - Cart contents: ${cart.contents}" }
                storage.set(SHOPPING_CART, cart)
            }

            return context.done()
        }

        if (message.`is`(ShoppingCartMessages.GET_CART)) {
            val getCartMessage = message.`as`(ShoppingCartMessages.GET_CART)

            val storage = context.storage()
            val cart = storage.get(SHOPPING_CART)

            if (cart.isPresent && context.caller().isPresent) {
                val caller = context.caller().get()
                val response = GetCartResponse(
                    cart.get().contents.map { entry -> GetCartResponse.CartResponseProduct(entry.key, entry.value) })
                val responseMessage = MessageBuilder.forAddress(caller.type(), caller.id())
                    .withCustomType(ShoppingCartMessages.GET_CART_RESPONSE, response).build()

                logger.info { "Shopping cart ${context.self().id()} - Sending GetCartResponse to caller with type ${caller.type()} and id ${caller.id()}"}
                context.send(responseMessage)
            }

            return context.done()
        }

        return context.done()
    }

    class ShoppingCart(val contents: MutableMap<String, Int>) {
        companion object {
            val TYPE = createJsonType("shoppingcart", ShoppingCart::class)
        }
    }
}