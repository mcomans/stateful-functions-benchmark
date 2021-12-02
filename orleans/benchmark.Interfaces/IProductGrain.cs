using System.Collections.Generic;
using System.Threading.Tasks;
using Orleans;

namespace benchmark.Interfaces
{
    public interface IProductGrain: IGrainWithGuidKey
    {
        Task IncreaseStock(int amount);
        
        /// <summary>
        /// Retracts stock of the product by the amount
        /// </summary>
        /// <param name="amount">The amount by which the stock will be retracted</param>
        /// <returns>A tuple, with a boolean describing if the action was successful and an integer describing the price of the product</returns>
        Task<(bool, int)> DecreaseStock(int amount);
        
        Task SetPrice(int price);
        Task<int> GetPrice();

        Task UpdateFrequentItems(List<IProductGrain> products);
    }
}