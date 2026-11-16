package com.example.service

import com.example.base.FirestoreEmulatorTestBase
import com.example.model.ServiceResult
import com.example.util.calculateMultiBankSplit
import com.google.firebase.firestore.FirebaseFirestoreException
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.UUID

class AsteraRepositoryRuleTest : FirestoreEmulatorTestBase() {

  @Test
  fun registrationAndAtomicTransfer_succeedsWithProperBalances() = runBlocking {
    val tag = UUID.randomUUID().toString().take(6)
    val aliceUid = signInTestUser("alice_$tag@astera.test")
    val authService = AuthService(firestore, auth)
    val walletService = WalletService(firestore)
    val bankService = BankService(firestore)

    val regAlice =
      withTimeout(TIMEOUT_MS) {
        authService.completeProfileRegistration(
          uid = aliceUid,
          email = "alice_$tag@astera.test",
          name = "Alice $tag",
          phone = "081234567890",
          pin = "123456",
        )
      }
    assertTrue("Alice registration failed: $regAlice", regAlice is ServiceResult.Success)

    val aliceBanks = withTimeout(TIMEOUT_MS) { bankService.observeBankAccounts(aliceUid).first { it.size == 2 } }
    assertEquals(5_000_000L, aliceBanks[0].balance)
    assertEquals(5_000_000L, aliceBanks[1].balance)

    // Register Bob
    val bobUid = signInTestUser("bob_$tag@astera.test")
    val regBob =
      withTimeout(TIMEOUT_MS) {
        authService.completeProfileRegistration(
          uid = bobUid,
          email = "bob_$tag@astera.test",
          name = "Bob $tag",
          phone = "081987654321",
          pin = "654321",
        )
      }
    assertTrue("Bob registration failed: $regBob", regBob is ServiceResult.Success)

    // Sign back in as Alice and transfer Rp 500.000 to Bob
    signInTestUser("alice_$tag@astera.test")
    val txService = TransactionService(firestore)
    val txResult =
      withTimeout(TIMEOUT_MS) {
        txService.executeTransfer(
          senderId = aliceUid,
          senderName = "Alice $tag",
          receiverId = bobUid,
          receiverName = "Bob $tag",
          amount = 500_000L,
          note = "Makan siang",
          outOfBudget = false,
        )
      }
    assertTrue("Transfer failed: $txResult", txResult is ServiceResult.Success)

    val aliceProfile = (withTimeout(TIMEOUT_MS) { walletService.getUserProfile(aliceUid) } as ServiceResult.Success).data!!
    assertEquals(9_500_000L, aliceProfile.balance)

    // Top up Rp 600.000 split across Alice's Bank Alpha & Bank Beta (300k each)
    val topUpResult =
      withTimeout(TIMEOUT_MS) {
        txService.executeTopUp(
          uid = aliceUid,
          userName = "Alice $tag",
          amount = 600_000L,
          selectedBankIds = aliceBanks.map { it.id },
          outOfBudget = true,
        )
      }
    assertTrue("TopUp failed: $topUpResult", topUpResult is ServiceResult.Success)
    val topUpTx = (topUpResult as ServiceResult.Success).data
    assertEquals(2, topUpTx.splitDetail.size)
    assertEquals(300_000L, topUpTx.splitDetail[0].amount)
    assertEquals(300_000L, topUpTx.splitDetail[1].amount)

    val aliceAfterTopUp = (withTimeout(TIMEOUT_MS) { walletService.getUserProfile(aliceUid) } as ServiceResult.Success).data!!
    assertEquals(10_100_000L, aliceAfterTopUp.balance)
  }

  @Test
  fun multiBankSplit_redistributesShortfallAccurately() {
    val banks =
      listOf(
        com.example.model.BankAccount(id = "b1", bankName = "Bank Alpha", balance = 200_000L),
        com.example.model.BankAccount(id = "b2", bankName = "Bank Beta", balance = 1_000_000L),
      )
    val res = calculateMultiBankSplit(800_000L, banks)
    assertTrue(res is ServiceResult.Success)
    val items = (res as ServiceResult.Success).data
    assertEquals(200_000L, items.first { it.bankId == "b1" }.amount)
    assertEquals(600_000L, items.first { it.bankId == "b2" }.amount)

    val insufficient = calculateMultiBankSplit(2_000_000L, banks)
    assertTrue(insufficient is ServiceResult.Error)
    assertEquals("Saldo bank tidak cukup", (insufficient as ServiceResult.Error).message)
  }

  @Test
  fun pinVerification_locksAfterThreeWrongAttempts() = runBlocking {
    val tag = UUID.randomUUID().toString().take(6)
    val uid = signInTestUser("pin_$tag@astera.test")
    val authService = AuthService(firestore, auth)
    val profileService = ProfileService(firestore, appContext)

    withTimeout(TIMEOUT_MS) {
      authService.completeProfileRegistration(uid, "pin_$tag@astera.test", "Pin User", "081222333444", "112233")
    }

    val r1 = withTimeout(TIMEOUT_MS) { profileService.verifyPin(uid, "000000") } as ServiceResult.Success
    assertEquals(PinVerifyOutcome.WrongPin(2), r1.data)

    val r2 = withTimeout(TIMEOUT_MS) { profileService.verifyPin(uid, "000000") } as ServiceResult.Success
    assertEquals(PinVerifyOutcome.WrongPin(1), r2.data)

    val r3 = withTimeout(TIMEOUT_MS) { profileService.verifyPin(uid, "000000") } as ServiceResult.Success
    assertTrue(r3.data is PinVerifyOutcome.Blocked)
  }

  @Test
  fun unauthenticatedObserver_failsWithPermissionDenied() = runBlocking {
    auth.signOut()
    val walletService = WalletService(firestore)
    var caught: Exception? = null
    try {
      withTimeout(FLOW_TIMEOUT_MS) {
        walletService.observeUserProfile("some_uid_123").first()
      }
    } catch (e: Exception) {
      caught = e
    }
    val firestoreEx = checkNotNull(caught?.unwrapFirestoreException()) {
      "Expected FirebaseFirestoreException, got $caught"
    }
    assertEquals(FirebaseFirestoreException.Code.PERMISSION_DENIED, firestoreEx.code)
  }

  private companion object {
    const val TIMEOUT_MS = 8000L
    const val FLOW_TIMEOUT_MS = 4000L
  }
}
