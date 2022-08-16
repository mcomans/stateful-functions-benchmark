using Microsoft.AspNetCore.Mvc;

namespace benchmark.API.Controllers
{
    [ApiController]
    [Route("health")]
    public class HealthController : ControllerBase
    {
        [HttpGet]
        public ActionResult Health()
        {
            return Ok();
        }
    }
}