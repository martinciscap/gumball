package com.example.nfcplugin;

import android.app.Activity;
import android.nfc.NfcAdapter;
import android.nfc.Tag;
import android.nfc.tech.Ndef;
import android.nfc.NdefMessage;
import android.os.Bundle;
import android.util.Log;

import org.apache.cordova.CallbackContext;
import org.apache.cordova.CordovaPlugin;
import org.apache.cordova.PluginResult;
import org.json.JSONObject;
import org.json.JSONException;
import org.json.JSONArray;

public class MyNfcPlugin extends CordovaPlugin implements NfcAdapter.ReaderCallback {
    private static final String TAG = "MyNfcPlugin";
    private NfcAdapter nfcAdapter;
    private CallbackContext readerCallback;

    @Override
    public boolean execute(String action, JSONArray args, CallbackContext callbackContext) throws JSONException {
        if ("start".equals(action)) {
            startReaderMode(callbackContext);
            return true;
        } else if ("stop".equals(action)) {
            stopReaderMode(callbackContext);
            return true;
        }
        return false;
    }
    
    /**
     * Activa el modo lector NFC.
     */
    private void startReaderMode(CallbackContext callbackContext) {
        readerCallback = callbackContext;
        final Activity activity = cordova.getActivity();
        nfcAdapter = NfcAdapter.getDefaultAdapter(activity);
        if (nfcAdapter == null) {
            callbackContext.error("El dispositivo no soporta NFC");
            return;
        }
        activity.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                // Se usan banderas para detectar distintas tecnologías (NFC-A, NFC-B, etc.)
                nfcAdapter.enableReaderMode(activity, MyNfcPlugin.this,
                        NfcAdapter.FLAG_READER_NFC_A | NfcAdapter.FLAG_READER_NFC_B |
                        NfcAdapter.FLAG_READER_NFC_F | NfcAdapter.FLAG_READER_NFC_V,
                        null);
            }
        });
        // Se informa al JS que el modo lector está activo.
        callbackContext.success("Modo lector iniciado");
    }
    
    /**
     * Desactiva el modo lector NFC.
     */
    private void stopReaderMode(CallbackContext callbackContext) {
        final Activity activity = cordova.getActivity();
        if (nfcAdapter != null) {
            activity.runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    nfcAdapter.disableReaderMode(activity);
                }
            });
        }
        callbackContext.success("Modo lector detenido");
    }

    /**
     * Este método se invoca cuando se detecta una etiqueta NFC.
     */
    @Override
    public void onTagDiscovered(Tag tag) {
        Log.d(TAG, "Etiqueta descubierta: " + tag.toString());
        JSONObject json = new JSONObject();
        try {
            // Se intenta obtener un objeto Ndef (si la etiqueta es NDEF)
            Ndef ndef = Ndef.get(tag);
            if (ndef != null) {
                ndef.connect();
                NdefMessage ndefMessage = ndef.getNdefMessage();
                ndef.close();
                if (ndefMessage != null) {
                    // Convertimos el mensaje NDEF a hexadecimal (como ejemplo)
                    json.put("ndefMessage", bytesToHex(ndefMessage.toByteArray()));
                } else {
                    json.put("message", "Etiqueta NDEF vacía");
                }
            } else {
                // Si la etiqueta no soporta NDEF, se devuelve al menos su ID
                json.put("id", bytesToHex(tag.getId()));
            }
        } catch (Exception e) {
            try {
                json.put("error", e.getMessage());
            } catch (JSONException jsonException) {}
        }
        PluginResult result = new PluginResult(PluginResult.Status.OK, json);
        // Para que se puedan enviar múltiples lecturas, se mantiene el callback.
        result.setKeepCallback(true);
        if (readerCallback != null) {
            readerCallback.sendPluginResult(result);
        }
    }

    /**
     * Método auxiliar para convertir un arreglo de bytes a una cadena hexadecimal.
     */
    private String bytesToHex(byte[] bytes) {
        final char[] hexArray = "0123456789ABCDEF".toCharArray();
        char[] hexChars = new char[bytes.length * 2];
        for (int j = 0; j < bytes.length; j++) {
            int v = bytes[j] & 0xFF;
            hexChars[j * 2] = hexArray[v >>> 4];
            hexChars[j * 2 + 1] = hexArray[v & 0x0F];
        }
        return new String(hexChars);
    }
}