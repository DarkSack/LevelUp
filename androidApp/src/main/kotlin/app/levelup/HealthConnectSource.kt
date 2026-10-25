package app.levelup

import android.content.Context
import androidx.health.connect.client.HealthConnectClient
import androidx.health.connect.client.permission.HealthPermission
import androidx.health.connect.client.records.ExerciseSessionRecord
import androidx.health.connect.client.records.StepsRecord
import androidx.health.connect.client.request.AggregateRequest
import androidx.health.connect.client.time.TimeRangeFilter
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

/**
 * Pasos y minutos de ejercicio de hoy desde Health Connect. Solo lectura, solo de hoy y solo para mostrarlos:
 * no se guardan en la base de datos ni se sincronizan.
 */
class HealthConnectSource(
    private val context: Context,
    private val request: suspend (Set<String>) -> Set<String>,
) : HealthSource {
    private val permissions = setOf(
        HealthPermission.getReadPermission(StepsRecord::class),
        HealthPermission.getReadPermission(ExerciseSessionRecord::class),
    )

    private fun client(): HealthConnectClient? =
        if (HealthConnectClient.getSdkStatus(context) == HealthConnectClient.SDK_AVAILABLE) HealthConnectClient.getOrCreate(context) else null

    override suspend fun available(): Boolean = client() != null

    override suspend fun hasPermission(): Boolean =
        client()?.permissionController?.getGrantedPermissions()?.containsAll(permissions) == true

    override suspend fun requestPermission(): Boolean = client() != null && request(permissions).containsAll(permissions)

    override suspend fun today(): HealthToday? {
        val client = client() ?: return null
        if (!hasPermission()) return null
        val zone = ZoneId.systemDefault()
        val result = client.aggregate(
            AggregateRequest(
                setOf(StepsRecord.COUNT_TOTAL, ExerciseSessionRecord.EXERCISE_DURATION_TOTAL),
                TimeRangeFilter.between(LocalDate.now(zone).atStartOfDay(zone).toInstant(), Instant.now()),
            ),
        )
        return HealthToday(
            steps = result[StepsRecord.COUNT_TOTAL] ?: 0,
            exerciseMinutes = result[ExerciseSessionRecord.EXERCISE_DURATION_TOTAL]?.toMinutes() ?: 0,
        )
    }
}
