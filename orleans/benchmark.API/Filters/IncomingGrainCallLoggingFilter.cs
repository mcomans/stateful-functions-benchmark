using System;
using System.Collections.Generic;
using System.Threading.Tasks;
using benchmark.Grains;
using Microsoft.Extensions.Logging;
using Orleans;
using Orleans.Runtime;

namespace benchmark.API.Filters
{
    public class IncomingGrainCallLoggingFilter : IIncomingGrainCallFilter
    {
        private readonly ILogger<IncomingGrainCallLoggingFilter> _logger;

        private static readonly HashSet<string> TracedGrains = new()
            {nameof(OrderGrain), nameof(ProductGrain), nameof(ShoppingCartGrain), nameof(UserGrain)};

        public IncomingGrainCallLoggingFilter(ILogger<IncomingGrainCallLoggingFilter> logger)
        {
            _logger = logger;
        }

        public async Task Invoke(IIncomingGrainCallContext context)
        {
            if (TracedGrains.Contains(context.Grain.GetType().Name))
            {
                var traceId = RequestContext.Get("traceId");
                var grainId = context.Grain.GetPrimaryKey();
                var grainType = context.Grain.GetType().Name;
                GrainContext.CurrentGrain.Value = context.Grain;
                var callId = RequestContext.Get("callId") as Guid?;
                GrainContext.CallId.Value = callId;

                using (_logger.BeginScope(new Dictionary<string, object>
                {
                    ["traceId"] = traceId,
                    ["grainId"] = grainId,
                    ["grainType"] = grainType,
                    ["method"] = context.ImplementationMethod.Name,
                    ["status"] = "INCOMING_CALL",
                    ["callId"] = callId
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
                    ["status"] = "RETURNING_CALL",
                    ["callId"] = callId
                }))
                {
                    _logger.Info("Execution complete");
                }
            }
            else
            {
                await context.Invoke();
            }
        }
    }
}