package cn.thens.okparcelable.sample

import android.app.Activity
import android.app.Service
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.os.Bundle
import android.os.IBinder
import android.util.Log
import android.widget.Toast
import cn.thens.okbinder.OkBinder
import cn.thens.okparcelable.OkParcelable
import kotlinx.android.synthetic.main.activity_main.*

@OkBinder.Interface
interface IRemoteService {
    fun test(aString: String, aPerson: Person): Person
}

data class Pet(
    val name: String
) : OkParcelable

data class Person(
    val name: String,
    val age: Int,
    val pet: Pet
) : OkParcelable

private const val TAG = "@OkParcelable"

class MainActivity : Activity(), ServiceConnection {
    private var isServiceConnected = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        vTestLocalService.setOnClickListener {
            rebindService(LocalService::class.java)
        }
        vTestRemoteService.setOnClickListener {
            rebindService(RemoteService::class.java)
        }
    }

    private fun rebindService(serviceClass: Class<*>) {
        if (isServiceConnected) {
            unbindService(this)
        }
        bindService(Intent(this, serviceClass), this, Context.BIND_AUTO_CREATE)
        isServiceConnected = true
    }

    override fun onServiceConnected(name: ComponentName?, service: IBinder?) {
        Toast.makeText(this, "please check the log", Toast.LENGTH_SHORT).show()
        val remoteService = OkBinder.proxy(service!!, IRemoteService::class.java)
        try {
            remoteService.test("HELLO", Person("Thens", 18, Pet("PET")))
        } catch (e: Exception) {
            Log.e(TAG, "remoteService.testError => \n" + Log.getStackTraceString(e))
        }
    }

    override fun onServiceDisconnected(name: ComponentName?) {
    }
}

abstract class BaseService : Service() {
    private val okBinder = OkBinder(object : IRemoteService {
        override fun test(aString: String, aPerson: Person): Person {
            Log.d(TAG, ">> ** IRemoteService.test: aString = $aString ** <<")
            Log.d(TAG, ">> ** IRemoteService.test: aPerson = $aPerson ** <<")
            return Person("Jessie", 18, Pet("PET"))
        }
    })

    override fun onBind(intent: Intent?): IBinder? {
        return okBinder
    }
}

class LocalService : BaseService()

class RemoteService : BaseService()