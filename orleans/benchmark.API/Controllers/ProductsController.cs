using System;
using System.Threading.Tasks;
using benchmark.Interfaces;
using Microsoft.AspNetCore.Mvc;
using Microsoft.Extensions.Logging;
using Orleans;

namespace benchmark.API.Controllers
{
    [ApiController]
    [Route("products")]
    public class ProductsController : ControllerBase
    {
        private readonly ILogger<ProductsController> _logger;
        private readonly IGrainFactory _client;

        public ProductsController(ILogger<ProductsController> logger, IGrainFactory client)
        {
            _logger = logger;
            _client = client;
        }

        [HttpPost]
        public async Task<ActionResult<Guid>> AddProduct(Product product)
        {
            var id =  Guid.NewGuid();
            var productGrain = _client.GetGrain<IProductGrain>(id);
            if (product.Price.HasValue)
            {
                await productGrain.SetPrice(product.Price.Value);
            }

            if (product.Stock.HasValue)
            {
                await productGrain.IncreaseStock(product.Stock.Value);
            }

            return id;
        }

        [HttpPatch("{id:guid}")]
        public async Task<ActionResult> PatchProduct(Guid id, [FromBody] Product product)
        {
            var productGrain = _client.GetGrain<IProductGrain>(id);
            if (product.Price.HasValue)
            {
                await productGrain.SetPrice(product.Price.Value);
            }

            if (product.Stock.HasValue)
            {
                await productGrain.IncreaseStock(product.Stock.Value);
            }

            return Ok();
        }

        [HttpGet("{id:guid}/price")]
        public async Task<int>GetPrice(Guid id)
        {
            var price = await _client.GetGrain<IProductGrain>(id).GetPrice();
            return price;
        }

    }
    
    public class Product
    {
        public int? Price { get; set; }
        public int? Stock { get; set; }
    }
}