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

    if (!email || !password || !username) {
      return res.status(400).json({ error: "Missing fields" });
    }

    const userRecord = await admin.auth().createUser({
      email,
      password,
      displayName: username,
    });

    const userData = {
      id: userRecord.uid,
      email,
      username,
      createdAt: new Date().toISOString(),
    };

    await db.collection("users").doc(userRecord.uid).set(userData);

    return res.status(201).json(userData);
  } catch (error) {
    return res.status(500).json({ error: error.message });
  }
});

// Obtener todos los usuarios
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