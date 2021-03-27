const functions = require("firebase-functions");
const ecc = require("eosjs-ecc");
const admin = require("firebase-admin");

const {
  eosAccount,
  tokenAccount,
  welcomeBonus,
  eosAPI,
  tokenAPI,
  transactionOptions,
  validateEOSName,
  encryptPrivateKey,
  rpc,
} = require("./utils");

const issueBonusToNewUser = async (username) => {
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
          quantity: `${welcomeBonus} EOS`,
          memo: "Welcome gift.",
        },
      },
    ],
  };
  const transactionResult = await tokenAPI.transact(
    transaction,
    transactionOptions
  );
  functions.logger.log(
    "Issued token to",
    username,
    "Transactioni result",
    transactionResult
  );
  return transactionResult.transaction_id;
};

const createBlockchainAccount = async (username, publicKey) => {
  functions.logger.log("Creating user", username);
  let transaction = {
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
  return transactionResult.transaction_id;
};

const createAccount = async (userId, username, password) => {
  let privateKey = await ecc.randomKey();
  let publicKey = ecc.privateToPublic(privateKey);
  let { iv, encryptedPK } = encryptPrivateKey(privateKey, password);
  username = validateEOSName(username);
  if (!username) {
    throw new functions.https.HttpsError(
      "invalid-argument",
      "The username is not valid.",
      "The username is not valid."
    );
  }
  try {
    let trxId = await createBlockchainAccount(username, publicKey);
    await admin.firestore().collection("transactions").doc(trxId).set({
      type: "userCreated",
      id: trxId,
      userId: userId,
      timestamp: Date.now(),
    });
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
        "The username is already existed. Please choose another one!",
        "The username is already existed. Please choose another one!"
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
        "Failed to create blockchain user. Please try again!",
        "Failed to create blockchain user. Please try again!"
      );
    }
  }

  try {
    let issueTrxId = await issueBonusToNewUser(username);
    await admin
      .firestore()
      .collection("transactions")
      .doc(issueTrxId)
      .set({
        type: "eosReceived",
        id: issueTrxId,
        userId: userId,
        timestamp: Date.now(),
        data: {
          from: tokenAccount,
          quantity: `${welcomeBonus} EOS`,
          memo: "Welcome gift.",
        },
      });
  } catch (e) {
    functions.logger.log("failed to issue welcome gift", userId, "error", e);
    throw new functions.https.HttpsError(
      "internal",
      "Failed to issue welcome gift. Please try again!",
      "Failed to issue welcome gift. Please try again!"
    );
  }
  await admin.firestore().collection("users").doc(userId).update({
    username: username,
    privateKeyIV: iv,
    privateKey: encryptedPK,
    publicKey: publicKey,
    eosAmount: welcomeBonus,
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
  let username = data.username;
  let password = data.password;
  await createAccount(uid, username, password);
  return {
    code: 200,
  };
});

exports.getAccountBalance = functions.https.onCall(async (data, context) => {
  if (!context.auth) {
    throw new functions.https.HttpsError(
      "unauthenticated",
      "The function must be called while authenticated.",
      "The function must be called while authenticated."
    );
  }
  let username = data.username;
  const balance = await rpc.get_currency_balance(tokenAccount, username, "EOS");
  return {
    code: 200,
    data: {
      balance: balance,
    },
  };
});

// exports.createAccountTesting = functions.firestore
//   .document("/users/{userId}")
//   .onCreate(async (snap, context) => {
//     const newData = snap.data();
//     var publicKey = "";
//     try {
//       publicKey = await createAccount(
//         newData.uid,
//         newData.username,
//         newData.password
//       );
//     } catch (e) {}
//     functions.logger.log(`Returning ${publicKey}`);
//     return {
//       publicKey: publicKey,
//     };
//   });
