package com.example.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.providers.ProviderRegistry
import com.example.data.sms.CompositeSmsParser
import com.example.data.sms.MatchResult
import com.example.data.sms.TransactionMatcher
import com.example.domain.model.PaymentProvider
import com.example.domain.model.SimInfo
import com.example.domain.model.Transaction
import com.example.domain.model.TransactionStatus
import com.example.domain.repository.TransactionRepository
import com.example.domain.usecase.InitiatePaymentUseCase
import com.example.domain.usecase.PaymentExecutionOutcome
import com.example.domain.usecase.ProcessManualSmsUseCase
import com.example.domain.usecase.ValidatePaymentUseCase
import com.example.domain.usecase.ValidationResult
import com.example.telephony.UssdManager
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class PaymentFormState(
    val selectedProvider: PaymentProvider = ProviderRegistry.getDefaultProvider(),
    val phoneNumber: String = "",
    val amount: String = "",
    val selectedSim: SimInfo? = null,
    val availableSims: List<SimInfo> = emptyList(),
    val useRootUssdOnly: Boolean = false,
    val validationError: ValidationResult.Invalid? = null,
    val isSubmitting: Boolean = false
)

enum class ProgressStep(val label: String, val description: String) {
    PREPARING("Preparing payment...", "Creating local transaction record and validating parameters"),
    STARTING_USSD("Starting USSD...", "Initiating mobile-money session via telephony service"),
    WAITING_RESPONSE("Waiting for response...", "Awaiting carrier system prompt or PIN verification"),
    WAITING_CONFIRMATION("Waiting for confirmation...", "Listening for official mobile-money SMS receipt"),
    CHECKING_TRANSACTION("Checking transaction...", "Verifying transaction signature and balance updates")
}

data class ProgressUiState(
    val currentStep: ProgressStep = ProgressStep.PREPARING,
    val secondsRemaining: Int = 60,
    val progressFraction: Float = 0.1f,
    val ussdMessage: String? = null,
    val isCancelled: Boolean = false
)

class PaymentViewModel(
    private val transactionRepository: TransactionRepository,
    private val ussdManager: UssdManager
) : ViewModel() {

    private val validatePaymentUseCase = ValidatePaymentUseCase()
    private val initiatePaymentUseCase = InitiatePaymentUseCase(transactionRepository, ussdManager)
    private val smsParser = CompositeSmsParser()

    val allTransactions: StateFlow<List<Transaction>> = transactionRepository.getAllTransactions()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val recentTransactions: StateFlow<List<Transaction>> = transactionRepository.getRecentTransactions(10)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _historyFilter = MutableStateFlow<TransactionStatus?>(null)
    val historyFilter: StateFlow<TransactionStatus?> = _historyFilter.asStateFlow()

    val filteredTransactions: StateFlow<List<Transaction>> = combine(allTransactions, _historyFilter) { list, filter ->
        if (filter == null) list else list.filter { it.status == filter }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _formState = MutableStateFlow(PaymentFormState())
    val formState: StateFlow<PaymentFormState> = _formState.asStateFlow()

    private val _progressState = MutableStateFlow(ProgressUiState())
    val progressState: StateFlow<ProgressUiState> = _progressState.asStateFlow()

    private var countdownJob: Job? = null

    init {
        refreshSims()
    }

    fun refreshSims() {
        val sims = ussdManager.getAvailableSims()
        _formState.update {
            it.copy(
                availableSims = sims,
                selectedSim = it.selectedSim ?: sims.firstOrNull()
            )
        }
    }

    fun selectProvider(provider: PaymentProvider) {
        _formState.update {
            it.copy(
                selectedProvider = provider,
                validationError = null
            )
        }
    }

    fun updatePhoneNumber(phone: String) {
        _formState.update { current ->
            // Auto-detect provider if phone prefix matches
            val detected = ProviderRegistry.detectProviderForPhone(phone)
            current.copy(
                phoneNumber = phone,
                selectedProvider = detected ?: current.selectedProvider,
                validationError = null
            )
        }
    }

    fun updateAmount(amount: String) {
        _formState.update {
            it.copy(amount = amount, validationError = null)
        }
    }

    fun selectSim(sim: SimInfo) {
        _formState.update { it.copy(selectedSim = sim) }
    }

    fun toggleRootUssd(useRoot: Boolean) {
        _formState.update { it.copy(useRootUssdOnly = useRoot) }
    }

    fun setHistoryFilter(status: TransactionStatus?) {
        _historyFilter.value = status
    }

    fun submitPayment(onInitiated: (transactionId: String) -> Unit) {
        val currentForm = _formState.value
        val validation = validatePaymentUseCase(
            provider = currentForm.selectedProvider,
            rawPhoneNumber = currentForm.phoneNumber,
            amountText = currentForm.amount
        )

        if (validation is ValidationResult.Invalid) {
            _formState.update { it.copy(validationError = validation) }
            return
        }

        val amountLong = currentForm.amount.replace(",", "").trim().toLong()

        _formState.update { it.copy(isSubmitting = true, validationError = null) }

        viewModelScope.launch {
            // Reset Progress state
            _progressState.value = ProgressUiState(
                currentStep = ProgressStep.PREPARING,
                secondsRemaining = 60,
                progressFraction = 0.15f
            )

            val outcome = initiatePaymentUseCase(
                provider = currentForm.selectedProvider,
                phoneNumber = currentForm.phoneNumber,
                amount = amountLong,
                selectedSim = currentForm.selectedSim,
                useRootUssdOnly = currentForm.useRootUssdOnly
            )

            _formState.update { it.copy(isSubmitting = false) }

            when (outcome) {
                is PaymentExecutionOutcome.Success -> {
                    onInitiated(outcome.transaction.id)
                    startProgressTracker(outcome.transaction.id)
                }
                is PaymentExecutionOutcome.Failed -> {
                    onInitiated(outcome.transaction.id)
                }
            }
        }
    }

    /**
     * Drives the payment progress sequence with realistic timing and timeout handling.
     */
    private fun startProgressTracker(transactionId: String) {
        countdownJob?.cancel()
        countdownJob = viewModelScope.launch {
            // Step 1: Preparing -> Starting USSD
            delay(1200)
            _progressState.update {
                it.copy(currentStep = ProgressStep.STARTING_USSD, progressFraction = 0.35f)
            }

            // Step 2: Waiting for response / PIN prompt
            delay(1500)
            _progressState.update {
                it.copy(currentStep = ProgressStep.WAITING_RESPONSE, progressFraction = 0.55f)
            }

            // Step 3: Waiting for confirmation SMS
            delay(1800)
            _progressState.update {
                it.copy(currentStep = ProgressStep.WAITING_CONFIRMATION, progressFraction = 0.75f)
            }

            // Run countdown for SMS receipt (60 seconds)
            var timeLeft = 60
            while (timeLeft > 0) {
                // Check if transaction has changed status in DB (e.g. via SmsReceiver)
                val current = transactionRepository.getTransactionByIdOnce(transactionId)
                if (current != null && (current.status.isTerminal || current.status == TransactionStatus.SUCCESS)) {
                    _progressState.update {
                        it.copy(currentStep = ProgressStep.CHECKING_TRANSACTION, progressFraction = 1.0f)
                    }
                    return@launch
                }

                delay(1000)
                timeLeft--
                _progressState.update { it.copy(secondsRemaining = timeLeft) }
            }

            // If time ran out and still processing: transition to TIMEOUT
            val finalTx = transactionRepository.getTransactionByIdOnce(transactionId)
            if (finalTx != null && finalTx.status == TransactionStatus.PROCESSING) {
                transactionRepository.updateTransactionStatus(
                    id = transactionId,
                    newStatus = TransactionStatus.TIMEOUT,
                    failureReason = "Confirmation timeout: SMS confirmation was not received within 60 seconds."
                )
            }
        }
    }

    fun cancelPayment(transactionId: String) {
        countdownJob?.cancel()
        viewModelScope.launch {
            transactionRepository.updateTransactionStatus(
                id = transactionId,
                newStatus = TransactionStatus.CANCELLED,
                failureReason = "Transaction cancelled by user."
            )
            _progressState.update { it.copy(isCancelled = true) }
        }
    }

    fun checkTransactionAgain(transactionId: String) {
        viewModelScope.launch {
            val tx = transactionRepository.getTransactionByIdOnce(transactionId) ?: return@launch
            if (tx.status == TransactionStatus.TIMEOUT || tx.status == TransactionStatus.PENDING) {
                // Transition back to PROCESSING to give another attempt/check
                transactionRepository.updateTransactionStatus(
                    id = transactionId,
                    newStatus = TransactionStatus.PROCESSING
                )
                startProgressTracker(transactionId)
            }
        }
    }

    fun pasteAndMatchSms(
        transactionId: String,
        smsText: String,
        onResult: (isSuccess: Boolean, message: String) -> Unit
    ) {
        viewModelScope.launch {
            val currentTx = transactionRepository.getTransactionByIdOnce(transactionId)
            if (currentTx == null) {
                onResult(false, "Transaction not found.")
                return@launch
            }

            // Attempt to parse SMS
            val parsed = smsParser.parse(currentTx.providerId, smsText)
                ?: smsParser.parse("UNKNOWN", smsText)

            if (parsed == null) {
                onResult(false, "Could not extract valid transaction details from the provided SMS.")
                return@launch
            }

            val matcher = TransactionMatcher(
                (transactionRepository as com.example.data.repository.TransactionRepositoryImpl)
                    .let {
                        // Access DAO directly from repository or execute matching
                        com.example.data.database.AppDatabase.getInstance(
                            com.example.telephony.UssdManager::class.java.classLoader?.let { null }
                                ?: return@launch
                        ).transactionDao()
                    }
            )
        }
    }

    /**
     * Direct verification of transaction with explicit SMS confirmation text.
     */
    fun verifyWithSmsText(
        transactionId: String,
        smsText: String,
        onComplete: (Boolean, String) -> Unit
    ) {
        viewModelScope.launch {
            val tx = transactionRepository.getTransactionByIdOnce(transactionId)
            if (tx == null) {
                onComplete(false, "Transaction not found.")
                return@launch
            }

            val parsed = smsParser.parse(tx.providerName, smsText)
                ?: smsParser.parse("SMS", smsText)

            if (parsed == null) {
                onComplete(false, "Unable to recognize mobile-money details in the provided SMS text.")
                return@launch
            }

            if (parsed.isDeclined) {
                transactionRepository.updateTransactionStatus(
                    id = transactionId,
                    newStatus = TransactionStatus.DECLINED,
                    failureReason = parsed.failureReason ?: "Declined as stated in SMS",
                    rawSms = smsText
                )
                onComplete(false, "Transaction marked as DECLINED based on SMS.")
            } else if (parsed.isSuccess) {
                transactionRepository.updateTransactionStatus(
                    id = transactionId,
                    newStatus = TransactionStatus.SUCCESS,
                    reference = parsed.reference ?: "SMS-CONFIRMED",
                    rawSms = smsText
                )
                onComplete(true, "Transaction successfully confirmed!")
            } else {
                onComplete(false, "SMS does not contain positive confirmation.")
            }
        }
    }

    fun clearAllHistory() {
        viewModelScope.launch {
            transactionRepository.clearHistory()
        }
    }
}
