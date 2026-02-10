# Local Registry

The Local Registry is an in-memory registry intended for single-process development or testing.

## How It Works
- Provider registers instances into local memory
- Consumer discovers via in-memory cache
- No external dependency (Nacos not required)

## Enable Local Registry
Set either:
- `aoaojiao.rpc.registry.enabled=false`
- OR leave `aoaojiao.rpc.registry.serverAddr` blank
- OR set `aoaojiao.rpc.registry.mode=local`

## Boot Profiles
Use the pre-built profiles:
- `application-local.yml`
- `scripts/run-boot-examples-local.sh`

## Diagnostics
Use `RegistryDiagnostics` to print:
- health: `RegistryDiagnostics.health(registryService)`
- snapshot: `RegistryDiagnostics.snapshot(registryService)`

HTTP endpoints (Boot examples):
- provider: `http://localhost:18080/health.json`
- consumer: `http://localhost:18081/health.json`
