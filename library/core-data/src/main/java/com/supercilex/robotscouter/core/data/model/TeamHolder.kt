package com.supercilex.robotscouter.core.data.model

import android.os.Bundle
import androidx.lifecycle.LiveData
import com.firebase.ui.common.ChangeEventType
import com.google.firebase.firestore.DocumentSnapshot
import com.supercilex.robotscouter.core.data.ChangeEventListenerBase
import com.supercilex.robotscouter.core.data.UniqueMutableLiveData
import com.supercilex.robotscouter.core.data.ViewModelBase
import com.supercilex.robotscouter.core.data.getTeam
import com.supercilex.robotscouter.core.data.isSignedIn
import com.supercilex.robotscouter.core.data.teams
import com.supercilex.robotscouter.core.data.uid
import com.supercilex.robotscouter.core.data.waitForChange
import com.supercilex.robotscouter.core.logFailures
import com.supercilex.robotscouter.core.model.Team
import kotlinx.coroutines.experimental.async

class TeamHolder : ViewModelBase<Bundle>(), ChangeEventListenerBase {
    private val _teamListener = UniqueMutableLiveData<Team?>()
    val teamListener: LiveData<Team?> = _teamListener

    override fun onCreate(args: Bundle) {
        val team = args.getTeam()
        if (isSignedIn && team.owners.contains(uid)) {
            if (team.id.isBlank()) {
                async {
                    for (potentialTeam in teams.waitForChange()) {
                        if (team.number == potentialTeam.number) {
                            _teamListener.postValue(potentialTeam.copy())
                            return@async
                        }
                    }

                    team.add()
                    _teamListener.postValue(team.copy())
                }.logFailures()
            } else {
                _teamListener.setValue(team)
            }
        } else {
            _teamListener.setValue(null)
        }

        teams.keepAlive = true
        teams.addChangeEventListener(this)
    }

    override fun onChildChanged(
            type: ChangeEventType,
            snapshot: DocumentSnapshot,
            newIndex: Int,
            oldIndex: Int
    ) {
        if (teamListener.value?.id != snapshot.id) return

        if (type == ChangeEventType.REMOVED) {
            _teamListener.setValue(null)
            return
        } else if (type == ChangeEventType.MOVED) {
            return
        }

        _teamListener.setValue(teams[newIndex].copy())
    }

    override fun onDataChanged() {
        val current = teamListener.value ?: return
        if (teams.none { it.id == current.id }) _teamListener.setValue(null)
    }

    override fun onCleared() {
        super.onCleared()
        teams.keepAlive = false
        teams.removeChangeEventListener(this)
    }
}
