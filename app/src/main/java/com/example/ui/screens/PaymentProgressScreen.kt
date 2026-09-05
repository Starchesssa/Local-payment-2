package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.HourglassBottom
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.PhoneInTalk
import androidx.compose.material.icons.filled.Sms
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.domain.model.Transaction
import com.example.domain.model.TransactionStatus
import com.example.ui.components.PinSecurityNotice
import com.example.ui.components.ProviderLogoIcon
import com.example.ui.viewmodel.PaymentViewModel
import com.example.ui.viewmodel.ProgressStep

@Composable
fun PaymentProgressScreen(
    transactionId: String,
    viewModel: PaymentViewModel,
    onNavigateToResult: (transactionId: String) -> Unit,
    modifier: Modifier = Modifier
) {
    val progressState by viewModel.progressState.collectAsStateWithLifecycle()
    val allTransactions by viewModel.allTransactions.collectAsStateWithLifecycle()

    val currentTransaction = remember(allTransactions, transactionId) {
        allTransactions.firstOrNull { it.id == transactionId }
    }

    // Auto-navigate to result screen when status reaches final or timeout state
    LaunchedEffect(currentTransaction?.status) {
        val status = currentTransaction?.status
        if (status != null && status != TransactionStatus.PROCESSING && status != TransactionStatus.PENDING) {
            onNavigateToResult(transactionId)
        }
    }

    var showPasteSmsDialog by remember { mutableStateOf(false) }
    var manualSmsInput by remember { mutableStateOf("") }
    var pasteMessageFeedback by remember { mutableStateOf<String?>(null) }

    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 0.95f,
        targetValue = 1.05f,
        animationSpec = infiniteRepeatable(
            animation = tween(900, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "scale"
    )

    Scaffold(modifier = modifier) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            Spacer(modifier = Modifier.height(12.dp))

            // Animated Pulse Icon
            Box(
                modifier = Modifier
                    .size(90.dp)
                    .graphicsLayer(scaleX = pulseScale, scaleY = pulseScale)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primaryContainer),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.PhoneInTalk,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(46.dp)
                )
            }

            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = "Payment in progress",
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground
                )
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = progressState.currentStep.description,
                    fontSize = 13.sp,
                    textAlign = TextAlign.Center,
                    color = MaterialTheme.colorScheme.outline
                )
            }

            // Transaction Summary Card
            if (currentTransaction != null) {
                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surface
                    ),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                    elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        ProviderLogoIcon(providerId = currentTransaction.providerId, size = 44.dp)
                        Spacer(modifier = Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "${currentTransaction.currency} ${currentTransaction.amount}",
                                fontWeight = FontWeight.Bold,
                                fontSize = 16.sp
                            )
                            Text(
                                text = "Recipient: ${currentTransaction.phoneNumber}",
                                fontSize = 13.sp,
                                color = MaterialTheme.colorScheme.outline
                            )
                            Text(
                                text = "Provider: ${currentTransaction.providerName}",
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.outline
                            )
                        }
                    }
                }
            }

            // Progress Bar
            Column(modifier = Modifier.fillMaxWidth()) {
                LinearProgressIndicator(
                    progress = { progressState.progressFraction },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(8.dp)
                        .clip(CircleShape),
                    color = MaterialTheme.colorScheme.primary,
                    trackColor = MaterialTheme.colorScheme.surfaceVariant
                )
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = progressState.currentStep.label,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text = "${progressState.secondsRemaining}s remaining",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.outline
                    )
                }
            }

            // Step-by-Step Flow List
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surface
                ),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(18.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    StepItem(
                        title = "Preparing payment...",
                        isDone = progressState.currentStep.ordinal > ProgressStep.PREPARING.ordinal,
                        isActive = progressState.currentStep == ProgressStep.PREPARING
                    )
                    StepItem(
                        title = "Starting USSD...",
                        isDone = progressState.currentStep.ordinal > ProgressStep.STARTING_USSD.ordinal,
                        isActive = progressState.currentStep == ProgressStep.STARTING_USSD
                    )
                    StepItem(
                        title = "Waiting for response / PIN prompt...",
                        isDone = progressState.currentStep.ordinal > ProgressStep.WAITING_RESPONSE.ordinal,
                        isActive = progressState.currentStep == ProgressStep.WAITING_RESPONSE
                    )
                    StepItem(
                        title = "Waiting for confirmation SMS...",
                        isDone = progressState.currentStep.ordinal > ProgressStep.WAITING_CONFIRMATION.ordinal,
                        isActive = progressState.currentStep == ProgressStep.WAITING_CONFIRMATION
                    )
                    StepItem(
                        title = "Checking transaction...",
                        isDone = progressState.currentStep.ordinal >= ProgressStep.CHECKING_TRANSACTION.ordinal,
                        isActive = progressState.currentStep == ProgressStep.CHECKING_TRANSACTION
                    )
                }
            }

            // Important Carrier Dialog Guidance
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.35f)
                ),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.secondary.copy(alpha = 0.2f)),
                elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(14.dp),
                    verticalAlignment = Alignment.Top
                ) {
                    Icon(
                        imageVector = Icons.Default.Lock,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.secondary,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        text = "Carrier Action Required: If the USSD dialog pops up on your screen, enter your mobile money PIN there to authorize the transfer. This app will automatically capture the confirmation SMS.",
                        fontSize = 12.sp,
                        lineHeight = 16.sp,
                        color = MaterialTheme.colorScheme.onSecondaryContainer
                    )
                }
            }

            // Security Notice
            PinSecurityNotice()

            Spacer(modifier = Modifier.height(4.dp))

            // Action: Manual SMS Input fallback
            TextButton(
                onClick = { showPasteSmsDialog = true },
                modifier = Modifier.testTag("paste_sms_button")
            ) {
                Icon(imageVector = Icons.Default.Sms, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text("Received SMS? Paste here to confirm", fontSize = 13.sp, color = MaterialTheme.colorScheme.primary)
            }

            // Cancel Button (where technically possible)
            OutlinedButton(
                onClick = { viewModel.cancelPayment(transactionId) },
                shape = CircleShape,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp)
                    .testTag("cancel_payment_button")
            ) {
                Icon(imageVector = Icons.Default.Close, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Cancel Payment", fontWeight = FontWeight.SemiBold)
            }
        }
    }

    // Manual SMS Paste Dialog
    if (showPasteSmsDialog) {
        AlertDialog(
            onDismissRequest = { showPasteSmsDialog = false },
            shape = RoundedCornerShape(24.dp),
            title = { Text("Paste Carrier SMS", fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = "If your carrier sent an SMS receipt to your notifications, paste the entire message here to verify instantly:",
                        fontSize = 13.sp
                    )
                    OutlinedTextField(
                        value = manualSmsInput,
                        onValueChange = { manualSmsInput = it },
                        placeholder = { Text("e.g. 5KA782910J Imethibitishwa. TSh 10,000 imetumwa...") },
                        shape = RoundedCornerShape(16.dp),
                        minLines = 3,
                        maxLines = 5,
                        modifier = Modifier.fillMaxWidth()
                    )
                    pasteMessageFeedback?.let { feedback ->
                        Text(text = feedback, color = MaterialTheme.colorScheme.error, fontSize = 12.sp)
                    }
                }
            },
            confirmButton = {
                Button(
                    shape = CircleShape,
                    onClick = {
                        if (manualSmsInput.isNotBlank()) {
                            viewModel.verifyWithSmsText(transactionId, manualSmsInput) { success, msg ->
                                if (success) {
                                    showPasteSmsDialog = false
                                    onNavigateToResult(transactionId)
                                } else {
                                    pasteMessageFeedback = msg
                                }
                            }
                        }
                    }
                ) {
                    Text("Verify SMS", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showPasteSmsDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}

@Composable
private fun StepItem(
    title: String,
    isDone: Boolean,
    isActive: Boolean
) {
    val indicatorColor by animateColorAsState(
        targetValue = when {
            isDone -> MaterialTheme.colorScheme.primary
            isActive -> MaterialTheme.colorScheme.secondary
            else -> MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)
        },
        label = "color"
    )

    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
            modifier = Modifier
                .size(24.dp)
                .clip(CircleShape)
                .background(indicatorColor),
            contentAlignment = Alignment.Center
        ) {
            if (isDone) {
                Icon(
                    imageVector = Icons.Default.Check,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onPrimary,
                    modifier = Modifier.size(14.dp)
                )
            } else if (isActive) {
                Icon(
                    imageVector = Icons.Default.Sync,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSecondary,
                    modifier = Modifier.size(14.dp)
                )
            } else {
                Box(
                    modifier = Modifier
                        .size(8.dp)
                        .clip(CircleShape)
                        .background(Color.White)
                )
            }
        }

        Spacer(modifier = Modifier.width(12.dp))

        Text(
            text = title,
            fontSize = 14.sp,
            fontWeight = if (isActive || isDone) FontWeight.SemiBold else FontWeight.Normal,
            color = if (isActive) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.outline
        )
    }
}
