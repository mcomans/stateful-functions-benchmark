using System.Collections.Generic;
using System.Linq;
using System.Threading.Tasks;
using benchmark.Interfaces;
using Microsoft.Extensions.Logging;
using Orleans;
using Orleans.Concurrency;
using Orleans.Transactions.Abstractions;

namespace benchmark.Grains
{
    [Reentrant]
    public class ProductGrain : Grain, IProductGrain
    {
        private readonly ITransactionalState<ProductState> _productState;

        public ProductGrain([TransactionalState("product", "benchmarkStore")] ITransactionalState<ProductState> productState,
            ILogger<ProductGrain> logger)
        {
            _productState = productState;
        }

        public async Task IncreaseStock(int amount)
        {
            await _productState.PerformUpdate(x => x.Stock += amount);
        }

        public Task<(bool, int)> DecreaseStock(int amount)
        {
            return _productState.PerformUpdate(x =>
            {
                if (x.Stock - amount < 0) return (false, x.Price);
                x.Stock -= amount;
                return (true, x.Price);
            });
        }

        public async Task SetPrice(int price)
        {
            await _productState.PerformUpdate(x => x.Price = price);
        }

        public Task<int> GetPrice()
        {
            return _productState.PerformRead(x => x.Price);
        }

        public async Task UpdateFrequentItems(List<IProductGrain> products)
        {
            await _productState.PerformUpdate(x =>
            {
                foreach (var product in products)
                    if (x.FrequentItems.ContainsKey(product))
                        x.FrequentItems[product] += 1;
                    else
                        x.FrequentItems[product] = 1;
            });
        }

        public async Task<ISet<IProductGrain>> GetFrequentItemsGraph(ISet<IProductGrain> visited, int depth = 3,
            int top = 3)
        {
            var frequentItems = await _productState.PerformRead(x => x.FrequentItems);
            var topProducts = frequentItems.OrderBy(p => p.Value)
                .Where(p => !visited.Contains(p.Key))
                .Take(top)
                .Select(p => p.Key)
                .ToList();

            if (depth == 1) return topProducts.ToHashSet();

            var newVisited = visited.Union(topProducts).ToHashSet();

            var tasks = topProducts.Select(p =>
                p.GetFrequentItemsGraph(newVisited, depth - 1, top));

            var results = await Task.WhenAll(tasks);

            return results.SelectMany(r => r).Union(topProducts).ToHashSet();
        }
    }

    public class ProductState
    {
        public int Price { get; set; }
        public int Stock { get; set; }

        public Dictionary<IProductGrain, int> FrequentItems { get; } = new();
    }
}