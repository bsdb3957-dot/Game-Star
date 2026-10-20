package com.dms.container.app;

import android.app.Application;
import android.content.Context;

import net_62v.external.MetaActivationManager;
import java.io.IOException;
import android.util.Base64;
import javax.crypto.Cipher;
import javax.crypto.spec.SecretKeySpec;

public class BoxApplication extends Application {
    
public static String decryptString(String encryptedString, String key) throws Exception {
        byte[] keyBytes = Base64.decode(key, Base64.DEFAULT);
        SecretKeySpec secretKeySpec = new SecretKeySpec(keyBytes, "AES");
        Cipher cipher = Cipher.getInstance("AES");
        cipher.init(Cipher.DECRYPT_MODE, secretKeySpec);
        byte[] encryptedBytes = Base64.decode(encryptedString, Base64.DEFAULT);
        byte[] decryptedBytes = cipher.doFinal(encryptedBytes);
        return new String(decryptedBytes);
    }
    @Override
    protected void attachBaseContext(Context base) {
        super.attachBaseContext(base);
      //  FCore.get().init(this, true);
    }

   @Override
    public void onCreate() {
        super.onCreate();
       // gApp = this;
        //FirebaseApp.initializeApp(this);
try {
          //  MetaActivationManager.activateSdk(BoxApp());
        String encryptedString = "aWW/=";
        String key = "+Gw==";
        String decryptedKey = decryptString(encryptedString, key);
        MetaActivationManager.activateSdk(decryptedKey);
        } catch (Exception exception) {
            exception.printStackTrace();
        }
        
    }
}
