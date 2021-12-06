using System.Collections.Generic;
using System.Linq;
using System.Runtime.CompilerServices;
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
        
        public async Task<(bool, int)> DecreaseStock(int amount)
        {
            if (_productState.State.Stock - amount < 0)
            {
                return (false, _productState.State.Price);
            }
            _productState.State.Stock -= amount;
            await _productState.WriteStateAsync();
            return (true, _productState.State.Price);
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

        public async Task UpdateFrequentItems(List<IProductGrain> products)
        {
            foreach (var product in products)
            {   
                if (_productState.State.FrequentItems.ContainsKey(product))
                {
                    _productState.State.FrequentItems[product] += 1;
                }
                else
                {
                    _productState.State.FrequentItems[product] = 1;
                }
            }

            await _productState.WriteStateAsync();
        }
        
        public async Task<ISet<IProductGrain>> GetFrequentItemsGraph(ISet<IProductGrain> visited, int depth = 3, int top = 3)
        {
            var topProducts = _productState.State.FrequentItems.OrderBy(p => p.Value).Take(top).Select(p => p.Key).ToList();

            if (depth == 1)
            {
                return topProducts.ToHashSet();
            }

            var newVisited = visited.Union(topProducts).ToHashSet();

            var tasks = topProducts.Select(p =>
                p.GetFrequentItemsGraph(newVisited, depth - 1, top));

            var results = await Task.WhenAll(tasks);

            return results.SelectMany(r => r).Union(topProducts).ToHashSet();
        }
    }

    public class ProductState
    {
        public int Price { get; set; } = 0;
        public int Stock { get; set; } = 0;

        public Dictionary<IProductGrain, int> FrequentItems { get; } = new Dictionary<IProductGrain, int>();
    }
}