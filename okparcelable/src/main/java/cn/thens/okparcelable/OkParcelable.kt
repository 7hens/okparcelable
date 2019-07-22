package cn.thens.okparcelable

import android.os.Parcel
import android.os.Parcelable
import java.lang.reflect.AccessibleObject
import java.lang.reflect.Modifier

interface OkParcelable: Parcelable {

    override fun writeToParcel(parcel: Parcel, flags: Int) {
        val dataClass = javaClass
        parcel.writeString(dataClass.name)
        for (field in dataClass.declaredFields) {
            if (Modifier.isStatic(field.modifiers)) continue
            parcel.writeValue(field.accessible().get(this))
        }
    }

    override fun describeContents(): Int {
        return 0
    }

    @Suppress("UNCHECKED_CAST")
    companion object CREATOR : Parcelable.Creator<OkParcelable> {
        private val unsafeAllocator by lazy { UnsafeAllocator() }

        override fun createFromParcel(parcel: Parcel): OkParcelable {
            val classLoader = OkParcelable::class.java.classLoader!!
            val dataClass = classLoader.loadClass(parcel.readString())
            val data = try {
                dataClass.getConstructor().accessible().newInstance()
            } catch (e: Exception) {
                unsafeAllocator.allocate(dataClass)
            }
            for (field in dataClass.declaredFields) {
                if (Modifier.isStatic(field.modifiers)) continue
                field.accessible().set(data, parcel.readValue(classLoader))
            }
            return data as OkParcelable
        }

        override fun newArray(size: Int): Array<OkParcelable?> {
            return arrayOfNulls(size)
        }

        private fun <T : AccessibleObject> T.accessible(): T = apply { isAccessible = true }

        private class UnsafeAllocator {
            private val unsafeClass = Class.forName("sun.misc.Unsafe")
            private val unsafe = unsafeClass.getDeclaredField("theUnsafe").accessible().get(null)
            private val allocateInstance = unsafeClass.getMethod("allocateInstance", Class::class.java)

            fun allocate(cls: Class<*>): Any {
                return allocateInstance.invoke(unsafe, cls)
            }
        }
    }
}