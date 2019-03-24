package com.anenigmatic.apogee19.screens.shared.data.storage

import android.content.SharedPreferences
import androidx.core.content.edit
import com.anenigmatic.apogee19.screens.shared.core.Ticket
import com.anenigmatic.apogee19.screens.shared.util.Optional
import io.reactivex.BackpressureStrategy
import io.reactivex.Completable
import io.reactivex.Flowable
import io.reactivex.subjects.BehaviorSubject

class UserStorageImpl(private val prefs: SharedPreferences) : UserStorage {

    private object Keys {

        const val id = "USER_ID"
        const val name = "USER_NAME"
        const val jwt = "JWT"
        const val qrCode = "QR_CODE"
        const val isBitsian = "IS_BITSIAN"
        const val tickets = "TICKETS"
        const val avatarId = "AVATAR_ID"
    }


    private val userDataSubject = BehaviorSubject.create<Optional<UserStorageData>>()

    private val defaultAvatarId = 0L


    override fun getUserData(): Flowable<Optional<UserStorageData>> {
        emitFromPreferences()
        return userDataSubject.toFlowable(BackpressureStrategy.LATEST)
    }

    override fun setUserData(userData: UserStorageData?): Completable {
        return Completable.create { emitter ->
            try {
                prefs.edit(commit = true) {
                    putLong(Keys.id, userData?.id?: 0)
                    putString(Keys.name, userData?.name)
                    val jwt = if(userData == null) { null } else { "JWT ${userData.jwt}" }
                    putString(Keys.jwt, jwt)
                    putString(Keys.qrCode, userData?.qrCode)
                    putBoolean(Keys.isBitsian, userData?.isBitsian?: false)
                    putStringSet(Keys.tickets, mutableSetOf())
                    putLong(Keys.avatarId, userData?.avatarId?: defaultAvatarId)
                }
                emitter.onComplete()
                emitFromPreferences()
            } catch(e: Exception) {
                emitter.onError(e)
            }
        }
    }

    override fun setAvatarId(avatarId: Long): Completable {
        return Completable.create { emitter ->
            try {
                prefs.edit(commit = true) {
                    putLong(Keys.avatarId, avatarId)
                }
                emitter.onComplete()
                emitFromPreferences()
            } catch(e: Exception) {
                emitter.onError(e)
            }
        }
    }


    private fun emitFromPreferences() {
        try {
            val id = prefs.getLong(Keys.id, 0)
            val name = prefs.getString(Keys.name, null)
            val jwt = prefs.getString(Keys.jwt, null)
            val qrCode = prefs.getString(Keys.qrCode, null)
            val isBitsian = prefs.getBoolean(Keys.isBitsian, false)
            val tickets = prefs.getStringSet(Keys.tickets, mutableSetOf()).toTickets().sortedBy { ticket -> ticket.name }
            val avatarId = prefs.getLong(Keys.avatarId, defaultAvatarId)

            if(id == 0L || name == null || jwt == null || qrCode == null) {
                userDataSubject.onNext(Optional.None)
                return
            }

            userDataSubject.onNext(Optional.Some(UserStorageData(id, name, jwt, qrCode, isBitsian, tickets, avatarId)))
        } catch(e: Exception) {
            userDataSubject.onError(e)
        }
    }


    private fun MutableSet<String>.toTickets(): List<Ticket> {
        return this.map { str ->
            Ticket(str.substringBefore("<|>"), str.substringAfter("<|>").toInt())
        }
    }
}