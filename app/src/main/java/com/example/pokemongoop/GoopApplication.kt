package com.example.pokemongoop

import android.app.Application
import com.example.pokemongoop.data.database.AppDatabase
import com.example.pokemongoop.data.repository.GameRepository

class GoopApplication : Application() {
    val database by lazy { AppDatabase.getDatabase(this) }
    val repository by lazy { GameRepository(database) }
}
