package com.dms.container;

import androidx.annotation.NonNull;
import android.os.Bundle;
import androidx.appcompat.app.AppCompatActivity;
import android.os.Bundle;
import androidx.appcompat.app.AppCompatActivity;
import android.animation.*;
import android.app.*;
import android.app.Activity;
import android.app.DialogFragment;
import android.app.Fragment;
import android.app.FragmentManager;
import android.content.*;
import android.content.res.*;
import android.graphics.*;
import android.graphics.Typeface;
import android.graphics.drawable.*;
import android.media.*;
import android.net.*;
import android.os.*;
import android.text.*;
import android.text.style.*;
import android.util.*;
import android.view.*;
import android.view.View;
import android.view.View.*;
import android.view.animation.*;
import android.webkit.*;
import android.widget.*;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import java.io.*;
import java.text.*;
import java.util.*;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.widget.EditText;
import java.util.regex.*;
import org.json.*;
public class LoginActivity extends AppCompatActivity {
    static {
        System.loadLibrary("dms");
    }
  private LinearLayout linear1;
	private LinearLayout linear2;
	private LinearLayout linear3;
	private LinearLayout linear4;
	private LinearLayout linear_key;
	private LinearLayout linear_ref;
	private Button button1;
	private TextView textview2;
	private LinearLayout linear7;
	private EditText edittext1;
	private LinearLayout linear_paste;
	private ImageView ic_paste;
	private TextView textview3;
	private LinearLayout linear8;
	private EditText edittext2;
	private TextView textview4;
     ProgressDialog progd;
    private SharedPreferences game;
   int Obb_Permission = 112;
    int Storage_Permission = 142;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
             setContentView(R.layout.activity_login);
         game = getSharedPreferences("save", Activity.MODE_PRIVATE);

        ids();
           drawabls();
               if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            if (!getPackageManager().canRequestPackageInstalls()) {
                startActivityForResult(new Intent("android.settings.MANAGE_UNKNOWN_APP_SOURCES"), this.Obb_Permission);
            } else if (ContextCompat.checkSelfPermission(this, "android.permission.WRITE_EXTERNAL_STORAGE") != 0) {
                ActivityCompat.requestPermissions(this, new String[]{"android.permission.WRITE_EXTERNAL_STORAGE", "android.permission.READ_EXTERNAL_STORAGE"}, this.Storage_Permission);
            }
        }
        progd = new ProgressDialog(this);
        progd.setTitle("Alert");
         ic_paste.setOnClickListener(new View.OnClickListener(){
             public void onClick(View v){
                 ClipboardManager clipboard = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);

// Check if there is data on the clipboard
if (clipboard.hasPrimaryClip()) {
    // Get the first item from the clipboard
    ClipData.Item item = clipboard.getPrimaryClip().getItemAt(0);

    // Get the text from the item
    String clipboardText = item.getText().toString();

    // Set the text to your EditText
    edittext1.setText(clipboardText);
}
             }
         });
        button1.setOnClickListener(new View.OnClickListener(){
            public void onClick(View v){
          //      startActivity(new Intent (getApplicationContext(),HomeActivity.class))
                    progd.setMessage("Logging...");
                    progd.show();
                    heck(getApplicationContext(),edittext1.getText().toString());
                    }
        });
        if(game.contains("key")){
            progd.setMessage("Logging...");
                    progd.show();
                    heck(getApplicationContext(),game.getString("key",""));
            
        }
        }
        public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == this.Storage_Permission && !hasAllPermissionsGranted(grantResults)) {
            Toast.makeText(this, "Unable To Get Storage Permission", Toast.LENGTH_SHORT).show();
        }
    }

    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode != this.Obb_Permission) {
            return;
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            if (!getPackageManager().canRequestPackageInstalls()) {
                Toast.makeText(this, "Unable To Get Obb Permission - (Relaunch the app)!", Toast.LENGTH_SHORT).show();
            } else if (ContextCompat.checkSelfPermission(this, "android.permission.WRITE_EXTERNAL_STORAGE") != 0) {
                ActivityCompat.requestPermissions(this, new String[]{"android.permission.WRITE_EXTERNAL_STORAGE", "android.permission.READ_EXTERNAL_STORAGE"}, this.Storage_Permission);
            }
        }
    }

    public boolean hasAllPermissionsGranted(int[] grantResults) {
        for (int grantResult : grantResults) {
            if (grantResult == -1) {
                return false;
            }
        }
        return true;
    }

    private void ids(){
        linear1 = findViewById(R.id.linear1);
		linear2 = findViewById(R.id.linear2);
		linear3 = findViewById(R.id.linear3);
		linear4 = findViewById(R.id.linear4);
		linear_key = findViewById(R.id.linear_key);
		linear_ref = findViewById(R.id.linear_ref);
		button1 = findViewById(R.id.button1);
		textview2 = findViewById(R.id.textview2);
		linear7 = findViewById(R.id.linear7);
		edittext1 = findViewById(R.id.edittext1);
		linear_paste = findViewById(R.id.linear_paste);
		ic_paste = findViewById(R.id.ic_paste);
		textview3 = findViewById(R.id.textview3);
		linear8 = findViewById(R.id.linear8);
		edittext2 = findViewById(R.id.edittext2);
		textview4 = findViewById(R.id.textview4);
		
    }
    private void drawabls(){
        linear_key.setBackground(new GradientDrawable() { public GradientDrawable getIns(int a, int b) { this.setCornerRadius(a); this.setColor(b); return this; } }.getIns((int)15, 0xFF202020));
		linear_ref.setBackground(new GradientDrawable() { public GradientDrawable getIns(int a, int b) { this.setCornerRadius(a); this.setColor(b); return this; } }.getIns((int)15, 0xFF202020));
		linear_paste.setBackground(new GradientDrawable() { public GradientDrawable getIns(int a, int b) { this.setCornerRadius(a); this.setColor(b); return this; } }.getIns((int)10, 0xFF303030));
		button1.setBackground(new GradientDrawable() { public GradientDrawable getIns(int a, int b) { this.setCornerRadius(a); this.setColor(b); return this; } }.getIns((int)10, 0xFF202020));
		textview2.setBackground(new GradientDrawable() { public GradientDrawable getIns(int a, int b) { this.setCornerRadius(a); this.setColor(b); return this; } }.getIns((int)20, 0xFF4CAF50));
		textview3.setBackground(new GradientDrawable() { public GradientDrawable getIns(int a, int b) { this.setCornerRadius(a); this.setColor(b); return this; } }.getIns((int)20, 0xFF4CAF50));
		textview4.setTypeface(Typeface.createFromAsset(getAssets(),"fonts/dms.ttf"), 0);
		textview2.setTypeface(Typeface.createFromAsset(getAssets(),"fonts/user.ttf"), 0);
		edittext1.setTypeface(Typeface.createFromAsset(getAssets(),"fonts/user.ttf"), 0);
		textview3.setTypeface(Typeface.createFromAsset(getAssets(),"fonts/user.ttf"), 0);
		edittext2.setTypeface(Typeface.createFromAsset(getAssets(),"fonts/user.ttf"), 0);
        button1.setTypeface(Typeface.createFromAsset(getAssets(),"fonts/dms.ttf"), 0);

    }
        private  void heck(final Context m_Context, final String userKey) {
        Toast.makeText(m_Context,"Checking Key...".toString(),1).show();

        final Handler loginHandler = new Handler() {
            @Override
            public void handleMessage(Message msg) {
                if (msg.what == 0) {
                  //  Toast.makeText(m_Context,"Server Connection Established".toString(),1).show();
                    Intent i = new Intent(m_Context.getApplicationContext(), HomeActivity.class);
                    game.edit().putString("key",userKey).apply();
                    progd.dismiss();
                    startActivity(i);
                } else if (msg.what == 1) {
                    progd.dismiss();
                    Toast.makeText(m_Context,"Invalid Key".toString(),1).show();
                }

            }
        };

        new Thread(new Runnable() {
            @Override
            public void run() {
                String result = Check(m_Context, userKey);
                if (result.equals("OK")) {
                    loginHandler.sendEmptyMessage(0);
                } else {
                    Message msg = new Message();
                    msg.what = 1;
                    msg.obj = result;
                    loginHandler.sendMessage(msg);
                }
            }
        }).start();
    }
    
        private static native String Check(Context mContext, String userKey);
}

