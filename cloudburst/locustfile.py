from locust import HttpUser, task, between
from random import randint

class BenchmarkUser(HttpUser):
    wait_time = between(1, 2)

    @task
    def checkout(self):
        product1_response = self.client.post("/products", json={})
        product1 = product1_response.text.strip("\"")
        product2_response = self.client.post("/products", json={})
        product2 = product2_response.text.strip("\"")

        product1_price = randint(1, 10)
        product1_stock = randint(10, 20)

        product2_price = randint(1, 10)
        product2_stock = randint(10, 20)

        self.client.patch(f"/products/{product1}", json={"price": product1_price, "stock": product1_stock})
        self.client.patch(f"/products/{product2}", json={"price": product2_price, "stock": product2_stock})

        user_response = self.client.post("/users", json={})
        user = user_response.text.strip("\"")
        user_credits = product1_price * product1_stock + product2_price * product2_stock

        self.client.patch(f"/users/{user}/credits/add", json={"credits": user_credits})

        shopping_cart_response = self.client.post("/shopping-carts")
        shopping_cart = shopping_cart_response.text.strip("\"")

        self.client.post(f"/shopping-carts/{shopping_cart}/products", json={"productId": product1, "amount": product1_stock})
        self.client.post(f"/shopping-carts/{shopping_cart}/products", json={"productId": product2, "amount": product2_stock})

        self.client.post("/orders/checkout", json={"shoppingCartId": shopping_cart, "userId": user})





        

