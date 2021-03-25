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
const tokenAccount = "nsntoken";
const eosAccount = "eosio";
const transactionOptions = {
  blocksBehind: 3,
  expireSeconds: 30,
};
const systemConfig = functions.config().nftsocialnet;
const systemEncryptionKey = systemConfig.system_encryption_key;
const eosPrivateKey = systemConfig.eos_private_key;
const nsnTokenPrivateKey = systemConfig.nsn_private_key;
const chainUri = systemConfig.chain_uri;
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

exports.listPostForSale = functions.https.onCall(async (data, context) => {
  if (!context.auth) {
    throw new functions.https.HttpsError(
      "unauthenticated",
      "The function must be called " + "while authenticated."
    );
  }
  let uid = context.auth.uid;
  let postId = data.postId;
  return {
    code: 200,
    data: {
      messagae: "success",
    },
  };
});

exports.purchasePost = functions.https.onCall(async (data, context) => {
  if (!context.auth) {
    throw new functions.https.HttpsError(
      "unauthenticated",
      "The function must be called " + "while authenticated."
    );
  }
  let uid = context.auth.uid;
  let postId = data.postId;
  return {
    code: 200,
    data: {
      messagae: "success",
    },
  };
});
