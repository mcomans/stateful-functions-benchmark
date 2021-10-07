using System.Threading.Tasks;
using benchmark.Interfaces;
using Microsoft.Extensions.Logging;
using Orleans.Runtime;

namespace benchmark.Grains
{
    public class ProductGrain : TracedGrain, IProductGrain
    {
        private readonly IPersistentState<ProductState> _productState;

        public ProductGrain([PersistentState("product", "benchmarkStore")] IPersistentState<ProductState> productState, 
            ILogger<ProductGrain> logger) : base(logger)
        {
            _productState = productState;
        }
        
        public async Task IncreaseStock(int amount)
        {
            _productState.State.Stock += amount;
            await _productState.WriteStateAsync();
        }

        public async Task DecreaseStock(int amount)
        {
            _productState.State.Stock -= amount;
            await _productState.WriteStateAsync();
        }

        public async Task SetPrice(int price)
        {
            _productState.State.Price = price;
            await _productState.WriteStateAsync();
        }

        public Task<int> GetPrice()
        {
            var price = _productState.State.Price;
            return Task.FromResult(price);
        }
    }

    public class ProductState
    {
        public int Price { get; set; } = 0;
        public int Stock { get; set; } = 0;
    }
}