const functions = require("firebase-functions");

const { Api, JsonRpc } = require("eosjs");
const { JsSignatureProvider } = require("eosjs/dist/eosjs-jssig"); // development only
const fetch = require("node-fetch"); // node only
const { TextDecoder, TextEncoder } = require("util"); // node only

const chainUri = "http://127.0.0.1:8888";
const tokenAccount = "nsntoken";
const transactionOptions = {
  blocksBehind: 3,
  expireSeconds: 30,
};

let privateKey = process.env["nsntoken_private_key"];
const nsntokenConfig = functions.config().nsntoken;
if (nsntokenConfig) {
  privateKey = nsntokenConfig.private_key;
}
functions.logger.log("Private key", privateKey);

const privateKeys = [privateKey];

const signatureProvider = new JsSignatureProvider(privateKeys);
const rpc = new JsonRpc(chainUri, { fetch });
const api = new Api({
  rpc,
  signatureProvider,
  textDecoder: new TextDecoder(),
  textEncoder: new TextEncoder(),
});

const admin = require("firebase-admin");
admin.initializeApp();

exports.userCreated = functions.auth.user().onCreate(async (user) => {
  const fullName = user.displayName || "Anonymous";
  const text = `Hello there ${fullName}`;
  const payload = {
    notification: {
      title: "Welcome to NSN!",
      body: text
        ? text.length <= 100
          ? text
          : text.substring(0, 97) + "..."
        : "",
      click_action: `https://facebook.com`,
    },
  };
  // Get the list of device tokens.
  const allTokens = await admin.firestore().collection("fcmTokens").get();
  const tokens = [];
  allTokens.forEach((tokenDoc) => {
    tokens.push(tokenDoc.id);
  });
  if (tokens.length > 0) {
    // Send notifications to all tokens.
    const response = await admin.messaging().sendToDevice(tokens, payload);
    await cleanupTokens(response, tokens);
    console.log("Notifications have been sent and tokens cleaned up.");
  }
});

function cleanupTokens(response, tokens) {
  // For each notification we check if there was an error.
  const tokensDelete = [];
  response.results.forEach((result, index) => {
    const error = result.error;
    if (error) {
      console.error("Failure sending notification to", tokens[index], error);
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

exports.postCreated = functions.firestore
  .document("/posts/{postId}")
  .onCreate(async (snap, context) => {
    const original = snap.data().original;
    const newData = snap.data().document;
    functions.logger.log(
      "Receive new post",
      context.params.postId,
      original,
      newData
    );

    // Checking balance
    const beforeBalance = await rpc.get_currency_balance(
      "nsntoken",
      "alice",
      "NSN"
    );
    functions.logger.log("Before balanace", beforeBalance);

    // Transfer welcome tokens
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
            to: "alice",
            quantity: "1.0000 NSN",
            memo: "Test from EOSJS",
          },
        },
      ],
    };

    const transactionResult = await api.transact(
      transaction,
      transactionOptions
    );
    functions.logger.log("Transactioni result", transactionResult);

    const afterBalance = await rpc.get_currency_balance(
      "nsntoken",
      "alice",
      "NSN"
    );
    functions.logger.log("After balanace", afterBalance);

    const result = await snap.ref.set(
      {
        rank: 0,
      },
      { merge: true }
    );
    functions.logger.log("Result", result);
    return result;
  });
