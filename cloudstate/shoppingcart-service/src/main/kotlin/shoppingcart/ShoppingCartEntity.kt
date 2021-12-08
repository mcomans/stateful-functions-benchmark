package shoppingcart

import com.google.protobuf.Empty
import io.cloudstate.javasupport.eventsourced.CommandContext
import io.cloudstate.kotlinsupport.annotations.EntityId
import io.cloudstate.kotlinsupport.annotations.eventsourced.*
import mu.KotlinLogging
import mu.withLoggingContext
import shoppingcart.persistence.Domain

private val logger = KotlinLogging.logger {}

@EventSourcedEntity
class ShoppingCartEntity(@EntityId private val entityId: String) {
   private val cart = mutableMapOf<String, ShoppingCartProduct>()

   @Snapshot
   fun snapshot(): Domain.ShoppingCart = Domain.ShoppingCart
      .newBuilder()
      .addAllProducts(cart.map {
         Domain.ShoppingCartProduct.newBuilder().setProductId(it.value.productId).setAmount(it.value.amount).build()
      })
      .build()

   @SnapshotHandler
   fun snapshotHandler(shoppingCart: Domain.ShoppingCart) {
      cart.putAll(shoppingCart.productsList.map { it.productId to ShoppingCartProduct(it.productId, it.amount) })
   }

   @EventHandler
   fun productAdded(productAdded: Domain.ProductAdded) {
      val amountInCart = cart[productAdded.productId]?.amount ?: 0
      val newAmount = amountInCart + productAdded.amount
      cart[productAdded.productId] = ShoppingCartProduct(productAdded.productId, newAmount)
      println("Cart $entityId - Cart contents: $cart")
   }

   @EventHandler
   fun productRemoved(productRemoved: Domain.ProductRemoved) {
      val amountInCart = cart[productRemoved.productId]?.amount ?: 0
      val newAmount = amountInCart - productRemoved.amount

      if (newAmount <= 0) {
         cart.remove(productRemoved.productId)
      } else {
         cart[productRemoved.productId] = ShoppingCartProduct(productRemoved.productId, newAmount)
      }
      println("Cart $entityId - Cart contents: $cart")
   }

   @CommandHandler
   fun getCartContents(getCartContentsMessage: Shoppingcart.GetCartContentsMessage): Shoppingcart.GetCartContentsResponse {
      withLoggingContext(
         "requestId" to getCartContentsMessage.requestId,
         "function" to "getCartContents",
         "entityType" to "shopping-cart",
         "entityId" to entityId,
      ) {
         logger.debug { "Returning cart contents" }
         return Shoppingcart.GetCartContentsResponse.newBuilder().addAllProducts(
            cart.map { Shoppingcart.CartProduct.newBuilder().setProductId(it.key).setAmount(it.value.amount).build() }
         ).build()
      }
   }

   @CommandHandler
   fun addToCart(addToCartMessage: Shoppingcart.AddToCartMessage, ctx: CommandContext): Empty {
      withLoggingContext(
         "requestId" to addToCartMessage.requestId,
         "function" to "addToCart",
         "entityType" to "shopping-cart",
         "entityId" to entityId,
      ) {
         logger.debug { "Adding product ${addToCartMessage.productId} with amount ${addToCartMessage.amount}" }
         ctx.emit(
            Domain.ProductAdded.newBuilder().setProductId(addToCartMessage.productId).setAmount(addToCartMessage.amount)
               .build()
         )
         return Empty.getDefaultInstance()
      }
   }

   @CommandHandler
   fun removeFromCart(removeFromCartMessage: Shoppingcart.RemoveFromCartMessage, ctx: CommandContext): Empty {
      withLoggingContext(
         "requestId" to removeFromCartMessage.requestId,
         "function" to "removeFromCart",
         "entityType" to "shopping-cart",
         "entityId" to entityId,
      ) {
         logger.debug { "Removing product ${removeFromCartMessage.productId} with amount ${removeFromCartMessage.amount}" }
         ctx.emit(
            Domain.ProductRemoved.newBuilder().setProductId(removeFromCartMessage.productId)
               .setAmount(removeFromCartMessage.amount).build()
         )
         return Empty.getDefaultInstance()
      }
   }

   data class ShoppingCartProduct(val productId: String, val amount: Int)
}