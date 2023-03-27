package com.example.whereisit

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import androidx.appcompat.app.AppCompatActivity

class Registrarse : AppCompatActivity(){
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.registro_usuarios)
        val registrar = findViewById<Button>(R.id.button2)
        registrar.setOnClickListener(){
            val intent = Intent(this, maps_class::class.java)
            startActivity(intent)
        }
    }
}