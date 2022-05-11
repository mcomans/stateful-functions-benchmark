using System.Threading;
using Orleans.Runtime;

namespace benchmark.API.Filters;

public class GrainContext
{
    public static readonly AsyncLocal<IAddressable> CurrentGrain = new();
}