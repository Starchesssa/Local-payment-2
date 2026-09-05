package com.example.telephony

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.telecom.TelecomManager
import android.telephony.SubscriptionInfo
import android.telephony.SubscriptionManager
import android.telephony.TelephonyManager
import androidx.core.content.ContextCompat
import com.example.domain.model.SimInfo
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume

sealed class UssdResult {
    /**
     * Programmatic response received directly via Android public API sendUssdRequest
     */
    data class ProgrammaticSuccess(val returnMessage: CharSequence) : UssdResult()

    /**
     * The carrier or OS requires interactive USSD session (e.g. PIN entry / submenus).
     * System dialer USSD dialog was safely launched so the user can interact directly.
     */
    data class CarrierDialogLaunched(val message: String) : UssdResult()

    /**
     * Fallback to dialer when CALL_PHONE is not granted or carrier requires manual dial.
     */
    data class DialerLaunched(val message: String) : UssdResult()

    /**
     * Failure with descriptive error
     */
    data class Failed(val error: UssdError) : UssdResult()
}

sealed class UssdError(val message: String) {
    object NoSimCard : UssdError("No active SIM card detected on device.")
    object SimNotReady : UssdError("SIM card is not ready or network service is unavailable.")
    object PermissionMissing : UssdError("CALL_PHONE permission is required to initiate USSD calls.")
    data class CarrierFailure(val code: Int, val detail: String) : UssdError("USSD failed ($code): $detail")
    data class GeneralException(val exceptionMessage: String) : UssdError("Telephony error: $exceptionMessage")
}

class UssdManager(private val context: Context) {

    private val telephonyManager = context.getSystemService(Context.TELEPHONY_SERVICE) as TelephonyManager
    private val mainHandler = Handler(Looper.getMainLooper())

    /**
     * Checks if a SIM card is present and ready.
     */
    fun isSimReady(): Boolean {
        return telephonyManager.simState == TelephonyManager.SIM_STATE_READY
    }

    /**
     * Inspects active SIM subscriptions using Android public SubscriptionManager APIs.
     * Returns a list of available SIM slots.
     */
    fun getAvailableSims(): List<SimInfo> {
        val hasPhoneStatePermission = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.READ_PHONE_STATE
        ) == PackageManager.PERMISSION_GRANTED

        if (!hasPhoneStatePermission) {
            // Return placeholder representation of default SIM without violating permissions
            return if (isSimReady()) {
                listOf(
                    SimInfo(
                        subscriptionId = SubscriptionManager.DEFAULT_SUBSCRIPTION_ID,
                        slotIndex = 0,
                        displayName = "Default SIM",
                        carrierName = telephonyManager.networkOperatorName.ifBlank { "Mobile Network" },
                        isDefault = true
                    )
                )
            } else emptyList()
        }

        return try {
            val subscriptionManager = context.getSystemService(Context.TELEPHONY_SUBSCRIPTION_SERVICE) as? SubscriptionManager
            val activeList: List<SubscriptionInfo>? = subscriptionManager?.activeSubscriptionInfoList
            if (!activeList.isNullOrEmpty()) {
                val defaultSubId = SubscriptionManager.getDefaultSubscriptionId()
                activeList.map { subInfo ->
                    SimInfo(
                        subscriptionId = subInfo.subscriptionId,
                        slotIndex = subInfo.simSlotIndex,
                        displayName = subInfo.displayName?.toString() ?: "SIM ${subInfo.simSlotIndex + 1}",
                        carrierName = subInfo.carrierName?.toString() ?: "Carrier",
                        isDefault = subInfo.subscriptionId == defaultSubId
                    )
                }
            } else {
                if (isSimReady()) {
                    listOf(
                        SimInfo(
                            subscriptionId = SubscriptionManager.DEFAULT_SUBSCRIPTION_ID,
                            slotIndex = 0,
                            displayName = "SIM 1",
                            carrierName = telephonyManager.networkOperatorName.ifBlank { "Active Carrier" },
                            isDefault = true
                        )
                    )
                } else emptyList()
            }
        } catch (_: SecurityException) {
            emptyList()
        }
    }

    /**
     * Executes a USSD request using Android public APIs.
     *
     * 1. If programmatic USSD is available and supported, it attempts sendUssdRequest.
     * 2. If the carrier requires interactive session, PIN entry, or returns failure code,
     *    it cleanly transitions to ACTION_CALL so Android's native carrier USSD dialog displays.
     * 3. If CALL_PHONE permission is not granted, it falls back to ACTION_DIAL.
     */
    suspend fun executeUssd(
        ussdCode: String,
        selectedSim: SimInfo? = null,
        forceCarrierDialog: Boolean = false
    ): UssdResult {
        if (!isSimReady()) {
            return UssdResult.Failed(UssdError.NoSimCard)
        }

        val hasCallPhone = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.CALL_PHONE
        ) == PackageManager.PERMISSION_GRANTED

        if (!hasCallPhone) {
            // Safe fallback without crash: Open dialer with pre-filled code
            return launchDialerIntent(ussdCode)
        }

        // If direct carrier dialog is preferred or if user explicitly requested interactive mode
        if (forceCarrierDialog) {
            return launchActionCall(ussdCode, selectedSim)
        }

        // Attempt Android 8.0+ (API 26+) public programmatic API
        return suspendCancellableCoroutine { continuation ->
            val targetTelephony = if (selectedSim != null && selectedSim.subscriptionId != SubscriptionManager.DEFAULT_SUBSCRIPTION_ID) {
                telephonyManager.createForSubscriptionId(selectedSim.subscriptionId)
            } else {
                telephonyManager
            }

            try {
                val callback = object : TelephonyManager.UssdResponseCallback() {
                    override fun onReceiveUssdResponse(
                        telephony: TelephonyManager,
                        request: String,
                        response: CharSequence
                    ) {
                        if (continuation.isActive) {
                            continuation.resume(UssdResult.ProgrammaticSuccess(response))
                        }
                    }

                    override fun onReceiveUssdResponseFailed(
                        telephony: TelephonyManager,
                        request: String,
                        failureCode: Int
                    ) {
                        // When failureCode is USSD_RETURN_FAILURE (-1), the carrier network typically requires
                        // interactive PIN entry or multi-step session which cannot be completed via programmatic single callback.
                        // We immediately fall back to the system carrier dialog!
                        if (continuation.isActive) {
                            val dialogResult = launchActionCall(
                                ussdCode,
                                selectedSim,
                                note = "Interactive session required by carrier. Launched system dialog."
                            )
                            continuation.resume(dialogResult)
                        }
                    }
                }

                targetTelephony.sendUssdRequest(ussdCode, callback, mainHandler)
            } catch (se: SecurityException) {
                if (continuation.isActive) {
                    continuation.resume(launchDialerIntent(ussdCode))
                }
            } catch (e: Exception) {
                if (continuation.isActive) {
                    // Fallback to ACTION_CALL
                    continuation.resume(launchActionCall(ussdCode, selectedSim, note = e.localizedMessage ?: "Fallback"))
                }
            }
        }
    }

    /**
     * Launches Android system Call Intent to trigger the official carrier USSD dialog.
     * This ensures the user safely enters their PIN into the carrier's secure system prompt.
     */
    fun launchActionCall(
        ussdCode: String,
        selectedSim: SimInfo? = null,
        note: String = "Carrier USSD dialog launched for secure PIN entry."
    ): UssdResult {
        val encodedUssd = Uri.encode(ussdCode)
        val uri = Uri.parse("tel:$encodedUssd")
        val intent = Intent(Intent.ACTION_CALL, uri).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
            // Dual SIM multi-carrier intent hints across Android OEMs
            selectedSim?.let { sim ->
                putExtra("com.android.phone.extra.slot", sim.slotIndex)
                putExtra("simSlot", sim.slotIndex)
                putExtra("subscription_id", sim.subscriptionId)
                putExtra("android.telephony.extra.SUBSCRIPTION_ID", sim.subscriptionId)
            }
        }

        return try {
            context.startActivity(intent)
            UssdResult.CarrierDialogLaunched(note)
        } catch (se: SecurityException) {
            launchDialerIntent(ussdCode)
        } catch (e: Exception) {
            UssdResult.Failed(UssdError.GeneralException(e.message ?: "Could not launch phone call"))
        }
    }

    /**
     * Fallback that requires NO permissions: opens Android's dialer with code ready.
     */
    fun launchDialerIntent(ussdCode: String): UssdResult {
        val encoded = Uri.encode(ussdCode)
        val uri = Uri.parse("tel:$encoded")
        val intent = Intent(Intent.ACTION_DIAL, uri).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        }
        return try {
            context.startActivity(intent)
            UssdResult.DialerLaunched("Dialer opened with USSD code. Please press call on your phone.")
        } catch (e: Exception) {
            UssdResult.Failed(UssdError.GeneralException(e.message ?: "Could not open dialer"))
        }
    }
}
