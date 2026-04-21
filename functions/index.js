// Importar dependencias necesarias
const functions = require("firebase-functions");
const admin = require("firebase-admin");
const express = require("express");
const cors = require("cors");

// Inicializa Firebase Admin SDK
admin.initializeApp();

// Crea la app REST
const app = express();
// Permite leer bodies JSON y habilita CORS para todas las rutas
app.use(cors({ origin: true }));
app.use(express.json());

// Acceso a Firestore
const db = admin.firestore();

// Endpoint de prueba
app.get("/hello-world", (req, res) => {
  return res.status(200).send("Hello world!");
});

// Registrar usuario
app.post("/register", async (req, res) => {
  try {
    const { email, password, username } = req.body;

    console.log("BODY RECEIVED:", req.body);

    if (!email || !password || !username) {
      console.log("Missing fields");
      return res.status(400).json({ error: "Missing fields" });
    }

    console.log("Creating auth user...");

    const userRecord = await admin.auth().createUser({
      email,
      password,
      displayName: username,
    });

    console.log("Auth user created:", userRecord.uid);

    const userData = {
      id: userRecord.uid,
      email,
      username,
      createdAt: new Date().toISOString(),
    };

    console.log("Saving user in Firestore...");

    await db.collection("users").doc(userRecord.uid).set(userData);

    console.log("User saved in Firestore");

    return res.status(201).json(userData);
  } catch (error) {
    console.error("REGISTER ERROR FULL:", error);
    return res.status(500).json({
      error: error.message,
      code: error.code || null
    });
  }
});

/**
//Login Users (Debug)
app.post("/login", async (req, res) => {
  try {
    console.log("LOGIN BODY:", req.body);

    const { email, password } = req.body;

    console.log("EMAIL:", email);
    console.log("PASSWORD:", password);

    if (!email || !password) {
      return res.status(400).json({ message: "Missing email or password" });
    }

    const snapshot = await db.collection("users")
      .where("email", "==", email)
      .limit(1)
      .get();

    console.log("MATCHING USERS:", snapshot.size);

    if (snapshot.empty) {
      return res.status(401).json({ message: "Invalid credentials" });
    }

    const userDoc = snapshot.docs[0];
    const user = userDoc.data();

    console.log("USER IN DB:", user);

    if (user.password !== password) {
      console.log("PASSWORD DOES NOT MATCH");
      return res.status(401).json({ message: "Invalid credentials" });
    }

    return res.status(200).json({
      id: userDoc.id,
      email: user.email,
      username: user.username
    });

  } catch (error) {
    console.error("LOGIN ERROR:", error);
    return res.status(500).json({ message: "Internal server error" });
  }
});
*/


// Obtener todos los usuarios (DEBUG)
app.get("/users", async (req, res) => {
  try {
    const snapshot = await db.collection("users").get();
    const users = snapshot.docs.map((doc) => doc.data());

    return res.status(200).json(users);
  } catch (error) {
    return res.status(500).json({ error: error.message });
  }
});

exports.app = functions.https.onRequest(app);