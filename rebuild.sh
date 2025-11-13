# 1. Parar todo
docker compose down

# 2. Reconstruir imagen de la API sin cache
docker compose build --no-cache api

# 3. Levantar todo de nuevo
docker compose up -d

# 4. Ver logs de la api en vivo
docker compose logs -f api
