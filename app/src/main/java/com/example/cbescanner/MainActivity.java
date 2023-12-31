package com.example.cbescanner;

import android.content.Intent;
import android.os.Bundle;
import android.os.StrictMode;
import android.util.Log;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.zxing.integration.android.IntentIntegrator;
import com.google.zxing.integration.android.IntentResult;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.util.ArrayList;

public class MainActivity extends AppCompatActivity {

    Button btnScan;
    EditText txtResultado;
    EditText txtCantidad;
    EditText txtDescripcion;
    ImageButton btnSave;
    ImageButton btnCancel;
    ListView lvListaCR;
    Button btnSincronizar;
    ArrayList<String> lngList;
    ProgressBar pbSinc;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        btnScan         = findViewById(R.id.btnScan);
        txtResultado    = findViewById(R.id.txtResultado);
        txtCantidad     = findViewById(R.id.txtCantidad);
        txtDescripcion  = findViewById(R.id.txtDescripcion);
        btnSave         = findViewById(R.id.btnSave);
        btnCancel       = findViewById(R.id.btnCancel);
        lvListaCR       = findViewById(R.id.lvListaCR);
        btnSincronizar  = findViewById(R.id.btnSincronizar);
        pbSinc          = findViewById(R.id.pbSinc);
        lngList         = new ArrayList();

        //carga de ejemplo, borrar antes de liberar
        Log.d("llenamos la lista de prueba","Listo");
        for (int i = 50; i < 54; i++) {
            lngList.add("1234567890"+i+" - "+i+" - producto "+i);
        }
        // hasta aqui
        setpbVisibility(false);

        btnSincronizar.setEnabled(true);//cambiar a false antes de liberar


        ArrayAdapter<String> adapter = new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, lngList);

        lvListaCR.setAdapter(adapter);

        btnScan.setOnClickListener(new View.OnClickListener() {
           @Override
           public void onClick(View view) {
               IntentIntegrator integrador = new IntentIntegrator(MainActivity.this);
               integrador.setDesiredBarcodeFormats(IntentIntegrator.ALL_CODE_TYPES);
               integrador.setPrompt("Lector - CDP");
               integrador.setCameraId(0);
               integrador.setBeepEnabled(true);
               integrador.setBarcodeImageEnabled(true);
               integrador.initiateScan();
           }
        });

        btnSave.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                String cb   = txtResultado.getText().toString().trim();
                String desc = txtDescripcion.getText().toString().trim();
                String cant = txtCantidad.getText().toString().trim();

                if (!cb.isEmpty()) {
                    if (!desc.isEmpty()) {
                        if (!cant.isEmpty()) {
                            lngList.add(cb + " - " + cant + " - " + desc);
                            adapter.notifyDataSetChanged();
                            //vaciamos los EditText
                            txtResultado.setText("");
                            txtDescripcion.setText("");
                            txtCantidad.setText("");
                            btnSincronizar.setEnabled(true);
                        }
                    }
                }
            }
        });

        btnCancel.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                //vaciamos los EditText
                txtResultado.setText("");
                txtDescripcion.setText("");
                txtCantidad.setText("");
            }
        });

        btnSincronizar.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                //vamos a guardar la lista de articulos escaneados
                //primero nos conectamos a la bd
                setpbVisibility(true);
                Connection connection = conexionBD();
                try {
                    if (connection != null) {
                        //recorremos el adaptador
                        for(String ss:lngList) {
                            //Toast.makeText(getApplicationContext(), ss, Toast.LENGTH_LONG).show();
                            //System.out.println(ss);
                            //separo codigo barras, cantidad y descripcion
                            String cb = ss.split("-")[0];
                            String cant = ss.split("-")[1];
                            String desc = ss.split("-")[2];
                            PreparedStatement stm = conexionBD().prepareStatement("insert into producto (codigo, etiqueta, cantidad, fecha_cap, id_usuario) values ('"+cb+"', '"+desc+"', "+cant+", getdate(),1);");
                            stm.executeUpdate();
                            Log.i("Producto insertado: ", desc );
                        }
                        lngList.removeAll(lngList);
                        adapter.notifyDataSetChanged();
                        btnSincronizar.setEnabled(false);
                        connection.close();
                        Toast.makeText(getApplicationContext(), "Sincronización correcta.", Toast.LENGTH_LONG).show();
                    }
                } catch (Exception e) {
                    String tit = "Error al tratar de sincronizar ";
                    String msg = e.getMessage();
                    Log.e(tit, msg);
                    Log.d(tit, msg);
                    Toast.makeText(getApplicationContext(), tit + msg, Toast.LENGTH_LONG).show();
                }
                setpbVisibility(false);
            }
        });
    }

    public void setpbVisibility(boolean visibility) {
        pbSinc.setVisibility(visibility?View.VISIBLE:View.INVISIBLE);
    }

    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        IntentResult result = IntentIntegrator.parseActivityResult(requestCode, resultCode, data);
        if(result != null) {
            if(result.getContents() == null) {
                Toast.makeText(this,"Lectura Cancelada", Toast.LENGTH_LONG).show();
            } else {
                Toast.makeText(this,result.getContents(), Toast.LENGTH_LONG).show();
                txtResultado.setText(result.getContents());
            }
        } else {
            super.onActivityResult(requestCode, resultCode, data);
        }
    }

    public Connection conexionBD() {
        try{
            Connection cnn = null;
            StrictMode.ThreadPolicy politica = new StrictMode.ThreadPolicy.Builder().permitAll().build();
            StrictMode.setThreadPolicy(politica);

            Class.forName("net.sourceforge.jtds.jdbc.Driver").newInstance();
            //cnn = DriverManager.getConnection("jdbc:jtds:sqlserver://192.168.1.109;databaseName=inventario;username:sa;password=q1w2e3r4;");
            cnn = DriverManager.getConnection("jdbc:jtds:sqlserver://192.168.1.109/inventario;instance=MSSQLSERVER;user=sa;password=q1w2e3r4;");
            return cnn;
        } catch (Exception e) {
            String tit = "Error al establecer la conexion BD ";
            String msg = tit + e.getMessage();
            Log.e(tit,msg);
            Log.d(tit,msg);
            Toast.makeText(this,tit + msg, Toast.LENGTH_LONG).show();
            return null;
        }
    }
}

