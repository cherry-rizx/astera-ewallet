const { initializeApp } = require("firebase-admin/app");
const { getFirestore } = require("firebase-admin/firestore");
const { getMessaging } = require("firebase-admin/messaging");
const { onDocumentCreated } = require("firebase-functions/v2/firestore");

initializeApp();
const db = getFirestore();

exports.notifyIncomingTransfer = onDocumentCreated(
  "transactions/{transactionId}",
  async (event) => {
    const tx = event.data?.data();
    if (!tx || tx.type !== "transfer" || tx.senderId === tx.receiverId) return;

    const [settingsSnap, tokenSnap] = await Promise.all([
      db.collection("notification_settings").doc(tx.receiverId).get(),
      db.collection("fcm_tokens").doc(tx.receiverId).get(),
    ]);
    const settings = settingsSnap.data();
    const token = tokenSnap.data()?.token;
    if (settings?.transactionAlerts === false || !token) return;

    try {
      await getMessaging().send({
        token,
        notification: {
          title: "Uang masuk",
          body: `${tx.senderName || "Pengguna ASTERA"} mengirim ${new Intl.NumberFormat("id-ID", { style: "currency", currency: "IDR", maximumFractionDigits: 0 }).format(tx.amount || 0)}.`,
        },
        android: { notification: { channelId: "astera_transactions" } },
        data: { transactionId: event.params.transactionId },
      });
    } catch (error) {
      if (error?.code === "messaging/registration-token-not-registered") {
        await tokenSnap.ref.delete();
        return;
      }
      throw error;
    }
  },
);
