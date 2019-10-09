package com.example.mailapp;

import androidx.appcompat.app.AppCompatActivity;

import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.widget.TextView;

import javax.mail.Address;
import javax.mail.Message;
import javax.mail.Multipart;
import javax.mail.Part;

public class ReadSingleMailActivity extends AppCompatActivity {
    private int EmailId;
    private SharedPreferences sharedPreferences;
    private TextView MailBody;
    private TextView MailSubject;
    private TextView MailFrom;

    private String Subject;
    private Address[] From;
    private String Body;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_read_single_mail);
        sharedPreferences = getSharedPreferences("LoginInfo", 0);
        MailBody = findViewById(R.id.mailBody);
        MailSubject = findViewById(R.id.mailSubject);
        MailFrom = findViewById(R.id.mailFrom);

        Bundle extras = getIntent().getExtras();
        if (extras != null) {
            EmailId = extras.getInt("MAIL_LINE");
        }

        try {
            boolean result = new ReadSingleMailAsyncTask().execute().get();
            if (result) {
                SetEmail();
            }
        } catch (Exception ex) {

        }
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
            }
            else if (p.isMimeType("text/plain")) {
                Body = p.getContent().toString();
            }
        }
    }
}
