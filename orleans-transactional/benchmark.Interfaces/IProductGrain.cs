using System.Collections.Generic;
using System.Threading.Tasks;
using Orleans;
using Orleans.Concurrency;

namespace benchmark.Interfaces
{
    public interface IProductGrain : IGrainWithGuidKey
    {
        [Transaction(TransactionOption.CreateOrJoin)]
        Task IncreaseStock(int amount);

        /// <summary>
        ///     Retracts stock of the product by the amount
        /// </summary>
        /// <param name="amount">The amount by which the stock will be retracted</param>
        /// <returns>
        ///     A tuple, with a boolean describing if the action was successful and an integer describing the price of the
        ///     product
        /// </returns>
        [Transaction(TransactionOption.CreateOrJoin)]
        Task<(bool, int)> DecreaseStock(int amount);

        [Transaction(TransactionOption.CreateOrJoin)]
        Task SetPrice(int price);
        
        [Transaction(TransactionOption.CreateOrJoin)]
        Task<int> GetPrice();

        [Transaction(TransactionOption.Suppress)]
        Task UpdateFrequentItems(List<IProductGrain> products);

        [AlwaysInterleave]
        Task<ISet<IProductGrain>> GetFrequentItemsGraph(ISet<IProductGrain> visited, int depth = 3, int top = 3);
    }
}