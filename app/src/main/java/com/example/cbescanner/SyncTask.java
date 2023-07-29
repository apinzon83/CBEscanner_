package com.example.cbescanner;

import android.util.Log;
import android.widget.ProgressBar;
import android.widget.Toast;

import java.sql.Connection;
import java.sql.PreparedStatement;

// SyncTask.java
public class SyncTask implements Runnable {

    private MainActivity mainActivity;
    private ProgressBar pbSinc;

    public SyncTask(MainActivity activity) {
        this.mainActivity = activity;
    }

    @Override
    public void run() {
        mainActivity.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                mainActivity.setpbSincVisibility(true);
                mainActivity.changebtnSincState(false);
            }
        });

        //vamos a guardar la lista de articulos escaneados
        //primero nos conectamos a la bd
        Connection connection = mainActivity.conexionBD();
        try {
            if (connection != null) {
                //recorremos el adaptador
                for (String ss : mainActivity.getLngList()) {
                    //separo codigo barras, cantidad y descripcion
                    String cb = ss.split("-")[0];
                    String cant = ss.split("-")[1];
                    String desc = ss.split("-")[2];
                    PreparedStatement stm = connection.prepareStatement("insert into producto (codigo, etiqueta, cantidad, fecha_cap, id_usuario) values ('" + cb + "', '" + desc + "', " + cant + ", getdate(),1);");
                    stm.executeUpdate();
                    Log.i("Producto insertado: ", desc);
                }
                mainActivity.getLngList().removeAll(mainActivity.getLngList());

                // Updating UI elements should be done on the UI thread
                mainActivity.runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        mainActivity.getAdapter().notifyDataSetChanged();
                        mainActivity.getBtnSincronizar().setEnabled(false);
                        Toast.makeText(mainActivity.getApplicationContext(), "Sincronización correcta.", Toast.LENGTH_LONG).show();
                    }
                });

                connection.close();
            }
        } catch (Exception e) {
            final String tit = "Error al tratar de sincronizar ";
            final String msg = e.getMessage();
            Log.e(tit, msg);
            Log.d(tit, msg);
            mainActivity.runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    Toast.makeText(mainActivity.getApplicationContext(), tit + msg, Toast.LENGTH_LONG).show();
                }
            });
        }

        mainActivity.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                mainActivity.setpbSincVisibility(false);
                mainActivity.changebtnSincState(true);
            }
        });
    }
}
