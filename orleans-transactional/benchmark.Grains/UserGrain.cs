using System.Threading.Tasks;
using benchmark.Interfaces;
using Microsoft.Extensions.Logging;
using Orleans;
using Orleans.Concurrency;
using Orleans.Runtime;
using Orleans.Transactions.Abstractions;

namespace benchmark.Grains
{
    [Reentrant]
    public class UserGrain : Grain, IUserGrain
    {
        private readonly ITransactionalState<UserState> _userState;

        public UserGrain([TransactionalState("user", "benchmarkStore")] ITransactionalState<UserState> userState,
            ILogger<UserGrain> logger)
        {
            _userState = userState;
        }

        public async Task AddCredit(int amount)
        {
            await _userState.PerformUpdate(x => x.Credits += amount);
        }

        public Task<bool> RetractCredit(int amount)
        {
            return _userState.PerformUpdate(x =>
            {
                if (x.Credits - amount < 0) return false;

                x.Credits -= amount;
                return true;
            });
        }
    }

    public class UserState
    {
        public int Credits { get; set; }
    }
}