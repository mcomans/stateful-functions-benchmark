package types.shoppingcart

import types.WrappedMessage

class AddToCart(val productId: String, val amount: Int) : WrappedMessage