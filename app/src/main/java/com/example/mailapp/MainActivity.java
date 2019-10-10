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
        introductionText += "You can use start mail, new mail, start new mail or create mail keywords to send e new mail. ";
        introductionText += "You can use logout keyword to logout from your account, you will be redirected to login screen. ";
        introductionText += "If you want to listen this introduction part again, you can use repeat commands keyword to replay introduction. ";
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

    }

    @Override
    public void onStart() {
        super.onStart();
        final String logTag = "reco 3";
        ArrayList<String> content = new ArrayList<>();

        inboxHeader = new ArrayList<String>();
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
            microphoneStream.close();
            synthesizer.close();
            reco.stopContinuousRecognitionAsync();
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
                    android.os.Process.killProcess(android.os.Process.myPid());
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
                            reco.stopContinuousRecognitionAsync();
                            SpeechSynthesisResult result = synthesizer.SpeakText("Replaying introduction now! ");
                            result.close();
                            result = synthesizer.SpeakText(introductionText);
                            result.close();
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
        synthesizer.close();
        microphoneStream.close();
        Intent intent = new Intent(this, SendMailActivity.class);
        startActivity(intent);
    }

    public void logOut(View view) {
        if (!isSpeakStop) return;
        reco.stopContinuousRecognitionAsync();
        synthesizer.close();
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
                for (int i = messageCount - 1; i >= 0; i--) {
                    if (allMessages[i].getSubject() == null) {
                        inboxHeader.add("empty subject");
                    } else {
                        inboxHeader.add(allMessages[i].getSubject());
                    }
                }
                return true;
            } catch (Exception ex) {
                return false;
            }
        }
    }

    @Override
    public void onBackPressed() {
    }
}
