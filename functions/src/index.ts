import * as functions from "firebase-functions";
import * as admin from "firebase-admin";


admin.initializeApp({
  credential: admin.credential.applicationDefault(),
  databaseURL: "https://coach-me-381521-default-rtdb.europe-west1.firebasedatabase.app",
});

// // Start writing functions
// // https://firebase.google.com/docs/functions/typescript
//
// export const helloWorld = functions.https.onRequest((request, response) => {
//   functions.logger.info("Hello logs!", {structuredData: true});
//   response.send("Hello from Firebase!");
// });

/*
    ========================================
    ***************   INFO   ***************
    ========================================

    Always before running the sendPushNotification cloud function locally,
    add the service account key to the environment variables:

    Windows:
    $env:GOOGLE_APPLICATION_CREDENTIALS="C:\Users\...
            \SDP-2023\functions\serviceAccountKey.json"

    Mac/Linux:
    export GOOGLE_APPLICATION_CREDENTIALS="/home/...
            /SDP-2023/functions/serviceAccountKey.json"

    where serviceAccountKey.json is the service account key file

    Then, you can run the cloud function locally as follows:
     - cd into the functions folder
     - run the command "npm run serve"
     - run the command "firebase emulators:start"
     - in the terminal, you can click on the View in Emulator UI link
        to open the Emulator UI in your browser
     - there, you can also upload a database instance from a json file
        (that you can download from the actual database in the Firebase console)
*/

// This cloud function listens for changes in the
// messages node of the database and sends a push notification to the
// recipient of the message if the
// recipient has not already read the message
export const sendPushNotification = functions.database
  .ref("/coachme/messages/{chatId}/messages/{messageId}")
  .onWrite(async (change, context) => {
    const chatId = context.params.chatId;
    const message = change.after.val();
    const sender = message.sender;
    const readByRecipient = message.readByRecipient;

    const senderWithCommas = sender.replace(/\./g, ",");
    const recipient = chatId.replace(senderWithCommas, "");

    // if recipient has already read the message, no need for push notification
    if (readByRecipient == true) {
      return;
    }

    const [recipientTokenSnapshot,
      firstNameSenderSnapshot,
      lastNameSenderSnapshot] =
        await Promise.all([
          change.after.ref.root
            .child("/coachme/fcmTokens/" + recipient)
            .once("value"),
          change.after.ref.root
            .child("/coachme/accounts/" + senderWithCommas + "/firstName")
            .once("value"),
          change.after.ref.root
            .child("/coachme/accounts/" + senderWithCommas + "/lastName")
            .once("value"),
        ]);

    // if no token found for given recipient (i.e., .val() returns null) or
    // return without sending push notification
    if (!recipientTokenSnapshot.val()) {
      return;
    }

    const firstNameSender = firstNameSenderSnapshot.val();
    const lastNameSender = lastNameSenderSnapshot.val();

    const payload = {
      notification: {
        title: `New message from ${firstNameSender} ${lastNameSender}`,
        body: message.content,
      },
      data: {
        notificationType: "messaging",
        sender: sender,
      },
    };

    await admin.messaging().sendToDevice(recipientTokenSnapshot.val(), payload);
  });
