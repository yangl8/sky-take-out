# Sky Take Out

## Local configuration

1. Copy `.env.example` to `.env`.
2. Fill in the local database, JWT, and OSS values in `.env`.
3. In VS Code, run the `SkyApplication` launch configuration.

The `.env` file contains local credentials and is intentionally excluded from Git.

### JWT keys

The application requires two independent HS256 signing keys in the local `.env` file: `JWT_ADMIN_SECRET_KEY` and `JWT_USER_SECRET_KEY`. Generate each value once on macOS with:

```sh
openssl rand -base64 32 | tr -d '\n'
```

Run the command separately for each variable, then put the two outputs in the matching `.env` entries. The command generates 32 random bytes and encodes them as Base64. Keep `.env` local, do not paste either value into tracked files or logs, and do not regenerate keys on each application start: changing a key invalidates tokens signed with its previous value. Startup fails with a configuration error if a key is missing, malformed, too short, or equal to the other key.
