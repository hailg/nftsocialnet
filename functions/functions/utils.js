const crypto = require("crypto");
const { customAlphabet } = require("nanoid");
const functions = require("firebase-functions");
const admin = require("firebase-admin");
const { Api, JsonRpc } = require("eosjs");
const { JsSignatureProvider } = require("eosjs/dist/eosjs-jssig"); // development only
const fetch = require("node-fetch"); // node only
const { TextDecoder, TextEncoder } = require("util"); // node only

const IV_LENGTH = 16;
const alphabet = new Set("12345abcdefghijklmnopqrstuvwxyz".split(""));
const systemConfig = functions.config().nftsocialnet;
const chainUri = systemConfig.chain_uri;
const systemEncryptionKey = systemConfig.system_encryption_key;
const rpc = new JsonRpc(chainUri, { fetch });

function getEncryptionKey(password) {
  return crypto
    .createHash("sha256")
    .update(systemEncryptionKey + password)
    .digest();
}

exports.welcomeBonus = "100.0000";
const eosPrivateKey = systemConfig.eos_private_key;
exports.eosPrivateKey = eosPrivateKey;
const tokenPrivateKey = systemConfig.nsn_private_key;
exports.tokenPrivateKey = tokenPrivateKey;
const dgoodsPrivateKey = systemConfig.dgoods_private_key;
exports.dgoodsPrivateKey = dgoodsPrivateKey;

exports.tokenAccount = "eosio.token";
exports.eosAccount = "eosio";
exports.dgoodsAccount = "dgood.token";
exports.dgoodSymbol = "NSN";
exports.transactionOptions = {
  blocksBehind: 3,
  expireSeconds: 30,
};

exports.rpc = rpc;

exports.eosAPI = new Api({
  rpc,
  signatureProvider: new JsSignatureProvider([eosPrivateKey]),
  textDecoder: new TextDecoder(),
  textEncoder: new TextEncoder(),
});

exports.tokenAPI = new Api({
  rpc,
  signatureProvider: new JsSignatureProvider([tokenPrivateKey]),
  textDecoder: new TextDecoder(),
  textEncoder: new TextEncoder(),
});

exports.dgoodsAPI = new Api({
  rpc,
  signatureProvider: new JsSignatureProvider([dgoodsPrivateKey]),
  textDecoder: new TextDecoder(),
  textEncoder: new TextEncoder(),
});

exports.encryptPrivateKey = (privateKey, password) => {
  let encryptionKey = getEncryptionKey(password);
  let iv = crypto.randomBytes(IV_LENGTH);
  let cipher = crypto.createCipheriv("aes256", encryptionKey, iv);
  return {
    iv: iv.toString("hex"),
    encryptedPK:
      cipher.update(privateKey, "binary", "hex") + cipher.final("hex"),
  };
};

const decryptPrivateKey = (encryptedPrivateKey, iv, password) => {
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
};

exports.validateEOSName = (username) => {
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
};

exports.sendTextNotification = async (tokens, text) => {
  let payload = {
    notification: {
      title: "NSN",
      body: text
        ? text.length <= 100
          ? text
          : text.substring(0, 97) + "..."
        : "",
    },
  };
  let response = await admin.messaging().sendToDevice(tokens, payload);
  await cleanupTokens(response, tokens);
};

exports.sendNotification = async (tokens, payload) => {
  const response = await admin.messaging().sendToDevice(tokens, payload);
  await cleanupTokens(response, tokens);
};

function cleanupTokens(response, tokens) {
  // For each notification we check if there was an error.
  const tokensDelete = [];
  response.results.forEach((result, index) => {
    const error = result.error;
    if (error) {
      functions.logger.error(
        "Failure sending notification to",
        tokens[index],
        error
      );
      // Cleanup the tokens who are not registered anymore.
      if (
        error.code === "messaging/invalid-registration-token" ||
        error.code === "messaging/registration-token-not-registered"
      ) {
        const deleteTask = admin
          .firestore()
          .collection("fcmTokens")
          .doc(tokens[index])
          .delete();
        tokensDelete.push(deleteTask);
      }
    }
  });
  return Promise.all(tokensDelete);
}

exports.eosNameGenerator = customAlphabet(
  ".12345abcdefghijklmnopqrstuvwxyz",
  12
);

exports.createEOSApi = (user, password) => {
  let encryptedPK = user.privateKey;
  let privateKeyIV = user.privateKeyIV;
  try {
    let privateKey = decryptPrivateKey(encryptedPK, privateKeyIV, password);
    return new Api({
      rpc,
      signatureProvider: new JsSignatureProvider([privateKey]),
      textDecoder: new TextDecoder(),
      textEncoder: new TextEncoder(),
    });
  } catch (e) {
    functions.logger.error("Failed to decrypt key", e);
    throw new functions.https.HttpsError(
      "invalid-argument",
      "You enter the wrong wallet password. We cannot unlock it!",
      "You enter the wrong wallet password. We cannot unlock it!"
    );
  }
};
