using System;
using System.Collections.Generic;
using System.Threading.Tasks;
using benchmark.Interfaces;
using Microsoft.Extensions.Logging;
using Orleans;
using Orleans.Runtime;
using Orleans.Transactions;

namespace benchmark.API.Filters
{
    public class OutgoingGrainCallLoggingFilter : IOutgoingGrainCallFilter
    {
        private readonly ILogger<OutgoingGrainCallLoggingFilter> _logger;

        private static readonly HashSet<string> TracedGrains = new()
            {nameof(IOrderGrain), nameof(IProductGrain), nameof(IShoppingCartGrain), nameof(IUserGrain)};

        public OutgoingGrainCallLoggingFilter(ILogger<OutgoingGrainCallLoggingFilter> logger)
        {
            _logger = logger;
        }

        public async Task Invoke(IOutgoingGrainCallContext context)
        {
            if (context.Grain is GrainReference grainRef && TracedGrains.Contains(grainRef.InterfaceName))
            {
                var traceId = RequestContext.Get("traceId");
                var grainId = grainRef.GetPrimaryKey();
                var grainInterface = grainRef.InterfaceName;
                var currentGrain = GrainContext.CurrentGrain.Value;
                var callId = Guid.NewGuid();
                var transactionId = TransactionContext.GetTransactionInfo()?.Id;
                RequestContext.Set("callId", callId);

                using (_logger.BeginScope(new Dictionary<string, object>
                {
                    ["traceId"] = traceId,
                    ["grainId"] = grainId,
                    ["grainInterface"] = grainInterface,
                    ["currentGrainId"] = currentGrain?.GetPrimaryKey(),
                    ["currentGrainType"] = currentGrain?.GetType().Name,
                    ["method"] = context.InterfaceMethod.Name,
                    ["status"] = "OUTGOING_CALL",
                    ["transactionId"] = transactionId,
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
                    ["grainInterface"] = grainInterface,
                    ["currentGrainId"] = currentGrain?.GetPrimaryKey(),
                    ["currentGrainType"] = currentGrain?.GetType().Name,
                    ["method"] = context.InterfaceMethod.Name,
                    ["status"] = "OUTGOING_CALL_RETURNED",
                    ["transactionId"] = transactionId,
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