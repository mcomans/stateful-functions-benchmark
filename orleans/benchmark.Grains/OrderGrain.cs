using System;
using System.Collections.Generic;
using System.Linq;
using System.Threading.Tasks;
using benchmark.Interfaces;
using Microsoft.Extensions.Logging;
using Orleans;

namespace benchmark.Grains
{
    public class OrderGrain : Grain, IOrderGrain
    {
        public OrderGrain(ILogger<OrderGrain> logger)
        {
        }

        public async Task<bool> Checkout(IShoppingCartGrain shoppingCart, IUserGrain user)
        {
            var products = await shoppingCart.GetContents();
            var retractStockResults = await Task.WhenAll(products.Select(async product =>
            {
                var productGrain = GrainFactory.GetGrain<IProductGrain>(product.Key);
                var (success, price) = await productGrain.DecreaseStock(product.Value);
                return (success, productTotal: price * product.Value, product: productGrain, amount: product.Value);
            }));

            // If any result of DecreaseStock is not successful, rollback changes to the other products
            if (retractStockResults.Any(result => !result.success))
            {
                await Task.WhenAll(retractStockResults
                    .Where(result => result.success)
                    .Select(async result => { await result.product.IncreaseStock(result.amount); }));
                return false;
            }

            var totalPrice = retractStockResults.Aggregate(0, (acc, result) => acc + result.productTotal);
            var retractCreditSuccessful = await user.RetractCredit(totalPrice);

            // If user does not have enough credits, rollback the changes to the stock
            if (!retractCreditSuccessful)
            {
                await Task.WhenAll(retractStockResults
                    .Select(async result => { await result.product.IncreaseStock(result.amount); }));
                return false;
            }

            UpdateFrequentItems(products.Select(p => p.Key));

            return true;
        }

        private async Task UpdateFrequentItems(IEnumerable<Guid> products)
        {
            var productGrains = products.Select(id => GrainFactory.GetGrain<IProductGrain>(id)).ToList();
            foreach (var product in productGrains)
                await product.UpdateFrequentItems(productGrains.Where(p => p != product).Select(p => p.GetPrimaryKey()).ToList());
        }
    }
}