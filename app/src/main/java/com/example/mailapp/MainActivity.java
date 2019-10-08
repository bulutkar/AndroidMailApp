package com.example.mailapp;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageButton;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import com.microsoft.cognitiveservices.speech.SpeechConfig;
import com.microsoft.cognitiveservices.speech.SpeechRecognizer;
import com.microsoft.cognitiveservices.speech.SpeechSynthesisResult;
import com.microsoft.cognitiveservices.speech.SpeechSynthesizer;
import com.microsoft.cognitiveservices.speech.audio.AudioConfig;

import java.util.ArrayList;
import java.util.concurrent.Future;

import static android.Manifest.permission.INTERNET;
import static android.Manifest.permission.RECORD_AUDIO;

public class MainActivity extends AppCompatActivity {
    private static final String SpeechSubscriptionKey = "7f54f290e9b64c45a3d649ecf5d0c7ba";
    private static final String SpeechRegion = "eastus";

    private SharedPreferences sharedPreferences;
    private ImageButton newMailButton;
    private Button logoutButton;

    private SpeechConfig speechConfig;
    private SpeechSynthesizer synthesizer;
    private MicrophoneStream microphoneStream;
    private SpeechRecognizer reco;
    private boolean isSpeakStop;
    private String introductionText;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        sharedPreferences = getSharedPreferences("LoginInfo", 0);
        newMailButton = findViewById(R.id.new_mail);
        logoutButton = findViewById(R.id.logoutButton);

        isSpeakStop = true;

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

        speechConfig = createSpeechConfig(SpeechSubscriptionKey, SpeechRegion);
        synthesizer = new SpeechSynthesizer(speechConfig);
        final String logTag = "reco 3";
        AudioConfig audioInput;
        ArrayList<String> content = new ArrayList<>();

        try {
            content.clear();
            audioInput = AudioConfig.fromStreamInput(createMicrophoneStream());
            reco = new SpeechRecognizer(speechConfig, audioInput);
            sharedPreferences = getSharedPreferences("IntroSpeaksMain", 0);
            boolean isRead = sharedPreferences.getBoolean("isRead", false);
            Future<SpeechSynthesisResult> speechSynthesisResult;
            if (!isRead) {
                speechSynthesisResult = synthesizer.SpeakTextAsync(introductionText);
                synthesizer.SynthesisCompleted.addEventListener((o, e) -> {
                    e.close();
                    speechSynthesisResult.cancel(true);
                    isSpeakStop = true;
                    SharedPreferences.Editor editor = sharedPreferences.edit();
                    editor.putBoolean("IntroSpeaksMain", true);
                    editor.apply();
                });
            } else {
                speechSynthesisResult = synthesizer.SpeakTextAsync("Listening your commands now!");
                synthesizer.SynthesisCompleted.addEventListener((o, e) -> {
                    e.close();
                    speechSynthesisResult.cancel(true);
                    isSpeakStop = true;
                });
            }

            reco.recognized.addEventListener((o, speechRecognitionResultEventArgs) -> {
                String s = speechRecognitionResultEventArgs.getResult().getText();
                Log.i(logTag, "Final result received: " + s);
                String[] splitedText = s.split("\\.");
                String comparedText = splitedText[0].toLowerCase();

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
                            newMailButton.callOnClick();

                            break;
                        case "logout":
                        case "log out":
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
        reco.stopContinuousRecognitionAsync();
        synthesizer.close();
        microphoneStream.close();
        Intent intent = new Intent(this, SendMailActivity.class);
        startActivity(intent);
    }

    public void logOut(View view) {
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
}
