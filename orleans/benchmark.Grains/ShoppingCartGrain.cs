using System.Collections.Generic;
using System.Linq;
using System.Threading.Tasks;
using benchmark.Interfaces;
using Microsoft.Extensions.Logging;
using Orleans.Runtime;

namespace benchmark.Grains
{
    public class ShoppingCartGrain : TracedGrain, IShoppingCartGrain
    {
        private readonly IPersistentState<ShoppingCartState> _shoppingCartState;

        public ShoppingCartGrain([PersistentState("shoppingCart", "benchmarkStore")]
            IPersistentState<ShoppingCartState> shoppingCartState, ILogger<ShoppingCartGrain> logger) : base(logger)
        {
            _shoppingCartState = shoppingCartState;
        }
        
        public async Task AddToCart(IProductGrain product, int amount)
        {
            if (_shoppingCartState.State.Contents.ContainsKey(product))
            {
                _shoppingCartState.State.Contents[product] += amount;
            }
            else
            {
                _shoppingCartState.State.Contents.Add(product, amount);
            }

            await _shoppingCartState.WriteStateAsync();
        }

        public async Task RemoveFromCart(IProductGrain product, int amount)
        {
            if (_shoppingCartState.State.Contents.ContainsKey(product))
            {
                _shoppingCartState.State.Contents[product] -= amount;
            }

            await _shoppingCartState.WriteStateAsync();
        }

        public Task<List<KeyValuePair<IProductGrain, int>>> GetContents()
        {
            return Task.FromResult(_shoppingCartState.State.Contents.ToList());
        }
    }

    public class ShoppingCartState
    {
        public Dictionary<IProductGrain, int> Contents { get; } = new Dictionary<IProductGrain, int>();
    }
}