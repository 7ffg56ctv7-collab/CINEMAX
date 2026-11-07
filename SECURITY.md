# Security policy

- Only HTTPS addon endpoints are accepted by the current client helper, except loopback HTTP for local development.
- Stremio addons are treated as remote HTTP data providers; their responses are parsed as data and never evaluated as code.
- Nuvio JavaScript plugins are not executed in this starter. A sandboxed runtime, strict resource limits and explicit consent are mandatory before enabling them.
- Do not commit credentials, API keys, private addon URLs or user data.
- Use only sources and media you are authorized to access.
