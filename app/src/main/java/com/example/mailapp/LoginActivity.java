package com.example.mailapp;

import android.app.ProgressDialog;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import com.microsoft.cognitiveservices.speech.SpeechConfig;
import com.microsoft.cognitiveservices.speech.SpeechRecognizer;
import com.microsoft.cognitiveservices.speech.audio.AudioConfig;
import com.microsoft.cognitiveservices.speech.SpeechSynthesisCancellationDetails;
import com.microsoft.cognitiveservices.speech.SpeechSynthesisResult;
import com.microsoft.cognitiveservices.speech.SpeechSynthesizer;
import com.microsoft.cognitiveservices.speech.ResultReason;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import static android.Manifest.permission.INTERNET;
import static android.Manifest.permission.RECORD_AUDIO;

public class LoginActivity extends AppCompatActivity {

    private static final String SpeechSubscriptionKey = "7f54f290e9b64c45a3d649ecf5d0c7ba";
    private static final String SpeechRegion = "eastus";
    private SpeechRecognizer reco;
    private TextView recognizedTextView;
    private Button loginButton;
    private TextView emailTextView;
    private TextView passwordTextView;
    private ProgressDialog progressDialog;

    private MicrophoneStream microphoneStream;
    private String emailAddress;
    private String password;
    private SharedPreferences sharedPreferences;
    private SpeechConfig speechConfig;
    private SpeechSynthesizer synthesizer;

    private boolean RecordEmail;
    private boolean RecordPassword;
    private boolean onLaunch;
    private List<String> emailInput;
    private List<String> pwInput;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);
        sharedPreferences = getSharedPreferences("LoginInfo", 0);
        String userMail = sharedPreferences.getString("Email", "empty");
        String userPassword = sharedPreferences.getString("Password", " ");

        recognizedTextView = findViewById(R.id.recognizedText);
        loginButton = findViewById(R.id.buttonLogin);
        emailTextView = findViewById(R.id.inputEmail);
        passwordTextView = findViewById(R.id.inputPassword);

        speechConfig = createSpeechConfig(SpeechSubscriptionKey, SpeechRegion);
        assert(speechConfig != null);
        synthesizer = new SpeechSynthesizer(speechConfig);
        assert(synthesizer != null);

        RecordEmail = false;
        RecordPassword = false;
        onLaunch = true;
        emailAddress = "";
        password = "";
        emailInput = new ArrayList<String>();
        pwInput = new ArrayList<String>();

        if (!userMail.equals("empty") && !userPassword.equals(" ")) {
            Toast.makeText(this, "Logging In...", Toast.LENGTH_SHORT).show();
            emailTextView.setText(userMail);
            passwordTextView.setText(userPassword);
            Boolean result = tryLogin();
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
            recognizedTextView.setText("Could not initialize: " + ex.toString());
        }

        final String logTag = "reco 3";

        AudioConfig audioInput;
        ArrayList<String> content = new ArrayList<>();
        clearTextBox();

        try {
            content.clear();
            audioInput = AudioConfig.fromStreamInput(createMicrophoneStream());
            reco = new SpeechRecognizer(speechConfig, audioInput);

            reco.recognized.addEventListener((o, speechRecognitionResultEventArgs) -> {
                String s = speechRecognitionResultEventArgs.getResult().getText();
                Log.i(logTag, "Final result received: " + s);
                String[] splitedText = s.split("\\.");
                String comparedText = splitedText[0].toLowerCase();

                if (onLaunch) {
                    reco.stopContinuousRecognitionAsync();

                    String speakText = "Welcome to the mail app!";
                    SpeechSynthesisResult result = synthesizer.SpeakText(speakText);
                    assert (result != null);
                    result.close();

                    speakText = "You can use start, begin or enter keywords to fill email and password field. For example start email keyword will begin to listen your email address. " +
                            "When you finish telling your input, you can use stop or end keyword to finish listening.";
                    result = synthesizer.SpeakText(speakText);
                    assert (result != null);
                    result.close();

                    speakText = "When you finish entering email address and password fields. You can use login keyword to enter your email account.";
                    result = synthesizer.SpeakText(speakText);
                    assert (result != null);
                    result.close();

                    speakText = "If you want to listen this inroduction part again. You can use repeat commands keyword to replay introduction.";
                    result = synthesizer.SpeakText(speakText);
                    assert (result != null);
                    result.close();

                    speakText = "Listening your commands now.";
                    result = synthesizer.SpeakText(speakText);
                    assert (result != null);
                    result.close();

                    onLaunch = false;
                    reco.startContinuousRecognitionAsync();
                }

                if (!RecordPassword && !RecordEmail) {
                    if (comparedText.equals("start email") || comparedText.equals("begin email") || comparedText.equals("enter email")
                    || comparedText.equals("start mail") || comparedText.equals("begin mail") || comparedText.equals("enter mail")) {
                        emailAddress = "";

                        reco.stopContinuousRecognitionAsync();
                        String speakText = "Recording email now";
                        SpeechSynthesisResult result = synthesizer.SpeakText(speakText);
                        assert (result != null);
                        result.close();

                        RecordEmail = true;
                        reco.startContinuousRecognitionAsync();
                    }
                    else if (comparedText.equals("start password") || comparedText.equals("begin password") || comparedText.equals("enter password")) {
                        password = "";

                        reco.stopContinuousRecognitionAsync();
                        String speakText = "Recording password now";
                        SpeechSynthesisResult result = synthesizer.SpeakText(speakText);
                        assert (result != null);
                        result.close();

                        RecordPassword = true;
                        reco.startContinuousRecognitionAsync();
                    }
                    else if (comparedText.equals("repeat commands")) {
                        reco.stopContinuousRecognitionAsync();

                        String speakText = "Replaying introduction now";
                        SpeechSynthesisResult result = synthesizer.SpeakText(speakText);
                        assert (result != null);
                        result.close();

                        onLaunch = true;
                        reco.startContinuousRecognitionAsync();
                    }
                }
                else if (RecordPassword) {
                    if (comparedText.equals("stop") || comparedText.equals("end")) {
                        for (int i = 0; i < pwInput.size(); i++) {
                            password += pwInput.get(i).replace(" ", "");
                        }
                        changeTextView(passwordTextView, password);

                        reco.stopContinuousRecognitionAsync();
                        String speakText = "Password recorded";
                        SpeechSynthesisResult result = synthesizer.SpeakText(speakText);
                        assert (result != null);
                        result.close();

                        pwInput.clear();

                        RecordPassword = false;
                        reco.startContinuousRecognitionAsync();
                    }
                    else if (comparedText.equals("repeat commands")) {
                        reco.stopContinuousRecognitionAsync();

                        String speakText = "Replaying introduction now";
                        SpeechSynthesisResult result = synthesizer.SpeakText(speakText);
                        assert (result != null);
                        result.close();

                        onLaunch = true;
                        reco.startContinuousRecognitionAsync();
                    }
                    else {
                        if (s != null && s.length() > 0 && s.charAt(s.length() - 1) == '.') {
                            s = s.substring(0, s.length() - 1);
                        }
                        pwInput.add(s.toLowerCase());
                    }
                }
                else if (RecordEmail) {
                    if (comparedText.equals("stop") || comparedText.equals("end")) {
                        for (int i = 0; i < emailInput.size(); i++) {
                            emailAddress += emailInput.get(i).replace(" ", "");
                        }
                        changeTextView(emailTextView, emailAddress);

                        reco.stopContinuousRecognitionAsync();
                        String speakText = "Email recorded";
                        SpeechSynthesisResult result = synthesizer.SpeakText(speakText);
                        assert (result != null);
                        result.close();

                        emailInput.clear();

                        RecordEmail = false;
                        reco.startContinuousRecognitionAsync();
                    }
                    else if (comparedText.equals("repeat commands")) {
                        reco.stopContinuousRecognitionAsync();

                        String speakText = "Replaying introduction now";
                        SpeechSynthesisResult result = synthesizer.SpeakText(speakText);
                        assert (result != null);
                        result.close();

                        onLaunch = true;
                        reco.startContinuousRecognitionAsync();
                    }
                    else {
                        if (s != null && s.length() > 0 && s.charAt(s.length() - 1) == '.') {
                            s = s.substring(0, s.length() - 1);
                        }
                        emailInput.add(s.toLowerCase());
                    }
                }

                if (comparedText.equals("login") || comparedText.equals("log in") || comparedText.equals("looking")) {
                    //Button login = findViewById(R.id.buttonLogin);
                    String speakText = "Logging in now";
                    SpeechSynthesisResult result = synthesizer.SpeakText(speakText);
                    assert result != null;
                    result.close();

                    synthesizer.close();
                    speechConfig.close();

                    loginButton.callOnClick();
                }
                content.add(s);
                setRecognizedText(TextUtils.join(" ", content));
            });

            final Future<Void> task = reco.startContinuousRecognitionAsync();

        } catch (Exception ex) {
            System.out.println(ex.getMessage());
            displayException(ex);
        }

    }

    public SpeechConfig createSpeechConfig(String key, String region) {
        final SpeechConfig speechConfig;
        try {
            speechConfig = SpeechConfig.fromSubscription(key, region);
            return speechConfig;
        } catch (Exception ex) {
            System.out.println(ex.getMessage());
            displayException(ex);
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
        if (emailAddress.isEmpty() || password.isEmpty()) return false;
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
        Boolean result = tryLogin();
        if (result) {
            reco.stopContinuousRecognitionAsync();
            SharedPreferences.Editor editor = sharedPreferences.edit();
            editor.putString("Email", emailAddress);
            editor.putString("Password", password);
            editor.apply();
            Intent intent = new Intent(this, MainActivity.class);
            startActivity(intent);
        }
    }

    private void displayException(Exception ex) {
        recognizedTextView.setText(ex.getMessage() + System.lineSeparator() + TextUtils.join(System.lineSeparator(), ex.getStackTrace()));
    }

    private void clearTextBox() {
        AppendTextLine("", true);
    }

    private void changeTextView(TextView textView, String text) {
        LoginActivity.this.runOnUiThread(() -> {
            textView.setText(text);
        });
    }

    private void setRecognizedText(final String s) {
        AppendTextLine(s, true);
    }

    private void AppendTextLine(final String s, final Boolean erase) {
        LoginActivity.this.runOnUiThread(() -> {
            if (erase) {
                recognizedTextView.setText(s);
            } else {
                String txt = recognizedTextView.getText().toString();
                recognizedTextView.setText(txt + System.lineSeparator() + s);
            }
        });
    }

    private void disableButtons() {
        LoginActivity.this.runOnUiThread(() -> {

        });
    }

    private void enableButtons() {
        LoginActivity.this.runOnUiThread(() -> {

        });
    }

    private void changeProgressDialogText(String text) {
        LoginActivity.this.runOnUiThread(() -> {
            progressDialog.setMessage(text);
        });
    }

    private <T> void setOnTaskCompletedListener(Future<T> task, OnTaskCompletedListener<T> listener) {
        s_executorService.submit(() -> {
            T result = task.get();
            listener.onCompleted(result);
            return null;
        });
    }

    private interface OnTaskCompletedListener<T> {
        void onCompleted(T taskResult);
    }

    private static ExecutorService s_executorService;

    static {
        s_executorService = Executors.newCachedThreadPool();
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