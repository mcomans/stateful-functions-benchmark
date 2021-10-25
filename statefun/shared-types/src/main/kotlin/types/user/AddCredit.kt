package types.user

import java.util.*

class AddCredit(val amount: Int, val requestId: String = UUID.randomUUID().toString())