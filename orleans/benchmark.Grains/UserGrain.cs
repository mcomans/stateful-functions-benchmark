using System.Threading.Tasks;
using benchmark.Interfaces;
using Microsoft.Extensions.Logging;
using Orleans.Runtime;

namespace benchmark.Grains
{
    public class UserGrain : TracedGrain, IUserGrain
    {
        private readonly IPersistentState<UserState> _userState;

        public UserGrain([PersistentState("user", "benchmarkStore")] IPersistentState<UserState> userState, 
            ILogger<UserGrain> logger) : base(logger)
        {
            _userState = userState;
        }
        
        public async Task AddCredit(int amount)
        {
            _userState.State.Credits += amount;
            await _userState.WriteStateAsync();
        }

        public async Task RetractCredit(int amount)
        {
            _userState.State.Credits -= amount;
            await _userState.WriteStateAsync();
        }
    }

    public class UserState
    {
        public int Credits { get; set; } = 0;
    }
}