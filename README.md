## How to install the app:

Download and install this apk file to your Android phone: https://github.com/hailg/nftsocialnet/blob/main/app-release.apk

I'm submitting the app to Play Store, but it's still under review at the moment.

## Inspiration

I was inspired by Steve Jobs and Albert Einstein about **Simplicity**. For the context, I knew about blockchain technology for a few years, but it's always hard for me to discuss or explain this topic with my friends without making them confused. I believe part of the reason is that they don't really use any blockchain product yet. I plan to make it simple, but no simpler, by inviting them to use a social network built on top of EOSIO and the dGoods contracts. By allowing them to use a blockchain product, I can link them with the concepts unfamiliar to them, e.g., blockchain transactions, smart contracts, logging, auditing...

I believe that to bring blockchain technology to the mass population, we need to work hard to make our product simple to reach everyone, not only the people working in the tech industry.

## What it does

In a short description, this is an MVP social network, so we have these basic features:

- Users can create new posts (also NFT tokens, managed by the dGoods contract). The post can have photos in jpeg, png, gif... format.
- Users can interact with posts through comment and like.
- Users can purchase posts (NFT tokens) that they like, resell them to other people.
- Users can see all their transactions that are stored in EOSIO blockchain.
- Users can earn a royal fee for their content (thanks to the dGoods contract).

## How we built it

The final product has the following components:

- The front-end is an Android application.
- EOSIO and dGoods contract running inside Google Compute Engine. This blockchain manages all the features related to content creation and users' transactions.
- We use Firebase Storage and Firestore to store the content of the NFT (to save RAM cost). Their id, however, will be linked to the blockchain.
- We use Firebase Functions to communicate and synchronize data between Firebase and EOSIO + dGoods contract.

This is the high-level architecture:
![Architecture](https://github.com/hailg/nftsocialnet/blob/main/diagrams/NFT%20Social%20Network.png?raw=true)

Let's use 1 example flow for Post creation to see how it really work between the social network feature and EOSIO + dGoods blockchain:
![Post creation sequence diagram](https://github.com/hailg/nftsocialnet/blob/main/diagrams/Create%20Post%20Sequence%20Diagram.png?raw=true)

In this sequence diagram, the interesting point is when we use Cloud Functions to create and issue tokens to the users. We actually do 2 operations here:

- To issued the post NFT token to user, we use our own system private key.
- To list the post NFT token on sale, we use the user's private key. I will explain more about how we store and use users' private keys in the next section.

## Challenges we ran into

We want to have a simple UI/UX so that anyone can use it. However, we don't want to sacrifice the most basic blockchain idea because each user must have their own wallet and private key. We built a wallet on our backend and exposed it to users through a simple concept, a wallet password.

This is easier said than done. The solution is that our backend will need to store users' private keys. But we don't want it to be seen or used by anyone except the key owner. We should also make it secure so that even if we get hacked, the hacker cannot retrieve the original private keys. In this case, even we, the creator of the system, cannot know about the private keys. I think this is the most challenging and interesting problem that we have solved.

The mechanism is reflected in our code on Github, but here is a short description of what we did to achieve this:

- We have a master and very complex password. Let's call it P1.
- Each user will have their own password. Let's call it P2.
- During user registration, a private key is generated for the user. We use the public key of this private key to register this user with EOSIO.
- The private key is encrypted using the AES algorithm, using the hash of the combination of P1 and P2.
- The user's password P2 is **NEVER** stored in our system.

With this mechanism, we assure that an attacker can only decrypt our users' private keys if he can achieve all the following conditions:

- Access to the encrypted private key by hacking into our system.
- Access to the system master password by hacking into our system.
- Access to the user's password by hacking/social engineering.

Even we, the system owner, cannot decrypt users' keys.

## Accomplishments that we're proud of

We've built an MVP social network that integrates perfectly with EOSIO and the dGoods NFT contract. It allows our users to build their content and earn profit from them. This simple rewards mechanism will attract our users to stay with the platform and produce great content.

And thanks to the PoS mechanism, our users don't have to pay the high gas price they have with other competitors.

## What we learned

This is the first time I built an end-to-end product using blockchain technology. It helps me understand the internal mechanism of blockchain, on top of all the concepts I've learned so far.

This is also the first time I have a chance to work on Firebase Firestore, Cloud Functions to quickly set up a full-featured backend for my mobile app. I consider this is the first step toward building more apps, consider how easy it is now for developers to implement their ideas.

## What's next for NFT Social Network

I'm looking forward to bring this social network onto the EOS main network. It will require some changes and I hope I can get support from EOS to keep the UI flow simple (or even simpler).
