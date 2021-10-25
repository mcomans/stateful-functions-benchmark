package types.user

import java.util.*

class RetractCredit(val amount: Int, val requestId: String = UUID.randomUUID().toString())