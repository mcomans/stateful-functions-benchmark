using System.Threading.Tasks;
using Orleans;

namespace benchmark.Interfaces
{
    public interface IProductGrain: IGrainWithGuidKey
    {
        Task IncreaseStock(int amount);
        Task DecreaseStock(int amount);
        Task SetPrice(int price);
        Task<int> GetPrice();
    }
}