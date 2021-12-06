using System;
using System.Threading.Tasks;
using benchmark.Interfaces;
using Microsoft.AspNetCore.Mvc;
using Microsoft.Extensions.Logging;
using Orleans;

namespace benchmark.API.Controllers
{
    [ApiController]
    [Route("users")]
    public class UsersController : ControllerBase
    {
        private readonly ILogger<UsersController> _logger;
        private readonly IGrainFactory _client;

        public UsersController(ILogger<UsersController> logger, IGrainFactory client)
        {
            _logger = logger;
            _client = client;
        }

        [HttpPost]
        public async Task<ActionResult<string>> CreateUser([FromBody] User user)
        {
            var id = Guid.NewGuid();

            var grain = _client.GetGrain<IUserGrain>(id);

            if (user.Credits > 0)
            {
                await grain.AddCredit(user.Credits);
            }

            return id.ToString();
        }

        [HttpPatch("{id:guid}/credits/add")]
        public async Task<ActionResult> AddCredit(Guid id, [FromBody] User user)
        {
            var grain = _client.GetGrain<IUserGrain>(id);

            await grain.AddCredit(user.Credits);

            return Ok();
        }
        
        [HttpPatch("{id:guid}/credits/retract")]
        public async Task<ActionResult> RetractCredit(Guid id, [FromBody] User user)
        {
            var grain = _client.GetGrain<IUserGrain>(id);

            await grain.RetractCredit(user.Credits);

            return Ok();
        }

    }

    public class User
    {
        public int Credits { get; set; } = 0;
    }
}