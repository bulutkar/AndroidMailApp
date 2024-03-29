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

import javax.mail.*;
import java.util.concurrent.Future;

import static android.Manifest.permission.INTERNET;
import static android.Manifest.permission.RECORD_AUDIO;

public class ReadSingleMailActivity extends AppCompatActivity {
    private static final String SpeechSubscriptionKey = "7f54f290e9b64c45a3d649ecf5d0c7ba";
    private static final String SpeechRegion = "eastus";

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
    private Message[] allMessages;
    private int messageCount;

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
        introductionText += "You can use go back or return keywords to return to main screen. ";
        introductionText += "You can delete mail using delete message, delete email or delete mail keywords. ";
        introductionText += "When you want to quit from app, you can use quit application or exit application keywords any where in the application. ";
        introductionText += "If you want to listen this introduction part again, you can use repeat commands or help keywords to replay introduction. ";
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
            } else {
                speechSynthesisResult = synthesizer.SpeakTextAsync("You are in read single mail page! Listening your commands now!");
                synthesizer.SynthesisCompleted.addEventListener((o, e) -> {
                    e.close();
                    speechSynthesisResult.cancel(true);
                    isSpeakStop = true;
                });
            }
        } catch (Exception e) {
            Log.e("ReadMailOnCreateEx", e.getMessage());
        }
        final String logTag = "Single reco 3";
        try {
            boolean result = new ReadSingleMailAsyncTask().execute().get();
            if (result) {
                SetEmail();
            }
        } catch (Exception ex) {
            System.out.println(ex.toString());
        }
        try {
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
                        case "read sender":
                        case "tell sender":
                        case "say sender":
                        case "read from":
                        case "tell from":
                        case "say from":
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
                            reco.startContinuousRecognitionAsync();
                            break;
                        case "repeat command":
                        case "repeat commands":
                        case "help":
                            isSpeakStop = false;
                            reco.stopContinuousRecognitionAsync();
                            String speakText = "Replaying introduction now";
                            SpeechSynthesisResult result = synthesizer.SpeakText(speakText);
                            result.close();
                            result = synthesizer.SpeakText(introductionText);
                            result.close();
                            reco.startContinuousRecognitionAsync();
                            isSpeakStop = true;
                            break;
                        case "delete message":
                        case "delete email":
                        case "delete mail": {
                            try {
                                allMessages[messageCount - EmailId - 1].setFlag(Flags.Flag.DELETED, true);
                                synthesizer.SpeakText("Message is deleted.");
                                back_button.callOnClick();
                            } catch (MessagingException e) {
                                e.printStackTrace();
                                synthesizer.SpeakText("Something was wrong. Message could not be deleted.");
                                reco.startContinuousRecognitionAsync();
                            }
                            break;
                        }
                        case "back":
                        case "go back":
                        case "return":
                            String successText = "Going back to main screen. ";
                            synthesizer.SpeakText(successText);
                            back_button.callOnClick();
                            break;
                    }
                }
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
    }

    public void onBack(View view) {
        if (!isSpeakStop) return;
        reco.stopContinuousRecognitionAsync();
        synthesizer.close();
        speechConfig.close();
        microphoneStream.close();
        Intent intent = new Intent(this, MainActivity.class);
        startActivity(intent); // change later
    }

    @Override
    public void onBackPressed() {
        if (!isSpeakStop) return;
        reco.stopContinuousRecognitionAsync();
        synthesizer.close();
        speechConfig.close();
        microphoneStream.close();
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
                getEmailBody(allMessages[messageCount - EmailId - 1]);
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
            }
        }
    }
}
