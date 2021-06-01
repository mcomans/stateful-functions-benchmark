package order

import createJsonType
import org.apache.flink.statefun.sdk.java.types.Type
import types.order.Checkout
import types.shoppingcart.GetCartResponse
import types.user.RetractCreditResponse
import types.product.RetractStockResponse

object OrderMessages {
    val CHECKOUT: Type<Checkout> = createJsonType("order", Checkout::class)
}