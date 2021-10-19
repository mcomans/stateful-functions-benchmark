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
            var id =  Guid.NewGuid();
            var grain = _client.GetGrain<IOrderGrain>(id);
            var shoppingCart = _client.GetGrain<IShoppingCartGrain>(order.ShoppingCartId);
            var user = _client.GetGrain<IUserGrain>(order.UserId);

            await grain.Checkout(shoppingCart, user);
            
            return Ok();
        }
    }

    public class Order
    {
        public Guid ShoppingCartId { get; set; }
        public Guid UserId { get; set; }
    }
}