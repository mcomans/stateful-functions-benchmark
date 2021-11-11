package shoppingcart

import com.google.protobuf.Empty
import io.cloudstate.javasupport.eventsourced.CommandContext
import io.cloudstate.kotlinsupport.annotations.EntityId
import io.cloudstate.kotlinsupport.annotations.eventsourced.*
import shoppingcart.persistence.Domain

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
   fun addToCart(addToCartMessage: Shoppingcart.AddToCartMessage, ctx: CommandContext): Empty {
      println("Cart $entityId - Adding product ${addToCartMessage.productId} with amount ${addToCartMessage.amount}")
      ctx.emit(Domain.ProductAdded.newBuilder().setProductId(addToCartMessage.productId).setAmount(addToCartMessage.amount).build())
      return Empty.getDefaultInstance()
   }

   @CommandHandler
   fun removeFromCart(removeFromCartMessage: Shoppingcart.RemoveFromCartMessage, ctx: CommandContext): Empty {
      println("Cart $entityId - Removing product ${removeFromCartMessage.productId} with amount ${removeFromCartMessage.amount}")
      ctx.emit(Domain.ProductRemoved.newBuilder().setProductId(removeFromCartMessage.productId).setAmount(removeFromCartMessage.amount).build())
      return Empty.getDefaultInstance()
   }

   data class ShoppingCartProduct(val productId: String, val amount: Int)
}