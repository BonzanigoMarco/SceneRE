package uzh.scenere.activities

import android.os.Bundle
import android.util.Log
import uzh.scenere.R
import uzh.scenere.datamodel.Project
import uzh.scenere.datamodel.database.DatabaseHelper
import uzh.scenere.helpers.TestingHelper

abstract class AbstractManagementActivity : AbstractBaseActivity() {
    enum class LockState{
        LOCKED, UNLOCKED
    }

    protected var lockState: LockState = LockState.LOCKED

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
    }

    protected fun changeLockState(): String{
        lockState = if (lockState==LockState.LOCKED) LockState.UNLOCKED else LockState.LOCKED
        return getLockIcon()
    }

    protected fun getLockIcon(): String{
        return when(lockState){
            LockState.LOCKED -> resources.getString(R.string.icon_lock)
            LockState.UNLOCKED -> resources.getString(R.string.icon_lock_open)
        }
    }
}