const functions = require("firebase-functions");
const admin = require("firebase-admin");
admin.initializeApp();

// exports.userWritten = functions.firestore
//   .document("/users/{userId}")
//   .onWrite(async (change, context) => {
//     const original = change.before.data();
//     const user = change.after.data();
//     if (!user) {
//       return;
//     }
//     if (original && original.fcmTokens && original.fcmTokens.length > 0) {
//       return;
//     }
//     let newTokens = user.fcmTokens;
//     if (!newTokens || newTokens.length == 0) {
//       return;
//     }
//     const fullName = user.name;
//     const text = `Hello there ${fullName}`;
//     const payload = {
//       notification: {
//         title: "Welcome to NSN!",
//         body: text
//           ? text.length <= 100
//             ? text
//             : text.substring(0, 97) + "..."
//           : "",
//       },
//       data: {
//         event: "New registration",
//       },
//     };
//     const response = await admin.messaging().sendToDevice(newTokens, payload);
//     await cleanupTokens(response, newTokens);
//     functions.logger.log("Notifications have been sent and tokens cleaned up.");
//   });

// function cleanupTokens(response, tokens) {
//   // For each notification we check if there was an error.
//   const tokensDelete = [];
//   response.results.forEach((result, index) => {
//     const error = result.error;
//     if (error) {
//       functions.logger.error(
//         "Failure sending notification to",
//         tokens[index],
//         error
//       );
//       // Cleanup the tokens who are not registered anymore.
//       if (
//         error.code === "messaging/invalid-registration-token" ||
//         error.code === "messaging/registration-token-not-registered"
//       ) {
//         const deleteTask = admin
//           .firestore()
//           .collection("fcmTokens")
//           .doc(tokens[index])
//           .delete();
//         tokensDelete.push(deleteTask);
//       }
//     }
//   });
//   return Promise.all(tokensDelete);
// }

// exports.postCreated = functions.firestore
//   .document("/posts/{postId}")
//   .onCreate(async (snap, context) => {
//     const original = snap.data().original;
//     const newData = snap.data().document;
//     functions.logger.log(
//       "Receive new post",
//       context.params.postId,
//       original,
//       newData
//     );

//     // Checking balance
//     const beforeBalance = await rpc.get_currency_balance(
//       "nsntoken",
//       "alice",
//       "NSN"
//     );
//     functions.logger.log("Before balanace", beforeBalance);

//     // Transfer welcome tokens
//     const transaction = {
//       actions: [
//         {
//           account: tokenAccount,
//           name: "transfer",
//           authorization: [
//             {
//               actor: tokenAccount,
//               permission: "active",
//             },
//           ],
//           data: {
//             from: tokenAccount,
//             to: "alice",
//             quantity: "1.0000 NSN",
//             memo: "Test from EOSJS",
//           },
//         },
//       ],
//     };

//     const transactionResult = await api.transact(
//       transaction,
//       transactionOptions
//     );
//     functions.logger.log("Transactioni result", transactionResult);

//     const afterBalance = await rpc.get_currency_balance(
//       "nsntoken",
//       "alice",
//       "NSN"
//     );
//     functions.logger.log("After balanace", afterBalance);

//     const result = await snap.ref.set(
//       {
//         rank: 0,
//       },
//       { merge: true }
//     );
//     functions.logger.log("Result", result);
//     return result;
//   });

exports.blockchain_account = require("./blockchain_account");
exports.blockchain_post = require("./blockchain_post");
