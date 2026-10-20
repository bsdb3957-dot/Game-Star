package com.dms.container;

import android.os.Bundle;
import android.provider.DocumentsContract;
import androidx.annotation.NonNull;
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
import android.view.View.*;
import android.view.animation.*;
import android.webkit.*;
import android.widget.*;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.documentfile.provider.DocumentFile;
import java.io.*;
import java.nio.channels.FileChannel;
import java.text.*;
import java.util.*;
import java.util.regex.*;
import me.ibrahimsn.particle.ParticleView;
import org.json.*;
import android.provider.DocumentsContract;
import androidx.documentfile.provider.DocumentFile;
import androidx.core.content.FileProvider;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import org.w3c.dom.Document;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import android.os.AsyncTask;
import android.os.Environment;
import android.widget.ProgressBar;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.channels.FileChannel;
import net_62v.external.MetaApplicationInstaller;
import net_62v.external.MetaActivityManager;
import net_62v.external.MetaPackageManager;
import net_62v.external.MetaStorageManager;

import com.dms.container.app.FileCopyTask;

public class HomeActivity extends AppCompatActivity {
    static {
        System.loadLibrary("dms");
    }
 private LinearLayout linear1;
	private TextView textview1;
	private LinearLayout linear_bg;
	private TextView textview_ver;
	private TextView textview_dver;
	private LinearLayout linear_gamelist;
	private ScrollView vscroll1;
	private LinearLayout linear4;
	private LinearLayout bg_gl;
	private TextView textview5;
	private LinearLayout linear6;
	private LinearLayout linear7;
	private ImageView imageview1;
	private LinearLayout linear8;
	private TextView textview_pkg;
	private LinearLayout linear9;
	private TextView textview_status;
	private TextView textview_gver;
	private LinearLayout btn_install;
	private LinearLayout btn_uninstall;
	private TextView textview9;
	private TextView textview10;
    private TextView textview11;
    private LinearLayout linear11;
	private LinearLayout linear_twitter;
	private LinearLayout linear_fb;
	private ImageView imageview5;
	private ImageView imageview6;
	private LinearLayout linear_home;
	private LinearLayout linear_set;
	private LinearLayout linear_updaye;
	private ImageView imageview2;
	private ImageView imageview3;
	private ImageView imageview4;
	private TextView t1;
	private TextView t2;
private LinearLayout linear_games;
	private LinearLayout linear_settings;
    ProgressDialog prog;
    private SharedPreferences game;
    private LinearLayout linear_injector;
	private ImageView imageview7;
	private TextView t3;
	String obbpath,vpath;
    Button unistall_all;
	int EXECUTE_CODE = 0;
private Timer _timer = new Timer();
	private TimerTask t;
    int OBB_CODE = 101;
int Obb_Permission = 112;
    int Storage_Permission = 142;
	//FCore fcore;
    String[] gamePackage = {"com.pubg.imobile","com.katana.facebook","com.twitter.android","com.dms.loader"};
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_home);
        game = getSharedPreferences("save", Activity.MODE_PRIVATE);
        //fcore = FCore.get();
        ids();
        drawabls();
            
      /*  try{
        File sourfeFile = new File ("/storage/emulated/0/Android/obb/com.pubg.imobile/main.17925.com.pubg.imobile.obb");
        File outout = new File("/storage/emulated/0/vSdcard/Android/obb/com.pubg.imobile/main.17925.com.pubg.imobile.obb");
          //  copyFile(sourfeFile,outout);
        Files.copy(sourfeFile.toPath(),outout.toPath(),StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException i){
        
            showMessage(i.getMessage());
        }*/
    
   
        linear_games.setVisibility(View.VISIBLE);
            linear_settings.setVisibility(View.GONE);
            prog = new ProgressDialog(this);
            prog.setTitle("Alert");
            prog.setCancelable(false);
            prog.setCanceledOnTouchOutside(false);
         if(isGameInstalled(gamePackage[0])){
             btn_install.setVisibility(View.VISIBLE);
            textview9.setText("Launch");
            btn_uninstall.setVisibility(View.VISIBLE);
            EXECUTE_CODE = 1;
         } else {
             textview9.setText("Install");
            btn_install.setVisibility(View.VISIBLE);
            btn_uninstall.setVisibility(View.GONE);
            EXECUTE_CODE = 0;
            openobb("com.pubg.imobile");
         }
           if(isGameInstalled(gamePackage[2])){
               t1.setText("Installed");
        t1.setBackground(new GradientDrawable() { public GradientDrawable getIns(int a, int b) { this.setCornerRadius(a); this.setColor(b); return this; } }.getIns((int)20, 0xFF4CAF50));
  
           }  else {
               t1.setVisibility(View.VISIBLE);
           } 
                if(isGameInstalled(gamePackage[1])){
              t2.setText("Installed");
        t2.setBackground(new GradientDrawable() { public GradientDrawable getIns(int a, int b) { this.setCornerRadius(a); this.setColor(b); return this; } }.getIns((int)20, 0xFF4CAF50));
  
           }  else {
               t2.setVisibility(View.VISIBLE);
           }         
                        
                        if(isGameInstalled(gamePackage[3])){
              t3.setText("Installed");
        t3.setBackground(new GradientDrawable() { public GradientDrawable getIns(int a, int b) { this.setCornerRadius(a); this.setColor(b); return this; } }.getIns((int)20, 0xFF4CAF50));
  
           }  else {
               t3.setVisibility(View.VISIBLE);
           }         
                        
    btn_install.setOnClickListener(new View.OnClickListener(){
        public void onClick(View v){
                            if(EXECUTE_CODE == 0){
                   /*     if(copyObb(HomeActivity.this,game.getString("obburi",""),"","/vSdcard/Android/obb/com.pubg.imobile/")){
                            showMessage("done");
                        } else {
                            showMessage("error");
                        }*/
                        FileCopyTask fctask = new FileCopyTask(HomeActivity.this);
                           fctask.copyObbAsync(game.getString("obburi",""),"main.18325.com.pubg.imobile.obb","/vSdcard/Android/obb/com.pubg.imobile/");
                       /*      try{
                                 
                          
                        obbpath =  getobbPath(gamePackage[0], getPackageManager().getPackageInfo(gamePackage[0],0).versionCode);
                        vpath = getVObb(gamePackage[0],getPackageManager().getPackageInfo(gamePackage[0],0).versionCode);
                        } catch (PackageManager.NameNotFoundException e){
                             
                         }
                        File obbv = new File(vpath);
                         File eobb  = new File(obbpath);   
                        if(obbv.exists()){
                            // install game
                        } else {
                            if(eobb.exists()){
                                FileCopyTask fileCopyTask = new FileCopyTask(HomeActivity.this, obbpath, vpath,gamePackage[0]);
                                fileCopyTask.execute();
                        
                            } else {
                                showMessage("OBB Not found!");
                            }
                        } */
                        
                            /*   InstallResult installResult = fcore.installPackageAsUser(gamePackage[0],0);
                          //  showMessage("Installing..");
                 if(installResult.getSuccess()){
                     textview9.setText("Launch");
                            EXECUTE_CODE =1;
                            prog.dismiss();
                             //   showMessage("Success!");
                                btn_uninstall.setVisibility(View.VISIBLE);
                        
                 } else {
                   textview9.setText(installResult.getMsg());  
                        
                 }*/
                            
                            } else if(EXECUTE_CODE == 1){
                            
      
                                  //  fcore.get().launchApk(gamePackage[0],0);
                                    MetaActivityManager.launchApp(gamePackage[0]);
                                
                                    }
            
                
            
        }
    });
                        linear_twitter.setOnClickListener(new View.OnClickListener(){
                            public void onClick(View v){
                
                                if(isGameInstalled(gamePackage[2])){
                              //  fcore.get().launchApk(gamePackage[2],0);
                                MetaActivityManager.launchApp(gamePackage[2]);
                            
                                } else {
                                    if(installGame(gamePackage[2])){
                                      //  showMessage("Installed");
                    checkOthers();
                                 t1.setVisibility(View.GONE);
                                    } else {
                                        showMessage("Error");
                                    }
                                }
                            }
                        });
                            linear_fb.setOnClickListener(new View.OnClickListener(){
                            public void onClick(View v){
                                if(isGameInstalled(gamePackage[1])){
                                               //    fcore.get().launchApk(gamePackage[1],0);
                                MetaActivityManager.launchApp(gamePackage[1]);
                            
                                } else {
                                    if(installGame(gamePackage[1])){
                                       // showMessage("Installed");
                    checkOthers();
                                 t1.setVisibility(View.GONE);
                                    } else {
                                        showMessage("Error");
                                    }
                                }
                            }
                        });
                            linear_injector.setOnClickListener(new View.OnClickListener(){
                            public void onClick(View v){
                                if(isGameInstalled(gamePackage[3])){
                                 //   showMessage("Launching..");
                     // fcore.get().launchApk(gamePackage[3],0);
                                MetaActivityManager.launchApp(gamePackage[3]);
                            
                        
                            } else {
                                    if(installGame(gamePackage[3])){
                         checkOthers();
                                       // showMessage("Installed");
                                 t1.setVisibility(View.GONE);
                                    } else {
                                        showMessage("Error");
                                    }
                                }
                            }
                        });
    btn_uninstall.setOnClickListener(new View.OnClickListener(){
        public void onClick(View v){
            if(EXECUTE_CODE == 1){
                timeprog("Uninstalling...");
              //  fcore.get().uninstallPackageAsUser(gamePackage[0],0);
                        MetaPackageManager.uninstallAppFully(gamePackage[0]);
        btn_uninstall.setVisibility(View.GONE);
         EXECUTE_CODE = 0;
        textview9.setText("Install");
            }
        }
    });
        unistall_all.setOnClickListener(new View.OnClickListener(){
            public void onClick(View v){
                if(isGameInstalled(gamePackage[3])){
                    timeprog("Uninstalling...");
               // fcore.get().uninstallPackageAsUser(gamePackage[3],0);
                        MetaPackageManager.uninstallAppFully(gamePackage[3]);
    checkOthers();
  
                }
            }
        });
        }
            boolean isRunning(String ipackage){
              //  return fcore.get().isRunning(ipackage,0);
        return MetaActivityManager.isAppRunning(ipackage, 0);
            }
    /*
    boolean installGame(String ipackage){
        InstallResult installresult = MetaApplicationInstaller.cloneApp(ipackage);
         if(installresult.getSuccess()){
             return true;
         } else {
             return false;
         }
        
    }
    
    public boolean isInstalled(String packageName) {
        try {
            return MetaPackageManager.isInnerAppInstalled(packageName);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
    */
       //  if (!ApkEnv.getInstance().isInstalled(packageName)) {
       boolean installGame(String ipackage){
                boolean installResult = MetaPackageManager.isInnerAppInstalled(ipackage);
                if (!installResult) {
                    return true;
         } else {
             return false;
                }
            }
    
    
   /* 
    public boolean installGame(String packageName) {
        ApplicationInfo applicationInfo = getApplicationInfo(packageName);
        if (applicationInfo == null) {
            return false;
        }
        
    	try {
            MetaApplicationInstaller.installAppByPath(applicationInfo.sourceDir);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        return true;
    }
    */
    void checkOthers (){
        if(isGameInstalled(gamePackage[2])){ // twitter
            t1.setText("Installed");
        t1.setBackground(new GradientDrawable() { public GradientDrawable getIns(int a, int b) { this.setCornerRadius(a); this.setColor(b); return this; } }.getIns((int)20, 0xFF4CAF50));
        } else {
           t1.setBackground(new GradientDrawable() { public GradientDrawable getIns(int a, int b) { this.setCornerRadius(a); this.setColor(b); return this; } }.getIns((int)20, 0xFFF44336));
            t1.setText("Not installed");
        }
     if(isGameInstalled(gamePackage[1])){ // fb
            t2.setText("Installed");
        t2.setBackground(new GradientDrawable() { public GradientDrawable getIns(int a, int b) { this.setCornerRadius(a); this.setColor(b); return this; } }.getIns((int)20, 0xFF4CAF50));
        } else {
           t2.setBackground(new GradientDrawable() { public GradientDrawable getIns(int a, int b) { this.setCornerRadius(a); this.setColor(b); return this; } }.getIns((int)20, 0xFFF44336));
            t2.setText("Not installed");
        }
    if(isGameInstalled(gamePackage[3])){ // fb
            t3.setText("Installed");
        t3.setBackground(new GradientDrawable() { public GradientDrawable getIns(int a, int b) { this.setCornerRadius(a); this.setColor(b); return this; } }.getIns((int)20, 0xFF4CAF50));
        } else {
           t3.setBackground(new GradientDrawable() { public GradientDrawable getIns(int a, int b) { this.setCornerRadius(a); this.setColor(b); return this; } }.getIns((int)20, 0xFFF44336));
            t3.setText("Not installed");
        }
    }
        boolean isGameInstalled(String ipackage){
        //    return fcore.isInstalled(ipackage,0);
        return MetaPackageManager.isInnerAppInstalled(ipackage);
        }
        void showMessage(String s){
            Toast.makeText(this,s,1).show();
        }
        
       public void openobb(String ipackage) {
    Intent i = new Intent();
    i.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION | Intent.FLAG_GRANT_WRITE_URI_PERMISSION);
    i.setAction(Intent.ACTION_OPEN_DOCUMENT_TREE);
    Uri muri = Uri.parse("content://com.android.externalstorage.documents/tree/primary%3AAndroid/document/primary%3AAndroid%2Fobb%2F"+ipackage);
    i.putExtra(DocumentsContract.EXTRA_INITIAL_URI, muri);
    startActivityForResult(i, OBB_CODE);  
    }
void ids(){
    linear1 = findViewById(R.id.linear1);
		textview1 = findViewById(R.id.textview1);
		linear_bg = findViewById(R.id.linear_bg);
		textview_ver = findViewById(R.id.textview_ver);
		textview_dver = findViewById(R.id.textview_dver);
		linear_gamelist = findViewById(R.id.linear_gamelist);
		vscroll1 = findViewById(R.id.vscroll1);
		linear4 = findViewById(R.id.linear4);
		bg_gl = findViewById(R.id.bg_gl);
		textview5 = findViewById(R.id.textview5);
		linear6 = findViewById(R.id.linear6);
		linear7 = findViewById(R.id.linear7);
		imageview1 = findViewById(R.id.imageview1);
		linear8 = findViewById(R.id.linear8);
		textview_pkg = findViewById(R.id.textview_pkg);
		linear9 = findViewById(R.id.linear9);
		textview_status = findViewById(R.id.textview_status);
		textview_gver = findViewById(R.id.textview_gver);
		btn_install = findViewById(R.id.btn_install);
		btn_uninstall = findViewById(R.id.btn_uninstall);
		textview9 = findViewById(R.id.textview9);
		textview10 = findViewById(R.id.textview10);
	  linear11 = findViewById(R.id.linear11);
         textview11 = findViewById(R.id.textview11);
		linear_twitter = findViewById(R.id.linear_twitter);
		linear_fb = findViewById(R.id.linear_fb);
		imageview5 = findViewById(R.id.imageview5);
		imageview6 = findViewById(R.id.imageview6);
		linear_home = findViewById(R.id.linear_home);
		linear_set = findViewById(R.id.linear_set);
		linear_updaye = findViewById(R.id.linear_updaye);
		imageview2 = findViewById(R.id.imageview2);
		imageview3 = findViewById(R.id.imageview3);
		imageview4 = findViewById(R.id.imageview4);
		t1 = findViewById(R.id.t1);
		t2 = findViewById(R.id.t2);
       linear_games = findViewById(R.id.linear_games);
                linear_settings = findViewById(R.id.linear_settings);
                linear_injector = findViewById(R.id.linear_injector);
		imageview7 = findViewById(R.id.imageview7);
		t3 = findViewById(R.id.t3);
            unistall_all = findViewById(R.id.btn_all);
} 
void drawabls(){
    linear_bg.setBackground(new GradientDrawable() { public GradientDrawable getIns(int a, int b) { this.setCornerRadius(a); this.setColor(b); return this; } }.getIns((int)10, 0xFF202020));
		textview_ver.setBackground(new GradientDrawable() { public GradientDrawable getIns(int a, int b) { this.setCornerRadius(a); this.setColor(b); return this; } }.getIns((int)15, 0xFF303030));
		textview_dver.setBackground(new GradientDrawable() { public GradientDrawable getIns(int a, int b) { this.setCornerRadius(a); this.setColor(b); return this; } }.getIns((int)15, 0xFF303030));
		bg_gl.setBackground(new GradientDrawable() { public GradientDrawable getIns(int a, int b) { this.setCornerRadius(a); this.setColor(b); return this; } }.getIns((int)10, 0xFF202020));
		textview_pkg.setBackground(new GradientDrawable() { public GradientDrawable getIns(int a, int b) { this.setCornerRadius(a); this.setColor(b); return this; } }.getIns((int)10, 0xFF303030));
		textview_status.setBackground(new GradientDrawable() { public GradientDrawable getIns(int a, int b) { this.setCornerRadius(a); this.setColor(b); return this; } }.getIns((int)10, 0xFF303030));
		textview_gver.setBackground(new GradientDrawable() { public GradientDrawable getIns(int a, int b) { this.setCornerRadius(a); this.setColor(b); return this; } }.getIns((int)10, 0xFF303030));
		textview1.setTypeface(Typeface.createFromAsset(getAssets(),"fonts/dms.ttf"), 0);
		textview_ver.setTypeface(Typeface.createFromAsset(getAssets(),"fonts/user.ttf"), 0);
		textview_dver.setTypeface(Typeface.createFromAsset(getAssets(),"fonts/user.ttf"), 0);
		textview5.setTypeface(Typeface.createFromAsset(getAssets(),"fonts/user.ttf"), 0);
		textview_pkg.setTypeface(Typeface.createFromAsset(getAssets(),"fonts/user.ttf"), 0);
		textview_status.setTypeface(Typeface.createFromAsset(getAssets(),"fonts/user.ttf"), 0);
		textview_gver.setTypeface(Typeface.createFromAsset(getAssets(),"fonts/user.ttf"), 0);
btn_install.setBackground(new GradientDrawable() { public GradientDrawable getIns(int a, int b) { this.setCornerRadius(a); this.setColor(b); return this; } }.getIns((int)15, 0xFF4CAF50));
btn_uninstall.setBackground(new GradientDrawable() { public GradientDrawable getIns(int a, int b) { this.setCornerRadius(a); this.setColor(b); return this; } }.getIns((int)15, 0xFF303030));
textview9.setTypeface(Typeface.createFromAsset(getAssets(),"fonts/user.ttf"), 0);
textview10.setTypeface(Typeface.createFromAsset(getAssets(),"fonts/user.ttf"), 0);
unistall_all.setBackground(new GradientDrawable() { public GradientDrawable getIns(int a, int b) { this.setCornerRadius(a); this.setColor(b); return this; } }.getIns((int)15, 0xFF4CAF50));

        t1.setTypeface(Typeface.createFromAsset(getAssets(),"fonts/user.ttf"), 0);
		t2.setTypeface(Typeface.createFromAsset(getAssets(),"fonts/user.ttf"), 0);
		unistall_all.setTypeface(Typeface.createFromAsset(getAssets(),"fonts/user.ttf"), 0);
	
    	linear_twitter.setBackground(new GradientDrawable() { public GradientDrawable getIns(int a, int b) { this.setCornerRadius(a); this.setColor(b); return this; } }.getIns((int)20, 0xFF303030));
		linear_fb.setBackground(new GradientDrawable() { public GradientDrawable getIns(int a, int b) { this.setCornerRadius(a); this.setColor(b); return this; } }.getIns((int)20, 0xFF303030));
		linear11.setBackground(new GradientDrawable() { public GradientDrawable getIns(int a, int b) { this.setCornerRadius(a); this.setColor(b); return this; } }.getIns((int)20, 0xFF303030));
		linear_bg.setElevation((float)5);
		linear11.setElevation((float)5);
		imageview2.setColorFilter(0xFF4CAF50, PorterDuff.Mode.MULTIPLY);
		imageview3.setColorFilter(0xFFFFFFFF, PorterDuff.Mode.MULTIPLY);
		imageview4.setColorFilter(0xFFFFFFFF, PorterDuff.Mode.MULTIPLY);
	      linear_home.setOnClickListener(new View.OnClickListener(){
              public void onClick(View  h){
                  linear_games.setVisibility(0);
                  linear_settings.setVisibility(8);
               imageview2.setColorFilter(0xFF4CAF50, PorterDuff.Mode.MULTIPLY);
		imageview3.setColorFilter(0xFFFFFFFF, PorterDuff.Mode.MULTIPLY);
		imageview4.setColorFilter(0xFFFFFFFF, PorterDuff.Mode.MULTIPLY);
	    
              }
          });
        linear_set.setOnClickListener(new View.OnClickListener(){
              public void onClick(View  h){
                  linear_games.setVisibility(8);
                  linear_settings.setVisibility(0);
               imageview2.setColorFilter(0xFFFFFFFF, PorterDuff.Mode.MULTIPLY);
		imageview3.setColorFilter(0xFF4CAF50, PorterDuff.Mode.MULTIPLY);
		imageview4.setColorFilter(0xFFFFFFFF, PorterDuff.Mode.MULTIPLY);
	    
              }
          });
        t1.setTypeface(Typeface.createFromAsset(getAssets(),"fonts/user.ttf"), 0);
t2.setTypeface(Typeface.createFromAsset(getAssets(),"fonts/user.ttf"), 0);
t3.setTypeface(Typeface.createFromAsset(getAssets(),"fonts/user.ttf"), 0);
linear_twitter.setBackground(new GradientDrawable() { public GradientDrawable getIns(int a, int b) { this.setCornerRadius(a); this.setColor(b); return this; } }.getIns((int)20, 0xFF303030));
linear_fb.setBackground(new GradientDrawable() { public GradientDrawable getIns(int a, int b) { this.setCornerRadius(a); this.setColor(b); return this; } }.getIns((int)20, 0xFF303030));
linear_injector.setBackground(new GradientDrawable() { public GradientDrawable getIns(int a, int b) { this.setCornerRadius(a); this.setColor(b); return this; } }.getIns((int)20, 0xFF303030));
t3.setBackground(new GradientDrawable() { public GradientDrawable getIns(int a, int b) { this.setCornerRadius(a); this.setColor(b); return this; } }.getIns((int)20, 0xFFF44336));
t2.setBackground(new GradientDrawable() { public GradientDrawable getIns(int a, int b) { this.setCornerRadius(a); this.setColor(b); return this; } }.getIns((int)20, 0xFFF44336));
t1.setBackground(new GradientDrawable() { public GradientDrawable getIns(int a, int b) { this.setCornerRadius(a); this.setColor(b); return this; } }.getIns((int)20, 0xFFF44336));
        }
   void timeprog(String s){
       ProgressDialog bd = new ProgressDialog(this);
       bd.setTitle("Alert");
       bd.setMessage(s);
bd.setCanceledOnTouchOutside(false);
bd.setCancelable(false);
bd.show();
t = new TimerTask() {
			@Override
			public void run() {
				runOnUiThread(new Runnable() {
					@Override
					public void run() {
						bd.cancel();
					}
				});
			}
		};
		_timer.schedule(t, (int)(3000));
   }
    @Override
       public void onActivityResult(int requestCode, int resultCode, Intent resultData) {
    if (requestCode == OBB_CODE && resultCode == Activity.RESULT_OK) {
      if (resultData != null) {
                
      Uri treeUri = resultData.getData();
      Uri uri = resultData.getData();
           final int takeFlags = resultData.getFlags()
			            & (Intent.FLAG_GRANT_READ_URI_PERMISSION
			            | Intent.FLAG_GRANT_WRITE_URI_PERMISSION);
			// Check for the freshest data.
			getContentResolver().takePersistableUriPermission(uri, takeFlags);
                         Uri checkuri = uri;
        game.edit().putString("obburi",checkuri.toString()).commit();
                  EXECUTE_CODE = 0;
    }
            }
        }

    

 
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == this.Storage_Permission && !hasAllPermissionsGranted(grantResults)) {
            Toast.makeText(getApplicationContext(), "Unable To Get Storage Permission", Toast.LENGTH_SHORT).show();
        }
    }
/*
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode != this.Obb_Permission) {
            return;
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            if (!getPackageManager().canRequestPackageInstalls()) {
                Toast.makeText(getApplicationContext(), "Unable To Get Obb Permission - (Relaunch the app)!", Toast.LENGTH_SHORT).show();
            } else if (ContextCompat.checkSelfPermission(getApplicationContext(), "android.permission.WRITE_EXTERNAL_STORAGE") != 0) {
                ActivityCompat.requestPermissions(this, new String[]{"android.permission.WRITE_EXTERNAL_STORAGE", "android.permission.READ_EXTERNAL_STORAGE"}, this.Storage_Permission);
            }
        }
    }*/

    public boolean hasAllPermissionsGranted(int[] grantResults) {
        for (int grantResult : grantResults) {
            if (grantResult == -1) {
                return false;
            }
        }
        return true;
    }     
        
void perm(){
    Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
intent.addCategory(Intent.CATEGORY_OPENABLE);
intent.setType("*/*"); // Specify the type of files you want to access

// Optionally, set additional flags to request access to all files:
intent.putExtra(DocumentsContract.EXTRA_INITIAL_URI, Uri.parse("content://com.android.externalstorage.documents/tree/primary"));
//intent.putExtra(DocumentsContract.EXTRA_SHOW_ADVANCED, true);

startActivityForResult(intent, 1);

}

private void copyFile(File src, File dst) throws IOException {

    FileInputStream inStream = new FileInputStream(src);

    if (!dst.exists()) {
        dst.mkdir();
    }

    if (!dst.canWrite()) {
       // System.out.print("CAN'T WRITE");
        return;
    }

    FileOutputStream outStream = new FileOutputStream(dst);
    FileChannel inChannel = inStream.getChannel();
    FileChannel outChannel = outStream.getChannel();
    inChannel.transferTo(0, inChannel.size(), outChannel);
    inStream.close();
    outStream.close();
}
   

public String getobbPath(String pkg, int ver) {
        return "/storage/emulated/0/Android/obb/" + pkg + "/main." + ver + "." + pkg + ".obb";
    }
public String getVObb(String pkg, int ver) {
        return "/storage/emulated/0/vSdcard/Android/obb/" + pkg + "/main." + ver + "." + pkg + ".obb";
    }

}


