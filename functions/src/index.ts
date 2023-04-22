import * as functions from "firebase-functions";
import * as admin from "firebase-admin";


admin.initializeApp({
    credential: admin.credential.applicationDefault(),
});

// // Start writing functions
// // https://firebase.google.com/docs/functions/typescript
//
// export const helloWorld = functions.https.onRequest((request, response) => {
//   functions.logger.info("Hello logs!", {structuredData: true});
//   response.send("Hello from Firebase!");
// });

/*
    ====================================================================================================
    *********************************************   INFO   *********************************************
    ====================================================================================================

    Always before running the sendPushNotification cloud function locally, add the service account key to the environment variables:

    Windows:
    $env:GOOGLE_APPLICATION_CREDENTIALS="C:\Users\...\SDP-2023\functions\src\serviceAccountKey.json"

    Mac/Linux:
    export GOOGLE_APPLICATION_CREDENTIALS="/home/.../SDP-2023/functions/src/serviceAccountKey.json"

    where serviceAccountKey.json is the service account key file

    Then, you can run the cloud function locally:
*/
export const sendPushNotification = functions.database.ref('/coachme/messages/{chatId}/messages/{messageId}')
    .onWrite(async (change, context) => {

        const chatId = context.params.chatId;
        const messageId = context.params.messageId;

        const message = change.after.val();
        const content = message.content;
        const sender = message.sender;
        const readByRecipient = message.readByRecipient;

        const senderWithCommas = sender.replace(/\./g, ",");
        functions.logger.info("senderWithCommas: " + senderWithCommas, {structuredData: true});
        const recipient = chatId.replace(senderWithCommas, "");


        functions.logger.info("chatId: " + chatId, {structuredData: true});
        functions.logger.info("messageId: " + messageId, {structuredData: true});
        functions.logger.info("content: " + content, {structuredData: true});
        functions.logger.info("sender: " + sender, {structuredData: true});
        functions.logger.info("readByRecipient: " + readByRecipient, {structuredData: true});
        functions.logger.info("recipient: " + recipient, {structuredData: true});

        // if recipient has already read the message, no need for push notification
        if (readByRecipient == true) {
            return
        }
        functions.logger.info("readByRecipient is false", {structuredData: true});

        const [recipientTokenSnapshot, pushNotificationsEnabledSnapshot, firstNameSenderSnapshot, lastNameSenderSnapshot] = await Promise.all([
            change.after.ref.root.child(`/coachme/fcmTokens/${recipient}/token`).once('value'),
            change.after.ref.root.child(`/coachme/fcmTokens/${recipient}/permissionGranted`).once('value'),
            change.after.ref.root.child(`/coachme/accounts/${senderWithCommas}/firstName`).once('value'),
            change.after.ref.root.child(`/coachme/accounts/${senderWithCommas}/lastName`).once('value')
        ]);

        if (!pushNotificationsEnabledSnapshot.val()) {
            return
        }

        const firstNameSender = firstNameSenderSnapshot.val();
        const lastNameSender = lastNameSenderSnapshot.val();

        functions.logger.info("firstName: " + firstNameSender, {structuredData: true});
        functions.logger.info("lastName: " + lastNameSender, {structuredData: true});


        const payload = {
            notification: {
                title: `New message from ${firstNameSender} ${lastNameSender}`,
                body: message.content,
//                 icon: '/firebase-logo.png'
            }
        };

        await admin.messaging().sendToDevice(recipientTokenSnapshot.val(), payload);
    });
