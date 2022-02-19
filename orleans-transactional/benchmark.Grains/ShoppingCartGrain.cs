using System.Collections.Generic;
using System.Linq;
using System.Threading.Tasks;
using benchmark.Interfaces;
using Microsoft.Extensions.Logging;
using Orleans.Transactions.Abstractions;

namespace benchmark.Grains
{
    public class ShoppingCartGrain : TracedGrain, IShoppingCartGrain
    {
        private readonly ITransactionalState<ShoppingCartState> _shoppingCartState;

        public ShoppingCartGrain(
            [TransactionalState("shoppingCart", "benchmarkStore")] ITransactionalState<ShoppingCartState> shoppingCartState,
            ILogger<ShoppingCartGrain> logger) : base(logger)
        {
            _shoppingCartState = shoppingCartState;
        }

        public async Task AddToCart(IProductGrain product, int amount)
        {
            await _shoppingCartState.PerformUpdate(x =>
            {
                if (x.Contents.ContainsKey(product))
                    x.Contents[product] += amount;
                else
                    x.Contents.Add(product, amount);
            });
        }

        public async Task RemoveFromCart(IProductGrain product, int amount)
        {
            await _shoppingCartState.PerformUpdate(x =>
            {
                if (x.Contents.ContainsKey(product))
                    x.Contents[product] -= amount;
            });
        }

        public Task<List<KeyValuePair<IProductGrain, int>>> GetContents()
        {
            return _shoppingCartState.PerformRead(x => x.Contents.ToList());
        }
    }

    public class ShoppingCartState
    {
        public Dictionary<IProductGrain, int> Contents { get; } = new();
    }
}