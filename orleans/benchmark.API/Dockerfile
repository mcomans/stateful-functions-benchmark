FROM mcr.microsoft.com/dotnet/aspnet:6.0 AS base
WORKDIR /app
EXPOSE 80
EXPOSE 443

FROM mcr.microsoft.com/dotnet/sdk:6.0 AS build
WORKDIR /src
COPY ["benchmark.API/benchmark.API.csproj", "benchmark.API/"]
RUN dotnet restore "benchmark.API/benchmark.API.csproj"
COPY . .
WORKDIR "/src/benchmark.API"
RUN dotnet build "benchmark.API.csproj" -c Release -o /app/build

FROM build AS publish
RUN dotnet publish "benchmark.API.csproj" -c Release -o /app/publish

FROM base AS final
WORKDIR /app
COPY --from=publish /app/publish .
ENTRYPOINT ["dotnet", "benchmark.API.dll"]
