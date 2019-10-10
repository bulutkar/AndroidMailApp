package com.example.mailapp;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ListView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import com.microsoft.cognitiveservices.speech.SpeechConfig;
import com.microsoft.cognitiveservices.speech.SpeechRecognizer;
import com.microsoft.cognitiveservices.speech.SpeechSynthesisResult;
import com.microsoft.cognitiveservices.speech.SpeechSynthesizer;
import com.microsoft.cognitiveservices.speech.audio.AudioConfig;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Future;

import javax.mail.Message;
import javax.mail.Multipart;
import javax.mail.Part;

import static android.Manifest.permission.INTERNET;
import static android.Manifest.permission.RECORD_AUDIO;

/*public static Drawable LoadImageFromWebOperations(String url) {
    try {
        InputStream is = (InputStream) new URL(url).getContent();
        Drawable d = Drawable.createFromStream(is, "src name");
        return d;
    } catch (Exception e) {
        return null;
    }
}*/

public class MainActivity extends AppCompatActivity {
    private static final String SpeechSubscriptionKey = "49551d7f82684ae196690097a1c79e0f";
    private static final String SpeechRegion = "westus";

    private SharedPreferences sharedPreferences;
    private SharedPreferences sharedPreferences2;
    private ImageButton newMailButton;
    private Button logoutButton;
    private ListView inboxList;

    private SpeechConfig speechConfig;
    private SpeechSynthesizer synthesizer;
    private MicrophoneStream microphoneStream;
    private SpeechRecognizer reco;
    private Future<SpeechSynthesisResult> speechSynthesisResult;
    private AudioConfig audioInput;

    private boolean isSpeakStop;
    private String introductionText;
    private List<String> inboxHeader;
    private String Body;
    private List<String> Bodies;
    private int messageCounts;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        sharedPreferences = getSharedPreferences("LoginInfo", 0);
        newMailButton = findViewById(R.id.new_mail);
        logoutButton = findViewById(R.id.logoutButton);
        inboxList = findViewById(R.id.listView1);

        isSpeakStop = false;

        introductionText = "Welcome to the main page! ";
        introductionText += "You can use start email, new email, start new email, create email keywords to send a new mail. ";
        introductionText += "You can use logout keyword to logout from your account, you will be redirected to login screen. ";
        introductionText += "You can listen the subject, sender and body information of last mail you received by saying play last email, say last email, tell last email, read last email keywords. ";
        introductionText += "You can listen the subject and sender information of all mails you received by saying play all emails, say all emails, tell all emails, read all emails keywords. ";
        introductionText += "When you want to quit from app, you can use quit application or exit application keywords any where in the application. ";
        introductionText += "If you want to listen this introduction part again, you can use repeat commands or help keywords to replay introduction. ";
        introductionText += "Listening your commands now! ";

        try {
            int permissionRequestId = 5;
            ActivityCompat.requestPermissions(MainActivity.this, new String[]{RECORD_AUDIO, INTERNET}, permissionRequestId);
        } catch (Exception ex) {
            Log.e("SpeechSDK", "could not init sdk, " + ex.toString());
        }
        try {
            speechConfig = createSpeechConfig(SpeechSubscriptionKey, SpeechRegion);
            synthesizer = new SpeechSynthesizer(speechConfig);
            sharedPreferences2 = getSharedPreferences("IntroSpeaksMain", 0);
            boolean isRead = sharedPreferences2.getBoolean("isRead", false);
            if (!isRead) {
                speechSynthesisResult = synthesizer.SpeakTextAsync(introductionText);
                synthesizer.SynthesisCompleted.addEventListener((o, e) -> {
                    e.close();
                    speechSynthesisResult.cancel(true);
                    isSpeakStop = true;
                    SharedPreferences.Editor editor = sharedPreferences2.edit();
                    editor.putBoolean("isRead", true);
                    editor.apply();
                });
            } else {
                speechSynthesisResult = synthesizer.SpeakTextAsync("You are in main page! Listening your commands now!");
                synthesizer.SynthesisCompleted.addEventListener((o, e) -> {
                    e.close();
                    speechSynthesisResult.cancel(true);
                    isSpeakStop = true;
                });
            }
        } catch (Exception e) {
            Log.e("MainCreateOnException", e.getMessage());
        }
        final String logTag = "Main reco 3";
        ArrayList<String> content = new ArrayList<>();

        inboxHeader = new ArrayList<String>();
        Bodies = new ArrayList<String>();
        try {
            boolean result = new ReceiveMailAsyncTask().execute().get();
            if (result) {
                ArrayAdapter<String> dataAdapter = new ArrayAdapter<String>(this, android.R.layout.simple_list_item_1, android.R.id.text1, inboxHeader);
                inboxList.setAdapter(dataAdapter);
            }
        } catch (Exception ex) {
            System.out.println(ex.toString());
        }

        inboxList.setOnItemClickListener((AdapterView<?> parent, View view, int position, long id) -> {
            if (!isSpeakStop) return;
            reco.stopContinuousRecognitionAsync();
            synthesizer.close();
            microphoneStream.close();
            Intent intent = new Intent(this, ReadSingleMailActivity.class);
            intent.putExtra("MAIL_LINE", position);
            startActivity(intent);
        });

        try {
            content.clear();
            audioInput = AudioConfig.fromStreamInput(createMicrophoneStream());
            reco = new SpeechRecognizer(speechConfig, audioInput);

            reco.recognized.addEventListener((o, speechRecognitionResultEventArgs) -> {
                String s = speechRecognitionResultEventArgs.getResult().getText();
                Log.i(logTag, "Final result received: " + s);
                String[] splitedText = s.split("\\.");
                String comparedText = splitedText[0].toLowerCase();

                if (comparedText.equals("quit from application") || comparedText.equals("exit from application")
                        || comparedText.equals("quit application") || comparedText.equals("exit application")
                        || comparedText.equals("quit from app") || comparedText.equals("exit from app")) {
                    finishAffinity();
                    System.exit(1);
                }
                if (isSpeakStop) {
                    switch (comparedText) {
                        case "start message":
                        case "new message":
                        case "start new message":
                        case "create message":
                        case "start email":
                        case "new email":
                        case "start new email":
                        case "create email":
                        case "start mail":
                        case "new mail":
                        case "start new mail":
                        case "create mail":
                            if (!speechSynthesisResult.isCancelled())
                                speechSynthesisResult.cancel(true);
                            newMailButton.callOnClick();

                            break;
                        case "logout":
                        case "log out":
                            if (!speechSynthesisResult.isCancelled())
                                speechSynthesisResult.cancel(true);
                            logoutButton.callOnClick();
                            break;
                        case "repeat command":
                        case "repeat commands":
                        case "help":
                            reco.stopContinuousRecognitionAsync();
                            SpeechSynthesisResult result = synthesizer.SpeakText("Replaying introduction now! ");
                            result.close();
                            result = synthesizer.SpeakText(introductionText);
                            result.close();
                            reco.startContinuousRecognitionAsync();
                            break;
                        case "play last email":
                        case "say last email":
                        case "tell last email":
                        case "read last email":
                            if (messageCounts < 1) break;
                            reco.stopContinuousRecognitionAsync();
                            synthesizer.SpeakText(inboxHeader.get(0));
                            synthesizer.SpeakText("Body: " + Bodies.get(0));
                            reco.startContinuousRecognitionAsync();
                            break;
                        case "read all emails":
                        case "play all emails":
                        case "say all emails":
                        case "tell all emails":
                            if (messageCounts < 1) break;
                            reco.stopContinuousRecognitionAsync();
                            for (int i = 0; i < messageCounts; i++) {
                                synthesizer.SpeakText(inboxHeader.get(i));
                                synthesizer.SpeakText("Body: " + Bodies.get(i));
                            }
                            reco.startContinuousRecognitionAsync();
                            break;
                    }
                }
                content.add(s);
            });

            final Future<Void> task = reco.startContinuousRecognitionAsync();

        } catch (Exception ex) {
            System.out.println(ex.getMessage());
        }
    }

    public void writeNewMail(View view) {
        if (!isSpeakStop) return;
        reco.stopContinuousRecognitionAsync();
        reco.close();
        synthesizer.close();
        speechConfig.close();
        microphoneStream.close();
        Intent intent = new Intent(this, SendMailActivity.class);
        startActivity(intent);
    }

    public void logOut(View view) {
        if (!isSpeakStop) return;
        reco.stopContinuousRecognitionAsync();
        reco.close();
        synthesizer.close();
        speechConfig.close();
        microphoneStream.close();
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.remove("Email");
        editor.remove("Password");
        editor.apply();
        Intent intent = new Intent(this, LoginActivity.class);
        startActivity(intent);
    }

    private MicrophoneStream createMicrophoneStream() {
        if (microphoneStream != null) {
            microphoneStream.close();
            microphoneStream = null;
        }
        microphoneStream = new MicrophoneStream();
        return microphoneStream;
    }

    public SpeechConfig createSpeechConfig(String key, String region) {
        final SpeechConfig speechConfig;
        try {
            speechConfig = SpeechConfig.fromSubscription(key, region);
            return speechConfig;
        } catch (Exception ex) {
            System.out.println(ex.getMessage());
            return null;
        }
    }

    private class ReceiveMailAsyncTask extends AsyncTask<Void, Void, Boolean> {
        private MailChecker mailChecker;
        private Message[] allMessages;
        private int messageCount;

        public ReceiveMailAsyncTask() {
            if (BuildConfig.DEBUG)
                Log.v(ReceiveMailAsyncTask.class.getName(), "ReceiveMailAsyncTask()");
            try {
                mailChecker = new MailChecker(sharedPreferences.getString("Email", "empty"), sharedPreferences.getString("Password", " "));
            } catch (Exception e) {
                Log.e("SendMailActivity", e.getMessage(), e);
            }
        }

        @Override
        protected Boolean doInBackground(Void... params) {
            try {
                mailChecker.login();
                allMessages = mailChecker.getMessages();
                messageCount = mailChecker.getMessageCount();
                messageCounts = messageCount;
                for (int i = messageCount - 1; i >= 0; i--) {
                    String Header = "From: ";
                    Header += allMessages[i].getFrom()[0].toString();
                    Header += "\r\nSubject: ";
                    if (allMessages[i].getSubject() == null) {
                        Header += "empty subject";
                        inboxHeader.add(Header);
                    } else {
                        Header += allMessages[i].getSubject();
                        inboxHeader.add(Header);
                    }
                    getEmailBody(allMessages[i]);
                    if (Body.isEmpty()) {
                        Body = "empty body";
                    }
                    Bodies.add(Body);//Adding for read body with command.
                }
                return true;
            } catch (Exception ex) {
                return false;
            }
        }

        public void getEmailBody(Part p) throws Exception {
            String sa = p.getContentType();
            if (p.isMimeType("multipart/*")) {
                Multipart mp = (Multipart) p.getContent();
                int count = mp.getCount();
                for (int i = 0; i < count; i++)
                    getEmailBody(mp.getBodyPart(i));
            } else if (p.isMimeType("text/plain")) {
                Body = p.getContent().toString();
            } /*else if (p.isMimeType("text/html")) {
                Body = HtmlCompat.fromHtml(p.getContent().toString(), 0).toString();
            }*/ // discuss
        }
    }

    @Override
    public void onBackPressed() {
    }
}
