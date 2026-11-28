package com.example.zaloclone.ui.viewModel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.zaloclone.api.model.*
import com.example.zaloclone.api.repository.IGroupRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class GroupState(
    val groups: List<GetGroupDataResponse> = emptyList(),
    val isGroupsLoading: Boolean = false,
    val error: String? = null
)

class GroupViewModel(
    private val repository: IGroupRepository
) : ViewModel() {

    private val _state = MutableStateFlow(GroupState())
    val state: StateFlow<GroupState> = _state.asStateFlow()

    fun getGroups() {
        viewModelScope.launch {
            _state.value = _state.value.copy(isGroupsLoading = true)
            try {
                val group_response = repository.getGroups()
                val filteredGroups = group_response.filter { group ->
                    group.members.size > 1
                }
                _state.value = _state.value.copy(groups = filteredGroups)
            } catch (e: Exception) {
                _state.value = _state.value.copy(
                    error = e.message ?: "Could not load groups"
                )
            } finally {
                _state.value = _state.value.copy(isGroupsLoading = false)
            }
        }
    }

    fun createGroup(
        name: String,
        memberIds: List<Int>,
        onResult: (Boolean) -> Unit
    ) {
        viewModelScope.launch {
            try {
                val request = GroupCreateRequest(
                    name = name,
                    member_ids = memberIds
                )
                repository.createGroup(request)
                onResult(true)
            } catch (e: Exception) {
                onResult(false)
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
        role: String = "member",
        onResult: (Boolean) -> Unit = {}
    ) {
        viewModelScope.launch {
            try {
                val request = GroupMemberAddRequest(
                    user_id = userId,
                    role = role.lowercase()
                )
                val response = repository.addMember(groupId, request)
                updateGroup(response)
                onResult(true)
            } catch (e: Exception) {
                _state.value = _state.value.copy(
                    error = e.message ?: "Could not add member to group"
                )
                onResult(false)
            }
        }
    }

    fun removeGroupMember(
        groupId: Int,
        userId: Int,
        role: String = "member",
        onResult: (Boolean) -> Unit = {}
    ) {
        viewModelScope.launch {
            try {
                val request = GroupMemberRemoveRequest(
                    user_id = userId,
                    role = role.lowercase()
                )
                val response = repository.removeMember(groupId, request)
                updateGroup(response)
                onResult(true)
            } catch (e: Exception) {
                _state.value = _state.value.copy(
                    error = e.message ?: "Could not remove member to group"
                )
                onResult(false)
            }
        }
    }

    fun leaveGroupMember(
        groupId: Int,
        userId: Int,
        role: String = "member",
        onResult: (Boolean) -> Unit = {}
    ) {
        viewModelScope.launch {
            try {
                val request = GroupMemberLeaveRequest(
                    user_id = userId,
                    role = role.lowercase()
                )
                val response = repository.leaveMember(groupId, request)
                updateGroup(response)
                onResult(true)
            } catch (e: Exception) {
                _state.value = _state.value.copy(
                    error = e.message ?: "Could not leave from group"
                )
                onResult(false)
            }
        }
    }

    fun updateGroupMemberRole(
        groupId: Int,
        userId: Int,
        role: String
    ) {
        viewModelScope.launch {
            try {
                val response = repository.updateMemberRole(groupId, userId, role)
                updateGroup(response)
            } catch (e: Exception) {
                _state.value = _state.value.copy(error = e.message ?: "Could not update member role")
            }
        }
    }
    
    fun refreshGroup(groupId: Int) {
        viewModelScope.launch {
            try {
                val groupResponse = repository.getGroups()
                val group = groupResponse.find {it.id == groupId} ?: return@launch
                updateGroup(group)
            } catch (e: Exception) {
                _state.value = _state.value.copy(error = e.message ?: "Could not refresh group")
            }
        }
    }

    fun clearSession() {
        _state.value = GroupState()
    }
}