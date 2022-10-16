using System.Collections.Generic;
using System.Threading.Tasks;
using Orleans;

namespace benchmark.Interfaces
{
    public interface IShoppingCartGrain : IGrainWithGuidKey
    {
        Task AddToCart(IProductGrain product, int amount);
        
        Task RemoveFromCart(IProductGrain product, int amount);
        
        [Transaction(TransactionOption.Supported)]
        Task<List<KeyValuePair<IProductGrain, int>>> GetContents();
    }
}