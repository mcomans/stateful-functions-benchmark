from locust import HttpUser, task, between
from random import randint
from uuid import uuid4

class BenchmarkUser(HttpUser):
    wait_time = between(1, 2)

    @task
    def checkout(self):
        product1_id = str(uuid4())
        product1_price = randint(1, 10)
        product1_stock = randint(10, 20)

        product2_id = str(uuid4())
        product2_price = randint(1, 10)
        product2_stock = randint(10, 20)

        self.client.post("/proxy", json={
            "topic":"set-price",
            "id": product1_id,
            "message": f"{{\"productId\": \"{product1_id}\", \"price\": {product1_price}}}"
        })

        self.client.post("/proxy", json={
            "topic":"add-stock",
            "id": product1_id,
            "message": f"{{\"productId\": \"{product1_id}\", \"amount\": {product1_stock}}}"
        })

        self.client.post("/proxy", json={
            "topic":"set-price",
            "id": product2_id,
            "message": f"{{\"productId\": \"{product2_id}\", \"price\": {product2_price}}}"
        })

        self.client.post("/proxy", json={
            "topic":"add-stock",
            "id": product1_id,
            "message": f"{{\"productId\": \"{product2_id}\", \"amount\": {product2_stock}}}"
        })

        user = str(uuid4())
        user_credits = product1_price * product1_stock + product2_price * product2_stock

        self.client.post("/proxy", json={
            "topic":"add-credit",
            "id": user,
            "message": f"{{\"userId\": \"{user}\", \"amount\": {user_credits}}}"
        })

        shopping_cart = str(uuid4())

        self.client.post("/proxy", json={
            "topic":"add-to-cart",
            "id": shopping_cart,
            "message": f"{{\"cartId\": \"{shopping_cart}\", \"productId\": \"{product1_id}\", \"amount\": {product1_stock}}}"
        })

        self.client.post("/proxy", json={
            "topic":"add-to-cart",
            "id": shopping_cart,
            "message": f"{{\"cartId\": \"{shopping_cart}\", \"productId\": \"{product2_id}\", \"amount\": {product2_stock}}}"
        })

        order = str(uuid4())

        self.client.post("/proxy", json={
            "topic":"checkout",
            "id": order,
            "message": f"{{\"orderId\": \"{order}\", \"shoppingCartId\": \"{shopping_cart}\", \"userId\": \"{user}\"}}"
        })





        

