/*
 * Copyright 2019 New Vector Ltd
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.matrix.androidsdk.crypto.verification


import android.support.test.InstrumentationRegistry
import android.support.test.runner.AndroidJUnit4
import org.junit.Assert.*
import org.junit.FixMethodOrder
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.MethodSorters
import org.matrix.androidsdk.MXSession
import org.matrix.androidsdk.common.CommonTestHelper
import org.matrix.androidsdk.common.CryptoTestHelper
import org.matrix.androidsdk.common.TestApiCallback
import org.matrix.androidsdk.crypto.data.MXDeviceInfo
import org.matrix.androidsdk.crypto.data.MXUsersDevicesMap
import org.matrix.androidsdk.listeners.MXEventListener
import org.matrix.androidsdk.rest.model.Event
import org.matrix.androidsdk.rest.model.crypto.KeyVerificationAccept
import org.matrix.androidsdk.rest.model.crypto.KeyVerificationCancel
import org.matrix.androidsdk.rest.model.crypto.KeyVerificationStart
import org.matrix.androidsdk.util.JsonUtils
import java.util.*
import java.util.concurrent.CountDownLatch

@RunWith(AndroidJUnit4::class)
@FixMethodOrder(MethodSorters.NAME_ASCENDING)
class SASTest {
    private val mTestHelper = CommonTestHelper()
    private val mCryptoTestHelper = CryptoTestHelper(mTestHelper)

    @Test
    fun test_aliceStartThenAliceCancel() {
        val context = InstrumentationRegistry.getContext()

        val cryptoTestData = mCryptoTestHelper.doE2ETestWithAliceAndBobInARoom()

        val aliceSession = cryptoTestData.firstSession
        val bobSession = cryptoTestData.secondSession

        val aliceSasMgr = aliceSession.crypto!!.shortCodeVerificationManager
        val bobSasMgr = bobSession!!.crypto!!.shortCodeVerificationManager

        bobSession.dataHandler.addListener(object : MXEventListener() {
            override fun onToDeviceEvent(event: Event?) {
                super.onToDeviceEvent(event)
            }
        })

        val bobTxCreatedLatch = CountDownLatch(1)
        val bobListener = object : VerificationManager.VerificationManagerListener {
            override fun transactionCreated(tx: VerificationTransaction) {
            }

            override fun transactionUpdated(tx: VerificationTransaction) {
                bobTxCreatedLatch.countDown()
            }

            override fun markedAsManuallyVerified(userId: String, deviceId: String) {}
        }
        bobSasMgr.addListener(bobListener)

        val txID = aliceSasMgr.beginKeyVerificationSAS(bobSession.myUserId!!, bobSession.crypto!!.myDevice.deviceId)
        assertNotNull("Alice should have a started transaction", txID)

        val aliceKeyTx = aliceSasMgr.getExistingTransaction(bobSession.myUserId, txID!!)
        assertNotNull("Alice should have a started transaction", aliceKeyTx)

        mTestHelper.await(bobTxCreatedLatch)
        bobSasMgr.removeListener(bobListener)

        val bobKeyTx = bobSasMgr.getExistingTransaction(aliceSession.myUserId, txID)

        assertNotNull("Bob should have started verif transaction", bobKeyTx)
        assertTrue(bobKeyTx is SASVerificationTransaction)
        assertNotNull("Bob should have starting a SAS transaction", bobKeyTx)
        assertTrue(aliceKeyTx is SASVerificationTransaction)
        assertEquals("Alice and Bob have same transaction id", aliceKeyTx!!.transactionId, bobKeyTx!!.transactionId)

        val aliceSasTx = aliceKeyTx as SASVerificationTransaction?
        val bobSasTx = bobKeyTx as SASVerificationTransaction?

        assertEquals("Alice state should be started", SASVerificationTransaction.SASVerificationTxState.Started, aliceSasTx!!.state)
        assertEquals("Bob state should be started by alice", SASVerificationTransaction.SASVerificationTxState.OnStarted, bobSasTx!!.state)

        //Let's cancel from alice side
        val cancelLatch = CountDownLatch(1)

        val bobListener2 = object : VerificationManager.VerificationManagerListener {
            override fun transactionCreated(tx: VerificationTransaction) {}

            override fun transactionUpdated(tx: VerificationTransaction) {
                if (tx.transactionId == txID) {
                    if ((tx as SASVerificationTransaction).state === SASVerificationTransaction.SASVerificationTxState.OnCancelled) {
                        cancelLatch.countDown()
                    }
                }
            }

            override fun markedAsManuallyVerified(userId: String, deviceId: String) {}
        }
        bobSasMgr.addListener(bobListener2)

        aliceSasTx.cancel(aliceSession, CancelCode.User)
        mTestHelper.await(cancelLatch)

        assertEquals("Should be cancelled on alice side",
                SASVerificationTransaction.SASVerificationTxState.Cancelled, aliceSasTx.state)
        assertEquals("Should be cancelled on bob side",
                SASVerificationTransaction.SASVerificationTxState.OnCancelled, bobSasTx.state)

        assertEquals("Should be User cancelled on alice side",
                CancelCode.User, aliceSasTx.cancelledReason)
        assertEquals("Should be User cancelled on bob side",
                CancelCode.User, aliceSasTx.cancelledReason)

        assertNull(bobSasMgr.getExistingTransaction(aliceSession.myUserId, txID))
        assertNull(aliceSasMgr.getExistingTransaction(bobSession.myUserId, txID))

        cryptoTestData.clear(context)
    }

    @Test
    fun test_key_agreement_protocols_must_include_curve25519() {
        val context = InstrumentationRegistry.getContext()

        val cryptoTestData = mCryptoTestHelper.doE2ETestWithAliceAndBobInARoom()

        val bobSession = cryptoTestData.secondSession

        val protocols = listOf("meh_dont_know")
        val tid = "00000000"

        //Bob should receive a cancel
        var canceledToDeviceEvent: Event? = null
        val cancelLatch = CountDownLatch(1)
        bobSession!!.dataHandler.addListener(object : MXEventListener() {
            override fun onToDeviceEvent(event: Event?) {
                if (event!!.getType() == Event.EVENT_TYPE_KEY_VERIFICATION_CANCEL) {
                    if (event.contentAsJsonObject?.get("transaction_id")?.asString == tid) {
                        canceledToDeviceEvent = event
                        cancelLatch.countDown()
                    }
                }
            }
        })

        val aliceSession = cryptoTestData.firstSession
        val aliceUserID = aliceSession.myUserId
        val aliceDevice = aliceSession.crypto!!.myDevice.deviceId

        val aliceListener = object : VerificationManager.VerificationManagerListener {
            override fun transactionCreated(tx: VerificationTransaction) {}

            override fun transactionUpdated(tx: VerificationTransaction) {
                if ((tx as IncomingSASVerificationTransaction).uxState === IncomingSASVerificationTransaction.State.SHOW_ACCEPT) {
                    (tx as IncomingSASVerificationTransaction).performAccept(bobSession)
                }
            }

            override fun markedAsManuallyVerified(userId: String, deviceId: String) {}
        }
        aliceSession.crypto?.shortCodeVerificationManager?.addListener(aliceListener)


        fakeBobStart(bobSession, aliceUserID, aliceDevice, tid, protocols = protocols)

        mTestHelper.await(cancelLatch)


        val cancelReq = JsonUtils.getBasicGson()
                .fromJson(canceledToDeviceEvent!!.content, KeyVerificationCancel::class.java)
        assertEquals("Request should be cancelled with m.unknown_method", CancelCode.UnknownMethod.value, cancelReq.code)

        cryptoTestData.clear(context)

    }

    @Test
    fun test_key_agreement_macs_Must_include_hmac_sha256() {
        val context = InstrumentationRegistry.getContext()

        val cryptoTestData = mCryptoTestHelper.doE2ETestWithAliceAndBobInARoom()

        val bobSession = cryptoTestData.secondSession


        val mac = listOf("shaBit")
        val tid = "00000000"

        //Bob should receive a cancel
        var canceledToDeviceEvent: Event? = null
        val cancelLatch = CountDownLatch(1)
        bobSession!!.dataHandler.addListener(object : MXEventListener() {
            override fun onToDeviceEvent(event: Event?) {
                if (event!!.getType() == Event.EVENT_TYPE_KEY_VERIFICATION_CANCEL) {
                    if (event.contentAsJsonObject?.get("transaction_id")?.asString == tid) {
                        canceledToDeviceEvent = event
                        cancelLatch.countDown()
                    }
                }
            }
        })

        val aliceSession = cryptoTestData.firstSession
        val aliceUserID = aliceSession.myUserId
        val aliceDevice = aliceSession.crypto!!.myDevice.deviceId

        fakeBobStart(bobSession, aliceUserID, aliceDevice, tid, mac = mac)

        mTestHelper.await(cancelLatch)


        val cancelReq = JsonUtils.getBasicGson()
                .fromJson(canceledToDeviceEvent!!.content, KeyVerificationCancel::class.java)
        assertEquals("Request should be cancelled with m.unknown_method", CancelCode.UnknownMethod.value, cancelReq.code)

        cryptoTestData.clear(context)

    }

    @Test
    fun test_key_agreement_short_code_include_decimal() {
        val context = InstrumentationRegistry.getContext()

        val cryptoTestData = mCryptoTestHelper.doE2ETestWithAliceAndBobInARoom()

        val bobSession = cryptoTestData.secondSession


        val codes = listOf("bin", "foo", "bar")
        val tid = "00000000"

        //Bob should receive a cancel
        var canceledToDeviceEvent: Event? = null
        val cancelLatch = CountDownLatch(1)
        bobSession!!.dataHandler.addListener(object : MXEventListener() {
            override fun onToDeviceEvent(event: Event?) {
                if (event!!.getType() == Event.EVENT_TYPE_KEY_VERIFICATION_CANCEL) {
                    if (event.contentAsJsonObject?.get("transaction_id")?.asString == tid) {
                        canceledToDeviceEvent = event
                        cancelLatch.countDown()
                    }
                }
            }
        })

        val aliceSession = cryptoTestData.firstSession
        val aliceUserID = aliceSession.myUserId
        val aliceDevice = aliceSession.crypto!!.myDevice.deviceId

        fakeBobStart(bobSession, aliceUserID, aliceDevice, tid, codes = codes)

        mTestHelper.await(cancelLatch)


        val cancelReq = JsonUtils.getBasicGson()
                .fromJson(canceledToDeviceEvent!!.content, KeyVerificationCancel::class.java)
        assertEquals("Request should be cancelled with m.unknown_method", CancelCode.UnknownMethod.value, cancelReq.code)

        cryptoTestData.clear(context)

    }

    private fun fakeBobStart(bobSession: MXSession,
                             aliceUserID: String?,
                             aliceDevice: String?,
                             tid: String,
                             protocols: List<String> = SASVerificationTransaction.KNOWN_AGREEMENT_PROTOCOLS,
                             hashes: List<String> = SASVerificationTransaction.KNOWN_HASHES,
                             mac: List<String> = SASVerificationTransaction.KNOWN_MACS,
                             codes: List<String> = SASVerificationTransaction.KNOWN_SHORT_CODES) {
        val startMessage = KeyVerificationStart()
        startMessage.fromDevice = bobSession.crypto!!.myDevice.deviceId
        startMessage.method = KeyVerificationStart.VERIF_METHOD_SAS
        startMessage.transactionID = tid
        startMessage.keyAgreementProtocols = protocols
        startMessage.hashes = hashes
        startMessage.messageAuthenticationCodes = mac
        startMessage.shortAuthenticationStrings = codes

        val contentMap = MXUsersDevicesMap<Any>()
        contentMap.setObject(startMessage, aliceUserID, aliceDevice)

        val sendLatch = CountDownLatch(1)
        bobSession.cryptoRestClient.sendToDevice(
                Event.EVENT_TYPE_KEY_VERIFICATION_START,
                contentMap,
                tid,
                TestApiCallback<Void>(sendLatch)
        )
    }


    //any two devices may only have at most one key verification in flight at a time.
    // If a device has two verifications in progress with the same device, then it should cancel both verifications.
    @Test
    fun test_aliceStartTwoRequests() {
        val context = InstrumentationRegistry.getContext()

        val cryptoTestData = mCryptoTestHelper.doE2ETestWithAliceAndBobInARoom()

        val aliceSession = cryptoTestData.firstSession
        val bobSession = cryptoTestData.secondSession


        val aliceSasMgr = aliceSession.crypto!!.shortCodeVerificationManager

        val aliceCreatedLatch = CountDownLatch(2)
        val aliceCancelledLatch = CountDownLatch(2)
        val createdTx = ArrayList<SASVerificationTransaction>()
        val aliceListener = object : VerificationManager.VerificationManagerListener {

            override fun transactionCreated(tx: VerificationTransaction) {
                createdTx.add(tx as SASVerificationTransaction)
                aliceCreatedLatch.countDown()
            }

            override fun transactionUpdated(tx: VerificationTransaction) {
                if ((tx as SASVerificationTransaction).state === SASVerificationTransaction.SASVerificationTxState.OnCancelled) {
                    aliceCancelledLatch.countDown()
                }
            }

            override fun markedAsManuallyVerified(userId: String, deviceId: String) {}
        }
        aliceSasMgr.addListener(aliceListener)

        val bobUserId = bobSession!!.myUserId
        val bobDeviceId = bobSession.crypto!!.myDevice.deviceId
        aliceSasMgr.beginKeyVerificationSAS(bobUserId, bobDeviceId)
        aliceSasMgr.beginKeyVerificationSAS(bobUserId, bobDeviceId)

        mTestHelper.await(aliceCreatedLatch)
        mTestHelper.await(aliceCancelledLatch)

        cryptoTestData.clear(context)
    }

    /**
     * Test that when alice starts a 'correct' request, bob agrees.
     */
    @Test
    fun test_aliceAndBobAgreement() {
        val context = InstrumentationRegistry.getContext()

        val cryptoTestData = mCryptoTestHelper.doE2ETestWithAliceAndBobInARoom()

        val aliceSession = cryptoTestData.firstSession
        val bobSession = cryptoTestData.secondSession

        val aliceSasMgr = aliceSession.crypto!!.shortCodeVerificationManager
        val bobSasMgr = bobSession!!.crypto!!.shortCodeVerificationManager

        var accepted: KeyVerificationAccept? = null
        var startReq: KeyVerificationStart? = null


        val aliceAcceptedLatch = CountDownLatch(1)
        val aliceListener = object : VerificationManager.VerificationManagerListener {
            override fun markedAsManuallyVerified(userId: String, deviceId: String) {}

            override fun transactionCreated(tx: VerificationTransaction) {}

            override fun transactionUpdated(tx: VerificationTransaction) {
                if ((tx as SASVerificationTransaction).state === SASVerificationTransaction.SASVerificationTxState.OnAccepted) {
                    val at = tx as SASVerificationTransaction
                    accepted = at.accepted
                    startReq = at.startReq
                    aliceAcceptedLatch.countDown()
                }
            }
        }
        aliceSasMgr.addListener(aliceListener)

        val bobListener = object : VerificationManager.VerificationManagerListener {
            override fun transactionCreated(tx: VerificationTransaction) {}

            override fun transactionUpdated(tx: VerificationTransaction) {
                if ((tx as IncomingSASVerificationTransaction).uxState === IncomingSASVerificationTransaction.State.SHOW_ACCEPT) {
                    val at = tx as IncomingSASVerificationTransaction
                    at.performAccept(bobSession)
                }
            }

            override fun markedAsManuallyVerified(userId: String, deviceId: String) {}
        }
        bobSasMgr.addListener(bobListener)


        val bobUserId = bobSession.myUserId
        val bobDeviceId = bobSession.crypto!!.myDevice.deviceId
        aliceSasMgr.beginKeyVerificationSAS(bobUserId!!, bobDeviceId)
        mTestHelper.await(aliceAcceptedLatch)

        assertTrue("Should have receive a commitment", accepted!!.commitment?.trim()?.isEmpty() == false)

        //check that agreement is valid
        assertTrue("Agreed Protocol should be Valid", accepted!!.isValid())
        assertTrue("Agreed Protocol should be known by alice", startReq!!.keyAgreementProtocols!!.contains(accepted!!.keyAgreementProtocol))
        assertTrue("Hash should be known by alice", startReq!!.hashes!!.contains(accepted!!.hash))
        assertTrue("Hash should be known by alice", startReq!!.messageAuthenticationCodes!!.contains(accepted!!.messageAuthenticationCode))

        accepted!!.shortAuthenticationStrings?.forEach {
            assertTrue("all agreed Short Code should be known by alice", startReq!!.shortAuthenticationStrings!!.contains(it))
        }

        cryptoTestData.clear(context)
    }

    @Test
    fun test_aliceAndBobSASCode() {
        val context = InstrumentationRegistry.getContext()

        val cryptoTestData = mCryptoTestHelper.doE2ETestWithAliceAndBobInARoom()

        val aliceSession = cryptoTestData.firstSession
        val bobSession = cryptoTestData.secondSession

        val aliceSasMgr = aliceSession.crypto!!.shortCodeVerificationManager
        val bobSasMgr = bobSession!!.crypto!!.shortCodeVerificationManager


        val aliceSASLatch = CountDownLatch(1)
        val aliceListener = object : VerificationManager.VerificationManagerListener {
            override fun transactionCreated(tx: VerificationTransaction) {
            }

            override fun transactionUpdated(tx: VerificationTransaction) {
                val uxState = (tx as OutgoingSASVerificationRequest).uxState
                when (uxState) {
                    OutgoingSASVerificationRequest.State.SHOW_SAS -> {
                        aliceSASLatch.countDown()
                    }
                }
            }

            override fun markedAsManuallyVerified(userId: String, deviceId: String) {}
        }
        aliceSasMgr.addListener(aliceListener)

        val bobSASLatch = CountDownLatch(1)
        val bobListener = object : VerificationManager.VerificationManagerListener {
            override fun transactionCreated(tx: VerificationTransaction) {

            }

            override fun transactionUpdated(tx: VerificationTransaction) {
                val uxState = (tx as IncomingSASVerificationTransaction).uxState
                when (uxState) {
                    IncomingSASVerificationTransaction.State.SHOW_ACCEPT -> {
                        tx.performAccept(bobSession)
                    }
                }
                if (uxState === IncomingSASVerificationTransaction.State.SHOW_SAS) {
                    bobSASLatch.countDown()
                }
            }

            override fun markedAsManuallyVerified(userId: String, deviceId: String) {}
        }
        bobSasMgr.addListener(bobListener)


        val bobUserId = bobSession.myUserId
        val bobDeviceId = bobSession.crypto!!.myDevice.deviceId
        val verificationSAS = aliceSasMgr.beginKeyVerificationSAS(bobUserId!!, bobDeviceId)
        mTestHelper.await(aliceSASLatch)
        mTestHelper.await(bobSASLatch)

        val aliceTx = aliceSasMgr.getExistingTransaction(bobUserId, verificationSAS!!) as SASVerificationTransaction
        val bobTx = bobSasMgr.getExistingTransaction(aliceSession.myUserId, verificationSAS) as SASVerificationTransaction

        assertEquals("Should have same SAS", aliceTx.getShortCodeRepresentation(KeyVerificationStart.SAS_MODE_DECIMAL),
                bobTx.getShortCodeRepresentation(KeyVerificationStart.SAS_MODE_DECIMAL))

        cryptoTestData.clear(context)
    }


    @Test
    fun test_happyPath() {
        val context = InstrumentationRegistry.getContext()

        val cryptoTestData = mCryptoTestHelper.doE2ETestWithAliceAndBobInARoom()

        val aliceSession = cryptoTestData.firstSession
        val bobSession = cryptoTestData.secondSession

        val aliceSasMgr = aliceSession.crypto!!.shortCodeVerificationManager
        val bobSasMgr = bobSession!!.crypto!!.shortCodeVerificationManager


        val aliceSASLatch = CountDownLatch(1)
        val aliceListener = object : VerificationManager.VerificationManagerListener {
            override fun transactionCreated(tx: VerificationTransaction) {
            }

            override fun transactionUpdated(tx: VerificationTransaction) {
                val uxState = (tx as OutgoingSASVerificationRequest).uxState
                when (uxState) {
                    OutgoingSASVerificationRequest.State.SHOW_SAS -> {
                        tx.userHasVerifiedShortCode(aliceSession)
                    }
                    OutgoingSASVerificationRequest.State.VERIFIED -> {
                        aliceSASLatch.countDown()
                    }
                }
            }

            override fun markedAsManuallyVerified(userId: String, deviceId: String) {}
        }
        aliceSasMgr.addListener(aliceListener)

        val bobSASLatch = CountDownLatch(1)
        val bobListener = object : VerificationManager.VerificationManagerListener {
            override fun transactionCreated(tx: VerificationTransaction) {

            }

            override fun transactionUpdated(tx: VerificationTransaction) {
                val uxState = (tx as IncomingSASVerificationTransaction).uxState
                when (uxState) {
                    IncomingSASVerificationTransaction.State.SHOW_ACCEPT -> {
                        tx.performAccept(bobSession)
                    }
                    IncomingSASVerificationTransaction.State.SHOW_SAS -> {
                        tx.userHasVerifiedShortCode(bobSession)
                    }
                    IncomingSASVerificationTransaction.State.VERIFIED -> {
                        bobSASLatch.countDown()
                    }
                }
            }

            override fun markedAsManuallyVerified(userId: String, deviceId: String) {}
        }
        bobSasMgr.addListener(bobListener)


        val bobUserId = bobSession.myUserId
        val bobDeviceId = bobSession.crypto!!.myDevice.deviceId
        val verificationSAS = aliceSasMgr.beginKeyVerificationSAS(bobUserId!!, bobDeviceId)
        mTestHelper.await(aliceSASLatch)
        mTestHelper.await(bobSASLatch)

        //Assert that devices are verified
        val dLatchAlice = CountDownLatch(1)
        var bobDeviceInfoFromAlicePOV: MXDeviceInfo? = null
        aliceSession.crypto?.getDeviceInfo(bobUserId, bobDeviceId, object : TestApiCallback<MXDeviceInfo>(dLatchAlice) {
            override fun onSuccess(info: MXDeviceInfo) {
                bobDeviceInfoFromAlicePOV = info
                super.onSuccess(info)
            }
        })
        val dLatchBob = CountDownLatch(1)
        var aliceDeviceInfoFromBobPOV: MXDeviceInfo? = null
        bobSession.crypto?.getDeviceInfo(aliceSession.myUserId, aliceSession.crypto!!.myDevice.deviceId, object : TestApiCallback<MXDeviceInfo>(dLatchBob) {
            override fun onSuccess(info: MXDeviceInfo) {
                aliceDeviceInfoFromBobPOV = info
                super.onSuccess(info)
            }
        })

        //latch wait a bit again
        Thread.sleep(1000)

        assertTrue("alice device should be verified from bob point of view", aliceDeviceInfoFromBobPOV!!.isVerified)
        assertTrue("bob device should be verified from alice point of view", bobDeviceInfoFromAlicePOV!!.isVerified)
        cryptoTestData.clear(context)
    }

    companion object {
        private const val LOG_TAG = "SASTest"
    }
}
