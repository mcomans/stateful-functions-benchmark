using System;
using System.Collections.Generic;
using Microsoft.AspNetCore.Mvc.Filters;
using Microsoft.Extensions.Logging;
using Orleans.Runtime;

namespace benchmark.API.Filters
{
    public class TracingActionFilter : IActionFilter
    {
        private ILogger<TracingActionFilter> _logger;

        public TracingActionFilter(ILogger<TracingActionFilter> logger)
        {
            this._logger = logger;
        }

        public void OnActionExecuting(ActionExecutingContext context)
        {
            var traceId = Guid.NewGuid();
            RequestContext.Set("traceId", traceId);
            
            using (_logger.BeginScope(new Dictionary<string, object>
            {
                ["traceId"] = traceId,
                ["status"] = "HTTP_EXECUTING",
                ["path"] = context.HttpContext.Request.Path,
                ["template"] = context.ActionDescriptor.AttributeRouteInfo?.Template,
                ["httpMethod"] = context.HttpContext.Request.Method
            }))
            {
                _logger.Info("Starting HTTP call");
            }
        }

        public void OnActionExecuted(ActionExecutedContext context)
        {
            var traceId = RequestContext.Get("traceId");   
            using (_logger.BeginScope(new Dictionary<string, object>
            {
                ["traceId"] = traceId,
                ["status"] = "HTTP_EXECUTED",
                ["path"] = context.HttpContext.Request.Path,
                ["template"] = context.ActionDescriptor.AttributeRouteInfo?.Template,
                ["httpMethod"] = context.HttpContext.Request.Method
            }))
            {
                _logger.Info("Returning HTTP call");
            }
        }
    }
}