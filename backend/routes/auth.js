import express from "express";
import bcrypt from "bcryptjs";
import jwt from "jsonwebtoken";
import pool from "../config/db.js";
import { protect } from "../middleware/auth.js";
// import { findUserByEmail, createUser } from "../models/userModel.js";
import UserModel from "../models/userModel.js";

const router = express.Router();

const cookieOptions = {
  httpOnly: true,
  secure: process.env.NODE_ENV === "production",
  sameSite: "Strict",
  maxAge: 30 * 24 * 60 * 60 * 1000, // 30 days
};

const generateToken = (id) => {
  return jwt.sign({ id }, process.env.JWT_SECRET, {
    expiresIn: "30d",
  });
};

// Register

router.post("/register", async (req, res) => {
  const { name, email, password } = req.body;

  if (!name || !email || !password) {
    return res
      .status(400)
      .json({ message: "Please provide all required fields" });
  }

  // const userExists = await pool.query("SELECT * FROM users WHERE email = $1", [
  //   email,
  // ]);

  // if (userExists.rows.length > 0) {
  //   return res.status(400).json({ message: "User already exists" });
  // }
  // const existingUser = await findUserByEmail(email);
  const existingUser = await UserModel.findOne({ email });

    if (existingUser) {
      return res.status(400).json({ message: "User already exists" });
    }

  const hashedPassword = await bcrypt.hash(password, 10);

  // const newUser = await pool.query(
  //   "INSERT INTO users (name, email, password) VALUES ($1, $2, $3) RETURNING id, name, email",
  //   [name, email, hashedPassword]
  // );

  const newUser = await UserModel.create({
  name: name,
  email: email,
  password: hashedPassword
});

  const token = generateToken(newUser.id);

  res.cookie("token", token, cookieOptions);

  return res.status(201).json({ user: newUser });
});

// Login
// Login
router.post("/login", async (req, res) => {
  const { email, password } = req.body;
  if (!email || !password) {
    return res
      .status(400)
      .json({ message: "Please provide all required fields" });
  }

  const user = await UserModel.findOne({ email });

  if (!user) {
    return res.status(400).json({ message: "User không tồn tại" });
  }


  const isMatch = await bcrypt.compare(password, user.password);

  if (!isMatch) {
    return res.status(400).json({ message: "Invalid credentials" });
  }

  const token = generateToken(user.id);

  res.cookie("token", token, cookieOptions);

  res.json({
    user: {
      id: user.id,
      name: user.name,
      email: user.email,
    },
  });
});

// Me
router.get("/me", protect, async (req, res) => {
  res.json(req.user);
  // return info of the logged in user from protect middleware
});

// Logout
router.post("/logout", (req, res) => {
  res.cookie("token", "", { ...cookieOptions, maxAge: 1 });
  res.json({ message: "Logged out successfully" });
});

// delete

router.delete("/remove", protect, async (req, res) => {
  try {
    const deletedUser = await UserModel.remove({
      id: req.user.id,
    });

    if (!deletedUser) {
      return res.status(404).json({
        message: "User not found",
      });
    }

    // Xóa cookie đăng nhập sau khi xóa tài khoản
    res.clearCookie("token", cookieOptions);

    return res.status(200).json({
      message: "User deleted successfully",
      user: {
        id: deletedUser.id,
        name: deletedUser.name,
        email: deletedUser.email,
      },
    });
  } catch (error) {
    console.error("Remove user error:", error);

    return res.status(500).json({
      message: "Internal server error",
    });
  }
});


 // Update user

router.put("/update", protect, async (req, res) => {
  try {
    const { name, password } = req.body;

    const dataToUpdate = {};

    if (name !== undefined) {
      dataToUpdate.name = name;
    }

    if (password !== undefined && password !== "") {
      dataToUpdate.password = await bcrypt.hash(password, 10);
    }

    if (Object.keys(dataToUpdate).length === 0) {
      return res.status(400).json({
        message: "Please provide name or password to update",
      });
    }

    // Dùng hàm an toàn trong userModel.js
    const updatedUser = await UserModel.safeUpdateUserByEmail(
      req.user.email,
      dataToUpdate
    );

    if (!updatedUser) {
      return res.status(404).json({
        message: "User not found",
      });
    }

    const { password: _, ...userWithoutPassword } = updatedUser;

    return res.status(200).json({
      message: "User updated successfully",
      user: userWithoutPassword,
    });
  } catch (error) {
    console.error("Update user error:", error);

    return res.status(500).json({
      message: "Internal server error",
    });
  }
});


export default router;