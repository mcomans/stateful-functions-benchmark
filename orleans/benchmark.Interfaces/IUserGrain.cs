using System.Threading.Tasks;
using Orleans;

namespace benchmark.Interfaces
{
    public interface IUserGrain : IGrainWithGuidKey
    {
        Task AddCredit(int amount);
        
        /// <summary>
        /// Retracts credit from the user by the amount
        /// </summary>
        /// <param name="amount"></param>
        /// <returns>A boolean, describing if the operation was successful</returns>
        Task<bool> RetractCredit(int amount);
    }
}