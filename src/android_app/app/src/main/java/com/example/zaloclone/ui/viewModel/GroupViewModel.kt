package com.example.zaloclone.ui.viewModel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.zaloclone.api.model.*
import com.example.zaloclone.api.repository.IGroupRepository
import com.example.zaloclone.socket.ISocketClient

import io.socket.emitter.Emitter
import com.google.gson.Gson
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone
import com.example.zaloclone.session.SessionManager
import android.util.Log

data class GroupState(
    val groups: List<GetGroupDataResponse> = emptyList(),
    val isGroupsLoading: Boolean = false,
    val error: String? = null
)

class GroupViewModel(
    private val groupRepository: IGroupRepository
) : ViewModel() {

    private val _state = MutableStateFlow(GroupState())
    val state: StateFlow<GroupState> = _state.asStateFlow()

    fun getGroups() {
        viewModelScope.launch {
            _state.value = _state.value.copy(isGroupsLoading = true)
            try {
                val group_response = groupRepository.getGroups()
                _state.value = _state.value.copy(groups = group_response)
            } catch (e: Exception) {
                _state.value = _state.value.copy(error = e.message ?: "Could not load groups")
            } finally {
               _state.value = _state.value.copy(isGroupsLoading = false)
            }
        }
    }

    fun createGroup(
        name: String,
        memberIds: List<Int>,
        onSuccess: (GetGroupDataResponse) -> Unit = {}
    ) {
        viewModelScope.launch {
            try {
                val create_request = GroupCreateRequest(name, memberIds)
                val group_created = groupRepository.createGroup(create_request)
                _state.value = _state.value.copy(groups = listOf(group_created) + _state.value.groups)
                onSuccess(group_created)
            } catch (e: Exception) {
                _state.value = _state.value.copy(error = e.message ?: "Could not create groups")
            }
        }
    }

    private fun updateGroup(group: GetGroupDataResponse) {
        val currentState = _state.value

        _state.value = currentState.copy(
            groups = currentState.groups.map {
                if (it.id == group.id) group else it
            }
        )
    }

    fun addGroupMember(
        groupId: Int,
        userId: Int,
        role: String = "member"
    ) {
        viewModelScope.launch {
            try {
                val addgrmem_request = GroupMemberAddRequest(
                    user_id = userId,
                    role = GroupMemberRoleData(
                        role = if (role.equals("admin", ignoreCase = true)) {
                            GroupMemberRole.ADMIN
                        } else {
                            GroupMemberRole.MEMBER
                        }
                    )
                )
                val addgrmem_response = groupRepository.addMember(groupId, addgrmem_request)
                updateGroup(addgrmem_response)
            } catch (e: Exception) {
                _state.value = _state.value.copy(error = e.message ?: "Could not add member to group")
            }
        }
    }

    fun updateGroupMemberRole(
        groupId: Int,
        userId: Int,
        role: GroupMemberRoleData
    ) {
        viewModelScope.launch {
            try {
                val response = groupRepository.updateMemberRole(groupId, userId, role)
                updateGroup(response)
            } catch (e: Exception) {
                _state.value = _state.value.copy(error = e.message ?: "Could not update member role")
            }
        }
    }
    
    fun refreshGroup(groupId: Int) {
        viewModelScope.launch {
            try {
                val groupResponse = groupRepository.getGroups()
                val group = groupResponse.find {it.id == groupId} ?: return@launch
                updateGroup(group)
            } catch (e: Exception) {
                _state.value = _state.value.copy(error = e.message ?: "Could not refresh group")
            }
        }
    }
}