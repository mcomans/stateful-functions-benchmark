def register_orders_functions(cloud):
  cloud.register_dag("checkout",
                     [
                         "checkout_get_cart_contents",
                         "checkout_retract_all_stock",
                         "checkout_retract_credits",
                         "checkout_rollback_stock"
                     ],
                     [
                         ("checkout_get_cart_contents", "checkout_retract_all_stock"),
                         ("checkout_retract_all_stock", "checkout_retract_credits"),
                         ("checkout_retract_credits", "checkout_rollback_stock")
                     ])
