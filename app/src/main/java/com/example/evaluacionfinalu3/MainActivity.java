package com.example.evaluacionfinalu3;

import androidx.appcompat.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import com.google.firebase.firestore.FirebaseFirestore;

import org.eclipse.paho.client.mqttv3.MqttClient;
import org.eclipse.paho.client.mqttv3.MqttConnectOptions;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.eclipse.paho.client.mqttv3.MqttMessage;

import java.util.HashMap;
import java.util.Map;

public class MainActivity extends AppCompatActivity {

    // UI components
    EditText etTitulo, etDescripcion, etFecha, etHora, etRecordatorio;
    Button btnGuardar, btnMostrar, btnEnviarMQTT;
    TextView tvListaRecordatorios;

    // Firebase and MQTT
    FirebaseFirestore db;
    MqttClient mqttClient;

    // MQTT credentials and settings
    private static final String MQTT_BROKER = "tcp://glassscale371.cloud.shiftr.io:1883";
    private static final String USERNAME = "glassscale371";
    private static final String PASSWORD = "JDoWGlPyJhMwc8Fy";
    private static final String CLIENT_ID = "APP";
    private static final String TOPICO = "Mensaje";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Initialize UI components
        etTitulo = findViewById(R.id.etTitulo);
        etDescripcion = findViewById(R.id.etDescripcion);
        etFecha = findViewById(R.id.etFecha);
        etHora = findViewById(R.id.etHora);
        btnGuardar = findViewById(R.id.btnGuardar);
        btnMostrar = findViewById(R.id.btnMostrar);
        btnEnviarMQTT = findViewById(R.id.btnEnviarMQTT);
        tvListaRecordatorios = findViewById(R.id.tvListaRecordatorios);

        // Initialize Firebase
        db = FirebaseFirestore.getInstance();

        // Connect to MQTT
        conectarMQTT();

        // Set up button listeners
        btnGuardar.setOnClickListener(v -> guardarRecordatorio());
        btnMostrar.setOnClickListener(v -> mostrarRecordatorios());
        btnEnviarMQTT.setOnClickListener(v -> enviarSenalMQTT());
    }

    // Save reminder to Firebase
    private void guardarRecordatorio() {
        String titulo = etTitulo.getText().toString();
        String descripcion = etDescripcion.getText().toString();
        String fecha = etFecha.getText().toString();
        String hora = etHora.getText().toString();

        if (!titulo.isEmpty() && !descripcion.isEmpty() && !fecha.isEmpty() && !hora.isEmpty()) {
            Map<String, Object> recordatorio = new HashMap<>();
            recordatorio.put("titulo", titulo);
            recordatorio.put("descripcion", descripcion);
            recordatorio.put("fecha", fecha);
            recordatorio.put("hora", hora);

            db.collection("recordatorios")
                    .add(recordatorio)
                    .addOnSuccessListener(documentReference -> {
                        etTitulo.setText("");
                        etDescripcion.setText("");
                        etFecha.setText("");
                        etHora.setText("");
                        tvListaRecordatorios.setText("Recordatorio guardado con éxito.");
                    })
                    .addOnFailureListener(e -> tvListaRecordatorios.setText("Error al guardar: " + e.getMessage()));
        } else {
            tvListaRecordatorios.setText("Todos los campos deben ser llenados.");
        }
    }

    // Display saved reminders
    private void mostrarRecordatorios() {
        db.collection("recordatorios")
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    StringBuilder lista = new StringBuilder();
                    queryDocumentSnapshots.forEach(doc -> {
                        lista.append(doc.getString("titulo")).append(" - ")
                                .append(doc.getString("descripcion")).append("\n")
                                .append(doc.getString("fecha")).append(" a las ")
                                .append(doc.getString("hora")).append("\n\n");
                    });
                    tvListaRecordatorios.setText(lista.toString());
                })
                .addOnFailureListener(e -> tvListaRecordatorios.setText("Error al cargar: " + e.getMessage()));
    }

    // Connect to MQTT broker
    private void conectarMQTT() {
        try {
            mqttClient = new MqttClient(MQTT_BROKER, MqttClient.generateClientId(), null);
            MqttConnectOptions options = new MqttConnectOptions();
            options.setUserName(USERNAME);
            options.setPassword(PASSWORD.toCharArray());
            options.setCleanSession(true);
            mqttClient.connect(options);
            tvListaRecordatorios.setText("Conectado a MQTT.");
        } catch (MqttException e) {
            tvListaRecordatorios.setText("Error al conectar a MQTT: " + e.getMessage());
        }
    }

    // Send MQTT message
    private void enviarSenalMQTT() {
        try {
            String mensaje = "Nuevo recordatorio agendado en la app!";
            MqttMessage mqttMessage = new MqttMessage(mensaje.getBytes());
            mqttMessage.setQos(1);
            mqttClient.publish(TOPICO, mqttMessage);
            tvListaRecordatorios.setText("Mensaje MQTT enviado: " + mensaje);
        } catch (MqttException e) {
            tvListaRecordatorios.setText("Error al enviar mensaje MQTT: " + e.getMessage());
        }
    }
}
