using System;
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
        private readonly IGrainFactory _client;
        private readonly ILogger<OrdersController> _logger;

        public OrdersController(ILogger<OrdersController> logger, IGrainFactory client)
        {
            _logger = logger;
            _client = client;
        }

        [HttpPost("checkout")]
        public async Task<ActionResult> CheckoutOrder([FromBody] Order order)
        {
            var id = Guid.NewGuid();
            var grain = _client.GetGrain<IOrderGrain>(id);
            var shoppingCart = _client.GetGrain<IShoppingCartGrain>(order.CartId);
            var user = _client.GetGrain<IUserGrain>(order.UserId);

            var success = await grain.Checkout(shoppingCart, user);

            return success ? Ok() : Problem("Checkout failed");
        }
    }

    public class Order
    {
        public Guid CartId { get; set; }
        public Guid UserId { get; set; }
    }
}