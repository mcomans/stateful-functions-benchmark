using System;
using System.Collections.Generic;
using System.Linq;
using System.Threading.Tasks;
using benchmark.Interfaces;
using Microsoft.AspNetCore.Mvc;
using Microsoft.Extensions.Logging;
using Orleans;
using Orleans.Runtime;

namespace benchmark.API.Controllers
{
    [ApiController]
    [Route("products")]
    public class ProductsController : ControllerBase
    {
        private readonly IGrainFactory _client;
        private readonly ILogger<ProductsController> _logger;

        public ProductsController(ILogger<ProductsController> logger, IGrainFactory client)
        {
            _logger = logger;
            _client = client;
        }

        [HttpPost]
        public async Task<ActionResult<string>> AddProduct([FromBody] Product product)
        {
            var id = Guid.NewGuid();
            var productGrain = _client.GetGrain<IProductGrain>(id);
            if (product.Price.HasValue) await productGrain.SetPrice(product.Price.Value);

            if (product.Stock.HasValue) await productGrain.IncreaseStock(product.Stock.Value);

            return id.ToString();
        }

        [HttpPatch("{id:guid}")]
        public async Task<ActionResult> PatchProduct(Guid id, [FromBody] Product product)
        {
            var productGrain = _client.GetGrain<IProductGrain>(id);
            if (product.Price.HasValue) await productGrain.SetPrice(product.Price.Value);

            if (product.Stock.HasValue) await productGrain.IncreaseStock(product.Stock.Value);

            return Ok();
        }

        [HttpGet("{id:guid}/price")]
        public async Task<int> GetPrice(Guid id)
        {
            var price = await _client.GetGrain<IProductGrain>(id).GetPrice();
            return price;
        }

        [HttpGet("{id:guid}/freq-items")]
        public async Task<ActionResult<List<string>>> GetFrequentItemsGraph(Guid id, int top = 3, int depth = 3)
        {
            using(_logger.BeginScope( new Dictionary<string, object>
            {
                ["top"] = top,
                ["depth"] = depth,
                ["traceId"] = RequestContext.Get("traceId")
            }))
            {
                _logger.Info("freq-items");  
            }
            var product = _client.GetGrain<IProductGrain>(id);
            var products = await product.GetFrequentItemsGraph(new HashSet<Guid> { id }, depth, top);

            return products.Select(p => p.ToString()).ToList();
        }
    }

    public class Product
    {
        public int? Price { get; set; }
        public int? Stock { get; set; }
    }

    public class FrequentItemsQuery
    {
        public int Top { get; set; } = 3;
        public int Depth { get; set; } = 3;
    }
}