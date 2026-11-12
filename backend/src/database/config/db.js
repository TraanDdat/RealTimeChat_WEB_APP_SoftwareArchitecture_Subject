import pg from "pg";
import dotenv from 'dotenv';

import path from "node:path";
import { fileURLToPath } from "node:url";

const __filename = fileURLToPath(import.meta.url);
const __dirname = path.dirname(__filename);

dotenv.config({
    path: path.resolve(__dirname, "../.env")
});

const pool = new pg.Pool({
    host: process.env.DB_HOST,
    port: process.env.DB_PORT,
    user: process.env.DB_USER,
    database: process.env.DB_NAME,
    password: process.env.DB_PASSWORD,
    connectionTimeoutMillis: 5000,
    max: 10
});

pool.on("connect", () =>{
    console.log("Connected to the database");
});

pool.on("error", (err) =>{
    console.log("Databse error", err);
});

export const createUserTable = async () => {
    await pool.query(`
        CREATE TABLE IF NOT EXISTS users (
            id SERIAL PRIMARY KEY,
            email VARCHAR(255) NOT NULL UNIQUE,
            username VARCHAR(50) NOT NULL,
            password VARCHAR(255) NOT NULL,
            role VARCHAR(255),
            profile_pic VARCHAR(500)
        )
    `);
    console.log("users table is ready");
}

export const createFriendshipsTable = async () => {
    await pool.query(`
        CREATE TABLE IF NOT EXISTS friendships (
            id SERIAL PRIMARY KEY,
            requester_id INTEGER NOT NULL REFERENCES users(id) ON DELETE CASCADE,
            addressee_id INTEGER NOT NULL REFERENCES users(id) ON DELETE CASCADE,
            status VARCHAR(20) NOT NULL DEFAULT 'pending',
            created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
            updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,

            CHECK (requester_id <> addressee_id),
            CHECK (status IN ('pending', 'accepted', 'rejected', 'blocked'))
        )
    `);

    // Prevent duplicate relationships in either direction
    await pool.query(`
        CREATE UNIQUE INDEX IF NOT EXISTS unique_friendship_pair
        ON friendships (
            LEAST(requester_id, addressee_id),
            GREATEST(requester_id, addressee_id)
        )
    `);

    console.log("friendships table is ready");
};

export default pool;