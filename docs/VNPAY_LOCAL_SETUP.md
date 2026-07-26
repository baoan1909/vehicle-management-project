# Local VNPAY Setup With Docker Ngrok

This setup exposes the backend running on the host machine at port `8080`.
The ngrok CLI does not need to be installed on Windows.

## 1. Configure local secrets

Create `vehicle-management-backend/vehicle-management/.env` from `.env.example`
and provide values from your own ngrok account:

```env
NGROK_AUTHTOKEN=your_ngrok_authtoken
NGROK_DOMAIN=your-domain.ngrok-free.dev

VNPAY_ENABLED=true
VNPAY_TMN_CODE=your_vnpay_tmn_code
VNPAY_HASH_SECRET=your_vnpay_hash_secret
VNPAY_IPN_URL=https://your-domain.ngrok-free.dev/vehicle-management/api/public/payments/vnpay/ipn
```

`NGROK_DOMAIN` and the domain in `VNPAY_IPN_URL` must be identical.
Do not commit `.env`, authtokens, or VNPAY secrets.

## 2. Start the backend

Run the Spring Boot backend on the host machine at port `8080`.

## 3. Start the tunnel

From `vehicle-management-backend/vehicle-management`, run:

```powershell
docker compose --env-file .env -f docker-compose.ngrok.yml up -d
```

Inspect requests at:

```text
http://localhost:4040
```

View container logs:

```powershell
docker compose --env-file .env -f docker-compose.ngrok.yml logs -f ngrok
```

## 4. Stop the tunnel

```powershell
docker compose --env-file .env -f docker-compose.ngrok.yml down
```

Each developer should use a separate ngrok token and domain. Two independent
local databases must not share one public callback domain at the same time.
