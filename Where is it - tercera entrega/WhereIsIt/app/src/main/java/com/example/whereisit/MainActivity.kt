package com.example.whereisit

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.widget.Button
import androidx.annotation.RequiresApi
import androidx.core.app.NotificationCompat
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.ktx.auth
import com.google.firebase.database.*
import com.google.firebase.ktx.Firebase

class MainActivity : AppCompatActivity() {


    private lateinit var auth: FirebaseAuth
    private lateinit var auth2: DatabaseReference
    private val database = FirebaseDatabase.getInstance()
    val PATH_USERS="users/"
    private lateinit var myRef02: DatabaseReference

    companion object {
        private const val CHANNEL_ID = "my_channel_id" // Define tu propio ID de canal
        private const val NOTIFICATION_ID = 123 // Define tu propio ID de notificación
    }

    override fun onStart() {
        super.onStart()
        val currentUser = auth.currentUser
        updateUI(currentUser)
    }

    override fun onCreate(savedInstanceState: Bundle?) {

        super.onCreate(savedInstanceState)
        setContentView(R.layout.main)

        agregarListenerDeEventos()

        auth = Firebase.auth
        auth2 = FirebaseDatabase.getInstance().reference

        val iniciar = findViewById<Button>(R.id.button5)
        iniciar.setOnClickListener(){
            val intent = Intent(this, IniciarSesion::class.java)
            startActivity(intent)
        }

        val registrar = findViewById<Button>(R.id.button6)
        registrar.setOnClickListener(){
            val intent = Intent(this, Registrarse::class.java)
            startActivity(intent)
        }

        myRef02 = database.getReference(PATH_USERS)

    }

    private fun updateUI(currentUser: FirebaseUser?) {
        if (currentUser != null) {
            startMapsActivity(currentUser)
        }
    }

    private fun startMapsActivity(currentUser: FirebaseUser?) {

        auth2 = database.getReference(PATH_USERS+auth.currentUser!!.uid)
        var tipoUsuario: String? = ""

        auth2.addListenerForSingleValueEvent(object : ValueEventListener {

            override fun onDataChange(dataSnapshot: DataSnapshot) {
                // Obtener el valor del campo deseado
                tipoUsuario = dataSnapshot.child("tipo").getValue(String::class.java)
                if (tipoUsuario == "Admin") {
                    val intent = Intent(this@MainActivity, MapsActivityAdmin::class.java)
                    if (currentUser != null) {
                        intent.putExtra("user", currentUser.email)
                    }
                    startActivity(intent)
                } else {
                    val intent = Intent(this@MainActivity, MapsActivityActivos::class.java)
                    if (currentUser != null) {
                        intent.putExtra("user", currentUser.email)
                    }
                    startActivity(intent)
                }
            }

            override fun onCancelled(databaseError: DatabaseError) {
                // Manejar el error de lectura de datos
                Log.e("TAG", "Error al leer datos: ${databaseError.message}")
            }

        })

    }

    private fun agregarListenerDeEventos() {

        val referencia = FirebaseDatabase.getInstance().getReference("users")
        referencia.addChildEventListener(object : ChildEventListener {
            override fun onChildAdded(snapshot: DataSnapshot, previousChildName: String?) {
                // No necesitamos hacer nada aquí, pero el método debe ser implementado
            }

            @RequiresApi(Build.VERSION_CODES.O)
            override fun onChildChanged(snapshot: DataSnapshot, previousChildName: String?) {
                val persona = snapshot.getValue(PersonaClass::class.java)
                if (persona != null && persona.disponible && !persona.toastMostrado) {
                    val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
                    val intent: Intent
                    // Verifica si hay una sesión activa
                    if (FirebaseAuth.getInstance().currentUser == null) {
                        // No hay sesión activa, inicia la actividad de inicio de sesión
                        intent = Intent(this@MainActivity, IniciarSesion::class.java)
                    } else {
                        intent = Intent(this@MainActivity, MapsActivityPorPersona::class.java)
                        intent.putExtra("uid", persona.uid) // Pasamos el uid como extra a la actividad
                    }
                    val pendingIntent = PendingIntent.getActivity(this@MainActivity, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT)
                    // Solo se crea el canal si se está ejecutando en Android 8.0 o versiones posteriores
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                        val name = getString(R.string.Usuario_conectado)
                        val descriptionText = getString(R.string.Usuario_conectado)
                        val importance = NotificationManager.IMPORTANCE_DEFAULT
                        val channel = NotificationChannel(CHANNEL_ID, name, importance).apply {
                            description = descriptionText
                        }
                        // Registra el canal con el sistema; no se puede cambiar la importancia ni otras configuraciones
                        // después de esta llamada
                        val notificationManager: NotificationManager =
                            getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
                        notificationManager.createNotificationChannel(channel)
                    }

                    val builder = NotificationCompat.Builder(this@MainActivity, CHANNEL_ID)
                        .setSmallIcon(R.drawable.diablillo)
                        .setContentTitle(getString(R.string.Usuario_conectado))
                        .setContentText("${persona.nombre} ahora está disponible")
                        .setContentIntent(pendingIntent) // Asignamos el PendingIntent a la notificación
                        .setAutoCancel(true) // La notificación se eliminará al hacer clic en ella
                        .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                    notificationManager.notify(NOTIFICATION_ID, builder.build())
                    myRef02.child(snapshot.key!!).child("toastMostrado").setValue(true)
                }
            }

            override fun onChildRemoved(snapshot: DataSnapshot) {
                // CODE
            }

            override fun onChildMoved(snapshot: DataSnapshot, previousChildName: String?) {
                // No necesitamos hacer nada aquí, pero el método debe ser implementado
            }

            override fun onCancelled(error: DatabaseError) {
                Log.w(ContentValues.TAG, "Error al obtener datos", error.toException())
            }
        })

    }


}