package io.github.cshadd.monitoring_and_data_collection_android;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.ActivityManager;
import android.content.Context;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.MotionEvent;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import com.google.ar.core.Anchor;
import com.google.ar.core.Frame;
import com.google.ar.core.HitResult;
import com.google.ar.core.LightEstimate;
import com.google.ar.core.Plane;
import com.google.ar.core.Session;
import com.google.ar.core.exceptions.CameraNotAvailableException;
import com.google.ar.sceneform.AnchorNode;
import com.google.ar.sceneform.rendering.ModelRenderable;
import com.google.ar.sceneform.ux.TransformableNode;
import com.karumi.dexter.Dexter;
import com.karumi.dexter.MultiplePermissionsReport;
import com.karumi.dexter.PermissionToken;
import com.karumi.dexter.listener.PermissionRequest;
import com.karumi.dexter.listener.multi.MultiplePermissionsListener;
import com.opencsv.CSVWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class MainActivity extends AppCompatActivity {
    private static final double MIN_OPENGL_VERSION = 3.0;
    private static final String STORAGE_DIRECTORY = android.os.Environment.getExternalStorageDirectory().getAbsolutePath();
    private static final String STORAGE_LOG_FILE_PATH_PREFIX = STORAGE_DIRECTORY + File.separator + "cshadd_madca.log";

    private SpecialArFragment arFragment;
    private ModelRenderable andyRenderable;
    private TransformableNode andy;
    private float currentLight;
    private Sensor lightSensor;
    private List<String[]> logData;
    private SensorEventListener sensorListener;
    private SensorManager sensorManager;

    public MainActivity() {
        super();
        this.currentLight = 0f;
        this.logData = new ArrayList<>();
        return;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (!checkArSupport()) {
            return;
        }

        super.setContentView(R.layout.activity_main);

        this.sensorManager = (SensorManager)getSystemService(SENSOR_SERVICE);
        final List<Sensor> lightSensors = sensorManager.getSensorList(Sensor.TYPE_LIGHT);
        if (lightSensors.size() > 0) {
            this.lightSensor = lightSensors.get(0);
        }
        this.sensorListener = new SensorEventListener(){
            @Override
            public void onAccuracyChanged(Sensor sensor, int accuracy) {
                return;
            }

            @Override
            public void onSensorChanged(SensorEvent event) {
                if (event.sensor.getType() == Sensor.TYPE_LIGHT) {
                    currentLight = event.values[0];
                }
                return;
            }

        };

        Dexter.withActivity(this)
                .withPermissions(
                        Manifest.permission.CAMERA,
                        Manifest.permission.WRITE_EXTERNAL_STORAGE)
                .withListener(new MultiplePermissionsListener() {
                    @Override
                    public void onPermissionsChecked(MultiplePermissionsReport report) {
                        if (report.areAllPermissionsGranted()) {
                            Log.i("WIEGLY", "Needed permissions were granted!");
                        }
                        else {
                            Log.w("WIEGLY", "Needed permissions were not granted!");
                        }
                    }
                    @Override
                    public void onPermissionRationaleShouldBeShown(List<PermissionRequest> permissions, PermissionToken token) {
                        /* ... */
                    }
                }).check();

        this.arFragment = (SpecialArFragment)super.getSupportFragmentManager().findFragmentById(R.id.ux_fragment);
        this.buildAr();

        Log.i("NOGA", "I love monitors!");

        return;
    }

    @SuppressLint("NewApi")
    private void buildAr() {
        ModelRenderable.builder()
                .setSource(this, R.raw.andy)
                .build()
                .thenAccept(renderable -> this.andyRenderable = renderable)
                .exceptionally(throwable -> {
                    Log.e("NOGA", "Cannot load Andy!");
                    return null;
                });

        this.arFragment.setOnTapArPlaneListener((HitResult hitResult, Plane plane, MotionEvent motionEvent) -> {
            Log.i("NOGA", "Clicking for Andy!");
            if (this.andyRenderable == null) {
                return;
            }

            final Anchor anchor = hitResult.createAnchor();
            final AnchorNode anchorNode = new AnchorNode(anchor);
            anchorNode.setParent(this.arFragment.getArSceneView().getScene());

            if (this.andy != null) {
                this.andy.getScene().removeChild(this.andy);
                this.andy.setRenderable(null);
            }

            this.andy = new TransformableNode(this.arFragment.getTransformationSystem());
            this.andy.setParent(anchorNode);
            this.andy.setRenderable(this.andyRenderable);
            this.andy.select();

            try {
                final Session session = this.arFragment.getArSceneView().getSession();
                final Frame frame = session.update();

                // Get the light estimate for the current frame.
                final LightEstimate lightEstimate = frame.getLightEstimate();

                if (this.logData.size() == 10) {
                    try {
                        Toast.makeText(this, "Logging!", Toast.LENGTH_SHORT).show();
                        this.logCSV(this.logData);
                        this.logData.clear();
                    }
                    catch (IOException e) {
                        Log.e("NOGA", "Could not log data due to " + e);
                        e.printStackTrace();
                    }
                    catch (Exception e) {
                        Log.e("NOGA", "Could not log data due to " + e);
                        e.printStackTrace();
                    }
                    finally { }
                }
                else {
                    Toast.makeText(this, "Sent to log!", Toast.LENGTH_SHORT).show();
                    this.logData.add(new String[] {
                            "" + hitResult.getDistance(),
                            "" + lightEstimate.getPixelIntensity(),
                            "" + this.currentLight,
                    });
                }
            }
            catch (CameraNotAvailableException e) {
                Log.i("NOGA", "Could not calculate light data due to " + e);
                e.printStackTrace();
            }
            catch (Exception e) {
                Log.i("NOGA", "Could not calculate light data due to " + e);
                e.printStackTrace();
            }

            return;
        });
        return;
    }

    private boolean checkArSupport() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.N) {
            Log.e("NOGA", "Sceneform requires Android N or later.");
            this.finish();
            return false;
        }
        final String openGlVersionString = ((ActivityManager)this.getSystemService(Context.ACTIVITY_SERVICE))
                .getDeviceConfigurationInfo()
                .getGlEsVersion();
        if (Double.parseDouble(openGlVersionString) < MainActivity.MIN_OPENGL_VERSION) {
            Log.e("NOGA", "Sceneform requires OpenGL ES 3.0 later.");
            this.finish();
            return false;
        }
        return true;
    }

    private void logCSV(List<String[]> data) throws IOException {
        final String timestamp = new SimpleDateFormat("yyyyMMddHHmm", Locale.US).format(new Date());
        final String name = MainActivity.STORAGE_LOG_FILE_PATH_PREFIX + "." + timestamp + ".csv";
        Log.w("NOGA", "Logging " +  data.size() + " items to " + name);

        final CSVWriter writer = new CSVWriter(new FileWriter(name));
        writer.writeAll(data);
        writer.close();
        return;
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (this.sensorManager != null && this.sensorListener != null) {
            if (this.lightSensor != null) {
                sensorManager.registerListener(this.sensorListener, this.lightSensor, SensorManager.SENSOR_DELAY_NORMAL);
            }
        }
        return;
    }

    @Override
    protected void onStop() {
        super.onStop();
        if (this.sensorManager != null && this.sensorListener != null) {
            this.sensorManager.unregisterListener(this.sensorListener);
        }
        this.logData.clear();
        return;
    }
}
