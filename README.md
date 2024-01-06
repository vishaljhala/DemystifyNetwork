# Context

Demystify Network is a threat intelligence platform and a security snap within MetaMask. Open source repo with MIT license will serve as a great example for future snap developers.

Demystify Network was built with generous grants from 
* Google Web3 Startups - <a href="https://etherscan.io/nft/0xcc4177805aad9483ea2b2b814b82a83abe564023/214"><img src="https://github.com/vishaljhala/DemystifyNetwork/blob/main/google_web3.png" align="left" width="96" ></a>
* Metamask Grants DAO. ![Metamask Grants DAO](metamaskdao.png) - https://metamaskgrants.org/snaps-sub-dao-grant-001--audits-for-snaps-xmtp-push-protocol-nocturne-demystify-network

# How to run 
* Make sure you follow SupportScripts and start database before running this application
* Run locally:  `mvn spring-boot:run -Dspring-boot.run.profiles=development`
* Deploy to production: `./build.sh`

# Why Demystify Network ?

Blockchains are built on the concept of anonymity and permissionless inclusivity. This means any one can participate in blockchain without revealing who they are and without permission from anyone. While this sounds like a financial democracy, bad actors tend to misuse this as an opportunity to disguise and commit fraud.

While there are many the solutions in market to protect businesses at a price, there exists no solutions to protect individual users. This not only allows bad actors to target individuals but also negatively impacts the adoption of blockchains.

Demystify Network was built with an intent to protect end users for free and keep the bad actors away while promoting blockchain adoption. Demystify Network achieves it by generating real-time threat intel.



# What does Demystify Network do?

[Have a look at short Demo](https://youtu.be/6S3rVBa6WNQ?feature=shared)

At the very core, Demystify Network provides novel illicit fund detection and risk scoring algorithm wrapped behind a API.
1) It takes any ethereum address as input.
2) Provides a risk score between 0 to 10
3) Identifies actual incomes and expenditure sources (ignoring all the addresses in between)
4) Detects illicit fund flow (income as well as expenditures)
5) Does all of it real-time, can evaluate millions of relationships in less than 2 seconds due to its unique in-memory representation.

Here is an example of how the risk profile of an address looks like from within a MetaMask wallet. When integrated with the wallet, users of the wallet gets threat intel right before signing the transaction and thereby enabling the user to protect themselves from frauds and scams.
![MetaMask](metamask.png)

# What are the key components of Demystify Network?

Demystify Network has 3 main components.
1) Backend - This project is application backend and implements a novel illicit fund detection algorithm specially designed for blockchains. Technically, its level order bfs custom built for blockchains to trace money laundering and illicit fund detection.
2) MetaMask Front end - https://github.com/demystify-network/ThreatIntelligence
3) DataLake - Holds a unique in-memory representation of entire blockchain(ethereum), along with ability to scan web2 and social media for realtime threats.

# How does it do that?

Think of Blockchain addresses as nodes and transactions as relationships a.k.a. edges. This converts blockchain into a massive directed and weighted graph. 

Its gets a lot easier after this. As shown in the screenshot below, we use level order BFS (it's more complicated than that though), to transverse the relationships and identify incomes and expenditures. And along the way we calculate risk score based on 
1) identified & tagged bad actors
2) percent of illicit funds
3) distance and weight of relationships.
![funds transfer](funds_transfer_example.jpg)
