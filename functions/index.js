// Importar dependencias necesarias
const functions = require("firebase-functions");
const admin = require("firebase-admin");
const express = require("express");
const cors = require("cors");
const axios = require("axios");

// Inicializa Firebase Admin SDK
admin.initializeApp();

// Crea la app REST
const app = express();
// Permite leer bodies JSON y habilita CORS para todas las rutas
app.use(cors({ origin: true }));
app.use(express.json());

// Acceso a Firestore
const db = admin.firestore();

// Auth middleware: verifies Firebase ID Token
const authenticate = async (req, res, next) => {
  const authHeader = req.headers.authorization;
  if (!authHeader || !authHeader.startsWith("Bearer ")) {
    return res.status(401).json({ error: "Missing or invalid Authorization header" });
  }

  try {
    const token = authHeader.split("Bearer ")[1];
    const decodedToken = await admin.auth().verifyIdToken(token);
    req.uid = decodedToken.uid;
    next();
  } catch (error) {
    return res.status(401).json({ error: "Invalid or expired token" });
  }
};

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

const WEEKLY_CREDITS = 5;
const WEEK_MILLIS = 7 * 24 * 60 * 60 * 1000;

app.post("/generate-missions", authenticate, async (req, res) => {
  const { prompt } = req.body;
  const uid = req.uid;

  if (!prompt) {
    return res.status(400).json({ error: "Missing prompt" });
  }

  const userRef = db.collection("users").doc(uid);

  try {
    await db.runTransaction(async (transaction) => {
      const snapshot = await transaction.get(userRef);
      if (!snapshot.exists) {
        throw new Error("USER_NOT_FOUND");
      }

      const user = snapshot.data();
      const now = Date.now();
      const needsReset = !user.creditsResetDate || user.creditsResetDate < now;
      const currentCredits = needsReset ? WEEKLY_CREDITS : (user.availableCredits ?? WEEKLY_CREDITS);

      if (currentCredits <= 0) {
        throw new Error("NO_CREDITS");
      }

      const updates = {
        availableCredits: currentCredits - 1,
      };
      if (needsReset) {
        updates.creditsResetDate = now + WEEK_MILLIS;
      }
      transaction.update(userRef, updates);
    });

    const openaiKey = process.env.OPENAI_API_KEY;
    if (!openaiKey) {
      await db.runTransaction(async (transaction) => {
        const snapshot = await transaction.get(userRef);
        const user = snapshot.data();
        transaction.update(userRef, {
          availableCredits: Math.min((user.availableCredits ?? 0) + 1, WEEKLY_CREDITS),
        });
      });
      return res.status(500).json({ error: "OpenAI API key not configured" });
    }

    const openaiResponse = await axios.post(
      "https://api.openai.com/v1/chat/completions",
      {
        model: "gpt-4o-mini",
        messages: [{ role: "user", content: prompt }],
        temperature: 0.7,
      },
      {
        headers: { Authorization: `Bearer ${openaiKey}` },
      }
    );

    const content = openaiResponse.data.choices[0].message.content;

    const updatedUser = await userRef.get();
    const credits = updatedUser.data().availableCredits ?? 0;

    return res.status(200).json({ content, availableCredits: credits });
  } catch (error) {
    if (error.message !== "NO_CREDITS" && error.message !== "USER_NOT_FOUND") {
      try {
        await db.runTransaction(async (transaction) => {
          const snapshot = await transaction.get(userRef);
          if (snapshot.exists) {
            const user = snapshot.data();
            transaction.update(userRef, {
              availableCredits: Math.min((user.availableCredits ?? 0) + 1, WEEKLY_CREDITS),
            });
          }
        });
      } catch (refundError) {
        console.error("Credit refund failed:", refundError);
      }
    }

    if (error.message === "NO_CREDITS") {
      return res.status(429).json({ error: "No credits available" });
    }
    if (error.message === "USER_NOT_FOUND") {
      return res.status(404).json({ error: "User not found" });
    }

    console.error("generate-missions error:", error.response?.data || error.message);
    return res.status(500).json({ error: "Failed to generate missions" });
  }
});

const { defineSecret } = require("firebase-functions/params");
const openaiKey = defineSecret("OPENAI_API_KEY");

exports.app = functions.https.onRequest(
  { secrets: [openaiKey], invoker: "public" },
  (req, res) => app(req, res)
);