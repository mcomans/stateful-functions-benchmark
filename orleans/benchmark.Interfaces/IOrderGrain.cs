using System.Threading.Tasks;

namespace benchmark.Interfaces
{
    public interface IOrderGrain
    {
        Task Checkout(IShoppingCartGrain shoppingCart, IUserGrain user);
    }
}