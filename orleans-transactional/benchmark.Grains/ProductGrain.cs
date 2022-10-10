using System.Collections.Generic;
using System.Linq;
using System.Threading.Tasks;
using benchmark.Interfaces;
using Microsoft.Extensions.Logging;
using Orleans;
using Orleans.Concurrency;
using Orleans.Runtime;
using Orleans.Transactions.Abstractions;

namespace benchmark.Grains
{
    [Reentrant]
    public class ProductGrain : Grain, IProductGrain
    {
        private readonly ITransactionalState<ProductState> _productState;
        private readonly IPersistentState<AnalyticsState> _analyticsState;

        public ProductGrain(
            [PersistentState("productAnalytics", "benchmarkStore")] IPersistentState<AnalyticsState> analyticsState,
            [TransactionalState("product", "benchmarkStore")] ITransactionalState<ProductState> productState,
            ILogger<ProductGrain> logger)
        {
            _productState = productState;
            _analyticsState = analyticsState;
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
            foreach (var product in products)
                if (_analyticsState.State.FrequentItems.ContainsKey(product))
                    _analyticsState.State.FrequentItems[product] += 1;
                else
                    _analyticsState.State.FrequentItems[product] = 1;

            await _analyticsState.WriteStateAsync();
        }

        public async Task<ISet<IProductGrain>> GetFrequentItemsGraph(ISet<IProductGrain> visited, int depth = 3,
            int top = 3)
        {
            var topProducts = _analyticsState.State.FrequentItems.OrderBy(p => p.Value)
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
    }

    public class AnalyticsState
    {
        public Dictionary<IProductGrain, int> FrequentItems { get; } = new();
    }
}