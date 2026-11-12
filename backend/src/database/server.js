import express from "express";
import cors from "cors";
import dotenv from "dotenv";
import cookieParser from "cookie-parser";
import authRouters from "./routes/auth.js";
import { createUserTable, createFriendshipsTable } from './config/db.js';

dotenv.config();
const app = express();

app.use(cors({
    origin: "*"
}));
app.use(express.json());
app.use(cookieParser());

app.use("/api/auth", authRouters);

app.get("/", (req, res) =>{
    res.send("Hello World!");
})

const PORT = process.env.PORT ||5000;

// app.listen(PORT, () => {
//     console.log("Server is running on port 5000");
// })

const startServer = async () => {
    try{
        await createUserTable();
        await createFriendshipsTable();
        app.listen(PORT, () => {
            console.log(`Server is running on port ${PORT}`);
        });
    } catch (error) {
        console.error("Failed to initialize the database", error);
        process.exit(1);
    }
}

startServer();