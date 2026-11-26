package com.directline.core.whatsapp

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.Uri
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

sealed interface WhatsAppLaunchResult {
  data class LaunchInitiated(val destination: WhatsAppDestination) : WhatsAppLaunchResult
  data class InvalidPhoneNumber(val validationResult: PhoneNumberValidationResult) : WhatsAppLaunchResult
  data class InvalidDestination(val reason: String) : WhatsAppLaunchResult
  data object ApplicationUnavailable : WhatsAppLaunchResult
  data class LaunchFailed(val reason: String) : WhatsAppLaunchResult
}

interface AndroidIntentLauncher {
  fun launch(destination: WhatsAppDestination): WhatsAppLaunchResult
}

@Singleton
class DefaultAndroidIntentLauncher @Inject constructor(
  @param:ApplicationContext private val context: Context,
) : AndroidIntentLauncher {
  override fun launch(destination: WhatsAppDestination): WhatsAppLaunchResult {
    val uri = try {
      Uri.parse(destination.url)
    } catch (e: Exception) {
      return WhatsAppLaunchResult.InvalidDestination("Could not construct a valid URI.")
    }

    if (uri.scheme != "https" || uri.host != "wa.me") {
      return WhatsAppLaunchResult.InvalidDestination("Unsupported destination URI.")
    }

    val intent = Intent(Intent.ACTION_VIEW, uri).apply {
      addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    }

    return try {
      context.startActivity(intent)
      WhatsAppLaunchResult.LaunchInitiated(destination)
    } catch (_: ActivityNotFoundException) {
      WhatsAppLaunchResult.ApplicationUnavailable
    } catch (e: SecurityException) {
      WhatsAppLaunchResult.LaunchFailed("Android prevented opening the destination.")
    } catch (e: Exception) {
      WhatsAppLaunchResult.LaunchFailed("Unable to launch destination.")
    }
  }
}

@Singleton
class WhatsAppLauncher @Inject constructor(
  private val phoneNumberValidator: PhoneNumberValidator,
  private val destinationBuilder: WhatsAppDestinationBuilder,
  private val intentLauncher: AndroidIntentLauncher,
) {
  fun launch(
    rawPhoneNumber: String,
    selectedCountryIso: String?,
    message: String? = null,
  ): WhatsAppLaunchResult {
    val validation = phoneNumberValidator.validate(rawPhoneNumber, selectedCountryIso)
    if (validation !is PhoneNumberValidationResult.Valid) {
      return WhatsAppLaunchResult.InvalidPhoneNumber(validation)
    }

    return launchNormalized(validation.normalizedE164, message)
  }

  fun launchNormalized(
    normalizedE164: String,
    message: String? = null,
  ): WhatsAppLaunchResult {
    return when (val built = destinationBuilder.build(normalizedE164, message)) {
      is DestinationValidationResult.InvalidPhoneNumber -> {
        WhatsAppLaunchResult.InvalidDestination("Phone number must be in valid E.164 format.")
      }
      is DestinationValidationResult.MessageTooLong -> {
        WhatsAppLaunchResult.InvalidDestination(
          "Message is too long (${built.actualLength}/${built.maxLength} characters).",
        )
      }
      is DestinationValidationResult.Valid -> {
        intentLauncher.launch(built.destination)
      }
    }
  }
}
