package com.example.gooponthego

import android.app.Application
import com.example.gooponthego.data.database.AppDatabase
import com.example.gooponthego.data.repository.GameRepository

class GoopApplication : Application() {
    val database by lazy { AppDatabase.getDatabase(this) }
    val repository by lazy { GameRepository(database) }
}
