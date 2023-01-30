using System;
using System.Threading;
using Orleans.Runtime;

namespace benchmark.API.Filters;

public class GrainContext
{
    public static readonly AsyncLocal<IAddressable> CurrentGrain = new();
    public static readonly AsyncLocal<Guid?> CallId = new();
}