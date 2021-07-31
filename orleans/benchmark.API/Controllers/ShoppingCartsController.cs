using System;
using System.Threading.Tasks;
using benchmark.Interfaces;
using Microsoft.AspNetCore.Mvc;
using Microsoft.Extensions.Logging;
using Orleans;

namespace benchmark.API.Controllers
{
    [ApiController]
    [Route("shopping-carts")]
    public class ShoppingCartsController : ControllerBase
    {
        private readonly ILogger<ShoppingCartsController> _logger;
        private readonly IGrainFactory _client;

        public ShoppingCartsController(ILogger<ShoppingCartsController> logger, IGrainFactory client)
        {
            _logger = logger;
            _client = client;
        }

        [HttpPost]
        public async Task<ActionResult<Guid>> CreateShoppingCart()
        {
            return Guid.NewGuid();
        }
        
        [HttpPost("{id:guid}/products")]
        public async Task<ActionResult> AddProductToShoppingCart(Guid id, [FromBody] ShoppingCartProduct product)
        {
            var shoppingCart = _client.GetGrain<IShoppingCartGrain>(id);
            var productGrain = _client.GetGrain<IProductGrain>(product.ProductId);
            await shoppingCart.AddToCart(productGrain, product.Amount);
            return new OkResult();
        }
        
        [HttpDelete("{id:guid}/products")]
        public async Task<ActionResult> RemoveProductFromShoppingCart(Guid id, [FromBody] ShoppingCartProduct product)
        {
            var shoppingCart = _client.GetGrain<IShoppingCartGrain>(id);
            var productGrain = _client.GetGrain<IProductGrain>(product.ProductId);
            await shoppingCart.RemoveFromCart(productGrain, product.Amount);
            return new OkResult();
        }
    }

    public class ShoppingCartProduct
    {
        public Guid ProductId { get; set; }
        public int Amount { get; set; }
    }
}