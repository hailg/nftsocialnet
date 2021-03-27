const functions = require("firebase-functions");
const ecc = require("eosjs-ecc");
const admin = require("firebase-admin");

const {
  tokenAccount,
  dgoodsAccount,
  rpc,
  transactionOptions,
  dgoodsAPI,
  dgoodSymbol,
  eosNameGenerator,
  createEOSApi,
} = require("./utils");

const issueNSNToUser = async (username, postId, tokenName, royalFee) => {
  functions.logger.log("Issuing NSN", postId, "to", username);
  let transaction = {
    actions: [
      {
        account: dgoodsAccount,
        name: "create",
        authorization: [
          {
            actor: dgoodsAccount,
            permission: "active",
          },
        ],
        data: {
          issuer: dgoodsAccount,
          rev_partner: username,
          category: "posts",
          token_name: tokenName,
          fungible: false,
          burnable: true,
          sellable: true,
          transferable: true,
          rev_split: royalFee,
          base_uri: `https://nftsocialnet.com/posts/${postId}`,
          max_issue_days: 0,
          max_supply: `1 ${dgoodSymbol}`,
        },
      },
      {
        account: dgoodsAccount,
        name: "issue",
        authorization: [
          {
            actor: dgoodsAccount,
            permission: "active",
          },
        ],
        data: {
          to: username,
          category: "posts",
          token_name: tokenName,
          quantity: `1 ${dgoodSymbol}`,
          relative_uri: "",
          memo: "Post created",
        },
      },
    ],
  };
  let transactionResult = await dgoodsAPI.transact(
    transaction,
    transactionOptions
  );
  functions.logger.log(
    "Issued NSN",
    postId,
    "to",
    username,
    "Transactioni result",
    transactionResult
  );
  const latestNSN = await rpc.get_table_rows({
    json: true,
    code: dgoodsAccount,
    scope: dgoodsAccount,
    table: "dgood",
    lower_bound: username,
    upper_bound: username,
    index_position: 2,
    key_type: "i64",
    limit: 1,
    reverse: true,
  });
  functions.logger.log(
    "Issued NSN",
    postId,
    "to",
    username,
    "dgood",
    latestNSN
  );
  return {
    trxId: transactionResult.transaction_id,
    dgoodId: latestNSN.rows[0].id,
    dgoodName: latestNSN.rows[0].token_name,
  };
};

const listNSNForSale = async (user, userAPI, postId, dgoodId, price) => {
  functions.logger.log(
    "Listing NSN for",
    user.username,
    "post",
    postId,
    "dgood id",
    dgoodId,
    "price",
    price
  );
  let username = user.username;
  let transaction = {
    actions: [
      {
        account: dgoodsAccount,
        name: "listsalenft",
        authorization: [
          {
            actor: username,
            permission: "active",
          },
        ],
        data: {
          seller: username,
          dgood_ids: [dgoodId],
          net_sale_amount: `${price} EOS`,
          sell_by_days: 0,
        },
      },
    ],
  };
  const transactionResult = await userAPI.transact(
    transaction,
    transactionOptions
  );
  functions.logger.log(
    "ListedNSN for",
    user.username,
    "post",
    postId,
    "dgood id",
    dgoodId,
    "price",
    price,
    "Transactioni result",
    transactionResult
  );
  const latestAsk = await rpc.get_table_rows({
    json: true,
    code: dgoodsAccount,
    scope: dgoodsAccount,
    table: "asks",
    lower_bound: username,
    upper_bound: username,
    index_position: 2,
    key_type: "i64",
    limit: 1,
    reverse: true,
  });
  functions.logger.log(
    "ListedNSN for",
    user.username,
    "post",
    postId,
    "dgood id",
    dgoodId,
    "price",
    price,
    "Ask",
    latestAsk
  );
  return {
    trxId: transactionResult.transaction_id,
    dgoodBatchId: latestAsk.rows[0].batch_id,
    dgoodIds: latestAsk.rows[0].dgood_ids,
    amount: latestAsk.rows[0].amount,
  };
};

const createPost = async (userId, post, password) => {
  let user = await (
    await admin.firestore().collection("users").doc(userId).get()
  ).data();
  var userAPI = null;
  try {
    if (password) {
      userAPI = createEOSApi(user, password);
    }
    let { trxId, dgoodId, dgoodName } = await issueNSNToUser(
      user.username,
      post.id,
      eosNameGenerator(),
      0.05
    );
    await admin
      .firestore()
      .collection("transactions")
      .doc(trxId)
      .set({
        type: "nsnIssued",
        trxId: trxId,
        userId: userId,
        timestamp: Date.now(),
        data: {
          postId: post.id,
          username: user.username,
          postTitle: post.title,
          postResource: post.resource,
          postResourceType: `${post.resourceType}`,
          dgoodId: dgoodId,
          dgoodName: dgoodName,
        },
      });
    var dgoodBatchId = -1;
    if (post.price !== "-1") {
      let saleResult = await listNSNForSale(
        user,
        userAPI,
        post.id,
        dgoodId,
        post.price
      );
      dgoodBatchId = saleResult.dgoodBatchId;
      await admin
        .firestore()
        .collection("transactions")
        .doc(saleResult.trxId)
        .set({
          type: "nsnListed",
          trxId: saleResult.trxId,
          userId: userId,
          timestamp: Date.now(),
          data: {
            postId: post.id,
            username: user.username,
            postTitle: post.title,
            postResource: post.resource,
            postResourceType: `${post.resourceType}`,
            dgoodId: dgoodId,
            dgoodName: dgoodName,
            dgoodBatchId: dgoodBatchId,
            price: post.price,
          },
        });
    }
    await admin.firestore().collection("posts").doc(post.id).update({
      dgoodId: dgoodId,
      dgoodName: dgoodName,
      dgoodBatchId: dgoodBatchId,
    });
    return {
      code: 200,
    };
  } catch (e) {
    functions.logger.log(
      "Error creating post. Will delete this post",
      post.id,
      "error",
      e
    );
    await admin.firestore().collection("posts").doc(post.id).delete();
    throw e;
  }
};

const purchaseNSN = async (user, password, batchId, price) => {
  functions.logger.log(
    "Purchasing NSN",
    user.username,
    "batch id",
    batchId,
    "price",
    price
  );
  let username = user.username;
  let userAPI = createEOSApi(user, password);
  const transaction = {
    actions: [
      {
        account: tokenAccount,
        name: "transfer",
        authorization: [
          {
            actor: username,
            permission: "active",
          },
        ],
        data: {
          from: username,
          to: dgoodsAccount,
          quantity: `${price} EOS`,
          memo: `${batchId},${username}`,
        },
      },
    ],
  };
  const transactionResult = await userAPI.transact(
    transaction,
    transactionOptions
  );
  functions.logger.log(
    "Purchased NSN",
    user.username,
    "batch id",
    batchId,
    "price",
    price,
    "transaction result",
    transactionResult
  );
  let latestNSN = await rpc.get_table_rows({
    json: true,
    code: dgoodsAccount,
    scope: username,
    table: "accounts",
    limit: 1,
    reverse: true,
  });
  functions.logger.log(
    "Latest NSN",
    user.username,
    "batch id",
    batchId,
    "price",
    price,
    "NSN",
    latestNSN
  );
  return {
    trxId: transactionResult.transaction_id,
    dgoodName: latestNSN.rows[0].token_name,
  };
};

const purchasePost = async (userId, post, password) => {
  let user = await (
    await admin.firestore().collection("users").doc(userId).get()
  ).data();

  let postOwnerId = post.owner.uid;
  let postAuthorId = post.author.uid;
  let purchase = await purchaseNSN(
    user,
    password,
    post.dgoodBatchId,
    post.price
  );
  let trxId = purchase.trxId;
  let transaction = await rpc.history_get_transaction(trxId);
  let actions = transaction.trx.trx.actions;
  functions.logger.log("purchase actions", actions);
  return {
    code: 200,
  };
};

exports.createPost = functions.https.onCall(async (data, context) => {
  if (!context.auth) {
    throw new functions.https.HttpsError(
      "unauthenticated",
      "The function must be called " + "while authenticated."
    );
  }
  let uid = context.auth.uid;
  let postId = data.postId;
  let password = data.password;
  let post = await (
    await admin.firestore().collection("posts").doc(postId).get()
  ).data();
  if (post.owner.uid != uid) {
    throw new functions.https.HttpsError(
      "permission-denied",
      "You're not the owner of this post.",
      "You're not the owner of this post."
    );
  }
  return await createPost(uid, post, password);
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
  let password = data.password;
  let post = await (
    await admin.firestore().collection("posts").doc(postId).get()
  ).data();
  if (post.owner.uid == uid) {
    throw new functions.https.HttpsError(
      "invalid-argument",
      "Cannot purchase. You're already the owner of this NSN post."
    );
  }
  return await purchasePost(uid, post, password);
});
