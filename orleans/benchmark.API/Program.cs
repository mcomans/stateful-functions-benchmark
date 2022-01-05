using System;
using System.Net;
using Microsoft.AspNetCore.Hosting;
using Microsoft.Extensions.Hosting;
using Orleans.Configuration;
using Orleans.Hosting;
using Serilog;
using Serilog.Formatting.Compact;

namespace benchmark.API
{
    public class Program
    {
        public static void Main(string[] args)
        {
            Log.Logger = new LoggerConfiguration()
                .Enrich.FromLogContext()
                .WriteTo.Console(new CompactJsonFormatter())
                .CreateLogger();

            CreateHostBuilder(args).Build().Run();
        }

        public static IHostBuilder CreateHostBuilder(string[] args)
        {
            return Host.CreateDefaultBuilder(args)
                .UseOrleans(siloBuilder =>
                {
                    siloBuilder
                        .UseAdoNetClustering(options =>
                        {
                            options.Invariant = "Npgsql";
                            options.ConnectionString =
                                "User ID=postgres;Host=localhost;Port=5432;Database=benchmark-orleans;Pooling=true;";
                        })
                        .AddAdoNetGrainStorage("benchmarkStore", options =>
                        {
                            options.Invariant = "Npgsql";
                            options.ConnectionString =
                                "User ID=postgres;Host=localhost;Port=5432;Database=benchmark-orleans;Pooling=true;";
                            options.UseJsonFormat = true;
                        })
                        // .UseLocalhostClustering()
                        .UseKubernetesHosting()
                        .AddMemoryGrainStorage("benchmarkStore")
                        .Configure<HostOptions>(opts => opts.ShutdownTimeout = TimeSpan.FromMinutes(1))
                        .Configure<ClusterOptions>(opts =>
                        {
                            opts.ClusterId = "dev";
                            opts.ServiceId = "BenchmarkAPIService";
                        })
                        .Configure<EndpointOptions>(opts => opts.AdvertisedIPAddress = IPAddress.Loopback);
                })
                .UseSerilog()
                .ConfigureWebHostDefaults(webBuilder => { webBuilder.UseStartup<Startup>(); });
        }
    }
}