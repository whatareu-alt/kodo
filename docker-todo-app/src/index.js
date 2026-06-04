require('dotenv').config();
const express = require('express');
const { Pool } = require('pg');

const app = express();
app.use(express.json());

// ------------------------------------------------------------------
// Database connection pool
// ------------------------------------------------------------------
const pool = new Pool({ connectionString: process.env.DATABASE_URL });

// Retry connecting to Postgres (it may take a moment to start up)
async function connectWithRetry(retries = 10, delay = 2000) {
    for (let i = 0; i < retries; i++) {
        try {
            await pool.query('SELECT 1');
            console.log('✅ Connected to PostgreSQL');
            return;
        } catch (err) {
            console.log(`⏳ Waiting for database... (attempt ${i + 1}/${retries})`);
            await new Promise((r) => setTimeout(r, delay));
        }
    }
    throw new Error('❌ Could not connect to the database after multiple retries.');
}

// Create the todos table if it does not already exist
async function initDb() {
    await pool.query(`
    CREATE TABLE IF NOT EXISTS todos (
      id        SERIAL PRIMARY KEY,
      title     TEXT    NOT NULL,
      completed BOOLEAN NOT NULL DEFAULT false,
      created_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
    )
  `);
    console.log('✅ Database initialized');
}

// ------------------------------------------------------------------
// Routes
// ------------------------------------------------------------------

// Health check
app.get('/', (req, res) => {
    res.json({ message: '🐳 Docker Todo API is running!' });
});

// GET /todos — list all todos
app.get('/todos', async (req, res) => {
    try {
        const result = await pool.query('SELECT * FROM todos ORDER BY created_at DESC');
        res.json(result.rows);
    } catch (err) {
        res.status(500).json({ error: err.message });
    }
});

// GET /todos/:id — get a single todo
app.get('/todos/:id', async (req, res) => {
    try {
        const result = await pool.query('SELECT * FROM todos WHERE id = $1', [req.params.id]);
        if (result.rows.length === 0) return res.status(404).json({ error: 'Todo not found' });
        res.json(result.rows[0]);
    } catch (err) {
        res.status(500).json({ error: err.message });
    }
});

// POST /todos — create a new todo
app.post('/todos', async (req, res) => {
    const { title } = req.body;
    if (!title) return res.status(400).json({ error: 'Title is required' });
    try {
        const result = await pool.query(
            'INSERT INTO todos (title) VALUES ($1) RETURNING *',
            [title]
        );
        res.status(201).json(result.rows[0]);
    } catch (err) {
        res.status(500).json({ error: err.message });
    }
});

// PATCH /todos/:id — toggle completed status
app.patch('/todos/:id', async (req, res) => {
    try {
        const result = await pool.query(
            'UPDATE todos SET completed = NOT completed WHERE id = $1 RETURNING *',
            [req.params.id]
        );
        if (result.rows.length === 0) return res.status(404).json({ error: 'Todo not found' });
        res.json(result.rows[0]);
    } catch (err) {
        res.status(500).json({ error: err.message });
    }
});

// DELETE /todos/:id — delete a todo
app.delete('/todos/:id', async (req, res) => {
    try {
        const result = await pool.query('DELETE FROM todos WHERE id = $1 RETURNING *', [req.params.id]);
        if (result.rows.length === 0) return res.status(404).json({ error: 'Todo not found' });
        res.json({ message: 'Todo deleted', todo: result.rows[0] });
    } catch (err) {
        res.status(500).json({ error: err.message });
    }
});

// ------------------------------------------------------------------
// Start server
// ------------------------------------------------------------------
const PORT = process.env.PORT || 3000;

(async () => {
    await connectWithRetry();
    await initDb();
    app.listen(PORT, () => {
        console.log(`🚀 Server running on http://localhost:${PORT}`);
    });
})();
