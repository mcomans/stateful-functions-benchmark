using System.Collections.Generic;
using System.Threading.Tasks;
using Microsoft.Extensions.Logging;
using Orleans;
using Orleans.Runtime;

namespace benchmark.Grains
{
    public class TracedGrain : Grain, IIncomingGrainCallFilter, IOutgoingGrainCallFilter
    {
        private ILogger<TracedGrain> _logger;

        public TracedGrain(ILogger<TracedGrain> logger)
        {
            _logger = logger;
        }

        public async Task Invoke(IIncomingGrainCallContext context)
        {
            var traceId = RequestContext.Get("traceId");
            var grainId = context.Grain.GetPrimaryKey();
            var grainType = context.Grain.GetType().Name;

            using (_logger.BeginScope(new Dictionary<string, object>
            {
                ["traceId"] = traceId,
                ["grainId"] = grainId,
                ["grainType"] = grainType,
                ["method"] = context.ImplementationMethod.Name,
                ["status"] = "INCOMING_CALL"
            }))
            {
                _logger.Info("Starting execution");
            }

            await context.Invoke();

            using (_logger.BeginScope(new Dictionary<string, object>
            {
                ["traceId"] = traceId,
                ["grainId"] = grainId,
                ["grainType"] = grainType,
                ["method"] = context.ImplementationMethod.Name,
                ["status"] = "RETURNING_CALL"
            }))
            {
                _logger.Info("Execution complete");
            }
        }

        public async Task Invoke(IOutgoingGrainCallContext context)
        {
            var traceId = RequestContext.Get("traceId");
            var grainId = context.Grain.GetPrimaryKey();
            var grainType = context.Grain.GetType().Name;

            using (_logger.BeginScope(new Dictionary<string, object>
            {
                ["traceId"] = traceId,
                ["grainId"] = grainId,
                ["grainType"] = grainType,
                ["method"] = context.InterfaceMethod.Name,
                ["status"] = "OUTGOING_CALL"
            }))
            {
                _logger.Info("Starting execution");
            }

            await context.Invoke();

            using (_logger.BeginScope(new Dictionary<string, object>
            {
                ["traceId"] = traceId,
                ["grainId"] = grainId,
                ["grainType"] = grainType,
                ["method"] = context.InterfaceMethod.Name.Length,
                ["status"] = "OUTGOING_CALL_RETURNED"
            }))
            {
                _logger.Info("Execution complete");
            }
        }
    }
}