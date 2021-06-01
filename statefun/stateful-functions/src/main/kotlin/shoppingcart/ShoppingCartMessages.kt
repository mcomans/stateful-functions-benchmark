package shoppingcart

import createJsonType
import org.apache.flink.statefun.sdk.java.types.Type;
import types.shoppingcart.AddToCart
import types.shoppingcart.GetCart
import types.shoppingcart.GetCartResponse
import types.shoppingcart.RemoveFromCart


object ShoppingCartMessages {
    val GET_CART: Type<GetCart> = createJsonType("shoppingcart", GetCart::class)
    val GET_CART_RESPONSE = createJsonType("shoppingcart", GetCartResponse::class)
    val ADD_TO_CART: Type<AddToCart> = createJsonType("shoppingcart", AddToCart::class)
    val REMOVE_FROM_CART: Type<RemoveFromCart> = createJsonType("shoppingcart", RemoveFromCart::class)
}

