import * as functions from "firebase-functions";
import * as admin from "firebase-admin";
import {Change} from "firebase-functions";
import {DataSnapshot} from "@firebase/database-types";


admin.initializeApp({
  credential: admin.credential.applicationDefault(),
  databaseURL: "https://coach-me-381521-default-rtdb.europe-west1.firebasedatabase.app",
});

// // Start writing functions
// // https://firebase.google.com/docs/functions/typescript

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

     To deploy the cloud function to Firebase, run the command:
     "firebase deploy --only functions"

     ========================================
*/

/**
 * Cloud function that sends a push notification to the recipient
 * of the message if the recipient has not already read the message.
 *
 * @param change - the database write event that triggered the function
 * @param context - the context of the function
 */
export const sendPushNotification = functions.database
  .ref("/coachme/messages/{chatId}/messages/{messageId}")
  .onWrite(async (change, context) => {
    functions.logger.log("sendPushNotification function started");
    const chatId = context.params.chatId;
    const message = change.after.val();
    const sender = message.sender;
    const readState = message.readState;

    const senderWithCommas = sender.replace(/\./g, ",");
    const recipient = chatId.replace(senderWithCommas, "");

    // if recipient has already read the message or
    // received a push notification for it, no need for push notification
    if (readState == "RECEIVED" || readState == "READ") {
      return;
    }

    const [participantsSnapshot,
      firstNameSenderSnapshot,
      lastNameSenderSnapshot]: DataSnapshot[] =
        await fetchSnapshotValues(change, chatId, senderWithCommas, recipient);

    functions.logger.log("before the if statement for participantsSnapshot", participantsSnapshot.val());
    // if no participants found for given chatId (i.e., .val() returns null)
    // return without sending push notification
    if (!participantsSnapshot.val()) {
      return;
    }

    functions.logger.log("after the if statement for participantsSnapshot");
    functions.logger.log("participants: " + participantsSnapshot.val());

    const recipientsTokenSnapshot: DataSnapshot[] =
      await fetchRecipientsTokensSnapshotValues(
        change, participantsSnapshot.val(), sender);

    const payload = createPayload(
      firstNameSenderSnapshot.val(), lastNameSenderSnapshot.val(),
      message.content, sender);

    functions.logger.log("before the for loop for recipientsTokenSnapshot");
    for (const recipientTokenSnapshot of recipientsTokenSnapshot) {
      functions.logger.log("recipientTokenSnapshot: " + recipientTokenSnapshot.val());
      // if recipient has not registered a token, no need for push notification
      if (!recipientTokenSnapshot.val()) {
        continue;
      }
      // send push notification
      await admin.messaging().sendToDevice(recipientTokenSnapshot.val(), payload);
    }
    functions.logger.log("after the for loop for recipientsTokenSnapshot");
    // update readState to RECEIVED
    await change.after.ref.update({readState: "RECEIVED"});
  });


/**
 * Helper function that fetches snapshot values for the recipient tokens
 *
 * @param change - the database write event that triggered the function
 * @param participants - the context of the function
 * @param sender - the sender of the message
 * @return {Promise<DataSnapshot[]>} An object containing the tokens
 *   snapshot for each recipient.
 */
async function fetchRecipientsTokensSnapshotValues(
  change: Change<DataSnapshot>,
  participants: string[],
  sender: string,
): Promise<DataSnapshot[]> {
  const recipientsTokensSnapshotPromises: Promise<DataSnapshot>[] = [];
  for (const participant of participants) {
    if (participant == sender) { // sender doesn't need push notification
      continue;
    }
    const recipientWithCommas = participant.replace(/\./g, ",");
    recipientsTokensSnapshotPromises.push(
    change.after.ref.root
      .child("/coachme/fcmTokens/" + recipientWithCommas)
      .once("value"),
    );
  }
  return Promise.all(recipientsTokensSnapshotPromises);
}


/**
 * Helper function that fetches snapshot values for the chat participants,
 * sender's first name, and sender's last name from the database in parallel.
 *
 * @param {Change<DataSnapshot>} change The database change object.
 * @param {string} chatId The ID of the chat.
 * @param {string} senderWithCommas The sender ID with dots replaced by commas.
 * @param {string} recipientWithCommas The ID of the recipient with
 *   dots replaced by commas.
 * @return {Promise<DataSnapshot[]>} An object containing the
 *   participants snapshot, sender's first name snapshot,
 *   and sender's last name snapshot.
 */
async function fetchSnapshotValues(
  change: Change<DataSnapshot>,
  chatId: string,
  senderWithCommas: string,
  recipientWithCommas: string,
): Promise<DataSnapshot[]> {
  const [participantsSnapshot,
    firstNameSenderSnapshot,
    lastNameSenderSnapshot] =
      await Promise.all([
        change.after.ref.root
          .child("/coachme/messages/" + chatId + "/participants")
          .once("value"),
        change.after.ref.root
          .child("/coachme/accounts/" + senderWithCommas + "/firstName")
          .once("value"),
        change.after.ref.root
          .child("/coachme/accounts/" + senderWithCommas + "/lastName")
          .once("value"),
      ]);

  return [participantsSnapshot,
    firstNameSenderSnapshot,
    lastNameSenderSnapshot];
}


/**
 * Helper function that creates the payload for the push notification.
 *
 * @param {string} firstNameSender - The first name of the sender.
 * @param {string} lastNameSender - The last name of the sender.
 * @param {string} bodyContent - The message content.
 * @param {string} sender - The ID of the sender.
 * @return {admin.messaging.MessagingPayload} - The payload object.
 */
function createPayload(
  firstNameSender: string,
  lastNameSender: string,
  bodyContent: string,
  sender: string): admin.messaging.MessagingPayload {
  return {
    notification: {
      title: `${firstNameSender} ${lastNameSender}`,
      body: bodyContent,
      // needed to tell the app to open the chat activity
      click_action: "OPEN_CHAT_ACTIVITY",
    },
    data: {
      // needed to tell the app what type of notification this is
      // (to enable different types of push notifications in the future)
      notificationType: "messaging",
      sender: sender,
    },
  };
}
