package io.github.cshadd.monitoring_and_data_collection_android;

import android.app.ActivityManager;
import android.content.Context;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.Gravity;
import android.view.MotionEvent;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import com.google.ar.core.Anchor;
import com.google.ar.core.HitResult;
import com.google.ar.core.Plane;
import com.google.ar.sceneform.AnchorNode;
import com.google.ar.sceneform.rendering.ModelRenderable;
import com.google.ar.sceneform.ux.ArFragment;
import com.google.ar.sceneform.ux.TransformableNode;

public class MainActivity extends AppCompatActivity {
    private static final double MIN_OPENGL_VERSION = 3.0;

    private ArFragment arFragment;
    private ModelRenderable andyRenderable;
    private TransformableNode andy;

    public MainActivity() {
        super();
        return;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (!checkArSupport()) {
            return;
        }

        super.setContentView(R.layout.activity_main);

        this.arFragment = (ArFragment)super.getSupportFragmentManager().findFragmentById(R.id.ux_fragment);
        this.andy = new TransformableNode(this.arFragment.getTransformationSystem());
        this.buildAr();

        Log.i("NOGA", "I love monitors!");
        return;
    }

    private void buildAr() {
        // When you build a Renderable, Sceneform loads its resources in the background while returning
        // a CompletableFuture. Call thenAccept(), handle(), or check isDone() before calling get().
        ModelRenderable.builder()
                .setSource(this, R.raw.andy)
                .build()
                .thenAccept(renderable -> this.andyRenderable = renderable)
                .exceptionally(
                        throwable -> {
                            final Toast toast =
                                    Toast.makeText(this, "Unable to load Andy renderable.", Toast.LENGTH_LONG);
                            toast.setGravity(Gravity.CENTER, 0, 0);
                            toast.show();
                            return null;
                        });

        this.arFragment.setOnTapArPlaneListener(
                (HitResult hitResult, Plane plane, MotionEvent motionEvent) -> {
                    if (this.andyRenderable == null) {
                        return;
                    }

                    // Create the Anchor.
                    final Anchor anchor = hitResult.createAnchor();
                    AnchorNode anchorNode = new AnchorNode(anchor);
                    anchorNode.setParent(this.arFragment.getArSceneView().getScene());

                    this.andy.setParent(anchorNode);
                    this.andy.setRenderable(this.andyRenderable);
                    this.andy.select();

                });
        return;
    }

    private boolean checkArSupport() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.N) {
            Log.e("NOGA", "Sceneform requires Android N or later.");
            Toast.makeText(this, "Sceneform requires Android N or later.", Toast.LENGTH_LONG).show();
            this.finish();
            return false;
        }
        final String openGlVersionString = ((ActivityManager)this.getSystemService(Context.ACTIVITY_SERVICE))
                .getDeviceConfigurationInfo()
                .getGlEsVersion();
        if (Double.parseDouble(openGlVersionString) < MainActivity.MIN_OPENGL_VERSION) {
            Log.e("NOGA", "Sceneform requires OpenGL ES 3.0 later.");
            Toast.makeText(this, "Sceneform requires OpenGL ES 3.0 or later.", Toast.LENGTH_LONG)
                    .show();
            this.finish();
            return false;
        }
        return true;
    }
}
