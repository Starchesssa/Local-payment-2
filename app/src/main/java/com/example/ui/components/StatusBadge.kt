package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.HourglassEmpty
import androidx.compose.material.icons.filled.QuestionMark
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.domain.model.TransactionStatus
import com.example.ui.theme.StatusDeclined
import com.example.ui.theme.StatusDeclinedContainer
import com.example.ui.theme.StatusFailed
import com.example.ui.theme.StatusFailedContainer
import com.example.ui.theme.StatusPending
import com.example.ui.theme.StatusPendingContainer
import com.example.ui.theme.StatusSuccess
import com.example.ui.theme.StatusSuccessContainer
import com.example.ui.theme.StatusTimeout
import com.example.ui.theme.StatusTimeoutContainer
import com.example.ui.theme.StatusUnknown
import com.example.ui.theme.StatusUnknownContainer

@Composable
fun StatusBadge(status: TransactionStatus, modifier: Modifier = Modifier) {
    val (backgroundColor, contentColor, icon, text) = when (status) {
        TransactionStatus.SUCCESS -> Quad(
            StatusSuccessContainer,
            StatusSuccess,
            Icons.Default.CheckCircle,
            "SUCCESS"
        )
        TransactionStatus.PENDING -> Quad(
            StatusPendingContainer,
            StatusPending,
            Icons.Default.HourglassEmpty,
            "PENDING"
        )
        TransactionStatus.PROCESSING -> Quad(
            Color(0xFFD1E4FF),
            Color(0xFF005AC1),
            Icons.Default.Sync,
            "PROCESSING"
        )
        TransactionStatus.FAILED -> Quad(
            StatusFailedContainer,
            StatusFailed,
            Icons.Default.Close,
            "FAILED"
        )
        TransactionStatus.DECLINED -> Quad(
            StatusDeclinedContainer,
            StatusDeclined,
            Icons.Default.Close,
            "DECLINED"
        )
        TransactionStatus.TIMEOUT -> Quad(
            StatusTimeoutContainer,
            StatusTimeout,
            Icons.Default.Timer,
            "TIMEOUT"
        )
        TransactionStatus.CANCELLED -> Quad(
            Color(0xFFF1F4F9),
            Color(0xFF41484D),
            Icons.Default.Close,
            "CANCELLED"
        )
        TransactionStatus.UNKNOWN -> Quad(
            StatusUnknownContainer,
            StatusUnknown,
            Icons.Default.QuestionMark,
            "UNKNOWN"
        )
    }

    Box(
        modifier = modifier
            .background(backgroundColor, CircleShape)
            .padding(horizontal = 8.dp, vertical = 3.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = contentColor,
                modifier = Modifier.size(11.dp)
            )
            Spacer(modifier = Modifier.width(3.dp))
            Text(
                text = text,
                color = contentColor,
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 0.5.sp
            )
        }
    }
}

private data class Quad<A, B, C, D>(val first: A, val second: B, val third: C, val fourth: D)
