package com.example.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.ai.AiBriefingCallResult
import com.example.data.ai.AiCapabilityTier
import com.example.data.ai.AiChatMessage
import com.example.data.ai.AiChatStreamEvent
import com.example.data.ai.AiFeatureType
import com.example.data.ai.AiQuotaManager
import com.example.data.ai.AiQuotaSnapshot
import com.example.data.ai.AiWeatherBriefing
import com.example.data.ai.LocalMeteorologistEngine
import com.example.data.ai.MeteorologistAiRepository
import com.example.data.model.CityLocation
import com.example.data.model.CurrentWeather
import com.example.data.model.UnitSystem
import com.example.data.repository.FullWeatherPackage
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.UUID

sealed interface AiBriefingUiState {
    data object Idle : AiBriefingUiState
    data object Loading : AiBriefingUiState
    data class Success(val briefing: AiWeatherBriefing) : AiBriefingUiState
    data class Error(
        val message: String,
        val isRateLimited: Boolean = false,
        val isQuotaExhausted: Boolean = false
    ) : AiBriefingUiState
}

data class AiMeteorologistUiState(
    val briefingState: AiBriefingUiState = AiBriefingUiState.Idle,
    val messages: List<AiChatMessage> = emptyList(),
    val isStreamingChat: Boolean = false,
    val chatError: String? = null,
    val chatRateLimited: Boolean = false,
    val lastQuestion: String = "",
    val showQuotaRecoveryCard: Boolean = false,
    val quota: AiQuotaSnapshot,
    val graceJustClaimed: Boolean = false
)

class AiMeteorologistViewModel(
    application: Application
) : AndroidViewModel(application) {

    private val quotaManager = AiQuotaManager(application)
    private val aiRepository = MeteorologistAiRepository(quotaManager, application)

    private val _uiState = MutableStateFlow(
        AiMeteorologistUiState(quota = quotaManager.getSnapshot())
    )
    val uiState: StateFlow<AiMeteorologistUiState> = _uiState.asStateFlow()

    private var briefingJob: Job? = null
    private var chatJob: Job? = null

    fun refreshQuota() {
        _uiState.update { it.copy(quota = quotaManager.getSnapshot()) }
    }

    fun formatResetCountdown(millis: Long): String = quotaManager.formatResetCountdown(millis)

    fun toggleEcoLiteMode() {
        val next = !quotaManager.getSnapshot().ecoLiteMode
        quotaManager.setEcoLiteMode(next)
        _uiState.update { it.copy(quota = quotaManager.getSnapshot()) }
    }

    fun enableEcoLiteMode() {
        quotaManager.setEcoLiteMode(true)
        _uiState.update { it.copy(quota = quotaManager.getSnapshot()) }
    }

    fun claimDailyGraceCredits(): Boolean {
        val claimed = quotaManager.claimDailyGraceCredits()
        _uiState.update {
            it.copy(
                quota = quotaManager.getSnapshot(),
                graceJustClaimed = claimed,
                showQuotaRecoveryCard = if (claimed) false else it.showQuotaRecoveryCard
            )
        }
        return claimed
    }

    fun saveByokKey(rawKey: String) {
        quotaManager.saveByokKey(rawKey)
        _uiState.update {
            it.copy(
                quota = quotaManager.getSnapshot(),
                showQuotaRecoveryCard = false
            )
        }
    }

    fun clearByokKey() {
        quotaManager.clearByokKey()
        _uiState.update { it.copy(quota = quotaManager.getSnapshot()) }
    }

    fun getCurrentByokKey(): String = quotaManager.getByokKey()

    fun toggleQuotaRecoveryCard(show: Boolean) {
        _uiState.update {
            it.copy(
                showQuotaRecoveryCard = show,
                quota = quotaManager.getSnapshot()
            )
        }
    }

    fun generateBriefing(
        location: CityLocation,
        current: CurrentWeather,
        pkg: FullWeatherPackage,
        unitSystem: UnitSystem,
        forceRefresh: Boolean = false,
        forceEcoLite: Boolean = false
    ) {
        briefingJob?.cancel()
        briefingJob = viewModelScope.launch {
            // Check 30-min per-city cache first (0 credits!)
            if (!forceRefresh) {
                val cached = aiRepository.getCachedBriefing(location, unitSystem)
                if (cached != null) {
                    quotaManager.recordCacheSave()
                    _uiState.update {
                        it.copy(
                            briefingState = AiBriefingUiState.Success(cached),
                            quota = quotaManager.getSnapshot()
                        )
                    }
                    return@launch
                }
            }

            // Deduct-with-refund guard
            val reserved = quotaManager.reserveCredit(AiFeatureType.BRIEFING)
            if (!reserved) {
                _uiState.update {
                    it.copy(
                        briefingState = AiBriefingUiState.Error(
                            message = getApplication<Application>().getString(com.example.R.string.ai_error_daily_allowance_reached),
                            isRateLimited = true,
                            isQuotaExhausted = true
                        ),
                        showQuotaRecoveryCard = true,
                        quota = quotaManager.getSnapshot()
                    )
                }
                return@launch
            }

            _uiState.update {
                it.copy(
                    briefingState = AiBriefingUiState.Loading,
                    quota = quotaManager.getSnapshot()
                )
            }

            when (val result = aiRepository.generateWeatherBriefing(
                location = location,
                current = current,
                pkg = pkg,
                unitSystem = unitSystem,
                forceRefresh = forceRefresh,
                forceEcoLite = forceEcoLite
            )) {
                is AiBriefingCallResult.Success -> {
                    _uiState.update {
                        it.copy(
                            briefingState = AiBriefingUiState.Success(result.briefing),
                            showQuotaRecoveryCard = false,
                            quota = quotaManager.getSnapshot()
                        )
                    }
                }
                is AiBriefingCallResult.Error -> {
                    // Refund credit so users never lose allowance to errors or timeouts
                    quotaManager.refundCredit(AiFeatureType.BRIEFING)
                    _uiState.update {
                        it.copy(
                            briefingState = AiBriefingUiState.Error(
                                message = result.message,
                                isRateLimited = result.isRateLimited,
                                isQuotaExhausted = false
                            ),
                            showQuotaRecoveryCard = result.isRateLimited,
                            quota = quotaManager.getSnapshot()
                        )
                    }
                }
            }
        }
    }

    fun useLocalFallbackBriefing(
        location: CityLocation,
        current: CurrentWeather,
        pkg: FullWeatherPackage,
        unitSystem: UnitSystem
    ) {
        val localBriefing = LocalMeteorologistEngine.buildDeterministicBriefing(
            location = location,
            current = current,
            pkg = pkg,
            unitSystem = unitSystem,
            context = getApplication()
        )
        _uiState.update {
            it.copy(
                briefingState = AiBriefingUiState.Success(localBriefing),
                showQuotaRecoveryCard = false,
                quota = quotaManager.getSnapshot()
            )
        }
    }

    fun sendChatMessage(
        question: String,
        location: CityLocation,
        current: CurrentWeather,
        pkg: FullWeatherPackage,
        unitSystem: UnitSystem,
        forceEcoLite: Boolean = false
    ) {
        val cleanQuestion = question.trim()
        if (cleanQuestion.isEmpty() || _uiState.value.isStreamingChat) return

        val reserved = quotaManager.reserveCredit(AiFeatureType.CHAT)
        if (!reserved) {
            _uiState.update {
                it.copy(
                    lastQuestion = cleanQuestion,
                    chatError = getApplication<Application>().getString(com.example.R.string.ai_error_daily_allowance_reached),
                    chatRateLimited = true,
                    showQuotaRecoveryCard = true,
                    quota = quotaManager.getSnapshot()
                )
            }
            return
        }

        val historySnapshot = _uiState.value.messages
        val userMsg = AiChatMessage(
            id = UUID.randomUUID().toString(),
            isUser = true,
            text = cleanQuestion
        )
        val assistantMsgId = UUID.randomUUID().toString()
        val placeholderAssistant = AiChatMessage(
            id = assistantMsgId,
            isUser = false,
            text = "",
            isStreaming = true
        )

        _uiState.update {
            it.copy(
                messages = it.messages + userMsg + placeholderAssistant,
                isStreamingChat = true,
                chatError = null,
                chatRateLimited = false,
                lastQuestion = cleanQuestion,
                quota = quotaManager.getSnapshot()
            )
        }

        chatJob?.cancel()
        chatJob = viewModelScope.launch {
            var completed = false
            aiRepository.streamMeteorologistAnswer(
                question = cleanQuestion,
                recentHistory = historySnapshot,
                location = location,
                current = current,
                pkg = pkg,
                unitSystem = unitSystem,
                forceEcoLite = forceEcoLite
            ).collect { event ->
                when (event) {
                    is AiChatStreamEvent.Chunk -> {
                        _uiState.update { state ->
                            state.copy(
                                messages = state.messages.map { m ->
                                    if (m.id == assistantMsgId) {
                                        m.copy(
                                            text = event.accumulatedText,
                                            tierUsed = event.tierUsed,
                                            modelLabel = event.modelLabel,
                                            isStreaming = true
                                        )
                                    } else m
                                }
                            )
                        }
                    }

                    is AiChatStreamEvent.Complete -> {
                        completed = true
                        if (event.isFromCache) {
                            // Dedup cache hit costs 0 credits
                            quotaManager.refundCredit(AiFeatureType.CHAT)
                        }
                        _uiState.update { state ->
                            state.copy(
                                messages = state.messages.map { m ->
                                    if (m.id == assistantMsgId) {
                                        m.copy(
                                            text = event.fullText,
                                            tierUsed = event.tierUsed,
                                            modelLabel = event.modelLabel,
                                            isStreaming = false,
                                            isFromCache = event.isFromCache
                                        )
                                    } else m
                                },
                                isStreamingChat = false,
                                chatError = null,
                                chatRateLimited = false,
                                quota = quotaManager.getSnapshot()
                            )
                        }
                    }

                    is AiChatStreamEvent.Error -> {
                        if (!completed) {
                            quotaManager.refundCredit(AiFeatureType.CHAT)
                        }
                        _uiState.update { state ->
                            state.copy(
                                messages = state.messages.filterNot { it.id == assistantMsgId },
                                isStreamingChat = false,
                                chatError = event.message,
                                chatRateLimited = event.isRateLimited,
                                showQuotaRecoveryCard = event.isRateLimited || state.showQuotaRecoveryCard,
                                quota = quotaManager.getSnapshot()
                            )
                        }
                    }
                }
            }
        }
    }

    fun sendLocalReflexAnswer(
        question: String,
        location: CityLocation,
        current: CurrentWeather,
        pkg: FullWeatherPackage,
        unitSystem: UnitSystem
    ) {
        val app = getApplication<Application>()
        val cleanQuestion = question.trim().ifEmpty { app.getString(com.example.R.string.ai_default_local_question) }
        val localAnswer = LocalMeteorologistEngine.answerLocally(
            question = cleanQuestion,
            location = location,
            current = current,
            pkg = pkg,
            unitSystem = unitSystem,
            context = app
        )
        val userMsg = AiChatMessage(
            id = UUID.randomUUID().toString(),
            isUser = true,
            text = cleanQuestion
        )
        val localMsg = AiChatMessage(
            id = UUID.randomUUID().toString(),
            isUser = false,
            text = localAnswer,
            tierUsed = AiCapabilityTier.LOCAL_METEOROLOGY,
            modelLabel = app.getString(com.example.R.string.ai_local_model_zero_credits),
            isStreaming = false
        )
        _uiState.update {
            it.copy(
                messages = it.messages + userMsg + localMsg,
                chatError = null,
                chatRateLimited = false,
                showQuotaRecoveryCard = false,
                quota = quotaManager.getSnapshot()
            )
        }
    }

    fun clearChat() {
        chatJob?.cancel()
        _uiState.update {
            it.copy(
                messages = emptyList(),
                isStreamingChat = false,
                chatError = null,
                chatRateLimited = false
            )
        }
    }

    override fun onCleared() {
        super.onCleared()
        briefingJob?.cancel()
        chatJob?.cancel()
    }
}
