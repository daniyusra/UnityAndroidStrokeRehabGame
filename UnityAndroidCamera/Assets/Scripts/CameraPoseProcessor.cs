using System;
using System.Collections;
using System.Collections.Generic;
using System.Runtime.InteropServices;
using UnityEngine;

public class CameraPoseProcessor : MonoBehaviour
{
    [DllImport("NativeCameraPlugin")]
    private static extern void SetTextureFromUnity(System.IntPtr texture);

    [DllImport("NativeCameraPlugin")]
    private static extern IntPtr GetRenderEventFunc();

    public Material displayMaterial;

    private AndroidJavaObject _androidJavaPlugin = null;

    void Start()
    {
        if (Application.platform == RuntimePlatform.Android)
        {
            using (AndroidJavaClass javaClass = new AndroidJavaClass("arp.camera.CameraPluginActivity"))
            {
                _androidJavaPlugin = javaClass.GetStatic<AndroidJavaObject>("_context");
            }

            //CreateTextureAndPassToPlugin();
            //yield return StartCoroutine("CallPluginAtEndOfFrames");
        }
    }





    // Update is called once per frame
    void Update()
    {
        bool a = _androidJavaPlugin.Call<bool>("isBothHandsAboveShoulder");

        if(a == true)
        {
            displayMaterial.SetColor("_Color", Color.red);

        } else
        {
            displayMaterial.SetColor("_Color", Color.white);
        }

        float b = _androidJavaPlugin.Call<float>("returnPersonAngle");
    }
}
