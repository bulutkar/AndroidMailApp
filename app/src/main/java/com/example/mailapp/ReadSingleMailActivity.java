package com.example.mailapp;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import com.microsoft.cognitiveservices.speech.SpeechConfig;
import com.microsoft.cognitiveservices.speech.SpeechRecognizer;
import com.microsoft.cognitiveservices.speech.SpeechSynthesisResult;
import com.microsoft.cognitiveservices.speech.SpeechSynthesizer;
import com.microsoft.cognitiveservices.speech.audio.AudioConfig;

import java.util.ArrayList;
import java.util.concurrent.Future;

import javax.mail.Address;
import javax.mail.Message;
import javax.mail.Multipart;
import javax.mail.Part;

import static android.Manifest.permission.INTERNET;
import static android.Manifest.permission.RECORD_AUDIO;

public class ReadSingleMailActivity extends AppCompatActivity {
    private static final String SpeechSubscriptionKey = "49551d7f82684ae196690097a1c79e0f";
    private static final String SpeechRegion = "westus";

    private MicrophoneStream microphoneStream;
    private SpeechRecognizer reco;
    private SpeechConfig speechConfig;
    private SpeechSynthesizer synthesizer;
    Future<SpeechSynthesisResult> speechSynthesisResult;
    AudioConfig audioInput;

    private SharedPreferences sharedPreferences;
    private SharedPreferences sharedPreferences2;
    private TextView MailBody;
    private TextView MailSubject;
    private TextView MailFrom;
    private Button back_button;

    private int EmailId;
    private String Subject;
    private Address[] From;
    private String Body;
    private boolean isSpeakStop;
    private String introductionText;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_read_single_mail);
        sharedPreferences = getSharedPreferences("LoginInfo", 0);
        MailBody = findViewById(R.id.mailBody);
        MailSubject = findViewById(R.id.mailSubject);
        MailFrom = findViewById(R.id.mailFrom);
        back_button = findViewById(R.id.back_button);
        Bundle extras = getIntent().getExtras();
        if (extras != null) {
            EmailId = extras.getInt("MAIL_LINE");
        }

        isSpeakStop = false;

        introductionText = "Welcome to read single mail page! ";
        introductionText += "You can use read, tell or say keywords to hear mail's sender, subject and body. ";
        introductionText += "For example read subject will tell you the subject of the mail. ";
        introductionText += "If you want to listen this introduction part again, you can use repeat commands keyword to replay introduction. ";
        introductionText += "Listening your commands now! ";


        try {
            int permissionRequestId = 5;
            ActivityCompat.requestPermissions(ReadSingleMailActivity.this, new String[]{RECORD_AUDIO, INTERNET}, permissionRequestId);
        } catch (Exception ex) {
            Log.e("SpeechSDK", "could not init sdk, " + ex.toString());
        }

        try {
            speechConfig = createSpeechConfig(SpeechSubscriptionKey, SpeechRegion);
            synthesizer = new SpeechSynthesizer(speechConfig);
            sharedPreferences2 = getSharedPreferences("IntroSpeaksReadSingleMail", 0);
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
            }
            else {
                speechSynthesisResult = synthesizer.SpeakTextAsync("You are in main page! Listening your commands now!");
                synthesizer.SynthesisCompleted.addEventListener((o, e) -> {
                    e.close();
                    speechSynthesisResult.cancel(true);
                    isSpeakStop = true;
                });
            }
        } catch (Exception e) {
            Log.e("ReadMailOnCreateEx", e.getMessage());
        }


    }

    @Override
    public void onStart() {
        super.onStart();
        final String logTag = "reco 3";
        ArrayList<String> content = new ArrayList<>();

        try {
            boolean result = new ReadSingleMailAsyncTask().execute().get();
            if (result) {
                SetEmail();
            }
        } catch (Exception ex) {
            System.out.println(ex.toString());
        }

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
                        case "read sender":
                        case "tell sender":
                        case "say sender":
                            reco.stopContinuousRecognitionAsync();
                            synthesizer.SpeakText(From[0].toString());
                            reco.startContinuousRecognitionAsync();
                            break;
                        case "read subject":
                        case "tell subject":
                        case "say subject":
                            reco.stopContinuousRecognitionAsync();
                            synthesizer.SpeakText(Subject);
                            reco.startContinuousRecognitionAsync();
                            break;
                        case "read body":
                        case "tell body":
                        case "say body":
                            reco.stopContinuousRecognitionAsync();
                            synthesizer.SpeakText(Body);
                            /*speechSynthesisResult = synthesizer.SpeakTextAsync(Body);
                            synthesizer.SynthesisCompleted.addEventListener((ob, e) -> {
                                e.close();
                                speechSynthesisResult.cancel(true);
                            });*/
                            reco.startContinuousRecognitionAsync();
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

    private MicrophoneStream createMicrophoneStream() {
        if (microphoneStream != null) {
            microphoneStream.close();
            microphoneStream = null;
        }
        microphoneStream = new MicrophoneStream();
        return microphoneStream;
    }

    public void SetEmail() {
        for (int i = 0; i < From.length; i++) {
            changeTextView(MailFrom, From[i].toString());
        }
        changeTextView(MailSubject, Subject);
        changeTextView(MailBody, Body);
        try {
        } catch (Exception ex) {
            Log.e("SetMailException", ex.getMessage());
        }
    }

    public void onBack(View view) {
        if (!isSpeakStop) return;
        microphoneStream.close();
        synthesizer.close();
        reco.stopContinuousRecognitionAsync();
        Intent intent = new Intent(this, MainActivity.class);
        startActivity(intent); // change later
    }

    @Override
    public void onBackPressed() {
        if (!isSpeakStop) return;
        microphoneStream.close();
        synthesizer.close();
        reco.stopContinuousRecognitionAsync();
        Intent intent = new Intent(this, MainActivity.class);
        startActivity(intent); // change later
    }

    private void changeTextView(TextView textView, String text) {
        ReadSingleMailActivity.this.runOnUiThread(() -> {
            textView.setText(text);
        });
    }

    private class ReadSingleMailAsyncTask extends AsyncTask<Void, Void, Boolean> {
        private MailChecker mailChecker;
        private Message[] allMessages;
        private int messageCount;

        public ReadSingleMailAsyncTask() {
            try {
                mailChecker = new MailChecker(sharedPreferences.getString("Email", "empty"), sharedPreferences.getString("Password", " "));
            } catch (Exception ex) {
                Log.e("ReadSingleMailActivity", ex.getMessage(), ex);
            }
        }

        @Override
        protected Boolean doInBackground(Void... params) {
            try {
                mailChecker.login();
                allMessages = mailChecker.getMessages();
                messageCount = mailChecker.getMessageCount();
                Subject = allMessages[messageCount - EmailId - 1].getSubject();
                From = allMessages[messageCount - EmailId - 1].getFrom();
                GetEmailBody(allMessages[messageCount - EmailId - 1]);
                return true;
            } catch (Exception ex) {
                return false;
            }
        }

        public void GetEmailBody(Part p) throws Exception {
            if (p.isMimeType("multipart/*")) {
                Multipart mp = (Multipart) p.getContent();
                int count = mp.getCount();
                for (int i = 0; i < count; i++)
                    GetEmailBody(mp.getBodyPart(i));
            } else if (p.isMimeType("text/plain")) {
                Body = p.getContent().toString();
            }
        }
    }
}
