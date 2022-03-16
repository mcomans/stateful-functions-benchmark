using System;
using System.Net;
using benchmark.API.Filters;
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
                .UseOrleans((ctx, siloBuilder) =>
                {
                    siloBuilder.AddIncomingGrainCallFilter<IncomingGrainCallLoggingFilter>();
                    siloBuilder.AddOutgoingGrainCallFilter<OutgoingGrainCallLoggingFilter>();
                    
                    if (ctx.HostingEnvironment.IsDevelopment())
                    {
                        siloBuilder
                            .UseLocalhostClustering()
                            .AddMemoryGrainStorage("benchmarkStore");
                    }
                    else
                    {
                        var storageMethod = Environment.GetEnvironmentVariable("STORAGE_METHOD");
                        var psqlConnectionString = Environment.GetEnvironmentVariable("PSQL_CONNECTION_STRING")!;
                        
                        siloBuilder
                            .UseKubernetesHosting()
                            .UseAdoNetClustering(options =>
                            {
                                options.Invariant = "Npgsql";
                                options.ConnectionString = psqlConnectionString;
                            });
                        
                        switch (storageMethod)
                        {
                            case null or "PSQL":
                                siloBuilder.AddAdoNetGrainStorage("benchmarkStore", options =>
                                {
                                    options.Invariant = "Npgsql";
                                    options.ConnectionString = psqlConnectionString;
                                    options.UseJsonFormat = true;
                                });
                                break;
                            case "IN_MEMORY":
                                siloBuilder.AddMemoryGrainStorage("benchmarkStore");
                                break;
                        }
                    }
                })
                .UseSerilog()
                .ConfigureWebHostDefaults(webBuilder => { webBuilder.UseStartup<Startup>(); });
        }
    }
}