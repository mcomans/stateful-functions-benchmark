package types.shoppingcart

import types.WrappedMessage

class RemoveFromCart(val productId: String, val amount: Int) : WrappedMessage