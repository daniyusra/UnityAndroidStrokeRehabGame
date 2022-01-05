package arp.camera;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.PixelFormat;
import android.hardware.camera2.CameraAccessException;
import android.hardware.camera2.CameraCaptureSession;
import android.hardware.camera2.CameraCharacteristics;
import android.hardware.camera2.CameraDevice;
import android.hardware.camera2.CameraManager;
import android.hardware.camera2.CameraMetadata;
import android.hardware.camera2.CaptureRequest;
import android.media.Image;
import android.media.ImageReader;
import android.opengl.GLES20;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.HandlerThread;
import android.renderscript.RenderScript;
import android.util.Log;
import android.util.Size;
import android.view.Surface;




import androidx.annotation.NonNull;
import androidx.camera.core.*;
import androidx.camera.lifecycle.ProcessCameraProvider;

import com.google.common.util.concurrent.ListenableFuture;
import com.unity3d.player.UnityPlayerActivity;

import java.nio.IntBuffer;
import java.util.Arrays;
import java.util.concurrent.ExecutionException;


public class CameraPluginActivity extends UnityPlayerActivity {

    private static final String TAG = "CameraPluginActivity";

    // Enable access to non-static methods from Unity
    public static CameraPluginActivity _context;
    private boolean _update;

    private static final int MAX_IMAGES = 4;
    private static final int CONVERSION_FRAME_RATE = 60;
    private Size _previewSize = new Size(640, 480);
    private CameraDevice _cameraDevice;
    private CameraCaptureSession _captureSession;
    private boolean isGoingUp = false;

    private ImageReader _imagePreviewReader;
    private RenderScript _renderScript;
    private YuvToRgb _conversionScript;
    private Surface _previewSurface;

    private HandlerThread _handlerThread;
    private ImageCapture imagecapture = null;

    @SuppressWarnings("JniMissingFunction")
    public native void nativeInit();

    @SuppressWarnings("JniMissingFunction")
    public native void nativeRelease();

    /**
     * An additional thread for running tasks that shouldn't block the UI.
     */
    private HandlerThread mBackgroundThread;

    /**
     * A {@link Handler} for running tasks in the background.
     */
    private Handler mBackgroundHandler;

    @Override
    protected void onCreate(Bundle bundle) {
        super.onCreate(bundle);

        System.loadLibrary("NativeCameraPlugin");
        nativeInit();

        setContext(this);

        _renderScript = RenderScript.create(this);

    }

    private synchronized static void setContext(CameraPluginActivity context) {
        CameraPluginActivity._context = context;
    }

    @Override
    public void onResume() {
        super.onResume();

        _handlerThread = new HandlerThread(TAG);
        _handlerThread.start();

        startCamera();
        startBackgroundThread();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

        nativeRelease();

        setContext(null);
    }

    @Override
    public void onPause() {

        _handlerThread.quitSafely();
        try {
            _handlerThread.join();
            _handlerThread = null;
        } catch (final Exception e) {
            e.printStackTrace();
        }

        stopCamera();
        stopBackgroundThread();
        super.onPause();
    }

    private final CameraDevice.StateCallback _cameraStateCallback = new CameraDevice.StateCallback() {
        @Override
        public void onOpened(CameraDevice camera) {
            _cameraDevice = camera;
            setupCameraDevice();
        }

        @Override
        public void onDisconnected(CameraDevice camera) {
            Log.w(TAG, "CameraDevice.StateCallback onDisconnected");
        }

        @Override
        public void onError(CameraDevice camera, int error) {
            Log.e(TAG, "CameraDevice.StateCallback onError[" + error + "]");
        }
    };

    private CameraCaptureSession.StateCallback _sessionStateCallback = new CameraCaptureSession.StateCallback() {
        @Override
        public void onConfigured(CameraCaptureSession session) {
            CameraPluginActivity.this._captureSession = session;
            try {
                session.setRepeatingRequest(createCaptureRequest(), null, mBackgroundHandler);
            } catch (CameraAccessException e) {
                e.printStackTrace();
            }
        }

        @Override
        public void onConfigureFailed(CameraCaptureSession session) {
            Log.e(TAG, "CameraCaptureSession.StateCallback onConfigureFailed");
        }
    };


    /*
     *
     * Called from NDK to update the texture in Unity.
     * It is done this way since Unity does not allow Java callbacks for GL.IssuePluginEvent
     *
     */
    private void requestJavaRendering(int texturePointer) {

        if (!_update) {
            return;
        }

        int[] imageBuffer = new int[0];

        if (_conversionScript != null) {
            imageBuffer = _conversionScript.getOutputBuffer();
        }

        if (imageBuffer.length > 1) {
            GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, texturePointer);

            GLES20.glTexSubImage2D(GLES20.GL_TEXTURE_2D, 0, 0, 0, _previewSize.getWidth(),
                    _previewSize.getHeight(), GLES20.GL_RGBA, GLES20.GL_UNSIGNED_BYTE,
                    IntBuffer.wrap(imageBuffer));

        }
    }

    private void setupCameraDevice() {
        try {
            if (_previewSurface != null) {
                _cameraDevice.createCaptureSession(Arrays.asList(_previewSurface),
                        _sessionStateCallback, mBackgroundHandler);
            } else {
                Log.e(TAG, "failed creating preview surface");
            }
        } catch (CameraAccessException e) {
            e.printStackTrace();
        }
    }

    private CaptureRequest createCaptureRequest() {
        try {
            CaptureRequest.Builder builder =
                    _cameraDevice.createCaptureRequest(CameraDevice.TEMPLATE_PREVIEW);

            builder.set(CaptureRequest.CONTROL_AF_MODE, CameraMetadata.CONTROL_AF_MODE_CONTINUOUS_PICTURE);

            builder.addTarget(_previewSurface);
            return builder.build();
        } catch (CameraAccessException e) {
            e.printStackTrace();
            return null;
        }
    }

    private void startCamera() {
        CameraManager manager = (CameraManager) getSystemService(CAMERA_SERVICE);
        try {

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                if (checkSelfPermission(Manifest.permission.CAMERA)
                        != PackageManager.PERMISSION_GRANTED) {
                    return;
                }
            }

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP_MR1) {

                String pickedCamera = getCamera(manager);
                manager.openCamera(pickedCamera, _cameraStateCallback, mBackgroundHandler);

                final int previewHeight = _previewSize.getHeight();
                final int previewWidth = _previewSize.getWidth();
                _imagePreviewReader = ImageReader.newInstance(previewWidth, previewHeight,
                        PixelFormat.RGBA_8888, MAX_IMAGES);
                _imagePreviewReader.setOnImageAvailableListener(
                        mOnImageAvailableListener, mBackgroundHandler);
                _conversionScript = new YuvToRgb(_renderScript, _previewSize, CONVERSION_FRAME_RATE);
                _conversionScript.setOutputSurface(_imagePreviewReader.getSurface());
                _previewSurface = _conversionScript.getInputSurface();
            }

        } catch (CameraAccessException e) {
            e.printStackTrace();
        } catch (SecurityException e) {
            e.printStackTrace();
        }
    }





    private void stopCamera() {
        try {
            _captureSession.abortCaptures();
            _captureSession.close();
        } catch (Exception e) {
            e.printStackTrace();
        }

        try {
            Image image = _imagePreviewReader.acquireLatestImage();
            if (image != null) {
                image.close();
            }
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            if (_imagePreviewReader != null) {
                _imagePreviewReader.close();
                _imagePreviewReader = null;
            }
        }

        try {
            _cameraDevice.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
        _conversionScript = null;

    }

    private String getCamera(CameraManager manager) {
        try {
            for (String cameraId : manager.getCameraIdList()) {
                CameraCharacteristics characteristics = manager.getCameraCharacteristics(cameraId);
                int cameraOrientation = characteristics.get(CameraCharacteristics.LENS_FACING);
                if (cameraOrientation == CameraCharacteristics.LENS_FACING_FRONT) {
                    return cameraId;
                }
            }
        } catch (CameraAccessException e) {
            e.printStackTrace();
        }
        return null;
    }

    @UsedThroughReflection
    public void enablePreviewUpdater(boolean update) {
        _update = update;
    }


    /* my shit code */

//    private void startCameraX(){
//        ListenableFuture<ProcessCameraProvider> cameraProviderFuture = ProcessCameraProvider.getInstance(this);
//        cameraProviderFuture.addListener(()->{
//                    try {
//                        ProcessCameraProvider cameraProvider= cameraProviderFuture.get();
//
//                        cameraProvider.unbindAll();
//
//                        CameraSelector cameraSelector = new CameraSelector.Builder()
//                                .requireLensFacing(CameraSelector.LENS_FACING_FRONT)
//                                .build();
//
//                        Preview preview = new Preview.Builder()
//                                .build();
//
//                        preview.setSurfaceProvider(new Preview.SurfaceProvider(){
//
//                            @Override
//                            public void onSurfaceRequested(@NonNull SurfaceRequest request) {
//                                request.provideSurface(_previewSurface,getMainExecutor(), (void) -> {
//                                    _previewSurface.release();
//
//                                });
//                            }
//                        });
//
//                    } catch (ExecutionException e) {
//                        e.printStackTrace();
//                    } catch (InterruptedException e) {
//                        e.printStackTrace();
//                    }
//                }, getMainExecutor()
//
//        );
//
//    }

    private void startBackgroundThread() {
        mBackgroundThread = new HandlerThread("CameraBackground");
        mBackgroundThread.start();
        mBackgroundHandler = new Handler(mBackgroundThread.getLooper());
    }

    private void stopBackgroundThread() {
        mBackgroundThread.quitSafely();
        try {
            mBackgroundThread.join();
            mBackgroundThread = null;
            mBackgroundHandler = null;
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    private final ImageReader.OnImageAvailableListener mOnImageAvailableListener
            = new ImageReader.OnImageAvailableListener() {

        @Override
        public void onImageAvailable(ImageReader reader) {
            //TODO: IMPLEMENT POSE ANALYSIS HERE

            //mBackgroundHandler.post(new ImageSaver(reader.acquireNextImage(), mFile));
        }

    };

}
