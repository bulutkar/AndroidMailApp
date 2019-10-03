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

import javax.mail.AuthenticationFailedException;
import javax.mail.MessagingException;
import javax.mail.internet.MimeMessage;

public class SendMailActivity extends AppCompatActivity {

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
    }

    public void onClickMail(View view) {
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
                Intent intent = new Intent(this, MainActivity.class);
                startActivity(intent); // change later
            } else {
                Toast.makeText(this, "Sending Failed.", Toast.LENGTH_SHORT).show();
            }
        } catch (Exception e) {
            Toast.makeText(this, "Sending Failed. Exception.", Toast.LENGTH_SHORT).show();
        }
    }

    private void changeTextView(TextView textView, String text) {
        SendMailActivity.this.runOnUiThread(() -> {
            textView.setText(text);
        });
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
