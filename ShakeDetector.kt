import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager

class ShakeDetector(private val context: Context, private val onShake: () -> Unit) : SensorEventListener {

    private val sensorManager = context.getSystemService(Context.SENSOR_SERVICE) as SensorManager
    private val accelerometer: Sensor? = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
    private var shakeTimestamp: Long = 0
    private val shakeThresholdGravity = 2.7f
    private val shakeSlopTimeMs = 500

    fun start() {
        accelerometer?.also { accel ->
            sensorManager.registerListener(this, accel, SensorManager.SENSOR_DELAY_UI)
        }
    }

    fun stop() {
        sensorManager.unregisterListener(this)
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {
        // No hacer nada
    }

    override fun onSensorChanged(event: SensorEvent?) {
        if (event == null) return

        if (event.sensor.type == Sensor.TYPE_ACCELEROMETER) {
            val x = event.values[0]
            val y = event.values[1]
            val z = event.values[2]

            val gX = x / SensorManager.GRAVITY_EARTH
            val gY = y / SensorManager.GRAVITY_EARTH
            val gZ = z / SensorManager.GRAVITY_EARTH

            // Calcular la magnitud del vector de gravedad
            val gForce = Math.sqrt((gX * gX + gY * gY + gZ * gZ).toDouble()).toFloat()

            if (gForce > shakeThresholdGravity) {
                val now = System.currentTimeMillis()
                if (shakeTimestamp + shakeSlopTimeMs > now) {
                    return
                }

                shakeTimestamp = now
                onShake()
            }
        }
    }
}
