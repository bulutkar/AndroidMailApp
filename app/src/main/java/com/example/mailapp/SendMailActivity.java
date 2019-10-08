package com.example.mailapp;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ImageButton;
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

import javax.mail.AuthenticationFailedException;
import javax.mail.MessagingException;
import javax.mail.internet.MimeMessage;

import static android.Manifest.permission.INTERNET;
import static android.Manifest.permission.RECORD_AUDIO;

public class SendMailActivity extends AppCompatActivity {
    private static final String SpeechSubscriptionKey = "7f54f290e9b64c45a3d649ecf5d0c7ba";
    private static final String SpeechRegion = "eastus";

    private TextView email_from;
    private TextView email_to;
    private TextView email_subject;
    private TextView email_body;
    private ImageButton send_button;

    private String fromEmail;
    private String toEmail;
    private String subject;
    private String body;
    private String user;
    private String password;

    SharedPreferences sharedPreferences;
    private String userMail;
    private String userPassword;

    private SpeechConfig speechConfig;
    private SpeechSynthesizer synthesizer;
    private MicrophoneStream microphoneStream;
    private SpeechRecognizer reco;

    private String introductionText;
    private boolean RecordReceiver;
    private boolean RecordSubject;
    private boolean RecordBody;
    private List<String> ReceiverInput;
    private List<String> SubjectInput;
    private List<String> BodyInput;
    private boolean isSpeakStop;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_send_mail);

        email_from = findViewById(R.id.email_from);
        email_to = findViewById(R.id.email_to);
        email_subject = findViewById(R.id.email_subject);
        email_body = findViewById(R.id.email_body);
        send_button = findViewById(R.id.email_send_button);

        sharedPreferences = getSharedPreferences("LoginInfo", 0);
        userMail = sharedPreferences.getString("Email", "");
        userPassword = sharedPreferences.getString("Password", "");
        changeTextView(email_from, userMail);
        password = userPassword;

        introductionText = "Welcome to the send mail page! ";
        introductionText += "You can use start, begin or enter keywords to fill receiver, subject and body of the mail. ";
        introductionText += "For example, start subject keyword will begin to listen the subject of your mail. ";
        introductionText += "When you finish telling your input, you can use stop or end keyword to finish listening. ";
        introductionText += "When you finish entering all the necessary fields. You can use send or deliver keyword to send the email. ";
        introductionText += "You will get a response whether your email is send or not. ";
        introductionText += "If you want to listen this introduction part again. You can use repeat commands keyword to replay introduction. ";
        introductionText += "Listening your commands now!";

        RecordBody = false;
        RecordReceiver = false;
        RecordSubject = false;

        ReceiverInput = new ArrayList<String>();
        SubjectInput = new ArrayList<String>();
        BodyInput = new ArrayList<String>();

        try {
            int permissionRequestId = 5;
            ActivityCompat.requestPermissions(SendMailActivity.this, new String[]{RECORD_AUDIO, INTERNET}, permissionRequestId);
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
            sharedPreferences = getSharedPreferences("IntroSpeaksSendMail", 0);
            boolean isRead = sharedPreferences.getBoolean("isRead", false);
            Future<SpeechSynthesisResult> speechSynthesisResult;
            if (!isRead) {
                speechSynthesisResult = synthesizer.SpeakTextAsync(introductionText);
                synthesizer.SynthesisCompleted.addEventListener((o, e) -> {
                    e.close();
                    speechSynthesisResult.cancel(true);
                    isSpeakStop = true;
                    SharedPreferences.Editor editor = sharedPreferences.edit();
                    editor.putBoolean("IntroSpeaksSendMail", true);
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
                    if (comparedText.equals("repeat command") || comparedText.equals("repeat commands")) {
                        reco.stopContinuousRecognitionAsync();
                        String speakText = "Replaying introduction now";
                        SpeechSynthesisResult result = synthesizer.SpeakText(speakText);
                        result.close();
                        result = synthesizer.SpeakText(introductionText);
                        result.close();
                        reco.startContinuousRecognitionAsync();
                    } else if (!RecordSubject && !RecordReceiver && !RecordBody) {
                        switch (comparedText) {
                            case "start receiver":
                            case "begin receiver":
                            case "enter receiver":
                            case "start receivers":
                            case "begin receivers":
                            case "enter receivers": {
                                toEmail = "";
                                reco.stopContinuousRecognitionAsync();
                                String speakText = "Recording receiver email address now! ";
                                SpeechSynthesisResult result = synthesizer.SpeakText(speakText);
                                result.close();
                                RecordReceiver = true;
                                reco.startContinuousRecognitionAsync();
                                break;
                            }
                            case "start subject":
                            case "begin subject":
                            case "enter subject":
                            case "start subjects":
                            case "begin subjects":
                            case "enter subjects": {
                                subject = "";
                                reco.stopContinuousRecognitionAsync();
                                String speakText = "Recording subject of the mail now! ";
                                SpeechSynthesisResult result = synthesizer.SpeakText(speakText);
                                result.close();
                                RecordSubject = true;
                                reco.startContinuousRecognitionAsync();
                                break;
                            }
                            case "start body":
                            case "begin body":
                            case "enter body": {
                                body = "";
                                reco.stopContinuousRecognitionAsync();
                                String speakText = "Recording body of the mail now! ";
                                SpeechSynthesisResult result = synthesizer.SpeakText(speakText);
                                result.close();
                                RecordBody = true;
                                reco.startContinuousRecognitionAsync();
                                break;
                            }
                        }
                    } else if (RecordBody) {
                        if (comparedText.equals("stop") || comparedText.equals("end")) {
                            for (int i = 0; i < BodyInput.size(); i++) {
                                body += BodyInput.get(i).replace(" ", "");
                                body += " ";
                            }
                            changeTextView(email_body, body);
                            reco.stopContinuousRecognitionAsync();
                            String speakText = "Email body recorded";
                            SpeechSynthesisResult result = synthesizer.SpeakText(speakText);
                            result.close();
                            BodyInput.clear();
                            RecordBody = false;
                            reco.startContinuousRecognitionAsync();
                        } else {
                            BodyInput.add(s);
                        }
                    } else if (RecordReceiver) {
                        if (comparedText.equals("stop") || comparedText.equals("end")) {
                            for (int i = 0; i < ReceiverInput.size(); i++) {
                                toEmail += ReceiverInput.get(i).replace(" ", "");
                                toEmail += " ";
                            }
                            changeTextView(email_to, toEmail);
                            reco.stopContinuousRecognitionAsync();
                            String speakText = "Receiver email address recorded";
                            SpeechSynthesisResult result = synthesizer.SpeakText(speakText);
                            result.close();
                            ReceiverInput.clear();
                            RecordReceiver = false;
                            reco.startContinuousRecognitionAsync();
                        } else {
                            if (!s.isEmpty() && s.charAt(s.length() - 1) == '.') {
                                s = s.substring(0, s.length() - 1);
                            }
                            ReceiverInput.add(s.toLowerCase());
                        }
                    } else if (RecordSubject) {
                        if (comparedText.equals("stop") || comparedText.equals("end")) {
                            for (int i = 0; i < SubjectInput.size(); i++) {
                                subject += SubjectInput.get(i).replace(" ", "");
                                subject += " ";
                            }
                            changeTextView(email_subject, subject);
                            reco.stopContinuousRecognitionAsync();
                            String speakText = "Email subject recorded";
                            SpeechSynthesisResult result = synthesizer.SpeakText(speakText);
                            result.close();
                            SubjectInput.clear();
                            RecordSubject = false;
                            reco.startContinuousRecognitionAsync();
                        } else {
                            SubjectInput.add(s.toLowerCase());
                        }
                    }

                    if (comparedText.equals("send") || comparedText.equals("deliver") || comparedText.equals("sent")) {
                        reco.stopContinuousRecognitionAsync();
                        if (send_button.callOnClick()) {
                            String successText = "Email sent successfully! ";
                            synthesizer.SpeakText(successText);
                            synthesizer.close();
                            speechConfig.close();
                            microphoneStream.close();
                            if (!speechSynthesisResult.isCancelled())
                                speechSynthesisResult.cancel(true);
                            Intent intent = new Intent(this, MainActivity.class);
                            startActivity(intent); // change later
                        } else {
                            String errorText = "Send email request failed! ";
                            synthesizer.SpeakText(errorText);
                            reco.startContinuousRecognitionAsync();
                        }
                    }
                }
                content.add(s);
            });
            final Future<Void> task = reco.startContinuousRecognitionAsync();
        } catch (Exception ex) {
            System.out.println(ex.getMessage());
        }
    }

    public boolean onClickMail(View view) {
        fromEmail = email_from.getText().toString();
        toEmail = email_to.getText().toString();
        subject = email_subject.getText().toString();
        body = email_body.getText().toString();
        user = fromEmail; //change later. // trymyappfortest@gmail.com pw. A987654321
        Boolean result = false;
        try {
            result = new SendEmailAsyncTask().execute().get();
            if (result) {
                Toast.makeText(this, "Mail Sent.", Toast.LENGTH_SHORT).show();
                return true;
            } else {
                Toast.makeText(this, "Sending Failed.", Toast.LENGTH_SHORT).show();
                return false;
            }
        } catch (Exception e) {
            Toast.makeText(this, "Sending Failed. Exception.", Toast.LENGTH_SHORT).show();
            return false;
        }
    }

    private void changeTextView(TextView textView, String text) {
        SendMailActivity.this.runOnUiThread(() -> {
            textView.setText(text);
        });
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

    class SendEmailAsyncTask extends AsyncTask<Void, Void, Boolean> {
        MailSender sender = new MailSender(user, password);
        MimeMessage message;

        public SendEmailAsyncTask() {
            if (BuildConfig.DEBUG)
                Log.v(SendEmailAsyncTask.class.getName(), "SendEmailAsyncTask()");
            try {
                message = sender.createMail(subject,
                        body,
                        fromEmail,
                        toEmail);
            } catch (Exception e) {
                Log.e("SendMailActivity", e.getMessage(), e);
            }
        }

        @Override
        protected Boolean doInBackground(Void... params) {
            if (BuildConfig.DEBUG) Log.v(SendEmailAsyncTask.class.getName(), "doInBackground()");
            try {
                sender.sendMail(message);
                return true;
            } catch (AuthenticationFailedException e) {
                Log.e(SendEmailAsyncTask.class.getName(), "Bad account details");
                e.printStackTrace();
                return false;
            } catch (MessagingException e) {
                Log.e(SendEmailAsyncTask.class.getName(), "failed");
                e.printStackTrace();
                return false;
            } catch (Exception e) {
                e.printStackTrace();
                return false;
            }
        }
    }
}
