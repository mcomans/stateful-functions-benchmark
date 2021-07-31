using System.Threading.Tasks;
using Orleans;

namespace benchmark.Interfaces
{
    public interface IUserGrain : IGrainWithGuidKey
    {
        Task AddCredit(int amount);
        Task RetractCredit(int amount);
    }
}