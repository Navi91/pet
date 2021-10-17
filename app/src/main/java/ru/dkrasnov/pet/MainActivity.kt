package ru.dkrasnov.pet

import android.annotation.SuppressLint
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import ru.dkrasnov.pet.recycler.RecyclerActivity

class MainActivity : AppCompatActivity() {


    @SuppressLint("MissingPermission")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        startActivity(RecyclerActivity.createIntent(this))
    }
}