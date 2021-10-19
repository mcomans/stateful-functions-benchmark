using System.Linq;
using System.Threading.Tasks;
using benchmark.Interfaces;
using Microsoft.Extensions.Logging;

namespace benchmark.Grains
{
    public class OrderGrain : TracedGrain, IOrderGrain
    {
        public async Task Checkout(IShoppingCartGrain shoppingCart, IUserGrain user)
        {
            var products = await shoppingCart.GetContents();
            var prices = await Task.WhenAll(products.Select(
                async product => product.Value * await product.Key.GetPrice()));
            var totalPrice = prices.Aggregate(0, (acc, price) => acc + price);
            
            await user.RetractCredit(totalPrice);
            await Task.WhenAll(products.Select(product => product.Key.DecreaseStock(product.Value)));
        }

        public OrderGrain(ILogger<OrderGrain> logger) : base(logger)
        {
        }
    }
}