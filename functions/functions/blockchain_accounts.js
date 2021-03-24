const crypto = require("crypto");
const functions = require("firebase-functions");
const admin = require("firebase-admin");
const ecc = require("eosjs-ecc");
const { Api, JsonRpc, RpcError } = require("eosjs");
const { JsSignatureProvider } = require("eosjs/dist/eosjs-jssig"); // development only
const fetch = require("node-fetch"); // node only
const { TextDecoder, TextEncoder } = require("util"); // node only
const { user } = require("firebase-functions/lib/providers/auth");

const IV_LENGTH = 16;
const chainUri = "http://127.0.0.1:8888";
const tokenAccount = "nsntoken";
const eosAccount = "eosio";
const transactionOptions = {
  blocksBehind: 3,
  expireSeconds: 30,
};
const alphabet = new Set("12345abcdefghijklmnopqrstuvwxyz".split(""));
const systemConfig = functions.config().nftsocialnet;
const systemEncryptionKey = systemConfig.system_encryption_key;
const eosPrivateKey = systemConfig.eos_private_key;
const nsnTokenPrivateKey = systemConfig.nsn_private_key;
const rpc = new JsonRpc(chainUri, { fetch });
const eosAPI = new Api({
  rpc,
  signatureProvider: new JsSignatureProvider([eosPrivateKey]),
  textDecoder: new TextDecoder(),
  textEncoder: new TextEncoder(),
});
const nsnTokenAPI = new Api({
  rpc,
  signatureProvider: new JsSignatureProvider([nsnTokenPrivateKey]),
  textDecoder: new TextDecoder(),
  textEncoder: new TextEncoder(),
});

function getEncryptionKey(password) {
  return crypto
    .createHash("sha256")
    .update(systemEncryptionKey + password)
    .digest();
}

function encryptPrivateKey(privateKey, password) {
  let encryptionKey = getEncryptionKey(password);
  let iv = crypto.randomBytes(IV_LENGTH);
  let cipher = crypto.createCipheriv("aes256", encryptionKey, iv);
  return {
    iv: iv.toString("hex"),
    encryptedPK:
      cipher.update(privateKey, "binary", "hex") + cipher.final("hex"),
  };
}

function decryptPrivateKey(encryptedPrivateKey, iv, password) {
  let encryptionKey = getEncryptionKey(password);
  let decipher = crypto.createDecipheriv(
    "aes256",
    encryptionKey,
    Buffer.from(iv, "hex")
  );
  return (
    decipher.update(encryptedPrivateKey, "hex", "binary") +
    decipher.final("binary")
  );
}

function validateEOSName(username) {
  if (!username || username.length != 12) {
    return false;
  }
  validatingUsername = username.toLowerCase();
  for (let c of validatingUsername) {
    if (!alphabet.has(c)) {
      return false;
    }
  }
  return validatingUsername;
}

const issueTokensToNewUser = async (username) => {
  functions.logger.log("Issued token to", username);
  const transaction = {
    actions: [
      {
        account: tokenAccount,
        name: "transfer",
        authorization: [
          {
            actor: tokenAccount,
            permission: "active",
          },
        ],
        data: {
          from: tokenAccount,
          to: username,
          quantity: "100.0000 NSN",
          memo: "Welcome gift.",
        },
      },
    ],
  };
  const transactionResult = await nsnTokenAPI.transact(
    transaction,
    transactionOptions
  );
  functions.logger.log(
    "Issued token to",
    username,
    "Transactioni result",
    transactionResult
  );
  return transactionResult;
};

const createBlockchainAccount = async (username, publicKey) => {
  functions.logger.log("Creating user", username);
  const transaction = {
    actions: [
      {
        account: eosAccount,
        name: "newaccount",
        authorization: [
          {
            actor: eosAccount,
            permission: "active",
          },
        ],
        data: {
          creator: eosAccount,
          name: username,
          owner: {
            threshold: 1,
            keys: [
              {
                key: publicKey,
                weight: 1,
              },
            ],
            accounts: [],
            waits: [],
          },
          active: {
            threshold: 1,
            keys: [
              {
                key: publicKey,
                weight: 1,
              },
            ],
            accounts: [],
            waits: [],
          },
        },
      },
    ],
  };
  let transactionResult = await eosAPI.transact(
    transaction,
    transactionOptions
  );
  functions.logger.log(
    "Created user",
    username,
    "Transactioni result",
    transactionResult
  );
  return transactionResult;
};

const createAccount = async (userId, username, password) => {
  let privateKey = await ecc.randomKey();
  let publicKey = ecc.privateToPublic(privateKey);
  let { iv, encryptedPK } = encryptPrivateKey(privateKey, password);
  username = validateEOSName(username);
  if (!username) {
    throw new functions.https.HttpsError(
      "invalid-argument",
      "The username is not valid."
    );
  }
  try {
    await createBlockchainAccount(username, publicKey);
  } catch (e) {
    if (e.json && e.json.error && e.json.error.code == "3050001") {
      functions.logger.log(
        "Failed to create block chain account for user",
        userId,
        "using username",
        username + ".",
        "Username is already taken."
      );
      throw new functions.https.HttpsError(
        "already-exists",
        "the username is already existed"
      );
    } else {
      functions.logger.log(
        "Failed to create block chain account for user",
        userId,
        "using username",
        username,
        "error",
        e
      );
      throw new functions.https.HttpsError(
        "internal",
        "failed to create blockchain user"
      );
    }
  }

  try {
    await issueTokensToNewUser(username);
  } catch (e) {
    functions.logger.log("failed to issue welcome gift", userId, "error", e);
    throw new functions.https.HttpsError(
      "internal",
      "failed to issue welcome gift"
    );
  }

  await admin.firestore().collection("users").doc(userId).update({
    username: username,
    privateKeyIV: iv,
    privateKey: encryptedPK,
    publicKey: publicKey,
  });
};

exports.createAccount = functions.https.onCall(async (data, context) => {
  if (!context.auth) {
    throw new functions.https.HttpsError(
      "unauthenticated",
      "The function must be called " + "while authenticated."
    );
  }
  let uid = context.auth.uid;
  functions.logger.log("Callling user", uid);
  let username = data.username;
  let password = data.password;
  try {
    await createAccount(uid, username, password);
  } catch (e) {
    functions.logger.log(
      "Failed to create block chain account for user",
      uid,
      "using username",
      username,
      e
    );
    if (e.json && e.json.code == "3050001") {
      throw new functions.https.HttpsError(
        "already-exists",
        "the username is already existed"
      );
    } else {
      throw new functions.https.HttpsError(
        "internal",
        "failed to create blockchain user"
      );
    }
  }
  return {
    code: 200,
    messagae: "success",
  };
});

exports.createAccountTesting = functions.firestore
  .document("/users/{userId}")
  .onCreate(async (snap, context) => {
    const newData = snap.data();
    var publicKey = "";
    try {
      publicKey = await createAccount(
        newData.uid,
        newData.username,
        newData.password
      );
    } catch (e) {}
    functions.logger.log(`Returning ${publicKey}`);
    return {
      publicKey: publicKey,
    };
  });
