package com.example.mailapp;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

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

import static android.Manifest.permission.INTERNET;
import static android.Manifest.permission.RECORD_AUDIO;

public class LoginActivity extends AppCompatActivity {

    private static final String SpeechSubscriptionKey = "7f54f290e9b64c45a3d649ecf5d0c7ba";
    private static final String SpeechRegion = "eastus";

    private Button loginButton;
    private TextView emailTextView;
    private TextView passwordTextView;

    private MicrophoneStream microphoneStream;
    private SpeechRecognizer reco;
    private SharedPreferences sharedPreferences;
    private SharedPreferences sharedPreferences2;
    private SpeechConfig speechConfig;
    private SpeechSynthesizer synthesizer;
    private Future<SpeechSynthesisResult> speechSynthesisResult;
    private AudioConfig audioInput;

    private boolean RecordEmail;
    private boolean RecordPassword;
    private List<String> emailInput;
    private List<String> pwInput;
    private String emailAddress;
    private String password;
    private String introductionText;
    private boolean isSpeakStop;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);
        sharedPreferences = getSharedPreferences("LoginInfo", 0);
        String userMail = sharedPreferences.getString("Email", "empty");
        String userPassword = sharedPreferences.getString("Password", " ");

        loginButton = findViewById(R.id.buttonLogin);
        emailTextView = findViewById(R.id.inputEmail);
        passwordTextView = findViewById(R.id.inputPassword);

        introductionText = "Welcome to the mail app!";
        introductionText += "You can use start, begin or enter keywords to fill email and password field. For example start email keyword will begin to listen your email address. ";
        introductionText += "When you finish telling your input, you can use stop or end keyword to finish listening. ";
        introductionText += "When you finish entering email address and password fields. You can use login keyword to enter your email account. ";
        introductionText += "When you want to quit from app, you can use quit application or exit application keywords any where in the application. ";
        introductionText += "If you want to listen this introduction part again. You can use repeat commands or help keywords to replay introduction. ";
        introductionText += "Listening your commands now! ";

        RecordEmail = false;
        RecordPassword = false;
        emailAddress = "";
        password = "";
        emailInput = new ArrayList<String>();
        pwInput = new ArrayList<String>();
        isSpeakStop = false;

        if (!userMail.equals("empty") && !userPassword.equals(" ") && !userPassword.equals("") && userMail.contains("@")) {
            Toast.makeText(this, "Logging In...", Toast.LENGTH_SHORT).show();
            emailTextView.setText(userMail);
            passwordTextView.setText(userPassword);
            boolean result = tryLogin();
            if (result) {
                Toast.makeText(this, "Logged In.", Toast.LENGTH_SHORT).show();
                Intent intent = new Intent(this, MainActivity.class);
                startActivity(intent);
                return;
            }
        }
        try {
            int permissionRequestId = 5;
            ActivityCompat.requestPermissions(LoginActivity.this, new String[]{RECORD_AUDIO, INTERNET}, permissionRequestId);
        } catch (Exception ex) {
            Log.e("SpeechSDK", "could not init sdk, " + ex.toString());
        }
        try {
            speechConfig = createSpeechConfig(SpeechSubscriptionKey, SpeechRegion);
            synthesizer = new SpeechSynthesizer(speechConfig);
            sharedPreferences2 = getSharedPreferences("IntroSpeaksLogin", 0);
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
                speechSynthesisResult = synthesizer.StartSpeakingTextAsync("You are in login page! Listening your commands now!");
                synthesizer.SynthesisCompleted.addEventListener((o, e) -> {
                    e.close();
                    speechSynthesisResult.cancel(true);
                    isSpeakStop = true;
                });
            }
        } catch (Exception e) {
            Log.e("LoginOnCreateException:", e.getMessage());
        }
        speechConfig = createSpeechConfig(SpeechSubscriptionKey, SpeechRegion);
        synthesizer = new SpeechSynthesizer(speechConfig);
        final String logTag = "Login reco 3";
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
                    if (!RecordPassword && !RecordEmail) {
                        switch (comparedText) {
                            case "start email":
                            case "begin email":
                            case "enter email":
                            case "start mail":
                            case "begin mail":
                            case "enter mail": {
                                emailAddress = "";
                                reco.stopContinuousRecognitionAsync();
                                String speakText = "Recording email now";
                                SpeechSynthesisResult result = synthesizer.SpeakText(speakText);
                                result.close();
                                RecordEmail = true;
                                reco.startContinuousRecognitionAsync();
                                break;
                            }
                            case "start password":
                            case "begin password":
                            case "enter password": {
                                password = "";
                                reco.stopContinuousRecognitionAsync();
                                String speakText = "Recording password now";
                                SpeechSynthesisResult result = synthesizer.SpeakText(speakText);
                                result.close();
                                RecordPassword = true;
                                reco.startContinuousRecognitionAsync();
                                break;
                            }
                            case "repeat commands":
                            case "repeat command":
                            case "help": {
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
                            }
                            case "login":
                            case "log in":
                            case "looking": {
                                String speakText = "Logging in now";
                                SpeechSynthesisResult result = synthesizer.SpeakText(speakText);
                                result.close();
                                if (!speechSynthesisResult.isCancelled())
                                    speechSynthesisResult.cancel(true);

                                loginButton.callOnClick();
                                break;
                            }
                        }
                    } else if (RecordPassword) {
                        if (comparedText.equals("stop") || comparedText.equals("end")) {
                            for (int i = 0; i < pwInput.size(); i++) {
                                password += pwInput.get(i).replace(" ", "");
                            }
                            changeTextView(passwordTextView, password);
                            reco.stopContinuousRecognitionAsync();
                            String speakText = "Password recorded";
                            SpeechSynthesisResult result = synthesizer.SpeakText(speakText);
                            result.close();
                            pwInput.clear();
                            RecordPassword = false;
                            reco.startContinuousRecognitionAsync();
                        } else {
                            if (!s.isEmpty() && s.charAt(s.length() - 1) == '.') {
                                s = s.substring(0, s.length() - 1);
                            }
                            pwInput.add(s.toLowerCase());
                        }
                    } else if (RecordEmail) {
                        if (comparedText.equals("stop") || comparedText.equals("end")) {
                            for (int i = 0; i < emailInput.size(); i++) {
                                emailAddress += emailInput.get(i).replace(" ", "");
                            }
                            changeTextView(emailTextView, emailAddress);
                            reco.stopContinuousRecognitionAsync();
                            String speakText = "Email recorded";
                            SpeechSynthesisResult result = synthesizer.SpeakText(speakText);
                            result.close();

                            emailInput.clear();

                            RecordEmail = false;
                            reco.startContinuousRecognitionAsync();
                        } else {
                            if (!s.isEmpty() && s.charAt(s.length() - 1) == '.') {
                                s = s.substring(0, s.length() - 1);
                            }
                            emailInput.add(s.toLowerCase());
                        }
                    }
                }
            });

            final Future<Void> task = reco.startContinuousRecognitionAsync();

        } catch (Exception ex) {
            System.out.println(ex.getMessage());
        }
    }

    @Override
    public void onBackPressed() {
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

    private boolean tryLogin() {
        emailAddress = emailTextView.getText().toString();
        password = passwordTextView.getText().toString();
        if (emailAddress.isEmpty() || password.isEmpty() || !emailAddress.contains("@"))
            return false;
        MailChecker checker = new MailChecker(emailAddress, password);
        Boolean result = false;
        try {
            result = new LoginAsyncTask(checker).execute().get();
        } catch (Exception e) {
            e.printStackTrace();
        }
        return result;
    }

    public void onLogin(View view) {
        boolean result = tryLogin();
        if (!isSpeakStop) return;
        if (result) {
            reco.stopContinuousRecognitionAsync();
            synthesizer.close();
            speechConfig.close();
            microphoneStream.close();
            SharedPreferences.Editor editor = sharedPreferences.edit();
            editor.putString("Email", emailAddress);
            editor.putString("Password", password);
            editor.apply();
            Intent intent = new Intent(this, MainActivity.class);
            startActivity(intent);
        } else {
            String errorText = "Login failed! Please try again! ";
            SpeechSynthesisResult res = synthesizer.SpeakText(errorText);
            res.close();
        }
    }

    private void changeTextView(TextView textView, String text) {
        LoginActivity.this.runOnUiThread(() -> {
            textView.setText(text);
        });
    }
    private class LoginAsyncTask extends AsyncTask<Void, Void, Boolean> {
        MailChecker checker;

        public LoginAsyncTask(MailChecker checker) {
            this.checker = checker;
            if (BuildConfig.DEBUG)
                Log.v(LoginAsyncTask.class.getName(), "SendEmailAsyncTask()");
        }

        @Override
        protected Boolean doInBackground(Void... params) {
            try {
                checker.login();
                return checker.isLoggedIn();
            } catch (Exception e) {
                e.fillInStackTrace();
                return false;
            }
        }
    }
}