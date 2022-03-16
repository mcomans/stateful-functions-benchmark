using System.Threading.Tasks;
using benchmark.Interfaces;
using Microsoft.Extensions.Logging;
using Orleans;
using Orleans.Runtime;

namespace benchmark.Grains
{
    public class UserGrain : Grain, IUserGrain
    {
        private readonly IPersistentState<UserState> _userState;

        public UserGrain([PersistentState("user", "benchmarkStore")] IPersistentState<UserState> userState,
            ILogger<UserGrain> logger)
        {
            _userState = userState;
        }

        public async Task AddCredit(int amount)
        {
            _userState.State.Credits += amount;
            await _userState.WriteStateAsync();
        }

        public async Task<bool> RetractCredit(int amount)
        {
            if (_userState.State.Credits - amount < 0) return false;

            _userState.State.Credits -= amount;
            await _userState.WriteStateAsync();

            return true;
        }
    }

    public class UserState
    {
        public int Credits { get; set; }
    }
}