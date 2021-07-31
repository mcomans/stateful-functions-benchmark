using System;
using System.Linq;
using System.Threading.Tasks;
using benchmark.Interfaces;
using Microsoft.AspNetCore.Mvc;
using Microsoft.Extensions.Logging;
using Orleans;

namespace benchmark.API.Controllers
{
    [ApiController]
    [Route("orders")]
    public class OrdersController : ControllerBase
    {
        private readonly ILogger<OrdersController> _logger;
        private readonly IGrainFactory _client;

        public OrdersController(ILogger<OrdersController> logger, IGrainFactory client)
        {
            _logger = logger;
            _client = client;
        }

        [HttpPost("checkout")]
        public async Task<ActionResult> CheckoutOrder([FromBody] Order order)
        {
            var shoppingCart = _client.GetGrain<IShoppingCartGrain>(order.ShoppingCartId);
            var products = await shoppingCart.GetContents();
            var prices = await Task.WhenAll(products.Select(
                async product => product.Value * await product.Key.GetPrice()));
            var totalPrice = prices.Aggregate(0, (acc, price) => acc + price);

            var user = _client.GetGrain<IUserGrain>(order.UserId);
            await user.RetractCredit(totalPrice);
            await Task.WhenAll(products.Select(product => product.Key.DecreaseStock(product.Value)));

            return Ok();
        }
    }

    public class Order
    {
        public Guid ShoppingCartId { get; set; }
        public Guid UserId { get; set; }
    }
}