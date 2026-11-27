package com.gzone.guesthousebooking

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.google.android.gms.tasks.Task
import com.google.android.gms.tasks.Tasks
import com.google.firebase.FirebaseApp
import com.google.firebase.FirebaseOptions
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Source
import org.json.JSONObject
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertThrows
import org.junit.Assume.assumeTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL
import java.util.concurrent.TimeUnit

@RunWith(AndroidJUnit4::class)
class BookingApiSmokeTest {
    private lateinit var app: FirebaseApp
    private lateinit var firestore: FirebaseFirestore
    private lateinit var token: String
    private lateinit var runId: String
    private var startDay: Int = 0

    @Before
    fun connect() {
        val args = InstrumentationRegistry.getArguments()
        assumeTrue("Requires the local booking API and Firebase emulators", args.getString("bookingSmoke") == "true")
        runId = requireNotNull(args.getString("runId"))
        startDay = requireNotNull(args.getString("startDay")).toInt()
        val role = requireNotNull(args.getString("role"))
        require(role in listOf("owner", "manager"))
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        app = FirebaseApp.initializeApp(
            context,
            FirebaseOptions.Builder()
                .setApplicationId("1:123456789:android:123456789abcdef0")
                .setProjectId("demo-guesthouse-trusted")
                .setApiKey("demo-emulator-only")
                .build(),
            "booking-api-smoke"
        )
        val auth = FirebaseAuth.getInstance(app)
        auth.useEmulator("10.0.2.2", 9099)
        val user = await(auth.signInWithEmailAndPassword("$role@android.example.test", "local-emulator-password")).user
        token = requireNotNull(await(requireNotNull(user).getIdToken(false)).token)
        firestore = FirebaseFirestore.getInstance(app)
        firestore.useEmulator("10.0.2.2", 8080)
    }

    @After
    fun disconnect() {
        if (::firestore.isInitialized) await(firestore.terminate())
        if (::app.isInitialized) app.delete()
    }

    @Test
    fun ownerCreatesGroup() {
        val (status, result) = post(command("group", 0, JSONObject().put("101", 1).put("102", 1)))
        assertEquals(200, status)
        assertEquals(1, result.getInt("version"))
        assertEquals(2, groupRooms("group").size)
    }

    @Test
    fun managerSeesConflictThenReleasesSelectedRoom() {
        assertEquals(2, groupRooms("group").size)
        val (status, result) = post(command("competing", 0, JSONObject().put("101", 1)))
        assertEquals(409, status)
        assertEquals("room-unavailable", result.getString("error"))
        val (editStatus, edited) = post(command("group", 1, JSONObject().put("101", JSONObject.NULL)))
        assertEquals(200, editStatus)
        assertEquals(2, edited.getInt("version"))
        assertEquals(setOf("102"), groupRooms("group").keys)
    }

    @Test
    fun ownerCanBookReleasedRoomAndConnectionFailureIsUnconfirmed() {
        assertEquals(setOf("102"), groupRooms("group").keys)
        assertEquals(200, post(command("rebook", 0, JSONObject().put("101", 1))).first)
        assertThrows(IOException::class.java) {
            post(command("disconnected", 0, JSONObject().put("102", 1)), port = 8788)
        }
        assertFalse(await(firestore.document(path("disconnected")).get(Source.SERVER)).exists())
    }

    private fun path(group: String) = "properties/android-demo/bookingGroups/$runId-$group"

    private fun groupRooms(group: String): Map<*, *> =
        await(firestore.document(path(group)).get(Source.SERVER)).get("rooms") as Map<*, *>

    private fun command(group: String, version: Int, changes: JSONObject): JSONObject =
        JSONObject().put("property", "android-demo")
            .put("groupId", "$runId-$group")
            .put("requestId", "$runId-$group-$version")
            .put("version", version)
            .put("changes", changes)
            .apply {
                if (version == 0) put("dates", JSONObject().put("startDay", startDay).put("endDay", startDay + 31))
            }

    private fun post(command: JSONObject, port: Int = 8787): Pair<Int, JSONObject> {
        val connection = URL("http://10.0.2.2:$port/booking-changes").openConnection() as HttpURLConnection
        try {
            connection.requestMethod = "POST"
            connection.instanceFollowRedirects = false
            connection.connectTimeout = 5_000
            connection.readTimeout = 30_000
            connection.doOutput = true
            connection.setRequestProperty("Authorization", "Bearer $token")
            connection.setRequestProperty("Content-Type", "application/json")
            connection.outputStream.use { it.write(command.toString().toByteArray(Charsets.UTF_8)) }
            val status = connection.responseCode
            val stream = if (status in 200..299) connection.inputStream else connection.errorStream
            return status to JSONObject(stream.bufferedReader().use { it.readText() })
        } finally {
            connection.disconnect()
        }
    }

    private fun <T> await(task: Task<T>): T = Tasks.await(task, 30, TimeUnit.SECONDS)
}
