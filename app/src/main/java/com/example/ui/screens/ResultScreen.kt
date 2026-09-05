package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
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
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.HourglassEmpty
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Sms
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.domain.model.TransactionStatus
import com.example.ui.components.StatusBadge
import com.example.ui.theme.StatusDeclined
import com.example.ui.theme.StatusFailed
import com.example.ui.theme.StatusPending
import com.example.ui.theme.StatusSuccess
import com.example.ui.viewmodel.PaymentViewModel
import java.text.NumberFormat
import java.util.Locale

@Composable
fun ResultScreen(
    transactionId: String,
    viewModel: PaymentViewModel,
    onDone: () -> Unit,
    onViewDetails: (String) -> Unit,
    onTryAgain: () -> Unit,
    onCheckAgain: () -> Unit,
    modifier: Modifier = Modifier
) {
    val allTransactions by viewModel.allTransactions.collectAsStateWithLifecycle()
    val transaction = remember(allTransactions, transactionId) {
        allTransactions.firstOrNull { it.id == transactionId }
    }

    var showPasteSmsDialog by remember { mutableStateOf(false) }
    var manualSmsInput by remember { mutableStateOf("") }
    var pasteMessageFeedback by remember { mutableStateOf<String?>(null) }

    val formatter = remember { NumberFormat.getNumberInstance(Locale.US) }
    val formattedAmount = transaction?.let { "${it.currency} ${formatter.format(it.amount)}" } ?: ""

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
            Spacer(modifier = Modifier.height(16.dp))

            when (transaction?.status) {
                TransactionStatus.SUCCESS -> {
                    // Success View
                    Box(
                        modifier = Modifier
                            .size(96.dp)
                            .clip(CircleShape)
                            .background(StatusSuccess.copy(alpha = 0.15f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Box(
                            modifier = Modifier
                                .size(64.dp)
                                .clip(CircleShape)
                                .background(StatusSuccess),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Check,
                                contentDescription = "Success",
                                tint = Color.White,
                                modifier = Modifier.size(40.dp)
                            )
                        }
                    }

                    Text(
                        text = "PAYMENT SUCCESSFUL",
                        fontSize = 22.sp,
                        fontWeight = FontWeight.Bold,
                        color = StatusSuccess
                    )

                    // Details Card
                    Card(
                        shape = RoundedCornerShape(24.dp),
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
                                .padding(20.dp),
                            verticalArrangement = Arrangement.spacedBy(14.dp)
                        ) {
                            ResultRow(label = "Amount", value = formattedAmount, isEmphasized = true)
                            ResultRow(label = "Provider", value = transaction.providerName)
                            ResultRow(label = "Recipient Phone", value = transaction.phoneNumber)
                            ResultRow(label = "Reference", value = transaction.reference ?: "Verified via SMS")
                        }
                    }

                    Spacer(modifier = Modifier.weight(1f, fill = false))

                    Button(
                        onClick = onDone,
                        shape = CircleShape,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primary
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(56.dp)
                            .testTag("done_button")
                    ) {
                        Text(text = "DONE", fontWeight = FontWeight.Bold, fontSize = 16.sp, letterSpacing = 1.sp)
                    }

                    OutlinedButton(
                        onClick = { onViewDetails(transactionId) },
                        shape = CircleShape,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(52.dp)
                            .testTag("view_details_button")
                    ) {
                        Text(text = "VIEW DETAILS", fontWeight = FontWeight.SemiBold, fontSize = 15.sp)
                    }
                }

                TransactionStatus.FAILED, TransactionStatus.DECLINED, TransactionStatus.CANCELLED -> {
                    // Failed View
                    val isDeclined = transaction.status == TransactionStatus.DECLINED
                    val headerColor = if (isDeclined) StatusDeclined else StatusFailed
                    val headerText = if (isDeclined) "PAYMENT DECLINED" else "PAYMENT FAILED"

                    Box(
                        modifier = Modifier
                            .size(96.dp)
                            .clip(CircleShape)
                            .background(headerColor.copy(alpha = 0.15f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Box(
                            modifier = Modifier
                                .size(64.dp)
                                .clip(CircleShape)
                                .background(headerColor),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "Failed",
                                tint = Color.White,
                                modifier = Modifier.size(40.dp)
                            )
                        }
                    }

                    Text(
                        text = headerText,
                        fontSize = 22.sp,
                        fontWeight = FontWeight.Bold,
                        color = headerColor
                    )

                    Card(
                        shape = RoundedCornerShape(24.dp),
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
                                .padding(20.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Text(
                                text = "Reason:",
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = transaction.failureReason ?: "Carrier transaction was not completed or funds could not be debited.",
                                fontSize = 14.sp,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.85f),
                                lineHeight = 20.sp
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            ResultRow(label = "Amount", value = formattedAmount)
                            ResultRow(label = "Provider", value = transaction.providerName)
                        }
                    }

                    Spacer(modifier = Modifier.weight(1f, fill = false))

                    Button(
                        onClick = onTryAgain,
                        shape = CircleShape,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primary
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(56.dp)
                            .testTag("try_again_button")
                    ) {
                        Text(text = "TRY AGAIN", fontWeight = FontWeight.Bold, fontSize = 16.sp, letterSpacing = 1.sp)
                    }

                    OutlinedButton(
                        onClick = onDone,
                        shape = CircleShape,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(52.dp)
                    ) {
                        Text(text = "BACK TO HOME", fontWeight = FontWeight.SemiBold, fontSize = 15.sp)
                    }
                }

                else -> {
                    // Pending / Timeout View
                    Box(
                        modifier = Modifier
                            .size(96.dp)
                            .clip(CircleShape)
                            .background(StatusPending.copy(alpha = 0.15f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Box(
                            modifier = Modifier
                                .size(64.dp)
                                .clip(CircleShape)
                                .background(StatusPending),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.HourglassEmpty,
                                contentDescription = "Pending",
                                tint = Color.White,
                                modifier = Modifier.size(40.dp)
                            )
                        }
                    }

                    Text(
                        text = "PAYMENT PENDING",
                        fontSize = 22.sp,
                        fontWeight = FontWeight.Bold,
                        color = StatusPending
                    )

                    Text(
                        text = "We are still waiting for confirmation from your mobile network.",
                        fontSize = 14.sp,
                        textAlign = TextAlign.Center,
                        color = MaterialTheme.colorScheme.outline
                    )

                    Card(
                        shape = RoundedCornerShape(24.dp),
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
                                .padding(20.dp),
                            verticalArrangement = Arrangement.spacedBy(14.dp)
                        ) {
                            ResultRow(label = "Amount", value = formattedAmount, isEmphasized = true)
                            ResultRow(label = "Provider", value = transaction?.providerName ?: "")
                            ResultRow(label = "Phone", value = transaction?.phoneNumber ?: "")
                            ResultRow(label = "Status", value = transaction?.status?.name ?: "PENDING")
                        }
                    }

                    // Fallback to paste SMS if received outside
                    TextButton(onClick = { showPasteSmsDialog = true }) {
                        Icon(imageVector = Icons.Default.Sms, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Paste SMS confirmation", fontSize = 13.sp, color = MaterialTheme.colorScheme.primary)
                    }

                    Spacer(modifier = Modifier.weight(1f, fill = false))

                    Button(
                        onClick = {
                            viewModel.checkTransactionAgain(transactionId)
                            onCheckAgain()
                        },
                        shape = CircleShape,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primary
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(56.dp)
                            .testTag("check_again_button")
                    ) {
                        Icon(imageVector = Icons.Default.Refresh, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(text = "CHECK AGAIN", fontWeight = FontWeight.Bold, fontSize = 16.sp, letterSpacing = 1.sp)
                    }

                    OutlinedButton(
                        onClick = { onViewDetails(transactionId) },
                        shape = CircleShape,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(52.dp)
                    ) {
                        Text(text = "VIEW DETAILS", fontWeight = FontWeight.SemiBold, fontSize = 15.sp)
                    }
                }
            }
        }
    }

    if (showPasteSmsDialog) {
        AlertDialog(
            onDismissRequest = { showPasteSmsDialog = false },
            shape = RoundedCornerShape(24.dp),
            title = { Text("Paste Carrier SMS", fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = "Paste the confirmation SMS you received from the carrier:",
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
private fun ResultRow(
    label: String,
    value: String,
    isEmphasized: Boolean = false
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            fontSize = 13.sp,
            color = MaterialTheme.colorScheme.outline
        )
        Text(
            text = value,
            fontSize = if (isEmphasized) 16.sp else 14.sp,
            fontWeight = if (isEmphasized) FontWeight.Bold else FontWeight.SemiBold,
            color = if (isEmphasized) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
        )
    }
}
