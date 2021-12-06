using System;
using System.Collections.Generic;
using System.Linq;
using System.Runtime.InteropServices;
using System.Threading.Tasks;
using benchmark.Interfaces;
using Microsoft.Extensions.Logging;

namespace benchmark.Grains
{
    public class OrderGrain : TracedGrain, IOrderGrain
    {
        public async Task<bool> Checkout(IShoppingCartGrain shoppingCart, IUserGrain user)
        {
            var products = await shoppingCart.GetContents();
            var retractStockResults = await Task.WhenAll(products.Select(async (product) =>
            {
                var (success, price) = await product.Key.DecreaseStock(product.Value);
                return (success, productTotal: price * product.Value, product: product.Key, amount: product.Value);
            }));

            // If any result of DecreaseStock is not successful, rollback changes to the other products
            if (retractStockResults.Any(result => !result.success))
            {
                await Task.WhenAll(retractStockResults
                    .Where(result => result.success)
                    .Select(async result =>
                    {
                        await result.product.IncreaseStock(result.amount);
                    }));
                return false;
            }
            
            var totalPrice = retractStockResults.Aggregate(0, (acc, result) => acc + result.productTotal);
            var retractCreditSuccessful = await user.RetractCredit(totalPrice);
            
            // If user does not have enough credits, rollback the changes to the stock
            if (!retractCreditSuccessful)
            {
                await Task.WhenAll(retractStockResults
                    .Select(async result =>
                    {
                        await result.product.IncreaseStock(result.amount);
                    }));
                return false;
            }
            
            await UpdateFrequentItems(products.Select(p => p.Key));
            
            return true;
        }

        private static async Task UpdateFrequentItems(IEnumerable<IProductGrain> products)
        {
            var productGrains = products.ToList();
            foreach (var product in productGrains)
            {
                await product.UpdateFrequentItems(productGrains.Where(p => p != product).ToList());
            }
        }

        public OrderGrain(ILogger<OrderGrain> logger) : base(logger)
        {
        }
    }
}