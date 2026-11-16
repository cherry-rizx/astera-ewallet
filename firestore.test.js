const {
  initializeTestEnvironment,
  assertFails,
  assertSucceeds,
} = require("@firebase/rules-unit-testing");
const { test, before, after, beforeEach } = require("node:test");
const fs = require("node:fs");

let testEnv;
const PROJECT_ID = process.env.GCP_PROJECT || "demo-no-project";
const ALICE_UID = "alice_123";
const BOB_UID = "bob_456";

const [emulatorHost, emulatorPortStr] = (
  process.env.FIRESTORE_EMULATOR_HOST || "127.0.0.1:8085"
).split(":");
const emulatorPort = parseInt(emulatorPortStr, 10);

function pastDate() {
  return new Date(Date.now() - 2000);
}

before(async () => {
  const rules = fs.readFileSync("./firestore.rules", "utf8");
  testEnv = await initializeTestEnvironment({
    projectId: PROJECT_ID,
    firestore: {
      rules,
      host: emulatorHost,
      port: emulatorPort,
    },
  });
});

after(async () => {
  if (testEnv) {
    await testEnv.cleanup();
  }
});

beforeEach(async () => {
  if (testEnv) {
    await testEnv.clearFirestore();
  }
});

test("Unauthenticated user: cannot read or write any collection", async () => {
  const unauthDb = testEnv.unauthenticatedContext().firestore();
  await assertFails(unauthDb.collection("users").doc(ALICE_UID).get());
  await assertFails(unauthDb.collection("user_directory").where("searchable", "==", true).get());
  await assertFails(unauthDb.collection("transactions").where("senderId", "==", ALICE_UID).get());
  await assertFails(unauthDb.collection("budgets").doc(ALICE_UID).get());
  await assertFails(unauthDb.collection("bank_accounts").where("ownerId", "==", ALICE_UID).get());
});

test("Users: owner can create and read own profile; cannot read or list other users", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const bobDb = testEnv.authenticatedContext(BOB_UID).firestore();

  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).set({
      uid: ALICE_UID,
      name: "Alice",
      phone: "081234567890",
      email: "alice@example.com",
      balance: 10000000,
      createdAt: pastDate(),
    })
  );

  await assertSucceeds(aliceDb.collection("users").doc(ALICE_UID).get());
  await assertFails(bobDb.collection("users").doc(ALICE_UID).get());
  await assertFails(bobDb.collection("users").get());
});

test("Users: shadow update with ghost field or negative balance fails", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  await aliceDb.collection("users").doc(ALICE_UID).set({
    uid: ALICE_UID,
    name: "Alice",
    phone: "081234567890",
    email: "alice@example.com",
    balance: 10000000,
    createdAt: pastDate(),
  });

  await assertFails(
    aliceDb.collection("users").doc(ALICE_UID).update({ isAdmin: true })
  );
  await assertFails(
    aliceDb.collection("users").doc(ALICE_UID).update({ balance: -100 })
  );
});

test("Users: wallet balances cannot be changed without a matching settlement", async () => {
  await testEnv.withSecurityRulesDisabled(async (ctx) => {
    for (const [uid, name, phone, email] of [
      [ALICE_UID, "Alice", "081234567890", "alice@example.com"],
      [BOB_UID, "Bob", "081987654321", "bob@example.com"],
    ]) {
      await ctx.firestore().collection("users").doc(uid).set({
        uid, name, phone, email, balance: 10000000, createdAt: pastDate(),
      });
    }
  });

  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  await assertFails(aliceDb.collection("users").doc(ALICE_UID).update({ balance: 20000000 }));
  await assertFails(aliceDb.collection("users").doc(BOB_UID).update({ balance: 10500000 }));
  // Cannot modify Bob's name
  await assertFails(
    aliceDb.collection("users").doc(BOB_UID).update({ name: "Hacked" })
  );
});

test("User secrets: strictly owner-only read/write, no delete", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const bobDb = testEnv.authenticatedContext(BOB_UID).firestore();

  await assertSucceeds(
    aliceDb.collection("user_secrets").doc(ALICE_UID).set({
      uid: ALICE_UID,
      pinHash: "a".repeat(64),
      failedAttempts: 0,
      lockUntilMs: 0,
      updatedAt: pastDate(),
    })
  );

  await assertSucceeds(aliceDb.collection("user_secrets").doc(ALICE_UID).get());
  await assertFails(bobDb.collection("user_secrets").doc(ALICE_UID).get());
  await assertFails(aliceDb.collection("user_secrets").doc(ALICE_UID).delete());
});

test("Transactions: standalone transfer and topup records cannot mint wallet balance", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const bobDb = testEnv.authenticatedContext(BOB_UID).firestore();

  await assertFails(
    aliceDb.collection("transactions").doc("tx_1").set({
      senderId: ALICE_UID,
      receiverId: BOB_UID,
      senderName: "Alice",
      receiverName: "Bob",
      type: "transfer",
      amount: 500000,
      note: "Bayar makan",
      outOfBudget: false,
      createdAt: pastDate(),
    })
  );

  // Unfiltered list fails
  await assertFails(aliceDb.collection("transactions").get());

  // Filtered list for sender/receiver succeeds
  await assertSucceeds(
    aliceDb.collection("transactions").where("senderId", "==", ALICE_UID).get()
  );
  await assertSucceeds(
    bobDb.collection("transactions").where("receiverId", "==", BOB_UID).get()
  );

  // A topup record without wallet credit and matching bank debits fails.
  await assertFails(
    aliceDb.collection("transactions").doc("tx_topup_1").set({
      senderId: ALICE_UID,
      receiverId: ALICE_UID,
      senderName: "Alice",
      receiverName: "Alice",
      type: "topup",
      amount: 1000000,
      note: "Top Up Multi-Bank",
      outOfBudget: false,
      splitDetail: [
        { bankId: "bank_1", bankName: "Bank Alpha", amount: 500000 },
        { bankId: "bank_2", bankName: "Bank Beta", amount: 500000 },
      ],
      createdAt: pastDate(),
    })
  );

  // Over max topup (> 1,000,000) also fails.
  await assertFails(
    aliceDb.collection("transactions").doc("tx_topup_bad").set({
      senderId: ALICE_UID,
      receiverId: ALICE_UID,
      senderName: "Alice",
      receiverName: "Alice",
      type: "topup",
      amount: 2000000,
      note: "Too big",
      outOfBudget: false,
      splitDetail: [{ bankId: "bank_1", bankName: "Bank Alpha", amount: 2000000 }],
      createdAt: pastDate(),
    })
  );
});

test("Budgets & Bank Accounts: owner isolation enforced", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const bobDb = testEnv.authenticatedContext(BOB_UID).firestore();

  await assertSucceeds(
    aliceDb.collection("budgets").doc(ALICE_UID).set({
      uid: ALICE_UID,
      monthlyLimit: 1000000,
      monthKey: "2026-11",
      spentThisMonth: 250000,
      updatedAt: pastDate(),
    })
  );
  await assertFails(bobDb.collection("budgets").doc(ALICE_UID).get());

  await assertSucceeds(
    aliceDb.collection("bank_accounts").doc(`${ALICE_UID}_alpha`).set({
      ownerId: ALICE_UID,
      bankName: "Bank Alpha",
      accountNumber: "1001 4821 0901",
      balance: 5000000,
      createdAt: pastDate(),
    })
  );
  await assertSucceeds(
    aliceDb.collection("bank_accounts").where("ownerId", "==", ALICE_UID).get()
  );
  await assertFails(bobDb.collection("bank_accounts").doc(`${ALICE_UID}_alpha`).get());
  await assertFails(
    aliceDb.collection("bank_accounts").doc(`${ALICE_UID}_alpha`).update({ balance: 0 })
  );
});
