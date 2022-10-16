using System.Collections.Generic;
using System.Linq;
using System.Threading.Tasks;
using benchmark.Interfaces;
using Microsoft.Extensions.Logging;
using Orleans;
using Orleans.Runtime;

namespace benchmark.Grains
{
    public class ShoppingCartGrain : Grain, IShoppingCartGrain
    {
        private readonly IPersistentState<ShoppingCartState> _shoppingCartState;
        private readonly ILogger<ShoppingCartGrain> _logger;

        public ShoppingCartGrain(
            [PersistentState("shoppingCart", "benchmarkStore")] IPersistentState<ShoppingCartState> shoppingCartState,
            ILogger<ShoppingCartGrain> logger)
        {
            _shoppingCartState = shoppingCartState;
            _logger = logger;
        }

        public async Task AddToCart(IProductGrain product, int amount)
        {
            if (_shoppingCartState.State.Contents.ContainsKey(product))
                _shoppingCartState.State.Contents[product] += amount;
            else
                _shoppingCartState.State.Contents.Add(product, amount);

            await _shoppingCartState.WriteStateAsync();
        }

        public async Task RemoveFromCart(IProductGrain product, int amount)
        {
            if (_shoppingCartState.State.Contents.ContainsKey(product))
                _shoppingCartState.State.Contents[product] -= amount;

            await _shoppingCartState.WriteStateAsync();
        }

        public Task<List<KeyValuePair<IProductGrain, int>>> GetContents()
        {
            var contents = _shoppingCartState.State.Contents.ToList();
            _logger.Info("{productCount} {traceId}", contents.Count, RequestContext.Get("traceId"));
            return Task.FromResult(contents);
        }
    }

    public class ShoppingCartState
    {
        public Dictionary<IProductGrain, int> Contents { get; } = new();
    }
}