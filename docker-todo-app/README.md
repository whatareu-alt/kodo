# 🐳 Docker Todo App

A beginner-friendly project to learn Docker using a **Node.js + Express REST API** backed by **PostgreSQL**, orchestrated with **Docker Compose**.

## Concepts You'll Learn

| Docker Concept | Where It's Used |
|---|---|
| `Dockerfile` | Describes how to build the Node.js image |
| Multi-stage builds | `deps` stage installs, `production` stage is lean |
| Layer caching | `COPY package.json` before `COPY src/` |
| `.dockerignore` | Excludes `node_modules`, `.env` from image |
| `docker-compose.yml` | Defines `app` + `db` services |
| Named volumes | `postgres_data` — data persists across restarts |
| `depends_on` + healthcheck | App waits for DB to be ready |
| `env_file` | Loads `.env` into `app` container |
| Port mapping | `3000:3000` and `5432:5432` |

## Project Structure

```
docker-todo-app/
├── src/
│   └── index.js          # Express API (CRUD for todos)
├── Dockerfile            # Multi-stage build
├── docker-compose.yml    # Orchestrates app + db
├── package.json
├── .env                  # DB credentials & config
└── .dockerignore
```

## 🚀 Running the Project

```bash
# 1. Start both containers (builds the image on first run)
docker compose up --build

# 2. (In a new terminal) Check running containers
docker ps

# 3. To stop everything
docker compose down

# 4. To stop AND wipe the database volume
docker compose down -v
```

## 📡 API Endpoints

Base URL: `http://localhost:3000`

| Method | Endpoint | Description |
|---|---|---|
| `GET` | `/` | Health check |
| `GET` | `/todos` | List all todos |
| `GET` | `/todos/:id` | Get one todo |
| `POST` | `/todos` | Create a todo `{ "title": "..." }` |
| `PATCH` | `/todos/:id` | Toggle completed |
| `DELETE` | `/todos/:id` | Delete a todo |

## 🧪 Try It Out (PowerShell)

```powershell
# Create a todo
Invoke-RestMethod -Uri http://localhost:3000/todos -Method POST `
  -ContentType "application/json" `
  -Body '{"title": "Learn Docker"}'

# List all todos
Invoke-RestMethod -Uri http://localhost:3000/todos

# Toggle completed (replace 1 with actual id)
Invoke-RestMethod -Uri http://localhost:3000/todos/1 -Method PATCH

# Delete a todo
Invoke-RestMethod -Uri http://localhost:3000/todos/1 -Method DELETE
```

## 🔬 Useful Docker Commands to Explore

```bash
# View logs from a specific service
docker compose logs app
docker compose logs db

# Open a shell inside the running app container
docker exec -it todo_app sh

# Connect to Postgres inside the db container
docker exec -it todo_db psql -U todouser -d tododb

# List Docker images
docker images

# List Docker volumes
docker volume ls

# Inspect the named volume
docker volume inspect docker-todo-app_postgres_data
```
