# ── Stage 1: Install dependencies ────────────────────────────────────
FROM node:20-alpine AS deps

WORKDIR /app

# Copy only package files first so this layer is cached
# and only re-runs when dependencies change
COPY package.json ./
RUN npm install --omit=dev

# ── Stage 2: Production image ────────────────────────────────────────
FROM node:20-alpine

WORKDIR /app

# Copy installed modules from Stage 1
COPY --from=deps /app/node_modules ./node_modules

# Copy application source
COPY src/ ./src/

# Copy package.json (needed for npm start)
COPY package.json ./

# Expose the port the app listens on
EXPOSE 3000

# Run the app
CMD ["node", "src/index.js"]
