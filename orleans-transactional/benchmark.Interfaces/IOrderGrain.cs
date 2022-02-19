using System.Threading.Tasks;
using Orleans;

namespace benchmark.Interfaces
{
    public interface IOrderGrain : IGrainWithGuidKey
    {
        [Transaction(TransactionOption.Create)]
        Task<bool> Checkout(IShoppingCartGrain shoppingCart, IUserGrain user);
    }
}