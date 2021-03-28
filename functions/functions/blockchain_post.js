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
  functions.logger.log(userId, "Creating post", post.id);
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
      .add({
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
        .add({
          type: "nsnListed",
          trxId: saleResult.trxId,
          userId: userId,
          timestamp: Date.now(),
          data: {
            postId: post.id,
            username: user.username,
            postTitle: post.title,
            postContent: post.content,
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
  } catch (e) {
    functions.logger.error(
      "Error creating post. Will delete this post",
      post.id,
      "error",
      e
    );
    await admin.firestore().collection("posts").doc(post.id).delete();
    throw e;
  }
  functions.logger.log(userId, "Created post", post.id);
  return {
    code: 200,
  };
};

const purchaseNSN = async (user, userAPI, batchId, price) => {
  functions.logger.log(
    "Purchasing NSN",
    user.username,
    "batch id",
    batchId,
    "price",
    price
  );
  let username = user.username;
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
  return {
    transactionResult: transactionResult,
  };
};

const purchasePost = async (userId, post, password, newPrice) => {
  functions.logger.log(
    userId,
    "Purchasing post",
    post.id,
    ". New price",
    newPrice
  );
  let user = await (
    await admin.firestore().collection("users").doc(userId).get()
  ).data();
  let userAPI = createEOSApi(user, password);
  var purchase = null;
  try {
    purchase = await purchaseNSN(user, userAPI, post.dgoodBatchId, post.price);
  } catch (e) {
    if (e.json) {
      var msg = "Cannot purchase this NSN post. Please try again!";
      functions.logger.error(
        "Failed to purchase post for user",
        userId,
        "post",
        post.id,
        "error json",
        e.json
      );
      if (e.json.error && e.json.error.details) {
        msg = `Cannot purchase this NSN post. Block chain error ${e.json.error.details[0].message}`;
      }
      throw new functions.https.HttpsError("internal", msg, msg);
    } else {
      functions.logger.error(
        "Failed to purchase post for user",
        userId,
        "post",
        post.id,
        "error",
        e
      );
      throw new functions.https.HttpsError(
        "internal",
        "Cannot purchase this NSN post. Please try again!",
        "Cannot purchase this NSN post. Please try again!"
      );
    }
  }

  await admin
    .firestore()
    .collection("posts")
    .doc(post.id)
    .update({
      owner: {
        name: user.name,
        photoUrl: user.photoUrl,
        uid: user.uid,
      },
      price: newPrice,
    });

  let postOwnerId = post.owner.uid;
  let postOwnerName = post.owner.name;
  let postOwnerPhoto = post.owner.photoUrl;
  let postAuthorId = post.author.uid;
  let postAuthorName = post.author.name;
  let postAuthorPhoto = post.author.photoUrl;
  let purchaseTransactionResult = purchase.transactionResult;
  await admin
    .firestore()
    .collection("transactions")
    .add({
      type: "nsnPurchased",
      trxId: purchaseTransactionResult.transaction_id,
      userId: userId,
      timestamp: Date.now(),
      data: {
        postId: post.id,
        ownerName: postOwnerName,
        ownerPhoto: postOwnerPhoto,
        authorId: postAuthorId,
        authorName: postAuthorName,
        authorPhoto: postAuthorPhoto,
        buyerId: userId,
        buyerName: user.name,
        buyerPhoto: user.photoUrl,
        postTitle: post.title,
        postContent: post.content,
        postResource: post.resource,
        postResourceType: `${post.resourceType}`,
        dgoodId: post.dgoodId,
        dgoodName: post.dgoodName,
        dgoodBatchId: post.dgoodBatchId,
        price: post.price,
      },
    });

  await admin
    .firestore()
    .collection("transactions")
    .add({
      type: "nsnSold",
      trxId: purchaseTransactionResult.transaction_id,
      userId: postOwnerId,
      timestamp: Date.now(),
      data: {
        postId: post.id,
        ownerName: postOwnerName,
        ownerPhoto: postOwnerPhoto,
        authorId: postAuthorId,
        authorName: postAuthorName,
        authorPhoto: postAuthorPhoto,
        buyerId: userId,
        buyerName: user.name,
        buyerPhoto: user.photoUrl,
        postTitle: post.title,
        postContent: post.content,
        postResource: post.resource,
        postResourceType: `${post.resourceType}`,
        dgoodId: post.dgoodId,
        dgoodName: post.dgoodName,
        dgoodBatchId: post.dgoodBatchId,
        price: post.price,
      },
    });

  if (postAuthorId != postOwnerId) {
    let purchaseActions =
      purchaseTransactionResult.processed.action_traces[0].inline_traces;
    for (let action of purchaseActions) {
      let actionName = action.act.name;
      if (actionName === "transfer") {
        let actData = action.act.data;
        if (actData.from === dgoodsAccount && actData.to === postAuthorId) {
          await admin
            .firestore()
            .collection("transactions")
            .add({
              type: "nsnRoyalFee",
              trxId: purchaseTransactionResult.transaction_id,
              userId: postAuthorId,
              timestamp: Date.now(),
              data: {
                postId: post.id,
                ownerName: postOwnerName,
                ownerPhoto: postOwnerPhoto,
                authorId: postAuthorId,
                authorName: postAuthorName,
                authorPhoto: postAuthorPhoto,
                buyerId: userId,
                buyerName: user.name,
                buyerPhoto: user.photoUrl,
                postTitle: post.title,
                postContent: post.content,
                postResource: post.resource,
                postResourceType: `${post.resourceType}`,
                dgoodId: post.dgoodId,
                dgoodName: post.dgoodName,
                dgoodBatchId: post.dgoodBatchId,
                price: post.price,
                amount: actData.quantity.replace(" EOS", ""),
              },
            });
          break;
        }
      }
    }
  }

  var saleResult = null;
  try {
    saleResult = await listNSNForSale(
      user,
      userAPI,
      post.id,
      post.dgoodId,
      newPrice
    );
  } catch (e) {
    await admin.firestore().collection("posts").doc(post.id).update({
      dgoodBatchId: -1,
      price: "-1",
    });
    if (e.json) {
      var msg = "Cannot list your NSN post to resell.";
      functions.logger.error(
        "Failed to relist post for user",
        userId,
        "post",
        post.id,
        "error json",
        e.json
      );
      if (e.json.error && e.json.error.details) {
        msg = `Cannot list your NSN poto to resell. Block chain error ${e.json.error.details[0].message}`;
      }
      throw new functions.https.HttpsError("internal", msg, msg);
    } else {
      functions.logger.error(
        "Failed to re-list post for user",
        userId,
        "post",
        post.id,
        "error",
        e
      );
      throw new functions.https.HttpsError(
        "internal",
        "Cannot list your NSN post to resell!",
        "Cannot list your NSN post to resell!"
      );
    }
  }

  let dgoodBatchId = saleResult.dgoodBatchId;
  await admin
    .firestore()
    .collection("transactions")
    .add({
      type: "nsnListed",
      trxId: saleResult.trxId,
      userId: userId,
      timestamp: Date.now(),
      data: {
        postId: post.id,
        username: user.username,
        postTitle: post.title,
        postContent: post.content,
        postResource: post.resource,
        postResourceType: `${post.resourceType}`,
        dgoodId: post.dgoodId,
        dgoodName: post.dgoodName,
        dgoodBatchId: dgoodBatchId,
        price: newPrice,
      },
    });
  await admin.firestore().collection("posts").doc(post.id).update({
    dgoodBatchId: dgoodBatchId,
  });
  functions.logger.log(
    userId,
    "Purchased post",
    post.id,
    ". New price",
    newPrice
  );
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
  let newPrice = data.newPrice;
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
  return await purchasePost(uid, post, password, newPrice);
});
