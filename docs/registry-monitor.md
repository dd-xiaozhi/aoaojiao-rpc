# Registry Monitor

## Periodic Reporter
- `RegistryReporter` prints health + snapshot every N seconds

## HTTP Endpoints
- `GET /health`
- `GET /snapshot`
- `GET /health.json`
- `GET /snapshot.json`

Boot example ports:
- provider: `http://localhost:18080/health`
- consumer: `http://localhost:18081/health`

## Configure Ports
Use properties:
- `aoaojiao.rpc.registry.monitor.enabled` (boolean, default: true)
- `aoaojiao.rpc.registry.monitor.port` (int, default: 18080)
- `aoaojiao.rpc.registry.monitor.intervalSeconds` (int, default: 5)

## Registry Mode
- `aoaojiao.rpc.registry.mode=auto|local|nacos`
