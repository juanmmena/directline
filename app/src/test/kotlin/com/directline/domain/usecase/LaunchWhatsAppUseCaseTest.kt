package com.directline.domain.usecase

import com.directline.core.whatsapp.AndroidIntentLauncher
import com.directline.core.whatsapp.MessageEncoder
import com.directline.core.whatsapp.PhoneNumberNormalizer
import com.directline.core.whatsapp.PhoneNumberParser
import com.directline.core.whatsapp.PhoneNumberValidator
import com.directline.core.whatsapp.WhatsAppDestination
import com.directline.core.whatsapp.WhatsAppDestinationBuilder
import com.directline.core.whatsapp.WhatsAppLaunchResult
import com.directline.core.whatsapp.WhatsAppLauncher
import com.directline.domain.model.Contact
import com.directline.domain.model.InteractionActionType
import com.directline.domain.model.InteractionHistory
import com.directline.domain.model.Tag
import com.directline.domain.repository.ContactRepository
import com.directline.domain.repository.HistoryRepository
import com.directline.domain.repository.PhoneNumberDraft
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class LaunchWhatsAppUseCaseTest {

  private class FakeIntentLauncher(
    private val resultFactory: (WhatsAppDestination) -> WhatsAppLaunchResult,
  ) : AndroidIntentLauncher {
    var lastDestination: WhatsAppDestination? = null
    override fun launch(destination: WhatsAppDestination): WhatsAppLaunchResult {
      lastDestination = destination
      return resultFactory(destination)
    }
  }

  private class FakeHistoryRepository : HistoryRepository {
    val recorded = mutableListOf<InteractionHistory>()
    override fun observeHistory(): Flow<List<InteractionHistory>> = MutableStateFlow(recorded)
    override fun observeRecentHistory(limit: Int): Flow<List<InteractionHistory>> =
      MutableStateFlow(recorded.take(limit))
    override suspend fun recordLaunchAttempt(
      contactId: String?,
      contactNameSnapshot: String?,
      phoneNumberSnapshot: String,
      messagePreview: String?,
    ) {
      recorded.add(
        InteractionHistory(
          id = "h${recorded.size + 1}",
          contactId = contactId,
          contactNameSnapshot = contactNameSnapshot,
          phoneNumberSnapshot = phoneNumberSnapshot,
          messagePreview = messagePreview,
          actionType = InteractionActionType.WHATSAPP_LAUNCH_ATTEMPT,
          createdAt = 1L,
        ),
      )
    }
    override suspend fun deleteHistoryItem(id: String) { recorded.removeAll { it.id == id } }
    override suspend fun clearHistory() { recorded.clear() }
  }

  private class FakeContactRepository : ContactRepository {
    override fun observeContacts(): Flow<List<Contact>> = MutableStateFlow(emptyList())
    override fun observeFavorites(): Flow<List<Contact>> = MutableStateFlow(emptyList())
    override fun observeTags(): Flow<List<Tag>> = MutableStateFlow(emptyList())
    override suspend fun getContactById(id: String): Contact? = null
    override suspend fun findContactByNormalizedPhone(normalizedPhone: String): Contact? = null
    override suspend fun saveContact(
      id: String?,
      name: String,
      notes: String,
      isFavorite: Boolean,
      phoneNumbers: List<PhoneNumberDraft>,
      tagNames: List<String>,
    ): String = "id"
    override suspend fun toggleFavorite(contactId: String, isFavorite: Boolean) = Unit
    override suspend fun deleteContact(contactId: String) = Unit
    override suspend fun deleteAllContacts() = Unit
  }

  private fun createUseCase(
    launcherResult: (WhatsAppDestination) -> WhatsAppLaunchResult,
    historyRepo: FakeHistoryRepository,
  ): Pair<LaunchWhatsAppUseCase, FakeIntentLauncher> {
    val parser = PhoneNumberParser()
    val validator = PhoneNumberValidator(parser, PhoneNumberNormalizer(parser))
    val builder = WhatsAppDestinationBuilder(MessageEncoder())
    val fakeIntent = FakeIntentLauncher(launcherResult)
    val whatsAppLauncher = WhatsAppLauncher(validator, builder, fakeIntent)
    val useCase = LaunchWhatsAppUseCase(whatsAppLauncher, FakeContactRepository(), historyRepo)
    return useCase to fakeIntent
  }

  @Test
  fun `valid launch initiates intent and records history snapshot`() = runTest {
    val history = FakeHistoryRepository()
    val (useCase, fakeIntent) = createUseCase(
      launcherResult = { WhatsAppLaunchResult.LaunchInitiated(it) },
      historyRepo = history,
    )

    val result = useCase(
      rawPhoneNumber = "300 123 4567",
      selectedCountryIso = "CO",
      message = "Hello!",
    )

    assertTrue(result is WhatsAppLaunchResult.LaunchInitiated)
    assertEquals("https://wa.me/573001234567?text=Hello%21", fakeIntent.lastDestination?.url)
    assertEquals(1, history.recorded.size)
    assertEquals("+573001234567", history.recorded.first().phoneNumberSnapshot)
  }

  @Test
  fun `invalid phone rejects without recording history`() = runTest {
    val history = FakeHistoryRepository()
    val (useCase, _) = createUseCase(
      launcherResult = { WhatsAppLaunchResult.LaunchInitiated(it) },
      historyRepo = history,
    )

    val result = useCase(
      rawPhoneNumber = "123",
      selectedCountryIso = "CO",
      message = null,
    )

    assertTrue(result is WhatsAppLaunchResult.InvalidPhoneNumber)
    assertTrue(history.recorded.isEmpty())
  }

  @Test
  fun `unavailable application returns ApplicationUnavailable and does not record history`() = runTest {
    val history = FakeHistoryRepository()
    val (useCase, _) = createUseCase(
      launcherResult = { WhatsAppLaunchResult.ApplicationUnavailable },
      historyRepo = history,
    )

    val result = useCase(
      rawPhoneNumber = "+573001234567",
      selectedCountryIso = null,
      message = null,
    )

    assertEquals(WhatsAppLaunchResult.ApplicationUnavailable, result)
    assertTrue(history.recorded.isEmpty())
  }
}
